package magpie.attributes.generators.crystal;

import java.util.LinkedList;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.Dataset;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Attributes based on the Atomic Property Weighted Radial Distribution Function
 * (AP-RDF) approach of <a href="http://dx.doi.org/10.1021/jz501331m">
 * Fernandez <i>et al.</i></a>. 
 * 
 * <p>User can specify the cutoff distance for the AP-RDF, the number of points
 * to evaluate it, the smoothing factors, and the properties used for weighting.
 * The recommended values of these parameters have yet to be determined, please
 * contact Logan Ward, or the authors of this paper if you have questions.
 * 
 * <usage><p><b>Usage</b>: &lt;cutoff&gt; &lt;# points&gt; &lt;smoothing&gt;
 * &lt;properties&gt;
 * <br><pr><i>cutoff</i>: Cutoff distance for the AP-RDF
 * <br><pr><i># points</i>: Number of unique distances to evaluate
 * <br><pr><i>smoothing</i>: Parameter used to smooth function
 * <br><pr><i>properties</i>: Properties used to weight the RDF</usage>
 * 
 * @author Logan Ward
 */
public class APRDFAttributeGenerator extends BaseAttributeGenerator 
        implements Citable {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void addAttributes(Dataset data) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new LinkedList<>();
        
        // Add in citation to Fernandez paper
        output.add(new ImmutablePair<>("Introduced AP-RDF for MOFs",
                new Citation(this.getClass(),
                        "Article", 
                        new String[]{"M. Fernandez", "et al."},
                        "Rapid and Accurate Machine Learning Recognition of High Performing Metal Organic Frameworks for CO 2 Capture",
                        "http://dx.doi.org/10.1021/jz501331m",
                        null)
        ));
        
        return output;
    }
}
