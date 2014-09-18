/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.models.classification;

import java.util.List;
import magpie.analytics.ClassificationStatistics;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.SplitModel;

/**
 * Model that uses several other models to model different parts of a dataset.
 * 
 * <p><b><u>How to Use this Class</u></b>
 * 
 * <p>Use this class in the same way as you would use any {@linkplain SplitModel}.
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class SplitClassifier extends SplitModel implements AbstractClassifier {
    /** Pointer to ClassificationStatistics interface of TrainingStats */
    protected ClassificationStatistics TrainingStatsPtr;
    /** Pointer to ClassificationStatistics interface of ValidationStates*/
    protected ClassificationStatistics ValidationStatsPtr;
    /** Number of classes model can distinguish between */
    protected int NClasses;
	/** Names of classes this model can distinguish between */
	protected String[] ClassNames = null;
    
    /** 
     * Create a blank SplitClassifier
     */
    public SplitClassifier() {
        TrainingStats = new ClassificationStatistics();
        ValidationStats = new ClassificationStatistics();
        TrainingStatsPtr = (ClassificationStatistics) TrainingStats;
        ValidationStatsPtr = (ClassificationStatistics) ValidationStats;
        setClassDiscrete();
    }

    @Override
    public void setOptions(List Options) throws Exception {
        super.setOptions(Options);
    }

    @Override
    public void setModel(int index, BaseModel x) {
        if (! (x instanceof AbstractClassifier)) {
            throw new Error("Model is not a classifier");
        }
        super.setModel(index, x); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setGenericModel(BaseModel x) {
        if (! (x instanceof AbstractClassifier)) {
            throw new Error("Model is not a classifier");
        }
        super.setGenericModel(x); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override public SplitClassifier clone() {
        SplitClassifier x = (SplitClassifier) super.clone();
        x.TrainingStatsPtr = (ClassificationStatistics) x.TrainingStats;
        x.ValidationStatsPtr = (ClassificationStatistics) x.ValidationStats;
        return x;
    }
    
    @Override
    public boolean classIsDiscrete() { return TrainingStatsPtr.classIsDiscrete(); }
    @Override
    final public void setClassDiscrete() { 
        ValidationStatsPtr.setClassDiscrete();
        TrainingStatsPtr.setClassDiscrete();
    }
    @Override
    final public void setClassContinuous() { 
        ValidationStatsPtr.setClassContinuous();
        TrainingStatsPtr.setClassContinuous();
    }
    
    @Override
    public void setClassCutoff(double x) {
        this.ValidationStatsPtr.setClassCutoff(x); 
        this.TrainingStatsPtr.setClassCutoff(x);
    }
    @Override
    public double getClassCutoff() { return ValidationStatsPtr.getClassCutoff(); }
    @Override
    public int getNClasses() { return NClasses; };

    @Override
    protected void train_protected(Dataset TrainingData) {
        NClasses = TrainingData.NClasses();
		ClassNames = TrainingData.getClassNames();
        super.train_protected(TrainingData);
    }

	@Override
	public void run_protected(Dataset Data) {
		if (Data.NClasses() != NClasses) {
			// Force it to have the correct numbers of classes
			Data.setClassNames(ClassNames);
		}
		super.run_protected(Data); 
	}
	
	
}
