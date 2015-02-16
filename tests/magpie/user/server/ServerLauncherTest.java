package magpie.user.server;

import java.io.*;
import java.util.*;
import java.net.Socket;

import magpie.data.materials.CompositionDataset;
import magpie.models.BaseModel;
import magpie.models.regression.GuessMeanRegression;
import magpie.user.server.thrift.Entry;
import magpie.user.server.thrift.MagpieServer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.Test;

/**
 * Run basic tests on Magpie server
 * @author Logan Ward
 */
public class ServerLauncherTest {

    public ServerLauncherTest() throws Exception {
        // Make a fake dataset
        CompositionDataset template = new CompositionDataset();
        template.setDataDirectory("./Lookup Data");
        template.importText("datasets/small_set.txt", null);
        template.setTargetProperty("delta_e", true);
        template.generateAttributes();
        template.saveState("ms-data.obj");
        new File("ms-data.obj").deleteOnExit();
        
        // Make a fake model
        BaseModel model = new GuessMeanRegression();
        model.train(template);
        model.saveState("ms-deltae.obj");
        new File("ms-deltae.obj").deleteOnExit();
    }
    
    private MagpieServer.Client getClient() {
        TTransport t = new TSocket("127.0.0.1", 4581);
        TProtocol prot = new TBinaryProtocol(t);
        MagpieServer.Client client = new MagpieServer.Client(prot);
        return client;
    }
    
    private TServer launchServer() throws Exception {
        List<String> args = new LinkedList<>();
        args.add("-data");
        args.add("ms-data.obj");
        args.add("-model");
        args.add("delta_e");
        args.add("ms-deltae.obj");
                
        return ServerLauncher.main(args.toArray(new String[0]));
    }

    @Test
    public void testServerStarting() throws Exception {
        TServer s = launchServer();
        s.stop();
    }
    
    @Test
    public void testEvaluateCommand() throws Exception {
        TServer s = launchServer();
        if (! s.isServing()) {
            throw new Error("Server not running");
        }
        MagpieServer.Client client = getClient();
        List<Entry> entries = new LinkedList<>();
        entries.add(new Entry("NaCl", new TreeMap<String, Double>()));
        entries.add(new Entry("Mg3Al", new TreeMap<String, Double>()));
        List<String> props = new LinkedList<>();
        props.add("delta_e");
        client.evaluateProperties(entries, props);
        s.stop();
    }
	
//	/**
//     * Test single objective search
//     */
//    public void testSingleObjectiveSearch() throws Exception {
//        ServerLauncher.NToAccept = 1;
//        
//        // Test server
//        launchServer(new String[]{"-data", "models/data.obj", "-model", 
//            "volume", "models/volume.obj"});
//		Thread.sleep(2000);
//        List<String> linesRead = runCommand(new String[]{
//			"search PhaseDiagramCompositionEntryGenerator 3 -crystal 5 Al Zr Ti Mg",
//			"objective volume target 20", 
//			"number 10",
//			"#done"});
//		assertEquals(10, linesRead.size());
//    }
//	
//	/**
//     * Test evaluate command
//     */
//    public void testMultiObjectiveSearch() throws Exception {
//        ServerLauncher.NToAccept = 1;
//        
//        // Test server
//        launchServer(new String[]{"-data", "models/data.obj", "-model", 
//            "volume", "models/volume.obj", "-model", "bandgap", "models/bandgap.obj" });
//		Thread.sleep(2000);
//       List<String> linesRead = runCommand(new String[]{
//			"search PhaseDiagramCompositionEntryGenerator 3 -crystal 5 Al Zr Ti Mg",
//			"multi 10", 
//			"volume target 20",
//			"bandgap maximize",
//			"number 10",
//			"#done"});
//		assertEquals(10, linesRead.size());
//    }
}
