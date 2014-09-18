/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.Dataset;

/**
 * Sets the class names of a Dataset.
 * 
 * <usage><p><b>Usage</b>: [&lt;names...&gt;]
 * <br><pr><i>names</i>: New names of class (same number as are currently in dataset).</usage>
 * 
 * @author Logan Ward
 */
public class SetClassNamesModifier extends BaseDatasetModifier {
	/** New class names */
	private String[] ClassNames = null;

	@Override
	public void setOptions(List<Object> Options) throws Exception {
		if (Options.isEmpty()) throw new Exception(printUsage());
		
		String[] temp = new String[Options.size()];
		for (int i=0; i < Options.size(); i++) {
			temp[i] = Options.get(i).toString();
		}
		setClassNames(temp);
	}

	@Override
	public String printUsage() {
		return "Usage: <names...>";
	}
	
	/**
	 * Define the new class names
	 * @param ClassNames Desired new class names
	 */
	public void setClassNames(String[] ClassNames) {
		this.ClassNames = ClassNames.clone();
	}

	@Override
	protected void modifyDataset(Dataset Data) {
		if (Data.NClasses() != ClassNames.length) {
			throw new Error("Improper number of class names");
		}
		Data.setClassNames(ClassNames);
	}
	
}
