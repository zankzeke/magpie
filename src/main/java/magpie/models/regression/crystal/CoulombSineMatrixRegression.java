package magpie.models.regression.crystal;

import java.util.List;
import magpie.data.materials.util.LookupData;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import vassal.data.Cell;

/**
 * Perform regression based on the Coulomb Sine Matrix approach of 
 * <a href="http://arxiv.org/abs/1503.07406">Faber <i>et al.</i></a>. 
 * 
 * <p>LW 1Apr15: This method does not appear to be insensitive to basis cell selection.
 * 
 * <usage><p><b>Usage</b>: &lt;lambda&gt; &lt;sigma&gt;
 * <br><pr><i>lambda</i>: Regularization parameter
 * <br><pr><i>sigma</i>: Normalization parameter in kernel function.</usage>
 * 
 * @author Logan Ward
 */
public class CoulombSineMatrixRegression extends StructureKRRBasedRegression {
    /** Normalization term in kernel function */
    private double Sigma = 1;

    /**
     * Set the normalization parameter in the kernel function.
     * @param Sigma Desired normalization parameter.
     */
    public void setSigma(double Sigma) {
        this.Sigma = Sigma;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        double sig, lam;
        try {
            lam = Double.parseDouble(Options.get(0).toString());
            sig = Double.parseDouble(Options.get(1).toString());
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        setSigma(sig);
        setLambda(lam);
    }

    @Override
    public String printUsage() {
        return "Usage: <lambda> <sigma>";
    }

    @Override
    protected double computeSimiliarity(Object strc1, Object strc2) {
        double[] e1 = (double[]) strc1;
        double[] e2 = (double[]) strc2;
        
        // Determine which is bigger
        double[] diff = e1.length > e2.length ? e1.clone() : e2.clone();
        if (e1.length > e2.length) {
            for (int i = 0; i < e2.length; i++) {
                diff[i] -= e2[i];
            }
        } else {
            for (int i = 0; i < e1.length; i++) {
                diff[i] -= e1[i];
            }
        }
        
        // Compute the L1 distance
        double dist = 0;
        for (double d : diff) {
            dist += Math.abs(d);
        }
        
        // Compute the Laplacian
        return Math.exp(-1 * dist / Sigma);
    }

    @Override
    public Object computeRepresentation(Cell strc) {
        // First, generate the Coloumb matrix
        RealMatrix matrix = computeCoulombMatrix(strc);
        
        // Compute the eigenvalues
        EigenDecomposition eign = new EigenDecomposition(matrix);
        return eign.getRealEigenvalues();
    }
    
    /**
     * Computes the Coulomb sine matrix. Equation 24 of the paper describing 
     * this method.
     * @param strc Structure to be evaluated
     * @return Coulomb sine matrix
     */
    protected RealMatrix computeCoulombMatrix(Cell strc) {
        // Get basis vectors
        RealMatrix basis = new Array2DRowRealMatrix(strc.getBasis(), false);
        
        // Get reciprocal basis vectors
        RealMatrix basisInverse = 
                new Array2DRowRealMatrix(strc.getInverseBasis(), false);
                
        // Create output matrix
        RealMatrix output = new BlockRealMatrix(strc.nAtoms(), strc.nAtoms());
        
        // Get the positions for each atom
        RealVector[] positions = new RealVector[strc.nAtoms()];
        for (int a=0; a<strc.nAtoms(); a++) {
            double[] pos = strc.getAtom(a).getPositionCartesian();
            positions[a] = new ArrayRealVector(pos);
        }
        
        // Get the atomic number of each element
        int[] typeZ = new int[strc.nTypes()];
        for (int i=0; i<strc.nTypes(); i++) {
            typeZ[i] = ArrayUtils.indexOf(LookupData.ElementNames, 
                    strc.getTypeName(i)) + 1;
            if (typeZ[i] == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Error("No such element: " + strc.getTypeName(i));
            }
        }
        
        // Compute all terms
        for (int r1=0; r1<strc.nAtoms(); r1++) {
            output.setEntry(r1, r1,
                    0.5 * Math.pow(typeZ[strc.getAtom(r1).getType()], 2.4));
            for (int r2=r1+1; r2<strc.nAtoms(); r2++) {
                // Displacement between the two atoms
                RealVector disp = positions[r1].subtract(positions[r2]);
                
                // Convert to fractional coordinates
                disp = basisInverse.operate(disp);
                
                // Multiply by pi and compute sin^2 of each element
                for (int i=0; i<3; i++) {
                    double sinex = Math.sin(disp.getEntry(i) * Math.PI);
                    disp.setEntry(i, sinex * sinex);
                }
                
                // Multiple by basis vectors
                disp = basis.operate(disp);
                
                // Get the result
                double res = 1.0 / disp.getNorm();
                res *= typeZ[strc.getAtom(r1).getType()]
                        * typeZ[strc.getAtom(r2).getType()];
                output.setEntry(r1, r2, res);
                output.setEntry(r2, r1, res);
            }
        }
        
        return output;
    }
    
    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = super.getCitations(); 
        
        output.add(new ImmutablePair<>("Adapted Coulomb Matrix for predicting properties of crystalline solids",
                new Citation(this.getClass(), 
                        "Article",
                        new String[]{"Faber, F.", "et al."},
                        "Crystal structure representations for machine learning models of formation energies",
                        "http://doi.wiley.com/10.1002/qua.24917",
                        null)));
        
        return output;
    }
}
