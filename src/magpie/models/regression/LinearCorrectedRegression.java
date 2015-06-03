package magpie.models.regression;

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
            if (Options.size() != 1) throw new Exception();
            submodel = (BaseModel) Options.get(0);
        } catch (Exception e) {
            throw new Exception(printUsage());
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
            throw new Exception("Model must be a regression model");
        }
        Submodel = submodel.clone();
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        if (Submodel == null) {
            throw new Error("Submodel not defined");
        }
        
        // Train submodel
        Submodel.train(TrainData);
        
        
        
        // Fit linear correction (i.e., error = a + b * predicted)
        
        //    Compute error
        double[] error = TrainData.getMeasuredClassArray();
        double[] predicted = TrainData.getPredictedClassArray();
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
    public String printModelDescription(boolean htmlFormat) {
        String output = getClass().getName();
        
        // Add identation: HTML
        if (htmlFormat) {
            output += "<div style=\"margin: 0 0 0 10\">\n";
        }
        
        // Append submodel description
        output += "Submodel: ";
        String submodel = Submodel.printModelDescription(htmlFormat);
        
        if (htmlFormat) {
            output += submodel;
        } else {
            for (String line : submodel.split("\n")) {
                output += "\t" + line + "\n";
            }
        }
        
        // Remove indentation: HTML
        if (htmlFormat) {
            output += "</div>";
        }
        
        return output;
    }
    
    
}
