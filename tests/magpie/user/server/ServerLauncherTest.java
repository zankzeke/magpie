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
import org.junit.*;
import org.junit.Assert.*;

/**
 * Run basic tests on Magpie Server
 * @author Logan Ward
 */
public class ServerLauncherTest {
    public TServer Server = null;

    public ServerLauncherTest() throws Exception {
        // Make a fake dataset
        CompositionDataset template = new CompositionDataset();
        template.setDataDirectory("./Lookup Data");
        template.importText("datasets/small_set.txt", null);
        template.setTargetProperty("delta_e", true);
        template.generateAttributes();
        template.saveState("ms-data.obj");
        new File("ms-data.obj").deleteOnExit();
        
        // Make a fake model for delta_e
        BaseModel model = new GuessMeanRegression();
        model.train(template);
        model.saveState("ms-deltae.obj");
        new File("ms-deltae.obj").deleteOnExit();
        
        // Make a fake model for volume
        template.setTargetProperty("volume_pa", true);
        model.train(template);
        model.saveState("ms-volume.obj");
        new File("ms-volume.obj").deleteOnExit();
    }
    
    private MagpieServer.Client getClient() throws Exception {
        TTransport t = new TSocket("127.0.0.1", 4581);
        t.open();
        TProtocol prot = new TBinaryProtocol(t);
        MagpieServer.Client client = new MagpieServer.Client(prot);
        return client;
    }
    
    @Before
    public void launchServer() throws Exception {
        List<String> args = new LinkedList<>();
        args.add("-data");
        args.add("ms-data.obj");
        args.add("-model");
        args.add("delta_e");
        args.add("ms-deltae.obj");
        args.add("-model");
        args.add("volume_pa");
        args.add("ms-volume.obj");
                
        Server = ServerLauncher.main(args.toArray(new String[0]));
        Thread.sleep(1000);
    }
    
    @After
    public void afterTest() {
        Server.stop();
    }

    @Test
    public void testServerStarting() throws Exception {
        Assert.assertTrue(Server.isServing());
    }
    
    @Test
    public void testEvaluateCommand() throws Exception {
        MagpieServer.Client client = getClient();
        List<Entry> entries = new LinkedList<>();
        entries.add(new Entry("NaCl", new TreeMap<String, Double>()));
        entries.add(new Entry("Mg3Al", new TreeMap<String, Double>()));
        List<String> props = new LinkedList<>();
        props.add("delta_e");
        List<List<String>> output = client.evaluateProperties(entries, props);
        Assert.assertEquals(2, output.size());
        Assert.assertEquals(2, output.get(0).size());
    }
	
	@Test
    public void testSingleObjectiveSearch() throws Exception {
        MagpieServer.Client client = getClient();
        List<Entry> output = client.searchSingleObjective(
                "delta_e minimize SimpleEntryRanker",
                "PhaseDiagramCompositionEntryGenerator 2 -alloy 2 Al Ni Zr",
                20);
        Assert.assertEquals(20, output.size());
    }
    
    @Test
    public void testMultiObjectiveSearch() throws Exception {
        MagpieServer.Client client = getClient();
        List<String> objs = new LinkedList<>();
        objs.add("delta_e minimize SimpleEntryRanker");
        objs.add("volume_pa minimize TargetEntryRanker 20.0");
        List<Entry> output = client.searchMultiObjective(10.0, objs,
                "PhaseDiagramCompositionEntryGenerator 2 -alloy 2 Al Ni Zr",
                20);
        Assert.assertEquals(20, output.size());
    }

}
