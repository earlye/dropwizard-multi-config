package com.thenewentity.utils.dropwizard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

public class MultipleConfigurationProviderTest {

    ConfigurationReader reader;
    MultipleConfigurationProvider provider;

    String inputStreamToString(InputStream stream) throws Exception {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer);
        return writer.toString();
    }

    @Before
    public void beforeTest() {
        reader = mock(ConfigurationReader.class);

        when(reader.readConfiguration(eq("main.yaml"))).thenReturn(StringUtils.join(new String[] {// @formatter:off
                "template: test", 
                "server:", 
                "  applicationConnectors:", 
                "    - type: http", 
                "      port: 5309", 
        }, "\n")); // @formatter:on
    }

    @Test
    public void testNullOverrides() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\n", effectiveYaml);
    }

    @Test
    public void testOverrideEmpty() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\n", effectiveYaml);
    }

    @Test
    public void testOverrideSingleTopLevelValue() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(Arrays.asList("override1.yaml"))
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("template: test2");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test2\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\n", effectiveYaml);
    }

    @Test
    public void testOverrideSingleNestedValue() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(Arrays.asList("override1.yaml"))
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("server:\n  applicationConnectors:\n  - {port: 5310}\n");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5310}\n", effectiveYaml);
    }

    @Test
    public void testAttemptOverrideSingleNestedValueByPath() throws Exception {
        // This may not be what some people would expect, given DropWizard's support for System variables of the form
        // -Ddw.yaml.path=value, but if you think about how yaml parsing works, this IS correct.
        // We're merging .YAML files, not re-implementing DropWizard's -Ddw.* support.
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(Arrays.asList("override1.yaml"))
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("server.applicationConnectors[0].port: 5310\n");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\nserver.applicationConnectors[0].port: 5310\n", effectiveYaml);
    }

    @Test
    public void testAddNestedValue() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(Arrays.asList("override1.yaml"))
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("server:\n  applicationConnectors:\n  -\n  - {port: 5310}\n");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\n  - {port: 5310}\n", effectiveYaml);
    }

    @Test
    public void testAddTopLevelValue() throws Exception {
        // @formatter:off
        provider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(Arrays.asList("override1.yaml"))
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setConfigurationReader(reader).build())
                .build();
        // @formatter:on

        when(reader.readConfiguration(eq("override1.yaml"))).thenReturn("template2: test\n");

        String effectiveYaml = inputStreamToString(provider.open("main.yaml"));
        assertNotNull(effectiveYaml);
        assertEquals("template: test\nserver:\n  applicationConnectors:\n  - {type: http, port: 5309}\ntemplate2: test\n", effectiveYaml);
    }
}
