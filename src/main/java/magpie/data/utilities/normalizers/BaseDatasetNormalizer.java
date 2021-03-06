
package magpie.data.utilities.normalizers;

import java.io.Serializable;
import java.util.*;
import magpie.data.Dataset;
import magpie.utility.interfaces.*;

/**
 * Base class for objects designed to scale attributes and or class variable
 *  to exist on the same range. By default, one of these objects will
 *  normalize the attributes of each entry but not the class variable. 
 * 
 * <p>Will only normalize class variable if a measure class is present. If it is 
 * present, this class will normalize both the measured and predicted class variable
 * using the same function. Will throw an error if you try to normalize discrete classes.
 * 
 * <p>Any implementation must provide the following operations:
 * 
 * <ol>
 * <li>{@link #trainOnAttributes(magpie.data.Dataset)} and {@linkplain #trainOnMeasuredClass(magpie.data.Dataset) }
 * <li>{@link #normalizeAttributes(magpie.data.Dataset)} and {@link #normalizeClassVariable(magpie.data.Dataset)}
 * <li>{@link #restoreAttributes(magpie.data.Dataset)} and {@linkplain #restoreClassVariable(magpie.data.Dataset) }
 * </ol>
 * 
 * <p>Once you implement these functions, make sure to test them using the "test" command.
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>normalize $&lt;dataset&gt;</b> - Normalize attributes and/or class variable of a dataset
 * <pr><br><i>dataset</i>: Dataset to be normalized
 * <br>Must be trained first
 * </command>
 * 
 * <command><p><b>restore $&lt;dataset&gt;</b> - Restore attributes from normalized
 *  to original ranges
 * <pr><br><i>dataset</i>: Dataset to be restored</command>
 * 
 * <command><p><b>train [attributes] [class] $&lt;dataset&gt;</b> - Train normalizer on a dataset
 * <pr><br><i>attributes</i>: Train to normalizing attributes
 * <pr><br><i>class</i>: Train for normalizing class variable
 * <pr><br><i>dataset</i>: Dataset to use for training</command>
 * 
 * <command><p><b>test $&lt;dataset&gt;</b> - Test whether restoration actually maps 
 * dataset back onto original ranges.
 * <pr><br><i>dataset</i>: Dataset used for testing
 * <br>This operation is only used for debugging purposes.</p></command>
 * 
 * @author Logan Ward
 */
abstract public class BaseDatasetNormalizer 
    implements Options, Commandable, Printable, Serializable, Cloneable {
    /** Whether this normalizer has been trained */
    private boolean Trained = false;
    /** Whether to normalize attributes */
    private boolean NormalizeAttributes = true;
    /** Whether to class variable */
    private boolean NormalizeClass = false;
    /** Names of attributes (to make sure the dataset is not different) */
    private String[]  AttributeNames = null;

    @Override
    public BaseDatasetNormalizer clone() {
        try {
            BaseDatasetNormalizer x = (BaseDatasetNormalizer) super.clone();
            x.AttributeNames = AttributeNames.clone();
            return x;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return Whether this normalizer has been trained
     */
    public boolean isTrained() {
        return Trained;
    }
    
    /**
     * @return Whether attributes will be normalized
     */
    public boolean willNormalizeAttributes() {
        return NormalizeAttributes;
    }
    
    /**
     * @return Whether class variable will be normalized
     */
    public boolean willNormalizeClass() {
        return NormalizeClass;
    }
    
    /**
     * Set whether to normalize attributes
     * @param input Whether attributes should be normalized
     */
    public void setToNormalizeAttributes(boolean input) {
        NormalizeAttributes = input;
        Trained = false;
    }
    
    /**
     * Set whether to normalize class variable
     * @param input Whether class variable should be normalized.
     */
    public void setToNormalizeClass(boolean input) {
        NormalizeClass = input;
        Trained = false;
    }
    
    /**
     * Train the normalizer. Calculates all parameters necessary to transform the 
     *  data from the original range to a standardized range and back.
     * @param Data Training set
     */
    public void train(Dataset Data) {
        if (willNormalizeClass()) {
            // Check if data has discrete classes
            if (Data.NClasses() != 1) {
                throw new RuntimeException("Discrete class variables cannot be normalized");
            }
            // Check if the data has a measured class variable
            if (! Data.getEntry(0).hasMeasurement()) {
                setToNormalizeClass(false); // Nothing to train against
            }
        }
        
        if (willNormalizeAttributes()) {
            trainOnAttributes(Data);
        } 
        if (willNormalizeClass()) {
            trainOnMeasuredClass(Data);
        }
        Trained = true;
        AttributeNames = Data.getAttributeNames();
    }
    
    /**
     * Perform the actual training work for attributes. Must establish the parameters 
     *  necessary to normalize attributes <i>and</i> return them to their original range.
     * @param Data Training set
     */
    abstract protected void trainOnAttributes(Dataset Data);
    
    /**
     * Perform the actual training work for the class variable. Must establish the parameters 
     *  necessary to normalize attributes <i>and</i> return them to their original range.
     * 
     * <p><b>NOTE:</b> Use the <b><u>measured</u></b> class variable to train normalizer.
     * @param Data Training set
     */
    abstract protected void trainOnMeasuredClass(Dataset Data);
    
    /**
     * Transform attributes from original to standardized range. 
     * 
     * @param Data Dataset to be transformed
     */
    public void normalize(Dataset Data) {
        // Check if it is trained
        if (!isTrained()) {
            throw new RuntimeException("Normalizer not trained");
        }
        
        // Check if attributes are different
        if (! Arrays.equals(AttributeNames, Data.getAttributeNames())) {
            throw new RuntimeException("Attribute names different: Different type of data?");
        }
        
        // Run the normalization
        if (willNormalizeAttributes()) {
            normalizeAttributes(Data);
        } 
        if (willNormalizeClass()) {
            normalizeClassVariable(Data);
        }
    }
    
    /**
     * Perform the actual normalization on the attributes
     * 
     * @param Data Dataset to be transformed
     */
    abstract protected void normalizeAttributes(Dataset Data);
    
    /**
     * Perform the actual normalization on the class variable. Predicted and measured
     *  class variables must be transformed the same way
     * 
     * @param Data Dataset to be transformed
     */
    abstract protected void normalizeClassVariable(Dataset Data);
    
    /**
     * Restore attributes from scaled to original range.
     * @param Data Dataset to be transformed
     */
    public void restore(Dataset Data) {
        // If it isn't trained, throw an error
        if (!isTrained()) {
            throw new RuntimeException("Normalizer has not been trained.");
        }
        
        // Check if attributes are different
        if (! Arrays.equals(AttributeNames, Data.getAttributeNames())) {
            throw new RuntimeException("Attribute names different: Different type of data?");
        }
        
        if (willNormalizeAttributes()) {
            restoreAttributes(Data);
        }
        if (willNormalizeClass()) {
            restoreClassVariable(Data);
        }
    }
    
    /**
     * Perform the actual restoration on the attributes
     *
     * @param Data Dataset to be transformed
     */
    abstract protected void restoreAttributes(Dataset Data);
    
    /**
     * Perform the actual restoration on the class variables (measured and predicted!)
     * @param Data Dataset to be transformed
     */
    abstract protected void restoreClassVariable(Dataset Data);
    
    /**
     * Check whether the normalize/restore operations are reversible. Tests both
     *  the attribute and class variables.
     * @param data Dataset to use for testings
     * @return Whether the model passes
     */
    public boolean test(Dataset data) {
        // Store original result
        double[][] before = data.getEntryArray();
        
        // Train, normalize, restore
        setToNormalizeAttributes(true);
        setToNormalizeClass(true);
        train(data);
        normalize(data);
        restore(data);
        
        // Make sure the results haven't changed
        double[][] after = data.getEntryArray();
        for (int row=0; row<before.length; row++) {
            for (int col=0; col<before[row].length; col++) {
                if (Math.abs(after[row][col] - before[row][col]) > 1e-6) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String about() {
        return "Trained" + Trained;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return about();
        }
        String Action = Command.get(0);
        switch (Action.toLowerCase()) {
            default: 
                throw new Exception("Normalizer print command not recognized: " + Action);
        }
    }
    
    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty()) {
            System.out.println(about());
            return null;
        }
        String Action = Command.get(0).toString();
        switch (Action.toLowerCase()) {
            case "normalize": {
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: normalize $<dataset>");
                }
                normalize(Data);
            } break;
            case "restore": {
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: restore $<dataset>");
                }
                restore(Data);
            } break;
            case "train": {
                Dataset Data;
                boolean doAttributes = false, doClass = false;
                try {
                    for (int i=1; i<Command.size()-1; i++) {
                        String word = Command.get(i).toString().toLowerCase();
                        if (word.startsWith("attr")) {
                            doAttributes = true;
                        } else if (word.startsWith("clas")) {
                            doClass = true;
                        } else {
                            throw new Exception();
                        }
                    }
                    Data = (Dataset) Command.get(Command.size() - 1);
                } catch (Exception e) {
                    throw new Exception("Usage: train [attributes] [class] $<dataset>");
                }
                
                // Define settings
                if (! (doAttributes || doClass)) {
                    throw new Exception("Must train attributes or class.");
                }
                setToNormalizeAttributes(doAttributes);
                setToNormalizeClass(doClass);
                
                // Run training
                train(Data);
                
                // Print out status
                String wasTrained;
                if (doAttributes && doClass) {
                    wasTrained = "attributes and class";
                } else {
                    wasTrained = doAttributes ? "attributes" : "class";
                }
                System.out.println("\tTrained to normalize " + wasTrained 
                        + " using " + Data.NEntries() + " entries");
            } break;
            case "test": {
                Dataset Data;
                try {
                    Data = (Dataset) Command.get(1);
                } catch (Exception e) {
                    throw new Exception("Usage: test $<dataset>");
                }
                boolean result = test(Data);
                if (result) {
                    System.out.println("\tTest passed!");
                } else {
                    System.out.println("\tTest failed.");
                }
            } break;
            default: 
                throw new Exception("Normalizer command not recognized: " + Action);
        }
        return null;
    }
}
