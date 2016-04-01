package magpie.attributes.generators.crystal;

import magpie.attributes.generators.BaseAttributeGenerator;

/**
 *
 * @author Logan Ward
 */
public class EffectiveCoordinationNumberAttributeGeneratorTest extends 
        CoordinationNumberAttributeGeneratorTest {

    public BaseAttributeGenerator getGenerator() throws Exception {
        return new EffectiveCoordinationNumberAttributeGenerator();
    }
    
}
