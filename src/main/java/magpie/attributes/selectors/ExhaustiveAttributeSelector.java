package magpie.attributes.selectors;

import java.io.Serializable;
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
import magpie.statistics.performance.BaseStatistics;
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
 * classification models.</p>
 * 
 * <usage><p><b>Usage</b>: $&lt;model&gt; [-min_size &lt;min&gt;]
 * [-max_size &lt;max&gt;] [-k_fold &lt;k&gt;] | [-random_cv &lt;test_frac&gt;
 * &lt;n_repeat&gt;] | [-train]
 * <pr><br><i>model</i>: {@linkplain BaseModel} used to test attribute sets
 * <pr><br><i>min</i>: Minimum attribute set size (default=1)
 * <pr><br><i>max</i>: Maximum attribute set size (default=4)
 * <pr><br><i>k</i>: Use k-fold CV to evaluate model: Number of folds to use during cross-validation
 * <pr><br><i>test_frac</i>: Use random split CV for validation: Size of split for test set
 * <pr><br><i>n_repeat</i>: Use  random split CV for validation: Number of times to repeat test
 * By default, class uses 10-fold CV. Can specify only one option of "-k_fold"
 * (for k-fold CV), -random_cv (for multiple test/train set splits), and "-train"
 * (for using training set score).</p></usage>
 * 
 * @author Logan Ward
 */
public class ExhaustiveAttributeSelector extends BaseAttributeSelector {
    /** List of methods used to evaluate model performance */
    public enum EvaluationMethod {
        /** Use the performance on the training set */
        TRAINING,
        /** Use k-fold cross-validation */
        KFOLD_CROSSVALIDATION,
        /** Use a number of random train/test splits */
        RANDOMSPLIT_CROSSVALIDATION
    }
    
    /** Model used for cross-validation */
    protected BaseModel Model = new RandomGuessRegression();
    /** Minimum attribute subset size */
    protected int MinSubsetSize = 1;
    /** Maximum attribute subset size */
    protected int MaxSubsetSize = 4;
    /** If test method is K-Fold: Number of folds used in cross-validation test */
    protected int KFolds = 10;
    /** Method used to evaluate models */
    protected EvaluationMethod TestMethod = EvaluationMethod.KFOLD_CROSSVALIDATION;
    /** If method is random split, fraction of entries withheld for test set */
    protected double RandomTestFraction = 0.25;
    /** If method is random split, number of times to repeat test */
    protected int RandomTestCount = 100;
    
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
        int min_size = 1;
        int max_size = 4;
        BaseModel model;
        EvaluationMethod testMethod = EvaluationMethod.KFOLD_CROSSVALIDATION;
        int n_folds = 10;
        double test_split = 0.25;
        
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
                    case "-k_fold":
                        pos++;
                        testMethod = EvaluationMethod.KFOLD_CROSSVALIDATION;
                        n_folds = Integer.parseInt(Options.get(pos).toString());
                        break;
                    case "-random_cv":
                        testMethod = EvaluationMethod.RANDOMSPLIT_CROSSVALIDATION;
                        test_split = Double.parseDouble(Options.get(++pos).toString());
                        n_folds = Integer.parseInt(Options.get(++pos).toString());
                        break;
                    case "-train":
                        testMethod = EvaluationMethod.TRAINING;
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
        
        // Set the evaluation method
        setTestMethod(testMethod);
        switch (testMethod) {
            case KFOLD_CROSSVALIDATION:
                setNFolds(n_folds); 
                break;
            case RANDOMSPLIT_CROSSVALIDATION:
                setRandomSplit(test_split, n_folds);
                break;
            case TRAINING:
                break;
            default:
                throw new IllegalArgumentException("Eval. method not recognized: " +
                        testMethod);
        }
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
     * Set method used to evaluate model performance
     * @param method Desired method
     */
    public void setTestMethod(EvaluationMethod method) {
        this.TestMethod = method;
    }
    
    /**
     * Set the number of folds used in K-fold cross-validation. Will set K-fold CV
     * to be the evaluation method
     * @param k Number of folds
     */
    public void setNFolds(int k) {
        KFolds = k;
        setTestMethod(EvaluationMethod.KFOLD_CROSSVALIDATION);
    }
    
    /**
     * Set the size of test set split and number of times to repeat CV. Will
     * set Random Split CV to be the evaluation method
     * @param test_split
     * @param n_repeats 
     */
    public void setRandomSplit(double test_split, int n_repeats) {
        RandomTestCount = n_repeats;
        RandomTestFraction = test_split;
        setTestMethod(EvaluationMethod.RANDOMSPLIT_CROSSVALIDATION);
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
                        switch (TestMethod) {
                            case KFOLD_CROSSVALIDATION:
                                myModel.crossValidate(KFolds, curData, randomSeed);
                                break;
                            case RANDOMSPLIT_CROSSVALIDATION:
                                myModel.crossValidate(RandomTestFraction, KFolds, curData, randomSeed);
                                break;
                            case TRAINING:
                                myModel.train(curData);
                                break;
                            default:
                                throw new RuntimeException("Eval method not defined: " 
                                        + TestMethod);
                        }
                        
                        // Get which stats to use 
                        BaseStatistics stats = TestMethod == EvaluationMethod.TRAINING ? 
                                myModel.TrainingStats : myModel.ValidationStats;
                        
                        // Check if results is better
                        double curScore = isClassifier ?
                                ((ClassificationStatistics) stats).Accuracy 
                                : ((RegressionStatistics) stats).RMSE;
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
        
        // Clear the iterator
        SetIterator = null;
        
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
        // Throw an meaningful error
        if (data.NAttributes() < MaxSubsetSize) {
            throw new RuntimeException("Input dataset has fewer attributes than maximum subset size");
        }
        if (data.NAttributes() < MinSubsetSize) {
            throw new RuntimeException("Input dataset has fewer attributes than minimum subset size");
        }
        
        // Initialize the iterator
        final int curSize = MinSubsetSize;
        final int maxSize = MaxSubsetSize;
        final Iterator<int[]> curCombinations = 
                CombinatoricsUtils.combinationsIterator(data.NAttributes(), curSize);
        
        SetIterator = new Iterator<int[]>() {
            private int CurSize = curSize;
            private Iterator<int[]> CurCombinations = curCombinations;

            @Override
            public boolean hasNext() {
                return CurSize < maxSize || CurCombinations.hasNext();
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

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
    }

    @Override
    public String printDescription(boolean htmlFormat) {
        String output = String.format("Find the subset of attributes with between "
                + "%d and %d members that leads to the model with the best performance ", 
                MinSubsetSize, MaxSubsetSize);
        
        // Define the test method
        switch (TestMethod) {
            case TRAINING:
                output += "on the training set."; 
                break;
            case KFOLD_CROSSVALIDATION:
                output += String.format(
                        "in %d-fold cross-validation.",
                        KFolds);
                break;
            case RANDOMSPLIT_CROSSVALIDATION:
                output += String.format(
                        "over %d iterations of %.1f/%.1f%% test/train split cross-valdation.",
                        RandomTestCount,
                        100 * RandomTestFraction,
                        100 * (1 - RandomTestFraction)
                );
                break;
            default:
                throw new RuntimeException("Evaluation method not implemented: " +
                        TestMethod);
        }
        
        return output;
    }
}
