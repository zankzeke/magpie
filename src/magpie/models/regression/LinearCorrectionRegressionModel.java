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
public class LinearCorrectionRegressionModel extends BaseRegression {
    /** Model to be corrected */
    private BaseModel Submodel;
    /** Intercept of linear correction */
    private double A;
    /** Slope of correction term */
    private double B;

    @Override
    public BaseRegression clone() {
        LinearCorrectionRegressionModel x = (LinearCorrectionRegressionModel) super.clone(); 
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
        
        // Fit linear correction
        SimpleRegression reg = new SimpleRegression(true);
        for (BaseEntry entry : TrainData.getEntries()) {
            reg.addData(entry.getPredictedClass(), entry.getPredictedClass());
        }
        
        // Save terms
        A = reg.getIntercept();
        B = reg.getSlope();
    }

    @Override
    public void run_protected(Dataset TrainData) {
        // Run submodel
        Submodel.run(TrainData);
        
        // Correct values
        for (BaseEntry entry : TrainData.getEntries()) {
            entry.setMeasuredClass(A + B * entry.getMeasuredClass());
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
}
