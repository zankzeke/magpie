/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
     
     * 
     * @param Command Command specifying what to print
     * @return String formatted as requested
     * @throws Exception If command not understood
     */
    abstract public String printCommand(List<String> Command) throws Exception;
}
