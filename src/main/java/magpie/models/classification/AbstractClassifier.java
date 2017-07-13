package magpie.models.classification;

/**
 * Interface that defines what operations a Classifier must fulfill
 * @author Logan Ward
 */
public interface AbstractClassifier {
    /**
     * @return Number of classes model will distinguish between
     */
    int getNClasses();

    /**
     * Get names of classes this model distinguishes between.
     * @return Names of classes
     */
    String[] getClassNames();
}
