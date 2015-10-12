package magpie.utility;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Logan Ward
 */
public class NDGridIteratorTest {

    @Test
    public void test() {
        NDGridIterator iter = new NDGridIterator(1, 1);
        assertArrayEquals(new int[1], iter.next());
        assertFalse(iter.hasNext());
        
        iter = new NDGridIterator(2, 1);
        assertEquals(1, getNIterations(iter));
        iter = new NDGridIterator(2, 2);
        assertEquals(4, getNIterations(iter));
        iter = new NDGridIterator(4, 3);
        assertEquals(81, getNIterations(iter));
        
        iter = new NDGridIterator(new int[]{3,4,5});
        assertEquals(60, getNIterations(iter));
    }
    
    public int getNIterations(NDGridIterator iter){
        int pos = 0;
        while (iter.hasNext()) {
            iter.next();
            pos++;
        }
        return pos++;
    }
}
