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

public class OSUtil {

    public static final String LOCALHOST_IP = "127.0.0.1";
    public static final String LOCALHOST = "localhost";
    private static final String OS_NAME = "os.name";
    private static final String WINDOWS = "Windows";

    private OSUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static String getLocalhostAddress() {
        return System.getProperty(OS_NAME).startsWith(WINDOWS) ? LOCALHOST_IP : LOCALHOST;
    }
}

