package magpie;

import java.util.Arrays;
import java.util.List;
import magpie.user.CommandHandler;
import magpie.user.InputFileParser;
import magpie.user.server.MagpieServer;

/**
 * Main program for text-based interface. Command line arguments are input files
 *  that you would like to read. After those are parsed, the program will switch to
 *  interactive mode.
 * 
 * @author Logan Ward
 * @version 0.1
 */
public class Magpie {
     /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
		// Determine whether the user wants to go to server mode
		if (args.length > 0 && args[0].equalsIgnoreCase("-server")) {
			// If so, call MagpieServer
			MagpieServer.main(Arrays.copyOfRange(args, 1, args.length));
		} else {
			// Prepare to parse input files
			InputFileParser Parser = new InputFileParser();
			CommandHandler Commander = new CommandHandler();
        
			// Read all provided input files
			for (String arg : args)
				Commander.readFile(arg);

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
}
