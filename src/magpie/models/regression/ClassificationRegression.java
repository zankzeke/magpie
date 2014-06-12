/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.models.regression;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.utilities.modifiers.PartitionToClassModifier;
import magpie.data.utilities.splitters.ObjectiveFunctionSplitter;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.models.classification.WekaClassifier;
import magpie.optimization.algorithms.OptimizationHelper;
import magpie.optimization.rankers.EntryRanker;
import magpie.optimization.rankers.SimpleEntryRanker;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Use a classification algorithm to perform regression. This method works by spliting
 *  all training examples into two sets: one where the class variable is
 *  past a threshold and one where it is not. This threshold is set by specifying
 *  the objective algorithm (with an {@linkplain EntryRanker}) and the treshold on which
 *  to split the data.
 * 
 * <p>Regression is performed by predicting that an entry 100% probability of being 
 * past the threshold to have a class variable equal to that of the training entry 
 * farthest past the threshold, 0% probability being equivalent to the entry farthest
 * before the threshold, and all others linearly interpolated between those two.
 * 
 * <usage><p><b>Usage</b>: $&lt;classifier> &lt;threshold> &lt;objective function> [&lt;o.f. options...>]
 * <br><pr><i>classifier</i>: {@linkplain BaseModel} that fills {@linkplain AbstractClassifier}, used to make predictions
 * <br><pr><i>threshold</i>: Desired threshold for the objective function value
 * <br><pr><i>objective function</i>: {@linkplain EntryRanker} used to rank the entries based on their class variable.
 * ("?" to list all available options)
 * <br><pr><i>o.f. options</i>: Any options for the objective function</usage>
 * @author Logan Ward
 */
public class ClassificationRegression extends BaseRegression {
    /** Classification algorithm that this class wraps around */
    private BaseModel Clfr = new WekaClassifier();
    /** Entry ranker used to order entries */
    private EntryRanker ObjFunction = new SimpleEntryRanker();
    /** Threshold on which to split data into classes */
    private double Threshold = 0.0;
    /** Minimum value of objective function of all entries in training set*/
    private double MinObjFun = 0.0;
    /** Range between minimum and maximum value of objective function 
     * of entries in the training set.
     */
    private double ObjFunRange = 1.0;

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public void setOptions(List<Object> Options) throws Exception {
        BaseModel NewClfr;
        String OFMethod;
        List<Object> OFOptions;
        try {
            NewClfr = (BaseModel) Options.get(0);
            setThreshold(Double.valueOf(Options.get(1).toString()));
            OFMethod = Options.get(2).toString();
            if (OFMethod.equalsIgnoreCase("?")) {
                System.out.println(CommandHandler.printImplmentingClasses(EntryRanker.class, false));
                return;
            }
            OFOptions = Options.subList(3, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        // Set classifier
        setClassifier(NewClfr);
        // Set objective function
        EntryRanker objFun = (EntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + OFMethod, OFOptions);
        setObjectiveFunction(objFun);
    }

    @Override
    public String printUsage() {
        return "Usage: $<classifier> <split threshold> <objective function> [<o.f. options...>]";
    }

    /**
     * Define the objective function used to order entries
     * @param ObjFunction Desired objective function
     */
    public void setObjectiveFunction(EntryRanker ObjFunction) {
        this.ObjFunction = ObjFunction;
    }

    /**
     * Classifier behind this regression algorithm
     * @param Clfr Untrained regression model
     * @throws Exception
     */
    public void setClassifier(BaseModel Clfr) throws Exception {
        if (!(Clfr instanceof AbstractClassifier)) {
            throw new Exception("Model must implement AbstractClassifier");
        }
        this.Clfr = Clfr.clone();
    }

    /**
     * Define threshold of objective function on which to split entries. Vales below 
     * this value will constitute one class, all others will constitute another.
     * @param Threshold 
     */
    public void setThreshold(double Threshold) {
        this.Threshold = Threshold;
    }    

    @Override
    protected void train_protected(Dataset TrainData) {
        // First, change dataset to allow it to be used by a classifier
        Dataset splitData = TrainData.clone();
        getModifier().transform(splitData);
        // Get the maximum/minimum objective function
        double[] objFunEval = ObjFunction.runObjectiveFunction(TrainData);
        int[] ranks = OptimizationHelper.sortAndGetRanks(objFunEval, false);
        MinObjFun = TrainData.getEntry(ranks[0]).getMeasuredClass();
        ObjFunRange = TrainData.getEntry(ranks[ranks.length-1]).getMeasuredClass() - MinObjFun;
        // Train the classifier on this data
        Clfr.train(splitData);
    }

    @Override
    public void run_protected(Dataset TrainData) {
        // First, change dataset to allow it to be used by a classifier
        Dataset splitData = TrainData.clone();
        getModifier().transform(splitData);
        // Run the classifier
        Clfr.run(splitData);
        // For each entry, map the probability of it being in class #0 to the regressed class variable
        for (int i=0; i<splitData.NEntries(); i++) {
            double prob = splitData.getEntry(i).getClassProbilities()[1];
            double x = MinObjFun + prob * ObjFunRange;
            TrainData.getEntry(i).setPredictedClass(x);
        }
    }
    
    /**
     * Get a {@linkplain PartitionToClassModifier} that converts a dataset into to
     *  a dataset with two classes: one with objective function below threshold and
     *  one with objective function above.
     * @return 
     */
    private PartitionToClassModifier getModifier() {
        PartitionToClassModifier mdfr = new PartitionToClassModifier();
        ObjectiveFunctionSplitter splt = new ObjectiveFunctionSplitter();
        splt.setSplitAbove(false);
        splt.setObjectiveFunction(ObjFunction);
        splt.setThreshold(Threshold);
        mdfr.setSplitter(splt);
        return mdfr;
    }

    @Override
    protected String printModel_protected() {
        String output = "Ranking Method: " + ObjFunction.getClass().getSimpleName();
        output += String.format("\nThreshold: %.3e", Threshold);
        output += "\nClassifier:\n" + Clfr.printModel();
        return output;
    }

    @Override
    public int getNFittingParameters() {
        return 0;
    }
}
