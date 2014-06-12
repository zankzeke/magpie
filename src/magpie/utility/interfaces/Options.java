/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.utility.interfaces;

import java.util.List;

/**
 * This interface signals that the options of a class can be 
 *  set by a properly-parsed string. This is a simple version of the one
 *  included in weka.core: {@link weka.core.OptionHandler}.
 * 
 * 
 * @author Logan Ward
 * @version 0.1
 */
public interface Options {
    /**
     * Set any options for this object. If parsing the options fails, return an 
     *  Exception with the usage as its message.
     * @param Options Array of options as Objects - can be <code>null</code>
     * @throws Exception if problem with inputs
     */
    abstract public void setOptions(List<Object> Options) throws Exception;     
    
    /**
     * Print out required format for options. For consistency, should start with
     *  "Usage: ".
     * @return Usage for this function
     */
    abstract public String printUsage();
}
