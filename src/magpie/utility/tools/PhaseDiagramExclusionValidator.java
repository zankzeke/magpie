package magpie.utility.tools;

import java.io.FileWriter;
import java.util.*;
import magpie.analytics.BaseStatistics;
import magpie.analytics.ClassificationStatistics;
import magpie.analytics.RegressionStatistics;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import magpie.data.utilities.filters.PhaseDiagramExclusionFilter;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;
import magpie.utility.interfaces.Savable;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.Combinations;

/**
 * Validate a model by using data each phase diagram as test set. 
 * 
 * <p>Provided a number of elements, iterates through all possible combinations of 
 * that number elements (i.e., all unique phase diagrams) and evaluates the predictive 
 * accuracy of a model by excluding all elements that contain all of those
 * elements from the dataset, training a model on the remaining entries, and
 * validates the performance of the model on the remaining data.
 * 
 * <p>Uses the {@linkplain PhaseDiagramExclusionFilter} to create training and test
 * sets.
 * 
 * <p><b><u>Implemented Commands</u></b>
 * 
 * <command><p><b>results = evaluate $&lt;model&gt; $&gt;test data&gt; - Evaluate
 * the performance of a model by iteratively using entries from different
 * phase diagrams as the test test.
 * <br><pr><i>model</i>: Model to be tested
 * <br><pr><i>test data</i>: Data to use when testing the model
 * <br><pr><i>results</i>: Output: Dataset containing results from test
 * </command>
 *
 * <p><b><u>Implemented Save Commands</u></b>
 * 
 * <save><p><b>human</b> - Print out results for each phase diagram from last
 * evaluation in a human-readable format.</save>
 * 
 * <p><b><u>Implemented Print Commands</u></b>
 * 
 * <print><p><b>last [&lt;command>]</b> - Print out statistics generated during last model evaluation
 * <br><pr><i>command</i>: Command to be passed to internal {@linkplain BaseStatistics} object.</print>
 * 
 * <print><p><b>nsystems</b> - Print number of systems evaluated in last test</print>
 * 
 * <usage><p><b>Usage</b>: &lt;# elements&gt;
 * <br><pr><i># elements</i>: Number of elements in each phase diagram
 * </usage>
 * @author Logan Ward
 */
public class PhaseDiagramExclusionValidator implements Options, Commandable,
        Savable, Printable {
    /** 
     * Number of elements to use. Default = 2 
     */
    protected int NElements = 2;
    /** 
     * Results from previous test. Key is the validation results, value is the
     * performance when those elements were used as the test set
     */
    final protected List<Pair<int[], BaseStatistics>> LastResults = new ArrayList<>();
    /**
     * Statistics from last test
     */
    protected BaseStatistics LastStatistics;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int nelem;
        try {
            if (Options.size() != 1) {
                throw new Exception();
            }
            nelem = Integer.parseInt(Options.get(0).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        setNElements(nelem);
    }

    @Override
    public String printUsage() {
        return "Usage: <# elements>";
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        
        String action = Command.get(0).toString().toLowerCase();
        switch(action) {
            case "evaluate": {
                
                // Parse input
                BaseModel model;
                Dataset data;
                try {
                    model = (BaseModel) Command.get(1);
                    data = (Dataset) Command.get(2);
                    if (Command.size() > 3) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: <results> = evaluate $<model> $<data>");
                }
                
                // Check that dataset is a CompositionDataset
                if (! (data instanceof CompositionDataset)) {
                    throw new Exception("Dataset must implement CompositionDataset");
                }
                Dataset results = evaluateModel(model, (CompositionDataset) data);
                
                // Print status message and return results
                System.out.println("\tEvalauted performance of model in " + 
                        LastResults.size() + " different phase diagrams");
                return results;
            }
            default:
                throw new Exception("Command not recognized: " + action);
        }
    }

    /** 
     * Set the number of elements in phase diagrams being iterated through
     * @param nElements Desired size of phase diagram
     */
    public void setNElements(int nElements) {
        this.NElements = nElements;
    }
    
    /**
     * Evaluate predictive accuracy of model by iteratively excluding elements
     * from a certain phase diagram. 
     * 
     * @param model Model to be validated
     * @param data Data to use for validation
     * @return Dataset containing properties predicted during validation
     */
    public CompositionDataset evaluateModel(BaseModel model, CompositionDataset data) {
        // Clear out old results
        LastResults.clear();
        
        // Gather list of all elements in the dataset
        List<Integer> allElements = getElements(data);
        
        // Create output array
        CompositionDataset output = data.emptyClone();
        
        // Clone model (so as not interfere with statistics)
        BaseModel modelToRun = model.clone();
        
        // Loop through all combinations of these elements
        for (int[] combination : new Combinations(allElements.size(), NElements)) {
            // Get the corresponding elements
            int[] elemsToExclude = new int[NElements];
            for (int i=0; i<elemsToExclude.length; i++) {
                elemsToExclude[i] = allElements.get(combination[i]);
            }
            
            // Run the model for those elements
            Dataset results = runExclusionTest(elemsToExclude, modelToRun, data);
            
            // If results are null (no entries in this system), continue
            if (results == null) {
                continue;
            }
            
            // Add entries to output
            output.addEntries(results.getEntries());
            
            // Clear attributes from output
            output.clearAttributes();
            
            // Evaluate statistics
            BaseStatistics stats = model instanceof AbstractRegressionModel ? 
                    new RegressionStatistics() : new ClassificationStatistics();
            stats.evaluate(results);
            
            // Store results
            LastResults.add(new ImmutablePair<>(elemsToExclude, stats));
        }
        
        // Store overall results
        LastStatistics = model instanceof AbstractRegressionModel ? 
                    new RegressionStatistics() : new ClassificationStatistics();
        LastStatistics.evaluate(output);
        
        return output;
    }
    
    /**
     * Print last results as formatted text. Style
     * @return Last results in a human-readable format
     */
    public String printLastResults() {
        String output = "";
        
        // Loop through each system
        for (Pair<int[],BaseStatistics> system : LastResults) {
            // Print out system names
            String[] elems = getElementNames(system.getKey());
            String elemCombined = elems[0];
            for (int i=1; i<elems.length; i++) {
                elemCombined += "-" + elems[i];
            }
            output += "System: " + elemCombined + "\n";
            
            // Print out stats
            String stats = system.getValue().toString();
            for (String line : stats.split("\n")) {
                output += "\t" + line + "\n";
            }
            
            // Add a line break
            output += "\n";
        }
        
        return output;
    }

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        // Make filename
        String filename = Basename;
        switch (Format.toLowerCase()) {
            case "human": filename += ".results"; break;
        }
        
        try (FileWriter fp = new FileWriter(filename)) 
        {
            // Write the files
            switch (Format.toLowerCase()) {
                case "human": {
                    fp.write(printLastResults());
                    break;
                } default:
            }
        }
        
        // Close and return
        return filename;
    }

    @Override
    public String about() {
        return this.getClass().getSimpleName() + " : "
                    + "Excluding " + NElements + " elements at a time";
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        // If command is empty, print about
        if (Command.isEmpty()) {
            return about();
        }
        
        // Print statistics
        String action = Command.get(0).toLowerCase();
        switch (action) {
            case "last": {
                // Pass on to statistics object
                return LastStatistics.printCommand(Command.subList(1, Command.size()));
            }
            case "nsystems": {
                return "Evaluated " + LastResults.size() + " systems with " 
                        + NElements + " elements"; 
            }
            default:
                throw new Exception("Print command not supported:" + action);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "Excluding " + NElements + " elements at a time";
        
        return output;
    }
    
    /**
     * Provided with a list of elements IDs, print out element names.
     * @param names Element IDs
     * @return Element names
     */
    protected static String[] getElementNames(int[] names) {
        String[] output = new String[names.length];
        for (int i=0; i<names.length; i++) {
            output[i] = LookupData.ElementNames[names[i]];
        }
        return output;
    }
    
    /**
     * Evaluate model by using excluding certain combinations of 
     * elements from a dataset, and using the excluded data as a test set.
     * @param elemsToExclude List of elements (by ID = Z - 1) defining
     * @param modelToTest Model to be bested
     * @param data Data to use as test data
     * @return Predictions from test sets
     */
    static public Dataset runExclusionTest(int[] elemsToExclude,
            BaseModel modelToTest, CompositionDataset data) {
        // Get elements as string
        String[] elems = new String[elemsToExclude.length];
        for (int i=0; i<elems.length; i++) {
            elems[i] = LookupData.ElementNames[elemsToExclude[i]];
        }
        
        // Create an appropriate filter
        PhaseDiagramExclusionFilter filter = new PhaseDiagramExclusionFilter();
        filter.setElementList(elems);
        
        // Create clones of input data for train and test set
        Dataset trainData = data.clone();
        Dataset testData = data.clone();
        
        // Use the filter to create test test
        filter.setExclude(false);
        filter.filter(testData);
        
        // If no test data, return null
        if (testData.NEntries() == 0) {
            return null;
        }
        
        // Use filter to create training set
        filter.setExclude(true);
        filter.filter(trainData);
        
        // Run the model
        modelToTest.train(trainData);
        modelToTest.run(testData);
        
        // Return results
        return testData;
    }
    
    /**
     * Provided a {@linkplain CompositionDataset}, get list of all included elements.
     * 
     * @param data Dataset to be analyzed
     * @return List of all elements in dataset
     */
    public static List<Integer> getElements(CompositionDataset data) {
        // Gather set of all elements
        Set<Integer> allElements = new TreeSet<>();
        for (BaseEntry ptr : data.getEntries()) {
            CompositionEntry entry = (CompositionEntry) ptr;
            for (int i : entry.getElements()) {
                allElements.add(i);
            }
        }
        
        // Convert to list
        return new ArrayList<>(allElements);
    }
}
