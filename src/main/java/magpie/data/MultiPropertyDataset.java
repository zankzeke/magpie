package magpie.data;

import magpie.data.utilities.output.PropertiesOutput;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dataset that can store multiple properties for each entry. User can define the names
 * of these properties, how many different classes are possible for each property (if any),
 * and which one to use as the class variable. If employed with {@link MultiPropertyEntry}, it 
 * will handle the storage of these properties transparently.
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>target &lt;name&gt; [-keep]</b> - Set class variable to be a certain property
 * <br><pr><i>name</i>: Name of property to use as class variable
 * <br><pr><i>-keep</i>: Whether to keep entries without a measurement for this property</command>
 * 
 * <p><b><u>Implemented Save Formats:</u></b>
 * 
 * <save><p><b>prop</b>: Print out the measured and predicted properties</save>
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
    public Dataset emptyClone() {
        MultiPropertyDataset x = (MultiPropertyDataset) super.emptyClone(); 
        x.PNames = new ArrayList<>(PNames);
        x.PClassNames = new LinkedList<>();
        for (String[] ar : PClassNames) {
            x.PClassNames.add(ar.clone());
        }
        x.TargetProperty = TargetProperty;
        return x;
    }

    @Override
    public MultiPropertyDataset createTemplate() {
        MultiPropertyDataset data = (MultiPropertyDataset) super.createTemplate(); 
        
        // Clear property data
        data.clearPropertyData();
        
        return data;
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
     * Add entries from another dataset. If other dataset has different attributes
     * or properties, you can force a merge between these datasets
     * by deleting the attributes and merging the properties.
     * 
     * <p>Entries will be cloned before adding them to this dataset.
     * 
     * <p>For datasets with different properties, the final dataset will contain
     * properties from both datasets. Entries from this datasets that lack 
     * properties from the other dataset will have no measured or predicted values
     * for those properties. And, for entries from the other dataset that lack entries
     * from this dataset they will have no measured or predicted values for those
     * properties.
     * 
     * @param otherDataset Dataset to be added to this one
     * @param forceMerge Whether to force merge if attributes / class / properties are different.
     * @throws java.lang.Exception If datasets have different classes or attributes,
     * and forceMerge is false.
     */
    @Override
    public void addEntries(Dataset otherDataset, boolean forceMerge) throws Exception {
        // Make a MultiPropertyDataset pointer for otherDataset
        MultiPropertyDataset otherPtr = (MultiPropertyDataset) otherDataset;
        
        // Set target class to be base property
        int thisTarget = getTargetPropertyIndex();
        int otherTarget = otherPtr.getTargetPropertyIndex();
        setTargetProperty(-1, true);
        otherPtr.setTargetProperty(-1, true);
        
        // Check whether attributes / class are the same
        boolean attrSame = otherDataset.AttributeName.equals(AttributeName);
        boolean classSame = Arrays.equals(otherDataset.getClassNames(), getClassNames());
        boolean propSame = PNames.equals(otherPtr.PNames);
        
        // If they are, and force is not true: Fail
        if (! ((attrSame && classSame && propSame) || forceMerge)) {
            throw new Exception("Datasets are not identical and merge not forced");
        }
        
        // Merge property names
        for (String otherProp : otherPtr.PNames) {
            // Check if this dataset contains that property
            int thisIndex = PNames.indexOf(otherProp);
            
            // If it doesn't exist, add it to this dataset and every entry in this set
            if (thisIndex == -1) {
                // Add property name to dataset
                if (otherPtr.getPropertyClasses(otherProp).length == 1) {
                    addProperty(otherProp);
                } else {
                    addProperty(otherProp, otherPtr.getPropertyClasses(otherProp));
                }
                
                // Add property to entries
                for (BaseEntry ePtr : Entries) {
                    MultiPropertyEntry entry = (MultiPropertyEntry) ePtr;
                    entry.addProperty();
                }
            } else {
                // If it does, make sure that the class names are the same
                String[] thisNames = PClassNames.get(thisIndex);
                String[] otherNames = otherPtr.PClassNames.get(
                        otherPtr.PNames.indexOf(otherProp));
                if (! Arrays.equals(thisNames, otherNames)) {
                    throw new RuntimeException("Unhandled case: Same property names, different class names for that property");
                }
            }
        }
        
        // Match up property names in other dataset
        int[] otherPropMatches = new int[otherPtr.NProperties()];
        for (int i=0; i<otherPropMatches.length; i++) {
            otherPropMatches[i] = PNames.indexOf(otherPtr.getPropertyName(i));
        }
        
        // Add in the entries
        for (BaseEntry otherEntry : otherDataset.Entries) {
            // Make the clone
            MultiPropertyEntry oldEntry = (MultiPropertyEntry) otherEntry;
            MultiPropertyEntry newEntry = oldEntry.clone();
            
            // Set new entry to have correct number of properties, but with
            //   no measured or predicted
            newEntry.clearPropertyData();
            newEntry.setNProperties(NProperties());
            
            // If needed, delete attributes or class
            if (! attrSame) {
                newEntry.clearAttributes();
            }
            if (! classSame) {
                newEntry.deleteMeasuredClass();
                newEntry.deletePredictedClass();
            }
            
            // Merge properties
            for (int p=0; p<otherPropMatches.length; p++) {
                if (oldEntry.hasMeasuredProperty(p)) {
                    newEntry.setMeasuredProperty(otherPropMatches[p], 
                            oldEntry.getMeasuredProperty(p));
                }
                if (oldEntry.hasPredictedProperty(p)) {
                    if (otherPtr.getPropertyClasses(p).length > 1) {
                        newEntry.setPredictedProperty(otherPropMatches[p], 
                                oldEntry.getPropertyClassProbabilties(p));
                    } else {
                        newEntry.setPredictedProperty(otherPropMatches[p],
                                oldEntry.getPredictedProperty(p));
                    }
                }
            }
            
            // Add it to this dataset
            addEntry(newEntry);
        }
        
        // Reset target property
        setTargetProperty(thisTarget, true);
        otherPtr.setTargetProperty(otherTarget, true);
    }
    
    

    /**
     * Combine the dataset with another. Will change the target property of the 
     * other dataset to be the same as this one
     * @param d Other dataset. Will change target property to be the same as this class
     */
    @Override
    public void combine(Dataset d) {
        MultiPropertyDataset ptr = (MultiPropertyDataset) d;
        if (! PNames.equals(Arrays.asList(ptr.getPropertyNames()))) {
            throw new Error("Property lists are different");
        }
        super.combine(d);
        setTargetProperty(TargetProperty, true);
        ptr.setTargetProperty(TargetProperty, true);
    }

	@Override
	public void addEntry(BaseEntry e) {
		// Make sure the entry has the correct number of properties
		//  and the correct target property
		MultiPropertyEntry p = (MultiPropertyEntry) e;
		p.setNProperties(NProperties());
		p.setTargetProperty(getTargetPropertyIndex());
		super.addEntry(e); 
	}

	@Override
    public void addEntries(Collection<? extends BaseEntry> entries) {
        // Update these entries to have the correct number and target property
        int nProps = NProperties();
        int tProp = getTargetPropertyIndex();
		for (BaseEntry ePtr : entries) {
             MultiPropertyEntry entry = (MultiPropertyEntry) ePtr;
             entry.setNProperties(nProps);
             entry.setTargetProperty(tProp);
        }

        // Add entry to the dataset
	    super.addEntries(entries);
    }

    /**
     * @return Number of properties known by this Dataset
     */
    public int NProperties() {
        return PNames.size();
    }
    
    /**
     * Get the names of each property stored in this dataset.
     * @return Array containing property names
     */
    public String[] getPropertyNames() {
        return PNames.toArray(new String[0]);
    }

    @Override
    public String[] getClassNames() {
        if (TargetProperty == -1) {
            return super.getClassNames();
        } else {
            return PClassNames.get(TargetProperty);
        }
    }

    @Override
    public void setClassNames(String[] newClassNames) {
        super.setClassNames(newClassNames);
        if (TargetProperty != -1) {
            setPropertyClasses(TargetProperty, newClassNames);
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
        return TargetProperty != -1;
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

    /** 
     * Define possible classes for a certain property (like "Metal" or "Nonmetal")
     * @param index Index of property
     * @param classes Possible classes
     */
    public void setPropertyClasses(int index, String[] classes) {
        PClassNames.set(index, classes);
    }
	
	/**
	 * Get list of possible classes for a certain property
	 * @param name Name of property
	 * @return List of classes for that property
	 */
	public String[] getPropertyClasses(String name) {
		int index = getPropertyIndex(name);
		if (index == -1) {
			throw new Error("No such property: " + name);
		}
		return getPropertyClasses(index);
	}
	
	/**
	 * Get list of possible classes for a certain property
	 * @param index Index of property
	 * @return List of classes for that property
	 */
	public String[] getPropertyClasses(int index) {
		return PClassNames.get(index).clone();
	}
	
	/**
	 * Get the number of classes for a certain property
	 * @param index Index of property
	 * @return Number of classes for that property
	 */
	public int getPropertyClassCount(int index) {
		return PClassNames.get(index).length;
	}
    
    /**
     * Check whether dataset contains a certain property
     * @param name Name of property
     * @return Whether that property is known
     */
    public boolean hasProperty(String name) {
        return PNames.contains(name);
    }

    /**
     * Define all known properties for a dataset. Removes duplicates
     * 
     * <p>Note: This does <b>not</b> mean that the entries will recognize all of these properties. 
     * You must run either {@link MultiPropertyEntry#addProperty(double)} or
     * {@link MultiPropertyEntry#setMeasuredProperties(double[])}.
     * 
     * <p>Note: Order of properties may be changed
     * 
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
	 * @param keepUnmeasured Whether to keep entries without a measurement for this property
     */
    public void setTargetProperty(String Property, boolean keepUnmeasured) {
        int Index = PNames.indexOf(Property);
        if (Index != -1) {
            setTargetProperty(Index, keepUnmeasured);
        } else {
            throw new RuntimeException("Property " + Property + " not found");
        }
    }

    /** 
     * Define which property to set as the measured class of each entry. Automatically
     * set this as the target property for each entry. Set to "-1" to use a variable 
     * besides a measured property as the class variable. 
     * 
     * @param index Index of the property to be used
	 * @param keepUnmeasured Whether to keep entries without a measurement for this property
     */
    public void setTargetProperty(int index, boolean keepUnmeasured) {
        if (index >= NProperties())
            throw new RuntimeException("Invalided target property: Greater than number of properties.");
        if (NEntries() > 0 && index >= getEntry(0).NProperties())
            throw new RuntimeException("Critical Error: Entries have fewer properties than Dataset!");
        Iterator<BaseEntry> iter = Entries.iterator();
        TargetProperty = index;
        if (TargetProperty >= 0) {
            setClassNames(PClassNames.get(index));
        }
        
        // Process the entries
        while (iter.hasNext()) {
            MultiPropertyEntry E = (MultiPropertyEntry) iter.next();
            if (TargetProperty >= 0) {
                if (E.hasMeasuredProperty(index)) {
                    E.setTargetProperty(index);
                } else {
					if (! keepUnmeasured) {
						iter.remove(); 
					} else {
						E.setTargetProperty(index);
					}
                }
            } else {
                E.setTargetProperty(-1);
            }
        }
    }
    
    /**
     * Clear all property information from this dataset.
     */
    public void clearPropertyData() {
        // Clear property names 
        PClassNames.clear();
        PNames.clear();
        
        // Clear property entries
        for (BaseEntry e : Entries) {
            MultiPropertyEntry ptr = (MultiPropertyEntry) e;
            ptr.clearPropertyData();
        }
        
        // Set target property to default class variable
        setTargetProperty(-1, true);
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
            return;
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
            throw new RuntimeException("Dataset does not contain property: " + PropertyName);
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
            throw new RuntimeException("Dataset does not contain property: " + PropertyName);
        }
        return getPredictedPropertyArray(ind);
    }
    
    /**
     * Get the measured value for a certain property for all entries in the dataset. For entries
     *  without a measurement for that property, will return NaN.
     * @param index Index of the desired property
     * @return Array containing measured property for each entry.
     */
    public double[] getMeasuredPropertyArray(int index) {
        // Check that input is sane
        if (index < 0) throw new RuntimeException("Invalid property index: " + index);
        if (index >= NProperties()) 
            throw new RuntimeException("Requested property " + index + " only " + NProperties() + " properties in dataset");
        
        // Get output
        double[] output = new double[NEntries()];
        for (int e=0; e<NEntries(); e++) {
			if (getEntry(e).hasMeasuredProperty(index)) {
				output[e] = getEntry(e).getMeasuredProperty(index);
			} else {
				output[e] = Double.NaN;
			}
        }
        return output;
    }
    
    /**
     * Get the predicted value for a certain property for all entries in the dataset. For entries
     *  without a measurement for that property, will return NaN.
     * @param index Index of the desired property
     * @return Array containing predicted property for each entry.
     */
    public double[] getPredictedPropertyArray(int index) {
        // Check that input is sane
        if (index < 0) throw new RuntimeException("Invalid property index: " + index);
        if (index >= NProperties()) 
            throw new RuntimeException("Requested property " + index + " only " + NProperties() + " properties in dataset");
        
        // Gather data
        double[] output = new double[NEntries()];
        for (int e=0; e<NEntries(); e++) {
			if (getEntry(e).hasPredictedProperty(index)) {
				output[e] = getEntry(e).getPredictedProperty(index);
			} else {
				output[e] = Double.NaN;
			}
        }
        return output;
    }

    @Override
    public String toString() {
        String output = "Number of entries:    " + NEntries();
        output += "\nNumber of attributes:   " + NAttributes();
        output += "\nNumber of properties: " + NProperties();
        output += "\nTarget property: " + getTargetPropertyName();
		if (NClasses() > 1) {
			String[] classes = getClassNames();
			output += "\n\tPossible classes: {" + classes[0];
			for (int i=1; i<classes.length; i++) {
				output += ", " + classes[i];
			}
			output += "}";
		}
        return output;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.size() > 0) {
            String Action = Command.get(0).toString().toLowerCase();
            switch (Action) {
                case "target": {
                    // Usage: target <name> [-keep]
                    String Target;
					boolean toKeep = false; 
					try {
						Target = Command.get(1).toString();
						if (Command.size() > 2) {
							if (Command.get(2).toString().equalsIgnoreCase("-keep")) {
								toKeep = true;
							} else {
								throw new Exception();
							}
						}
					} catch (Exception e) {
						throw new Exception("Usage: <target> [-keep]");
					}
                    int originalSize = NEntries();
                    setTargetProperty(Target, toKeep);
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

    @Override
    public String saveCommand(String Basename, String Format) throws Exception {
        switch (Format.toLowerCase()) {
            case "prop": {
                new PropertiesOutput().writeDataset(this, Basename + ".prop");
                return Basename + ".prop";
            }
            default:
                return super.saveCommand(Basename, Format);
        }
    }

    /**
     * Given the line describing property names in the input file, read in property
     * names and possible classes.
     * @param line Line describing property names
     */
    protected void importPropertyNames(String line) {
        // Clear out current property data
        clearPropertyData();
        
        // Initialize regex
        String[] propertyNames = line.split("\\s+");
        Pattern namePattern = Pattern.compile("^[\\d\\w]+"); // Given name/classes, get name
        Pattern classPattern = Pattern.compile("\\{.*\\}"); // Get the possible classes
        
        // Find all property names
        for (int p=1; p<propertyNames.length; p++) {
            String total = propertyNames[p];
            Matcher tempMatcher = namePattern.matcher(total);
            tempMatcher.find();
            String name = tempMatcher.group();
            if (!total.contains("{")) {
                addProperty(name);
            } else {
                tempMatcher = classPattern.matcher(total);
                tempMatcher.find();
                String classList = tempMatcher.group();
                // Trim off the "{,}"
                classList = classList.substring(1);
                classList = classList.substring(0, classList.length() - 1);
                // Get the class names
                String[] classes = classList.split(",");
                for (int i = 0; i < classes.length; i++) {
                    classes[i] = classes[i].trim();
                }
                addProperty(name, classes);
            }
        }
    }

    /**
     * Used by {@linkplain #importText(java.lang.String, java.lang.Object[]) }
     * to import property measurements for each entry.
     *
     * @param words Line describing entry, split into words
     * @return Property measurements for this entry
     */
    protected double[] importEntryProperties(String[] words) {
        double[] properties;
        // Get the properties
        properties = new double[NProperties()];
        for (int p = 0; p < NProperties(); p++) {
            try {
                if (getPropertyClassCount(p) == 1) {
                    properties[p] = Double.parseDouble(words[p + 1]);
                } else {
                    int index = ArrayUtils.indexOf(getPropertyClasses(p), words[p + 1]);
                    if (index == -1) {
                        index = Integer.parseInt(words[p + 1]);
                    }
                    properties[p] = index;
                }
            } catch (NumberFormatException exc) {
                // System.err.println("Warning: Entry #"+i+" has an invalid property.");
                properties[p] = Double.NaN;
            }
        }
        return properties;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject output = super.toJSON(); 
        
        // Add in property information
        JSONArray properties = new JSONArray();
        for (int p=0; p<NProperties(); p++) {
            JSONObject propInfo = new JSONObject();
            
            propInfo.put("name", getPropertyName(p));
            if (getPropertyClassCount(p) > 1) {
                propInfo.put("classes", getPropertyClasses(p));
            }
            properties.put(propInfo);
        }
        output.put("properties", properties);
        
        return output;
    }
    
}
