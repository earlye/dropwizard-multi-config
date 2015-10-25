package com.thenewentity.utils.dropwizard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DefaultConfigurationReader implements ConfigurationReader {

    @Override
    public String readConfiguration(String path) {
        try {
            Path filePath = Paths.get(path.replaceFirst("^~" + File.separator, System.getProperty("user.home") + File.separator));
            byte[] encoded = Files.readAllBytes(filePath);
            return new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

}
