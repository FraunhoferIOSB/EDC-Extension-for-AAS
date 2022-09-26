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

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Singleton class.
 * Wrapper for prefix logging using
 * org.eclipse.dataspaceconnector.spi.monitor.Monitor.
 * 
 */
public class Logger {
    private static final String PREFIX_SEPARATOR = " :: ";
    private static Logger instance;
    private String prefix = "";
    private Monitor monitor;

    private Logger() {
    }

    /**
     * Get the instance of this singleton. If no instance is available, one will be
     * created.

     * @return Instance of this class.
     */
    public static Logger getInstance() {
        if (Objects.isNull(instance)) {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Set a new prefix for this logger.

     * @param prefix The new prefix
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Get the prefix of this logger.

     * @return The prefix of this logger
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Only to be called by the extension itself.

     * @param monitor The monitor used to log stuff from
     */
    public void setMonitor(Monitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Log a message with severity "info"

     * @param message Message to be logged
     */
    public void log(String message) {
        monitor.info(prefix + PREFIX_SEPARATOR + message);
    }

    /**
     * Log a message with severity "info"

     * @param message Message to be logged
     */
    public void log(String... message) {
        monitor.info(prefix + PREFIX_SEPARATOR + Stream.of(message).collect(Collectors.joining(" ")));
    }

    /**
     * Log a message with severity "debug"

     * @param message Message to be logged
     */
    public void debug(String message) {
        monitor.debug(prefix + PREFIX_SEPARATOR + message);
    }

    /**
     * Log a message with severity "debug"

     * @param message Message to be logged
     */
    public void warn(String message, Throwable... errors) {
        monitor.warning(prefix + PREFIX_SEPARATOR + message, errors);
    }
    
    /**
     * Log a message with severity "severe"

     * @param message Message to be logged
     */
    public void error(String message, Throwable... errors) {
        monitor.severe(prefix + PREFIX_SEPARATOR + message, errors);
    }
}
