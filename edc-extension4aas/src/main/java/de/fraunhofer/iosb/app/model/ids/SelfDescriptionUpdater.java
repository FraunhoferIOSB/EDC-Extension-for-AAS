package de.fraunhofer.iosb.app.model.ids;

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO correct package?
 */
public class SelfDescriptionUpdater extends PipelineStep<Map<String, Asset>, Map<Asset, Asset>> {

    private final SelfDescriptionRepository selfDescriptionRepository;

    public SelfDescriptionUpdater(SelfDescriptionRepository selfDescriptionRepository) {
        this.selfDescriptionRepository = selfDescriptionRepository;
    }

    /**
     * Checks new Assets into self-description repository
     *
     * @param registered URL and asset for each registered AAS service
     * @return ID of old asset and new asset for synchronizer to create changeSet
     */
    @Override
    public Map<Asset, Asset> execute(Map<String, Asset> registered) throws Exception {
        Map<Asset, Asset> result = new HashMap<>();

        for (var entry : registered.entrySet()) {
            result.put(selfDescriptionRepository.getSelfDescriptionAsset(entry.getKey()), entry.getValue());
            selfDescriptionRepository.updateSelfDescription(entry.getKey(), entry.getValue());
        }

        return result;
    }
}
