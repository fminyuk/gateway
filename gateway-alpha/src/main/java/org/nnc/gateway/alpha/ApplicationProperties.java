package org.nnc.gateway.alpha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApplicationProperties {
    private final String inputFolder;
    private final String inputExtension;
    private final long inputTimeout;

    private final String outputFolder;
    private final String outputExtension;

    private final int executors;

    public ApplicationProperties(
            @Value("${gateway.input.folder}") String inputFolder,
            @Value("${gateway.input.extension}") String inputExtension,
            @Value("${gateway.input.timeout}") long inputTimeout,
            @Value("${gateway.output.folder}") String outputFolder,
            @Value("${gateway.output.extension}") String outputExtension,
            @Value("${executors}") int executors
    ) {
        this.inputFolder = inputFolder;
        this.inputExtension = inputExtension;
        this.inputTimeout = inputTimeout;

        this.outputFolder = outputFolder;
        this.outputExtension = outputExtension;

        this.executors = executors;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public String getInputExtension() {
        return inputExtension;
    }

    public long getInputTimeout() {
        return inputTimeout;
    }

    public String getOutputFolder() {
        return outputFolder;
    }

    public String getOutputExtension() {
        return outputExtension;
    }

    public int getExecutors() {
        return executors;
    }
}
