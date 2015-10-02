package com.thenewentity.utils.dropwizard;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

public class MungedCommandLineTest {

    public void testMunging(String[] expectedMungedArguments, Collection<String> expectedOverrides, String[] inputArguments, String[] defaultArguments) {
        // @formatter:off
        MungedCommandLine result = MungedCommandLine.builder()
            .setOriginalArguments(inputArguments)
            .setDefaultArguments(defaultArguments)
            .build();
        // @formatter:on

        assertNotNull("result", result);
        assertArrayEquals("munged arguments are correct.", expectedMungedArguments, result.getMungedArguments());
        assertEquals("overrides are correct.", expectedOverrides, result.getExtraArguments());
        assertArrayEquals("original arguments are preserved.", inputArguments, result.getOriginalArguments());
        assertArrayEquals("default arguments are preserved.", defaultArguments, result.getDefaultArguments());
    }

    @Test
    public void testMunging() {
        // Make sure that a conventional command line works.
        testMunging(new String[] { "server", "test.yaml" }, null, new String[] { "server", "test.yaml" }, null);

        // Make sure that a command line with the '--' works when there is only a single argument afterwards.
        testMunging(new String[] { "server", "--", "test.yaml" }, null, new String[] { "server", "--", "test.yaml" }, null);

        // Make sure that a command line with the '--' and two succeeding arguments works.
        testMunging(new String[] { "server", "--", "test.yaml" }, Arrays.asList("override.yaml"), new String[] { "server", "--", "test.yaml", "override.yaml" }, null);

        // Make sure that a command line with the '--' and three succeeding arguments works.
        testMunging(new String[] { "server", "--", "test.yaml" }, Arrays.asList("override.yaml", "override2.yaml"), new String[] { "server", "--", "test.yaml", "override.yaml",
                "override2.yaml" }, null);

        // Make sure that a command line with multiple arguments preceding the '--', and three succeeding arguments works.
        testMunging(new String[] { "special", "--properties", "prop1", "prop2", "--", "test.yaml" }, Arrays.asList("override.yaml", "override2.yaml"), new String[] { "special",
                "--properties", "prop1", "prop2", "--", "test.yaml", "override.yaml", "override2.yaml" }, null);

        // Make sure that a command line with multiple '--' works.
        testMunging(new String[] { "special", "--", "prop1", "prop2", "--", "test.yaml" }, Arrays.asList("override.yaml", "override2.yaml"), new String[] { "special", "--",
                "prop1", "prop2", "--", "test.yaml", "override.yaml", "override2.yaml" }, null);

        // Make sure that a command line with no arguments succeeding the '--' works.
        testMunging(new String[] { "server", "--" }, null, new String[] { "server", "--" }, null);

        // Make sure that a command line with one argument works.
        testMunging(new String[] { "server" }, null, new String[] { "server" }, null);

        // Make sure that a command line with no arguments works.
        testMunging(new String[] {}, null, new String[] {}, null);

        // Make sure that a command line with no arguments, but with defaults, works.
        testMunging(new String[] { "server" }, null, new String[] {}, new String[] { "server" });
    }

}
