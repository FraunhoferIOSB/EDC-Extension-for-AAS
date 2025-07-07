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
package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.model.aas.AasProvider;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;

public class InetTools {

    private InetTools() {
        throw new RuntimeException("Utility class");
    }

    /**
     * <a href="https://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability">...</a>
     *
     * @param host    Host
     * @param port    Port
     * @param timeout Timeout
     * @return True if host is reachable under given port within timeout seconds.
     */
    public static boolean pingHost(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }

    private static boolean checkUrlAvailability(URL toCheck) {
        try {
            // Open basic http connection with "GET" method and check if IOException occurs
            HttpURLConnection connection = (HttpURLConnection) toCheck.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            connection.getResponseCode();
            return true;
        } catch (IOException e) {
            return false;
        }
    }


    public static boolean pingHost(AasProvider provider) {
        var host = provider.getAccessUrl().getHost();
        var port = provider.getAccessUrl().getPort();
        // If no port available, port should be 443 or 80
        if (port == -1) {
            // Iff http:// go with port 80
            port = host.startsWith("http:") ? 80 : 443;
        }

        return pingHost(host, port, 10) || checkUrlAvailability(provider.getAccessUrl());
    }
}
