
package magpie.csp;

import java.util.*;
import magpie.utility.interfaces.Printable;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Holds data about the performance of a CSP algorithm.
 * 
 * @author Logan Ward
 */
public class CSPPerformanceStats implements Printable {
    /**
     * Position of correct prototype in the list of predictions.
     */
    private final List<Integer> listPosition = new LinkedList<>(); 
    
    /**
     * Fraction of structures before correct prototype.
     */
    private final List<Double> listFraction = new LinkedList<>();
    
    /**
     * Number of crystal structure predictions that have been evaluated.
     * @return Number results
     */
    public int NResults() {
        return listPosition.size();
    }
    
    /**
     * Add results of a prediction to performance dataset.
     * @param correctAnswer Correct prototype
     * @param predictions Predicted prototypes ordered such that the most probable answer is first
     */
    public void addResult(String correctAnswer, List<Pair<String,Double>> predictions) {
        // Get the position of correct answer in list
        boolean wasFound = false;
        int position;
        for (position = 0; position < predictions.size(); position++) {
            if (predictions.get(position).getKey().equals(correctAnswer)) {
                wasFound = true;
				break;
            }
        }
        
        // Only add results where the correct answer was somewhere in the list
        if (wasFound) {
            listPosition.add(position);
            listFraction.add(((double) position -1) / (double) predictions.size());
        }
    }
    
    /**
     * Removes results from previous tests.
     */
    public void clear() {
        listPosition.clear(); listFraction.clear();
    }
    
    /**
     * Compute the minimum length of a list required to find the correct structure
     * @param minSuccess Minimum probability to predict the correct structure (between 0 and 1)
     * @param nPoints Number of at which to evaluate this value in the range
     * [minSuccess, 1). Points will be equally spaced.
     * @return 
     */
    public double[][] computeListLengthCurve(double minSuccess, int nPoints) {
        // --> Step 1: Sort the listPosition array
        List<Integer> positionCopy = new ArrayList<>(listPosition);
        Collections.sort(positionCopy);
        
        // --> Step 2: Determine step size
        nPoints = Math.min(nPoints, listPosition.size() - 1);
        int stepSize = (int) Math.ceil((1.0 - minSuccess) * 
                (double) (listPosition.size() - nPoints) / (double) (nPoints + 1));
        int startSize = (int) Math.floor(minSuccess * listFraction.size());
        
        // --> Step 3: Compute minimum list size such that x% of the data is underneath 
        //   a certain fraction
        List<double[]> output = new LinkedList<>();
        for (int size = startSize; size < listPosition.size(); size += stepSize) {
            int listLength = positionCopy.get(size);
            output.add(new double[]{(double) size / listPosition.size(),
                listLength});
        }
        return output.toArray(new double[0][]);
    }

    @Override
    public String about() {
        return "Number results: " + listPosition.size();
    }
    
    @Override
    public String printCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            return about();
        }
        String Action = Command.get(0).toLowerCase();
        switch(Action) {
            case "list-length": {
                String output = String.format("%24s\t%10s\n", "Prediction Accuracy", "List Length");
                double[][] listLength = computeListLengthCurve(0.7, 20);
                for (double[] value : listLength) {
                    output += String.format("%24.3f\t%10.0f\n", value[0], value[1]);
                }
                return output;
            }
            default:
                throw new Exception("CSPPerformance print command not defined:" + Action);
        }
    }
}
