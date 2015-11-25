package magpie.attributes.expanders;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * This class creates new attributes that are functions of single, already-existing attributes by 
 * simply raising them to a specified power
 * 
 * <usage><p><b>Usage</b>: &lt;exponents...>
 * <br><pr><i>exponents...</i>: Create new attributes based on old ones raised to these powers</usage>
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class PowerExpander extends BaseAttributeExpander {
    /** List of exponents to use when creating new attributes */
    List<Double> Exponent = new LinkedList<>();

    @Override
    public void expand(Dataset Data) {
        int OriginalAttributeCount = Data.NAttributes();
                
        /* Check if each exponent works for each feature */
        // Look for conditions where an exponent will give a bad value
        double[][] attributes = Data.getAttributeArray();
        boolean[][] is_ok = new boolean[OriginalAttributeCount][Exponent.size()];
        
        for (int e=0; e<Exponent.size(); e++) {
            // #1: Negative values and non-integar exponents
            for (int f=0; f<OriginalAttributeCount; f++) {
                is_ok[f][e] = true;
                if (Exponent.get(e) - Math.floor(Exponent.get(e)) < Double.MIN_NORMAL)
                    continue;
                for (int i=0; i<Data.NEntries(); i++)
                    if (attributes[i][f] < 0) { is_ok[f][e] = false; break; }
            }
            // #2: Zero and negative exponent
            for (int f=0; f<OriginalAttributeCount; f++) {
                is_ok[f][e] = true;
                if (Exponent.get(e) >= 0)
                    continue;
                for (int i=0; i<Data.NEntries(); i++)
                    if (attributes[i][f] == 0) { is_ok[f][e] = false; break; }
            }
        }
                   
        /** Generate names of new attributes */
        int NNAttributes = 0;
        List<String> attributeNames = new LinkedList<>(Arrays.asList(Data.getAttributeNames()));
        for (int i=0; i<OriginalAttributeCount; i++) {
            for (int j=0; j<Exponent.size(); j++)
                if (is_ok[i][j]) {
                    attributeNames.add(attributeNames.get(i) + "^" + j);
                    NNAttributes++;
                }
        }
        Data.setAttributeNames(attributeNames);
        
        /** Generate new attributes */
        for (int i=0; i<Data.NEntries(); i++) {
            BaseEntry Entry = Data.getEntry(i);
            double[] newAttributes = new double[OriginalAttributeCount + NNAttributes];
            System.arraycopy(Entry.getAttributes(), 0, newAttributes, 0, OriginalAttributeCount);
            int pos = OriginalAttributeCount;
            for (int j=0; j<OriginalAttributeCount; j++)
                for (int k=0; k<Exponent.size(); k++) {
                    if (is_ok[j][k]) {
                        newAttributes[pos] = Math.pow(newAttributes[j], Exponent.get(k));
                        pos++;
                    }
                }
            Entry.setAttributes(newAttributes);
        }
    }

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        Exponent.clear();
        try {
            for (Object Option : Options) {
                Exponent.add(Double.valueOf(Option.toString()));
            }
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
    }

    @Override
    public String printUsage() {
        return "Usage: <exponents...>";
    }
}
