/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package magpie.user.server;

import java.io.*;
import java.util.*;
import java.net.Socket;
import junit.framework.TestCase;
import magpie.data.materials.CompositionDataset;
import magpie.models.BaseModel;
import magpie.models.regression.GuessMeanRegression;

/**
 * Run basic tests on Magpie server
 * @author Logan Ward
 */
public class MagpieServerTest extends TestCase {
    
    public MagpieServerTest(String testName) {
        super(testName);
    }
    
    private void launchServer(final String[] args) {
        // Make a fake dataset
        CompositionDataset template = new CompositionDataset();
        try { 
            template.setDataDirectory("../Magpie/Lookup Data/");
            template.addEntry("CuZr");
            template.addEntry("NiZr");
            template.getEntry(0).setMeasuredClass(0.0);
            template.getEntry(1).setMeasuredClass(1.0);
            template.generateAttributes();
        } catch (Exception e) {
            throw new Error(e);
        }
        MagpieServer.TemplateDataset = template;
        
        // Make a fake model
        BaseModel model = new GuessMeanRegression();
        model.train(template);
        
        // Add fake model to server
        MagpieServer.Models.put("prop", model);
        
        Runnable toLaunch = new Runnable() {
            @Override
            public void run() {
                MagpieServer.main(args);
            }
        };
        Thread thread = new Thread(toLaunch);
        thread.start();
    }

    /**
     * Test the ability to start the sever
     */
    public void testServerStarting() {
        MagpieServer.NToAccept = 0;
        launchServer(new String[0]);
    }
    
    /**
     * Test connecting to a client
     */
    public void testClientConnection() {
        MagpieServer.NToAccept = 1;
        launchServer(new String[0]);
		List<String> linesRead = runCommand(new String[]{"#done"});
		assertTrue(linesRead.isEmpty());
    }
    
    /**
     * Test evaluate command
     */
    public void testEvaluateCommand() {
        MagpieServer.NToAccept = 1;
        
        // Test server
        launchServer(new String[0]);
        List<String> linesRead = runCommand(new String[]{"evaluate prop", "NaCl", "#done"});
		assertEquals(1, linesRead.size());
    }
	
	/**
     * Test single objective search
     */
    public void testSingleObjectiveSearch() throws Exception {
        MagpieServer.NToAccept = 1;
        
        // Test server
        launchServer(new String[]{"-data", "models/data.obj", "-model", 
            "volume", "models/volume.obj"});
		Thread.sleep(2000);
        List<String> linesRead = runCommand(new String[]{
			"search PhaseDiagramCompositionEntryGenerator 3 -crystal 5 Al Zr Ti Mg",
			"objective volume target 20", 
			"number 10",
			"#done"});
		assertEquals(10, linesRead.size());
    }
	
	/**
     * Test evaluate command
     */
    public void testMultiObjectiveSearch() throws Exception {
        MagpieServer.NToAccept = 1;
        
        // Test server
        launchServer(new String[]{"-data", "models/data.obj", "-model", 
            "volume", "models/volume.obj", "-model", "bandgap", "models/bandgap.obj" });
		Thread.sleep(2000);
       List<String> linesRead = runCommand(new String[]{
			"search PhaseDiagramCompositionEntryGenerator 3 -crystal 5 Al Zr Ti Mg",
			"multi 10", 
			"volume target 20",
			"bandgap maximize",
			"number 10",
			"#done"});
		assertEquals(10, linesRead.size());
    }
    
    public void testRealLaunch() throws Exception {
        MagpieServer.NToAccept = 1;
        
        // Test server
        launchServer(new String[]{"-data", "models/data.obj", "-model", 
            "volume", "models/volume.obj"});
		Thread.sleep(1000);
        List<String> linesRead = runCommand(new String[]{"evaluate volume", "NaCl", "#done"});
		assertEquals(1, linesRead.size());
    }

	/**
	 * Open a socket and run a command.
	 * @param command List of lines in command to be run
	 * @return Output from command
	 */
	private List<String> runCommand(String[] command) {
		Socket connection;
		try {
			connection = new Socket("127.0.0.1", MagpieServer.ListenPort);
		} catch (IOException e) {
			throw new Error(e);
		}
		
		// Interact with server
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			PrintWriter out = new PrintWriter(connection.getOutputStream(), true);
			
			// Write things to the server
			for (String line : command) 
				out.println(line);
			
			// Read in lines
			List<String> linesRead = new LinkedList<>();
			while (true) {
				String line = in.readLine();
				if (line != null) {
					linesRead.add(line);
				} else {
					break;
				}
			}
            
            // Wrap up
            connection.close();
			return linesRead;
		} catch (IOException e) {
			throw new Error(e);
		}
	}
    
}
