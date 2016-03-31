package magpie.attributes.generators.crystal;

import org.apache.commons.lang3.tuple.Pair;
import vassal.analysis.VoronoiCellBasedAnalysis;

/**
 * Compute attributes based on the local variance in elemental properties
 * around each atom. 
 * 
 * <p>Variance in property is computed by first finding the face-weighted 
 * mean property of 
 * 
 * 
 * @author Logan Ward
 * @see LocalPropertyDifferenceAttributeGenerator
 */
public class LocalPropertyVarianceAttributeGenerator 
        extends LocalPropertyDifferenceAttributeGenerator {

    public LocalPropertyVarianceAttributeGenerator() {
        super();
        AttrName = "NeighVar";
        AttrDescription = "variance in elemental properties for neighbors";
    }

    public LocalPropertyVarianceAttributeGenerator(int... shells) {
        super(shells);
        AttrName = "NeighVar";
        AttrDescription = "variance in elemental properties for neighbors";
    }    

    @Override
    protected double[] getAtomProperties(VoronoiCellBasedAnalysis voro, Pair<int[][], double[][]> faceInfo, double[] propValues) {
        return voro.neighborPropertyVariances(propValues, faceInfo.getRight(),
                faceInfo.getLeft());
    }

    
}
