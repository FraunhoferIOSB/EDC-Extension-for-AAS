package de.fraunhofer.iosb.app.client.contract;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

public class ContractOfferStore {
    private final List<ContractOffer> offers;

    public ContractOfferStore() {
        this.offers = new ArrayList<>();
    }

    public List<ContractOffer> getOffers() {
        return offers;
    }

    public void putOffer(ContractOffer offer) {
        offers.add(offer);
    }
}
