package magpie.user;

import magpie.data.BaseEntry;
import magpie.data.Dataset;
import magpie.data.MultiPropertyDataset;
import magpie.models.BaseModel;
import magpie.models.regression.AbstractRegressionModel;
import magpie.models.regression.MultiObjectiveRegression;
import magpie.models.regression.MultiPropertyRegression;
import magpie.utility.UtilityOperations;
import magpie.utility.WekaUtility;
import magpie.utility.interfaces.*;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;

/**
 * This class turns text commands into actions.
 *
 * @author Logan Ward
 */
public class CommandHandler {
    /** Keep track of which variables have been created */
    protected final Workspace Workspace = new Workspace();
    /** Which elemental properties to use when calculating features */
    protected final Set<String> ElementalProperties = new TreeSet<>();
    /** Development use: Print stack trace on failures */
    final private boolean Debug = true;
    /** Holds start times of various timers */
    final private Map<String,Long> Timers = new TreeMap<>();
    /**
     * Whether to echo commands to screen
     */
    public boolean EchoCommands = false;
    /** Holds start time of default timer */
    public long StartTime = System.currentTimeMillis();
    /**
     * Directory that contain elemental property lookup files
     */
    protected String ElementalPropertyDirectory = "./lookup-data";
    /**
     * Whether to exit on errors
     */
    private boolean Forgiving = true;

    /**
     * Run an evaluation command. Given a model to run, a Dataset to generate
     * attributes, and prints out the predictions.
     * <p>
     * <p><b>Usage</b>: $&lt;model&gt; $&lt;dataset&gt; &lt;entries...&gt;
     * <br><i>model</i>: Model used to make predictions
     * <br><i>dataset</i>: Dataset template used to generate attributes
     * <br><i>entries...</i>: Strings representing entries to evaluate
     *
     * @param Command Command to evaluate
     * @return Table of results
     * @throws Exception
     */
    static protected String runEvaluationCommand(List<Object> Command) throws Exception {
        // Get model, dataset
        BaseModel model;
        Dataset data;
        try {
            model = (BaseModel) Command.get(0);
            data = (Dataset) Command.get(1);
            data = data.emptyClone();
        } catch (Exception e) {
            throw new Exception("Usage: $<model> $<dataset template> <entries to evaluate...>");
        }

        // Parse entries
        try {
            for (String entry : convertCommandToString(Command.subList(2, Command.size()))) {
                data.addEntry(entry);
            }
        } catch (Exception e) {
            throw new Exception(e);
        }

        // Generate attributes for all entries
        data.generateAttributes();

        // Run them
        model.run(data);

        // Get names of each compound
        String[] names = new String[data.NEntries()];
        int nameLength = 0;
        for (int i = 0; i < data.NEntries(); i++) {
            BaseEntry entry = data.getEntry(i);
            names[i] = entry.toString();
            if (names[i].length() > nameLength) {
                nameLength = names[i].length();
            }
        }

        // Assemble table
        String output;
        if (model instanceof MultiObjectiveRegression) {
            // Print the value of each property for each entry

            // Start lines of the header and each entry
            output = String.format("%" + (nameLength + 2) + "s", "Entry");
            String[] entryLine = new String[data.NEntries()];
            for (int i = 0; i < entryLine.length; i++) {
                entryLine[i] = String.format("%" + (nameLength + 2) + "s", names[i]);
            }

            // Add name of each property to header, and values to each entry
            MultiObjectiveRegression mptr = (MultiObjectiveRegression) model;
            MultiPropertyDataset dptr = (MultiPropertyDataset) data;
            for (String property : mptr.getPropertiesBeingModeled()) {
                int propLength = Math.max(property.length(), 10);
                output += String.format("  %" + (propLength) + "s", property);
                double[] propValues = dptr.getPredictedPropertyArray(property);
                for (int i = 0; i < data.NEntries(); i++) {
                    entryLine[i] += String.format("  %" + (propLength) + ".4g", propValues[i]);
                }
            }

            // If it is not a MultiPropertyRegression, add in predicted class
            if (!(model instanceof MultiPropertyRegression)) {
                double[] predClass = data.getPredictedClassArray();
                output += String.format("  %16s", "Predicted Class");
                for (int i = 0; i < data.NEntries(); i++) {
                    entryLine[i] += String.format("  %16.4g", predClass[i]);
                }
            }

            // Compile everything together
            for (String line : entryLine) {
                output += "\n" + line;
            }
        } else {
            output = String.format("%" + (nameLength + 2) + "s  %16s", "Entry", "Predicted Class");
            for (int i = 0; i < data.NEntries(); i++) {
                BaseEntry entry = data.getEntry(i);
                if (model instanceof AbstractRegressionModel) {
                    output += String.format("\n%" + (nameLength + 2) + "s  %16.4g",
                            names[i], entry.getPredictedClass());
                } else {
                    int cls = (int) entry.getPredictedClass();
                    double prob = entry.getClassProbilities()[cls] * 100.0;
                    output += String.format("\n%" + (nameLength + 2)
                                    + "s   %s (%.2f %%)",
                            names[i], data.getClassName(cls), prob);
                }
            }
        }
        return output + "\n";
    }

    /**
     * Create new instance of a class given type and options. Class name must be fully
     * specified within the magpie package (i.e. "data.materials.CompositionDataset")
     *
     * @param ClassType Full name of class to create minus "magpie"
     * @param Options   Array specifying option commands (can be empty/null)
     * @return Newly instantiated class
     * @throws Exception For various reasons
     */
    static public Object instantiateClass(String ClassType, List<Object> Options) throws Exception {
        Object NewObj;
        try {
            Class x = Class.forName("magpie." + ClassType);
            NewObj = x.newInstance();
        } catch (ClassNotFoundException e) {
            throw new Exception("Class " + ClassType + " not found");
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Something wrong with " + ClassType + "'s implementation\n" + e);
        }
        if (NewObj instanceof Options) {
            if (Options == null)
                Options = new LinkedList<>();
            Options Ptr = (Options) NewObj;
            Ptr.setOptions(Options);
        }
        return NewObj;
    }

    /**
     * Convert a command (List&lt;Object>) to string. Uses {@link Object#toString()}
     *
     * @param Command Command to be converted
     * @return Command as an array of strings
     */
    static public String[] convertCommandToString(List<Object> Command) {
        String[] output = new String[Command.size()];
        for (int i = 0; i < output.length; i++)
            output[i] = Command.get(i).toString();
        return output;
    }
    
    /**
     * Given a command, run some actions
     * @param TextCommand List of words that comprise a command
     */
    public void runCommand(List<String> TextCommand) {
        // If desired, print command to screen
        if (EchoCommands && TextCommand.size() > 0) {
            Iterator<String> iter = TextCommand.iterator();
            while (iter.hasNext()) {
                String temp = iter.next();
                if (temp.contains(" "))
                    System.out.print("\"" + temp + "\"" + " ");
                else
                    System.out.print(temp + " ");
            }
            System.out.println();
        }

        // Reset standard error and standard out
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));

        try {
            // Go through each possible command syntax
            if (TextCommand.isEmpty()) {
            } else if (TextCommand.get(0).equalsIgnoreCase("exit")) {
                System.exit(0);
            } else if (TextCommand.get(0).equalsIgnoreCase("list")) {
                System.out.println(Workspace.printWorkspace());
            } else if (TextCommand.get(0).equalsIgnoreCase("citations")) {
                printCitations(TextCommand);
            } else if (TextCommand.get(0).equalsIgnoreCase("delete")) {
                // Delete variables from the Workspace
                for (int i=1; i<TextCommand.size(); i++)
                    Workspace.removeVariable(TextCommand.get(i));
            } else if (TextCommand.get(0).equalsIgnoreCase("echo")) {
                for (int i = 1; i < TextCommand.size(); i++) {
                    if (TextCommand.get(i).contains(" "))
                        System.out.print("\"");
                    System.out.print(TextCommand.get(i) + " ");
                    if (TextCommand.get(i).contains(" "))
                        System.out.print("\"");
                }
                System.out.println();
			} else if (TextCommand.get(0).equalsIgnoreCase("evaluate")) {
				String output = runEvaluationCommand(expandTextCommand(TextCommand.subList(1, TextCommand.size())));
				System.out.println();
				System.out.println(output);
            } else if (TextCommand.get(0).equalsIgnoreCase("print")) {
                handlePrinting(TextCommand);
            } else if (TextCommand.get(0).equalsIgnoreCase("save")) {
                handleSaving(TextCommand);
            } else if (TextCommand.get(0).equalsIgnoreCase("read")) {
                if (TextCommand.size() < 2)
                    throw new Exception("Usage: read <filename>");
                readFile(TextCommand.get(1));
            } else if (TextCommand.get(0).equalsIgnoreCase("set")) {
                handleSettings(TextCommand);
            } else if (TextCommand.get(0).equalsIgnoreCase("timer")) {
                String output = runTimerCommand(TextCommand.subList(1, TextCommand.size()));
                System.out.println("\t" + output);
            } else if (TextCommand.size() > 1 && TextCommand.get(1).equals("=")) {
                assignment(TextCommand);
            } else {
                // If none of the other motifs hit, then it is variable command
                String variableName = TextCommand.get(0);
                List<Object> varCommand = expandTextCommand(TextCommand.subList(1, TextCommand.size()));
                runCommandOnVariable(variableName, varCommand);
            }
        } catch (Exception e) {
            if (Debug) {
                e.printStackTrace();
            }
            System.err.println(e.getMessage());
            if (! Forgiving) {
                System.exit(3);
            }
        }
    }

    /**
     * Given a text command, change any word marked with a "$" in the front
     *  to the corresponding variable.
     * @param TextCommand Command as a list of text words
     * @return Command as a list of objects
     * @throws Exception
     */
    protected List<Object> expandTextCommand(List<String> TextCommand) throws Exception {
        List<Object> varCommand = new ArrayList<>(TextCommand.size());
        for (String s : TextCommand) {
            Object toAdd;
            if (s.startsWith("$")) {
                toAdd = Workspace.getObject(s.substring(1));
            } else {
                toAdd = s;
            }
            varCommand.add(toAdd);
        }
        return varCommand;
    }

    /**
     * Handle variable assignment
     * @param Command Command to be parsed
     * @throws Exception If something goes wrong
     */
    protected void assignment(List<String> Command) throws Exception {
        // Check if command is properly formatted
        if (Command.size() < 4)
            throw new Exception("Usage: <variable name> = <action> [<options...>]");

        String VariableName = Command.get(0);
        String Action = Command.get(2); // What to do
        Object NewObj = null;

        switch (Action.toLowerCase()) {
            case "new":
                // Make a new instance of the expected class
                String ClassType = Command.get(3);
                List<Object> Options = expandTextCommand(Command.subList(4, Command.size()));
                NewObj = instantiateClass(ClassType, Options);
                break;
            case "load":
                // Ensure we have the full Weka library
                WekaUtility.importWekaHome();
                // Load in a serialized class
                String Filename = Command.get(3);
                try (FileInputStream fp = new FileInputStream(Filename)) {
                    ObjectInputStream in; in = new ObjectInputStream(fp);
                    NewObj = in.readObject();
                    in.close(); fp.close();
                } catch (InvalidClassException i) {
                    throw new Exception("File contains out-of-date class: " + i.classname);
                } catch (IOException i) {
                    throw new Exception("Failed to load object at: " + Filename
                            + "\nReason: " + i.getMessage());
                } catch (ClassNotFoundException e) {
                    throw new Exception("Class definition not found. Check libaries. Error text: " + e.getLocalizedMessage());
                }
                break;
            default: {
                String varToRunOn = Command.get(2);
                List<Object> varCommand = expandTextCommand(Command.subList(3, Command.size()));
                NewObj = runCommandOnVariable(varToRunOn, varCommand);
            }

        }

        Workspace.addVariable(VariableName, NewObj);
    }
    
    /**
     * Handles commands that change global settings
     * @param Command Command to be parsed
     * @throws Exception For various reasons
     */
    protected void handleSettings(List<String> Command) throws Exception {
        switch (Command.get(1).toLowerCase()) {
            case "echo" : {
                switch (Command.get(2).toLowerCase()) {
                    case "true": case "on": case "yes":
                        EchoCommands = true; break;
                    case "false": case "off": case "no":
                        EchoCommands = false; break;
                    default:
                        throw new Exception("Usage: set echo <on|off>");
                } break;
            }
            default:
                throw new Exception("ERROR: Setting " + Command.get(1) + " not recognized.");
        }
    }

    /**
     * Pass commands to variables. The requested operations may generate some output, but
     *   otherwise generate null.
     *
     * @param variableName Name of variable to act on
     * @param Command Command to be executed
     * @return Output from the requested operation, null if nothing generated
     * @throws Exception If execution fails
     */
    protected Object runCommandOnVariable(String variableName, List<Object> Command) throws Exception {
        // Look to see if this is a variable name
        if (Workspace.hasVariable(variableName)) {
            // Run operations for different variable types
            Object Obj = Workspace.getObject(variableName);
            if (Obj instanceof Commandable) {
                Commandable Ptr = (Commandable) Obj;
                return Ptr.runCommand(Command);
            } else {
                throw new Exception("Variable " + variableName + " does not take commands.");
            }
        } else {
            throw new Exception("ERROR: No such variable: " + variableName);
        }
    }

    /**
     * Reads commands from a text file
     * @param Filename Path of file to read
     */
    public void readFile(String Filename) {
        boolean EchoBefore = this.EchoCommands; EchoCommands = true;
        boolean ForgiveBefore = this.Forgiving; Forgiving = false;
        InputFileParser Parser = new InputFileParser();
        Parser.openFile(Filename);
        List<String> TempCommands;
        do {
            TempCommands = Parser.getCommands();
            if (TempCommands == null) break;
            this.runCommand(TempCommands);
        } while (true);
        EchoCommands = EchoBefore;
        Forgiving = ForgiveBefore;
    }
    
    /**
     * Run commands that require printing something to screen.
     *
     * <p>Expects commands to be of the form: print &lt;variable name> &lt;command> [&lt;options>]
     *
     * @param Command Print commands to be parsed
     * @throws Exception For various reasons
     */
    protected void handlePrinting(List<String> Command) throws Exception {
        System.out.println();
        if (Command.size() < 2) {
            throw new Exception("Usage: print <variable name> <command> [<options...>]");
        }

        // Get the variable
        String VariableName = Command.get(1);
        Object Variable = Workspace.getObject(VariableName);
        List<String> PrintCommand = Command.subList(2, Command.size());

        // Make sure this is printable
        if (! (Variable instanceof Printable))
            throw new Exception("Variable " + VariableName + " is not printable.");

        // Get the output header
        String header = "Variable: " + VariableName;
        if (PrintCommand.size() > 1) {
            header += " - Command: " + PrintCommand.get(0);
            for (int j=1; j < PrintCommand.size(); j++)
                header += " " + PrintCommand.get(j);
        }
        System.out.println(header);

        // Run the print command
        Printable Ptr = (Printable) Variable;

        if (PrintCommand.isEmpty()) {
            System.out.println(Ptr.about());
        } else {
            System.out.println(Ptr.printCommand(PrintCommand));
        }
        System.out.println();
    }
    
    /**
     * Run a certain time command. Two options:
     *
     * <ul>
     * <li><b>start [&lt;name&gt;]</b> - Initialize timer. If no name, alters default timer</li>
     * <li><b>elapsed [&lt;name&gt;]</b> - Return elapsed time. If no name, prints default timer</lI>
     * </ul>
     *
     * @param Command Timer command to be parsed
     * @return String describing result
     * @throws Exception
     */
    protected String runTimerCommand(List<String> Command) throws Exception {
        if (Command.isEmpty()) {
            throw new Exception("Possible timer commands: \"start\" and \"elapsed\"");
        }
        String Action = Command.get(0).toLowerCase();
        switch (Action.toLowerCase()) {
            case "start":
                if (Command.size() < 2) {
                    StartTime = System.currentTimeMillis();
                    return "Zeroed default timer";
                } else {
                    String name = Command.get(1);
                    Timers.put(name, System.currentTimeMillis());
                    return "Started timer: " + name;
                }
            case "elapsed": {
                long start = StartTime;
                String wording = "Total time elapsed";
                if (Command.size() > 1) {
                    String name = Command.get(1);
                    if (! Timers.containsKey(name)) {
                        throw new Exception("Timer not defined: " + name);
                    }
                    start = Timers.get(name);
                    wording = "Time elapsed on " + name;
                }
                double elapsed = (double) (System.currentTimeMillis() - start);
                return String.format("%s: %.3f s", wording, elapsed / 1000.0);
            }
            default:
                throw new Exception("Timer command not defined: " + Action);
        }
    }

    /**
     * Save objects to disk as various formats.
     * @param Command Command to be parsed
     * @throws java.lang.Exception
     */
    protected void handleSaving(List<String> Command) throws Exception {
        String Basename, Option, ObjectName;
        try {
            ObjectName = Command.get(1);
            Basename = Command.get(2);
            Option = Command.size() > 3 ? Command.get(3) : "serial";
        } catch (Exception e) {
            throw new Exception("Usage: save <variable name> <filename> [<format=serial>]");
        }

        Object Obj = Workspace.getObject(ObjectName);
        if (! Option.equalsIgnoreCase("serial")) { // Do something fancy
            if (Obj instanceof Savable) {
                    Savable Ptr = (Savable) Obj;
                String Filename = Ptr.saveCommand(Basename, Option);
                System.out.println("\tSaved " + ObjectName + " to disk in " + Option
                            + " format to " + Filename);
                } else
                    throw new Exception("ERROR: Object \"" + ObjectName + "\" does"
                            + " not implement the Savable interface");
        } else { // Just serialize it
            UtilityOperations.saveState(Obj, Basename + ".obj");
            System.out.println("\tSerialized " + ObjectName + " to " + Basename + ".obj");
        }
    }
    
    /**
     * Run command to print out citations. First word should be "citations".
     * If there are additional words in the command, they should be names of variables
     *
     * <p><b>Usage</b>: citations [&lt;variable names...&gt;]
     * <br><pr><i>variable names</i>: Optional: Names of variables to be printed
     *
     * @param command Command to be run
     * @throws Exception If variables are not found in workspace
     */
    public void printCitations(List<String> command) throws Exception {
        // If command is only one word, print out citations for all variables
        List<String> varToPrint;
        if (command.size() == 1) {
            varToPrint = new ArrayList<>(Workspace.getVariableNames());
        } else {
            varToPrint = command.subList(1, command.size());
        }

        // Print out all of the variables
        for (String varName : varToPrint) {
            // Print out title
            System.out.println("Suggested citations for " + varName + ":");
            System.out.println();

            // Print out citation data
            for (String line : getCitationDescriptions(varName)) {
                System.out.println("\t" + line);
            }

            // Print out an extra newline
            System.out.println();
        }
    }
    
    /**
     * Print out citation information associated with a single variable
     * @param variableName Variable to be assessed
     * @return Lines of output for variable being assessed
     * @throws java.lang.Exception If variable not found
     */
    public List<String> getCitationDescriptions(String variableName) throws Exception {
        // Get the variable
        Object var = Workspace.getObject(variableName);

        // Initialize output
        List<String> output = new ArrayList<>();

        // Get citation information
        if (var instanceof Citable) {
            // Get the citations
            Citable intf = (Citable) var;
            List<Pair<String, Citation>> citations = intf.getCitations();

            // Format the output
            for (Pair<String,Citation> citation : citations) {
                // Add in reason for citation
                output.add("Reason: " + citation.getLeft());

                // Add in the citation information
                output.addAll(Arrays.asList(citation.getRight().printInformation().split("\n")));
                output.add(""); // Newline
            }

        } else {
            // If not citable, say so
            output.add("No citation necessary.");
        }

        return output;
    }
}
