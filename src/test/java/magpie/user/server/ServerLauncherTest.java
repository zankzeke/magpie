package magpie.user.server;

import magpie.data.Dataset;
import magpie.data.materials.CompositionDataset;
import magpie.data.materials.CrystalStructureDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.models.BaseModel;
import magpie.models.classification.WekaClassifier;
import magpie.models.regression.GuessMeanRegression;
import magpie.models.regression.WekaRegression;
import magpie.user.server.operations.ServerInformationGetter;
import magpie.utility.UtilityOperations;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Run basic tests on Magpie Server
 * @author Logan Ward
 */
public class ServerLauncherTest {
    /** Timeout for test */
    @Rule
    public Timeout globalTimeout = new Timeout(60, TimeUnit.SECONDS);

    /**
     * Client used to interact with test server
     */
    private WebTarget Target;

    public ServerLauncherTest() throws Exception {
        // Make a fake dataset
        CompositionDataset template = new CompositionDataset();
        template.setDataDirectory("./lookup-data");
        template.importText("datasets/small_set.txt", null);
        template.setTargetProperty("delta_e", true);
        template.generateAttributes();
        template.saveState("ms-data.obj");
        new File("ms-data.obj").deleteOnExit();
        
        // Make a fake model for delta_e
        BaseModel model = new GuessMeanRegression();
        model.train(template);
        model.crossValidate(10, template);
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

        // Make a crystal structure dataset model
        CrystalStructureDataset crystalDataset = new CrystalStructureDataset();
        crystalDataset.importText("datasets/icsd-sample", null);
        crystalDataset.setTargetProperty("delta_e", true);
        crystalDataset.generateAttributes();

        WekaRegression crystModel = new WekaRegression("trees.REPTree", null);
        crystModel.train(crystalDataset, true);
        crystModel.crossValidate(0.1, 5, crystalDataset, 1);

        crystalDataset.saveState("crystalModel-data.obj");
        new File("crystalModel-data.obj").deleteOnExit();
        crystModel.saveState("crystalModel-model.obj");
        new File("crystalModel-model.obj").deleteOnExit();
        
        // Create fake input file
        PrintWriter fp = new PrintWriter("ms-model.yml");
        fp.println("---");
        fp.println("name: delta_e");
        fp.println("modelPath: ms-deltae.obj");
        fp.println("datasetPath: ms-data.obj");
        fp.println("description: Just a formation enthalpy model");
        fp.println("property: '&Delta;H'");
        fp.println("units: eV/atom");
        fp.println("training: Some OQMD calculations");
        fp.println("author: Logan Ward");
        fp.println("citation: None");
        fp.println("notes: Simple model created to demonstrate formation energy prediction");
        fp.println("---");
        fp.println("name: volume_pa");
        fp.println("modelPath: ms-volume.obj");
        fp.println("datasetPath: ms-data.obj");
        fp.println("description: Just a specific volume model");
        fp.println("training: Some OQMD calculations");
        fp.println("property: V");
        fp.println("units: Angstrom<sup>3</sup>atom");
        fp.println("author: Logan Ward");
        fp.println("citation: None");
        fp.println("notes: Simple model created to demonstrate volume prediction");
        fp.println("---");
        fp.println("name: ismetal");
        fp.println("modelPath: ms-metal.obj");
        fp.println("datasetPath: ms-data.obj");
        fp.println("description: Guesses whether a material is metallic or not");
        fp.println("training: Some OQMD calculations");
        fp.println("property: E<sub>g</sub> > 0");
        fp.println("author: Logan Ward");
        fp.println("citation: None");
        fp.println("notes: Simple model that predicts whether an entry is a metal or not?");
        fp.println("---");
        fp.println("name: delta_e-crystal");
        fp.println("modelPath: crystalModel-model.obj");
        fp.println("datasetPath: crystalModel-data.obj");
        fp.println("description: Just a formation enthalpy model, but now on crystal structures");
        fp.println("property: '&Delta;H'");
        fp.println("units: eV/atom");
        fp.println("training: Some OQMD calculations");
        fp.println("author: Logan Ward");
        fp.println("citation: None");
        fp.println("notes: Simple model created to demonstrate formation energy prediction");
        fp.close();
        new File("ms-model.yml").deleteOnExit();
    }

    @Before
    public void launchServer() throws Exception {
        ServerLauncher.main(new String[]{"-port", "4234", "-models", "ms-model.yml"});

        Client c = ClientBuilder.newClient();
        Target = c.target("http://127.0.0.1:4234");
    }

    @After
    public void shutdownServer() throws Exception {
        ServerLauncher.Server.shutdownNow();
    }

    @Test
    public void testLaunch() throws Exception {
        assertNotNull(ServerLauncher.Server);
        assertTrue(ServerLauncher.Server.isStarted());
    }

    @Test
    public void testGetVersion() throws Exception {
        String response = Target.path("/server/version").request().get(String.class);
        assertEquals("0.0.1", response);
    }

    @Test
    public void testServerStatus() throws Exception {
        String response = Target.path("server/status").request().get(String.class);
        JSONObject status = new JSONObject(response);
        assertEquals(new ServerInformationGetter().getVersion(), status.get("apiVersion"));
        System.out.println(status.toString(2));
    }

    @Test
    public void testModelInformation() throws Exception {
        // Get the info of an existing model
        String response = Target.path("model/delta_e/info").request().get(String.class);
        JSONObject info = new JSONObject(response);
        assertEquals("Just a formation enthalpy model", info.get("description"));
        System.out.println(info.toString(2));

        // Test the 404
        Response response2 = Target.path("model/nope/info").request().get();
        assertEquals(404, response2.getStatus());
    }

    @Test
    public void testModelOutput() throws Exception {
        // Get the info of an existing model
        byte[] response = Target.path("model/delta_e/model").request().get(byte[].class);
        BaseModel model = (BaseModel) UtilityOperations.loadState(new ByteArrayInputStream(response));
        assertEquals(ServerLauncher.Models.get("delta_e").Model.getTrainTime(), model.getTrainTime());

        // Get the info of an existing model
        response = Target.path("model/delta_e/dataset").request().get(byte[].class);
        Dataset data = (Dataset) UtilityOperations.loadState(new ByteArrayInputStream(response));
        assertEquals(ServerLauncher.Models.get("delta_e").Dataset.NAttributes(), data.NAttributes());
    }

    @Test
    public void testGenerateAttributes() throws Exception {
        // Get the info of an existing model
        Form dataEntryForm = new Form("entries",
                new JSONObject().put("entries",
                        new JSONArray().put(new JSONObject().put("name", "NaCl"))
                                .put(new JSONObject().put("name", "Al2O3"))).toString());
        Response response = Target.path("model/delta_e/attributes").request().post(Entity.form(dataEntryForm));
        assertEquals(200, response.getStatus());

        // Test the results
        JSONObject output = new JSONObject(response.readEntity(String.class));
        System.out.println(output.toString(2));
        assertTrue(output.has("attributes"));
        assertTrue(Arrays.asList(ServerLauncher.Models.get("delta_e").Dataset.getAttributeNames()).equals(
                output.getJSONArray("attributes").toList()));
        assertEquals("NaCl", output.getJSONArray("entries").getJSONObject(0).getString("name"));
        assertEquals(ServerLauncher.Models.get("delta_e").Dataset.NAttributes(),
                output.getJSONArray("entries").getJSONObject(0).getJSONArray("attributes").length());
    }

    @Test
    public void testRunClassifier() throws Exception {
        // Get the info of an existing model
        Form dataEntryForm = new Form("entries",
                new JSONObject().put("entries",
                        new JSONArray().put(new JSONObject().put("name", "NaCl"))
                                .put(new JSONObject().put("name", "Al2O3"))).toString());
        Response response = Target.path("model/delta_e/run").request().post(Entity.form(dataEntryForm));
        assertEquals(200, response.getStatus());

        // Test the results
        JSONObject output = new JSONObject(response.readEntity(String.class));
        System.out.println(output.toString(2));
        assertTrue(output.has("units"));
        assertEquals(output.getString("units"), ServerLauncher.Models.get("delta_e").getUnits());
        assertEquals("NaCl", output.getJSONArray("entries").getJSONObject(0).getString("name"));
        assertTrue(output.getJSONArray("entries").getJSONObject(0).has("predictedValue"));

        // Get the model status, to see if it tracked the model evaluation time
        response = Target.path("model/delta_e/info").request().get();
        JSONObject info = new JSONObject(response.readEntity(String.class));
        assertEquals("Just a formation enthalpy model", info.get("description"));
        assertEquals(1, info.get("numberTimesRun"));
        System.out.println(info.toString(2));
    }

    @Test
    public void testModelQuery() throws Exception {
        // Get all the models
        Response response = Target.path("models").request().get();
        assertEquals(200, response.getStatus());
        JSONObject info = new JSONObject(response.readEntity(String.class));
        assertEquals(4, info.length());

        // Get only models that are based on a CompositionDataset
        response = Target.path("models").queryParam("datasetType", "materials.CompositionDataset").request().get();
        assertEquals(200, response.getStatus());
        info = new JSONObject(response.readEntity(String.class));
        assertEquals(3, info.length());

        // Make sure it crashes if I give it a bad class name
        response = Target.path("models").queryParam("datasetType", "materials.CompDataset").request().get();
        assertEquals(400, response.getStatus());
        System.out.println(response.readEntity(String.class));

        // Get only models that, at least, support data that supports CompositionDataset.
        response = Target.path("models").queryParam("supportsDatasetType", "materials.CompositionDataset").request().get();
        assertEquals(200, response.getStatus());
        info = new JSONObject(response.readEntity(String.class));
        assertEquals(4, info.length());
    }
}
