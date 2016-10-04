package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import magpie.Magpie;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.models.classification.WekaClassifier;
import magpie.models.regression.PolynomialRegression;
import org.junit.Test;
import static org.junit.Assert.*;
import weka.classifiers.Evaluation;

/**
 *
 * @author Logan Ward
 */
public class ExhaustiveAttributeSelectorTest {

    @Test
    public void testIterator() {
        // Make a dataset with 4 attributes
        Dataset data = new Dataset();
        data.addAttributes(Arrays.asList(new String[]{"A", "B", "C", "D"}));
        
        // Make the attribute selector and tell it to search between 2 and 3
        ExhaustiveAttributeSelector sel = new ExhaustiveAttributeSelector();
        sel.setMinSubsetSize(1);
        sel.setMaxSubsetSize(3);
        
        // Generate the iterator
        sel.setCombinationIterator(data);
        
        // Loop over the iterator
        Iterator<int[]> combIter = sel.SetIterator;
        int hits = 0;
        for (; combIter.hasNext(); ) {
            int[] set = combIter.next();
            hits++;
            assertTrue(set.length >= 1 && set.length <= 3);
        }
        assertEquals(4+6+4, hits);
    }
    
    @Test
    public void testRegression() throws Exception {
        Dataset data = makeTestDataset();
        
        // Set the class such it is A^2 + 2*B
        for (BaseEntry entry : data.getEntries()) {
            entry.setMeasuredClass(entry.getAttribute(0) * entry.getAttribute(0)
                    + 2 * entry.getAttribute(1));
        }
        
        // Make the selector
        ExhaustiveAttributeSelector sel = new ExhaustiveAttributeSelector();
        
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(2);
        
        List<Object> options = new ArrayList<>();
        options.add(model);
        options.add("-min_size");
        options.add(1);
        options.add("-max_size");
        options.add(3);
        options.add("-k_fold");
        options.add(10);
        
        sel.setOptions(options);
        
        testSelector(sel, data);
    }
    
    @Test
    public void testClassifier() throws Exception {
        Dataset data = makeTestDataset();
        
        // Set two classes
        data.setClassNames(new String[]{"Yes","No"});
        
        // Set the class such it is "A < 0.25, B < 0.5, or A + B < 0.55"
        for (BaseEntry entry : data.getEntries()) {
            entry.setMeasuredClass(entry.getAttribute(0) < 0.25
                    || entry.getAttribute(1) < 0.5
                    || entry.getAttribute(0) + entry.getAttribute(1) < 0.55 ?
                    0 : 1 );
        }
        
        // Make the selector
        ExhaustiveAttributeSelector sel = new ExhaustiveAttributeSelector();
        
        WekaClassifier model = new WekaClassifier("trees.REPTree", null);
        
        List<Object> options = new ArrayList<>();
        options.add(model);
        options.add("-min_size");
        options.add(1);
        options.add("-max_size");
        options.add(3);
        options.add("-k_fold");
        options.add(10);
        
        sel.setOptions(options);
        
        testSelector(sel, data);
    }

    /**
     * Test the selector to make sure it gets the right number of attributes
     * @param sel Selector to be tested
     * @param data Data used for testing
     */
    protected void testSelector(ExhaustiveAttributeSelector sel, Dataset data) {
        // Run the selector
        sel.train(data);
        
        // Make sure it gets the two important attributes
        assertTrue(sel.getSelections().contains(0));
        assertTrue(sel.getSelections().contains(1));
        
        // Re-run with more threads
        Magpie.NThreads = 2;
        sel.train(data);
        
        assertTrue(sel.getSelections().contains(0));
        assertTrue(sel.getSelections().contains(1));
        
        Magpie.NThreads = 1;
    }
    
    /**
     * Make a test dataset with 10 attributes and 100 entries
     * @return Example dataset with no measured class
     */
    protected Dataset makeTestDataset() {
        Dataset data = new Dataset();
        for (int a=0; a<10; a++) {
            data.addAttribute(new Character((char) (65+a)).toString(), new double[0]);
        }
        
        Random rand = new Random();
        
        for (int i=0; i<200; i++) {
            BaseEntry entry = new BaseEntry();
            entry.setAttributes(new double[10]);
            
            for (int a=0; a<10; a++) {
                entry.setAttribute(a, rand.nextDouble());
            }
            
            data.addEntry(entry);
        }
        
        return data;
    }
    
    
    @Test
    public void testEvalMethods() throws Exception {
        Dataset data = makeTestDataset();
        
        // Set the class such it is A^2 + 2*B
        for (BaseEntry entry : data.getEntries()) {
            entry.setMeasuredClass(entry.getAttribute(0) * entry.getAttribute(0)
                    + 2 * entry.getAttribute(1));
        }
        
        // Make the selector
        ExhaustiveAttributeSelector sel = new ExhaustiveAttributeSelector();
        
        PolynomialRegression model = new PolynomialRegression();
        model.setOrder(2);
        
        // Set for the training set
        List<Object> options = new ArrayList<>();
        options.add(model);
        options.add("-min_size");
        options.add(1);
        options.add("-max_size");
        options.add(3);
        options.add("-train");
        
        sel.setOptions(options);
        
        // Test settings
        assertEquals(1, sel.MinSubsetSize);
        assertEquals(3, sel.MaxSubsetSize);
        assertEquals(ExhaustiveAttributeSelector.EvaluationMethod.TRAINING, 
                sel.TestMethod);
        sel.train(data);
        System.out.println(sel.printDescription(true));
        
        // Set for the training set
        options = new ArrayList<>();
        options.add(model);
        options.add("-min_size");
        options.add(1);
        options.add("-max_size");
        options.add(3);
        options.add("-k_fold");
        options.add(8);
        
        sel.setOptions(options);
        
        // Test settings
        assertEquals(1, sel.MinSubsetSize);
        assertEquals(3, sel.MaxSubsetSize);
        assertEquals(8, sel.KFolds);
        assertEquals(ExhaustiveAttributeSelector.EvaluationMethod.KFOLD_CROSSVALIDATION, 
                sel.TestMethod);
        sel.train(data);
        System.out.println(sel.printDescription(true));
        
        // Set for the training set
        options = new ArrayList<>();
        options.add(model);
        options.add("-min_size");
        options.add(1);
        options.add("-max_size");
        options.add(3);
        options.add("-random_cv");
        options.add(0.25);
        options.add(100);
        
        sel.setOptions(options);
        
        // Test settings
        assertEquals(1, sel.MinSubsetSize);
        assertEquals(3, sel.MaxSubsetSize);
        assertEquals(100, sel.RandomTestCount);
        assertEquals(0.25, sel.RandomTestFraction, 1e-6);
        assertEquals(ExhaustiveAttributeSelector.EvaluationMethod.RANDOMSPLIT_CROSSVALIDATION,
                sel.TestMethod);
        sel.train(data);
        System.out.println(sel.printDescription(true));
    }
}
