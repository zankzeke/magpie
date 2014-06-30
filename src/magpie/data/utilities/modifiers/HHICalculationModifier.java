/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data.utilities.modifiers;

import java.util.List;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

/**
 * Calculates the Herfindahl-Hirschman Index (HHI) for compounds, stores as property.
 *  Uses <a href="http://www.mrl.ucsb.edu:8080/datamine/hhi.jsp">data</a> from 
 *  the Materials Research Laboratory at UCSB, and computes the HHI value as the
 *  minimum HHI<sub>p</sub> of any element present. Smaller HHI<sub>p</sub> values indicate
 *  materials that are more economically-feasible to produce
 * 
 * <p>This property is added to the dataset and all entries it contains. Both the measured
 *  and predicted values of this property are set since it is something that 
 *  is not intended to predicted by a model.</p>
 * 
 * <usage><p><b>Usage</b>: *No options*</usage>
 * 
 * @author Logan Ward
 */
public class HHICalculationModifier extends BaseDatasetModifier {

	@Override
	public void setOptions(List<Object> Options) throws Exception {
		if (Options.size() > 0) {
			throw new Exception(printUsage());
		}
	}

	@Override
	public String printUsage() {
		return "*No options*";
	}
	
	
	@Override
	protected void modifyDataset(Dataset Data) {
		if (! (Data instanceof CompositionDataset)) {
			throw new Error("Data must extend CompositionDataset");
		}
		CompositionDataset p = (CompositionDataset) Data;
		
		// Get the HHI data 
		double[] hhip = p.getPropertyLookupTable("HHIp");
		
		// Add property to dataset
		p.addProperty("HHI");
		
		// Compute and add property to entries
		for (int i=0; i<p.NEntries(); i++) {
			CompositionEntry e = p.getEntry(i);
			double x = e.getMinimum(hhip);
			if (Double.isNaN(x)) {
				x = 1e6;
			}
			e.addProperty(x, x);
		}
	}
	
}
