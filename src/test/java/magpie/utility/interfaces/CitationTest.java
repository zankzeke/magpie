package magpie.utility.interfaces;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class CitationTest {

    @Test
    public void testCompleteInfo() {
        // Create article
        Citation c = new Citation(this.getClass(), 
                "Article",
                new String[]{"L. Ward", "C. Wolverton"},
                "A test article",
                "http://google.com/",
                "Some notes");
        
        // Test toString
        String res = c.toString();
        System.out.println(res);
        assertTrue(res.contains(this.getClass().getSimpleName()));
        assertTrue(res.contains("Article"));
        assertTrue(res.contains("google.com"));
        
        // Test toString
        res = c.printInformation();
        System.out.println();
        System.out.println(res);
        assertTrue(res.contains(this.getClass().getSimpleName()));
        assertTrue(res.contains("Article"));
        assertTrue(res.contains("L. Ward"));
        assertTrue(res.contains("C. Wolverton"));
        assertTrue(res.contains("A test article"));
        assertTrue(res.contains("google.com"));
        assertTrue(res.contains("Some notes"));
    }
    
    @Test
    public void testMinimalInfo() {
        // Create article
        Citation c = new Citation(this.getClass(), 
                "Article, unpublished",
                null,
                "An incomplete paper",
                null,
                null);
        
        // Test toString
        String res = c.toString();
        System.out.println(res);
        assertTrue(res.contains(this.getClass().getSimpleName()));
        assertTrue(res.contains("Article, unpublished"));
        
        // Test toString
        res = c.printInformation();
        System.out.println();
        System.out.println(res);
        assertTrue(res.contains(this.getClass().getSimpleName()));
        assertTrue(res.contains("Article, unpublished"));
    }
    
}
