package magpie;

import java.util.Arrays;
import java.util.List;
import magpie.user.CommandHandler;
import magpie.user.InputFileParser;
import magpie.user.server.ServerLauncher;

/**
 * Main program for text-based interface. Command line arguments are input files
 *  that you would like to read. After those are parsed, the program will switch to
 *  interactive mode.
 * 
 * <p><b><u>Command Line Options</u></b>
 * 
 * <p>Besides the name of input files, you can also supply a few options as 
 * input.
 * 
 * <p><b>-n &lt;threads&gt;</b>: Maximum number of threads 
 * 
 * <p><b>-server [...]</b>: Start in server mode. See {@linkplain ServerLauncher}
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class Magpie {
    /** Number of threads allowed */
    static public int NThreads = 1;
    
     /**
      * Launch Magpie.
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {        
        // Read in commands
        int pos = 0;
        while (pos < args.length) {
            boolean wasFile = false;
            switch (args[pos].toLowerCase()) {
                case "-n": case "-np":
                    NThreads = Integer.parseInt(args[++pos]);
                    break;
                case "-server":
                    ServerLauncher.main(Arrays.copyOfRange(args, pos+1, args.length));
                    return;
                default: // Assume the argument is a file name
                    wasFile = true;
                    break;
            }
            if (wasFile) break; // Go to file mode
            pos++;
        }
        
        // Prepare to parse input files
        InputFileParser Parser = new InputFileParser();
        CommandHandler Commander = new CommandHandler();

        // Read all provided input files
        for (int i=pos; i<args.length; i++) {
            Commander.readFile(args[i]);
        }

        // Get additional commands from standard in
        List<String> Command;
        do {
            Command = Parser.getCommands();
            if (Command != null) 
                Commander.runCommand(Command);
        } while (Command != null);

        // Close whatever we are reading from standard in
        Parser.closeFile();
    }
}
