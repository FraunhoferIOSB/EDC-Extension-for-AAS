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
package de.fraunhofer.iosb.app.testutils;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.fail;

public class FileManager {

    private static final File RESOURCES_DIRECTORY = new File("src/test/resources");

    private FileManager() {
    }

    public static String loadResource(String fileName) {
        try (FileInputStream x = new FileInputStream(new File(RESOURCES_DIRECTORY, fileName))) {
            return IOUtils.toString(x, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            fail("File not found exception on file " + fileName);
            return null;
        } catch (IOException e) {
            fail("IO exception on file " + fileName);
            return null;
        }
    }
}
