package org.momtsim.identity;

import java.util.Map;

public interface Identifiable {
    String getId();

    String getName();

    Identity getIdentity();

    Map<String, Object> getIdentityAsMap();
}
