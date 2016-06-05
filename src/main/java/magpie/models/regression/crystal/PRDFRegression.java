package magpie.models.regression.crystal;

import java.util.*;
import magpie.data.materials.util.LookupData;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.*;
import org.apache.commons.math3.stat.StatUtils;
import vassal.analysis.PairDistanceAnalysis;
import vassal.data.Cell;

/**
 * Uses the partial radial distribution function to perform regression. Each material
 * is represented by a matrix containing the PRDF between each element type.
 * Distance between structures are computed as Frobenius norm of the difference
 * between their PRDF matrices.
 * If you use this method, please cite <a href="http://link.aps.org/doi/10.1103/PhysRevB.89.205118">
 * Schutt, Glawe, et al. PRB (2015)</a>.
 * 
 * <usage><p><b>Usage</b>: &lt;lambda&gt; &lt;sigma&gt; &lt;cutoff&gt; &lt;bins&gt;
 * <br><pr><i>lambda</i>: Regularization parameter
 * <br><pr><i>sigma</i>: Normalization parameter in kernel function.
 * <br><pr><i>cutoff</i>: Distance cutoff used for PRDF (Angstroms)
 * <br><pr><i>bins</i>: Number of bins used in PRDF</usage>
 * @author Logan Ward
 */
public class PRDFRegression extends StructureKRRBasedRegression {
    /** Normalization term in kernel function */
    private double Sigma = 1;
    /** Number of bins in PRDF */
    private int NBins = 25  ;
    /** Cutoff distance of PRDF */
    private double Cutoff = 7.0;

    /**
     * Set the normalization parameter in the kernel function.
     * @param sigma Desired normalization parameter.
     */
    public void setSigma(double sigma) {
        this.Sigma = sigma;
    }

    /**
     * Set the number of bins used when computing the PRDF.
     * @param nBins Number of bins (>1)
     */
    public void setNBins(int nBins) {
        this.NBins = nBins;
    }

    /**
     * Set the cutoff distance used when computing the PRDF 
     * @param cutoff Cutoff distance (>0 Angstrom)
     */
    public void setCutoff(double cutoff) {
        this.Cutoff = cutoff;
    }
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        double sig, lam, cutoff;
        int bins;
        try {
            lam = Double.parseDouble(Options.get(0).toString());
            sig = Double.parseDouble(Options.get(1).toString());
            cutoff = Double.parseDouble(Options.get(2).toString());
            bins = Integer.parseInt(Options.get(3).toString());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        setSigma(sig);
        setLambda(lam);
        setCutoff(cutoff);
        setNBins(bins);
    }

    @Override
    public String printUsage() {
        return "Usage: <lambda> <sigma> <cutoff> <nbins>";
    }

    @Override
    protected double computeSimiliarity(Object strc1, Object strc2) {
        Map<ImmutablePair<Integer,Integer>, double[]> rep1 = 
                (Map<ImmutablePair<Integer, Integer>, double[]>) strc1;
        Map<ImmutablePair<Integer,Integer>, double[]> rep2 = 
                (Map<ImmutablePair<Integer, Integer>, double[]>) strc2;
        
        // Compile a complete list of the element pairs for both structures
        Set<ImmutablePair<Integer,Integer>> pairs = new TreeSet<>();
        pairs.addAll(rep1.keySet());
        pairs.addAll(rep2.keySet());
        
        // For each pair, compute the squared differences between the two PRDFs
        //  This is equivalent to the Froebius norm
        double difference = 0;
        for (ImmutablePair<Integer,Integer> pair : pairs) {
            double[] prdf1 = rep1.get(pair);
            double[] prdf2 = rep2.get(pair);
            
            if (prdf1 == null) {
                // Assume prdf1 == 0
                difference += StatUtils.sumSq(prdf2);
            } else if (prdf2 == null) {
                // Assume prdf2 == 0
                difference += StatUtils.sumSq(prdf1);
            } else {
                double x;
                for (int i=0; i<prdf1.length; i++) {
                    x = prdf1[i] - prdf2[i];
                    difference += x * x;
                }
            }
        }
        
        // Compute kernel function to get similarity
        return Math.exp(-1 * difference / Sigma);
    }

    /**
     * Compute the pair distribution function
     * @param strc Structure to be evaluated
     * @return Map where the key is (Z_a,Z_b) and the value is the PRDF
     */
    @Override
    public Object computeRepresentation(Cell strc) {
        // Get the atomic number of each type
        int[] typeZ = new int[strc.nTypes()];
        for (int i=0; i<typeZ.length; i++) {
            String name = strc.getTypeName(i);
            typeZ[i] = ArrayUtils.indexOf(LookupData.ElementNames, name);
            if (typeZ[i] == ArrayUtils.INDEX_NOT_FOUND) {
                throw new Error("No such element: " + name);
            }
        }
        
        // Compute the PRDF of this structure
        PairDistanceAnalysis pda = new PairDistanceAnalysis();
        pda.setCutoffDistance(Cutoff);
        try {
            pda.analyzeStructre(strc);
        } catch (Exception e) {
            throw new Error(e);
        }
        double[][][] prdf = pda.computePRDF(NBins);
        
        // Store them as a RealMatrix 
        Map<ImmutablePair<Integer, Integer>,double[]> rep;
        rep = new TreeMap<>();
        for (int i=0; i<prdf.length; i++) {
            for (int j=0; j<prdf.length; j++) {
                rep.put(new ImmutablePair<>(typeZ[i],typeZ[j]), prdf[i][j]);
            }
        }
        
        return rep;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = super.getCitations(); 
        
        output.add(new ImmutablePair<>("Introduced use of PRDF for predicting properties of crystalline solids",
                new Citation(this.getClass(), 
                        "Article",
                        new String[]{"Schutt, K.T.", "et al."},
                        "How to represent crystal structures for machine learning: Towards fast prediction of electronic properties",
                        "http://link.aps.org/doi/10.1103/PhysRevB.89.205118",
                        null)));
        
        return output;
    }
    
}
