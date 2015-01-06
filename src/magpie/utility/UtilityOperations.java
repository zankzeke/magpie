package magpie.utility;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This is only a class full of useful static methods. 
 * @author Logan Ward
 */
public class UtilityOperations {

    /** Read the state from file using serialization
     * @param filename Filename for input
     * @return
     */
    public static Object loadState(String filename) {
        Object output;
        try {
            FileInputStream fp = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fp);
            output = in.readObject();
            in.close();
            fp.close();
        } catch (Exception i) {
            throw new Error(i);
        }
        return output;
    }
    
    /**
     * Use serialization to save object state to file
     * @param obj Object to be serialized and saved
     * @param filename Desired filename
     */
    public static void saveState(Object obj, String filename) {
        try {
            FileOutputStream fp = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fp);
            out.writeObject(obj);
            out.close();
            fp.close();
        } catch (IOException i) {
            throw new Error(i);
        }
    }

    /**
     * Print elapsed time to screen
     * @param start_time When operation started (as reported by <code>System.currentTimeMillis()</code>)
     */
    public static void printRunTime(long start_time) {
        System.out.printf("Elapsed time: %.3f s\n", (double) (System.currentTimeMillis() - start_time) / 1000.0);
    }
    
    /**
     * Randomly select number from a list of a certain length
     * @param Length Numbers will range from [0,Length)
     * @param ToSelect Number to select
     * @return Array of length ToSelect that is randomly populated (no duplicates)
     */
    public int[] getRandomList(int Length, int ToSelect) {
        // Shuffle an ascending list
        ArrayList<Integer> total = new ArrayList<>(Length);
        for (int i=0; i<Length; i++) total.add(i);
        Collections.shuffle(total);
        
        // Retrive a shortened segment
        int[] output = new int[ToSelect];
        for (int i=0; i<ToSelect; i++)
            output[i] = total.get(i);
        return output;
    }

 
    /**
     * Determine whether a string represents an integer. Thanks to 
     *  <a href="http://stackoverflow.com/questions/237159/whats-the-best-way-to-check-to-see-if-a-string-represents-an-integer-in-java">
     *  StackOverflow</a>
     * @param str String containing a single word
     * @return Whether string represents an integer
     */
    public static boolean isInteger(String str) {
	if (str == null) {
		return false;
	}
	int length = str.length();
	if (length == 0) {
		return false;
	}
	int i = 0;
	if (str.charAt(0) == '-') {
		if (length == 1) {
			return false;
		}
		i = 1;
	}
	for (; i < length; i++) {
		char c = str.charAt(i);
		if (c <= '/' || c >= ':') {
			return false;
		}
	}
	return true;
}
}
