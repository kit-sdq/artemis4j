/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Represents an identifier to uniquely identify an artemis user.
 */
public final class UserIdentifier {
    private final String login;

    // This constructor and the login are non-public, to prevent
    // artemis4j users from depending on this information.
    //
    // This should reduce breakage in case the identifier has to
    // be changed again in a future version.
    UserIdentifier(String login) {
        this.login = Objects.requireNonNull(login);
    }

    String login() {
        return this.login;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        }

        if (other == null || other.getClass() != this.getClass()) {
            return false;
        }

        return ((UserIdentifier) other).login().equals(this.login());
    }

    @Override
    public int hashCode() {
        return this.login().hashCode();
    }

    @Override
    public String toString() {
        return this.login();
    }
}
