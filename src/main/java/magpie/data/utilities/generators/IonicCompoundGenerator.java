package magpie.data.utilities.generators;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generate compositions of ionic compounds. For each compositions, it must 
 * be possible to form a charge balance compound assuming each element takes
 * exactly one nonzero integer charge state.
 * 
 * <p>Dev Note: This class simply generates all compositions using the super class
 * and filters out the ones that are no charge balanced. There is certainly
 * a more efficient process.
 * 
 * <usage><p><b>Usage</b>: &lt;min order&gt; &lt;max order&gt; &lt;number&gt; &lt;elements...&gt;
 * <br><pr><i>min order</i>: Minimum number of constituents
 * <br><pr><i>max order</i>: Maximum number of constituents
 * <br><pr><i>number</i>: Maximum number of atoms in the formula unit
 * <br><pr><i>elements</i>: List of elements to include</usage>
 * 
 * @author Logan Ward
 */
public class IonicCompoundGenerator extends PhaseDiagramCompositionEntryGenerator {
    /** Dataset used to compute ionic compounds */
    protected CompositionDataset LookupData;

    public IonicCompoundGenerator() {
        evenSpacing = false;
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int minOrder, maxOrder, size;
        Set<String> elements;
        try {
            minOrder = Integer.parseInt(Options.get(0).toString());
            maxOrder = Integer.parseInt(Options.get(1).toString());
            size = Integer.parseInt(Options.get(2).toString());
            elements = new TreeSet<>();
            for (Object word : Options.subList(3, Options.size())) {
                elements.add(word.toString());
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setOrder(minOrder, maxOrder);
        setSize(size);
		setElementsByName(elements);
    }

	@Override
	public String printUsage() {
		return "Usage: <min order> <max order> <size> [<elements...>]";
	}

    /**
     * Not applicable for this class. Only "crystal" compositions are supported
     * @param evenSpacing Don't use this
     */
    @Override
    public void setEvenSpacing(boolean evenSpacing) {
        throw new UnsupportedOperationException("Not valid options for this generator");
    }

    /**
     * Define the dataset used to lookup oxidation states.
     * @param dataset Dataset to use as a calculator
     */
    public void setLookupData(CompositionDataset dataset) {
        this.LookupData = dataset;
    }

    @Override
    public void train(Dataset dataset) {
        super.train(dataset);
        setLookupData((CompositionDataset) dataset);
    }

    @Override
    public Iterator<BaseEntry> iterator() {
        final Iterator<BaseEntry> allIter = super.iterator();
        
        // Find the first entry that can form an ionic compound
        BaseEntry firstEntry = allIter.next();
        while (! LookupData.compositionCanFormIonic((CompositionEntry) firstEntry) &&
                allIter.hasNext()) {
            firstEntry = allIter.next();
        }
        
        // See if the first entry is an ionic
        final BaseEntry startEntry = LookupData.compositionCanFormIonic((CompositionEntry) firstEntry) ?
                firstEntry : null;
        
        return new Iterator<BaseEntry>() {
            BaseEntry nextEntry = startEntry;
            
            @Override
            public boolean hasNext() {
                return nextEntry != null;
            }

            @Override
            public BaseEntry next() {
                BaseEntry output = nextEntry;
                nextEntry = null;
                while (allIter.hasNext()) {
                    BaseEntry candEntry = allIter.next();
                    if (LookupData.compositionCanFormIonic((CompositionEntry) candEntry)) {
                        nextEntry = candEntry;
                        break;
                    }
                }
                return output;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported."); 
            }
        };
    }

    @Override
    public void addEntriesToDataset(Dataset data) {
        if (! (data instanceof CompositionDataset)) {
            throw new IllegalArgumentException("Data must be a CompositionDataset");
        }
        super.addEntriesToDataset(data); 
    }
    
}
