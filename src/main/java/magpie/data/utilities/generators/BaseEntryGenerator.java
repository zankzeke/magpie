
package magpie.data.utilities.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
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
abstract public class BaseEntryGenerator implements Options, Iterable<BaseEntry> {
    
    /**
     * Generate a list of new entries according to the currently-defined settings.
     * @return List of entries
     */
    final public List<BaseEntry> generateEntries() {
        Iterator<BaseEntry> iter = iterator();
        List<BaseEntry> output = new ArrayList<>();
        while (iter.hasNext()) {
            output.add(iter.next());
        }
        return output;
    }
    
    /**
     * Generate entries, and add them new a dataset. Takes care of tedious things
	 * like adding the appropriate number of properties.
     * @param data Dataset to which to add entries
     */
    public void addEntriesToDataset(Dataset data) {
        List<BaseEntry> toAdd = generateEntries();
		if (data instanceof MultiPropertyDataset) {
			MultiPropertyDataset dptr = (MultiPropertyDataset) data;
			for (BaseEntry entry : toAdd) {
				MultiPropertyEntry ptr = (MultiPropertyEntry) entry;
				ptr.setNProperties(dptr.NProperties());
				ptr.setTargetProperty(dptr.getTargetPropertyIndex());
			}
		}
        data.addEntries(toAdd);
    }
}
