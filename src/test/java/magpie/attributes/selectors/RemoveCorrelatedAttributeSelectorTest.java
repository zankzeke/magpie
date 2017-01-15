package magpie.attributes.selectors;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Logan Ward
 */
public class RemoveCorrelatedAttributeSelectorTest {
    @Test
    public void test() throws Exception {
        RemoveCorrelatedAttributeSelector sel = new RemoveCorrelatedAttributeSelector();
        Dataset data = makeTestData();

        // Set the options
        List<Object> options = new LinkedList<>();
        options.add("-useclass");
        options.add("-pearson");
        options.add(0.95);

        sel.setOptions(options);

        // Test it
        sel.train(data);
        List<Integer> selections = sel.getSelections();
        assertEquals(2, selections.size());
        assertTrue(selections.contains(1));
        assertTrue(selections.contains(3));

        // Turn off the use class, should return 0,3 as attributes
        options.remove(0);

        sel.setOptions(options);

        sel.train(data);
        selections = sel.getSelections();
        assertEquals(2, selections.size());
        assertTrue(selections.contains(0));
        assertTrue(selections.contains(3));

        // Lower the threshold to 0.8, should only get the first attribute
        sel.setThreshold(0.8);

        sel.train(data);
        selections = sel.getSelections();
        assertEquals(1, selections.size());
        assertTrue(selections.contains(0));

        // Make sure it doesn't crash with kendall or spearman
        options.set(0, "-spearman");
        sel.setOptions(options);
        assertEquals(RemoveCorrelatedAttributeSelector.CorrelationMeasure.SPEARMAN, sel.Measure);
        sel.train(data);

        options.set(0, "-kendall");
        sel.setOptions(options);
        assertEquals(RemoveCorrelatedAttributeSelector.CorrelationMeasure.KENDALL, sel.Measure);
        sel.train(data);
    }

    @Test
    public void measureCorrelation() throws Exception {
        RemoveCorrelatedAttributeSelector sel = new RemoveCorrelatedAttributeSelector();
        Dataset data = makeTestData();

        sel.setMeasure(RemoveCorrelatedAttributeSelector.CorrelationMeasure.PEARSON);
        assertEquals(0.825637, sel.measureCorrelation(data.getSingleAttributeArray(3), data.getMeasuredClassArray()), 1e-3);

        sel.setMeasure(RemoveCorrelatedAttributeSelector.CorrelationMeasure.SPEARMAN);
        assertEquals(0.8, sel.measureCorrelation(data.getSingleAttributeArray(3), data.getMeasuredClassArray()), 1e-3);

        sel.setMeasure(RemoveCorrelatedAttributeSelector.CorrelationMeasure.KENDALL);
        assertEquals(2f / 3, sel.measureCorrelation(data.getSingleAttributeArray(3), data.getMeasuredClassArray()), 1e-3);
    }

    /**
     * Make a dataset to test this class
     *
     * @return Class to make test
     * @throws Exception
     */
    protected Dataset makeTestData() throws Exception {
        Dataset data = new Dataset();
        for (int i = 0; i < 4; i++) {
            data.addEntry(new BaseEntry());
        }

        // Add attributes
        data.addAttribute("x1", new double[]{1, 2, 3, 4});
        data.addAttribute("x2", new double[]{1.010066492, 2.018900432, 3.068901602, 4.079447963});
        data.addAttribute("x3", new double[]{1.110756607, 4.109451873, 9.087648589, 16.09700455});
        data.addAttribute("x4", new double[]{0.826991946, 0.548999254, 0.33727554, 0.434670001});
        data.setMeasuredClasses(new double[]{3.235480034, 4.012097769, 5.27253711, 6.30265795});

        return data;
    }
}