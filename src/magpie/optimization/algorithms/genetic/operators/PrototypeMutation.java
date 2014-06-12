/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package magpie.optimization.algorithms.genetic.operators;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import magpie.data.BaseEntry;
import magpie.data.materials.CompositionEntry;
import magpie.data.materials.PrototypeEntry;

/**
 *
 * @author Logan Ward
 * @version 0.1
 */
public class PrototypeMutation extends BaseMutationFunction {
    /** Mutation function for each site */
    final private List<BaseMutationFunction> SiteMutationFunction = new LinkedList<>();

    @Override
    public void configureFunction(Set<BaseEntry> searchSpace) {
        if (! (searchSpace.iterator().next() instanceof PrototypeEntry))
            throw new Error("Entries must implemented PrototypeEntry");
        Set<BaseEntry> sites = new HashSet<>(searchSpace.size());
        PrototypeEntry ptr = (PrototypeEntry) searchSpace.iterator().next();
        SiteMutationFunction.clear();
        // For each site, extract the composition for each entry
        for (int s=0; s<ptr.NSites(); s++) {
            sites.clear();
            for (BaseEntry E_ptr : searchSpace) {
                PrototypeEntry E = (PrototypeEntry) E_ptr;
                sites.add(E.getSiteComposition(s));
            }
            // Use it to configure a Compound Mutator
            SimpleCompoundMutation siteMutator = new SimpleCompoundMutation();
            siteMutator.configureFunction(sites);
            SiteMutationFunction.add(siteMutator);
        }
    }

    @Override
    public void mutate(BaseEntry Entry) {
        PrototypeEntry Ptr = (PrototypeEntry) Entry;
        // Mutate each site
        for (int s=0; s<Ptr.NSites(); s++) {
            CompositionEntry comp = Ptr.getSiteComposition(s);
            SiteMutationFunction.get(s).mutate(comp);
            Ptr.setSiteComposition(s, comp);
        }
    }
}
