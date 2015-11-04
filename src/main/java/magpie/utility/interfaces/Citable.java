package magpie.utility.interfaces;

import java.util.List;
import magpie.attributes.generators.BaseAttributeGenerator;
import magpie.attributes.generators.composition.ValenceShellAttributeGenerator;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Interface for objects that should be referenced if they are used.
 * 
 * <p>Users need two pieces of information before citing a resource:
 * <ol>
 * <li>Why this resources is being cited
 * <li>Where to find the resource
 * </ol>
 * For that reason, each citation is provided along with a short description of
 * what that resource contains, which should justify why it should be cited.
 * 
 * <p>Some objects may require several distinct citations or composed of several
 * other objects (ex: a {@linkplain Dataset} may have several {@linkplain BaseAttributeGenerator}).
 * For that reason, the {@linkplain #getCitations() } commands returns a list
 * of citations. Those from a subcomponent should be listed last, and have 
 * the name of that class listed before the reason. 
 * 
 * <p>For example, a {@linkplain CompositionDataset} that uses {@linkplain ValenceShellAttributeGenerator}
 * should cite the paper by <a href="http://journals.aps.org/prb/abstract/10.1103/PhysRevB.89.094104">
 * Meredig <i>et al.</i></a> where these attributes were first used. Therefore,
 * it should include the citation:
 * 
 * <p><u>Reason</u>: Introduced using the fraction of electrons in each valence
 * shell of the constituent elements as attributes.
 * <br><u>Citation</u>: {@link ValenceShellAttributeGenerator}. B. Meredig, A. Agrawal, et al. "Combinatorial screening 
 * for new materials in unconstrained composition space with machine learning".
 * http://link.aps.org/doi/10.1103/PhysRevB.89.094104.
 * 
 * @author Logan Ward
 */
public interface Citable {
    
    /**
     * Return a list of citations for this object and any underlying objects.
     * @return List of pairs: [Reason for Citation], [Citation]
     */
    public List<Pair<String,Citation>> getCitations();
    
}
