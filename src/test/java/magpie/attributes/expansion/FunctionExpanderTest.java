package magpie.attributes.expansion;

import java.util.*;
import magpie.data.Dataset;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan
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
    public void harderTest() throws Exception {
        Dataset data = getTestSet();
        
        // Create attributes that are the square of every attribute
        FunctionExpander expdr = new FunctionExpander();
        expdr.addNewFunction("#{x}^2");
        expdr.addNewFunction("(#{x} + #{y})^2");
        
        // Test expansion
        int initialCount = data.NAttributes();
        expdr.expand(data);
        assertTrue(initialCount * 2 < data.NAttributes());
        
        // Test out the names
        assertTrue(ArrayUtils.contains(data.getAttributeNames(), 
                data.getAttributeName(0) + "^2"));
        assertTrue(ArrayUtils.contains(data.getAttributeNames(), "(" + 
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
    
}
