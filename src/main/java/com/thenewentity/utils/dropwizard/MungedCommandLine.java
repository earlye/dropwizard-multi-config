package com.thenewentity.utils.dropwizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MungedCommandLine {

    private String[] mungedArguments;
    private Collection<String> yamlOverrides;
    private String[] originalArguments;
    private String[] defaultArguments;

    public String[] getMungedArguments() {
        return mungedArguments;
    }

    public Collection<String> getYamlOverrides() {
        return yamlOverrides;
    }

    public String[] getOriginalArguments() {
        return originalArguments;
    }

    public String[] getDefaultArguments() {
        return defaultArguments;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private MungedCommandLine result;

        Builder() {
            result = new MungedCommandLine();
        }

        /**
         * Removes configuration override paths from the command line, if specified by putting a '--' before the main
         * configuration.yaml.
         * 
         * @return
         */
        public MungedCommandLine build() {
            // Either use the original arguments or the defaults, if none were specified...
            List<String> argv = new ArrayList<>();
            argv.addAll(Arrays.asList(result.originalArguments));
            if (argv.isEmpty()) {
                argv.addAll(Arrays.asList(result.defaultArguments));
            }

            // Look for a '--', which indicates that everything after it is a list of .yaml files.
            // The first yaml will be sent to MultipleConfigurationProvider by DropWizard. The rest will have to be handed to it
            // by the application implementation.
            int overridesBegin = argv.indexOf("--");
            if (overridesBegin >= 0) {
                ++overridesBegin; // skip the "--".
                ++overridesBegin; // skip the main .yaml that dropwizard reads.
                if (overridesBegin < argv.size()) {
                    result.yamlOverrides = argv.subList(overridesBegin, argv.size());
                    argv = argv.subList(0, overridesBegin);
                }
            }

            // And these are the arguments that DropWizard will "see"
            result.mungedArguments = argv.toArray(new String[0]);

            return result;
        }

        /**
         * Pass the arguments as passed to your application's main()
         * 
         * @param arguments
         * @return
         */
        public Builder setOriginalArguments(String[] arguments) {
            result.originalArguments = arguments;
            return this;
        }

        /**
         * Pass the arguments to use if main() received an empty list.
         * 
         * @param defaultArguments
         * @return
         */
        public Builder setDefaultArguments(String[] defaultArguments) {
            result.defaultArguments = defaultArguments;
            return this;
        }

    }

}
