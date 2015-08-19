package magpie.utility.interfaces;

import java.util.List;

/**
 * Dictates commands that must be fulfilled for an object to be "printable".
 * 
 * <p>Ensure that you add any new printing commands to the Javadoc for the class.
 * Surround them by &lt;print> HTML tags and follow the same format as any other command 
 * (see {@linkplain magpie.utility.interfaces.Commandable} for more details)
 * 
 * @author Logan Ward
 * @version 0.1
 */
public interface Printable {
    
    /**
     * Prints a simple status message about this object
     * @return Short status message
     */
    abstract public String about();
    
    /**
     * Handles more complicated printing commands. 
     * 
     * @param Command Command specifying what to print
     * @return String formatted as requested
     * @throws Exception If command not understood
     */
    abstract public String printCommand(List<String> Command) throws Exception;

    /**
     * Print full name of object, and a simple description of the options. This
     * description should print enough information to reproduce how this 
     * object was created, but not necessarily enough to run it (e.g., coefficients
     * for the model). 
     *
     * <p>Example: For a model training a separate WekaRegression for intermetallics
     * <p>magpie.models.regression.SplitRegression
     * <div style="margin: 0 0 0 25px">
     * Splitter: AllMetalsSplitter
     * <br>All Metals: magpie.models.regression.WekaRegression trees.REPTree
     * <br>Contains Nonmetal: magpie.regression.LASSORegreession -maxterms 2
     * </div>
     *
     * @param htmlFormat Whether format for output to an HTML page
     * (e.g., &lt;div&gt; to create indentation) or for printing to screen.
     * @return String describing the model
     * @see #printModel()
     */
    String printDescription(boolean htmlFormat);
}
