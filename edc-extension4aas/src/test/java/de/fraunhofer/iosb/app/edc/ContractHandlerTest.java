package de.fraunhofer.iosb.app.edc;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.Logger;

public class ContractHandlerTest {

    private static final String DEFAULT_CONTRACT_NAME = "DEFAULT_CONTRACT";
    private ContractHandler contractHandler;
    private ContractDefinitionStore mockedContractDefinitionStore = mock(ContractDefinitionStore.class);
    private PolicyDefinitionStore mockedPolicyDefinitionStore = mock(PolicyDefinitionStore.class);

    @BeforeAll
    public static void initializeLogger() {
        Logger.getInstance().setMonitor(mock(Monitor.class));
    }

    @BeforeEach
    public void initializeAasAgent() {
        contractHandler = new ContractHandler(mockedContractDefinitionStore, mockedPolicyDefinitionStore);
    }

    @Test
    public void registerAssetToDefaultContractTest() {
        assertEquals(format("%s1", DEFAULT_CONTRACT_NAME),
                contractHandler.registerAssetToDefaultContract("test-asset"));
        verify(mockedContractDefinitionStore, times(1)).save((ContractDefinition) any());
        verify(mockedPolicyDefinitionStore, times(2)).save((PolicyDefinition) any());
    }

}
