package org.momtsim.actors;

import org.momtsim.MoMTSimState;
import org.momtsim.identity.BankIdentity;
import org.momtsim.identity.Identity;

import java.util.Map;

public class Bank extends SuperActor {

    private final BankIdentity identity;

    public Bank(MoMTSimState state, BankIdentity identity) {
        super(state);
        this.identity = identity;
    }

    @Override
    public Type getType() {
        return Type.BANK;
    }

    @Override
    public String getId() {
        return identity.id;
    }

    @Override
    public String getName() {
        return identity.name;
    }

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public Map<String, Object> getIdentityAsMap() {
        return identity.asMap();
    }
}
