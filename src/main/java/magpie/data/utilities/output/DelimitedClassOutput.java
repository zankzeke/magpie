package magpie.data.utilities.output;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import magpie.data.BaseEntry;
import magpie.data.Dataset;

/**
 * Output only the measured and predicted class values to a delimited file.
 * Designed to be used when measuring algorithm performance with an external program.
 * 
 * <usage><p><b>Usage</b>: &lt;delimiter&gt;
 * <br><pr><i>delimiter</i>: Type of delimeter. Put whitespace inside of quotation
 * marks. This command accepts escaped characters</usage>
 * 
 * @author Logan Ward
 */
public class DelimitedClassOutput extends DelimitedOutput {
    /** Whether the class has multiple, discrete values */
    protected boolean ClassIsDiscrete;
    /** Number of class values */
    protected int NClasses = -1;

    public DelimitedClassOutput() {
    }
    
    /**
     * Create a file writer with a certain delimiter
     * @param delimiter Desired delimiter
     */
    public DelimitedClassOutput(String delimiter) {
        super(delimiter);
    }

    @Override
    public void printHeader(Dataset data, OutputStream output) {
        PrintWriter fp = new PrintWriter(output, true);
        
        // Print out header
        fp.print("Entry" + Delimiter + "Measured" + Delimiter + "Predicted");
        
        // If dataset has multiple classes, print out room for their predicted probs
        if (data.NClasses() > 1) {
            ClassIsDiscrete = true;
            NClasses = data.NClasses();
            for (String name : data.getClassNames()) {
                fp.print(Delimiter + String.format("P(%s)", replaceDelimiter(name)));
            }
        } else {
            ClassIsDiscrete = false;
            NClasses = 1;
        }
        
        fp.println();
    }

    @Override
    public void printEntries(Collection<BaseEntry> entries, OutputStream output) {
        // Get a buffered version of the output
        PrintWriter fp = new PrintWriter(output, true);
        
        // Check whether printHeader has been run
        if (NClasses == -1) {
            throw new RuntimeException("printHeader must be run first");
        }
        
        // Print each entry
        for (BaseEntry entry : entries) {
            // Print the entry
            fp.print(replaceDelimiter(entry.toString()));
            
            // Print the measured class, if available
            fp.print(Delimiter);
            if (entry.hasMeasurement()) {
                fp.print(entry.getMeasuredClass());
            } else {
                fp.print("None");
            }
            
            // Print the predicted class, if available
            if (entry.hasPrediction()) {
                fp.print(Delimiter);
                fp.print(entry.getPredictedClass());
            } else {
                fp.print(Delimiter + "None");
            }
            
            // If multiple classes, print predicted probabilities
            if (ClassIsDiscrete) {
                if (entry.hasClassProbabilities()) {
                    double[] probs = entry.getClassProbilities();
                    for (double x : probs) {
                        fp.print(Delimiter);
                        fp.print(x);
                    }
                } else {
                    for (int i=0; i<NClasses; i++) {
                        fp.print(Delimiter + "None");
                    }
                }
            } 
            
            // End line
            fp.println();
        }
    }

    @Override
    public void printEnd(OutputStream output) {
        super.printEnd(output); 
        NClasses = -1;
    }
    
    
}
