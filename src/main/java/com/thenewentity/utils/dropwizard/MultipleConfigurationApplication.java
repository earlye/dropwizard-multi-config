package com.thenewentity.utils.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract public class MultipleConfigurationApplication<T extends Configuration> extends Application<T> {

    private final MungedCommandLine mungedCommandLine;
    private final MultipleConfigurationProvider multipleConfigurationProvider;

    public MultipleConfigurationApplication(String[] arguments, String[] defaultArguments, ObjectMapper mapper) {
        //@formatter:off
        mungedCommandLine = MungedCommandLine.builder()
                .setOriginalArguments(arguments)
                .setDefaultArguments(defaultArguments)
                .build();        
        //@formatter:on

        //@formatter:off
        multipleConfigurationProvider = MultipleConfigurationProvider.builder()
                .setOverrideFiles(mungedCommandLine.getExtraArguments())
                .setMultipleConfigurationMerger(MultipleConfigurationMerger.builder().setObjectMapper(mapper).build())
                .build();
        //@formatter:on
    }

    public MungedCommandLine getMungedCommandLine() {
        return mungedCommandLine;
    }

    public MultipleConfigurationProvider getMultipleConfigurationProvider() {
        return multipleConfigurationProvider;
    }

    public void run() throws Exception {
        super.run(mungedCommandLine.getMungedArguments());
    }

    public void initialize(Bootstrap<T> bootstrap) {
        bootstrap.setConfigurationSourceProvider(getMultipleConfigurationProvider());
    }
}
