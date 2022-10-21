package de.fraunhofer.iosb.app.testUtils;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class FileManager {

    private FileManager() {
    }

    private static File resourcesDirectory = new File("src/test/resources");

    public static String loadResource(String fileName) {
        try (FileInputStream x = new FileInputStream(new File(resourcesDirectory, fileName))) {
            return IOUtils.toString(x, "UTF-8");
        } catch (FileNotFoundException e) {
            fail("File not found exception on file " + fileName);
            return null;
        } catch (IOException e) {
            fail("IO exception on file " + fileName);
            return null;
        }
    }
}
