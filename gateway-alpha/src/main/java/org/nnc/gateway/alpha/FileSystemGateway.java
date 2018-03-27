package org.nnc.gateway.alpha;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.nnc.gateway.RequestDto;
import org.nnc.gateway.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Component
public class FileSystemGateway implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemGateway.class);

    private final ApplicationProperties applicationProperties;
    private final ProxyComponent proxyComponent;

    private final ExecutorService threadPool;
    private final Set<Path> processing = new HashSet<>();

    @Autowired
    public FileSystemGateway(ApplicationProperties applicationProperties, ProxyComponent proxyComponent) {
        this.applicationProperties = applicationProperties;
        this.proxyComponent = proxyComponent;

        threadPool = Executors.newFixedThreadPool(applicationProperties.getExecutors());
    }

    private void process(final Path path) {
        final String name = getName(path);
        LOG.info("process " + name);

        synchronized (processing) {
            while (!check(path)) {
                LOG.debug("wait " + name);
                try {
                    processing.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        try {
            LOG.info("read " + path);
            final byte[] data = Files.readAllBytes(path);
            LOG.info("read bytes: " + data.length);

            final RequestDto request = SerializationUtils.deserialize(data);
            final ResponseDto response = proxyComponent.getResponse(request);

            write(name, response);

            synchronized (processing) {
                LOG.info("delete " + path);
                Files.delete(path);
                processing.remove(path);
            }
        } catch (IOException | SerializationException e) {
            LOG.error("process error", e);
        }
    }

    private static String getName(final Path path) {
        String name = FilenameUtils.removeExtension(path.getFileName().toString());
        final int pos = name.lastIndexOf('.');
        if (pos >= 0) {
            name = name.substring(0, pos);
        }

        return name;
    }

    private boolean check(final Path path) {
        try {
            LOG.debug("check " + path);
            final String filename = FilenameUtils.removeExtension(path.getFileName().toString());
            final int pos = filename.lastIndexOf('.');
            if (pos >= 0) {
                final long realLength = Long.parseLong(filename.substring(pos + 1));
                final long fileLength = Files.size(path);
                LOG.debug("real length " + realLength);
                LOG.debug("file length " + fileLength);
                if (realLength == fileLength) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOG.error("check error", e);
        }

        return false;
    }

    private <T extends Serializable> void write(final String name, final T dto) throws IOException {
        final byte[] data = SerializationUtils.serialize(dto);
        final Path path = Paths.get(applicationProperties.getOutputFolder(), getOutputFilename(name, data));

        LOG.info("write " + path);
        Files.write(path, data);
        LOG.info("write bytes: " + data.length);
    }

    private String getOutputFilename(final String name, final byte[] data) {
        return name + "." + data.length + "." + applicationProperties.getOutputExtension();
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        final WatchService watchService = FileSystems.getDefault().newWatchService();
        final Path inputFolderPath = Paths.get(applicationProperties.getInputFolder());
        inputFolderPath.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);

        final Path inputFolder = Paths.get(applicationProperties.getInputFolder());
        final String inputGlob = "*." + applicationProperties.getInputExtension();

        while (true) {
            WatchKey watchKey;
            try {
                watchKey = watchService.poll(applicationProperties.getInputTimeout(), TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return;
            }

            if (watchKey != null) {
                LOG.info("watch: new data");
            } else {
                LOG.info("watch: timeout");
            }

            synchronized (processing) {
                if (watchKey != null) {
                    watchKey.pollEvents();
                    watchKey.reset();
                }

                try (final DirectoryStream<Path> dir = Files.newDirectoryStream(inputFolder, inputGlob)) {
                    dir.forEach(f -> {
                        if (!processing.contains(f)) {
                            processing.add(f);
                            threadPool.submit(() -> process(f));
                        }
                    });
                } catch (IOException e) {
                    LOG.error("watch error", e);
                }

                processing.notifyAll();
            }
        }
    }
}
