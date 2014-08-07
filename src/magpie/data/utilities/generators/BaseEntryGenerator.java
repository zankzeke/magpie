/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.generators;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.utility.interfaces.Options;

/**
 * Base for classes that generate new entries.
 * 
 * <p><b><u>How to use:</u></b>
 * 
 * <ol>
 * <li>Define options for entry generation as desired
 * <li>Call {@linkplain #generateEntries()} to create a list of new entries
 * <li>Or, add them automatically to a dataset using {@linkplain #generateEntries() }
 * </ol>
 * 
 * <p><b><u>How to implement:</u></b>
 * 
 * <ol>
 * <li>Create a class name that implies what kind of {@linkplain BaseEntry} is generated
 * <li>Define class variables that hold any user options
 * <li>Create operations that allow the user to set those options
 * <li>Implement the {@linkplain Options} interface
 * <li>Implement the {@linkplain #generateEntries() } command, which should take
 * whatever settings the user defined into account
 * </ol>
 * 
 * @author Logan Ward
 */
abstract public class BaseEntryGenerator implements Options {
    
    /**
     * Generate a list of new entries according to the currently-defined settings.
     * @return List of entries
     */
    abstract public List<BaseEntry> generateEntries();
    
    /**
     * Generate entries, and add them new a dataset.
     * @param data Dataset to which to add entries
     */
    public void addEntriesToDataset(Dataset data) {
        List<BaseEntry> toAdd = generateEntries();
        data.addEntries(toAdd);
    }
}
