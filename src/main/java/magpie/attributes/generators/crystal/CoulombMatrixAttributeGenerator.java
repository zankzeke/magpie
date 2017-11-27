package magpie.attributes.generators.crystal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.CrystalStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.models.regression.crystal.CoulombSineMatrixRegression;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Compute attributes using the Coulomb Sine Matrix representation. Based
 * on work by <a href="http://doi.wiley.com/10.1002/qua.24917">Faber <i>et al.</i></a>.
 * 
 * <p>This method works by computing an approximation for the Coulomb matrix that 
 * considers periodicity. Specifically, we use the Coulomb Sine matrix, which
 * is described in detail in the Faber <i>et al.</i>. For molecules, the Coloumb 
 * matrix is defined as
 * 
 * <center><img src="https://latex.codecogs.com/gif.latex?C_{i,j}&space;=&space;\left\{\begin{matrix}&space;Z_i^{2.4}&space;\text{&space;if&space;}&space;i=j&space;\\&space;\frac{Z_iZ_j}{r_ij}&space;\text{&space;if&space;}&space;i&space;\ne&space;j&space;\end{matrix}\right." title="C_{i,j} = \left\{\begin{matrix} Z_i^{2.4} \text{ if } i=j \\ \frac{Z_iZ_j}{r_ij} \text{ if } i \ne j \end{matrix}\right." /></center>
 * 
 * The eigenvalues of this matrix are then used as attributes. In order to
 * provided a fixed number of attributes, the first N attributes are defined
 * to be the N eigenvalues from the Coulomb matrix. The remaining attributes
 * are defined to be zero.
 * 
 * <p><b>Note</b>: The Coulomb Matrix attributes are dependant on unit cell choice. 
 * Please consider transforming your input crystal structures to the primitive
 * cell before using these attributes.
 * 
 * <usage><p><b>Usage</b>: &lt;n eigenvalues&gt;
 * <pr><br><i>n eigenvalues</i>: Maximum number of Coulomb matrix eigenvalues
 * to use as attributes</usage>
 * @author Logan Ward
 */
public class CoulombMatrixAttributeGenerator extends BaseAttributeGenerator 
        implements Citable {
    /** Maximum number of atoms to consider. Defines number of attributes */
    protected int NEigenvalues = 30;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int nAtoms;
        
        try {
            nAtoms = Integer.parseInt(Options.get(0).toString());
            if (Options.size() > 1) {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        setNEigenvalues(nAtoms);
    }

    @Override
    public String printUsage() {
        return "Usage: <# eigenvalues>";
    }

    /**
     * Set the number of eigenvalues used in representation
     * @param x Desired numbers
     */
    public void setNEigenvalues(int x) {
        this.NEigenvalues = x;
    }
    
    @Override
    public void addAttributes(Dataset data) throws Exception {
        // Create tool to compute eigenvalues
        CoulombSineMatrixRegression computer = new CoulombSineMatrixRegression();
        
        // Check input
        if (! (data instanceof CrystalStructureDataset)) {
            throw new IllegalArgumentException("Data must be a CrystalStructureDataset");
        }
        
        // Create attribute names
        List<String> newNames = new ArrayList<>(NEigenvalues);
        for (int i=0; i<NEigenvalues; i++) {
            newNames.add("CoulombMatrix_Eigenvalue" + i);
        }
        data.addAttributes(newNames);
        
        // Compute attributes
        double[] attrs = new double[NEigenvalues];
        for (BaseEntry e : data.getEntries()) {
            // Compute eigenvalues
            double[] eigen = (double[]) computer.computeRepresentation(
                    ((CrystalStructureEntry) e).getStructure());
            
            // Copy values
            Arrays.fill(attrs, 0);
            System.arraycopy(eigen, 0, attrs, 0, Math.min(eigen.length, NEigenvalues));
            
            // Add attributes to entry
            e.addAttributes(attrs);
        }
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = getClass().getName() + (htmlFormat ? " " : ": ");
        
        output += "(" + NEigenvalues + ") Eigenvalues of the Coloumb Sine matrix, " +
                " as described by ";
        if (htmlFormat) {
            output += "<a href=\"http://doi.wiley.com/10.1002/qua.24917\">Faber <i>et al.</i></a>";
        } else {
            output += "Faber et al.";
        }
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new ArrayList<>();
        
        output.add(new ImmutablePair<>("Introduced methods using the Coulomb Matrix for inorganic crystals", 
                new Citation(this.getClass(), "Article", 
                        new String[]{"F. Faber", "A. Lindmaa", "et al."},
                        "Crystal structure representations for machine learning models of formation energies",
                        "http://doi.wiley.com/10.1002/qua.24917",
                        null)));
                        
        return output;
    }
}
