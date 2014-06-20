/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyEntry;
import expr.*;
import java.util.LinkedList;
import magpie.data.MultiPropertyDataset;

/**
 * Use a formula of several properties. Requires that entries are instances of
 *  {@linkplain MultiPropertyEntry}.
 * 
 * <usage><p><b>Usage</b>: $&lt;data template&gt; &lt;formula...&gt;
 * <pr><br><i>data template</i>: Template used to determine whether properties exist in dataset
 * <pr><br><i>formula</i>: Formula to use as objective function. Property names must be surrounded by #{}'s.
 * <br>Example formula: #{volume_pa} * #{bandgap}</usage>
 * 
 * @author Logan Ward
 */
public class PropertyFormulaRanker extends EntryRanker implements MultiobjectiveRanker {
    /** Engine used to evaluate formula */
    private Expr Evaluator = null;
    /** Holds links to the value of variables in the Expr formula */
    final private List<Variable> Variables = new LinkedList<>();
    /** Index of properties used in the formula */
    private int[] PropertyIndex;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String formula;
        MultiPropertyDataset template;
        try {
            template = (MultiPropertyDataset) Options.get(0);
            formula = Options.get(1).toString();
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setFormula(template, formula);
    }
    
    @Override
    public String printUsage() {
        return "Usage: $<data template> <formula...>";
    }

    @Override
    public void train(MultiPropertyDataset data) {
        // Nothing to do
    }
    
    @Override
    public String[] getObjectives() {
        String[] output = new String[Variables.size()];
        for (int i=0; i < Variables.size(); i++) {
            output[i] = Variables.get(i).name();
        }
        return output;
    }
    
    /**
     * Define the formula used by this class. Names of properties used in the 
     *  calculation should be surrounded #{}'s.
     * @param formula Formula to be used
     * @throws Exception If parsing failed or template does not contain 
     */
    public void setFormula(MultiPropertyDataset template, String formula) throws Exception {
        // Find variables in the formula
        formula = extractVariables(formula);
        // Ensure dataset contains those variables
        PropertyIndex = new int[Variables.size()];
        for (int i=0; i < Variables.size(); i++) {
            PropertyIndex[i] = template.getPropertyIndex(Variables.get(i).name());
            if (PropertyIndex[i] == -1) {
                throw new Exception("Dataset does not contain property: " + Variables.get(i).name());
            }
        }
        
        // Generate the expression
        Parser Parser = new Parser();
        // Define the variable set
        for (Variable var : Variables) {
            Parser.allow(var);
        }
        // Parse the expression
        Evaluator = Parser.parseString(formula);
    }
    
    /**
     * Parse a formula to find names of properties used as variables. They should
     *  be surrounded by #{}'s. Stores result internally.
     * @param Formula Formula to be parsed
     * @return Formula in a form parsable by Expr
     */
    protected String extractVariables(String Formula) throws Exception {
        // Prepare 
        Variables.clear();
        List<String> variableNames = new LinkedList<>();
        
        // Parse formula
        int curPos = 0; // Current position of parser
        while (true) {
            // Find the next variable
            int varPos = Formula.indexOf("#{", curPos);
            if (varPos == -1) break; // No more variables
            int varEnd = Formula.indexOf("}", varPos);
            if (varEnd == -1) throw new Exception("Poorly formed expression - Can't find }");
            curPos = varEnd; // Move up the serach
            String varName = Formula.substring(varPos + 2, varEnd);
            variableNames.add(varName);
        }
        
        // Create a list of variables with the following order
        // Also, remove the #{<...>} notation from the formula string
        for (String varName : variableNames) {
            Variable newVariable = Variable.make(varName);
            Formula = Formula.replace("#{" + varName + "}", varName);
            Variables.add(newVariable);
        }
        
        return Formula;
    }
    
    @Override
    public double objectiveFunction(BaseEntry Entry) {
        if (! (Entry instanceof MultiPropertyEntry)) {
            throw new Error("Entry must be a MultiPropertyEntry");
        }
        MultiPropertyEntry e = (MultiPropertyEntry) Entry;
        for (int i=0; i<Variables.size(); i++) {
            double x = UseMeasured ? e.getMeasuredProperty(PropertyIndex[i]) 
                : e.getMeasuredProperty(PropertyIndex[i]);
            Variables.get(i).setValue(x);
        }
        return Evaluator.value();
    }
}