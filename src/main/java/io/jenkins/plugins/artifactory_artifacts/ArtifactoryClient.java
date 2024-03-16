package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.UploadableArtifact;
import org.jfrog.artifactory.client.model.File;
import org.jfrog.filespecs.FileSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ArtifactoryClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryClient.class);

    private final ArtifactoryGenericArtifactConfig config;
    private final UsernamePasswordCredentials credentials;

    public ArtifactoryClient() {
        this(Utils.getArtifactConfig());
    }

    public ArtifactoryClient(ArtifactoryGenericArtifactConfig config) {
        this.config = config;
        credentials = Utils.getCredentials(this.config);
    }

    public void uploadArtifact(Path file, String targetPath) throws IOException {
        try (Artifactory artifactory = buildArtifactory()) {
            UploadableArtifact artifact =
                    artifactory.repository(config.getRepository()).upload(targetPath, file.toFile());
            artifact.withSize(Files.size(file));
            artifact.withListener(
                    (bytesRead, totalBytes) -> LOGGER.info(String.format("Uploaded %d/%d", bytesRead, totalBytes)));
            artifact.doUpload();
            LOGGER.trace(String.format("Uploaded %s to %s", file, targetPath));
        }
    }

    public void deleteArtifact(String targetPath) {
        try (Artifactory artifactory = buildArtifactory()) {
            artifactory.repository(config.getRepository()).delete(targetPath);
        }
    }

    public InputStream downloadArtifact(String targetPath) throws IOException {
        try (Artifactory artifactory = buildArtifactory()) {
            DownloadableArtifact artifact =
                    artifactory.repository(config.getRepository()).download(targetPath);
            return artifact.doDownload();
        }
    }

    public boolean isFolder(String targetPath) throws IOException {
        try (Artifactory artifactory = buildArtifactory()) {
            return artifactory.repository(config.getRepository()).isFolder(targetPath);
        }
    }

    public List<String> list(String targetPath) throws IOException {
        if (!isFolder(targetPath)) {
            throw new IllegalArgumentException("Target path is not a folder. Cannot list files");
        }
        try (Artifactory artifactory = buildArtifactory()) {
            FileSpec fileSpec = FileSpec.fromString(
                    String.format("{\"files\": [{\"pattern\": \"%s/%s*\"}]}", config.getRepository(), targetPath));
            return artifactory.searches().artifactsByFileSpec(fileSpec).stream()
                    .map((item -> String.format("%s/%s", item.getPath(), item.getName())))
                    .collect(Collectors.toList());
        }
    }

    public boolean isFile(String targetPath) throws IOException {
        return !isFolder(targetPath);
    }

    public long lastUpdated(String targetPath) throws IOException {
        LOGGER.trace(String.format("Getting last updated time for %s", targetPath));
        try (Artifactory artifactory = buildArtifactory()) {
            return artifactory
                    .repository(config.getRepository())
                    .file(targetPath)
                    .info()
                    .getLastModified()
                    .getTime();
        }
    }

    public long size(String targetPath) throws IOException {
        if (isFolder(targetPath)) {
            return 0;
        }
        LOGGER.trace(String.format("Getting size for %s", targetPath));
        try (Artifactory artifactory = buildArtifactory()) {
            File file = artifactory
                    .repository(config.getRepository())
                    .file(targetPath)
                    .info();
            return file.getSize();
        }
    }

    private Artifactory buildArtifactory() {
        return ArtifactoryClientBuilder.create()
                .setUrl(config.getServerUrl())
                .setUsername(credentials.getUsername())
                .setPassword(credentials.getPassword().getPlainText())
                .build();
    }
}
