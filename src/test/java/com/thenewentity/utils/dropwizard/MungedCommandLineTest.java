package com.thenewentity.utils.dropwizard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

public class MungedCommandLineTest {

    public void testMunging(String[] input, String[] expected, Collection<String> overrides) {
        // @formatter:off
        MungedCommandLine result = MungedCommandLine.builder()
            .setOriginalArguments(input)
            .build();
        // @formatter:on

        assertNotNull("result", result);
        assertArrayEquals("munged arguments are correct.", expected, result.getMungedArguments());
        assertEquals("overrides are correct.", overrides, result.getYamlOverrides());
    }

    @Test
    public void testMunging() {
        testMunging(new String[] { "server", "test.yaml" }, new String[] { "server", "test.yaml" }, null);

        testMunging(new String[] { "server", "--", "test.yaml" }, new String[] { "server", "--", "test.yaml" }, null);

        testMunging(new String[] { "server", "--", "test.yaml", "override.yaml" }, new String[] { "server", "--", "test.yaml" }, Arrays.asList("override.yaml"));

        testMunging(new String[] { "server", "--", "test.yaml", "override.yaml", "override2.yaml" }, new String[] { "server", "--", "test.yaml" },
                Arrays.asList("override.yaml", "override2.yaml"));

        testMunging(new String[] { "special", "--properties", "prop1", "prop2", "--", "test.yaml", "override.yaml", "override2.yaml" }, new String[] { "special", "--properties",
                "prop1", "prop2", "--", "test.yaml" }, Arrays.asList("override.yaml", "override2.yaml"));

        testMunging(new String[] { "server", "--" }, new String[] { "server", "--" }, null);

        testMunging(new String[] { "server" }, new String[] { "server" }, null);
    }

}
