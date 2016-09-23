package magpie.attributes.selectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import magpie.Magpie;
import magpie.data.Dataset;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.models.regression.RandomGuessRegression;
import magpie.statistics.performance.ClassificationStatistics;
import magpie.statistics.performance.RegressionStatistics;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * From all possible subsets, find the one that leads to the highest CV score.
 * 
 * <p>Iterates through all possible attribute subsets between two user-defined sizes.
 * Each subset is rated by measuring which can be used to train a model with the
 * highest score in cross-validation. For regression models, the subset with the 
 * lowest RMSE is selected and the model with the highest accuracy is used for 
 * classification models.
 * 
 * <usage><p><b>Usage</b>: $&lt;model&gt; [-min_size &lt;min&gt;]
 * [-max_size &lt;max&gt;] [-n_folds &lt;k&gt;]
 * <pr><br><i>model</i>: {@linkplain BaseModel} used to test attribute sets
 * <pr><br><i>min</i>: Minimum attribute set size (default=1)
 * <pr><br><i>max</i>: Maximum attribute set size (default=4)
 * <pr><br><i>k</i>: Number of folds to use during cross-validation (default=10)</usage>
 * 
 * @author Logan Ward
 */
public class ExhaustiveAttributeSelector extends BaseAttributeSelector {
    /** Model used for cross-validation */
    protected BaseModel Model = new RandomGuessRegression();
    /** Minimum attribute subset size */
    protected int MinSubsetSize = 1;
    /** Maximum attribute subset size */
    protected int MaxSubsetSize = 4;
    /** Number of folds used in cross-validation test */
    protected int KFolds = 10;
    
    // State variables used to store the progress of the search
    /** Iterator over subsets of attributes to be tested */
    protected Iterator<int[]> SetIterator;

    @Override
    public BaseAttributeSelector clone() {
        ExhaustiveAttributeSelector selector = 
                (ExhaustiveAttributeSelector) super.clone();
        
        selector.Model = Model.clone();
        
        return selector;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        int n_folds = 10;
        int min_size = 1;
        int max_size = 4;
        BaseModel model;
        
        try {
            model = (BaseModel) Options.get(0);
            
            // Loop through optional arguments
            int pos=1;
            while (pos < Options.size()) {
                switch (Options.get(pos).toString().toLowerCase()) {
                    case "-min_size":
                        pos++;
                        min_size = Integer.parseInt(Options.get(pos).toString());
                        break;
                    case "-max_size":
                        pos++;
                        max_size = Integer.parseInt(Options.get(pos).toString());
                        break;
                    case "-n_folds":
                        pos++;
                        n_folds = Integer.parseInt(Options.get(pos).toString());
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                pos++;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(printUsage());
        }
        
        // Set the choices
        setModel(model);
        setMinSubsetSize(min_size);
        setMaxSubsetSize(max_size);
        setNFolds(n_folds);
    }

    @Override
    public String printUsage() {
        return "Usage: $<model> [-min_size <min>] [-max_size <max>] [-n_folds <k>]";
    }

    /**
     * Set model used to test subset performance
     * @param model Model template
     */
    public void setModel(BaseModel model) {
        this.Model = model.clone();
    }

    /**
     * Set minimum size of subset to be tested
     * @param size Desired minimum size
     */
    public void setMinSubsetSize(int size) {
        this.MinSubsetSize = size;
    }

    /**
     * Set maximum size of subset to be tested
     * @param size Desired maximum size
     */
    public void setMaxSubsetSize(int size) {
        this.MaxSubsetSize = size;
    }
    
    /**
     * Set the number of folds used in K-fold cross-validation.
     * @param k Number of folds
     */
    public void setNFolds(int k) {
        KFolds = k;
    }

    @Override
    protected List<Integer> train_protected(Dataset data) {
        // Record the initial thread count
        int originalNThreads = Magpie.NThreads;
        Magpie.NThreads = 1;
        
        // Generate the iterator over all test sets
        setCombinationIterator(data);
        
        // Prepare to launch threads
        ExecutorService executor = Executors.newFixedThreadPool(originalNThreads);
        final BaseModel modelPtr = Model;
        final Dataset datasetPtr = data;
        final long randomSeed = new Random().nextLong();
        
        // Launch them
        List<Future<Pair<int[], Double>>> bestSets = new ArrayList<>(originalNThreads);
        for (int n=0; n<originalNThreads; n++) {
            Callable<Pair<int[], Double>> thread = new Callable<Pair<int[], Double>>() {
                @Override
                public Pair<int[], Double> call() throws Exception {
                    // Make a local copy of the model
                    BaseModel myModel = modelPtr.clone();
                    
                    // Check whether this is a classifier
                    boolean isClassifier = myModel instanceof AbstractClassifier;
                    
                    // Initialize the outputs for this model
                    int[] bestSet = null;
                    double bestScore = isClassifier ? 0.0 : Double.POSITIVE_INFINITY;
                    
                    // Loop until no more sets to run
                    while (true) {
                        int[] curSet = getNextSet();
                        
                        // If set is null, no more sets are available
                        if (curSet == null) {
                            break;
                        }
                        
                        // Get dataset with only the subset attributes
                        Dataset curData = datasetPtr.clone();
                        List<Integer> selections = 
                                Arrays.asList(ArrayUtils.toObject(curSet));
                        applyAttributeSelection(selections, curData);
                        
                        // Run cross-validation using the same random seed as 
                        //  the previous tests
                        myModel.crossValidate(KFolds, curData, randomSeed);
                        
                        // Check if results is better
                        double curScore = isClassifier ?
                                ((ClassificationStatistics) myModel.ValidationStats).Accuracy 
                                : ((RegressionStatistics) myModel.ValidationStats).RMSE;
                        if (isClassifier) {
                            if (curScore > bestScore) {
                                bestScore = curScore;
                                bestSet = curSet;
                            }
                        } else {
                            if (curScore < bestScore) {
                                bestScore = curScore;
                                bestSet = curSet;
                            }
                        }
                    }
                    
                    return new ImmutablePair<>(bestSet, bestScore);
                };
            };
            
            // Launch the thread
            bestSets.add(executor.submit(thread));
        }
        
        // Initialize the best scores for all threads
        boolean isClassifier = Model instanceof AbstractClassifier;
        int[] bestSet = null;
        double bestScore = isClassifier ? 0.0 : Double.POSITIVE_INFINITY;
        
        // As results come back, find the best one
        executor.shutdown();
        for (Future<Pair<int[], Double>> invocation : bestSets) {
            // Get the result from the thread
            Pair<int[], Double> result;
            try {
                result = invocation.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            
            // Check whether it beats the current best
            if (isClassifier) {
                if (result.getValue() > bestScore) {
                    bestSet = result.getKey();
                    bestScore = result.getValue();
                }
            } else {
                if (result.getValue() < bestScore) {
                    bestSet = result.getKey();
                    bestScore = result.getValue();
                }
            }
        }

        // Restore the thread count
        Magpie.NThreads = originalNThreads;
        
        // Return the result
        return Arrays.asList(ArrayUtils.toObject(bestSet));
    }
    
    /**
     * Read from the current iterator
     * @return Next set, if available, or <code>null</code> if not.
     */
    synchronized private int[] getNextSet() {
        if (SetIterator.hasNext()) {
            return SetIterator.next();
        } else {
            return null;
        }
    }
    
    /**
     * Create an iterator over all attribute subsets being considered. Set it
     * as the current state 
     * @param data Dataset being used to train attribute selector
     */
    protected void setCombinationIterator(final Dataset data) {
        // Initialize the iterator
        final int curSize = MinSubsetSize;
        final Iterator<int[]> curCombinations = 
                CombinatoricsUtils.combinationsIterator(data.NAttributes(), curSize);
        
        SetIterator = new Iterator<int[]>() {
            private int CurSize = curSize;
            private Iterator<int[]> CurCombinations = curCombinations;

            @Override
            public boolean hasNext() {
                return CurSize < MaxSubsetSize || CurCombinations.hasNext();
            }

            @Override
            public int[] next() {
                if (CurCombinations.hasNext()) {
                    return CurCombinations.next();
                }
                
                // Otherwise, incrememt the size counter
                CurSize++;
                CurCombinations = CombinatoricsUtils.combinationsIterator(
                        data.NAttributes(), CurSize);
                return CurCombinations.next();
            }
        };
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        return super.printDescription(htmlFormat); //To change body of generated methods, choose Tools | Templates.
    }
}
