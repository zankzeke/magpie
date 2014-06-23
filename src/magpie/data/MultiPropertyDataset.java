/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.data;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

/**
 * Dataset that can store multiple properties for each entry. User can define the names
 * of these properties, how many different classes are possible for each property (if any),
 * and which one to use as the class variable. If employed with {@link MultiPropertyEntry}, it 
 * will handle the storage of these properties transparently.
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>target &lt;name></b> - Set class variable to be a certain property
 * <br><pr><i>name</i>: Name of property to use as class variable
 * Any entries without this property will be removed from the dataset.</command>
 * 
 * @author Logan Ward
 * @version 0.1
 * @see MultiPropertyEntry
 */
public class MultiPropertyDataset extends Dataset {
    /** For properties with multiple classes, store the acceptable class names */
    private List<String[]> PClassNames = new LinkedList<>();
    /** Names of the properties known about each entry */
    private List<String> PNames = new LinkedList<>();
    /** Which property is currently used as the class variable */
    private int TargetProperty = -1;

    @Override
    @SuppressWarnings({"CloneDeclaresCloneNotSupported", "CloneDoesntCallSuperClone"})
    public Dataset clone() {
        MultiPropertyDataset x = (MultiPropertyDataset) super.clone(); 
        x.PNames = new LinkedList<>(PNames);
        x.PClassNames = new LinkedList<>(PClassNames);
        x.TargetProperty = TargetProperty;
        return x;
    }

    @Override
    public MultiPropertyEntry getEntry(int index) {
        return (MultiPropertyEntry) super.getEntry(index); 
    }

    @Override
    public int NClasses() {
        if (TargetProperty == -1) {
            return super.NClasses();
        } else {
            return PClassNames.get(TargetProperty).length;
        }
    }

    /**
     * @return Number of properties known by this Dataset
     */
    public int NProperties() {
        return PNames.size();
    }

    @Override
    public String[] getClassNames() {
        if (TargetProperty == -1) {
            return super.getClassNames();
        } else {
            return PClassNames.get(TargetProperty);
        }
    }

    /**
     * Retrieve the name of a certain property.
     * @param index Index of property to look up
     * @return Name of that property
     */
    public String getPropertyName(int index) {
        return PNames.get(index);
    }

    /**
     * Get the index of the currently active property. Returns -1 if no property currently in use.
     * @return Index of target property
     */
    public int getTargetPropertyIndex() {
        return TargetProperty;
    }
    
    /**
     * @return Whether a property is being used as the class variable.
     */
    public boolean usingPropertyAsClass() {
        return TargetProperty == -1;
    }

    /** 
     * Lookup what target property is being used.
     * @return Name of property being used as class variable. "None" if no property is being used.
     */
    public String getTargetPropertyName() {
        if (TargetProperty != -1) {
            return PNames.get(TargetProperty);
        } else {
            return "None";
        }
    }
    
    /**
     * Lookup the index of a property. Returns -1 if no property by that name exists
     * @param name Name of property
     * @return Index of that property
     */
    public int getPropertyIndex(String name) {
        return PNames.indexOf(name);
    }

    @Override
    public void setClassNames(String[] newClassNames) {
        super.setClassNames(newClassNames);
        if (TargetProperty != -1) {
            setPropertyClasses(TargetProperty, newClassNames);
        }
    }

    /** 
     * Define possible classes for a certain property (like "Metal" or "Nonmetal")
     * @param index Index of property
     * @param classes Possible classes
     */
    public void setPropertyClasses(int index, String[] classes) {
        PClassNames.set(index, classes);
    }

    /**
     * Define all known properties for a dataset. Removes duplicates
     * <p>Note: This does <b>not</b> mean that the entries will recognize all of these properties. 
     * You must run either {@link MultiPropertyEntry#addProperty(double)} or
     * {@link MultiPropertyEntry#setMeasuredProperties(double[])}.
     * @param names Names of properties
     */
    public void definePropertyNames(String[] names) {
        PNames.clear();
        PNames.addAll(new TreeSet<>(Arrays.asList(names)));
        if (PNames.size() != names.length)
            throw new Error("Duplicates not supported");
    }

    /**
     * Define which property to use as the class variable.
     * @param Property Name of this property
     */
    public void setTargetProperty(String Property) {
        int Index = PNames.indexOf(Property);
        if (Index != -1) {
            setTargetProperty(Index);
        } else {
            throw new Error("Property " + Property + " not found");
        }
    }

    /** 
     * Define which property to set as the measured class of each entry. Automatically
     * set this as the target property for each entry. Set to "-1" to use a variable 
     * besides a measured property as the class variable. 
     * 
     * Note: This removes entries which do not have this property defined. 
     * 
     * @param Index Index of the property to be used
     */
    public void setTargetProperty(int Index) {
        if (Index >= NProperties())
            throw new Error("Invalided target property: Greater than number of properties.");
        if (NEntries() > 0 && Index >= getEntry(0).NProperties())
            throw new Error("Critical Error: Entries have fewer properties than Dataset!");
        Iterator<BaseEntry> iter = Entries.iterator();
        TargetProperty = Index;
        if (TargetProperty >= 0) {
            setClassNames(PClassNames.get(Index));
        }
        
        // Process the entries
        while (iter.hasNext()) {
            MultiPropertyEntry E = (MultiPropertyEntry) iter.next();
            if (TargetProperty >= 0) {
                if (E.hasMeasuredProperty(Index)) {
                    E.setTargetProperty(Index);
                } else {
                    iter.remove();
                }
            } else {
                E.setTargetProperty(-1);
            }
        }
    }

    /**
     * Add a new property to dataset. 
     * <p>NOTE: You <i>must</i> add properties to each entry in the Dataset. This is not done 
     * automatically because there are several possible ways to add properties to an 
     * entry, depending on what you already know about the entry (see below)
     * @param name Name of new property
     * @see MultiPropertyEntry#addProperty() 
     * @see MultiPropertyEntry#addProperty(double) 
     * @see MultiPropertyEntry#addProperty(double, double) 
     */
    public void addProperty(String name) {
        String[] newClassNames = new String[]{name};
        addProperty(name, newClassNames);
    }
    
    /**
     * Add a new property to dataset. 
     * <p>NOTE: You <i>must</i> add properties to each entry in the Dataset. This is not done 
     * automatically because there are several possible ways to add properties to an 
     * entry, depending on what you already know about the entry (see below)
     * @param name Name of new property
     * @param possibleClasses Possible classes for this property
     * @see MultiPropertyEntry#addProperty() 
     * @see MultiPropertyEntry#addProperty(double) 
     * @see MultiPropertyEntry#addProperty(double, double) 
     */
    public void addProperty(String name, String[] possibleClasses) {
        if (PNames.contains(name))
            throw new Error("Duplicate names not supported.");
        PNames.add(name);
        PClassNames.add(possibleClasses);
    }
    
    /**
     * Get measured values for a certain property for all entries in the dataset. For entries
     *  without a measurement for that property, will return NaN.
     * @param PropertyName Name of desired property
     * @return Array containing measured property for each entry.
     */
    public double[] getMeasuredPropertyArray(String PropertyName) {
        int ind = getPropertyIndex(PropertyName);
        if (ind == -1) {
            throw new Error("Dataset does not contain property: " + PropertyName);
        }
        return getMeasuredPropertyArray(ind);
    }
    
    /**
     * Get predicted values for a certain property for all entries in the dataset. For entries
     *  without a prediction for that property, will return NaN.
     * @param PropertyName Name of desired property
     * @return Array containing predicted property for each entry.
     */
    public double[] getPredictedPropertyArray(String PropertyName) {
        int ind = getPropertyIndex(PropertyName);
        if (ind == -1) {
            throw new Error("Dataset does not contain property: " + PropertyName);
        }
        return getPredictedPropertyArray(ind);
    }
    
    /**
     * Get the measured value for a certain property for all entries in the dataset. For entries
     *  without a measurement for that property, will return NaN.
     * @param Index Index of the desired property
     * @return Array containing measured property for each entry.
     */
    public double[] getMeasuredPropertyArray(int Index) {
        if (Index < 0) throw new Error("Invalid property index: " + Index);
        if (Index >= NProperties()) 
            throw new Error("Requested property " + Index + " only " + NProperties() + " properties in dataset");
        double[] output = new double[NEntries()];
        for (int i=0; i<NEntries(); i++) {
            output[i] = getEntry(i).getMeasuredProperty(Index);
        }
        return output;
    }
    
    /**
     * Get the predicted value for a certain property for all entries in the dataset. For entries
     *  without a measurement for that property, will return NaN.
     * @param Index Index of the desired property
     * @return Array containing predicted property for each entry.
     */
    public double[] getPredictedPropertyArray(int Index) {
        if (Index < 0) throw new Error("Invalid property index: " + Index);
        if (Index >= NProperties()) 
            throw new Error("Requested property " + Index + " only " + NProperties() + " properties in dataset");
        double[] output = new double[NEntries()];
        for (int i=0; i<NEntries(); i++) {
            output[i] = getEntry(i).getPredictedProperty(Index);
        }
        return output;
    }

    @Override
    public String toString() {
        String output = "Number of entries:    " + NEntries();
        output += "\nNumber of attributes:   " + NAttributes();
        output += "\nNumber of properties: " + NProperties();
        output += "\nTarget property: " + getTargetPropertyName();
        return output;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.size() > 0) {
            String Action = Command.get(0).toString().toLowerCase();
            switch (Action) {
                case "target": {
                    // Usage: target <name>
                    String Target = Command.get(1).toString();
                    int originalSize = NEntries();
                    setTargetProperty(Command.get(1).toString());
                    String output = "\tSet target property to " + Target;
                    if (originalSize > NEntries()) {
                        output += ". " + (originalSize - NEntries()) 
                                + " entries were removed.";
                    }
                    System.out.println(output);
                } break;
                default:
                    return super.runCommand(Command);
            }
            return null;
        }
        return super.runCommand(Command); 
    }    
}
