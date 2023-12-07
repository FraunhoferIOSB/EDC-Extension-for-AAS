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
package de.fraunhofer.iosb.app;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;

/**
 * Singleton class.
 * Wrapper for logging with prefix
 * ({@link org.eclipse.edc.spi.monitor.ConsoleMonitor}).
 */
public class Logger extends ConsoleMonitor {
    private static Logger instance;

    private Logger() {
    }

    /**
     * Get the instance of this singleton. If no instance is available, one will be
     * created.
     *
     * @return Instance of this class.
     */
    public static Logger getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Logger();
        }
        return instance;
    }

    @Override
    public String sanitizeMessage(Supplier<String> supplier) {
        return "EDC4AAS: " + super.sanitizeMessage(supplier);
    }
}
