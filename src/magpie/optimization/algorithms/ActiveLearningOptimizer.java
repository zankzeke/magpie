/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.algorithms;

import java.util.List;
import magpie.optimization.BaseOptimizer;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.regression.CompositeRegression;

/**
 * An active learning scheme that using a machine-learning model to find optimal candidates. 
 * Algorithm iteratively builds a model on all evaluated entries and uses it to select 
 * which ones evaluate next. 
 * 
 * <p>This implementation uses one uses a {@linkplain  CompositeRegression} model. The
 * variance between the predictions of the submodels defines the accuracy of the prediction, which
 * can also be used when selecting entries.
 * 
 * <usage><p><b>Usage</b>: &lt;Random fraction> &lt;Worst fraction>
 * <br><pr><i>Random fraction</i>: Fraction of entries each generation that will be randomly chosen
 * <br><pr><i>Worst fraction</i>: Fraction of entries to select from the entries with least-reliable predictions</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>set model $&lt;model></b> - Define group of models to train during optimization
 * <br><pr><i>model</i>: Model to iteratively train and use predictively. Must implement {@linkplain CompositeRegression}.</command>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ActiveLearningOptimizer extends BaseOptimizer {
    /** Internal model used to make predictions */
    protected CompositeRegression Model = null;
    /** Fraction of new entries to select randomly */
    protected double RandomFraction = 0.0;
    /** Fraction of entries to select from the worst predictions */
    protected double WorstFraction = 0.0;

    @Override
    public void setOptions(List Options) throws Exception {
        double opt1, opt2;
        try {
            opt1 = Double.valueOf(Options.get(0).toString());
            opt2 = Double.valueOf(Options.get(1).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setRandomFraction(opt1);
        setWorstFraction(opt2);
    }

    @Override
    public String printUsage() {
        return "Usage: <random fraction> <worst fraction>";
    }

    /**
     * Define what fraction of new entries are to come from entries with the 
     *  least-reliable predictions
     * @param WorstFraction Desired fraction
     * @throws java.lang.Exception If fraction is out of range
     */
    public void setWorstFraction(double WorstFraction) throws Exception {
        if (WorstFraction < 0 || WorstFraction > 1) 
            throw new Exception("Fraction must be between 0 and 1");
        this.WorstFraction = WorstFraction;
    }

    /**
     * Define what fraction of new entries should be randomly selected.
     * @param RandomFraction Desired fraction
     * @throws Exception If fraction is out of range
     */
    public void setRandomFraction(double RandomFraction) throws Exception {
        if (RandomFraction < 0 || RandomFraction > 1)
            throw new Exception("Fraction must be between 0 and 1");
        this.RandomFraction = RandomFraction;
    }
    
    /**
     * Define the model used to perform the active learning
     * @param model Desired model
     * @throws Exception If model does not contain any submodels
     */
    public void setModel(CompositeRegression model) throws Exception {
        if (model.NModels() == 0)
            throw new Exception("Model contains no submodels");
        Model = model.clone();
    }
    
    @Override protected void checkComponents() throws Exception {
        super.checkComponents(); 
        if (Model == null) throw new Exception("Model not set");
    }
    
    @Override protected Dataset getNewCandidates() {
        // Train a new model on full set
        Dataset Full = getFullDataset(CurrentIteration);
        Model.train(Full);
        
        // Define the list of entries we are searching through
        TreeSet ToSearch_Set = new TreeSet(SearchSpace);
        ToSearch_Set.removeAll(Full.getEntries());
        Dataset ToSearch = InitialData.emptyClone();
        ToSearch.addEntries(ToSearch_Set);
        
        // Get randomly selected entries
        Dataset NewCandidates;
        if (RandomFraction > 0.0)
            NewCandidates = ToSearch.randomSplit((int) (RandomFraction * (double) EntriesPerGeneration));
        else 
            NewCandidates = InitialData.emptyClone();
        
        // Get the full ensemble predictions of the remaining entries
        double[][] EnsemblePredictions = Model.getEnsemblePredictions(ToSearch);
        
        // Add the entries that have the most variation
        if (WorstFraction > 0.0) {
            double[] PredictionVar = Model.getEnsembleVariation(EnsemblePredictions);
            int[] rank = OptimizationHelper.sortAndGetRanks(PredictionVar, true);
            int to_add = (int) (WorstFraction * (double) EntriesPerGeneration);
            for (int i=0; i<to_add; i++)
                NewCandidates.addEntry(ToSearch.getEntry(rank[i]));
            NewCandidates.removeDuplicates();
        }
        
        // Add the best performing entries
        double[] MeanPrediction = Model.getEnsembleMean(EnsemblePredictions);
        ToSearch.setPredictedClasses(MeanPrediction);
        int[] rank = ObjectiveFunction.rankEntries(ToSearch, false);
        int i=0;
        while (NewCandidates.NEntries() < EntriesPerGeneration && i < ToSearch.NEntries()) {
            BaseEntry Entry = ToSearch.getEntry(rank[i]);
            i++;
            if (NewCandidates.containsEntry(Entry))
                continue;
            NewCandidates.addEntry(Entry);
        }
        
        // Done!
        return NewCandidates;
    }

	@Override
	protected void setComponent(List<Object> Command) throws Exception {
		if (Command.isEmpty())
			super.setComponent(Command); 
		String Component = Command.get(0).toString().toLowerCase();
		switch (Component) {
			case "model": {
				// Usage: set model $<model>
				BaseModel NewModel; 
				try { NewModel = (BaseModel) Command.get(1); }
				catch (Exception e) { throw new Exception("Usage: set model $<model>"); }
				if (! (NewModel instanceof CompositeRegression)) {
					throw new Exception("ERROR: Model must implement CompositeRegression");
				}
				CompositeRegression Ptr = (CompositeRegression) NewModel;
				setModel(Ptr);
				System.out.println("\tSet algorithm to use composite of " + Ptr.NModels() + " models");
			} break;
			default:
				super.setComponent(Command);
		}
	}
}
