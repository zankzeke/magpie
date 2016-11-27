
package magpie.utility;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import magpie.user.CommandHandler;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import weka.classifiers.AbstractClassifier;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;

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
        if (! model_type.startsWith("weka.classifiers.")) {
            model_type = "weka.classifiers." + model_type;
        }
        AbstractClassifier Model;
        Model = (AbstractClassifier) Class.forName(model_type).newInstance();
        if (Model instanceof OptionHandler) {
            Model.setOptions(options);
        }
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
     * Convert a Weka TechnicalInformation object to a Magpie Citation object.
     * @param techInfo Technical information from Weka class
     * @param component Component this citation is associated with
     * @return Citation object containing information from Weka technical info
     */
    public static Citation convertToCitation(TechnicalInformation techInfo, 
            Class component) {
        // Check if the URL is known
        String url = techInfo.getValue(TechnicalInformation.Field.URL);
        if (url.length() <= 1) {
            url = null;
        }
        
        // Check if any note is given
        String note = techInfo.getValue(TechnicalInformation.Field.NOTE);
        if (note.length() <= 1) {
            note = null;
        }
        
        // Generate citation
        Citation cite = new Citation(component,
                techInfo.getType().getComment(),
                new String[]{techInfo.getValue(TechnicalInformation.Field.AUTHOR)}, 
                techInfo.getValue(TechnicalInformation.Field.TITLE),
                url,
                note);
        
        return cite;
    }

    /**
     * Generate citations from a Weka object
     * @param wekaClass Class to be checked
     * @param component Magpie component which these citations are for
     * @return List of citations
     */
    public static List<Pair<String, Citation>> getWekaObjectCitations(Object wekaClass, Class component) {
        // Intiailize output
        List<Pair<String, Citation>> wekaCitations = new ArrayList<>();
        
        // Citation for using Weka
        wekaCitations.add(new ImmutablePair<>("Using Weka",
                new Citation(component, "Article", 
                new String[]{"M. Hall", "et al."},
                "The WEKA data mining software",
                "http://portal.acm.org/citation.cfm?doid=1656274.1656278",
                null)));
        
        // Check if it has technical info
        if (wekaClass instanceof TechnicalInformationHandler) {
            // Get the interface
            TechnicalInformationHandler intf = (TechnicalInformationHandler) wekaClass;
            TechnicalInformation techInfo = intf.getTechnicalInformation();
            
            // Convert main citation
            Citation cite = WekaUtility.convertToCitation(techInfo, component);
            wekaCitations.add(new ImmutablePair<>("For using " + wekaClass.getClass().getCanonicalName(), cite));
            
            // Convert any additioanl citatations
            if (techInfo.hasAdditional()) {
                for (TechnicalInformation addTechInfo : Collections.list(techInfo.additional())) {
                    cite = WekaUtility.convertToCitation(addTechInfo, component);
                    wekaCitations.add(new ImmutablePair<>("For using " + wekaClass.getClass().getCanonicalName(), cite));
                }
            }
        }
        
        return wekaCitations;
    }
}
