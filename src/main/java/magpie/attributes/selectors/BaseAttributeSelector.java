package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import magpie.data.Dataset;
import magpie.utility.interfaces.Commandable;
import magpie.utility.interfaces.Options;
import magpie.utility.interfaces.Printable;

/**
 * Interface to select a subset of attributes for a dataset. Implementations must
 * define a method to select a subset of attribute:<p>
 * 
 * <ul>
 * <li>{@linkplain #train_protected(magpie.data.Dataset) } - Select attributes, return list of their indexes
 * <li>{@linkplain #setOptions(java.util.List) } - Set any options for your implementation. Make sure
 *  to make them settable from the Java interface as well.
 * </ul>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * <command><p><b>train $&lt;dataset></b> - Train an attribute selector. Selects
 *  a subset of attributes.
 * <br><pr><i>dataset</i>: {@linkplain Dataset} used to train selector</command>
 * 
 * <command><p><b>run $&lt;dataset></b> - Reduce number of attributes in a dataset
 * <br><pr><i>dataset</i>: {@linkplain Dataset} to run selector on</command>
 * 
 * @author Logan Ward 
 * @version 0.1
 */
abstract public class BaseAttributeSelector implements java.io.Serializable, 
        java.lang.Cloneable, Options, Printable, Commandable {
    /** Whether this BaseAttributeSelector has been trained */
    protected boolean trained = false;
    /** List of attributes that were selected */
    private List<Integer> Attribute_ID = new LinkedList<>();
    /** Names of attributes that were selected */
    final private List<String> Attribute_Names = new LinkedList<>();
    
    /** 
     * Train an attribute selection algorithm on a dataset. Selects which attributes
     *  will be used, but does not reduce the dataset (see {@linkplain #run(magpie.data.Dataset) }).
     * @param Data Data used for training purposes
     */
    public void train(Dataset Data) {
        Attribute_ID.clear();
        Attribute_ID.addAll(train_protected(Data));
        trained = true;
        // Store the names
        Attribute_Names.clear();
        Iterator<Integer> iter = Attribute_ID.iterator();
        String[] attributeName = Data.getAttributeNames();
        while (iter.hasNext()) {
            Attribute_Names.add(attributeName[iter.next()]);
        }
    }
    
    /**
     * @return Whether this selector has been trained 
     */
    public boolean isTrained() { return trained; }
    
    /**
     * Operation that actually does the work for training. Must generate a list 
     *  of attribute IDs
     * @param Data Dataset used to train selector
     * @return List of indexes of selected attributes
     */
    abstract protected List<Integer> train_protected(Dataset Data);
    
    /**
     * Adjust the attribute list of a dataset, based on a trained selection algorithm
     * @param Data Dataset to be filtered
     */
    public void run(Dataset Data) {
        if (! trained) 
            throw new Error("AttributeSelector not trained");
        if (Attribute_ID.isEmpty())
            throw new Error("No attributes selected.");
        
        // Get the new list of feature names
        ArrayList<String> NewAttributeNames = new ArrayList<>(Attribute_ID.size());
        String[] oldAttributeNames = Data.getAttributeNames();
        for (Integer id : Attribute_ID) {
            NewAttributeNames.add((String) oldAttributeNames[id]);
        }
        Data.setAttributeNames(NewAttributeNames);
        
        // For each entry, redo the feature list
        for (int i=0; i<Data.NEntries(); i++) {
            double[] newAttributes = new double[Data.NAttributes()];
            for (int j=0; j<Data.NAttributes(); j++)
                newAttributes[j] = Data.getEntry(i).getAttribute(Attribute_ID.get(j));
            Data.getEntry(i).setAttributes(newAttributes);
        }
    }    

    @Override
    public BaseAttributeSelector clone() {
        BaseAttributeSelector x;
        try { x = (BaseAttributeSelector) super.clone(); }
        catch (CloneNotSupportedException c) { throw new Error(c); }
        x.Attribute_ID = new LinkedList<>(this.Attribute_ID);
        return x;
    }
    
    /**
     * This operation prints out the names of attributes that were selected
     * @return Formatted string containing a table of attribute names
     */
    public String printSelections() {
        if (! isTrained() )
            return "AttributeSelector has not been trained.";
        String output = "Selected attributes:\n";
        int i=0;
        Iterator<String> iter = Attribute_Names.iterator();
        while (iter.hasNext()) {
            output += String.format("%32s", iter.next());
            if (i % 2 == 1) 
                output += "\n";
            i++;
        }
        if (i % 2 == 1) output += "\n";
        return output;
    }

    @Override
    public String about() {
        if (trained)
            return "Trained: true - Number of attributes: " + Attribute_ID.size();
        else 
            return "Trained: false";
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        // There are no print options, just print all selected attributes
        if (Command.isEmpty())
            return printSelections();
        else 
            throw new Exception("ERROR: Print command not recognized: " + Command.get(0));
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about()); return null;
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "train": case "subset": {
                // Usage: train $<dataset>
                // Train an attribute selector
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: train $<dataset>");
                }
                train(Data);
                System.out.println(printSelections());
            } break;
            case "run": {
                // Usage: run $<dataset>
                // Use attribute selector on dataset
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: train $<dataset>");
                }
                run(Data);
                System.out.println("\tRan attribute selector on " + Data.NEntries() + " entries.");
            } break;
            default: 
                throw new Exception("ERROR: Attribute selection command " + Action
                        + " not recognized.");
        }
        return null;
    }
}
