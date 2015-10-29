package magpie.models.classification;

import magpie.analytics.ClassificationStatistics;
import magpie.models.BaseModel;
import magpie.data.Dataset;

/**
 * <p>Base class for all classification models. As such, it 
 * provides a constructor and functions to control whether the class variable is
 * treated as discrete or not.
 * <p>This class also includes a "class cutoff" used with continuous class variables, 
 * which is used to convert it to a discrete value. For a predicted class, x, the 
 * discrete is floor(x) if rem(x,1) is less than the cutoff and ceil(x) otherwise.
 * <p>Any models that implements this interface must determine the probability of each class occurring
 * if the class variable is discrete
 *
 * 
 * @author Logan Ward
 * @version 0.1
 */
abstract public class BaseClassifier extends BaseModel implements AbstractClassifier {
    /** Whether the class variable is treated as discrete */
    protected boolean DiscreteClass = true;
    /** Number of classes to distinguish between */
    protected int NClasses = 0;
	/** Names of classes this model can distinguish between */
	protected String[] ClassNames = null;
    
    
    public BaseClassifier() {
        TrainingStats = new ClassificationStatistics();
        ValidationStats = new ClassificationStatistics();
        setClassDiscrete();
    }
    
    @Override public BaseClassifier clone() {
        BaseClassifier x = (BaseClassifier) super.clone();
        x.DiscreteClass = DiscreteClass;
        if (! classIsDiscrete())
            x.setClassCutoff(this.getClassCutoff());
        return x;
    }
    
    // Operations to control whether class variable is treated as discrete
    @Override
    public boolean classIsDiscrete() { 
        return DiscreteClass; 
    }
    
    @Override
    final public void setClassDiscrete() { 
        DiscreteClass = true; 
        ClassificationStatistics Ptr = (ClassificationStatistics) ValidationStats;
        Ptr.setClassDiscrete();
        Ptr = (ClassificationStatistics) TrainingStats;
        Ptr.setClassDiscrete();
    }
    
    @Override
    final public void setClassContinuous() { 
        DiscreteClass = false; 
        ClassificationStatistics Ptr = (ClassificationStatistics) ValidationStats;
        Ptr.setClassContinuous();
        Ptr = (ClassificationStatistics) TrainingStats;
        Ptr.setClassContinuous();
    }
    
    /** Set the class cutoff used when calculating statistics. 
     * @param x Class cutoff (0 <= x <= 1)
     */
    @Override
    public void setClassCutoff(double x) {
        setClassContinuous();
        ClassificationStatistics Ptr = (ClassificationStatistics) ValidationStats;
        Ptr.setClassCutoff(x);
        Ptr = (ClassificationStatistics) TrainingStats;
        Ptr.setClassCutoff(x);
    }
    
    @Override
    public double getClassCutoff() { 
        ClassificationStatistics Ptr = (ClassificationStatistics) ValidationStats;
        return Ptr.getClassCutoff(); 
    }
    
    @Override
    public int getNClasses() { return NClasses; };

    @Override
    public String[] getClassNames() {
        return ClassNames.clone();
    }
    
    @Override public void train(Dataset D, boolean b) {
        if (D.NClasses() == 1)
            throw new Error("ERROR: Dataset must have at least 2 classes");
		NClasses = D.NClasses();
		ClassNames = D.getClassNames();
        super.train(D, b);
    }

	@Override
	public void run(Dataset TestData) {
		if (TestData.NClasses() != NClasses) {
			// Force it to have the correct numbers of classes
			TestData.setClassNames(ClassNames);
		}
		super.run(TestData);
	}
}
