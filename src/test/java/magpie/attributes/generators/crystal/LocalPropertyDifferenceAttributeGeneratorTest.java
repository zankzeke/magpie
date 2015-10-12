package magpie.attributes.generators.crystal;

import magpie.attributes.generators.BaseAttributeGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class LocalPropertyDifferenceAttributeGeneratorTest 
        extends CoordinationNumberAttributeGeneratorTest {

    @Override
    public BaseAttributeGenerator getGenerator() {
        return new LocalPropertyDifferenceAttributeGenerator();
    }

    @Override
    public int expectedCount() {
        return 4;
    }
    
}
