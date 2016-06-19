package magpie.models.regression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

/**
 * Predicts the class of a {@link CompositionEntry} based on the composition-weighted mean. 
 * Only works on datasets that fulfill {@link CompositionDataset}.
 * 
 * <p>Special features:
 * <ul>
 * <li>Can determine class based on the average of a property, or the average of its inverse (<code>1/p = x_1/p_1 + ...</code>).</li>
 * <li>Can "correct" weighted property using a linear model: <code>class = a + b * meanProperty</code></li>
 * <li>Can fit elemental properties for a certain element</li>
 * </ul>
 * 
 * <usage><p><b>Usage</b>: &lt;property> [-invert] [-correct] [-fit &lt;ElementNames>]
 * <br><pr><i>property</i>: Name of property to use for mixing rule model
 * <br><pr><i>-invert</i>: Whether to compute the 
 * <a href="http://mathworld.wolfram.com/HarmonicMean.html">harmonic mean</a>.
 * <br><pr><i>-correct</i>: Fit linear correction factors to mixing rule equation
 * <br><pr><i>ElementNames</i>: List elements whose elemental property should be fitted</usage>
 * 
 * <p><b><u>Implemented Print Commands:</u></b>
 * 
 * <print><p><b>fitted</b> - Print fitted elemental properties (if any)</print>
 * 
 * @author Logan Ward
 * @version 1.0
 */
public class MixingRuleRegression extends BaseRegression {
    /** Property used for weighting */
    protected String PropertyName = null;
    /** Lookup table for property of interest */
    protected double[] LookupTable;
    /** Whether to compute harmonic mean. */
    protected boolean UseInverse = false; 
    /** 
     * Coefficients used for correction factor. 
     * 0 = Intercept. 1 = Slope.
     */
    protected double[] CorrectionFactors;
    /** Whether to apply correction factors */
    protected boolean UseCorrection = false;
    /** Elements to fit using least squares */
    protected List<String> FittedElements = new LinkedList<>();
    /** Indices of fitted elements */
    private List<Integer> FittedElementIndices = new LinkedList<>();

    @Override
    public BaseRegression clone() {
        MixingRuleRegression x = (MixingRuleRegression) super.clone(); 
        if (CorrectionFactors != null)
            x.CorrectionFactors = CorrectionFactors.clone();
        if (LookupTable != null)
            x.LookupTable = LookupTable.clone();
        x.FittedElements = new LinkedList<>(FittedElements);
        x.FittedElementIndices = new LinkedList<>(FittedElementIndices);
        return x;
    }    

    /**
     * Define which property to use for alloy mean
     * @param PropertyName Name of desired property
     */
    public void setPropertyName(String PropertyName) {
        resetModel();
        this.PropertyName = PropertyName;
    }

    /**
     * Retrieve the property used for alloy mean
     * @return Name of desired property
     */
    public String getPropertyName() {
        return PropertyName;
    }

    /**
     * Define whether to take the average of the property, or the average of the inverse
     * of the property.
     * @param UseInverse Whether to use the inverse
     * @see MixingRuleRegression
     */
    public void setUseInverse(boolean UseInverse) {
        resetModel();
        this.UseInverse = UseInverse;
    }
    
    /**
     * @return Whether the inverse of the target property is averaged
     */
    public boolean getUseInverse() {
        return UseInverse;
    }

    /**
     * Whether to fit a linear model to correct differences between the alloy mean
     *  and class variable
     * @param UseCorrection Whether to use corrections
     */
    public void setUseCorrection(boolean UseCorrection) {
        resetModel();
        this.UseCorrection = UseCorrection;
    }

    @Override
    public void setOptions(List OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            PropertyName = Options[0];
            if (Options.length == 1) return;
            int i=0; 
            while (i + 1 < Options.length) {
                i++;
                switch (Options[i].toLowerCase()) {
                    case "-invert":
                        setUseInverse(true); break;
                    case "-correct":
                        setUseCorrection(true); break;
                    case "-fit":
                        while (i + 1 < Options.length && !Options[i+1].contains("-")) {
                            i++; FittedElements.add(Options[i]);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <Property Name> [-invert] [-correct] [-fit <Element Names>]";
    }

    /**
     * Set which elements to fit.
     * @param elements Elements to be fit to minimize model error. List of element abbreviations. 
     */
    public void setFittedElements(List<String> elements) {
        this.FittedElements = new ArrayList<>(elements);
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        // Check the input data
        if (PropertyName == null)
            throw new Error("Property name not set");
        if (!(TrainData instanceof CompositionDataset))
            throw new Error("Data must be a CompositionDataset");
        CompositionDataset DataPtr = (CompositionDataset) TrainData;
        
        // Get the lookup table
		try {
			LookupTable = DataPtr.getPropertyLookupTable(PropertyName).clone();
		} catch (Exception e) {
			throw new Error("Failed to load property data");
		}
        
        // Invert the property if requested
        if (UseInverse)
            for (int i=0; i<LookupTable.length; i++) LookupTable[i] = 1.0 / LookupTable[i];
        
        // For now, just fit one or the other. I am not sure if it is worth investing the time to do both
        // Idea - Iterative fitting correction constants and elemental properties.
        fitElementProperties(DataPtr);
        if (UseCorrection) {
            if (!FittedElements.isEmpty())
                System.err.println("WARNING: For now, it is a bad idea to fit both elemental properties and correction factors");
            fitCorrectionFactors(DataPtr);
        }
    }

    @Override
    public int getNFittingParameters() {
        return UseCorrection ? FittedElements.size() + 2 : FittedElements.size();
    }

    @Override
    public void run_protected(Dataset TrainData) {
        if (!(TrainData instanceof CompositionDataset))
            throw new Error("Data must be a CompositionDataset");
        CompositionDataset DataPtr = (CompositionDataset) TrainData;
        
        // Get the alloy mean
        double[] alloyMean = new double[DataPtr.NEntries()];
        for (int i=0; i<DataPtr.NEntries(); i++) {
            CompositionEntry e = DataPtr.getEntry(i);
            alloyMean[i] = e.getMean(LookupTable);
            if (UseInverse) alloyMean[i] = 1.0 / alloyMean[i];
        }
        
        // Apply correction factors, if needed
        if (UseCorrection)
            for (int i=0; i<DataPtr.NEntries(); i++)
                alloyMean[i] = CorrectionFactors[0] + alloyMean[i] * CorrectionFactors[1];
        
        // Store results
        DataPtr.setPredictedClasses(alloyMean);
    }
    
    /**
     * Fits properties for elements specified by user. For now, uses OLS regression.
     * @param Data Dataset to use for fitting
     */
    protected void fitElementProperties(CompositionDataset Data) {
        if (FittedElements.isEmpty()) return;
         
        List<String> ElementNames = Arrays.asList(Data.ElementNames);
        FittedElementIndices.clear();
        
        // Get indices of each element
        for (String FittedElement : FittedElements) {
            int index = ElementNames.indexOf(FittedElement);
            if (index == -1) {
                throw new Error(FittedElement + " is not recognized as an element.");
            }
            FittedElementIndices.add(index);
        }
        
        // Mark the property of fitted elements as zero
        double[] originalValues = new double[FittedElementIndices.size()];
        for (int i=0; i < FittedElementIndices.size(); i++) {
            originalValues[i] = LookupTable[FittedElementIndices.get(i)];
            LookupTable[FittedElementIndices.get(i)] = 0;
        }
        
        // Get fitting data
        double[][] sampleXData = new double[Data.NEntries()][FittedElementIndices.size()];
        double[] sampleYData = new double[Data.NEntries()];
        boolean[] hasFittingData = new boolean[FittedElementIndices.size()];
        for (int i=0; i<Data.NEntries(); i++) {
            // X values are the fractions of elements to be fitted
            for (int j=0; j < FittedElementIndices.size(); j++) {
                sampleXData[i][j] = Data.getEntry(i).getElementFraction(FittedElementIndices.get(j));
                if (sampleXData[i][j] > 0) hasFittingData[j] = true;
            }
            
            // Y values are the predicted class - contributions from not-fitted elements
            sampleYData[i] = UseInverse ? 1.0 / Data.getEntry(i).getMeasuredClass()
                    : Data.getEntry(i).getMeasuredClass();
            sampleYData[i] -= Data.getEntry(i).getMean(LookupTable);
        }
        
        // Make sure all elements have fitting data
        boolean goodToGo = true;
        for (int i=0; i<hasFittingData.length; i++)
            if (!hasFittingData[i]) { goodToGo = false; break; }
        if (!goodToGo) { 
            
            // We are missing fitting data for some elements, remove them from list and refit
            List<String> originalList = new LinkedList<>(FittedElements);
            
            // Copy original data back to LookupData
            for (int i=0; i<hasFittingData.length; i++)
                LookupTable[FittedElementIndices.get(i)] = originalValues[i];
            
            // Remove offending elements
            int j=0, i=0;
            while (i < FittedElements.size()) {
                if (!hasFittingData[j]) {
                    System.out.println("Insufficient fitting data for " + FittedElements.get(i) + " using supplied value");
                    FittedElements.remove(i);
                } else i++;
                j++;
            }
            fitElementProperties(Data);
            FittedElements = originalList;
        }
        
        
        // Use OLS regression to fit estimates
        OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
        regression.setNoIntercept(true);
        regression.newSampleData(sampleYData, sampleXData);
        double[] fittedProperties = regression.estimateRegressionParameters();
        
        // Store that data in the lookup table
        for (int i=0; i < fittedProperties.length; i++)
            LookupTable[FittedElementIndices.get(i)] = fittedProperties[i];
    }

    @Override
    protected String printModel_protected() {
        String output = UseInverse ? "1 / Class = " : "Class = ";
        if (UseCorrection)
            output += String.format("%.5e + %.5e * ", CorrectionFactors[0], CorrectionFactors[1]);
        output += "sum{ x_i ";
        if (UseInverse) output += "/ " + PropertyName + "_i";
        else output += "* " + PropertyName + "_i";
        return output + " }\n";
    }    

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        output.add("Property: " + PropertyName);
        
        // Whether a linear correction is being fitted
        if (UseCorrection) {
            output.add("Using linear correction terms.");
        }
        
        // Whether harmonic mean
        if (UseInverse) {
            output.add("Usingharmonic mean.");
        }
        
        // Which elements are being fitted
        if (FittedElements.size() > 0) {
            String temp = "Fitting elements:";
            for (String elem : FittedElements) {
                temp += " " + elem;
            }
            output.add(temp);
        }
        
        return output;
    }

    /**
     * Fit correction factors to the mixing rule model.
     * @param Data Dataset to use for training
     */
    protected void fitCorrectionFactors(CompositionDataset Data) {
        // If desired, fit linear correction factors
        // Get the alloy mean for all entries
        double[] alloyMean = new double[Data.NEntries()];
        for (int i=0; i<Data.NEntries(); i++) {
            CompositionEntry e = Data.getEntry(i);
            alloyMean[i] = e.getMean(LookupTable);
            if (UseInverse) alloyMean[i] = 1.0 / alloyMean[i];
        }
        
        // If required, generate correction factors
        double[] measured = Data.getMeasuredClassArray();
        CorrectionFactors = LASSORegression.linearFit(alloyMean, measured, true);
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
		if (Command.isEmpty()) return super.printCommand(Command);
        String Action = Command.get(0);
        switch (Action.toLowerCase()) {
            case "fitted":
                if (FittedElements.isEmpty())
                    return "Did not fit any elemental properties";
                String output = "Fitted elemental properties:\n";
                for (int i=0; i<FittedElements.size(); i++)
                    output += String.format("\t%s\t%.3f\n", FittedElements.get(i),
                            LookupTable[FittedElementIndices.get(i)]);
                return output;
            default:
                return super.printCommand(Command);
        }
    }    
    
}
