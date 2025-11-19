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
package de.fraunhofer.iosb.dataplane.aas.pipeline;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;


/**
 * Inspired by EDCs NonChunkedTransferRequestBody.
 */
public class AasTransferRequestBody extends RequestBody {

    private final byte[] bytes;
    private final String contentType;


    public AasTransferRequestBody(byte[] bytes, String contentType) {
        this.contentType = contentType;
        this.bytes = bytes;
    }


    @Override
    public long contentLength() {
        return bytes == null ? 0 : bytes.length;
    }


    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        if (bytes == null) {
            return;
        }

        try (var os = sink.outputStream()) {
            os.write(bytes);
        }
    }


    @Nullable
    @Override
    public MediaType contentType() {
        return MediaType.parse(contentType);
    }
}

