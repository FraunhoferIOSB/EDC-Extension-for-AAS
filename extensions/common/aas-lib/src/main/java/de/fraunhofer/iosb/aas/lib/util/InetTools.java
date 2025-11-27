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
package de.fraunhofer.iosb.aas.lib.util;

import java.io.IOException;
import java.net.Socket;
import java.util.Locale;


public class InetTools {

    private InetTools() {
        throw new RuntimeException("Utility class");
    }


    /**
     * <a href="https://stackoverflow.com/questions/3584210/preferred-java-way-to-ping-an-http-url-for-availability">...</a>
     *
     * @param host Host
     * @param port Port
     * @return True if host is reachable under given port within timeout seconds.
     */
    public static boolean pingHost(String host, int port) {
        int actualPort;

        if (port != -1) {
            actualPort = port;
        }
        else if (host.toLowerCase(Locale.ROOT).startsWith("https")) {
            actualPort = 443;
        }
        else {
            actualPort = 80;
        }

        try (Socket ignored = new Socket(host, actualPort)) {
            return true;
        }
        catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
}
