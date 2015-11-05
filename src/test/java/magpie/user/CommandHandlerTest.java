package magpie.user;

import java.util.Arrays;
import java.util.List;
import magpie.utility.interfaces.Citation;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CommandHandlerTest {

    @Test
    public void testCitation() throws Exception {
        // Initialize handler
        CommandHandler handler = new CommandHandler();
        
        // Create two variables - a citable WekaRegression and an uncitable expander
        handler.runCommand(Arrays.asList(
                new String[]{"x", "=", "new", "models.regression.WekaRegression", "trees.M5P"}
        ));
        handler.runCommand(Arrays.asList(
                new String[]{"y", "=", "new", "attributes.expansion.CrossExpander"}
        ));
        
        // Check that we indeed have two variables
        assertEquals(2, handler.Workspace.Variables.size());
        
        // Get citations for #1: Should have plenty of information
        List<String> citations = handler.getCitationDescriptions("x");
        assertTrue(citations.size() > 3);
        
        // Get citations for #2: Shouldn't be any
        citations = handler.getCitationDescriptions("y");
        assertTrue(citations.size() == 1);
        
        // Get all citations through Command interface
        handler.runCommand(Arrays.asList(
                new String[]{"citations"}
        ));
        
        // Get only citatiosn for x through Command interface
        handler.runCommand(Arrays.asList(
                new String[]{"citations", "x"}
        ));
    }
    
}
