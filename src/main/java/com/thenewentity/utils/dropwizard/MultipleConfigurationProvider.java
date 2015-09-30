package com.thenewentity.utils.dropwizard;

import io.dropwizard.configuration.ConfigurationSourceProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class MultipleConfigurationProvider implements ConfigurationSourceProvider {

    private final Collection<String> overrideFiles;
    private final Yaml yaml = new Yaml();
    private String effectiveConfig;
    
    /**
     * Constructor - {@code overrideFiles} is a list of filenames to merge into the yaml specified in the {@code path} provided to
     * {@link #open(String)}
     * 
     * @param overrideFiles
     */
    public MultipleConfigurationProvider(Collection<String> overrideFiles) {
        this.overrideFiles = overrideFiles;
    }

    /**
     * Read the specified yaml, then merge any {@link #overrideFiles} specified in the
     * {@link #MultipleConfigurationProvider(Collection) constructor} on top of it. Then, dump that out as yaml into
     * effectiveConfig, and return an InputStream to DropWizard.
     */
    @Override
    public InputStream open(String path) throws IOException {
        Map<Object, Object> config = new LinkedHashMap<>();

        mergeConfig(config, path);
        for (String overridePath : overrideFiles) {
            try {
                mergeConfig(config, overridePath);
            } catch (FileNotFoundException e) {
                // Do nothing - we couldn't find an override file. It's not the end of the world.
            }
        }

        effectiveConfig = yaml.dump(config);
        InputStream result = new ByteArrayInputStream(effectiveConfig.getBytes(StandardCharsets.UTF_8));
        return result;
    }

    public String getEffectiveConfig() {
        return effectiveConfig;
    }

    /**
     * Given an existing {@code config} object and a {@code path} to an override file, read the override file and merge its
     * contents into {@code config}.
     * 
     * @param config
     * @param path
     * @throws FileNotFoundException
     *             if path doesn't exist.
     */
    private void mergeConfig(Map<Object, Object> config, String path) throws FileNotFoundException {
        final File file = new File(path);
        Object overrides = yaml.load(new FileInputStream(file));
        if (overrides == null) {
            return;
        }
        mergeNode(config, overrides);
    }

    /**
     * Merge corresponding nodes in a configuration tree.
     * 
     * @param targetNode
     * @param sourceNode
     * @return - true: caller should replace targetNode with sourceNode in a collection.
     */
    @SuppressWarnings("unchecked")
    private boolean mergeNode(Object targetNode, Object sourceNode) {
        if (sourceNode == null) {
            return false;
        } else if (targetNode == null) {
            return true;
        } else if (targetNode instanceof Map<?, ?> && sourceNode instanceof Map<?, ?>) {
            mergeNodeMaps((Map<Object, Object>) targetNode, (Map<Object, Object>) sourceNode);
            return false;
        } else if (targetNode instanceof List<?> && sourceNode instanceof List<?>) {
            mergeNodeLists((List<Object>) targetNode, (List<Object>) sourceNode);
            return false;
        } else {
            // Otherwise, just replace the target with the source. Can't do this directly; only the caller really knows how.
            return true;
        }
    }

    /**
     * Merge lists in a configuration tree. In the initial range of {@code targetNode} where {@code sourceNode} has entries, the
     * children objects in the list are merged, unless {@code mergeNode} says to replace the entry outright. Any nodes in
     * {@code sourceNode} which are beyond the existing length of {@code targetNode} are simply appended.
     * 
     * @param targetNode
     * @param sourceNode
     */
    private void mergeNodeLists(List<Object> targetNode, List<Object> sourceNode) {
        for (int i = 0; i != targetNode.size(); ++i) {
            Object targetEntry = targetNode.get(i);
            Object sourceEntry = sourceNode.get(i);
            if (mergeNode(targetEntry, sourceEntry)) {
                targetNode.set(i, sourceEntry);
            }
        }
        for (int i = targetNode.size(); i < sourceNode.size(); ++i) {
            targetNode.add(sourceNode.get(i));
        }
    }

    /**
     * Merge maps in a configuration tree. If {@code targetNode} does not contain an entry from {@code sourceNode}, the entry is
     * copied into {@code targetNode}. If an entry exists in both {@code targetNode} and {@code sourceNode}, the two entries will
     * be merged.
     * 
     * @param targetNode
     * @param sourceNode
     */
    private void mergeNodeMaps(Map<Object, Object> targetNode, Map<Object, Object> sourceNode) {
        for (Map.Entry<Object, Object> entry : sourceNode.entrySet()) {
            if (!targetNode.containsKey(entry.getKey())) {
                targetNode.put(entry.getKey(), entry.getValue());
            } else {
                Object targetEntry = targetNode.get(entry.getKey());
                Object sourceEntry = entry.getValue();
                if (mergeNode(targetEntry, sourceEntry)) {
                    targetNode.put(entry.getKey(), sourceEntry);
                }
            }
        }
    }

}
