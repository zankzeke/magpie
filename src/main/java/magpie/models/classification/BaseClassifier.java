package magpie.models.classification;

import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.statistics.performance.ClassificationStatistics;

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
    /** Number of classes to distinguish between */
    protected int NClasses = 0;
	/** Names of classes this model can distinguish between */
	protected String[] ClassNames = null;
    
    
    public BaseClassifier() {
        TrainingStats = new ClassificationStatistics();
        ValidationStats = new ClassificationStatistics();
    }
    
    @Override public BaseClassifier clone() {
        BaseClassifier x = (BaseClassifier) super.clone();
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
    public void train(Dataset D, boolean b) {
        if (D.NClasses() == 1)
            throw new IllegalArgumentException("Dataset must have at least 2 classes");
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
