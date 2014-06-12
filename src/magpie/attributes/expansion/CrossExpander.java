/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.attributes.expansion;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Creates new attributes as multiplicative cross terms of currently 
 * available ones. It automatically generates every possible combination, which 
 * may be behavior that could be removed in the future.
 * 
 * <usage><p><b>Usage</b>: *No options to set*</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class CrossExpander extends BaseAttributeExpander {

    @Override
    public void expand(Dataset Data) {
        int OriginalAttributeCount = Data.NAttributes();
        
        /** Create new names */
        for (int i=0; i<OriginalAttributeCount - 1; i++)
            for (int j=i+1; j<OriginalAttributeCount; j++) {
                Data.AttributeName.add(Data.AttributeName.get(i) + "*"
                        + Data.AttributeName.get(j));
            }
        
        /** Create new features */
        int NNAttributes = (OriginalAttributeCount - 1) * OriginalAttributeCount / 2;
        for (int i=0; i<Data.NEntries(); i++) {
            BaseEntry Entry = Data.getEntry(i);
            double[] newfeatures = new double[OriginalAttributeCount + NNAttributes];
            System.arraycopy(Entry.getAttributes(), 0, newfeatures, 0, OriginalAttributeCount);
            int pos = OriginalAttributeCount;
            for (int j=0; j<OriginalAttributeCount - 1; j++)
                for (int k=j+1; k<OriginalAttributeCount; k++) {
                    newfeatures[pos] = newfeatures[j] * newfeatures[k];
                    pos++;
                }
            Entry.setAttributes(newfeatures);
        }
    }

    @Override
    public void setOptions(List<Object> Options) { /* Nothing to do. */ }

    @Override
    public String printUsage() {
        return "Usage: *No options*";
    }
    
    
}
