package magpie.user;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Parses user input files. Contains operations that remove comments, blank lines, and combine
 * words surrounded by quotation marks in order to supply the {@link CommandHandler} with
 * clean commands. Can read from an input file or standard in (if no file is provided).<p>
 * 
 * Note: If a user input a "" from standard in, the program will exit. I haven't 
 *  figured out how to fix and let the program exit when an EOF is return from command line.
 * 
 * @author Logan Ward
 * @version 0.1
 */

public class InputFileParser {
    /** Reader holding the input file */
    protected BufferedReader inputFile = null;
    /** Whether an input file is open */
    protected boolean fileIsOpen = false;

    /** 
     * Create an instance that reads from standard in.
     */
    public InputFileParser() {
        inputFile = new BufferedReader(new InputStreamReader(System.in));
    }
    
    /** 
     * Open an input file 
     * @param filename File to be read as input
     */
    public void openFile(String filename) {
        try {
            inputFile = Files.newBufferedReader(Paths.get(filename), Charset.forName("ISO-8859-1"));
        } catch (Exception e) {
            throw new Error("ERROR: Input file \"" + filename + "\" cannot be opened");
        }
        fileIsOpen = true;
    }
    
    /**
     * Closes currently open file. If none open, returns quietly. Returns focus back to
     * standard in.
     */
    public void closeFile() { 
        try { 
            if (fileIsOpen) inputFile.close();
            inputFile = new BufferedReader(new InputStreamReader(System.in));
        }
        catch (IOException e ) { /* */ }
    }
    
    /** 
     * From the open input file, read in the next command.
     * @return Command parsed into individual words. Null if no more input
     */
    public List<String> getCommands() {
        LinkedList<String> commands = new LinkedList<>();
        // Read the base of the command
        if (! fileIsOpen) {
            System.out.print("> ");
        }
        String Line = readLine();
        if (Line == null) return null;
        String[] Words = Line.split("\\s+");
        commands.addAll(Arrays.asList(Words));
        
        // Read any extra lines 
        while (Words[Words.length-1].equals("&")) {
            commands.remove("&");
            Line = readLine();
            if (Line == null) return null;
            Words = Line.split("\\s+");
            commands.addAll(Arrays.asList(Words));
        }
        
        // Add together commands separated by quotes
        int openQuotes = -1, i = 0;
        while (i < commands.size()) {
            if (openQuotes != -1) { // Extend the command
                String temp = commands.get(openQuotes); 
                temp += " " + commands.get(i);
                commands.remove(i);
                if (temp.endsWith("\"")) {
                    temp = temp.substring(0, temp.length() - 1);
                    commands.set(openQuotes, temp);
                    openQuotes = -1;
                } else 
                    commands.set(openQuotes, temp);
            } else {
                if (commands.get(i).startsWith("\"")) {
                    String temp = commands.get(i).substring(1); // Remove the \"
                    if (temp.endsWith("\"")) 
                        temp = temp.substring(0, temp.length() - 1);
                    else 
                        openQuotes = i;
                    commands.set(i, temp);
                }
                i++;
            }
        }
        
        return commands;
    }
    
    /** 
     * Gets a complete, nonempty line out of the input stream. Removes comments
     * @return Complete line
     */
    protected String readLine() {
        String Line = "";
        try {
            while (Line.length() == 0) {
                Line = inputFile.readLine();
                if (Line == null || ("".equals(Line) && ! inputFile.ready())) // EOF has been reached
                    return null;
                        
                // Remove comments
                int x = Line.indexOf("//");
                if (x != -1)
                    if (x == 0) { Line = ""; continue; }
                    else Line = Line.substring(0, x - 1);
                Line = Line.trim(); // Removes whitespace
            }
        } catch (Exception e) {
            throw new Error("Some sort of IO error has occured");
        }
        return Line;
    }
}
