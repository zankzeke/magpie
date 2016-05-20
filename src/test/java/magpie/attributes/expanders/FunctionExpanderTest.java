package magpie.attributes.expanders;

import magpie.attributes.expanders.FunctionExpander;
import java.util.*;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class FunctionExpanderTest {

    public Dataset getTestSet() throws Exception {
        Dataset out = new Dataset();
        out.importText("datasets/simple-data.txt", null);
        return out;
    }

    @Test
    public void simpleTest() throws Exception {
        Dataset data = getTestSet();
        
        // Create attributes that are the square of every attribute
        FunctionExpander expdr = new FunctionExpander();
        expdr.addNewFunction("#{x}^2");
        
        // Test expansion
        int initialCount = data.NAttributes();
        expdr.expand(data);
        assertEquals(initialCount * 2, data.NAttributes());
        assertEquals(initialCount * 2, data.getEntry(0).NAttributes());
        assertTrue(ArrayUtils.contains(data.getAttributeNames(), data.getAttributeName(0) + "^2"));
        assertEquals(Math.pow(data.getEntry(0).getAttribute(0), 2), 
                data.getEntry(0).getAttribute(initialCount), 1e-6);
    }
    
    @Test
    public void threadTest() throws Exception {
        Dataset data = new CompositionDataset();
        data.importText("datasets/small_set.txt", null);
        
        // Create attributes that are the square of every attribute
        FunctionExpander expdr = new FunctionExpander();
        expdr.addNewFunction("#{x}^2");
        data.addAttribueExpander(expdr);
        Dataset originalData = data.clone();
        
        // Get serial result
        data.generateAttributes();
        double[][] serialResults = data.getAttributeArray();
        
        // Get threaded result
        Magpie.NThreads = 8;
        data = originalData.clone();
        data.generateAttributes();
        double[][] results = data.getAttributeArray();
        
        // Check equality
        for (int e=0; e<data.NEntries(); e++) {
            assertArrayEquals(serialResults[e], results[e], 1e-6);
        }
    }
    
    @Test
    public void regexTest() throws Exception {
        Dataset data = getTestSet();
        
        // Compute square of only X
        FunctionExpander func = new FunctionExpander();
        List<Object> options = new LinkedList<>();
        options.add("#{r:x,x}^2");
        func.setOptions(options);
        
        // Run the expander
        func.expand(data);
        
        // Check results
        assertEquals(3, data.NAttributes());
        for (BaseEntry entry : data.getEntries()) {
            assertEquals(entry.getAttribute(0) * entry.getAttribute(0),
                    entry.getAttribute(2), 1e-6);
        }
        
        // Another regex: Multiply any variable containing x by any other attribute
        options.set(0, "#{y} * #{r:x,^x.*}");
        func.setOptions(options);
        func.expand(data);
        
        // Check results
        assertEquals(7, data.NAttributes());
        for (BaseEntry entry : data.getEntries()) {
            assertTrue(ArrayUtils.contains(entry.getAttributes(), 
                    entry.getAttribute(0) * entry.getAttribute(1)));
            assertTrue(ArrayUtils.contains(entry.getAttributes(), 
                    entry.getAttribute(0) * entry.getAttribute(2)));
            assertTrue(ArrayUtils.contains(entry.getAttributes(), 
                    entry.getAttribute(2) * entry.getAttribute(0)));
            assertTrue(ArrayUtils.contains(entry.getAttributes(), 
                    entry.getAttribute(2) * entry.getAttribute(1)));
        }
    }
    
    @Test
    public void harderTest() throws Exception {
        Dataset data = getTestSet();
        
        // Create attributes that are the square of every attribute
        FunctionExpander expdr = new FunctionExpander();
        expdr.addNewFunction("#{x}^2");
        expdr.addNewFunction("exp(#{x} + #{y})^2");
        
        // Test expansion
        int initialCount = data.NAttributes();
        expdr.expand(data);
        assertTrue(initialCount * 2 < data.NAttributes());
        
        // Test out the names
        assertTrue(ArrayUtils.contains(data.getAttributeNames(), 
                data.getAttributeName(0) + "^2"));
        assertTrue(ArrayUtils.contains(data.getAttributeNames(), "exp(" + 
                data.getAttributeName(0) + " + " + data.getAttributeName(1) + ")^2"));
    }
    
    @Test
    public void optionsTest() throws Exception {
        // Create attributes that are the square of every attribute
        FunctionExpander expdr = new FunctionExpander();
        List<Object> options = new LinkedList<>();
        options.add("#{x} + #{y}");
        expdr.setOptions(options);
    }
    
    @Test
    public void citableTest() {
        // Make a copy of this object
        FunctionExpander o = new FunctionExpander();
        
        // Get the citation and print to make sure it looks right
        List<Pair<String,Citation>> cite = o.getCitations();
        
        // Print out it to screen
        for (Pair<String,Citation> c : cite) {
            System.out.println(c.getKey());
            System.out.println(c.getValue().printInformation());
        }
    }
}
