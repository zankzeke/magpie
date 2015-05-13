
package magpie.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Set;
import magpie.user.CommandHandler;
import org.reflections.Reflections;
import weka.classifiers.AbstractClassifier;

/**
 * Holds static methods useful for Weka-based models/clusterers.
 *
 * @author Logan Ward
 * @verion 0.1
 */
abstract public class WekaUtility {

    /**
     * Instantiate an {@linkplain AbstractClassifier} object from Weka.
     * @param model_type Name of classifiers (i.e. trees.REPTree)
     * @param options Any options for model (can be null)
     * @return A new instance of desired classifier
     * @throws java.lang.Exception
     */
    static public AbstractClassifier instantiateWekaModel(String model_type, 
            String[] options) throws Exception {
        importWekaHome();
        if (model_type.contains("?")) {
            System.out.println("Available Weka Classifiers:");
            System.out.println(printImplmentingClasses(AbstractClassifier.class, true));
            return null;
        }
        if (! model_type.startsWith("weka.classifiers.")) {
            model_type = "weka.classifiers." + model_type;
        }
        AbstractClassifier Model;
        Model = (AbstractClassifier) AbstractClassifier.forName(model_type, options);
        return Model;
    }
    
    /**
     * Load in packages that were installed with the package manager.
     */
    static public void importWekaHome() {
        // Make sure none of this prints
        PrintStream original = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                // Do Nothing
            }
        }));
        
        // Load additional packages
        weka.core.WekaPackageManager.loadPackages(false);
        
        // Reset output
        System.setErr(original);
    }
	
	/**
     * Print the names and options (if applicable) of all classes that are 
     *  subtypes of a certain class.
     * @param cls Superclass of all objects of interest
     * @param printPackage Whether to print the package name as well
     * @return List of names and options of all 
     */
    static public String printImplmentingClasses(Class cls, boolean printPackage) {
        Reflections reflections = new Reflections("weka");
        Set<Class<? extends Object>> allClasses = reflections.getSubTypesOf(cls);
        return CommandHandler.printClassInformation(allClasses, printPackage);
    }
}
