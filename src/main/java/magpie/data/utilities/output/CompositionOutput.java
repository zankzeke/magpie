package magpie.data.utilities.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyEntry;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Output the fraction of each element in a material. Prints into CSV format
 * and writes all measured and predicted properties (similar to {@linkplain PropertiesOutput}.
 * There are a few options for controlling which elements are included in output:
 * 
 * <ol>
 * <li>All 113 known elements
 * <li>A user-defined list of elements
 * <li>All elements present in certain dataset
 * </ol>
 * 
 * <usage><p><b>Usage</b>: &lt;-all|-dataset|-list|-dynamic&gt; [&lt;options...&gt;]
 * <pr><br><i>-all</i>: Print out fractions of all elements, no options
 * <pr><br><i>-dataset</i>: Print out all elements that are present in a certain 
 * dataset, which is provided as the only option
 * <pr><br><i>-list</i>: Print out only elements in a certain list, which is provided
 * as options to the command
 * <pr><br><i>-dynamic</i>: Print out only elements in the in the dataset being 
 * print, no options</usage>
 * 
 * @author Logan Ward
 */
public class CompositionOutput extends PropertiesOutput {
    /** How to determine which elements to print */
    public enum ElementSelectionMethod {
        /** Print all known elements */
        ALL_ELEMENTS,
        /** Print a user defined list */
        USER_DEFINED,
        /** Determine list of elements from dataset being printed */
        DYNAMIC
    }
    /** Method used to determine which elements to select */
    protected ElementSelectionMethod SelectionMethod = ElementSelectionMethod.DYNAMIC;
    /** User-defined set of elements to print */
    protected SortedSet<Integer> ElementsToPrint = new TreeSet<>();

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // Check that the user gave options
        if (Options.isEmpty()) {
            throw new IllegalArgumentException(printUsage());
        }
        
        // Determine the element selection method
        ElementSelectionMethod selMethod;
        switch (Options.get(0).toString().toLowerCase()) {
            case "-all":
                selMethod = ElementSelectionMethod.ALL_ELEMENTS; break;
            case "-dataset": case "-list":
                selMethod = ElementSelectionMethod.USER_DEFINED; break;
            case "-dynamic":
                selMethod = ElementSelectionMethod.DYNAMIC; break;
            default:
                throw new IllegalArgumentException(String.format(
                        "Selection style %s not recognized. Options are:"
                                + " -all, -dataset, -list, -dynamic",
                        Options.get(0).toString()));
        }
        
        // Given selection method, set options accordingly
        setSelectionMethod(selMethod);
        if (selMethod == ElementSelectionMethod.USER_DEFINED) {
            clearSelectedElementList();
            if (Options.get(0).toString().equalsIgnoreCase("-dataset")) {
                if (Options.size() != 2 || !(Options.get(1) instanceof CompositionDataset)) {
                    throw new Exception("Usage: -dataset $<data>");
                }
                addAllElementsInDataset((CompositionDataset) Options.get(1));
            } else {
                for (Object opt : Options.subList(1, Options.size())) {
                    addElement(opt.toString());
                }
            }
        }
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Set the method used to determine which elements to print.
     * @param method Desired method
     */
    public void setSelectionMethod(ElementSelectionMethod method) {
        this.SelectionMethod = method;
    }
    
    /**
     * Clear the list of user-specific elements
     */
    public void clearSelectedElementList() {
        ElementsToPrint.clear();
    }
    
    /**
     * Add element to the list of elements being printed.
     * @param id ID number of element (Z - 1)
     */
    public void addElement(int id) {
        ElementsToPrint.add(id);
    }
    
    /**
     * Add several elements to the list of elements being printed.
     * @param ids Collection of element ID #s (Z-1)
     */
    public void addElements(Collection<Integer> ids) {
        ElementsToPrint.addAll(ids);
    }
    
    /**
     * Add element to the list of elements being printed
     * @param elem Abbreviation of element (e.g., Fe)
     */
    public void addElement(String elem) {
        int id = ArrayUtils.indexOf(LookupData.ElementNames, elem);
        if (id == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalArgumentException("No such element:" + elem);
        }
        addElement(id);
    }
    
    /**
     * Add several elements to the list of elements being printed. This will change
     * the element selection mode to {@linkplain ElementSelectionMethod#USER_DEFINED}.
     * @param elems List of element abbreviations (e.g., Fe)
     */
    public void addElementsByName(Collection<String> elems) {
        for (String elem : elems) {
            addElement(elem);
        }
    }
    
    /**
     * Add all elements present in a dataset to the list of elements being printed.
     * Note: You may want to change the element selection mode to 
     * {@linkplain ElementSelectionMethod#USER_DEFINED}
     * @param data Dataset containing elements to be printed
     */
    public void addAllElementsInDataset(CompositionDataset data) {
        for (BaseEntry entry : data.getEntries()) {
            for (int id : ((CompositionEntry)entry).getElements()) {
                addElement(id);
            }
        }
    }

    @Override
    public void printHeader(Dataset dataPtr, OutputStream output) {
        CompositionDataset data = (CompositionDataset) dataPtr;
        
        // Step 1: Assemble list of elements
        switch (SelectionMethod) {
            case ALL_ELEMENTS:
                clearSelectedElementList();
                for (int e = 0; e < LookupData.ElementNames.length; e++) {
                    addElement(e);
                }
                break;
            case DYNAMIC:
                clearSelectedElementList();
                addAllElementsInDataset(data);
                break;
        } 
        
        super.printHeader(data, output); 
    }

    @Override
    protected void printAttributeNames(Dataset data, PrintWriter fp) {
        // Print the list of elements
        boolean started = false;
        for (Integer elem : ElementsToPrint) {
            if (! started) {
                started = true;
            } else {
                fp.print(",");
            }
            
            // Print element name
            fp.print("X_" + LookupData.ElementNames[elem]);
        }
    }

    @Override
    protected void printEntryAttributes(MultiPropertyEntry entryPtr, PrintWriter fp) {
        // Cast entry as a composition entry
        CompositionEntry entry = (CompositionEntry) entryPtr;
        
        // Rather than printing attributes, print fraction of each element
        boolean started = false;
        for (int elem : ElementsToPrint) {
            double frac = entry.getElementFraction(elem);
            if (! started) {
                fp.print(frac);
                started = true;
            } else {
                fp.print(",");
                fp.print(frac);
            }
        }
    }
}
