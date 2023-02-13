package org.momtsim.actors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.momtsim.IteratingMoMTSim;
import org.momtsim.parameters.Parameters;

public class ClientTest {

    Parameters parameters;

    @BeforeEach
    void setup() {
        parameters = new Parameters("MoMTSim.properties");
    }

    @Test
    void testPickingRandomMerchants() {
        IteratingMoMTSim sim = new IteratingMoMTSim(parameters, 100);

    }
}
