package magpie.attributes.selectors;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LassoAttributeSelectorTest {

    /**
     * Make a dataset with 5 attributes, and 100 entries class equal to a linear combination
     * of those attributes
     * @return 
     */
    public Dataset makeDataset() {
        Dataset data = new Dataset();
        data.addAttributes(Arrays.asList(new String[]{"A","B","C","D","E"}));
        for (int e=0; e<100; e++) {
            BaseEntry entry = new BaseEntry();
            double[] attrs = new double[data.NAttributes()];
            for (int a=0; a<attrs.length; a++) {
                attrs[a] = Math.random();
            }
            entry.setAttributes(attrs);
            entry.setMeasuredClass(entry.getAttribute(0) - 
                    2 * entry.getAttribute(1));
            data.addEntry(entry);
        }
        return data;
    }
    
    @Test
    public void test() throws Exception {
        // Get data
        Dataset data = makeDataset();
        
        // Make the selector
        LassoAttributeSelector sel = new LassoAttributeSelector();
        List <Object> options = new LinkedList<>();
        options.add("-n_lasso");
        options.add(3);
        options.add("-max_dim");
        options.add(2);
        options.add("-debug");
        
        sel.setOptions(options);
        
        // Run it
        System.out.println("\tSimple test: Lasso = 3, no dim red, max dim = 2");
        List<Integer> attrs = sel.train_protected(data);
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(0));
        assertTrue(attrs.contains(1));
        System.out.println(sel.printDescription(true));
        
        // Run another test
        options.add("-cv_method");
        options.add(0.1);
        options.add(100);
        
        sel.setOptions(options);
        
        System.out.println("\tSimple test: Lasso = 3, no dim red, max dim = 2, cv test");
        attrs = sel.train_protected(data);
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(0));
        assertTrue(attrs.contains(1));
        System.out.println(sel.printDescription(true));
        
        // Run another test
        options.add("-pick_best");
        
        sel.setOptions(options);
        
        System.out.println("\tSimple test: Lasso = 3, no dim red, max dim = 2, cv test");
        attrs = sel.train_protected(data);
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(0));
        assertTrue(attrs.contains(1));
        System.out.println(sel.printDescription(true));
        
        // Run another test
        options.set(1, 4);
        options.set(3, 3);
        
        sel.setOptions(options);
        
        System.out.println("\tSimple test: Lasso = 4, no dim red, max dim = 3, cv test");
        attrs = sel.train_protected(data);
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(0));
        assertTrue(attrs.contains(1));
        System.out.println(sel.printDescription(true));
        
        // Run another test
        options.add("-corr_downselect");
        options.add(3);
        
        sel.setOptions(options);
        
        System.out.println("\tSimple test: Lasso = 4, dim red = 3, max dim = 3, cv test");
        attrs = sel.train_protected(data);
        System.out.println(sel.printDescription(true));
        assertTrue(attrs.size() <= 3);
    }
    
    @Test
    public void testCiations() throws Exception {
        // Make the selector
        LassoAttributeSelector lasso = new LassoAttributeSelector();
        
        System.out.println("\tBase Lasso selector");
        System.out.println(lasso.getCitations());
        assertEquals(1, lasso.getCitations().size());
        
        System.out.println("\tWith CV");
        lasso.setCVFraction(0.1);
        System.out.println(lasso.getCitations());
        assertEquals(2, lasso.getCitations().size());
        
        System.out.println("\tWithout CV");
        lasso.setCVFraction(-1);
        System.out.println(lasso.getCitations());
        assertEquals(1, lasso.getCitations().size());
        
        System.out.println("\tWith downselection");
        lasso.setNDownselect(5);
        System.out.println(lasso.getCitations());
        assertEquals(2, lasso.getCitations().size());
        
    }
    
}
