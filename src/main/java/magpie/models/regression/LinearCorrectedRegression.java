package magpie.models.regression;

import java.util.Arrays;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Apply a linear correction to results from another model. Let f(x) be the 
 * submodel. This model finds g(x) = a + b * f(x), such that (y - g(x))^2 is minimized.
 * 
 * <usage><p><b>Usage</b>: $&lt;submodel&gt;
 * <br><pr><i>submodel</i>: Model to be corrected</usage>
 * 
 * @author Logan Ward
 */
public class LinearCorrectedRegression extends BaseRegression {
    /** Model to be corrected */
    private BaseModel Submodel;
    /** Intercept of linear correction */
    private double A;
    /** Slope of correction term */
    private double B;

    @Override
    public BaseRegression clone() {
        LinearCorrectedRegression x = (LinearCorrectedRegression) super.clone(); 
        x.Submodel = Submodel.clone();
        return x;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        BaseModel submodel;
        try {
            if (Options.size() != 1) throw new IllegalArgumentException();
            submodel = (BaseModel) Options.get(0);
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        setSubmodel(submodel);
    }

    @Override
    public String printUsage() {
        return "Usage: $<submodel>";
    }
    
    /**
     * Define the model that this class corrects.
     * @param submodel Another regression model
     * @throws java.lang.Exception If model isn't a regression model
     */
    public void setSubmodel(BaseModel submodel) throws Exception {
        if (! (submodel instanceof AbstractRegressionModel)) {
            throw new IllegalArgumentException("Model must be a regression model");
        }
        Submodel = submodel.clone();
    }

    @Override
    protected void train_protected(Dataset trainData) {
        if (Submodel == null) {
            throw new Error("Submodel not defined");
        }
        
        // Make clone of TrainData to ensure predicted class variables are not affected
        Dataset localCopy = trainData.clone();
        
        // Train submodel (run it as well)
        Submodel.train(localCopy, true);
        
        // Fit linear correction (i.e., error = a + b * predicted)
        
        //    Compute error
        double[] error = localCopy.getMeasuredClassArray();
        double[] predicted = localCopy.getPredictedClassArray();
        for (int i=0; i<error.length; i++) {
            error[i] -= predicted[i];
        }
        
        //    Perform linear regression to minimize error
        SimpleRegression reg = new SimpleRegression();
        for (int i=0; i<error.length; i++) {
            reg.addData(predicted[i], error[i]);
        }
        
        // Store correction terms
        A = reg.getIntercept();
        B = reg.getSlope();
    }

    @Override
    public void run_protected(Dataset TrainData) {
        // Run submodel
        Submodel.run(TrainData);
        
        // Correct values
        for (BaseEntry entry : TrainData.getEntries()) {
            double subPrediction = entry.getPredictedClass();
            entry.setPredictedClass(subPrediction + subPrediction * B + A);
        }
    }

    @Override
    public int getNFittingParameters() {
        AbstractRegressionModel ptr = (AbstractRegressionModel) Submodel;
        return 2 + ptr.getNFittingParameters();
    }
    
    @Override
    protected String printModel_protected() {
        String output = "Correction: \n";
        output += String.format("%.4e + %.4e * f(x)\n", A, B);
        output += "\nf(x):\n";
        return output + Submodel.printModel();
    }

    @Override
    public List<String> printModelDescriptionDetails(boolean htmlFormat) {
        List<String> output = super.printModelDescriptionDetails(htmlFormat);
        
        // Get details of submodel
        String[] submodel = Submodel.printDescription(htmlFormat).split("\n");
        submodel[0] = "Model being corrected: " + submodel[0];
        
        output.addAll(Arrays.asList(submodel));
        
        return output;
    }
        
}
