package magpie.utility.tools;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.models.BaseModel;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;

/**
 * Generate attributes and simpleRun models in batches. Useful if a dataset contains too
 * many entries to store all of their attributes in memory at one time.
 * 
 * <usage><p><b>Usage</b>: $&lt;model&gt; &lt;batch size&gt;
 * <br><pr><i>model</i>: Model to be evaluated 
 * <br><pr><i>batch size</i>: Number of entries to evaluate at one time</usage>
 * 
 * <p><b><u>Implemented Commands</u></b>
 * 
 * <command><p><b>run $&lt;data&gt;</b> - Evaluate a model in batch model
 * <br><pr><i>data</i>: Dataset to be evaluated</command>
 * 
 * <command><p><b>add $&lt;data&gt; $&lt;generator&gt; [&lt;include|exclude&gt; $&lt;filter&gt;]</b>
 * - Generate entries, run them using a model, and add them to a dataset if 
 * they pass a filter
 * <br><pr><i>data</i>: Dataset to be added to</command>
 * <br><pr><i>generator</i>: {@linkplain BaseEntryGenerator} used to generate entries
 * <br><pr><i>include|exclude</i>: Optional: Whether to include or exclude data based on filter
 * <br><pr><i>filter</i>: {@linkplain BaseDatasetFilter} used to determine whether entries should be added to data</usage>
 * @author Logan Ward
 */
public class BatchModelEvaluator implements Commandable, Options {
    /** Model to be evaluated */
    private BaseModel Model;
    /** Number of entries to evaluate at once */
    private int BatchSize = 10000;
    /** Iterator over entries to be evaluated */
    protected Iterator<BaseEntry> EntryIterator;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            if (Options.size() != 2) {
                throw new Exception();
            }
            setModel((BaseModel) Options.get(0));
            setBatchSize(Integer.parseInt(Options.get(1).toString()));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<model> <batch size>";
    }

    /**
     * Define which model this class is used to evaluate
     * @param model Model to be evaluated
     */
    public void setModel(BaseModel model) {
        this.Model = model;
    }

    /**
     * Define how many entries to evaluate at once. 
     * @param size Desired batch size
     */
    public void setBatchSize(int size) {
        this.BatchSize = size;
    }
    
    /**
     * Generate entries and store them in a dataset. Optionally, filter data
     *  before adding it to dataset
     * @param data Dataset to be added to
     * @param generator Generator for creating entries
     * @param filter Filter to be used before adding entries to data
     * @throws java.lang.Exception
     */
    public void evaluate(Dataset data, BaseEntryGenerator generator, 
            BaseDatasetFilter filter) throws Exception {
        // Get iterator from the entry generator
        EntryIterator = generator.iterator();
        
        // Execute
        if (Magpie.NThreads == 1) {
            // Create clone used to store results
            Dataset runData = data.emptyClone();
            
            // Evaluate in serial
            data.addEntries(runAndFilter(Model, runData, filter));
        } else {
            // Store number of threads
            int nThreads = Magpie.NThreads;
            
            // Turn off parallelism for any other parts of the code
            Magpie.NThreads = 1;
            
            // Create executor service
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            
            // Make final pointer to input data and filter
            final Dataset dataPtr = data;
            final BaseDatasetFilter filterPtr = filter;
            
            // Start threads
            List<Future<List<BaseEntry>>> results = new ArrayList<>(nThreads);
            for (int i=0; i<nThreads; i++) {
                Callable<List<BaseEntry>> thread = new Callable<List<BaseEntry>>() {

                    @Override
                    public List<BaseEntry> call() {
                        // Make a clone of the model
                        BaseModel localModel = Model.clone();
                        
                        // Make a clone of the dataset
                        Dataset localData = dataPtr.emptyClone();
                        
                        // Run serially
                        try {
                            return runAndFilter(localModel, localData, filterPtr);
                        } catch (Exception e) {
                            throw new Error(e);
                        }
                    }
                };
                
                // Submit thread
                results.add(executor.submit(thread));
            }
            
            // Wait until all have finished
            executor.shutdown();
            for (Future<List<BaseEntry>> result : results) {
                List<BaseEntry> entries = result.get();
                data.addEntries(entries);
            }
            
            // Restore parallelism
            Magpie.NThreads = nThreads;
        }
    }
    
    /**
     * Evaluate the model on a dataset in batch mode.
     * @param data Dataset to be evaluated
     * @throws Exception 
     */
    public void evaluate(Dataset data) throws Exception {
        // Get iterator over all entries to be evaluated
        EntryIterator = data.getEntries().iterator();
        
        // Execute
        if (Magpie.NThreads == 1) {
            // Create clone used to store results
            Dataset runData = data.emptyClone();
            
            // Evaluate in serial
            simpleRun(Model, runData);
        } else {
            // Store number of threads and original batch size
            int nThreads = Magpie.NThreads;
            int origBatchSize = BatchSize;
            
            // Turn off parallelism for any other parts of the code
            Magpie.NThreads = 1;
            
            // Reduce the batch size to reduce memory usage
            BatchSize /= nThreads;
            
            // Create executor service
            ExecutorService executor = Executors.newFixedThreadPool(nThreads);
            
            // Make final pointer to input data
            final Dataset dataPtr = data;
            
            // Start threads
            for (int i=0; i<nThreads; i++) {
                Runnable thread = new Runnable() {

                    @Override
                    public void run() {
                        // Make a clone of the model
                        BaseModel localModel = Model.clone();
                        
                        // Make a clone of the dataset
                        Dataset localData = dataPtr.emptyClone();
                        
                        // Run serially
                        try {
                            simpleRun(localModel, localData);
                        } catch (Exception e) {
                            throw new Error(e);
                        }
                    }
                };
                
                // Submit thread
                executor.submit(thread);
            }
            
            // Wait until all have finished
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                throw new Error(e);
            }
            
            // Restore parallelism and batch size
            Magpie.NThreads = nThreads;
            BatchSize = origBatchSize;
        }
    }

    /**
     * Evaluate all entries in serial mode.
     * @param model Model to be run
     * @param data Empty dataset used to compute attributes
     * @throws Exception 
     */
    protected void simpleRun(BaseModel model, Dataset data) throws Exception {
        while (true) {
            // Get the subset
            List<BaseEntry> subList = getSubList();
            
            // If no entries left to evaluate, return
            if (subList.isEmpty()) {
                return;
            }
            
            // Run them
            data.addEntries(subList);
            data.generateAttributes();
            model.run(data);
            
            // Clean up
            for (BaseEntry entry : subList) {
                entry.clearAttributes();
            }
            data.clearData();
            System.gc();
        }
    }
    
    /**
     * Create entries, evaluate them, filter out acceptable candidates
     * @param model Model to be run
     * @param data Empty dataset used to compute attributes
     * @param filter Filter to be used to select best attributes. null to not 
     *  filter results
     * @return Entries that passed the filter
     * @throws Exception 
     */
    protected List<BaseEntry> runAndFilter(BaseModel model, Dataset data, 
            BaseDatasetFilter filter) throws Exception {
        // Create list for storing output
        List<BaseEntry> output = new ArrayList<>();
        
        // Loop until no more entries are created
        while (true) {
            // Get the subset
            List<BaseEntry> subList = getSubList();
            
            // If no entries left to evaluate, return
            if (subList.isEmpty()) {
                return output;
            }
            
            // Run them
            data.addEntries(subList);
            data.generateAttributes();
            model.run(data);
            
            // Optional: Filter out data
            if (filter != null) {
                filter.filter(data);
            }
            
            // Delete attributes
            for (BaseEntry entry : subList) {
                entry.clearAttributes();
            }
            
            // Store results
            output.addAll(data.getEntries());
            
            // Prepare for next loop
            data.clearData();
            System.gc();
        }
    }
    
    /**
     * Get list of entries to be evaluated
     * @return List of entries. If none remaining, list will be empty
     */
    public synchronized List<BaseEntry> getSubList() {
        List<BaseEntry> output = new ArrayList<>(BatchSize);
        int pos = 0;
        while (EntryIterator.hasNext() && pos < BatchSize) {
            output.add(EntryIterator.next());
            pos++;
        }
        return output;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println("Evaluator with a " + Model.getClass().getSimpleName());
            return null;
        }
        
        // Parse input
        String action = Command.get(0).toString().toLowerCase();
        switch (action.toLowerCase()) {
            case "run": {
                Dataset data;
                try {
                    data = (Dataset) Command.get(1);
                    if (Command.size() != 2) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: run $<data>");
                }
                
                // Run it
                evaluate(data);
                System.out.println("\tEvaluated a " + Model.getClass().getSimpleName() + 
                    " on " + data.NEntries() + " entries in blocks of " + BatchSize);
                break;
            }
            case "add": {
                Dataset data;
                BaseEntryGenerator gen;
                BaseDatasetFilter filter = null;
                try {
                    data = (Dataset) Command.get(1);
                    gen = (BaseEntryGenerator) Command.get(2);
                    if (Command.size() > 3) {
                        boolean toExclude;
                        String word = Command.get(3).toString().toLowerCase();
                        if (word.startsWith("exc")) {
                            toExclude = true;
                        } else if (word.startsWith("inc")) {
                            toExclude = false;
                        } else {
                            throw new Exception();
                        }
                        filter = (BaseDatasetFilter) Command.get(4);
                        filter.setExclude(toExclude);
                        if (Command.size() > 5) {
                            throw new Exception();
                        }
                    } 
                } catch (Exception e) {
                    throw new Exception("Usage: $<data> $<generator> [<include|exclude> $<filter>]");
                }
                
                // Generate the entries
                int initialEntries = data.NEntries();
                evaluate(data, gen, filter);
                
                if (filter != null) {
                    System.out.format("\tAdded %d entries created using a %s that "
                            + " passed a %s\n", data.NEntries() - initialEntries, 
                            gen.getClass().getSimpleName(),
                            filter.getClass().getSimpleName());
                } else {
                    System.out.format("\tAdded %d entries to dataset using a %s\n", 
                        data.NEntries() - initialEntries, gen.getClass().getSimpleName());
                }
                break;
            }
            default: 
                throw new Exception("Command not recognized: " + action);
        }
        return null;
    }
}
