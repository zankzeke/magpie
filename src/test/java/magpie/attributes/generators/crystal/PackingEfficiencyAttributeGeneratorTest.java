package magpie.attributes.generators.crystal;

import magpie.attributes.generators.BaseAttributeGenerator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class PackingEfficiencyAttributeGeneratorTest 
        extends CoordinationNumberAttributeGeneratorTest {

    @Override
    public BaseAttributeGenerator getGenerator() {
        return new PackingEfficiencyAttributeGenerator();
    }

    @Override
    public int expectedCount() {
        return 1;
    }
    
}
