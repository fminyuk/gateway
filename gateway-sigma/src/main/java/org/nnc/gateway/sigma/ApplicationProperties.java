package org.nnc.gateway.sigma;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
    private final String inputFolder;
    private final String inputExtension;
    private final int inputTimeout;

    private final String outputFolder;
    private final String outputExtension;

    public ApplicationProperties(
            @Value("${gateway.input.folder}") String inputFolder,
            @Value("${gateway.input.extension}") String inputExtension,
            @Value("${gateway.input.timeout}") int inputTimeout,
            @Value("${gateway.output.folder}") String outputFolder,
            @Value("${gateway.output.extension}") String outputExtension) {

        this.inputFolder = inputFolder;
        this.inputExtension = inputExtension;
        this.inputTimeout = inputTimeout;

        this.outputFolder = outputFolder;
        this.outputExtension = outputExtension;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public String getInputExtension() {
        return inputExtension;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public String getOutputExtension() {
        return outputExtension;
    }

    public int getInputTimeout() {
        return inputTimeout;
    }
}
