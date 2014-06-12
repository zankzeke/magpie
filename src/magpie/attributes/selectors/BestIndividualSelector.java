/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.attributes.selectors;

import java.util.List;
import magpie.attributes.evaluators.BaseAttributeEvaluator;
import magpie.data.Dataset;
import magpie.user.CommandHandler;
import org.apache.commons.math3.exception.OutOfRangeException;

/**
 * Selects attributes based on their individual performance. User defines a 
 *  {@linkplain BaseAttributeEvaluator} to rank the power of each individual, and
 *  how many of the top attributes by that metric to select. As this class does not
 *  consider the collective predictive power of a set of attributes, it may select
 *  attributes that correlate with each other. It is recommended that you only 
 *  used this to select a single attribute.
 * 
 * <usage><p><b>Usage</b>: &lt;number> &lt;method> [&lt;options...>]
 * <br><pr><i>number</i>: Number of top-performing attributes to select
 * <br><pr><i>method</i>: Name of a {@linkplain BaseAttributeEvaluator} class used to rank attributes. ("?" to print available options)
 * <br><pr><i>options...</i>: Options for the attribute evaluator (unique to each method)</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see BaseAttributeEvaluator
 */
public class BestIndividualSelector extends BaseAttributeSelector {
    /** Number of attributes to select */
    private int NToSelect;
    /** Evaluator used to assess individual attributes */
    BaseAttributeEvaluator Evaluator;

    @Override
    public void setOptions(List<Object> OptionsObj) throws Exception {
        String[] Options = CommandHandler.convertCommandToString(OptionsObj);
        try {
            NToSelect = Integer.parseInt(Options[0]);
            if (Options[1].equalsIgnoreCase("?")) {
                System.out.println("Available Attribute Evaluators:");
                CommandHandler.printImplmentingClasses(BaseAttributeEvaluator.class, false);
                return;
            }
            String Method = Options[1];
            List<Object> MethodOptions = OptionsObj.subList(2, OptionsObj.size());
            Evaluator = (BaseAttributeEvaluator) CommandHandler.instantiateClass(
                    "attributes.evaluators." + Method, MethodOptions);
        } catch (OutOfRangeException e) {
            throw new Exception(printUsage());
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public String printUsage() {
        return "Usage: <Number to Select> <AttributeEvaluator method> [<Evaluator options>]";
    }
    
    @Override
    protected void train_protected(Dataset Data) {
        if (Evaluator == null) 
            throw new Error("Evaluator not set");
        int[] ranks = Evaluator.getAttributeRanks(Data);
        for (int i=0; i<NToSelect; i++)
            Attribute_ID.add(ranks[i]);
    }
}
