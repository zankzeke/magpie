
package magpie.optimization.rankers;

import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;

/**
 * Base class for multi-objective entry rankers. Users need to specify the following operations:
 * 
 * <ul>
 * <li>{@linkplain #train(magpie.data.MultiPropertyDataset) ) - Run necessary calculations
 * <li>
 * </ul>
 * 
 * @author Logan Ward
 */
abstract public class MultiObjectiveEntryRanker extends BaseEntryRanker {

    @Override
    @SuppressWarnings("CloneDeclaresCloneNotSupported")
    public MultiObjectiveEntryRanker clone()  {
        return (MultiObjectiveEntryRanker) super.clone(); 
    }
    
    /**
     * Train the entry ranker. This may be necessary for multi-objective methods
     *  that rely on the performance of other entries (e.g. Pareto fronts).
     * 
     * <p>Note: This might be an opportune operation to map the name
     * of each property with their index to allow for faster operation of 
     * the {@linkplain EntryRanker#objectiveFunction(magpie.data.BaseEntry)}
     * @param data Dataset to use for training
     */
    abstract public void train(MultiPropertyDataset data);
    
    /**
     * Get properties currently used in objective functions.
     *
     * @return Array of properties currently in use
     */
    abstract public String[] getObjectives();

	@Override
	public void train(Dataset data) {
		if (! (data instanceof MultiPropertyDataset)) {
			throw new Error("Data must be a MultiPropertyDataset");
		}
		MultiPropertyDataset ptr = (MultiPropertyDataset) data;
		train(ptr);
	}
}
