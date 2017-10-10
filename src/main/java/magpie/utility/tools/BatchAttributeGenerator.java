package magpie.utility.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.data.utilities.output.BaseDatasetOutput;
import magpie.data.utilities.output.DelimitedOutput;
import magpie.user.CommandHandler;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;

/**
 * Compute attributes and save them to a file in batches. This prevents needing
 * to store all attributes in memory at a single time. Also supports computing 
 * attributes in parallel.
 * 
 * <p>Note: Order of entries is not guaranteed to be preserved for parallel evaluation.
 * 
 * <p><b>How to Use This Class</b>
 * 
 * <p>There are two major use cases for this class
 * 
 * <ol>
 * <li>Entries already stored in {@linkplain Dataset}:
 * <ul>
 * <li>Java Interface: Call 
 * {@linkplain #output(magpie.data.Dataset, java.io.File) } or 
 * {@linkplain #output(magpie.data.Dataset, java.io.OutputStream) }
 * from the to generate entries at write them to file
 * <li>Text Interface: write &lt;filename&gt; $&lt;data&gt;, where "data" is a {@linkplain Dataset}
 * </ul>
 * <li>Entries generated iteratively: Dynamically generate entries, and compute 
 * attributes. Entries will not be stored
 * <ul>
 * <li>Java Interface: Call {@linkplain #output(magpie.data.Dataset, java.util.Iterator, java.io.OutputStream) }
 * <li>Text Interface: write &lt;filename&gt; $&lt;data&gt; $&lt;generator&gt;, where "data"
 * is a template for computing attributes,and "generator" is an 
 * </ul>
 * </ol>
 * 
 * <usage><p><b>Usage</b>: &lt;batch size&gt; &lt;output method&gt;
 * [&lt;output options...&gt;]
 * <pr><br><i>batch size</i>: Number of entries to compute at once
 * <pr><br><i>output method</i>: Name of {@linkplain BaseDatasetOutput} 
 * method used for output
 * <pr><br><i>output options</i>: Any options for the output method</usage>
 * 
 * @author Logan Ward
 */
public class BatchAttributeGenerator implements Commandable, Options {
    /** Tool used to output data */
    protected BaseDatasetOutput Outputter = new DelimitedOutput();
    /** Number of entries to evaluate at a single time */
    protected int BatchSize = 10000;
    /** Dataset used to generate attributes */
    protected Dataset AttributeComputer;
    /** Iterator over entries being processed */
    private Iterator<BaseEntry> EntryIterator;

    /**
     * Create an batch writer using the default settings: comma-delimited file,
     * batch size of 10000 entries
     */
    public BatchAttributeGenerator() {
    }

    /**
     * Create a batch evaluator with a specified output method and batch size
     * @param outputter Output method (default: {@linkplain DelimitedOutput})
     * @param batchSize Batch size (default: 10000)
     */
    public BatchAttributeGenerator(BaseDatasetOutput outputter, 
            int batchSize) {
        Outputter = outputter;
        BatchSize = batchSize;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int batchSize;
        String outputMethod;
        List<Object> outputOptions;
        
        // Parse options
        try {
            batchSize = Integer.parseInt(Options.get(0).toString());
            outputMethod = Options.get(1).toString();
            outputOptions = Options.subList(2, Options.size());
        } catch (Exception e) {
            throw new RuntimeException(printUsage());
        }
        
        // Enact options
        setBatchSize(batchSize);
        
        BaseDatasetOutput output = (BaseDatasetOutput) CommandHandler.instantiateClass(
                "data.utilities.output." + outputMethod, outputOptions);
        setOutputter(output);
    }

    @Override
    public String printUsage() {
        return "Usage: <batch size> <output method> [<output method options...>]";
    }

    /**
     * Define tool used to output data
     * @param output Output method
     */
    public void setOutputter(BaseDatasetOutput output) {
        this.Outputter = output;
    }

    /**
     * Define maximum number of entries to compute at a single time
     * @param size Desired batch size
     */
    public void setBatchSize(int size) {
        this.BatchSize = size;
    }
    
    /**
     * Set the iterator over entries being processed
     * @param iterator Iterator
     */
    protected void setEntryIterator(Iterator<BaseEntry> iterator) {
        EntryIterator = iterator;
    }
    
    /**
     * Compute attributes and write entries to a file
     * @param data Dataset to be written
     * @param file Path to output file
     * @throws java.io.IOException
     */
    public void output(Dataset data, File file) throws IOException {
        OutputStream out = new FileOutputStream(file);
        output(data, out);
        out.close();
    }
    
    /**
     * Compute attributes and write entries to an output stream in batches
     * @param data Dataset to be written
     * @param out Output stream for data
     */
    public void output(Dataset data, OutputStream out) {
        // Get an iterator over the entries
        Iterator<BaseEntry> entryIterator = data.getEntriesWriteAccess().iterator();
        
        // Call output operation
        output(data.emptyClone(), entryIterator, out);
    }
    
    /**
     * Compute attributes and write entries to an output stream in batches
     * @param template Dataset used to generate attributes
     * @param iterator Iterator over entries to be printed
     * @param out Output stream for data
     */
    public void output(Dataset template, Iterator<BaseEntry> iterator, OutputStream out) {
        // Set the entry iterator 
        setEntryIterator(iterator);
        
        // Make empty clone of template
        template = template.emptyClone();
        
        // Compute attributes before writing header. Gets the right names
        try {
            template.generateAttributes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // Write header to output stream
        Outputter.printHeader(template, out);
        
        // Initialize threads
        final OutputStream finalOut = out;
        final Dataset runData = template;
        List<Runnable> threads = new ArrayList<>(Magpie.NThreads);
        for (int t=0; t<Magpie.NThreads; t++) {
            threads.add(new Runnable() {
                @Override
                public void run() {
                    // Make a clone of the dataset
                    Dataset myData = runData.emptyClone();
                    
                    // Until no more entries left to process
                    while (true) {
                        // Get up to the batch size
                        List<BaseEntry> batch = getBatch();
                        
                        // Check if all batches are complete
                        if (batch.isEmpty()) {
                            break;
                        }
                        
                        // Add entries to dataset, compute attributes
                        try {
                            myData.addEntries(batch);
                            myData.generateAttributes();

                            // Write to output
                            writeEntries(myData, finalOut);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        
                        // Delete attributes and representations from entries
                        myData.clearAttributes();
                        for (BaseEntry entry : myData.getEntries()) {
                            entry.reduceMemoryFootprint();
                        }
                        
                        // Clear data from this thread
                        myData.clearData();
                        
                        // Suggest that the JVM run the GC
                        System.gc();
                    }
                }
            });
        }
        
        // Set the thread count to 1 to ensure no other threads are launched
        int oldThreads = Magpie.NThreads;
        Magpie.NThreads = 1;
        
        // Launch threads
        ExecutorService executor = Executors.newFixedThreadPool(oldThreads);
        List<Future> futures = new ArrayList<>(threads.size());
        for (Runnable thread : threads) {
            Future f = executor.submit(thread);
            futures.add(f);
        }
        
        // Wait until all threads have finished
        executor.shutdown();
        for (Future f : futures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                System.err.println("Thread failed due to: " + e);
                throw new RuntimeException("Attribute generation failed");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Reset thread count
        Magpie.NThreads = oldThreads;
        
        // Write the end of the data file
        Outputter.printEnd(out);
    }
    
    /**
     * Get a batch of entries.
     * @return List containing up to {@linkplain #BatchSize} entries. May be empty
     */
    synchronized protected List<BaseEntry> getBatch() {
        int count = 0;
        List<BaseEntry> output = new ArrayList<>(BatchSize);
        while (count < BatchSize && EntryIterator.hasNext()) {
            output.add(EntryIterator.next());
            count++;
        }
        return output;
    }
    
    /**
     * Write entries to output stream
     * @param toWrite Entries to be written
     * @param output Desired output stream
     */
    synchronized protected void writeEntries(Dataset toWrite, OutputStream output) {
        Outputter.printEntries(toWrite.getEntries(), output);
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        // Check input
        if (Command.isEmpty() || ! Command.get(0).toString().equalsIgnoreCase("write")) {
            throw new IllegalArgumentException("Usage: <filename> $<data> [$<generator>]");
        }
        
        // Parse instructions
        String filename;
        Dataset data;
        BaseEntryGenerator gen = null;
        try {
            filename = Command.get(1).toString();
            data = (Dataset) Command.get(2);
            if (Command.size() >= 4) {
                gen = (BaseEntryGenerator) Command.get(3);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Usage: <filename> $<data> [$<generator>]");
        }
        
        // Make the writer to the output file
        FileOutputStream output = new FileOutputStream(filename);
        
        // Run the appropriate mehtod
        if (gen == null) {
            output(data, output);
            System.out.format("\tWrote data to %s using %s\n", filename,
                    Outputter.getClass().getSimpleName());
        } else {
            output(data, gen.iterator(), output);
            System.out.format("\tWrote entries generated by a %s to %s using %s\n", 
                    gen.getClass().getSimpleName(),
                    filename,
                    Outputter.getClass().getSimpleName());
        }
        
        return filename;
    }
}
