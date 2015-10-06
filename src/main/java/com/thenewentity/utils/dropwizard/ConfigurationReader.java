package com.thenewentity.utils.dropwizard;

/**
 * An interface for reading configuration files given a filename. This interface is primarily here to make it easer to write JUnit
 * tests for MultipleConfigurationProvider, by using mocks to provide configuration contents.
 */
public interface ConfigurationReader {

    /**
     * Read a configuration file. If there are *any* errors reading the file, the method should return an empty string "".
     */
    public String readConfiguration(String path);
}
