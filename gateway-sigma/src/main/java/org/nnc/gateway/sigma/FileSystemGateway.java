package org.nnc.gateway.sigma;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;
import org.nnc.gateway.RequestDto;
import org.nnc.gateway.ResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

@Controller
public class FileSystemGateway {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemGateway.class);

    private final ApplicationProperties applicationProperties;
    private final WatchService watchService;
    private final Object sync = new Object();

    @Autowired
    public FileSystemGateway(ApplicationProperties applicationProperties) throws IOException {
        this.applicationProperties = applicationProperties;

        watchService = FileSystems.getDefault().newWatchService();
        final Path path = Paths.get(applicationProperties.getInputFolder());
        path.register(watchService, ENTRY_MODIFY, ENTRY_CREATE);

        final Thread watchThread = new Thread(this::watch);
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public ResponseDto getResponse(final String name, final RequestDto requestDto) throws Exception {
        write(name, requestDto);

        return read(name, ResponseDto.class);
    }

    private void watch() {
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

            synchronized (sync) {
                if (watchKey != null) {
                    watchKey.pollEvents();
                    watchKey.reset();
                }

                sync.notifyAll();
            }
        }
    }

    private <T> T read(final String name, final Class<T> clazz) {
        Path path;
        synchronized (sync) {
            while ((path = check(name)) == null) {
                LOG.debug("wait " + name);
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }

        try {
            LOG.info("read " + path);
            final byte[] data = Files.readAllBytes(path);
            LOG.info("read bytes: " + data.length);

            T response = clazz.cast(SerializationUtils.deserialize(data));

            LOG.info("delete: " + path);
            Files.delete(path);

            return response;
        } catch (IOException | SerializationException e) {
            LOG.error("process error", e);
            return null;
        }
    }

    private Path check(final String name) {
        final Path folder = Paths.get(applicationProperties.getInputFolder());
        final String glob = name + ".*." + applicationProperties.getInputExtension();
        try (final DirectoryStream<Path> dir = Files.newDirectoryStream(folder, glob)) {
            for (final Path path : dir) {
                LOG.debug("check " + path);
                final String filename = FilenameUtils.removeExtension(path.getFileName().toString());
                final int pos = filename.lastIndexOf('.');
                if (pos >= 0) {
                    final long realLength = Long.parseLong(filename.substring(pos + 1));
                    final long fileLength = Files.size(path);
                    LOG.debug("real length " + realLength);
                    LOG.debug("file length " + fileLength);
                    if (realLength == fileLength) {
                        return path;
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("check error", e);
        }

        return null;
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
}
