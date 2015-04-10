package magpie.models.regression.crystal;

import java.util.List;
import magpie.data.materials.util.LookupData;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.special.Erf;
import vassal.data.Cell;
import vassal.util.VectorCombinationComputer;

/**
 * Perform regression based on the Ewald Matrix approach of 
 * <a href="http://arxiv.org/abs/1503.07406">Faber <i>et al.</i></a>. 
 * 
 * <usage><p><b>Usage</b>: &lt;lambda&gt; &lt;sigma&gt;
 * <br><pr><i>lambda</i>: Regularization parameter
 * <br><pr><i>sigma</i>: Normalization parameter in kernel function.</usage>
 * 
 * @author Logan Ward
 */
public class CoulombEwaldMatrixRegression extends StructureKRRBasedRegression {
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
            throw new Exception(printUsage());
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
        Vector3D[] latVecs = strc.getLatticeVectors();
        
        // Get reciprocal lattice vectors
        Vector3D[] recipLatVecs = strc.getReciprocalLatticeVectors();
        for (int i=0; i<3; i++) {
            recipLatVecs[i] = recipLatVecs[i].scalarMultiply(Math.PI * 2);
        }
        
        // Determine parameters for Ewald sum
        //   Following http://protomol.sourceforge.net/ewald.pdf
        double V = strc.volume();
        double alpha = Math.sqrt(Math.PI) * Math.pow(0.01 * strc.nAtoms() / V, 1.0 / 6.0);
        double epsilon = 1e-12;
        double Lmax = Math.sqrt(-Math.log(epsilon)) / alpha;
        double Gmax = 2 * alpha * Math.sqrt(-Math.log(epsilon));
        
        // Get the lattice (and reciprocal lattice vectors) over which 
        //   the Ewald sum will occur
        List<Vector3D> real = new VectorCombinationComputer(latVecs, Lmax, false).getVectors();
        List<Vector3D> recip = new VectorCombinationComputer(recipLatVecs, Gmax, false).getVectors();
        
        // Compute prefactors of reciprocal space vectors
        double[] recipPrefactor = new double[recip.size()];
        for (int i=0; i<recip.size(); i++) {
            double norm = recip.get(i).getNormSq();
            recipPrefactor[i] = Math.exp(-1 * norm / 4 / alpha / alpha) / Math.PI / V;
        }
        
        // Create output matrix
        RealMatrix output = new BlockRealMatrix(strc.nAtoms(), strc.nAtoms());
        
        // Get the positions for each atom
        Vector3D[] positions = new Vector3D[strc.nAtoms()];
        for (int a=0; a<strc.nAtoms(); a++) {
            double[] pos = strc.getAtom(a).getPositionCartesian();
            positions[a] = new Vector3D(pos);
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
        int [] Z = new int[strc.nAtoms()];
        for (int i=0; i<strc.nAtoms(); i++) {
            Z[i] = typeZ[strc.getAtom(i).getType()];
        }
        
        
        // Compute all terms
        for (int r1=0; r1<strc.nAtoms(); r1++) {
            for (int r2=r1; r2<strc.nAtoms(); r2++) {
                // Shortest displacement between the two atoms
                Vector3D disp = positions[r1].subtract(strc.getClosestImage(r1, r2).getPosition());
                
                // Compute real space contribution
                double xReal = 0.0;
                for (Vector3D L : real) {
                    double dist = disp.add(L).getNorm();
                    if (dist > 0) {
                        xReal += Erf.erfc(alpha * dist) / dist;
                    }
                }
                if (r1 != r2) {
                    xReal += Erf.erfc(alpha * disp.getNorm()) / disp.getNorm();
                }
                xReal *= Z[r1] * Z[r2];
                        
                // Compute the reciprocal space contribution
                double xRecip = 0.0;
                for (int i=0; i<recip.size(); i++) {
                    xRecip += recipPrefactor[i] * Math.cos(recip.get(i).dotProduct(disp));
                }
                xRecip *= Z[r1] * Z[r2];
                
                // Compute the zero term
                double xZero;
                if (r1 != r2) {
                    xZero = -1 * ((Z[r1] * Z[r1] + Z[r2] * Z[r2]) 
                        * alpha / Math.sqrt(Math.PI)
                        + (Z[r1] + Z[r2]) * (Z[r1] + Z[r2]) 
                        * Math.PI / 2 / V / alpha / alpha);
                } else {
                    xZero = -1 * Z[r1] * Z[r1] 
                        * (alpha / Math.sqrt(Math.PI)
                        + Math.PI / 2 / V / alpha / alpha);
                }
                
                // Multiply by pi and compute sin^2 of each element
                if (r1 == r2) {
                    output.setEntry(r1, r1, (xReal + xRecip + xZero) / 2);
                } else {
                    double x = xReal + xRecip + xZero;
                    output.setEntry(r2, r1, x);
                    output.setEntry(r1, r2, x);
                }
            }
        }
        
        return output;
    }
}
