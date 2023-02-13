package org.momtsim.actors;

import com.opencsv.bean.CsvRecurse;
import org.momtsim.MoMTSimState;
import org.momtsim.identity.Identity;
import org.momtsim.identity.MerchantIdentity;

import java.util.Map;

public class Merchant extends SuperActor {
    @CsvRecurse
    private final MerchantIdentity identity;

    public Merchant(MoMTSimState state, MerchantIdentity identity) {
        super(state);
        this.identity = identity;
    }

    public void setHighRisk(boolean isHighRisk) {
        this.identity.setHighRisk(isHighRisk);
    }

    @Override
    public Type getType() {
        return Type.MERCHANT;
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
