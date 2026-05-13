package de.fraunhofer.iosb.aas.test;

import org.eclipse.edc.boot.vault.InMemoryVault;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.result.ServiceResult;


public class DefaultVault extends InMemoryVault {

    public DefaultVault() {
        super(new ConsoleMonitor(),
                () -> ServiceResult.success(ParticipantContext.Builder.newInstance()
                        .participantContextId("did:web:mock:participant")
                        .identity("did:web:mock:participant")
                        .build()));
    }
}
