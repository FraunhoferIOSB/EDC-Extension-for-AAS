package de.fraunhofer.iosb.app.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DataTransferObservable {

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
    
    public void update(String agreementId, String data){
        observers.get(agreementId).complete(data);
    }
}
