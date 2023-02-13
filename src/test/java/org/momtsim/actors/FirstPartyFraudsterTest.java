package org.momtsim.actors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.momtsim.IteratingMoMTSim;
import org.momtsim.identity.ClientIdentity;
import org.momtsim.parameters.Parameters;

import java.util.HashSet;

public class FirstPartyFraudsterTest {
    private Parameters parameters;

    @BeforeEach
    void setup() {
        parameters = new Parameters("MoMTSim.properties");
    }

    @Test
    void allClientsShouldBeUnique() throws Exception {
        // XXX: setup for this test sucks, run sim with large queue, drain sim. Should use a testing momtsim version.
        IteratingMoMTSim sim = new IteratingMoMTSim(parameters, 100000);
        sim.run();
        sim.forEachRemaining(t -> {
        });

        ClientIdentity fpfIdentity = sim.generateIdentity();
        FirstPartyFraudster fpf = new FirstPartyFraudster(sim, fpfIdentity, 2);

        // Run 9 times...should generate collision guaranteed if bad logic is in place
        for (int i = 0; i < 9; i++) {
            fpf.commitFraud(sim);
        }

        Assertions.assertEquals(9, fpf.fauxAccounts.size());
        final HashSet<String> fakeIds = new HashSet<>();
        fpf.fauxAccounts.forEach(acct -> fakeIds.add(acct.getId()));
        Assertions.assertEquals(9, fakeIds.size());
    }
}
