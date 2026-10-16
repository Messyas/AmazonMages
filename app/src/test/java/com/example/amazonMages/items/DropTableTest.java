package com.example.amazonMages.items;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
public class DropTableTest {
    @Test public void configuredProbabilitiesAndQuantitiesAreRespected() {
        DropTable table = new DropTable(new DropTable.Entry("erva",1,2,2),new DropTable.Entry("mana",0,1,1));
        int[] count={0};
        table.roll(new Random(1),(item,n)->{ assertEquals("erva",item); assertEquals(2,n); count[0]++; });
        assertEquals(1,count[0]);
    }
    @Test(expected=IllegalArgumentException.class) public void invalidDropIsRejected() {
        new DropTable.Entry("erva",Double.NaN,1,2);
    }
}
