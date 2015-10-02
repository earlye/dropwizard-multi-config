package com.thenewentity.utils.dropwizard;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;

abstract public class MultipleConfigurationApplication<T extends Configuration> extends Application<T> {

    private final MungedCommandLine mungedCommandLine;
    private final MultipleConfigurationProvider multipleConfigurationProvider;

    public MultipleConfigurationApplication(String[] arguments, String[] defaultArguments) {
        // @formatter:off
        mungedCommandLine = MungedCommandLine.builder()
                .setOriginalArguments(arguments)
                .setDefaultArguments(defaultArguments)
                .build();
        // @formatter: on
        
        multipleConfigurationProvider = new MultipleConfigurationProvider(mungedCommandLine.getExtraArguments());        
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
