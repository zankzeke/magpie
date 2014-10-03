/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.models.regression;

import java.util.List;
import magpie.optimization.rankers.MultiObjectiveEntryRanker;
import magpie.optimization.rankers.PropertyFormulaRanker;

/**
 * Extension of {@linkplain MultiObjectiveEntryRanker} where one does not care 
 *  about the class variable.
 * 
 * <usage><p><b>Usage</b>: &lt;properties...&gt;
 * <br><pr><i>properties</i>: Properties to be modeled</usage>
 * 
 * @author Logan Ward
 */
public class MultiPropertyRegression extends MultiObjectiveRegression {

	@Override
	public void setOptions(List<Object> Options) throws Exception {
		if (Options.isEmpty()) {
			throw new Exception(printUsage());
		}
		String formula = "#{" + Options.get(0) + "}";
		for (Object prop : Options.subList(1, Options.size())) {
			formula += " + #{" + prop.toString() + "}";
		}
		PropertyFormulaRanker ranker = new PropertyFormulaRanker();
		ranker.setFormula(formula);
		setObjectiveFunction(ranker);
	}

	@Override
	public String printUsage() {
		return "Usage: <properties...>";
	}
	
}
