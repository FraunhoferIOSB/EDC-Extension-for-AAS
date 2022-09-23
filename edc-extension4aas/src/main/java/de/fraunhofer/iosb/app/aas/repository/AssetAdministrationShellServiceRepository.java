package de.fraunhofer.iosb.app.aas.repository;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds all AAS services (e.g., FA³ST, BaSyx)
 */
public class AssetAdministrationShellServiceRepository<T> {
    private Map<URL, T> services;

    public AssetAdministrationShellServiceRepository() {
        services = new HashMap<>();
    }

    public T getService(URL serviceUrl) {
        return services.get(serviceUrl);
    }

    public Collection<T> getAllValues() {
        return services.values();
    }

    public Collection<URL> getAllEntries() {
        return services.keySet();
    }

    public void add(URL serviceUrl, T service) {
        services.put(serviceUrl, service);
    }

    public void remove(URL serviceUrl) {
        services.remove(serviceUrl);
    }

    public void update(URL serviceUrl, T service) {
        if (services.containsKey(serviceUrl)) {
            services.put(serviceUrl, service);
        }
    }

}
