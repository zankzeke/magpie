/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.modifiers;

import magpie.data.MultiPropertyDataset;
import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyEntry;

/**
 * Adds properties to a {@linkplain MultiPropertyDataset}.
 * 
 * <usage><p><b>Usage</b>: &lt;properties to add&gt;
 * <br><pr><i>properties</i>: List of properties to add to dataset</usage>
 * @author Logan Ward
 */
public class AddPropertyModifier extends BaseDatasetModifier {
	/** List of properties to be added */
	private List<String> ToAdd = null;

	@Override
	public void setOptions(List<Object> Options) throws Exception {
		List<String> props = new LinkedList<>();
		for (Object temp: Options) {
			props.add(temp.toString());
		}
		setPropertiesToAdd(props);
	}

	@Override
	public String printUsage() {
		return "Usage: <property names..>";
	}
	
	

	/**
	 * Define list of properties to add to a dataset.
	 * @param ToAdd List of property names
	 */
	public void setPropertiesToAdd(List<String> ToAdd) {
		this.ToAdd = ToAdd;
	}

	/**
	 * Retrieve list of properties that would be added to a dataset.
	 * @return List of property names
	 */
	public List<String> getPropertiesToAdd() {
		return ToAdd;
	}

	@Override
	protected void modifyDataset(Dataset Data) {
		if (! (Data instanceof MultiPropertyDataset)) {
			throw new Error("Data must implement MultiPropertyDataset");
		}
		
		MultiPropertyDataset ptr = (MultiPropertyDataset) Data;
		// Add properties
		for (String prop : ToAdd) {
			ptr.addProperty(prop);
			for (BaseEntry entry : ptr.getEntries()) {
				MultiPropertyEntry eptr = (MultiPropertyEntry) entry;
				eptr.addProperty();
			}
		}
		
	}
}
