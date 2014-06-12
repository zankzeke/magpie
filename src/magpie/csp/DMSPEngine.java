/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.csp;

import java.util.List;
import magpie.csp.diagramdata.PhaseDiagramStatistics;
import magpie.data.materials.PrototypeDataset;
import magpie.models.classification.BaseClassifier;
import magpie.models.classification.CumulantExpansionClassifier;

/**
 * Data-Mining Structure Predictor (DMSP) from <a
 * href="http://www.nature.com/nmat/journal/v5/n8/full/nmat1691.html">Fisher
 * <i>et al</i></a>.
 * 
 * <p><usage><b>Usage</b>: *No options*</usage>
 *
 * <p>
 * <b>Method</b>: Uses strategy described in {@linkplain PhaseDiagramStatistics}
 * to calculate probabilities of each prototype appearing at a certain
 * composition. Using the notation from the that documentation, the goal is to
 * calculate:
 * <center>P(C<sub>j</sub> = y | C<sub>1</sub> = a<sub>1</sub> &cap; ... &cap;
 * C<sub>j-1</sub> = a<sub>j-1</sub>
 * &cap; C<sub>j+1</sub> = a<sub>j+1</sub> &cap; ... &cap; E<sub>N</sub> =
 * Z<sub>N</sub>) </center>
 * 
 * @author Logan Ward
 */
public class DMSPEngine extends CSPEngine {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        // No options
        if (! Options.isEmpty())
            throw new Exception(printUsage());
    }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    

    @Override
    protected BaseClassifier makeClassifier(PhaseDiagramStatistics statistics, PrototypeDataset trainData) {
        CumulantExpansionClassifier clfr = new CumulantExpansionClassifier();
        clfr.setPhaseDiagramStats(statistics);
        clfr.train(trainData);
        return clfr;
    }

}
