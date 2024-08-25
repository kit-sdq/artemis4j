/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

public abstract class ArtemisConnectionHolder {
    private final ArtemisConnection connection;

    protected ArtemisConnectionHolder(ArtemisConnection connection) {
        this.connection = connection;
    }

    protected ArtemisConnectionHolder(ArtemisConnectionHolder connectionHolder) {
        this.connection = connectionHolder.getConnection();
    }

    public ArtemisConnection getConnection() {
        return connection;
    }
}
