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
package de.fraunhofer.iosb.ilt.dataspace.client.transfer.exception;

/**
 * Thrown on exceptional behaviour during the transfer process.
 */
public class TransferProcessException extends Exception {
    private final String transferProcessId;

    /**
     * Builds the exception.
     *
     * @param processId id of the process that the initiator timed out on.
     * @param throwable timeout exception causing this exception to fire.
     * @param message exception message.
     */
    public TransferProcessException(String processId, String message, Throwable throwable) {
        super(message, throwable);
        this.transferProcessId = processId;
    }


    /**
     * Builds the exception.
     *
     * @param processId id of the process that the initiator timed out on.
     * @param throwable timeout exception causing this exception to fire.
     */
    public TransferProcessException(String processId, Throwable throwable) {
        super(throwable);
        this.transferProcessId = processId;
    }


    /**
     * Builds the exception.
     *
     * @param processId id of the process that the initiator timed out on.
     * @param message exception message.
     */
    public TransferProcessException(String processId, String message) {
        super(message);
        this.transferProcessId = processId;
    }


    public String getTransferProcessId() {
        return transferProcessId;
    }
}
