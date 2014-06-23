/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.optimization.rankers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyDataset;
import magpie.data.MultiPropertyEntry;
import magpie.user.CommandHandler;
import org.apache.commons.math3.stat.StatUtils;

/**
 * Transforms the values of multiple objectives to a single variable using 
 *  automatically-determined weights. Employs a strategy attributed to 
 *  <a href="http://www.sciencedirect.com/science/article/pii/0270025582900380">Wierzbicki</a>, where
 *  the goal is to minimize the function:
 * 
 *  <center>max<sub><i>i</i></sub>[(f<sub><i>i</i></sub>(x)- z<sub><i>i</i></sub><sup>ideal</sup>) / 
 *     (z<sub><i>i</i></sub><sup>worst</sup> - z<sub><i>i</i></sub><sup>ideal</sup>)]
 *     + p / N * sum[(f<sub><i>i</i></sub>(x)- z<sub><i>i</i></sub><sup>ideal</sup>) / 
 *     (z<sub><i>i</i></sub><sup>worst</sup> - z<sub><i>i</i></sub><sup>ideal</sup>)]</center>
 * 
 * where f is the vector objective function, z<sup>ideal</sup>/z<sup>worst</sup> is the best/worst solutions found
 * so far for a certain objective, p is a tradeoff parameter, and N is the number of objectives.
 * 
 * <usage><p><b>Usage</b>: &lt;p&gt; -obj &lt;maximize|minimize&gt; &lt;property&gt; &lt;ranker name&gt; [&lt;ranker options...&gt;] [-opt &lt;...&gt;]
 * <br><pr><i>p</i>: Trade-off parameter between favoring entries that are best in a single category, and those that are good in many (default=1.0)
 * <br><pr><i>property</i>: Name of property to be optimized using this ranker
 * <br><pr><i>maximize|minimize</i>: Whether the goal is to minimize this objective function
 * <br><pr><i>ranker method</i>: Name of an {@linkplain EntryRanker}. Avoid using another multi-objective ranker
 * <br><pr><i>ranker options</i>: Any options for that entry ranker
 * <br>The "-opt" flag can be used multiple times, and the syntax for each additional flag is identical. Also, this function
 * is designed to be minimized.</usage>
 * 
 * @author Logan Ward
 */
public class AdaptiveScalarizingEntryRanker extends MultiObjectiveEntryRanker {
    /** Trade-off between best in single objective, and generally-good */
    protected double P = 1.0;
    /** Map of property name to objective function */
    protected SortedMap<String,EntryRanker> ObjectiveFunction = new TreeMap<>();
    /** Maximum value of each objective function in training data */
    protected double[] ObjectiveMaximum;
    /** Minimum value of each objective function in training data*/
    protected double[] ObjectiveMinimum;
    /** Index of each property of interest */
    protected int[] PropertyIndex;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() < 2) {
            throw new Exception(printUsage());
        }
        
        try {
            setP(Double.parseDouble(Options.get(0).toString()));
        } catch (NumberFormatException e) {
            throw new Exception(printUsage());
        }
        
        int pos = 1;
        while (pos < Options.size()) {
            String objName;
            String property;
            boolean toMaximize;
            List<Object> objOptions = new LinkedList<>();
            try {
                if (! Options.get(pos++).toString().equalsIgnoreCase("-opt")) {
                    throw new Exception();
                }
                
                objName = Options.get(pos++).toString().toLowerCase();
                if (objName.startsWith("max")) {
                    toMaximize = true;
                } else if (objName.startsWith("min")) {
                    toMaximize = false;
                } else {
                    throw new Exception();
                }
                
                property = Options.get(pos++).toString();
                objName = Options.get(pos++).toString();
                
                while ( pos < Options.size() &&
                        Options.get(pos+1).toString().equalsIgnoreCase("-opt")) {
                    objOptions.add(Options.get(pos++));
                }
            } catch (Exception e) {
                throw new Exception(printUsage());
            }
            
            EntryRanker obj = (EntryRanker) CommandHandler.instantiateClass(
                    "optimization.rankers." + objName, objOptions);
            obj.MaximizeFunction = toMaximize;
            obj.UseMeasured = true;
            addObjectiveFunction(property, obj);
        }
    }

    @Override
    public String printUsage() {
        return "<p> -obj <maximize|minimize> <property> <ranker name> [<ranker options...>] [-opt <...>]";
    }
    
    

    /**
     * Get tradeoff parameter.
     * @return Current tradeoff
     */
    public double getP() {
        return P;
    }
    
    /**
     * Set tradeoff parameter between optimal in single category and decent in all
     * @param P Desired tradeoff (should be positive) [default = 1]
     */
    public void setP(double P) {
        this.P = P;
    } 
    
    /**
     * Clear out list of currently-defined objective functions
     */
    public void clearObjectiveFunctions() {
        ObjectiveFunction.clear();
    }
    
    /**
     * Define a new objective function. Order in which you add these does not matter
     * @param property Name of property to be optimized
     * @param function Objective function for that property
     */
    public void addObjectiveFunction(String property, EntryRanker function) {
        ObjectiveFunction.put(property, function.clone());
    }

    /**
     * Get objective function for a certain property.
     * @param property Name of property
     * @return Objective function (null if not defined)
     */
    public EntryRanker getObjectiveFunction(String property) {
        return ObjectiveFunction.get(property);
    }

    @Override
    public String[] getObjectives() {
        String[] output = new String[ObjectiveFunction.size()];
        int i=0; 
        for (String name : ObjectiveFunction.keySet()) {
            output[i++] = name;
        }
        return output;
    }

    @Override
    public void train(MultiPropertyDataset data) {
        // Initialization stuff
        int originalIndex = data.getTargetPropertyIndex();
        ObjectiveMaximum = new double[ObjectiveFunction.size()];
        ObjectiveMinimum = new double[ObjectiveFunction.size()];
        PropertyIndex = new int[ObjectiveFunction.size()];
        
        // Main work
        int pos = 0;
        for (Map.Entry<String,EntryRanker> pair : ObjectiveFunction.entrySet()) {
            // Set class to a certain property
            String property = pair.getKey();
            PropertyIndex[pos] = data.getPropertyIndex(property);
            data.setTargetProperty(property);
            
            // Get the maximum, minimum objective function for this objective
            EntryRanker obj = pair.getValue();
            ObjectiveMaximum[pos] = -1 * Double.MAX_VALUE;
            ObjectiveMinimum[pos] = Double.MAX_VALUE;
            for (BaseEntry entry : data.getEntries()) {
                double x = obj.objectiveFunction(entry);
                if (ObjectiveMaximum[pos] < x) ObjectiveMaximum[pos] = x;
                if (ObjectiveMinimum[pos] > x) ObjectiveMinimum[pos] = x;
            }
            
            // Increment loop counter
            pos++;
        }
        
        // Return to its original state
        data.setTargetProperty(originalIndex);
    }

    @Override
    public double objectiveFunction(BaseEntry Entry) {
        double[] f = new double[ObjectiveFunction.size()];
        
        // Get the value of each optimization algorithm
        int pos = 0;
        MultiPropertyEntry p = (MultiPropertyEntry) Entry;
        for (EntryRanker obj : ObjectiveFunction.values()) {
            p.setTargetProperty(PropertyIndex[pos]);
            f[pos] = obj.objectiveFunction(Entry);
            f[pos] = obj.MaximizeFunction ? 
                    (ObjectiveMaximum[pos] - f[pos])
                    : (f[pos] - ObjectiveMinimum[pos]);
            f[pos] /= ObjectiveMaximum[pos] - ObjectiveMinimum[pos];
            pos++;
        }
        
        // Compute the function
        return StatUtils.max(f) + P * StatUtils.mean(f);
    }
}
