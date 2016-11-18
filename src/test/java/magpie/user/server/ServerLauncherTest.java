package magpie.user.server;

import java.io.*;
import java.util.*;

import magpie.data.materials.CompositionDataset;
import magpie.data.utilities.modifiers.NonZeroClassModifier;
import magpie.models.BaseModel;
import magpie.models.classification.WekaClassifier;
import magpie.models.regression.GuessMeanRegression;
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
        template.setDataDirectory("./lookup-data");
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

}
