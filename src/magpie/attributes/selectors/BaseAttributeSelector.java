/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
 * define a method to train the selector and one to apply it to another dataset.<p>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * <p><b>Usage: train $&lt;dataset></b> - Train an attribute selector<br>
 * <i>dataset</i>: {@linkplain Dataset} used to train selector
 * 
 * <p><b>Usage: run $&lt;dataset></b> - Reduce number of attributes in a dataset<br>
 * <i>dataset</i>: {@linkplain Dataset} to run selector on
 * 
 * @author Logan Ward 
 * @version 0.1
 */
abstract public class BaseAttributeSelector implements java.io.Serializable, 
        java.lang.Cloneable, Options, Printable, Commandable {
    /** Whether this BaseAttributeSelector has been trained */
    protected boolean trained = false;
    /** List of attributes that were selected */
    protected List<Integer> Attribute_ID = new LinkedList<>();
    /** Names of attributes that were selected */
    final private List<String> Attribute_Names = new LinkedList<>();
    
    /** 
     * Train an attribute selection algorithm on a dataset
     * @param Data Data used for training purposes
     */
    public void train(Dataset Data) {
        Attribute_ID.clear();
        train_protected(Data);
        trained = true;
        // Store the names
        Attribute_Names.clear();
        Iterator<Integer> iter = Attribute_ID.iterator();
        while (iter.hasNext())
            Attribute_Names.add(Data.AttributeName.get(iter.next()));
    }
    
    /**
     * @return Whether this selector has been trained 
     */
    public boolean isTrained() { return trained; }
    
    /**
     * Operation that actually does the work for training. Must generate a list 
     *  of attribute IDs and store it in {@link #Attribute_ID}.
     */
    abstract protected void train_protected(Dataset Data);
    
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
        for (Integer Attribute_ID1 : Attribute_ID) {
            NewAttributeNames.add((String) Data.AttributeName.get(Attribute_ID1));
        }
        Data.AttributeName = NewAttributeNames;
        
        // For each entry, redo the feature list
        for (int i=0; i<Data.NEntries(); i++) {
            double[] newAttributes = new double[Data.NAttributes()];
            for (int j=0; j<Data.NAttributes(); j++)
                newAttributes[j] = Data.getEntry(i).getAttribute(Attribute_ID.get(j));
            Data.getEntry(i).setAttributes(newAttributes);
        }
    }    

    @Override public BaseAttributeSelector clone() {
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
