package magpie.user.server;

import java.io.*;
import java.util.*;

import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.models.BaseModel;
import magpie.models.classification.WekaClassifier;
import magpie.models.regression.GuessMeanRegression;
import magpie.user.server.thrift.Entry;
import magpie.user.server.thrift.MagpieServer;
import magpie.user.server.thrift.ModelInfo;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.junit.*;
import org.junit.rules.Timeout;

/**
 * Run basic tests on Magpie Server
 * @author Logan Ward
 */
public class ServerLauncherTest {
    /** Timeout for test */
    @Rule
    public Timeout globalTimeout = new Timeout(60000);

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
        model.crossValidate(10, template);
        model.saveState("ms-volume.obj");
        new File("ms-volume.obj").deleteOnExit();
        
        // Make a fake metal/nonmetal model
        WekaClassifier metal = new WekaClassifier("trees.REPTree", null);
        template.setTargetProperty("bandgap", false);
        NonZeroClassModifier mdfr = new NonZeroClassModifier();
        mdfr.transform(template);
        metal.train(template);
        metal.saveState("ms-metal.obj");
        new File("ms-metal.obj").deleteOnExit();
        
        // Create fake input file
        PrintWriter fp = new PrintWriter("ms-model.info");
        fp.println("entry delta_e");
        fp.println("ms-deltae.obj");
        fp.println("ms-data.obj");
        fp.println("property &Delta;H");
        fp.println("units eV/atom");
        fp.println("author Logan Ward");
        fp.println("citation None");
        fp.println("notes Simple model created to demonstrate formation energy prediction");
        fp.println("entry volume_pa");
        fp.println("ms-volume.obj");
        fp.println("ms-data.obj");
        fp.println("entry ismetal");
        fp.println("ms-metal.obj");
        fp.println("ms-data.obj");
        fp.close();
        new File("ms-model.info").deleteOnExit();
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
        if (ServerLauncher.isRunning()) {
            ServerLauncher.stopServer();
        }
        
        List<String> args = new LinkedList<>();
        args.add("-model");
        args.add("ms-model.info");
                
        ServerLauncher.main(args.toArray(new String[0]));
    }
    
    @After
    public void afterTest() throws Exception {
        ServerLauncher.stopServer();
    }

    @Test
    public void testServerStarting() throws Exception {
        Assert.assertTrue(ServerLauncher.HTTPServer.isStarted());
    }
    
    @Test
    public void testModelInfo() throws Exception {
        MagpieServer.Client client = getClient();
        
        Map<String, ModelInfo> info = client.getModelInformation();
        
        Assert.assertEquals(3, info.size());
        Assert.assertEquals("NonZero;Zero", info.get("ismetal").units);
        Assert.assertTrue(info.get("volume_pa").valMethod.contains("10-fold"));
        Assert.assertTrue(info.get("delta_e").valMethod.contains("Un"));
        System.out.println(info.get("delta_e").trainTime);
    }
    
    @Test
    public void testEvaluateCommand() throws Exception {
        MagpieServer.Client client = getClient();
        List<Entry> entries = new LinkedList<>();
        Entry newEntry = new Entry(); newEntry.name = "NaCl";
        entries.add(newEntry);
        newEntry = new Entry(); newEntry.name = "Mg3Al";
        entries.add(newEntry);
        newEntry = new Entry(); newEntry.name = "#!";
        entries.add(newEntry);
        List<String> props = new LinkedList<>();
        props.add("delta_e");
        List<Entry> output = client.evaluateProperties(entries, props);
        Assert.assertEquals(3, output.size());
        Assert.assertEquals(1, output.get(0).predictedProperties.size());
        Assert.assertTrue(Double.isNaN(output.get(2).predictedProperties.get("delta_e")));
    }
	
	@Test
    public void testSingleObjectiveSearch() throws Exception {
        MagpieServer.Client client = getClient();
        List<Entry> output = client.searchSingleObjective(
                "delta_e minimize SimpleEntryRanker",
                "PhaseDiagramCompositionEntryGenerator 1 2 -alloy 2 Al Ni Zr",
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
                "PhaseDiagramCompositionEntryGenerator 1 2 -alloy 2 Al Ni Zr",
                20);
        Assert.assertEquals(20, output.size());
    }
    
    @Test
    public void testClassProbability() throws Exception {
        MagpieServer.Client client = getClient();
        
        // Evaluate command
        List<Entry> entries = new LinkedList<>();
        Entry newEntry = new Entry();
        newEntry.name = "NaCl";
        entries.add(newEntry);
        List<String> props = new LinkedList<>();
        props.add("ismetal");
        entries = client.evaluateProperties(entries, props);
        Assert.assertEquals(1, entries.get(0).classProbs.size());
        Assert.assertEquals(2, entries.get(0).classProbs.get("ismetal").size());
        
        // Single objective search
        List<String> objs = new LinkedList<>();
        objs.add("ismetal maximize ClassProbabilityRanker NonZero");
        List<Entry> output = client.searchSingleObjective(objs.get(0),
                "PhaseDiagramCompositionEntryGenerator 1 2 -alloy 2 Al Ni Zr",
                20);
        Assert.assertEquals(20, output.size());

        // Multi objective search
        objs.add("delta_e minimize SimpleEntryRanker");
        output = client.searchMultiObjective(10.0, objs,
                "PhaseDiagramCompositionEntryGenerator 1 3 -crystal 4 Al Ni Zr O",
                20);
        Assert.assertEquals(20, output.size());
        Assert.assertEquals(2, output.get(0).classProbs.get("ismetal").size());
    }

}
