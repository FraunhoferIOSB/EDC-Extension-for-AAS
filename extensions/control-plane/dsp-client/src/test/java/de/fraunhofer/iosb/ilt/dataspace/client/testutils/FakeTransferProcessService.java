/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.ilt.dataspace.client.testutils;

import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcessStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyPreparedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.NotifyStartedCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.ResumeTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.SuspendTransferCommand;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.command.TerminateTransferCommand;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Stateful in-process fake for {@link TransferProcessService}. Captures the {@link TransferRequest} submitted by the
 * production {@code PullTransferInitiator} and returns a successful result carrying a well-formed real
 * {@link TransferProcess} that holds the EDR {@link DataAddress} arranged by the test ({@link #respondWithEdr}).
 * The tests then drive the real {@code TransferProcessObservable} to move the registered transfer into the desired
 * state.
 */
public class FakeTransferProcessService implements TransferProcessService {

    private final AtomicReference<TransferRequest> capturedRequest = new AtomicReference<>();
    private final AtomicReference<TransferProcess> lastTransferProcess = new AtomicReference<>();
    private final AtomicInteger callCount = new AtomicInteger();
    private final AtomicReference<String> assetId = new AtomicReference<>();
    private final AtomicReference<DataAddress> edrDataAddress = new AtomicReference<>();

    /**
     * Arranges the EDR data address the faked transfer process will carry, and the asset id reported on it.
     */
    public void respondWithEdr(String assetId, DataAddress dataAddress) {
        this.assetId.set(assetId);
        this.edrDataAddress.set(dataAddress);
    }


    /**
     * Clears all recorded state and the arranged EDR (the negotiate flow stops calling the fake when a test forgets
     * to arrange an EDR, so tests must call {@link #respondWithEdr} again after this reset).
     */
    public void reset() {
        capturedRequest.set(null);
        lastTransferProcess.set(null);
        callCount.set(0);
        assetId.set(null);
        edrDataAddress.set(null);
    }


    public TransferRequest capturedRequest() {
        return capturedRequest.get();
    }


    public TransferProcess lastTransferProcess() {
        return lastTransferProcess.get();
    }


    /**
     * Returns the started data the tests have to pass to {@code TransferProcessObservable} for the fake process: the
     * real {@code TransferProcessEventListener} dereferences it unconditionally.
     */
    public TransferProcessStartedData lastTransferStartedData() {
        var dataAddress = Objects.requireNonNull(edrDataAddress.get(), "no EDR arranged; call respondWithEdr first");
        return TransferProcessStartedData.Builder.newInstance().dataAddress(dataAddress).build();
    }


    public int callCount() {
        return callCount.get();
    }


    @Override
    public @NotNull ServiceResult<TransferProcess> initiateTransfer(ParticipantContext participantContext, TransferRequest request) {
        capturedRequest.set(request);
        callCount.incrementAndGet();
        var dataAddress = Objects.requireNonNull(edrDataAddress.get(), "no EDR arranged; call respondWithEdr first");
        var process = TransferProcess.Builder.newInstance()
                .id("transfer-process-%s-%s".formatted(callCount.get(), participantContext.getParticipantContextId()))
                .type(TransferProcess.Type.CONSUMER)
                .state(TransferProcessStates.STARTED.code())
                .protocol(request.getProtocol())
                .counterPartyAddress(request.getCounterPartyAddress())
                .contractId(request.getContractId())
                .transferType(request.getTransferType())
                .assetId(Objects.requireNonNull(assetId.get(), "no asset id arranged; call respondWithEdr first"))
                .participantContextId(participantContext.getParticipantContextId())
                .contentDataAddress(dataAddress)
                .build();
        lastTransferProcess.set(process);
        return ServiceResult.success(process);
    }


    @Override
    public TransferProcess findById(String transferProcessId) {
        throw new UnsupportedOperationException("findById is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<List<TransferProcess>> search(QuerySpec query) {
        throw new UnsupportedOperationException("search is not part of the dsp-client negotiate flow");
    }


    @Override
    public String getState(String transferProcessId) {
        throw new UnsupportedOperationException("getState is not part of the dsp-client negotiate flow");
    }


    @Override
    public @NotNull ServiceResult<Void> complete(String transferProcessId) {
        throw new UnsupportedOperationException("complete is not part of the dsp-client negotiate flow");
    }


    @Override
    public @NotNull ServiceResult<Void> terminate(TerminateTransferCommand command) {
        throw new UnsupportedOperationException("terminate is not part of the dsp-client negotiate flow");
    }


    @Override
    public @NotNull ServiceResult<Void> suspend(SuspendTransferCommand command) {
        throw new UnsupportedOperationException("suspend is not part of the dsp-client negotiate flow");
    }


    @Override
    public @NotNull ServiceResult<Void> resume(ResumeTransferCommand command) {
        throw new UnsupportedOperationException("resume is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<Void> notifyPrepared(NotifyPreparedCommand command) {
        throw new UnsupportedOperationException("notifyPrepared is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<Void> notifyStarted(NotifyStartedCommand command) {
        throw new UnsupportedOperationException("notifyStarted is not part of the dsp-client negotiate flow");
    }
}
