package magpie.models.classification;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.SplitModel;
import magpie.statistics.performance.ClassificationStatistics;

import java.util.List;

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
    }

    @Override
    public void setOptions(List Options) throws Exception {
        super.setOptions(Options);
    }

    @Override
    public void setModel(int index, BaseModel x) {
        if (! (x instanceof AbstractClassifier)) {
            throw new IllegalArgumentException("Model is not a classifier");
        }
        super.setModel(index, x); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setGenericModel(BaseModel x) {
        if (! (x instanceof AbstractClassifier)) {
            throw new IllegalArgumentException("Model is not a classifier");
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
    public int getNClasses() {
        return NClasses;
    }

    @Override
    public String[] getClassNames() {
        return ClassNames.clone();
    }

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
