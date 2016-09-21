package magpie.attributes.selectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import magpie.data.Dataset;
import magpie.data.utilities.output.DelimitedOutput;
import magpie.utility.UtilityOperations;

/**
 * Abstract class for performing attribute selection with a Python code. This class
 * works by launching an external thread, and communicating with it via standard
 * in and out. 
 * 
 * <p>The python code must take a CSV file via standard in as inputs, options
 * as command line arguments, and return the result via standard out. The selected
 * attributes should be listed by name on a line that starts with "[Answer]". Names
 * should be separated by spaces.
 * 
 * @author Logan Ward
 */
public abstract class PythonBasedAttributeSelector extends BaseAttributeSelector {
    /** Path to script to be executed */
    protected String Script = "py/lasso_attribute_selection.py";
    /** Debug mode. Pipe output from subprocess to stdout */
    public boolean Debug = false;

    @Override
    protected List<Integer> train_protected(Dataset data) {
        // Create system call
        File lassoCodePath = UtilityOperations.findFile(Script);
        if (lassoCodePath == null) {
            throw new RuntimeException("can't find lasso_attribute_selection.py");
        }
        List<String> call = assembleSystemCall(lassoCodePath);
        // Start the subprocess
        final Process python;
        try {
            python = new ProcessBuilder(call).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Start a tread reading from the error stream
        spawnStderrReader(python);
        List<String> attrNames = new ArrayList<>();
        try {
            // Write dataset to the lasso code
            DelimitedOutput delimitedOutput = new DelimitedOutput(",");
            delimitedOutput.writeDataset(data, python.getOutputStream());
            python.getOutputStream().close(); // Done, let the subprocess go
            // Read until the answer or null is found
            BufferedReader fp = new BufferedReader(new InputStreamReader(python.getInputStream()));
            String line = fp.readLine();
            while (line != null) {
                line = fp.readLine();
                if (Debug) {
                    System.out.println(line);
                }
                if (line.startsWith("[Answer]")) {
                    break;
                }
            }
            // If the line is null, an error has occured
            if (line == null) {
                throw new Exception("Answer not found.");
            }
            // Otherwise, get the attribute names
            String[] words = line.split(" ");
            for (int i = 1; i < words.length; i++) {
                attrNames.add(words[i]);
            }
        } catch (Exception e) {
            python.destroy();
            throw new RuntimeException(e);
        }
        // Match names with id
        List<Integer> output = new ArrayList<>(attrNames.size());
        for (String name : attrNames) {
            int id = data.getAttributeIndex(name);
            if (id == -1) {
                throw new RuntimeException("Attribute not found: " + name);
            }
            output.add(id);
        }
        return output;
    }

    /**
     * Prepare the system call with all command-line arguments
     *
     * @param codePath Path to executable or script to be run
     * @return Command to be executed
     */
    protected abstract List<String> assembleSystemCall(File codePath);

    /**
     * Launch a thread that does nothing but read from the standard error of an
     * external process.
     *
     * Prevents the subprocess from stalling due to a locked buffer.
     *
     * @param proc Process to be read from.
     */
    protected void spawnStderrReader(final Process proc) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int b = proc.getErrorStream().read();
                    while (b != -1) {
                        b = proc.getErrorStream().read();
                        System.err.write(b);
                    }
                } catch (IOException e) {
                }
            }
        });
        t.start();
    }
}
