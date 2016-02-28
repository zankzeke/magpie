package magpie.data;

import java.io.BufferedReader;
import java.io.FileReader;
import magpie.data.utilities.DatasetHelper;
import weka.core.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.naming.OperationNotSupportedException;
import magpie.Magpie;
import magpie.attributes.evaluators.BaseAttributeEvaluator;
import magpie.attributes.expanders.BaseAttributeExpander;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.utilities.DatasetOutput;
import magpie.data.utilities.filters.BaseDatasetFilter;
import magpie.data.utilities.generators.BaseEntryGenerator;
import magpie.data.utilities.modifiers.BaseDatasetModifier;
import magpie.optimization.rankers.BaseEntryRanker;
import static magpie.user.CommandHandler.instantiateClass;
import static magpie.user.CommandHandler.printImplmentingClasses;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;
import magpie.utility.interfaces.Savable;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import weka.core.converters.ArffLoader;

/**
 * Provides a basic storage container for data-mining tasks. Must be filled with
 * entries that are subclasses of BaseEntry
 *
 * <p>
 * To implement a new Dataset, you first need to create an extension of
 * {@linkplain BaseEntry} that represents the new kind of data. Then, you need
 * to overload the following operations:
 *
 * <ul>
 * <li>{@linkplain #getEntry(int) } - Might be useful to overload with an
 * operation that returns entry type associated with this model
 * <li>{@linkplain  #addEntry(java.lang.String) } - Call the constructor to the
 * associated entry type
 * <li>{@linkplain #calculateAttributes() } - Compute any new attributes for for
 * class
 * <li>{@linkplain #emptyClone() } - Create clone of dataset. Cloning entries
 * is handled in {@linkplain #clone() }, which does not need to be modified.
 * </ul>
 *
 * <usage><p>
 * <b>Usage</b>: *No options to set*</usage>
 *
 * <p>
 * <b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>add &lt;entries...&gt;</b> - Add entries to a dataset
 * <br><pr><i>entries...</i>: Strings describing entries to be added</command>
 *
 * <command><p><b>combine $&lt;dataset&gt;</b> - Add entries from another dataset
 * <br><pr><i>dataset</i>: Dataset to combine with this dataset</command>
 * 
 * <command><p><b>add $&lt;dataset&gt; [-force]</b> - Add entries from another dataset
 * <br><pr><i>dataset</i>: Dataset to be merged with this one
 * <br><pr><i>-force</i>: Optional: Whether to force merge if attributes / classes /
 * properties are different
 * <br>If attributes, classes, or properties are different, attributes and class values 
 * in new entries (i.e., those from the other dataset) will be deleted and properties
 * will be merged
 * </command>
 * 
 * <command><p><b>add $&lt;dataset&gt; [-force]</b> - Add entries from another dataset
 * <br><pr><i>dataset</i>: Dataset to be merged with this one
 * <br><pr><i>-force</i>: Optional: Whether to force merge if attributes / classes /
 * properties are different
 * <br>If attributes, classes, or properties are different, attributes and class values 
 * in new entries (i.e., those from the other dataset) will be deleted and properties
 * will be merged
 * </command>
 * 
 * <command><p>
 * <b>&lt;output> = clone [-empty]</b> - Create a copy of this dataset
 * <br><pr><i>-empty</i>: Do not copy entries from dataset into clone
 * </command>
 * 
 * <command><p><b>combine $&lt;dataset&gt;</b> - Add all entries from another dataset
 * <br><pr><i>dataset</i>: Dataset to merge with this one. It will remain unchanged.
 * </command>
 *
 * <command><p>
 * <b>filter &lt;include|exclude> &lt;method> [&lt;options...>]</b> - Run
 * dataset through a filter
 * <br><pr><i>include|exclude</i>: Whether to include/exclude only entries that
 * pass the filter
 * <br><pr><i>method</i>: Filtering method. Name of a
 * {@linkplain BaseDatasetFilter} ("?" to print available methods)
 * <br><pr><i>options...</i>: Options for the filter</command>
 *
 * <command><p>
 * <b>generate &lt;method&gt; [&gt;options&lt;]</b> - Generate new entries
 * <br><pr><i>method</i>: Name of a {@linkplain BaseEntryGenerator}. ("?" for
 * options)
 * <br><pr><i>options</i>: Any options for the entry generator</command>
 * 
 * <command><p>
 * <b>match $&lt;dataset&gt; &lt;num&gt;</b> - Find most similar entries in this dataset
 * <br><pr><i>dataset</i>: Dataset containing entries to be matched
 * <br><pr><i>num</i>: Number of closest entries to print
 * <br>Prints the most similar entries in this dataset to those in the dataset
 * passed as the argument.</command>
 *
 * <command><p>
 * <b>modify &lt;method> [&lt;options>]</b> - Modify the dataset
 * <br><pr><i>method</i>: How to modify dataset. Name of a
 * {@linkplain BaseDatasetModifier}. ("?" to print available methods)
 * <br><pr><i>options</i>: Any options for the dataset</command>
 *
 * <command><p>
 * <b>import &lt;filename> [&lt;options...>]</b> - Import data by reading a file
 * <br><pr><i>filename</i>: Name of file to import data from
 * <br><pr><i>options...</i>: Any options used when parsing this dataset
 * (specific to type of Dataset)</command>
 *
 * <command><p>
 * <b>rank &lt;number> &lt;maximum|minimum> &lt;measured|predicted> &lt;method>
 * [&lt;options>]</b> - Print the top ranked entries based by some measure
 * <br><pr><i>number</i>: Number of top entries to print
 * <br><pr><i>maximum|minimum</i>: Whether to print entries with the largest or
 * smallest objection function
 * <br><pr><i>measured|predicted</i>: Whether to use the measured or predicted
 * values when calculation
 * <br><pr><i>method</i>: Object function used to rank entries. Name of a
 * {@link BaseEntryRanker} ("?" for available methods)
 * <br><pr><i>options...</i>: Any options for the objective function</command>
 *
 * <command><p>
 * <b>&lt;output> = split &lt;number|fraction></b> - Randomly select and remove
 * entries from dataset
 * <br><pr><i>number|fraction</i>: Either the fraction or number of entries to
 * be removed
 * <br><pr><i>output</i>: New dataset containing randomly selected entries that
 * were in this dataset</command>
 *
 * <command><p>
 * <b>&lt;output> = subset &lt;number|fraction></b> - Generate a random subset
 * from this dataset
 * <br><pr><i>number|fraction</i>: Either the fraction or number of entries to
 * select
 * <br><pr><i>output</i>: New dataset containing random selection from this
 * dataset</command>
 *
 * <command><p>
 * <b>attributes</b> - Print all attributes</command>
 *
 * <command><p>
 * <b>attributes expanders add &lt;method> [&lt;options...>]</b> - Add an
 * attribute expander to be run after generating attributes
 * <br><pr><i>method</i>: How to expand attributes. Name of a
 * {@linkplain BaseAttributeExpander} ("?" to print available methods)
 * <br><pr><i>options...</i>: Any options for the expansion method These
 * expanders are designed to create new attributes based on existing
 * ones.</command>
 *
 * <command><p>
 * <b>attributes expanders clear</b> - Clear the current list of attribute
 * expanders</command>
 *
 * <command><p>
 * <b>attributes expanders run</b> - Run the currently-defined list of attribute
 * expanders</command>
 *
 * <command><p>
 * <b>attributes generators add &lt;method> [&lt;options...>]</b> - Add a new
 * attribute generator to list of generators
 * <br><pr><i>method</i>: New generation method. Name of a
 * {@linkplain BaseAttributeGenerator} ("?" to print available methods)
 * <br><pr><i>options...</i>: Any options for the generator method These
 * expanders are designed to create new attributes tailored for a specific
 * application.</command>
 *
 * <command><p>
 * <b>attributes generators run</b> - Run the currently-defined list of
 * attribute expanders</command>
 *
 * <command><p>
 * <b>attributes generators clear</b> - Clear the current list of attribute
 * generators</command>
 *
 * <command><p>
 * <b>attributes generate</b> - Generate attributes for each entry</command>
 *
 * <command><p>
 * <b>attributes rank &lt;number> &lt;method> [&lt;options...&gt;]</b> - Rank
 * attributes based on predictive power
 * <br><pr><i>number</i>: Number of top attributes to print
 * <br><pr><i>method</i>: Method used to rank attributes. Name of a
 * {@linkplain BaseAttributeEvaluator} ("?" to print available methods)
 * <br><pr><i>options...</i>: Options for the evaluation method.</command>
 *
 * <p>
 * <b><u>Implemented Print Commands:</u></b>
 *
 * <print><p>
 * <b>details</b> - Print details about this class</print>
 *
 * <print><p>
 * <b>dist</b> - Print distribution of entries between known classes</print>
 *
 * <p>
 * <b><u>Implemented Save Formats:</u></b> TBD
 *
 * <save><p>
 * <b>csv</b> - Comma-separated value format.
 * <br>The value of each attribute and the measured class variable, if
 * defined.</save>
 *
 * <save><p>
 * <b>arff</b> - Weka's ARFF format.
 * <br>Requires that a measured value is available for the class variable of
 * each entry.</save>
 *
 * <save><p>
 * <b>stats</b> - Writes predicted and measured class variables.
 * <br>This is intended to allow an external program to evaluate model
 * performance.</save>
 *
 * @author Logan Ward
 * @version 0.1
 */
public class Dataset extends java.lang.Object implements java.io.Serializable,
        java.lang.Cloneable, Printable, Savable, Options, Commandable, Citable {

    /**
     * Names of attributes that describe each entry
     */
    protected ArrayList<String> AttributeName;
    /**
     * Names of the class(s) of each entry
     */
    private String[] ClassName;
    /**
     * Internal array that stores entries
     */
    protected ArrayList<BaseEntry> Entries;
    /**
     * Tools to generate new attributes based on existing ones
     */
    protected List<BaseAttributeExpander> Expanders = new LinkedList<>();
    /**
     * Tools to generate special-purpose attributes
     */
    protected List<BaseAttributeGenerator> Generators = new LinkedList<>();

    /**
     * Read the state from file using serialization
     *
     * @param filename Filename for input
     * @return Dataset stored in that file
     * @throws java.lang.Exception
     */
    public static Dataset loadState(String filename) throws Exception {
        return (Dataset) UtilityOperations.loadState(filename);
    }

    /**
     * Generate a blank dataset
     */
    public Dataset() {
        this.ClassName = new String[]{"Class"};
        this.AttributeName = new ArrayList<>();
        this.Entries = new ArrayList<>();
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        /* Nothing to do */
    }

    @Override
    public String printUsage() {
        return "Usage: *No Options*";
    }

    /**
     * Creates a new instance of this dataset, and clones of each entry.
     * @return Clone of this dataset
     */
    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public Dataset clone() {
        Dataset copy = emptyClone();

        // Make unique copies of the entries
        copy.Entries = new ArrayList<>(NEntries());
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            copy.addEntry(iter.next().clone());
        }
        return copy;
    }

    /**
     * Creates a new instance with the same class and attribute names, but
     * without any entries.
     *
     * @return Dataset with same properties, no entries
     */
    public Dataset emptyClone() {
        Dataset copy;
        try {
            copy = (Dataset) super.clone();
        } catch (CloneNotSupportedException c) {
            throw new Error(c);
        }
        copy.AttributeName = new ArrayList<>(AttributeName);
        copy.ClassName = ClassName.clone();
        copy.Expanders = new LinkedList<>(Expanders);
        copy.Generators = new LinkedList<>(Generators);
        
        // Make unique copies of the entries
        copy.Entries = new ArrayList<>();
        return copy;
    }

    /**
     * Get a dataset that only contains entries with a measured class variable.
     *
     * @return Dataset with all entries that have a measured class
     */
    public Dataset getTrainingExamples() {
        Dataset output = emptyClone();
        for (BaseEntry entry : Entries) {
            if (entry.hasMeasurement()) {
                output.addEntry(entry);
            }
        }
        return output;
    }

    /**
     * Clear out all entries
     */
    public void clearData() {
        this.Entries.clear();
    }
    
    /**
     * Clear attribute data. Used when it is necessary to conserve memory
     */
    public void clearAttributes() {
        AttributeName.clear();
        for (BaseEntry entry : Entries) {
            entry.clearAttributes();
        }
    }

    /**
     * Add a new tool to expand the number of attributes for this dataset.
     *
     * @param expander New expander
     */
    public void addAttribueExpander(BaseAttributeExpander expander) {
        Expanders.add(expander);
    }

    /**
     * Reset the list of attribute expanders
     */
    public void clearAttributeExpanders() {
        Expanders.clear();
    }

    /**
     * Get a copy of the list of currently-employed attribute expanders.
     *
     * @return List of attribute expanders
     * @see
     * #addAttribueExpander(magpie.attributes.expanders.BaseAttributeExpander)
     */
    public List<BaseAttributeExpander> getAttributeExpanders() {
        return new LinkedList<>(Expanders);
    }

    /**
     * Expand the list of attributes using the currently-set list of attribute
     * expanders.
     *
     * @see #getAttributeExpanders()
     */
    public void runAttributeExpanders() {
        for (BaseAttributeExpander expander : Expanders) {
            expander.expand(this);
        }
    }

    /**
     * Add a new tool to generate additional attributes
     *
     * @param generator New generator
     */
    public void addAttribueGenerator(BaseAttributeGenerator generator) {
        Generators.add(generator);
    }

    /**
     * Reset the list of attribute generators
     */
    public void clearAttributeGenerators() {
        Generators.clear();
    }

    /**
     * Get a copy of the list of currently-employed attribute generators.
     *
     * @return List of attribute expanders
     * @see
     * #addAttribueExpander(magpie.attributes.expansion.BaseAttributeExpander)
     */
    public List<BaseAttributeGenerator> getAttributeGenerators() {
        return new LinkedList<>(Generators);
    }

    /**
     * Expand the list of attributes using the currently-set list of attribute
     * generators.
     *
     * @throws java.lang.Exception
     * @see #getAttributeGenerators()
     */
    public void runAttributeGenerators() throws Exception {
        for (BaseAttributeGenerator generator : Generators) {
            generator.addAttributes(this);
        }
    }

    /**
     * Generate attributes for this dataset
     *
     * @throws java.lang.Exception If any error is encountered
     */
    final public void generateAttributes() throws Exception {
        if (Magpie.NThreads > 1 && NEntries() > Magpie.NThreads) {
            // Store the number of threads 
            int originalNThreads = Magpie.NThreads;
            
            // Set Magpie.NThreads to 1 to prevent children from spawning threads
            Magpie.NThreads = 1;
            
            // Split dataset for threading
            Dataset[] threadData = splitForThreading(originalNThreads);
            
            // Create executor
            ExecutorService executor = Executors.newFixedThreadPool(originalNThreads);
            
            // Launch threads
            List<Future<Integer>> results = new ArrayList<>(originalNThreads);
            for (final Dataset part : threadData) {
                Callable<Integer> thread = new Callable<Integer>() {

                    @Override
                    public Integer call() throws Exception {
                        part.generateAttributes();
                        return part.NAttributes();
                    }
                };
                results.add(executor.submit(thread));
            }
            
            // Wait until all threads are done
            executor.shutdown();
            
            // Get attribute names from thread #0
            try {
                // Wait until that thread finishes
                results.get(0).get();
                
                // Set the names for this instance to those of the first part
                AttributeName = threadData[0].AttributeName;
            } catch (ExecutionException e) {
                throw new Exception(e.getCause());
            }
            
            // Wait for the other entries
            for (int id=1; id<originalNThreads; id++) {
                try {
                    // Wait until that thread finishes
                    results.get(id).get();
                } catch (ExecutionException e) {
                    throw new Exception(e.getCause());
                }
            }
            
            // Restore the number of threads
            Magpie.NThreads = originalNThreads;
        } else {
            // First things first, clear out old data
            AttributeName.clear();
            for (BaseEntry e : Entries) {
                e.clearAttributes();
            }

            // Now compute attributes
            calculateAttributes();

            // Run generators
            runAttributeGenerators();

            // Run expanders
            runAttributeExpanders();

            // Reduce memory footprint, where possible
            finalizeGeneration();
        }
    }

    /**
     * Compute attributes that are specific to this class.
     *
     * @throws Exception
     */
    protected void calculateAttributes() throws Exception {
        throw new OperationNotSupportedException("Dataset does not support attribute generation");
    }

    /**
     * @return Names of all attributes
     */
    public String[] getAttributeNames() {
        return AttributeName.toArray(new String[0]);
    }

    /**
     * Get name of a specific attribute
     *
     * @param index Attribute number
     * @return Name of that attribute
     */
    public String getAttributeName(int index) {
        return AttributeName.get(index);
    }

    /**
     * Set the names of each attributes.
     *
     * <p>
     * NOTE: This will not effect the number of attributes of each entry. Make
     * sure to update those if needed!
     *
     * @param attributeNames
     */
    public void setAttributeNames(List<String> attributeNames) {
        AttributeName.clear();
        AttributeName.addAll(attributeNames);
    }

    /**
     * Get index of a certain attribute
     *
     * @param Name Name of desired attribute
     * @return Index of that attribute (-1 if it does not exist)
     */
    public int getAttributeIndex(String Name) {
        return AttributeName.indexOf(Name);
    }

    /**
     * Imports data from a text file. Expected format for file:
     * <p>
     * Attribute1Name, Attribute2Name, ..., AttributeNName, Class<br>
     * Attribute1, Attribute2, ..., AttributeN, ClassVariable
     *
     * @param filename Path to data file
     * @param options Any options used to control import
     * @throws java.lang.Exception If text import fails
     */
    public void importText(String filename, Object[] options) throws Exception {
        // Open the file
        BufferedReader fp = new BufferedReader(new FileReader(filename));

        // Clear out old data
        AttributeName.clear();
        Entries.clear();

        // If the file is a Weka arff
        if (filename.toLowerCase().contains("arff")) {
            // Import an ARFF file
            Instances arff = new ArffLoader.ArffReader(fp).getData();

            // Determine which attribute is the class index. If none is 
            //  specified in the ARFF, assume it is the last one
            int classIndex = arff.classIndex();
            if (classIndex == -1) {
                classIndex = arff.numAttributes() - 1;
                arff.setClassIndex(classIndex);
            }

            // Get possible values of the class index
            if (arff.attribute(classIndex).enumerateValues() == null) {
                ClassName = new String[]{arff.attribute(classIndex).name()};
            } else {
                List<String> classNames = new LinkedList<>();
                Enumeration enums = arff.attribute(classIndex).enumerateValues();
                while (enums.hasMoreElements()) {
                    classNames.add(enums.nextElement().toString());
                }
                ClassName = classNames.toArray(new String[0]);
            }

            // Read in attributes (only get the numeric ones)
            Set<Integer> excludedAttributes = new TreeSet<>();
            for (int i = 0; i < arff.numAttributes(); i++) {
                if (i == classIndex) {
                    continue;
                }
                if (arff.attribute(i).isNumeric()) {
                    AttributeName.add(arff.attribute(i).name());
                } else if (i != classIndex) {
                    excludedAttributes.add(i);
                }
            }

            // Read in data
            Entries.ensureCapacity(arff.numInstances());
            for (Instance inst : arff) {
                double[] attr = new double[NAttributes()];

                // Read in attributes
                int counter = 0;
                for (int a = 0; a < inst.numAttributes(); a++) {
                    if (!(a == classIndex || excludedAttributes.contains(a))) {
                        attr[counter++] = inst.value(a);
                    }
                }

                // Store values 
                BaseEntry entry = new BaseEntry();
                entry.setAttributes(attr);

                // Set class variable
                try {
                    entry.setMeasuredClass(inst.classValue());
                } catch (Exception e) {
                    // do nothing
                }
                addEntry(entry);
            }
            return;
        }

        // Process header
        String Line = fp.readLine();
        String[] Words = Line.split("[, \t]");
        AttributeName.addAll(Arrays.asList(Arrays.copyOfRange(Words, 0, Words.length - 1)));
        ClassName = new String[]{Words[Words.length - 1]};

        // Add in entries
        while (true) {
            // Read line
            Line = fp.readLine();
            if (Line == null) {
                break;
            }
            Words = Line.split("[, \t]");
            if (Words.length == 0) {
                break;
            }

            // Read in data
            double[] attributes = new double[NAttributes()];
            double cValue;
            try {
                for (int i = 0; i < AttributeName.size(); i++) {
                    attributes[i] = Double.parseDouble(Words[i]);
                }
                cValue = Double.parseDouble(Words[Words.length - 1]);
            } catch (NumberFormatException e) {
                // If a problem reading numbers, just continue
                continue;
            }

            // Add entry
            BaseEntry E = new BaseEntry();
            E.setAttributes(attributes);
            E.setMeasuredClass(cValue);
            addEntry(E);
        }
    }

    /**
     * Set name of class variable (or possible classes)
     *
     * @param newClassNames New name(s) to use
     */
    public void setClassNames(String[] newClassNames) {
        ClassName = newClassNames.clone();
    }

    /**
     * @return Names of possible classes for class variable
     */
    public String[] getClassNames() {
        return ClassName.clone();
    }

    /**
     * Get the name of a certain class (for data with multiple possible
     * classes)
     *
     * @param value Value of class variable
     * @return Name of that class
     */
    public String getClassName(int value) {
        return ClassName[value];
    }

    /**
     * Add in a new attribute. Places at end of list
     *
     * @param name Name to be added
     * @param values Value of attribute for each entry
     */
    public void addAttribute(String name, double[] values) {
        if (AttributeName.contains(name)) {
            throw new Error("Dataset already contains attribute: " + name);
        }
        AttributeName.add(name);
        for (int i = 0; i < NEntries(); i++) {
            BaseEntry E = getEntry(i);
            E.addAttribute(values[i]);
        }
    }
    
    /**
     * Add new attributes. If you use this operation, you must add attributes to 
     * each new entry manually.
     * @param names Names of new attributes
     * @see BaseEntry#addAttributes(double[]) 
     */
    public void addAttributes(List<String> names) {
        for (String name : names) {
            if (AttributeName.contains(name)) {
                throw new RuntimeException("Dataset already contains attribute: " + name);
            }
        }
        AttributeName.addAll(names);
    }

    /**
     * @return Number of features describing each entry
     */
    public int NAttributes() {
        return AttributeName.size();
    }

    /**
     * @return Number of possible (discrete) values for class variable. 1 means
     * variable is continuous
     */
    public int NClasses() {
        return ClassName.length;
    }

    /**
     * @return Number of entries in Dataset
     */
    public int NEntries() {
        return Entries.size();
    }

    /**
     * Add an entry. You may need to run {@linkplain #generateAttributes(java.lang.Object[])
     * }.
     *
     * @param e Entry to be added
     */
    public void addEntry(BaseEntry e) {
        Entries.add(e);
    }

    /**
     * A new entry by parsing an input string. After using this operation, it
     * may be necessary to recalculate attributes.
     *
     * @param input String describing the entry
     * @return Entry representing the parsed string.
     * @throws Exception If conversion fails
     */
    public BaseEntry addEntry(String input) throws Exception {
        BaseEntry toAdd = new BaseEntry(input);
        addEntry(toAdd);
        return toAdd;
    }

    /**
     * Add many entries to a the data set
     *
     * @param entries Any collection type of entries
     */
    public void addEntries(Collection<? extends BaseEntry> entries) {
        Entries.addAll(entries);
    }
    
    /** 
     * Add entries from another dataset. If other dataset has different attributes
     * or class attributes, you can force a merge between these datasets
     * by deleting the attributes and classes from the other dataset.
     * 
     * <p>Entries will be cloned before adding them to this dataset.
     * 
     * @param otherDataset Dataset to be added to this one
     * @param forceMerge Whether to force merge if attributes / class are different.
     * @throws java.lang.Exception If datasets have different classes or attributes,
     * and forceMerge is false.
     */
    public void addEntries(Dataset otherDataset, boolean forceMerge) throws Exception {
        // Check whether attributes / class are the same
        boolean attrSame = otherDataset.AttributeName.equals(AttributeName);
        boolean classSame = Arrays.equals(otherDataset.ClassName, ClassName);
        
        // If they are, and force is not true: Fail
        if (! ((attrSame && classSame) || forceMerge)) {
            throw new Exception("Datasets are not identical and merge not forced");
        }
        
        // Add in the entries
        for (BaseEntry otherEntry : otherDataset.Entries) {
            // Make the clone
            BaseEntry entry = otherEntry.clone();
            
            // If needed, delete attributes or class
            if (! attrSame) {
                entry.clearAttributes();
            }
            if (! classSame) {
                entry.deleteMeasuredClass();
                entry.deletePredictedClass();
            }
            
            // Add it to this dataset
            addEntry(entry);
        }
    }

    /**
     * Remove all duplicate entries
     */
    public void removeDuplicates() {
        Set Filter = new HashSet<>(Entries);
        Entries.clear();
        Entries.addAll(Filter);
    }

    /**
     * Determine whether a dataset contains a certain entry
     *
     * @param Entry Entry to be tested
     * @return Whether the dataset contains <code>Entry</code>
     */
    public boolean containsEntry(BaseEntry Entry) {
        return Entries.contains(Entry);
    }
    
    /**
     * Find entries in this dataset with attributes closest to a user-provided
     * entry. Measures distance using the L2 norm.
     * @param entry Entry to be matched
     * @param n Number of top entries to list
     * @return List of top entries
     */
    public List<BaseEntry> matchEntries(BaseEntry entry, int n) {
        // Make sure the entry has the same number of attriutes
        if (entry.NAttributes() != NAttributes()) {
            throw new RuntimeException("Entry has wrong number of attributes: " 
                    + entry.NAttributes() + " != " + NAttributes());
                    
        }
        
        // Make the priority queue to be sorted
        PriorityQueue<Pair<BaseEntry, Double>> queue = new PriorityQueue<>(n, 
                new Comparator<Pair<BaseEntry, Double>>() {

            @Override
            public int compare(Pair<BaseEntry, Double> o1, Pair<BaseEntry, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        
        // Loop through all entries in the dataset
        DistanceMeasure meas = new org.apache.commons.math3.ml.distance.EuclideanDistance();
        double[] myAtt = entry.getAttributes();
        
        for (BaseEntry e : Entries) {
            // Get the distance
            double dist = meas.compute(myAtt, e.getAttributes());
            
            // Add to queue
            queue.add(new ImmutablePair<>(e, dist));
            
            // Pop off the current worst
            if (queue.size() > n) {
                queue.poll();
            }
        }
        
        // Transfer to a list
        List<BaseEntry> output = new ArrayList<>(n);
        
        for (Pair<BaseEntry, Double> pair : queue) {
            output.add(0, pair.getKey());
        }
        
        return output;
    }

    /**
     * Combine the data structure with another. Does not alter the other dataset.
     * Assumes that the two dataset have the same attributes, and classes.
     *
     * <p>Entries are not cloned when during combination.
     * 
     * <p>If you are looking to add entries from another dataset, also consider
     * using {@linkplain #addEntries(magpie.data.Dataset, boolean) }.
     * @param d Dataset to be added
     */
    public void combine(Dataset d) {
        if (d.NAttributes() != NAttributes()) {
            throw new Error("Data set has wrong number of features");
        }
        if (d.NClasses() != NClasses()) {
            throw new Error("Data set has wrong number of classes");
        }
        Entries.addAll(d.Entries);
    }

    /**
     * Combine the data structure with an array of other Datasets. Does not
     * alter input Datasets.
     *
     * @param d Array of Datasets
     */
    public void combine(Dataset[] d) {
        for (Dataset data : d) {
            combine(data);
        }
    }

    /**
     * Combine the data structure with a collection of other data structures.
     * Does not alter input arrays
     *
     * @param d Collection of Datasets
     */
    public void combine(Collection<Dataset> d) {
        for (Dataset data : d) {
            combine(data);
        }
    }

    /**
     * Remove all entries that are in another dataset from this dataset
     *
     * @param data Second dataset
     */
    public void subtract(Dataset data) {
        TreeSet<BaseEntry> TempSet = new TreeSet<>(Entries);
        TempSet.removeAll(data.Entries);
        Entries = new ArrayList(TempSet);
    }

    /**
     * Retrieve a single entry from the dataset
     *
     * @param index Index of entry
     * @return Specified entry
     */
    public BaseEntry getEntry(int index) {
        return Entries.get(index);
    }

    /**
     * Return copy of list of entries. Does not allow user to delete entries 
     * from the dataset.
     *
     * @return Collection of entries
     */
    public List<BaseEntry> getEntries() {
        return new ArrayList<>(Entries);
    }
    
    /**
     * Get the internal list of entries from this dataset. User can modify
     * anything about the internal dataset (ex: sort, delete entries)
     * @return Internal list of entries
     */
    public List<BaseEntry> getEntriesWriteAccess() {
        return Entries;
    }

    /**
     * Given a list of labels, separate Dataset into multiple subsets
     *
     * @param labels Label defining in which subset to label an entry
     * @return Array of subsets of length <code>max(label) + 1</code>, where
     * each member, i, contains entries with <code>label[i] == i</code>.
     */
    public Dataset[] partition(int[] labels) {
        int maxLabel = -1;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i] > maxLabel) {
                maxLabel = labels[i];
            }
        }
        return partition(labels, (int) maxLabel + 1);
    }

    /**
     * Given a list of labels, separate Dataset into multiple subsets (some may
     * be empty)
     *
     * @param labels Label defining in which subset to label an entry
     * @param number Number subsets to produce, must be greater than max(labels)
     * @return Array of subsets of length max(label), where each member, i,
     * contains entries with label[i] == i.
     */
    public Dataset[] partition(int[] labels, int number) {
        if (labels.length != this.NEntries()) {
            throw new Error("Number of labels != Number of entries!");
        }
        Dataset[] subsets = new Dataset[number];
        for (int i = 0; i < number; i++) {
            subsets[i] = this.emptyClone();
        }
        int toSubset;
        for (int i = 0; i < labels.length; i++) {
            toSubset = labels[i];
            if (toSubset >= number) {
                throw new Error("number < max(labels)");
            }
            subsets[toSubset].addEntry(this.getEntry(i));
        }
        return subsets;
    }

    /**
     * Get a specific list of entries from the dataset. These entries are not
     * removed from the original dataset
     *
     * @param indicies List of entry IDs to be removed
     * @return A new dataset containing only the specified entries
     */
    public Dataset getSubset(int[] indicies) {
        Dataset output = emptyClone();
        for (int i = 0; i < indicies.length; i++) {
            output.addEntry(getEntry(indicies[i]));
        }
        return output;
    }

    /**
     * Split off a certain number of entries into a separate dataset. Deletes
     * those entries from the original set
     *
     * @param number Number of entries in new set
     * @return Dataset containing a subset of entries
     */
    public Dataset randomSplit(int number) {
        if (number < 0 || number > NEntries()) {
            throw new RuntimeException("Number must be positive, and less than the size of the set");
        }

        // Create a list of which entries to move over
        Boolean[] to_switch = new Boolean[NEntries()];
        Arrays.fill(to_switch, 0, number, true);
        Arrays.fill(to_switch, number, NEntries(), false);
        Collections.shuffle(Arrays.asList(to_switch));

        // Delete or switch, as suggested
        Dataset out = emptyClone();
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        ArrayList<BaseEntry> new_set = new ArrayList<>(number);
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            if (to_switch[id]) {
                new_set.add(e);
                iter.remove();
            }
            id++;
        }
        out.addEntries(new_set);
        return out;
    }

    /**
     * Split off a certain number of entries into a separate dataset. Deletes
     * those entries from the original set
     *
     * @param fraction Fraction of entries of original set to move to new set
     * @return Dataset containing a subset of entries
     */
    public Dataset randomSplit(double fraction) {
        if (fraction > 1 || fraction < 0) {
            throw new RuntimeException("Fraction must be between 0 and 1");
        }
        int to_new = (int) Math.floor((double) NEntries() * fraction);
        return randomSplit(to_new);
    }

    /**
     * Generate a random subset of the original data, which is left intact
     *
     * @param number Number of entries to move over
     * @return Dataset containing a subset of entries
     */
    public Dataset getRandomSubset(int number) {
        if (number < 0 || number > NEntries()) {
            throw new RuntimeException("Number must be positive, and less than the size of the set");
        }

        // Create a list of which entries to move over
        Boolean[] to_switch = new Boolean[NEntries()];
        Arrays.fill(to_switch, 0, number, true);
        Arrays.fill(to_switch, number, NEntries(), false);
        Collections.shuffle(Arrays.asList(to_switch));

        // Add to subset if desired
        Dataset out = emptyClone();
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        ArrayList<BaseEntry> new_set = new ArrayList<>(number); // Faster than adding to set
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            if (to_switch[id]) {
                new_set.add(e);
            }
            id++;
        }
        out.addEntries(new_set);
        return out;
    }

    /**
     * Generate a random subset of the original data, which is left intact
     *
     * @param fraction Fraction of entries used in new set
     * @return Dataset containing a subset of entries
     */
    public Dataset getRandomSubset(double fraction) {
        if (fraction > 1 || fraction < 0) {
            throw new RuntimeException("Fraction must be between 0 and 1");
        }
        int to_new = (int) Math.floor((double) NEntries() * fraction);
        return getRandomSubset(to_new);
    }

    /**
     * Split for threading purposes. Does not worry about randomization,
     * thread n contains all entries where [entry ID] % NThreads == 0. 
     * 
     * <p>Example: 4 threads, thread 1 has entry #1, 5, 9, ...
     * 
     * @param NThreads Number of subsets to create
     * @return Array of equally-sized Dataset objects
     */
    public Dataset[] splitForThreading(int NThreads) {
        Dataset[] output = new Dataset[NThreads];
        
        // Number to split per thread
        int to_split = NEntries() / NThreads;
        
        // Make the thread datasets
        for (int i=0; i< NThreads; i++) {
            output[i] = emptyClone();
            output[i].Entries.ensureCapacity(to_split);
        }
        
        // Fill in each thread
        for (int e=0; e<NEntries(); e++) {
            output[e % NThreads].Entries.add(Entries.get(e));
        }
        
        return output;
    }

    /**
     * Split the dataset into multiple folds for cross-validation, empties the
     * original test set
     *
     * @param folds Number of folds
     * @return Vector of independent test sets
     */
    public Dataset[] splitIntoFolds(int folds) {
        // Generate the output array
        Dataset[] output = new Dataset[folds];
        for (int i = 0; i < folds; i++) {
            output[i] = emptyClone();
        }
        if (NClasses() == 1) {
            // Generate list of entries to shuffle
            int to_split = (int) Math.floor((double) NEntries() / (double) folds);
            Integer[] to_switch = new Integer[NEntries()];
            int count = 0;
            for (int i = 1; i < folds; i++) {
                Arrays.fill(to_switch, count, count + to_split, i);
                count += to_split;
            }
            Arrays.fill(to_switch, count, NEntries(), 0);
            Collections.shuffle(Arrays.asList(to_switch));

            // Assign them to the appropriate array
            int id = 0;
            Iterator<BaseEntry> iter = Entries.iterator();
            while (iter.hasNext()) {
                BaseEntry e = iter.next();
                iter.remove(); // Remove from old set
                output[to_switch[id]].addEntry(e);
                id++;
            }
        } else {
            for (int i = 0; i < NClasses(); i++) {
                final int cls = i;
                
                // Get the entries that are in class # cls
                Predicate splitter = new Predicate() {
                    @Override
                    public boolean evaluate(Object input) {
                        BaseEntry input_obj = (BaseEntry) input;
                        return input_obj.getMeasuredClass() == cls;
                    }
                };
                Dataset split = DatasetHelper.split(this, splitter);

                // Split them into folds for cross-validation
                split.setClassNames(new String[]{"Class"});
                Dataset[] split_folds = split.splitIntoFolds(folds);
                for (Dataset S : split_folds) {
                    S.setClassNames(getClassNames());
                }

                // Add them to the output structure
                for (int j = 0; j < folds; j++) {
                    output[j].combine(split_folds[j]);
                }
            }
        }
        return output;
    }

    /**
     * Convert to Weka Instances object, delete attribute information in each
     * entry. This can be used to conserve memory when using Weka.
     *
     * @param useClass Whether to output class data. Note: If there is no measured
     * class data and useDiscreteClass is true, value will be set to Zero. This 
     * allows the Instances to contain information about how many classes are available
     * regardless of whether this Dataset contains any measurements.
     * @param useDiscreteClass Whether to treat class variable as discrete
     * @return Dataset in Weka format
     * @see Dataset#restoreAttributes(weka.core.Instances)
     */
    public Instances transferToWeka(boolean useClass, boolean useDiscreteClass) {
        // Create an array of attribute names
        ArrayList<Attribute> Attributes = new ArrayList<>();
        for (int i = 0; i < NAttributes(); i++) {
            Attribute att = new Attribute(AttributeName.get(i));
            Attributes.add(att);
        }
        if (!useClass) {
            // Do nothing  
        } else if (useDiscreteClass) {
            Attributes.add(new Attribute("Class", Arrays.asList(getClassNames())));
        } else {
            Attributes.add(new Attribute("Class"));
        }

        Instances weka_out = new Instances("Output", Attributes, NEntries());
        int j;
        for (int i = 0; i < NEntries(); i++) {
            BaseEntry entry = Entries.get(i);
            DenseInstance inst = new DenseInstance(Attributes.size());
            inst.setDataset(weka_out);
            for (j = 0; j < NAttributes(); j++) {
                inst.setValue(j, entry.getAttribute(j));
            }
            if (!useClass) {
            }// Do nothing 
            else if (useDiscreteClass) {
                if (entry.hasMeasurement()) {
                    double cls = Math.min(entry.getMeasuredClass(), NClasses() - 1);
                    cls = Math.max(0, cls);
                    inst.setValue(j, getClassName((int) cls));
                } else {
                    inst.setValue(j, getClassName(0));
                }
            } else {
                inst.setValue(j, entry.getMeasuredClass());
            }
            weka_out.add(inst);
            entry.clearAttributes();
        }
        if (useClass) {
            weka_out.setClassIndex(NAttributes());
        }
        return weka_out;
    }

    /**
     * Restore attribute data to each entry.
     *
     * @param weka Weka object containing attribute information. Assumes last
     * variable has class variable
     * @throws java.lang.Exception
     * @see Dataset#transferToWeka(boolean, boolean)
     */
    public void restoreAttributes(Instances weka) throws Exception {
        // Check input
        if (weka.numInstances() != NEntries()) {
            throw new Exception("Wrong number of entries");
        }
        boolean hasClass = weka.classIndex() >= 0;
        if ((weka.numAttributes() != NAttributes() && !hasClass)
                || (weka.numAttributes() - 1 != NAttributes() && hasClass)) {
            throw new Exception("Wrong number of attributes");
        }

        // Transfer data
        Iterator<Instance> iter = weka.iterator();
        for (int i = 0; i < this.NEntries(); i++) {
            Entries.get(i).clearAttributes();
            Instance inst = iter.next();
            double[] attr = inst.toDoubleArray();
            iter.remove();
            if (!hasClass) {
                Entries.get(i).addAttributes(attr);
            } else {
                Entries.get(i).addAttributes(Arrays.copyOf(attr, NAttributes()));
            }
        }
    }

    /**
     * Output the attributes and class of each entry
     *
     * @return Array where the last column is the measured class variable (0 if
     * no measured)
     */
    public double[][] getEntryArray() {
        double[][] output = new double[NEntries()][NAttributes() + 1];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            System.arraycopy(e.getAttributes(), 0, output[id], 0, NAttributes());
            output[id][NAttributes()] = e.hasMeasurement() ? e.getMeasuredClass() : 0;
            id++;
        }
        return output;
    }

    /**
     * Output the attributes of each entry into an array
     *
     * @return Array of attributes
     */
    public double[][] getAttributeArray() {
        double[][] output = new double[NEntries()][NAttributes()];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            System.arraycopy(e.getAttributes(), 0, output[id], 0, NAttributes());
            id++;
        }
        return output;
    }

    /**
     * Output a single attribute for each entry
     *
     * @param Attribute Which Attribute to output
     * @return Array of attribute values
     */
    public double[] getSingleAttributeArray(int Attribute) {
        double[] output = new double[NEntries()];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            output[id] = e.getAttribute(Attribute);
            id++;
        }
        return output;
    }

    /**
     * Output an array of the measured classes for each entry
     *
     * @return 1D double array containing measured classes
     */
    public double[] getMeasuredClassArray() {
        if (!Entries.iterator().next().hasMeasurement()) {
            throw new Error("Entries have no measured class");
        }
        double[] output = new double[NEntries()];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            if (e.hasMeasurement()) {
                output[id] = e.getMeasuredClass();
            } else {
                throw new Error("Entry " + id + " does not have a measured class variable");
            }
            id++;
        }
        return output;
    }

    /**
     * Get the predicted class for each entry
     *
     * @return 1D double array containing measured classes
     */
    public double[] getPredictedClassArray() {
        if (!Entries.iterator().next().hasPrediction()) {
            throw new Error("Entries have no predicted class");
        }
        double[] output = new double[NEntries()];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            if (e.hasPrediction()) {
                output[id] = e.getPredictedClass();
            } else {
                throw new Error("Entry " + id + " does not have a predicted class variable");
            }
            id++;
        }
        return output;
    }

    /**
     * Get an array of class probabilities
     *
     * @return Probabilities of each entry being in each class
     */
    public double[][] getClassProbabilityArray() {
        if (!Entries.iterator().next().hasPrediction()) {
            throw new Error("Entries have no predicted class");
        }
        double[][] output = new double[NEntries()][NClasses()];
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            output[id] = e.getClassProbilities();
            id++;
        }
        return output;
    }
    
    /**
     * Delete all measured classes from entry from entries
     */
    public void deleteMeasuredClasses() {
        for (BaseEntry entry : Entries) {
            entry.deleteMeasuredClass();
        }
    }

    /**
     * Set predicted class for each entry, given an array of predictions
     *
     * @param predictions Predictions in the same order as generated by
     * getFeatures
     */
    public void setPredictedClasses(double[] predictions) {
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            e.setPredictedClass(predictions[id]);
            id++;
        }
    }

    /**
     * Set measured class for each entry, given an array of measurements
     *
     * @param measurements Measurements in the same order as generated by
     * getFeatures
     */
    public void setMeasuredClasses(double[] measurements) {
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            e.setMeasuredClass(measurements[id]);
            id++;
        }
    }

    /**
     * Set class probabilities for each entry
     *
     * @param predictions Probabilities in the same order as generated by
     * getFeatures
     */
    public void setClassProbabilities(double[][] predictions) {
        int id = 0;
        Iterator<BaseEntry> iter = Entries.iterator();
        while (iter.hasNext()) {
            BaseEntry e = iter.next();
            e.setClassProbabilities(predictions[id]);
            id++;
        }
    }

    @Override
    public String about() {
        String output = "Number of entries:  " + NEntries();
        output += " - Number of features: " + NAttributes();
        return output;
    }

    /**
     * Print out name of dataset and what attributes are generated.
     * @param htmlFormat Whether to print in HTML format
     * @return String describing this dataset
     */
    @Override
    public String printDescription(boolean htmlFormat) {
        // Print out class name
        String output = "";
        if (htmlFormat) {
            output += "<label>";
        }
        output += "Dataset Type";
        if (htmlFormat) {
            output += "</label> ";
        } else {
            output += ": ";
        }
        output += getClass().getName() + "\n";
        
        // Print out entry description
        if (htmlFormat) {
            output += "<br><label>";
        }
        output += "Entry Description";
        if (htmlFormat) {
            output += "</label> ";
        } else {
            output += ": ";
        }
        output += printEntryDescription(htmlFormat) + "\n";
        
        // Print out what attributes are generated by default
        String defaultAttr = printAttributeDescription(htmlFormat);
        if (defaultAttr.length() > 1) {
            // Print out header
            if (htmlFormat) {
                output += "<h3>";
            }
            output += "Default Attributes";
            if (htmlFormat) {
                output += "</h3>";
            }
            output += "\n";
            
            // Print out description
            if (htmlFormat) {
                output += "<p>";
            }
            output += defaultAttr + "\n";
        }
        
        // Print out attribute generators
        if (Generators.size() > 0) {
            // Print out header
            if (htmlFormat) {
                output += "<h4>";
            }
            output += "Attribute Generators";
            if (htmlFormat) {
                output += "</h4>";
            }
            output += "\n";
            
            // Print out start of HTML list
            if (htmlFormat) {
                output += "<ol>\n";
            }
        }
        
        for (int i=0; i<Generators.size(); i++) {
            // Print out description of the generator
            if (htmlFormat) {
                output += "<li> ";
            } else {
                output += i + ". ";
            }
            output += Generators.get(i).printDescription(htmlFormat);
            if (htmlFormat) {
                output += "</li>";
            }
            output += "\n";
        }
        
        if (Generators.size() > 0 && htmlFormat) {
            output += "</ol>\n";
        }
        
        // Print out attribute expanders
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        // Initialize output
        List<Pair<String, Citation>> output = new ArrayList<>();
        
        // Get citations from attribute generators
        for (BaseAttributeGenerator gen : Generators) {
            if (gen instanceof Citable) {
                Citable itf = (Citable) gen;
                output.addAll(itf.getCitations());
            }
        }
        
        // Get citations from attribute expanders
        for (BaseAttributeExpander exp : Expanders) {
            if (exp instanceof Citable) {
                Citable itf = (Citable) exp;
                output.addAll(itf.getCitations());
            }
        }
        
        return output;
    }
    
    /**
     * Print out description of attributes. 
     * 
     * <p><b>Implementation Guide</b>
     * <p>Subclasses should describe what kind of attributes are generated 
     * <i>by default</i>. If it uses a separate generator class, those are captured
     * by the {@linkplain #printDescription(boolean) } section.
     * @param htmlFormat Whether to print in HTML format
     * @return Description of the attributes or an empty string ("") if there
     * are no default attributes.
     */
    public String printAttributeDescription(boolean htmlFormat) {
        return "";
    }
    
    /**
     * Print out what the entries to this dataset are.
     * @param htmlFormat Whether to print in HTML format
     * @return Description of each entry (i.e., what kind of data is this).
     */
    public String printEntryDescription(boolean htmlFormat) {
        return "A list of ordinary numbers";
    }

    @Override
    public String toString() {
        String output = "Number of entries:  " + NEntries();
        output += "\nNumber of features: " + NAttributes();
        return output;
    }

    /**
     * Get the distribution of entries between known classes
     *
     * @return Number of entries of each class
     */
    public int[] getDistributionCount() {
        int[] output = new int[NClasses()];
        if (NClasses() == 1) {
            output[0] = NEntries();
            return output;
        }
        for (BaseEntry Entry : Entries) {
            output[(int) Entry.getMeasuredClass()]++;
        }
        return output;
    }

    /**
     * Get the distribution of entries between known classes
     *
     * @return Fraction of entries of each class
     */
    public double[] getDistribution() {
        double[] output = new double[NClasses()];
        if (NClasses() == 1) {
            output[0] = 1.0;
            return output;
        }
        int[] count = new int[NClasses()];
        for (BaseEntry Entry : Entries) {
            count[(int) Entry.getMeasuredClass()]++;
        }
        for (int i = 0; i < NClasses(); i++) {
            output[i] = (double) count[i] / (double) NEntries();
        }
        return output;
    }

    /**
     * Print out the distribution of entries in the known classes
     *
     * @return Distribution as a String
     */
    public String printDistribution() {
        if (ClassName.length == 1) {
            return "All entries in single class: " + ClassName[0];
        }
        String output = "";
        double[] dist = getDistribution();
        for (int i = 0; i < NClasses(); i++) {
            output += String.format("%s (%.2f%%) ", ClassName[i], dist[i] * 100.0);
        }
        return output;
    }

    /**
     * Print out data regarding a list of entries. Format:<br>
     * <center>ID, Entry, Measured Class, Predicted Class, Class
     * Probabilities</center>
     *
     * @param list ID numbers of entries to be printed.
     * @return Desired information as a String
     */
    public String printEntries(int[] list) {
        String output = "";
        // Print out a header
        output += "ID\tEntry\tMeasuredClass\tPredictedClass";
        if (NClasses() > 1) {
            output += "\tClassProbabilities";
        }
        output += "\n";
        // Print out each entry
        for (int i = 0; i < list.length; i++) {
            output += String.format("%d\t%s\t", list[i], Entries.get(list[i]));
            if (Entries.get(list[i]).hasMeasurement()) {
                output += String.format("%.3f\t", Entries.get(list[i]).getMeasuredClass());
            } else {
                output += "None\t";
            }
            if (Entries.get(list[i]).hasPrediction()) {
                output += String.format("%.3f\t", Entries.get(list[i]).getPredictedClass());
            } else {
                output += "None\t";
            }
            if (NClasses() > 1) {
                double[] probs = Entries.get(i).getClassProbilities();
                output += String.format("(%.3f", probs[0]);
                for (int j = 1; j < NClasses(); j++) {
                    output += String.format(",%.3f", probs[j]);
                }
                output += ")";
            }
            output += "\n";
        }
        return output;
    }

    /**
     * Save the state of this object using serialization
     *
     * @param filename Filename for output
     */
    public void saveState(String filename) {
        UtilityOperations.saveState(this, filename);
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        switch (Command.get(0).toLowerCase()) {
            case "details":
                return this.toString();
            case "dist":
                return this.printDistribution();
            default:
                throw new Exception("ERROR: Print command \"" + Command.get(0)
                        + "\" not recognized");

        }
    }

    @Override
    public String saveCommand(String Basename, String Command) throws Exception {
        switch (Command) {
            case "csv": // Save as CSV file
                DatasetOutput.saveDelimited(this, Basename + ".csv", ",");
                return Basename + ".csv";
            case "arff": // Save as an ARFF
                DatasetOutput.saveARFF(this, Basename + ".arff");
                return Basename + ".arff";
            case "stats": // Save for statistics (only: name, predicted, measured)
                DatasetOutput.printForStatistics(this, Basename + ".csv");
                return Basename + ".csv";
            default:
                throw new Exception("ERROR: Save command \"" + Command
                        + "\" not recognized");
        }
    }

    @Override
    public Object runCommand(List<Object> command) throws Exception {
        if (command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        
        String Action = command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "add": {
                if (command.size() == 1) {
                    throw new Exception("Usage: \"add $<dataset> [-force]\" "
                            + "or \"add <entries...>\"");
                }
                if (command.get(1) instanceof Dataset) {
                    Dataset data = (Dataset) command.get(1);
                    boolean force = false;
                    if (command.size() == 3) {
                        if (command.get(2).toString().equalsIgnoreCase("-force")) {
                            force = true;
                        } else {
                            throw new Exception("Available options: -force");
                        }
                    } else if (command.size() > 3) {
                        throw new Exception("Usage: add $<dataset> [-force]");
                    }
                    addEntries(data, force);
                    System.out.println("\tAdded " + data.NEntries() +
                            " entries from another dataset.");
                    return null;
                } else {
                    int nAdded = 0;
                    for (Object entry : command.subList(1, command.size())) {
                        try {
                            addEntry(entry.toString());
                            nAdded++;
                        } catch (Exception ex) {
                            System.err.println(entry.toString() +
                                    " failed to parse: " + ex.getMessage());
                        }
                    }
                    System.out.println("\tAdded " + nAdded + " entries.");
                    return null;
                }
            }
            case "attributes": case "attr":
                return runAttributeCommand(command.subList(1, command.size()));
            case "clone":
                // Usage: <output> = clone [-emptyy]
                if (command.size() == 1) {
                    return clone();
                } else if (command.get(1).toString().equalsIgnoreCase("-empty")) {
                    return emptyClone();
                } else {
                    throw new Exception("Usage: clone [-empty]");
                }
            case "combine": {
                try {
                    if (command.size() != 2) {
                        throw new Exception();
                    }
                    Dataset other = (Dataset) command.get(1);
                    combine(other);
                    System.out.format("\tAdded %d entries. New size: %d\n", 
                            other.NEntries(), NEntries());
                } catch (Exception e) {
                    throw new Exception("Usage: combine $<other dataset>");
                }
            } break;
            case "filter": {
                // Usage: <include|exclude> <method> [<options...>]
                String Method;
                List<Object> Options;
                boolean Exclude;
                try {
                    if (command.get(1).toString().toLowerCase().startsWith("ex")) {
                        Exclude = true;
                    } else if (command.get(1).toString().toLowerCase().startsWith("in")) {
                        Exclude = false;
                    } else {
                        throw new Exception();
                    }
                    Method = command.get(2).toString();
                    if (Method.equals("?")) {
                        System.out.println(printImplmentingClasses(BaseDatasetFilter.class, false));
                        return null;
                    }
                    Options = command.subList(3, command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> filter <exclude|include> <method> <options...>");
                }
                BaseDatasetFilter Filter = (BaseDatasetFilter) instantiateClass("data.utilities.filters." + Method, Options);
                Filter.setExclude(Exclude);
                Filter.train(this);
                Filter.filter(this);
                System.out.println("\tFiltered using a " + Method + ". New size: " + NEntries());
            }
            break;
            case "generate": {
                // Generate new entries. Usage: generate <method> [<options...>]
                String Method = "";
                List<Object> MethodOptions;
                try {
                    Method = command.get(1).toString();
                    if (Method.equalsIgnoreCase("?")) {
                        System.out.println("Available Entry Generators");
                        System.out.println(printImplmentingClasses(BaseEntryGenerator.class, false));
                        return null;
                    }
                    MethodOptions = command.subList(2, command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: generate <method> [<options...>]");
                }
                BaseEntryGenerator generator = (BaseEntryGenerator) instantiateClass("data.utilities.generators." + Method, MethodOptions);
                int initialCount = this.NEntries();
                generator.addEntriesToDataset(this);
                System.out.println(String.format("\tGenerated %d new entries with a %s. Total Count: %s",
                        NEntries() - initialCount, Method, NEntries()));
            }
            break;
            case "import": {
                // Usage: import <filename> [<options...>]
                String filename = command.get(1).toString();
                Object[] options = command.subList(2, command.size()).toArray();
                importText(filename, options);
                System.out.println("\tImported " + NEntries() + " entries");
            }
            break;
            case "match": {
                // Usage: match $<dataset> <number to print>
                Dataset toMatch;
                int numToPrint;
                
                // Parse input
                try {
                    toMatch = (Dataset) command.get(1);
                    numToPrint = Integer.parseInt(command.get(2).toString());
                    if (command.size() != 3) {
                        throw new Exception();
                    }
                } catch (Exception e) {
                    throw new Exception("Usage: $<dataset> <num to print>");
                }
                
                // Run matching
                for (BaseEntry entry : toMatch.getEntries()) {
                    // Find matches
                    List<BaseEntry> matches = matchEntries(entry, numToPrint);
                    
                    // Print results
                    System.out.println("Matches for " + entry.toString() + ":");
                    for (BaseEntry match : matches) {
                        System.out.println("\t" + match.toString());
                    }
                    System.out.println();
                }
            } break;
            case "modify": {
                if (command.size() < 2) {
                    throw new Exception("Usage: <dataset> modify <method> <options>");
                }
                // Get command
                String Method = command.get(1).toString();
                if (Method.equals("?")) {
                    System.out.println(printImplmentingClasses(BaseDatasetModifier.class, false));
                    return null;
                }
                // Get options
                List<Object> Options = command.subList(2, command.size());
                // Modify the Dataset
                BaseDatasetModifier Mdfr = (BaseDatasetModifier) instantiateClass("data.utilities.modifiers." + Method, Options);
                Mdfr.transform(this);
                System.out.println("\tModified dataset using a " + Method);
            }
            break;
            case "rank": {
                // Usage: <number> <max|min> <meas|pred> <method> <options...>
                boolean measured = true;
                boolean maximize = true;
                int numberToPrint = -1;
                String Method;
                List<Object> Options;
                try {
                    numberToPrint = Integer.parseInt(command.get(1).toString());
                    if (command.get(2).toString().toLowerCase().startsWith("max")) {
                        maximize = true;
                    } else if (command.get(2).toString().toLowerCase().contains("min")) {
                        maximize = false;
                    } else {
                        throw new Exception();
                    }
                    if (command.get(3).toString().toLowerCase().startsWith("mea")) {
                        measured = true;
                    } else if (command.get(3).toString().toLowerCase().startsWith("pre")) {
                        measured = false;
                    } else {
                        throw new Exception();
                    }
                    // Get Method and its options
                    Method = command.get(4).toString();
                    if (Method.equalsIgnoreCase("?")) {
                        System.out.println("Available EntryRankers:");
                        System.out.println(printImplmentingClasses(
                                BaseEntryRanker.class, false));
                        return null;
                    }
                    Options = command.subList(5, command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> rank <number> <maximum|minimum> <measured|predicted> <method> [<options>]");
                }
                BaseEntryRanker ranker = (BaseEntryRanker) instantiateClass(
                        "optimization.rankers." + Method, Options);
                ranker.setMaximizeFunction(maximize);
                ranker.setUseMeasured(measured);
                ranker.train(this);
                System.out.println(DatasetOutput.printTopEntries(this, ranker, numberToPrint));
            }
            break;
            case "subset":
            case "split": {
                // Usage: split|subset <fraction|number> = <output>
                double size;
                try {
                    size = Double.parseDouble(command.get(1).toString());
                } catch (Exception e) {
                    throw new Exception("Usage: " + Action + " <fraction|number> = <output>");
                }
                Dataset output;
                if (Action.toLowerCase().startsWith("sub")) {
                    output = size >= 1 ? getRandomSubset((int) size) : getRandomSubset(size);
                    System.out.println("\tGenerated a subset containing " + output.NEntries() + " entries.");
                } else {
                    output = size >= 1 ? randomSplit((int) size) : randomSplit(size);
                    System.out.println("\tSplit off " + output.NEntries() + " entries from dataset");
                }
                return output;
            }
            default:
                throw new Exception("ERROR: Dataset command not recognized: " + Action);
        }
        return null;
    }

    /**
     * Run commands related to attributes of each entry. Starts with the action
     * to perform on the attributes
     *
     * @param Command Operation to be run on/about attributes
     * @return Any output (null if nothing is created)
     * @throws Exception On any error
     */
    protected Object runAttributeCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.print("Attributes contained within dataset:\n");
            for (int i = 0; i < NAttributes(); i++) {
                System.out.format("%32s", AttributeName.get(i));
                if (i % 2 == 1) {
                    System.out.println();
                }
            }
            if (NAttributes() % 2 == 1) {
                System.out.println();
            }
            return null;
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "expanders": {
                runAttributeExpansionCommand(Command.subList(1, Command.size()));
            }
            break;
            case "generators": {
                runAttributeGeneratorCommand(Command.subList(1, Command.size()));
            }
            break;
            case "generate":
                // Usage: generate
                if (Command.size() > 1) {
                    throw new Exception("Usage: <dataset> generate");
                }
                generateAttributes();
                System.out.println("\tGenerated " + NAttributes() + " attributes.");
                break;
            case "rank": {
                // Usage: <number> 
                String Method;
                int NumToPrint;
                List<Object> MethodOptions;
                try {
                    if (Command.get(1) instanceof Integer) {
                        NumToPrint = (Integer) Command.get(1);
                    } else {
                        NumToPrint = Integer.parseInt(Command.get(1).toString());
                    }
                    Method = Command.get(2).toString();
                    if (Method.equals("?")) {
                        System.out.println(printImplmentingClasses(BaseAttributeEvaluator.class, false));
                        return null;
                    }
                    MethodOptions = Command.subList(3, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> attributes rank <number> <method> [<method options...>]");
                }
                BaseAttributeEvaluator Evaluator = (BaseAttributeEvaluator) instantiateClass("attributes.evaluators." + Method, MethodOptions);
                System.out.print(Evaluator.printRankings(this, NumToPrint));
            }
            break;
            default:
                throw new Exception("ERROR: Dataset attribute command not recognized" + Action);
        }
        return null;
    }

    /**
     * Run commands relating to expanding the attribute pool.
     *
     * @param Command Attribute expansion command (e.g., "run")
     * @throws Exception
     */
    protected void runAttributeExpansionCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Available attribute expansion commands: run, add, clear");
        }
        String action = Command.get(0).toString().toLowerCase();

        switch (action) {
            case "add":
                // Usage: add <method> <options...>
                String Method;
                List<Object> Options;
                try {
                    Method = Command.get(1).toString();
                    if (Method.equals("?")) {
                        System.out.println(printImplmentingClasses(BaseAttributeExpander.class, false));
                        return;
                    }
                    Options = Command.subList(2, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> expand <method> <options...>");
                }
                BaseAttributeExpander expander = (BaseAttributeExpander) instantiateClass("attributes.expanders." + Method, Options);
                addAttribueExpander(expander);
                System.out.println("\tAdded a " + Method + " to list of attribute expanders");
                break;
            case "clear":
                clearAttributeExpanders();
                System.out.println("\tCleared list of attribute expanders.");
                break;
            case "run":
                runAttributeExpanders();
                System.out.println("\tExpanded number of attributes to " + NAttributes());
                break;
            default:
                throw new Exception("Attribute expansion command not recognized: " + action);
        }
    }
    
    /**
     * Run commands relating to generating new attributes.
     *
     * @param Command Attribute expansion command (e.g., "run")
     * @throws Exception
     */
    protected void runAttributeGeneratorCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Available attribute generator commands: run, add, clear");
        }
        String action = Command.get(0).toString().toLowerCase();

        switch (action) {
            case "add":
                // Usage: add <method> <options...>
                String Method;
                List<Object> Options;
                try {
                    Method = Command.get(1).toString();
                    if (Method.equals("?")) {
                        System.out.println(printImplmentingClasses(BaseAttributeGenerator.class, false));
                        return;
                    }
                    Options = Command.subList(2, Command.size());
                } catch (Exception e) {
                    throw new Exception("Usage: <dataset> expand <method> <options...>");
                }
                BaseAttributeGenerator generator = (BaseAttributeGenerator) instantiateClass("attributes.generators." + Method, Options);
                addAttribueGenerator(generator);
                System.out.println("\tAdded a " + Method + " to list of attribute generators");
                break;
            case "clear":
                clearAttributeGenerators();
                System.out.println("\tCleared list of attribute generators.");
                break;
            case "run":
                int oldCount = NAttributes();
                runAttributeGenerators();
                System.out.format("\tGenerated %d new attributes\n", NAttributes() - oldCount);
                break;
            default:
                throw new Exception("Attribute generator command not recognized: " + action);
        }
    }

    /**
     * Run after generating attributes. Performs some operations to reduce the
     * amount of memory used.
     */
    protected void finalizeGeneration() {
        System.gc();
    }
}
