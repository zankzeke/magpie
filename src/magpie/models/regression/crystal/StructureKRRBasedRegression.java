package magpie.models.regression.crystal;

import java.util.*;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.materials.AtomicStructureEntry;
import magpie.data.materials.CrystalStructureDataset;
import magpie.models.regression.BaseRegression;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import vassal.data.Cell;

/**
 * Abstract class for methods that predict properties of crystal structures with 
 * KKR-based schemes. Data must be a {@linkplain CrystalStructureDataset}.
 * 
 * <p>At the moment, this classes uses Weka to perform the regression.
 * 
 * <p><b>How to Extend this Class</b>
 * 
 * <ol>
 * <li>Implement {@linkplain #computeRepresentation(vassal.data.Cell) } to create
 * a representation of a structure.
 * <li>Implement {@link #computeSimiliarity(Object, Object)}
 * to compute the similarity between two structures based on their representations.
 * Note: This operation should include your kernel function.
 * <li>Implement {@linkplain #setOptions(java.util.List) } and 
 * <li>Blame Logan Ward if there's something else he forgot to list!
 * </ol>
 * 
 * @author Logan Ward
 */
abstract public class StructureKRRBasedRegression extends BaseRegression {
    /** Regularization parameter (lambda) */
    private double Lambda;
    /** Coefficients of model */
    private double[] Alpha;
    /** Representation of structures in the training set. */
    private List<Object> TrainingStructures;

    @Override
    public StructureKRRBasedRegression clone() {
        StructureKRRBasedRegression x = (StructureKRRBasedRegression) super.clone();
        if (Alpha != null) {
            x.TrainingStructures = new ArrayList<>(TrainingStructures);
            x.Alpha = Alpha.clone();
        }
        return x;
    }

    /**
     * Set the ridge regularization parameter.
     * @param lambda 
     */
    public void setLambda(double lambda) {
        resetModel();
        this.Lambda = lambda;
    }

    @Override
    protected void train_protected(Dataset TrainData) {
        if (! (TrainData instanceof CrystalStructureDataset)) {
            throw new Error("Data must be a CrystalStructureDataset");
        }
        
        // Retrieve the crystal structures
        TrainingStructures = new ArrayList<>(TrainData.NEntries());
        CrystalStructureDataset ptr = (CrystalStructureDataset) TrainData;
        for (int e=0; e<TrainData.NEntries(); e++) {
            Cell strc = ptr.getEntry(e).getStructure();
            TrainingStructures.add(computeRepresentation(strc));
        }
        
        // Compute similiarity between each crystal structure
        RealMatrix K = new BlockRealMatrix(TrainingStructures.size(),
                TrainingStructures.size());
        for (int a1=0; a1<TrainingStructures.size(); a1++) {
            K.setEntry(a1, a1, 1.0 + Lambda);
            for (int a2=a1+1; a2<TrainingStructures.size(); a2++) {
                double sim = computeSimiliarity(TrainingStructures.get(a1),
                        TrainingStructures.get(a2));
                K.setEntry(a1, a2, sim);
                K.setEntry(a2, a1, sim);
            }
        }
        
        // Compute the terms
        RealVector y = new ArrayRealVector(TrainData.getMeasuredClassArray());
        Alpha = new CholeskyDecomposition(K).getSolver().solve(y).toArray();
    }

    @Override
    public void run_protected(Dataset TrainData) {
        try {
            for (BaseEntry ptr : TrainData.getEntries()) {
                AtomicStructureEntry entry = (AtomicStructureEntry) ptr;
                Object rep = computeRepresentation(entry.getStructure());
                double y = 0;
                for (int a = 0; a < TrainingStructures.size(); a++) {
                    double sim = computeSimiliarity(rep, TrainingStructures.get(a));
                    y += Alpha[a] * sim;
                }
                entry.setPredictedClass(y);
            }
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    
    /**
     * Compute similarity between two crystal structures.
     *
     * @param strc1 Representation of structure #1
     * @param strc2 Representation of structure #2
     * @return Similarity between two structures
     */
    abstract protected double computeSimiliarity(Object strc1, Object strc2);
    
    /**
     * Given a structure, compute representation of the structure. For instance,
     * this code could be used to compute the RDF or Coulomb matrix.
     * @param strc Structure
     * @return Representation of the structure
     */
    abstract public Object computeRepresentation(Cell strc);

    @Override
    protected String printModel_protected() {
        return String.format("KRR model with %d points and a lambda = %.2f", 
                TrainingStructures.size(), Lambda);
    }    

    @Override
    public int getNFittingParameters() {
        return 1;
    }
    
    
}
