/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.optimization.rankers;

import java.util.List;
import magpie.data.BaseEntry;
import magpie.data.MultiPropertyEntry;

/**
 * Use a formula of several properties. Requires that entries are instances of
 *  {@linkplain MultiPropertyEntry}.
 * @author Logan Ward
 */
public class PropertyFormulaRanker extends EntryRanker {

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String printUsage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public double objectiveFunction(BaseEntry Entry) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
