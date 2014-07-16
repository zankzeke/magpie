/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.csp;

import java.util.List;
import magpie.csp.diagramdata.PhaseDiagramStatistics;
import magpie.data.materials.PrototypeDataset;
import magpie.data.materials.PrototypeEntry;
import magpie.data.materials.util.PrototypeSiteInformation;
import magpie.models.classification.BaseClassifier;

/**
 * Use machine learning model trained on the composition of the entries.
 * 
 * <usage><b>Usage</b>: $&lt;model template>
 * <br><pr><i>model template</i>: Template of a {@linkplain BaseClassifier} used 
 * to predict which structure is most likely</usage>
 * 
 * <p><b><u>Implemented Commands:</u></b>
 * 
 * <command><p><b>properties [&lt;command...>]</b> - Set the properties used to generate  
 * attributes based on the composition of each compound
 * <br><pr><i>command</i>: Property command from {@linkplain PrototypeDataset}</command>
 * 
 * @author Logan Ward
 */
public class CompositionBasedCSPEngine extends CSPEngine {
    /** Template dataset. Used to provide a standard method for generating attributes */
    final private PrototypeDataset datasetTemplate = new PrototypeDataset();
    /** Template model. Specified by user */
    private BaseClassifier ClfrTempate;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        try {
            setClassifierTempate((BaseClassifier) Options.get(0));
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
    }

    @Override
    public String printUsage() {
        return "Usage: $<classifier template>";
    }
    
    /**
     * Define a template for the classifier used by this class
     * @param ClfrTempate 
     */
    public void setClassifierTempate(BaseClassifier ClfrTempate) {
        this.ClfrTempate = ClfrTempate.clone();
        this.ClfrTempate.setClassDiscrete();
    }
    
    @Override
    protected BaseClassifier makeClassifier(PhaseDiagramStatistics statistics, PrototypeDataset trainData) {
        
        // Create a copy of training data using the settings of our template
        BaseClassifier newClassifier = ClfrTempate.clone();
        PrototypeDataset temp = (PrototypeDataset) datasetTemplate.emptyClone();
        PrototypeSiteInformation siteInfo = trainData.getSiteInfo();
        for (int i=0; i<siteInfo.NGroups(); i++) {
            siteInfo.setGroupIncludedInAttribute(i, false); // Ensure site IDs are not used
        }
        temp.setSiteInfo(trainData.getSiteInfo());
        temp.addEntries(trainData.getEntries());
        temp.setClassNames(trainData.getClassNames());
        
        // Generate attributes
        try {
            temp.generateAttributes();
        } catch (Exception e) {
            throw new Error(e);
        }
        
        // Train and return
        newClassifier.train(temp);
        return newClassifier;
    }

    @Override
    protected double[] getProbabilities(BaseClassifier classifier, List<String> knownPrototypes, 
            PrototypeSiteInformation siteInfo, PrototypeEntry entryToPredict) {
        
        // Make dataset.
        PrototypeDataset runData = (PrototypeDataset) datasetTemplate.clone();
        runData.setClassNames(knownPrototypes.toArray(new String[0]));
        runData.setSiteInfo(siteInfo);
        runData.addEntry(entryToPredict);
        
        // Calculate attributes and run
        try {
            runData.generateAttributes();
        } catch (Exception e) {
            throw new Error(e);
        }
        classifier.run(runData);
        double[] probs = runData.getEntry(0).getClassProbilities();
        return probs;
    }

    @Override
    public Object runCommand(List<Object> Command) throws Exception {
        if (Command.isEmpty())
            return super.runCommand(Command);
        String Action = Command.get(0).toString().toLowerCase();
        switch (Action) {
            case "properties":
                return datasetTemplate.runCommand(Command);
            default:
                return super.runCommand(Command);
        }
    }
}
