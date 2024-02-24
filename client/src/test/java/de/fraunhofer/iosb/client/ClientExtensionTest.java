package de.fraunhofer.iosb.client;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(DependencyInjectionExtension.class)
public class ClientExtensionTest {

    private ClientExtension clientExtension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(CatalogService.class, mock(CatalogService.class));
        context.registerService(ConsumerContractNegotiationManager.class,
                mock(ConsumerContractNegotiationManager.class));
        context.registerService(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        context.registerService(ContractNegotiationObservable.class, mock(ContractNegotiationObservable.class));
        context.registerService(TransferProcessManager.class, mock(TransferProcessManager.class));
        context.registerService(WebService.class, mock(WebService.class));
        context.registerService(Monitor.class, mock(Monitor.class));

        this.context = spy(context);
        clientExtension = factory.constructInstance(ClientExtension.class);
    }

    @Test
    public void initializeTest() {
        // See if initializing the extension works
        clientExtension.initialize(this.context);
    }

}
