/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Rank entries based on the probability that they are a certain class.
 * 
 * <usage><p><b>Usage</b>: &lt;target class&gt;
 * <br><pr><i>target class</i>: Name of class being assessed.</usage>
 * 
 * @author Logan Ward
 */
public class ClassProbabilityRanker extends EntryRanker {
    /** Name of target class */
    private String TargetClass;
    /** Index of target class */
    private int TargetClassInd = -1;

    /**
     * Create new instance of ranker.
     * @param TargetClass Name of target class
     */
    public ClassProbabilityRanker(String TargetClass) {
        this.TargetClass = TargetClass;
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        if (Options.size() != 1) {
            throw new Exception(printUsage());
        }
        TargetClass = Options.get(0).toString();
    }

    @Override
    public String printUsage() {
        return "Usage: <target class>";
    }
    
    /**
     * Set the name of the class of interest
     * @param TargetClass Name of target class
     */
    public void setTargetClass(String TargetClass) {
        this.TargetClass = TargetClass;
    }
    
    @Override
    public double objectiveFunction(BaseEntry Entry) {
        if (isUsingMeasured()) {
            return Entry.getMeasuredClass() == TargetClassInd ? 1 : 0;
        } else {
            return Entry.getClassProbilities()[TargetClassInd];
        }
    }

    @Override
    public void train(Dataset data) {
        TargetClassInd = ArrayUtils.indexOf(data.getClassNames(), TargetClass);
        if (TargetClassInd == ArrayUtils.INDEX_NOT_FOUND) {
            throw new Error("Dataset does not contain class: " + TargetClass);
        }
    }
}
