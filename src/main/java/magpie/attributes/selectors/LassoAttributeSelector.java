package magpie.attributes.selectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import magpie.Magpie;
import magpie.data.Dataset;
import magpie.data.utilities.output.DelimitedOutput;
import magpie.utility.UtilityOperations;
import magpie.utility.interfaces.Citable;
import magpie.utility.interfaces.Citation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Uses a combination of LASSO and screening attribute sets with linear
 * regression to select attributes. Based on work by <a href="http://journals.aps.org/prl/abstract/10.1103/PhysRevLett.114.105503">
 * Ghiringhelli <i>et al.</i></a> and <a href="http://www.nature.com/articles/srep19375">
 * Pilania <i>et al.</i></a>.
 * 
 * <p>This method works by first using compressed sensing (i.e., LASSO) to select
 * a subset of attributes that best describe a property, and then selecting a
 * smaller set of attributes from the LASSO set by finding a set with maximum 
 * fitness with a linear regression model.
 * 
 * <p>There are a few options for how to do this and all are implemented by
 * this class
 * 
 * <ol>
 * <li>How many attributes to select with LASSO
 * <li>Whether to remove attributes that are strongly correlated with others
 * in the LASSO subset
 * <li>Whether to evaluate the linear regression model on the training set
 * or in cross-validation
 * <li>The maximum size of the final attribute set
 * <li>Whether to determine the attribute set size by selecting the attribute
 * set with the highest accuracy in cross-validation
 * </ol>
 * 
 * <usage><p><b>Usage</b>: -n_lasso &lt;# lasso&gt; -max_dim &lt;dim&gt; 
 * [-corr_downselect &lt;# corr&gt;] [-cv_method &lt;cv frac&gt; &lt;cv iters&gt;]
 * [-pick_best] [-debug]
 * <pr><br><i># lasso</i>: Number of attributes to select with LASSO
 * <pr><br><i>max dim</i>: Maximum dimension of final set
 * <pr><br><i># corr</i>: Set size after removing strongly-correlated attributes (by default,
 * this step is not run)
 * <pr><br><i>cv frac</i>: Fraction of entries to withhold during cross validation
 * (by default, cross-validation is not performed)
 * <pr><br><i>cv iter</i>: Number of cross-validation tests to run
 * <pr><br><i>-pick_best</i>: Whether to pick dimension based on cross-validation results
 * <pr><br><i>-debug</i>: Print status messages from the underlying Python script to screen
 * </usage>
 * 
 * @author Logan Ward
 */
public class LassoAttributeSelector extends PythonBasedAttributeSelector 
        implements Citable {
    /** Number of attributes to pick via LASSO. Default = 20 */
    protected int NLASSO = 20;
    /** 
     * Number of entries to downselect to based on correlations. A negative 
     * number indicates that this step should be skipped. Default: Skip
     */
    protected int NDownselect = -1;
    /** Maximum number of attributes to select. Default = 5 */
    protected int MaxCount = 5;
    /** Fraction of entries to withhold of cv test set. A negative number
     * indicates that CV will not be used. Default: No CV*/
    protected double CVFraction = -1;
    /** Number of times to repeat CV test. Default: 100 */
    protected int CVIterations = 100;
    /** Whether to pick dataset size via cross-validation. */
    protected boolean SelectSizeAutomatically = false;
    
    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int nLasso, maxDim; // Mandatory
        int nDown = -1, cvIter = -1;
        double cvFrac = -1;
        boolean pickBest = false;
        Debug = false;
        
        try {
            // Mandatory arguments
            nLasso = Integer.parseInt(Options.get(1).toString());
            maxDim = Integer.parseInt(Options.get(3).toString());
            int pos = 4;
            while (pos < Options.size()) {
                String tag = Options.get(pos).toString().toLowerCase();
                switch (tag) {
                    case "-corr_downselect":
                        nDown = Integer.parseInt(Options.get(++pos).toString());
                        break;
                    case "-cv_method":
                        cvFrac = Double.parseDouble(Options.get(++pos).toString());
                        cvIter = Integer.parseInt(Options.get(++pos).toString());
                        break;
                    case "-pick_best":
                        pickBest = true;
                        break;
                    case "-debug":
                        Debug = true;
                        break;
                    default:
                        throw new Exception();
                }
                pos++;
            } 
        } catch (Exception e) {
            throw new Exception(printUsage());
        }

        // Set options
        setNLASSO(nLasso);
        setNDownselect(nDown);
        setCVFraction(cvFrac);
        if (cvFrac > 0) {
            setCVIterations(cvIter);
        }
        setMaximumDimension(maxDim);
        setSelectSizeAutomatically(pickBest);
    }

    @Override
    public String printUsage() {
        return "Usage: -n_lasso <# lasso> -max_dim <dim> [-corr_downselect <# corr>] [-cv_method <cv frac> <cv iters>] [-pick_best]";
    }

    @Override
    protected String getScriptPath() {
        return "py/lasso_attribute_selection.py";
    }

    /**
     * Set the fraction of entries to without during cross-validation. Set to a negative number
     * to use training set fitness rather than CV score
     * @param cvFraction Fraction of entries to use as test set
     */
    public void setCVFraction(double cvFraction) {
        if (cvFraction > 1) {
            throw new IllegalArgumentException("fraction must be < 1");
        }
        this.CVFraction = cvFraction;
    }

    /**
     * Set the number of iterations to perform during cross-validation.
     * @param nIter Number of iterations
     */
    public void setCVIterations(int nIter) {
        if (nIter <= 0) {
            throw new IllegalArgumentException("number of iterations must be positive");
        }
        this.CVIterations = nIter;
    }

    /**
     * Set the maximum number of attributes to select
     * @param count Maximum number of attributes
     */
    public void setMaximumDimension(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("dimension must be positive");
        }
        this.MaxCount = count;
    }

    /**
     * Set number of attributes to downselect to by removing strongly-correlated
     * entries. Set to negative to skip this step
     * @param num Size of dataset after downselection
     */
    public void setNDownselect(int num) {
        this.NDownselect = num;
    }

    /**
     * Set whether to determine number of attributes through cross-validation.
     * @param x Desired setting
     */
    public void setSelectSizeAutomatically(boolean x) {
        this.SelectSizeAutomatically = x;
    }

    /**
     * Set the number of parameters to determine via LASSO
     * @param NLASSO Number of attributes
     */
    public void setNLASSO(int NLASSO) {
        this.NLASSO = NLASSO;
    }
    
    /**
     * Prepare the system call with all command-line arguments
     * 
     * @param codePath Path to executable or script to be run
     * @param data Dataset being used to train attribute selector
     * @return Command to be executed
     */
    protected List<String> assembleSystemCall(File codePath, Dataset data) {
        List<String> call = new LinkedList<>();
        call.add("python");
        call.add(codePath.getAbsolutePath());
        call.add("-n_lasso");
        call.add(Integer.toString(NLASSO));
        if (NDownselect > 0) {
            call.add("-corr_downselect"); call.add(Integer.toString(NDownselect));
        }
        call.add("-max_dim");
        call.add(Integer.toString(MaxCount));
        call.add("-n_procs");
        call.add(Integer.toString(Magpie.NThreads));
        if (SelectSizeAutomatically) {
            call.add("-pick_best");
        }
        if (CVFraction > 0) {
            call.add("-cv_method"); call.add(Double.toString(CVFraction));
            call.add(Integer.toString(CVIterations));
        }
        return call;
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = "Uses compressed sensing (LASSO) and linear regression to identify a subset of "
                + "attributes that make the best linear model. ";
        
        // Number of LASSO steps
        output += String.format("First, a %d attribute model is created with LASSO"
                + " and these attributes are used as a starting set.", NLASSO);
        if (NDownselect > 0) {
            output += String.format(" Next, the set of attributes is reduced to "
                    + "%d by removing iteratively removing the attribute with "
                    + "most correlation with the other attributes.", NDownselect);
        }
        
        output += " The final subset of ";
        if (SelectSizeAutomatically) {
            output += String.format("1 to %d", MaxCount);
        } else {
            output += MaxCount;
        }
        output += " attributes is selected to be the set that generates a linear "
                + "regression model with the lowest mean squared error ";
        if (CVFraction > 0) {
            output += String.format("over %d iterations of a hold-%.1f%%-out "
                    + "cross-validation test.", CVIterations, CVFraction * 100);
        } else {
            output += "on the whole dataset.";
        }
        
        return output;
    }

    @Override
    public List<Pair<String, Citation>> getCitations() {
        List<Pair<String, Citation>> output = new LinkedList<>();
        
        // Always Ghiringhelli paper
        output.add(new ImmutablePair<>("Introduced the LASSO attribute selector",
                new Citation(this.getClass(),
                "Article",
                new String[]{"L. Ghiringhelli", "et al."},
                "Big Data of Materials Science: Critical Role of the Descriptor",
                "http://link.aps.org/doi/10.1103/PhysRevLett.114.105503",
                null
            )));
        
        // Optionally: Pilania's
        if (CVFraction > 0 || NDownselect > 0) {
            output.add(new ImmutablePair<>("Extended LASSO selector to use cross-validation"
                    + " and a scheme for removing correlated attributes",
                new Citation(this.getClass(),
                "Article",
                new String[]{"G. Pilania", "et al."},
                "Machine learning bandgaps of double perovskites",
                "http://www.nature.com/articles/srep19375",
                null
            )));
        }
        
        return output;
    }    
}
