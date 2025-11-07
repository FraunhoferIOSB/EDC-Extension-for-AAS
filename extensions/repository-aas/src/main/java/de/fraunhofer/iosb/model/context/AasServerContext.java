package de.fraunhofer.iosb.model.context;

import java.net.URI;

public abstract class AasServerContext {

    private final URI uri;

    protected AasServerContext(URI uri) {
        this.uri = uri;
    }

    /**
     * Get the full URI to access this AAS repository, including
     *
     * @return The full accessor URI for this repository, e.g., <a href="">https://my-aas:1337/api/v3.0</a>.
     */
    public URI getUri() {
        return uri;
    }
}
