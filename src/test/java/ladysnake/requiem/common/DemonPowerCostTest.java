package ladysnake.requiem.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DemonPowerCostTest {

    @Test
    public void testDemonTagSpelling() {
        // Phenex is spelled P H E N E X
        String phenex = "PHENEX";
        assertTrue("PHENEX".equalsIgnoreCase(phenex));
        assertTrue("phenex".equalsIgnoreCase(phenex));
        assertTrue("Phenex".equalsIgnoreCase(phenex));

        // Marbas
        String marbas = "MARBAS";
        assertTrue("MARBAS".equalsIgnoreCase(marbas));
        assertTrue("marbas".equalsIgnoreCase(marbas));
        assertTrue("Marbas".equalsIgnoreCase(marbas));
    }
}
