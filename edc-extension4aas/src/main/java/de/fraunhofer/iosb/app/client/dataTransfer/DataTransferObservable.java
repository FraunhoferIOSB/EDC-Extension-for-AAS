package de.fraunhofer.iosb.app.client.dataTransfer;

import static java.lang.String.format;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import de.fraunhofer.iosb.app.Logger;

public class DataTransferObservable {

    private static final Logger LOGGER = Logger.getInstance();

    private final Map<String, CompletableFuture<String>> observers;

    public DataTransferObservable() {
        observers = new ConcurrentHashMap<>();
    }

    public void register(CompletableFuture<String> observer, String agreementId) {
        observers.put(agreementId, observer);
    }

    public void unregister(String agreementId) {
        observers.remove(agreementId);
    }

    public void update(String agreementId, String data) {
        if (!observers.containsKey(agreementId)) {
            LOGGER.warn(format(
                    "A POST request to the client's data transfer endpoint with an unknown agreementID was caught. AgreementID: %s",
                    agreementId));
            return;
        }
        observers.get(agreementId).complete(data);
    }
}
