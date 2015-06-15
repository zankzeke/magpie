package magpie.data.materials;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CompositionDatasetTest {
    
    @Test
    public void testDescription() throws Exception {
        // Make dataset
        CompositionDataset data = new CompositionDataset();
        data.addEntry("NaCl");
        data.addEntry("Fe2O3");
        data.addElementalProperty("Number");
        data.addElementalProperty("Electronegativity");
        
        // Generate attributes
        data.generateAttributes();
        
        // Print description in HTML and normal format
        System.out.println("Plain Format");
        System.out.println(data.printDescription(false));
        System.out.println("HTML Format");
        System.out.println(data.printDescription(true));
    }
    
}
