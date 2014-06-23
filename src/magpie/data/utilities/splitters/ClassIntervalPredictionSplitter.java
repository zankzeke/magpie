/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.data.utilities.splitters;

import java.util.Arrays;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.utilities.modifiers.ClassIntervalModifier;
import magpie.models.BaseModel;
import magpie.models.classification.AbstractClassifier;
import magpie.models.classification.WekaClassifier;

/**
 * Splits data based on whether the class variable is predicted to be within certain intervals.
 *  Partitioning is based on an internal classifier, which is trained using the 
 *  {@linkplain #train(magpie.data.Dataset) } operation.
 * 
 * <usage><p><b>Usage</b>: $&lt;classifier&gt; &lt;interval edges...&gt;
 * <br><pr><i>classifier</i>: {@linkplain AbstractClassifier} used to predict class interval.
 * <br><pr><i>interval edges...</i>: Values that define the edges of intervals. Edges 
 will extend from (-Inf,Value1], (Value1,Value2], (ValueN, Inf).</usage>
 * 
 * @author Logan Ward
 */
public class ClassIntervalPredictionSplitter extends BaseDatasetSplitter {
    /** Classifier used to predict range */
    private BaseModel Clfr = new WekaClassifier("trees.REPTree", null);
    /** Edges on which to split data */
    private double[] Edges = new double[]{0};

    @Override
    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    public void setOptions(List<Object> Options) throws Exception {
        try {
            setClassifier((BaseModel) Options.get(0));
            double[] inter = new double[Options.size() - 1];
            for (int i=0; i<inter.length; i++) {
                inter[i] = Double.parseDouble(Options.get(i+1).toString());
            }
            setEdges(inter);
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<classifier> <interval edges...>";
    }
    
    /**
     * Define the classifier used to predict on which interval a class variable
     *  most likely lies.
     * @param Clfr Classifier will that will be used
     */
    public void setClassifier(BaseModel Clfr) {
        if (! (Clfr instanceof AbstractClassifier)) {
            throw new Error("Model not a classification model");
        }
        this.Clfr = Clfr.clone();
    }

    /**
     * Define edges describing different values of the class variable 
     *  on which entries are partitioned. Edges are defined by:
     *  <center>(-Inf, edge[0]], (edge[0], edge[1]], ..., (edge[N], Inf)</center>
     * @param edges Edges of class intervals
     */
    public void setEdges(double[] edges) {
        if (edges.length == 0) {
            throw new Error("At least one edge must be defined.");
        }
        this.Edges = edges.clone();
        Arrays.sort(this.Edges);
    }

    @Override
    public void train(Dataset TrainingSet) {
        // Modify dataset to have multiple classes
        Dataset Copy = TrainingSet.clone();
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        mdfr.setEdges(Edges);
        mdfr.transform(Copy);
        
        // Train classifier
        Clfr.train(Copy);
    }

    @Override
    public int[] label(Dataset D) {
        // Modify dataset to have multiple classes
        Dataset Copy = D.clone();
        ClassIntervalModifier mdfr = new ClassIntervalModifier();
        mdfr.setEdges(Edges);
        mdfr.transform(Copy);
        
        // Get predicted class variable
        Clfr.run(Copy);
        double[] variable = Copy.getPredictedClassArray();
        int[] output = new int[Copy.NEntries()];
        for (int i=0; i < output.length; i++) {
            output[i] = (int) variable[i];
        }
        return output;
    }
}
