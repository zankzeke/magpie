package magpie.utility;

import magpie.Magpie;
import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * This is only a class full of useful static methods. 
 * @author Logan Ward
 */
public class UtilityOperations {

    /** Read the state from file using serialization
     * @param filename Filename for input
     * @return Object stored in that file
     * @throws java.lang.Exception
     */
    public static Object loadState(String filename) throws Exception {
        Object output;
        FileInputStream fp = new FileInputStream(filename);
        output = loadState(fp);
        fp.close();
        return output;
    }

    /**
     * Read in a Magpie object from a stream
     *
     * @param input Input stream
     * @return Object stored in the stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object loadState(InputStream input) throws IOException, ClassNotFoundException {
        Object output;
        ObjectInputStream in = new ObjectInputStream(input);
        output = in.readObject();
        in.close();
        return output;
    }

    /**
     * Use serialization to save object state to file
     * @param obj Object to be serialized and saved
     * @param filename Desired filename
     */
    public static void saveState(Object obj, String filename) throws IOException {
        FileOutputStream fp = new FileOutputStream(filename);
        saveState(obj, fp);
        fp.close();
    }

    /**
     * Use serialization to save object state to an output stream
     *
     * @param obj    Output
     * @param output Output stream
     * @throws IOException
     */
    public static void saveState(Object obj, OutputStream output) throws IOException {
        ObjectOutputStream out = new ObjectOutputStream(output);
        out.writeObject(obj);
        out.close();
    }

    /**
     * Print elapsed time to screen
     * @param start_time When operation started (as reported by <code>System.currentTimeMillis()</code>)
     */
    public static void printRunTime(long start_time) {
        System.out.printf("Elapsed time: %.3f s\n", (double) (System.currentTimeMillis() - start_time) / 1000.0);
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

    /**
     * Given the path of a path relative to the Magpie jar, find the actual path.
     * @param toFind Relative path to file or directory (e.g. "py/lasso.py")
     * @return Path to file. null if not found
     */
    public static File findFile(String toFind) {
        // Get the path to Magpie
        File magpiePath = null;
        try {
            magpiePath = new File(Magpie.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            // Nothing
        }

        // Get a list of paths over which to search
        List<File> toSearch = new ArrayList<>();
        toSearch.add(new File(".")); // Current directory
        toSearch.add(new File("..")); // One directory down
        toSearch.add(new File("../..")); // Two directories down
        if (magpiePath != null) {
            toSearch.add(magpiePath); // Magpie path
            toSearch.add(magpiePath.getParentFile()); // One directory down from Magpie
            toSearch.add(magpiePath.getParentFile().getParentFile()); // Two directories down from Magpie
        }

        // Look in these directories
        for (File path : toSearch) {
            File guess = new File(path, toFind);
            if (guess.exists()) {
                return guess;
            }
        }
        return null;
    }
    
    /**
     * Split a list into smaller sublists that are as equally-sized as possible.
     * @param toSplit List to be split
     * @param nPartitions Number of partitions to create
     * @return List of sublists
     */
    static public <T> List<List<T>> partitionList(List<T> toSplit,
            int nPartitions) {
        // Prepare the output
        List<List<T>> output = new ArrayList<>(nPartitions);
        for (int n=0; n<nPartitions; n++) {
            output.add(new ArrayList(toSplit.size() / nPartitions + 1));
        }

        // Iterate over all entries in the list to split
        int pos = 0;
        Iterator<T> iter = toSplit.iterator();
        while (iter.hasNext()) {
            output.get(pos++ % nPartitions).add(iter.next());
        }
        return output;
    }

    /**
     * Convert an array of doubles into a JSON array.
     *
     * <p>Changes in NaN or inf values into strings (JSON doesn't support these as numbers</p>
     * @param array Array to be converted
     * @return Array as JSON
     */
    public static JSONArray toJSONArray(double[] array) {
        JSONArray output = new JSONArray();

        for (double x : array) {
            if (Double.isNaN(x)) {
                output.put("NaN");
            } else if (Double.isInfinite(x)) {
                output.put(x < 0 ? "-inf" : "inf");
            } else {
                output.put(x);
            }
        }

        return output;
    }

    /**
     * Print a time, stored in milliseconds, into a human-readable string.
     *
     * @param time Time in milliseconds
     * @return Time as a human readable string
     */
    public static String millisecondsToString(long time) {
        return String.format("%d days, %02d hours, %02d minutes, %02d.%03d seconds",
                TimeUnit.MILLISECONDS.toDays(time),
                TimeUnit.MILLISECONDS.toHours(time) - 24 * TimeUnit.MILLISECONDS.toDays(time),
                TimeUnit.MILLISECONDS.toMinutes(time) - 60 * TimeUnit.MILLISECONDS.toHours(time),
                TimeUnit.MILLISECONDS.toSeconds(time) - 60 * TimeUnit.MILLISECONDS.toMinutes(time),
                time - 1000 * TimeUnit.MILLISECONDS.toSeconds(time)
        );
    }

    /**
     * Sort an array and return the original indices of the each value.
     * @param x Array to be sorted
     * @param descending Whether to sort the list in descending order
     * @return List of indices, sorted in same order as x
     */
    public static int[] sortAndGetRanks(double[] x, boolean descending) {
        // Put each entry in a sorted set
        final boolean desFinal = descending;
        Comparator<Double> comp = new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return desFinal ? o2.compareTo(o1) : o1.compareTo(o2);
            }
        };

        return sortAndGetRanks(x, comp);
    }

    /**
     * Sort an array and return the original index of each member. After
     * operation, x will be sorted.
     * @param x Array to be sorted
     * @param comp How to compare entries in the array
     * @return Original index of each point
     */
    public static int[] sortAndGetRanks(double[] x, Comparator<Double> comp) {
        // Initialize the output array
        Integer[] output = new Integer[x.length];
        for (int i=0; i<x.length; i++) {
            output[i] = i;
        }

        // Create a copy of x that won't be sorted
        final double[] xOriginal = x.clone();

        // Make a comparator for the indicies
        final Comparator<Double> compFinal = comp;
        Comparator<Integer> indexComp = new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return compFinal.compare(xOriginal[o1], xOriginal[o2]);
            }
        };
        Arrays.sort(output, indexComp);

        // Sort x and return list
        for (int i=0; i<output.length; i++) {
            x[i] = xOriginal[output[i]];
        }
        return ArrayUtils.toPrimitive(output);
    }
}
