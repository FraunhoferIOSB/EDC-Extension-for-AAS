package org.eclipse.dataspaceconnector.extensions.idsaasextension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.Endpoint;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.RequestType;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.Requires;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Extension providing/connecting EDC logic to the EDC-AAS-Application.
 */
@Requires({ WebService.class })
public class IdsAasExtension implements ServiceExtension {

    @Inject
    private ContractDefinitionStore contractStore;
    @Inject
    private AssetLoader assetLoader;
    @Inject
    private PolicyDefinitionStore policyStore;
    @Inject
    private OkHttpClient okHttpClient;

    private static final String SETTINGS_PREFIX = "edc.idsaasapp.";
    private final Logger logger = Logger.getInstance();

    private Endpoint endpoint;
    private AasController aasController;

    private final ScheduledThreadPoolExecutor syncExecutor = new ScheduledThreadPoolExecutor(1);

    @Override
    public void initialize(ServiceExtensionContext context) {
        logger.setMonitor(context.getMonitor());

        // Distribute controllers, repositories
        final var selfDescriptionRepository = new SelfDescriptionRepository();
        final var resourceController = new ResourceController(assetLoader, okHttpClient, contractStore, policyStore);
        aasController = new AasController(okHttpClient);

        endpoint = new Endpoint(selfDescriptionRepository, aasController, resourceController);

        loadConfig(context);
        var configInstance = Configuration.getInstance();

        // Remote AAS service URL supplied?
        if (Objects.nonNull(configInstance.getRemoteAasLocation())) {
            endpoint.postAasService(configInstance.getRemoteAasLocation());
        }

        // AAS model supplied?
        if (Objects.nonNull(configInstance.getLocalAasModelPath())) {
            endpoint.postAasEnvironment(configInstance.getLocalAasModelPath(), configInstance.getAasServiceConfigPath(),
                    configInstance.getLocalAasServicePort());
        }

        syncExecutor.scheduleAtFixedRate(
                () -> selfDescriptionRepository.getAll() // Task: get all AAS service URLs, synchronize EDC and AAS
                        .forEach((url, selfDescription) -> endpoint.syncAasWithEdc(url)),
                configInstance.getSyncPeriod(),
                configInstance.getSyncPeriod(), TimeUnit.SECONDS);

        context.getService(WebService.class).registerResource(endpoint);
    }

    /**
     * Get extension specific configuration from EDC config object

     * @param context EDC config reference provider
     */
    private void loadConfig(ServiceExtensionContext context) {
        final var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Config config = context.getConfig();

        logger.setPrefix(config.getString(SETTINGS_PREFIX + "logPrefix", "IDS AAS Extension"));

        String configAsString;
        try {
            configAsString = objectMapper.writeValueAsString(config.getRelativeEntries(SETTINGS_PREFIX));
        } catch (JsonProcessingException e) {
            // This should not be reached, unless there is an error inside EDC's Config.java
            logger.error("Could not load IDS AAS extension configuration, using default values", e);
            configAsString = "";
        }
        // Currently, only one configuration at a time is supported
        new ConfigurationController().handleRequest(RequestType.PUT, null, configAsString);
    }

    @Override
    public void shutdown() {
        syncExecutor.shutdown();
        aasController.stopServices();
    }
}
