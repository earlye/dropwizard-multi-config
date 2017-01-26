package com.thenewentity.utils.dropwizard;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.yaml.snakeyaml.Yaml;

public class MultipleConfigurationMerger {

    private static Logger log = LoggerFactory.getLogger(MultipleConfigurationMerger.class);

    private ConfigurationReader configurationReader;
    private ObjectMapper mapper;
    private static final Yaml yaml = new Yaml();

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        MultipleConfigurationMerger result;

        Builder() {
            result = new MultipleConfigurationMerger();
        }

        public MultipleConfigurationMerger build() {
            if (result.mapper == null) {
                result.mapper = new ObjectMapper();
            }
            if (result.configurationReader == null) {
                result.configurationReader = new DefaultConfigurationReader();
            }
            return result;
        }

        public Builder setConfigurationReader(ConfigurationReader value) {
            result.configurationReader = value;
            return this;
        }

        public Builder setObjectMapper(ObjectMapper value) {
            result.mapper = value;
            return this;
        }
    }

    /**
     * Merge configuration .yaml files specified by {@code paths}, and return a Map<Object,Object> representing the merged
     * configs.
     * 
     * @param paths
     * @return Map<Object, Object> representing the merged .yaml files.
     */
    public Map<Object, Object> mergeConfigs(Collection<String> paths) {
        Map<Object, Object> config = new LinkedHashMap<>();

        if (paths != null) {
            for (String overridePath : paths) {
                try {
                    mergeConfig(config, overridePath);
                } catch (IOException e) {
                    // Just log it - we couldn't find a yaml file. It's not the end of the world.
                    log.debug("Could not merge .yaml at:" + overridePath);
                }
            }
        }

        return config;
    }

    /**
     * <p>
     * Merge configuration files specified by {@code paths}, and parse them into the specified {@code configurationType}
     * </p>
     * 
     * <p>
     * <b>Note</b> the merging process does not know about {@code configurationType}; it produces a merged yaml in memory, and
     * <b>then reparses</b> the merged yaml into the target type.
     * </p>
     * 
     * @param paths
     * @param configurationType
     * @return Parsed configuration object.
     */
    public <T> T loadConfigs(Collection<String> paths, Class<T> configurationType) {
        Map<Object, Object> configMap = mergeConfigs(paths);
        YAMLFactory yamlFactory = new YAMLFactory();
        String configStr = yaml.dump(configMap);
        try {
            return mapper.readValue(yamlFactory.createParser(configStr), configurationType);
        } catch (IOException e) {
            log.error("failed to loadConfigs", e);
            return null;
        }
    }

    /**
     * Given an existing {@code config} object and a {@code path} to an override file, read the override file and merge its
     * contents into {@code config}.
     * 
     * @param config
     * @param path
     * @throws IOException
     *             if the file couldn't be read for any reason.
     */
    private void mergeConfig(Map<Object, Object> config, String path) throws IOException {
        String configuration = this.configurationReader.readConfiguration(path);
        Object overrides = yaml.load(configuration);
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
            // TODO: check if entry.getKey() contains a path, and expand the node if so.

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
