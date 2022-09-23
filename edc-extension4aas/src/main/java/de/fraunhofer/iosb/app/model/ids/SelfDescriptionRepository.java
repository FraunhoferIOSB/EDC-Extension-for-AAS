package de.fraunhofer.iosb.app.model.ids;

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository holding selfDescriptions. The ID of a self description is the
 * URL of the AAS instance holding the AAS model that is represented by
 * the self description.
 */
public class SelfDescriptionRepository {

    private Map<URL, SelfDescription> selfDescriptions;

    public SelfDescriptionRepository() {
        selfDescriptions = new HashMap<>();
    }

    /**
     * Get a self description by its underlying AAS service's URL.

     * @param aasServiceUrl The underlying AAS service URL of the self description
     * @return The matching self description, or null if this repository contains no
     *         mapping for the URL
     */
    public SelfDescription get(URL aasServiceUrl) {
        return selfDescriptions.get(aasServiceUrl);
    }

    /**
     * Get all entries of this repository
     */
    public Map<URL, SelfDescription> getAll() {
        return selfDescriptions;
    }
    
    /**
     * Add a new self description to the repository

     * @param aasServiceUrl   URL of the underlying AAS service of this self
     *                        description's AAS model.
     * @param selfDescription The self description to be added
     */
    public void add(URL aasServiceUrl, SelfDescription selfDescription) {
        selfDescriptions.put(aasServiceUrl, selfDescription);
    }

    /**
     * Update an existing self description

     * @param aasServiceUrl   url of the self description to be updated
     * @param selfDescription new self description
     */
    public void update(URL aasServiceUrl, SelfDescription selfDescription) {
        if (selfDescriptions.containsKey(aasServiceUrl)) {
            selfDescriptions.put(aasServiceUrl, selfDescription);
        } else {
            throw new EdcException("Self description with id " + aasServiceUrl + "does not exist");
        }
    }

    /**
     * Remove a self description by its underlying AAS service's URL

     * @param aasServiceUrl The underlying URL
     */
    public SelfDescription remove(URL aasServiceUrl) {
        return selfDescriptions.remove(aasServiceUrl);
    }
}
