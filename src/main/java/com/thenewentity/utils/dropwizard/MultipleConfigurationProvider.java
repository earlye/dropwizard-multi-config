package com.thenewentity.utils.dropwizard;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.yaml.snakeyaml.Yaml;

/**
 * <p>
 * An implementation of ConfigurationSourceProvider which knows how to merge multiple .yaml files so that you can have one
 * "master" configuration with environment- and user- specific overrides.
 * </p>
 * 
 * <p>
 * In your DropWizard application class's initialize(Bootstrap bootstrap) method, pass a MultipleConfigurationProvider to
 * bootstrap.setConfigurationSourceProvider()
 * </p>
 * 
 * <p>
 * MultipleConfigurationProvider relies on {@link MultipleConfigurationMerger} to actually parse and merge the yaml files.
 * </p>
 * 
 */
public class MultipleConfigurationProvider implements ConfigurationSourceProvider {

    private final Collection<String> overrideFiles;
    private String effectiveConfig;
    private MultipleConfigurationMerger multipleConfigurationMerger;
    private final Yaml yaml = new Yaml();
    private static Set<Character> globChars = buildGlobChars();

    /**
     * Constructor
     * 
     * @param overrideFiles
     *            - a list of filenames to merge into the yaml specified in the {@code path} provided to {@link #open(String)}
     */
    public MultipleConfigurationProvider(Collection<String> overrideFiles) {
        this(overrideFiles, new DefaultConfigurationReader());
    }

    /**
     * Constructor
     * 
     * @param overrideFiles
     *            - list of yamls to merge into the yaml specified in the {@link #open(String)} method.
     * @param configurationMerger
     *            - the object which actually merges the yamls.
     */
    public MultipleConfigurationProvider(Collection<String> overrideFiles, MultipleConfigurationMerger configurationMerger) {
        this.overrideFiles = overrideFiles;
        this.multipleConfigurationMerger = configurationMerger;
    }

    /**
     * Constructor
     * 
     * @param overrideFiles
     *            - a list of filenames to merge into the yaml specified in the {@code path} provided to {@link #open(String)}.
     * @param configurationReader
     *            - is the object which actually reads the yamls.
     */
    public MultipleConfigurationProvider(Collection<String> overrideFiles, ConfigurationReader configurationReader) {
        this(overrideFiles, new MultipleConfigurationMerger(configurationReader));
    }

    /**
     * <p>
     * Called by DropWizard during application startup; this method is where the merging of multiple configuration files happens.
     * </p>
     * 
     * <p>
     * Read the specified yaml, then merge any {@link #overrideFiles} specified in the
     * {@link #MultipleConfigurationProvider(Collection) constructor} on top of it. Then, dump that out as yaml into
     * {@link #effectiveConfig}, and return an InputStream to DropWizard.
     * </p>
     * 
     * <p>
     * <b>Side Effects</b>
     * <dd>As discussed in the description, changes {@link #effectiveConfig}.</dd>
     * </p>
     * 
     * @param path
     *            - the path provided by DropWizard
     * 
     */
    @Override
    public InputStream open(String path) throws IOException {
        List<String> paths = new ArrayList<String>();
        paths.addAll(globPath(path));
        if (overrideFiles != null) {
            for (String entry : overrideFiles) {
                paths.addAll(globPath(entry));
            }
        }
        Map<Object, Object> merged = multipleConfigurationMerger.mergeConfigs(paths);

        effectiveConfig = yaml.dump(merged);
        InputStream result = new ByteArrayInputStream(effectiveConfig.getBytes(StandardCharsets.UTF_8));
        return result;
    }

    /**
     * Getter; provides a String containing the effective configuration, in .yaml format.
     */
    public String getEffectiveConfig() {
        return effectiveConfig;
    }

    /**
     * Build a set of characters containing all of the glob characters recognized by {@link FileSystem#getPathMatcher}
     * 
     * @return
     */
    private static Set<Character> buildGlobChars() {
        Set<Character> result = new HashSet<>();
        result.addAll(Arrays.asList('[', ']', '{', '}', '*', '?', '\\'));
        return result;
    }

    /**
     * Finds the last occurrence of File.separatorChar prior to the first occurrence of glob pattern characters. If there are no
     * glob pattern characters, returns -1.
     * 
     * @param path
     */
    private int lastNonGlobPath(String path) {
        int last = 0;
        for (int i = 0; i != path.length(); ++i) {
            if (globChars.contains(path.charAt(i))) {
                return last;
            }
            if (path.charAt(i) == File.separatorChar) {
                last = i + 1;
            }
        }
        return -1; // no glob found.
    }

    /**
     * Expands {@code path} with glob patterns to return a sorted collection of absolute paths.
     * 
     * @param path
     */
    private Collection<String> globPath(String path) throws IOException {
        path = path.replaceFirst("^~" + File.separator, System.getProperty("user.home") + File.separator);

        int lastSeparator = lastNonGlobPath(path);
        if (lastSeparator >= 0) {
            Set<String> absPaths = new TreeSet<>();
            String dir = path.substring(0, lastSeparator);
            path = path.substring(lastSeparator);
            try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Paths.get(dir), path)) {
                for (Path additionalPath : dirStream) {
                    String absPath = additionalPath.toAbsolutePath().toString();
                    absPaths.add(absPath);
                }
            }
            return absPaths;
        } else {
            return Arrays.asList(path);
        }
    }
}
