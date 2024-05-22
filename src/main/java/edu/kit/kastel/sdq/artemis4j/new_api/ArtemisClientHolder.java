package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;

public abstract class ArtemisClientHolder {
    private final ArtemisClient client;

    public ArtemisClientHolder(ArtemisClient client) {
        this.client = client;
    }

    public ArtemisClientHolder(ArtemisClientHolder clientHolder) {
        this.client = clientHolder.getClient();
    }

    public ArtemisClient getClient() {
        return client;
    }
}
