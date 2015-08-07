package magpie.data.utilities.filters;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CompositionEntry;
import magpie.optimization.rankers.BaseEntryRanker;
import magpie.optimization.rankers.SimpleEntryRanker;
import magpie.user.CommandHandler;

/**
 * Select only the best N entries from any one phase diagram. "Phase Diagram" is defined
 * as the set of constituent elements for a certain composition (e.g., AlNi -> {Al,Ni{).
 * Entries are only in the same diagram if they have the same set of constituent elements
 * (ex: AlTiNi and AlNi are in different diagrams).
 * 
 * <usage><b>Usage</b>: &lt;N&gt; &lt;maximum|minimum&gt; &lt;measured|predicted&gt;
 *      &lt;ranker method&gt; [&lt;ranker options...&gt;]
 * <br><pr><i>N</i>: Number of entries to pick per diagram
 * <br><pr><i>maximum|minimum</i>: Whether to select the entries with the 
 * highest or lowest ranking score
 * <br><pr><i>measured|predicted</i>: Whether to use measured or predicted values
 * <br><pr><i>ranker method</i>: Name of a {@linkplain BaseEntryRanker} used
 * to score entries
 * <br><pr><i>ranker options</i>: Any options for that ranker</usage>
 * @author Logan Ward
 */
public class BestInAlloySystemFilter extends BaseDatasetFilter {
    /** Ranker used to score entries */
    private BaseEntryRanker Ranker = new SimpleEntryRanker();
    /** Number of top entries to select */
    private int NPerDiagram = 1;

    @Override
    public void setOptions(List<Object> Options) throws Exception {
        String rankMethod;
        List<Object> rankOptions;
        int N;
        boolean max, measured; 
        
        try {
            N = Integer.parseInt(Options.get(0).toString());
            
            String temp = Options.get(1).toString().toLowerCase();
            if (temp.startsWith("max")) {
                max = true;
            } else if (temp.startsWith("min")) {
                max = false;
            } else {
                throw new Exception();
            }
            
            temp = Options.get(2).toString().toLowerCase();
            if (temp.startsWith("meas")) {
                measured = true;
            } else if (temp.startsWith("pred")) {
                measured = false;
            } else {
                throw new Exception();
            }
            
            rankMethod = Options.get(3).toString();
            rankOptions = Options.subList(4, Options.size());
        } catch (Exception e) {
            throw new Exception(printUsage());
        }
        
        BaseEntryRanker ranker = (BaseEntryRanker) CommandHandler.instantiateClass(
                "optimization.rankers." + rankMethod, rankOptions);
        ranker.setMaximizeFunction(max);
        ranker.setUseMeasured(measured);
        setRanker(ranker);
        setNPerDiagram(N);
    }

    @Override
    public String printUsage() {
        return "Usage: <num per diagram> <maximum|minimum> <measured|predicted>"
                + " <ranker method> [<ranker options>]";
    }

    /**
     * Set number of entries to collect per diagram
     * @param NPerDiagram Desired number of entries
     */
    public void setNPerDiagram(int NPerDiagram) throws Exception {
        if (NPerDiagram <= 0) {
            throw new Exception("Number / diagram must be > 0");
        }
        this.NPerDiagram = NPerDiagram;
    }

    /**
     * Set how to rank entries
     * @param Ranker Ranker to use to score entries
     */
    public void setRanker(BaseEntryRanker Ranker) {
        this.Ranker = Ranker;
    }
    
    @Override
    public void train(Dataset TrainingSet) {
        Ranker.train(TrainingSet);
    }

    @Override
    protected int parallelMinimum() {
        return Integer.MAX_VALUE; // Must consider the whole set
    }

    @Override
    protected boolean[] label(Dataset D) {
        // Check to make sure this is a CompositionDataset
        if (! (D instanceof CompositionDataset)) {
            throw new Error("Data is not a CompositionDataset");
        }
        
        // Initialize output array
        Map<int[], PriorityQueue<Integer>> best = new TreeMap<>(new Comparator<int[]>() {

            @Override
            public int compare(int[] o1, int[] o2) {
                // Compare the lengths
                int comp = Integer.compare(o1.length, o2.length);
                if (comp != 0) return comp;
                
                // Compare each entry
                for (int i=0; i<o1.length; i++) {
                    comp = Integer.compare(o1[i], o2[i]);
                    if (comp != 0) return comp;
                }
                return 0;
            }
        });
        
        // Compute the ranking score of each entry
        final double[] score = Ranker.runObjectiveFunction(D);
        
        // Create tool to sort entries by score
        final boolean maximize = Ranker.isMaximizing();
        Comparator<Integer> scoreRanker = new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return maximize ? Double.compare(score[o1], score[o2]) :
                        Double.compare(score[o2], score[o1]);
            }
        };
        
        // Find the best entries in each diagram
        for (int i=0; i<D.NEntries(); i++) {
            CompositionEntry entry = (CompositionEntry) D.getEntry(i);
            
            // Get the elements
            int[] elems = entry.getElements();
            
            // Check if this phase diagram is yet in the output
            PriorityQueue<Integer> queue = best.get(elems);
            
            // If not, create a new queue
            if (queue == null) {
                queue = new PriorityQueue<>(NPerDiagram + 1, scoreRanker);
                queue.add(i);
                best.put(elems, queue);
            } else {
                // If so, add it in
                queue.add(i);
                
                // If the list is too large, remove the worst
                if (queue.size() > NPerDiagram) {
                    queue.poll();
                }
            }
        }
        
        // Assemble output
        boolean[] output = new boolean[D.NEntries()];
        Arrays.fill(output, false);
        for (PriorityQueue<Integer> diagram : best.values()) {
            for (Integer entry : diagram) {
                output[entry] = true;
            }
        }
        
        return output;
    }
    
}
