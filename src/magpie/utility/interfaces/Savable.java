/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.utility.interfaces;

/**
 * Dictates commands that must be fulfilled for an object to be "savable".
 * @author Logan Ward
 * @version 0.1
 */
public interface Savable {
    
    /**
     * Handles complicated saving commands. Assumes that Command[0] is the 
     *  name of this object.
     * @param Basename Name of file without extension
     * @param Command Command specifying what to print
     * @return Filename
     * @throws Exception If command not understood
     */
    abstract public String saveCommand(String Basename, String Command) throws Exception;
}
