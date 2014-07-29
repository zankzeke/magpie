/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.models.BaseModel;

/**
 * Subtract the predicted class from a dataset, leaving only the error signal as the 
 *  measured class variable. Only works with regression models.
 * 
 * <usage><p><b>Usage</b>: $&lt;model&gt; &lt;absolute|relative&gt;
 * <br><pr><i>model</i>: Model to use to generate error signal
 * <br><pr><i>absolute|relative</i>: Whether to use absolute or relative error
 * </usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class ErrorSignalModifier extends BaseDatasetModifier {
    /** Model used to make class predictions */
    private BaseModel Model = null;
    /** Whether to calculate absolute error */
    private boolean ComputeAbsolute = true;

    @Override
    public void setOptions(List Options) throws Exception {
        try {
            setModel((BaseModel) Options.get(0));
            String word = Options.get(1).toString().toLowerCase();
            if (word.startsWith("abs")) {
                useAbsoluteError();
            } else if (word.startsWith("rel")) {
				useRelativeError();
			} else {
				throw new Exception();
			}
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<model> <absolute|relative>";
    }
    
    /**
     * Set filter to compute absolute error.
     */
    public void useAbsoluteError() {
        ComputeAbsolute = true;
    }
    
    /**
     * Set filter to computer relative error.
     */
    public void useRelativeError() {
        ComputeAbsolute = false;
    }

    /**
     * Set the model used to make predictions. Must be trained.
     * @param Model Link to a model (will be cloned)
     */
    public void setModel(BaseModel Model) {
        if (! Model.isTrained()) throw new Error("Model not trained");
        this.Model = Model.clone();
    }

    @Override
    protected void modifyDataset(Dataset Data) {
        if(Model == null) throw new Error("No model supplied");
        
        if (Data instanceof MultiPropertyDataset) {
            MultiPropertyDataset Ptr = (MultiPropertyDataset) Data;
            if (Ptr.usingPropertyAsClass()) {
                // If necessary, add new property
                Ptr.addProperty("ErrorSignal");
                for (int i=0; i<Ptr.NEntries(); i++)
                    Ptr.getEntry(i).addProperty(Double.NaN);
                Ptr.setTargetProperty("ErrorSignal", true);
            }
        }

        // Calculate the error signal
        double[] measured = Data.getMeasuredClassArray();
        Model.run(Data);
        double[] predicted = Data.getPredictedClassArray();
        Data.setPredictedClasses(new double[Data.NEntries()]); // Sets predicted class to all 0's
		double[] error = new double[measured.length];
        for (int i=0; i<measured.length; i++) {
            error[i] = measured[i] - predicted[i];
			if (! ComputeAbsolute) {
				error[i] /= measured[i];
			}
		}
        Data.setMeasuredClasses(error);
    }

}
