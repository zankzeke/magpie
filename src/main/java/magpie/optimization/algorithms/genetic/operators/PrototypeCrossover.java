
package magpie.optimization.algorithms.genetic.operators;

import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeEntry;

/**
 * Perform crossover for a {@linkplain PrototypeEntry}. Simply chooses 
 * @author Logan Ward
 * @verion 0.1
 */
public class PrototypeCrossover extends BaseCrossoverFunction {
    
    @Override
    public BaseEntry crossover(BaseEntry A_ptr, BaseEntry B_ptr) {
        if (! (A_ptr instanceof PrototypeEntry &&
                B_ptr instanceof PrototypeEntry))
            throw new Error("Only works with PrototypeEntry");
        PrototypeEntry A = (PrototypeEntry) A_ptr;
        PrototypeEntry B = (PrototypeEntry) B_ptr;
        PrototypeEntry C = A.clone();
        for (int i=0; i<A.NSites(); i++) {
            double rand = Math.random();
            CompositionEntry SiteComp;
            if (rand > 0.5) SiteComp = A.getSiteComposition(i);
            else SiteComp = B.getSiteComposition(i);
            C.setSiteComposition(i, SiteComp);
        }
        return C;
    }
}
