package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.UploadableArtifact;
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
        LOGGER.info("Uploading artifact to " + targetPath);
        try (Artifactory artifactory = buildArtifactory()) {
            UploadableArtifact artifact =
                    artifactory.repository(config.getRepository()).upload(targetPath, file.toFile());
            artifact.withSize(Files.size(file));
            artifact.withListener(
                    (bytesRead, totalBytes) -> LOGGER.info(String.format("Uploaded %d/%d", bytesRead, totalBytes)));
            artifact.doUpload();
            LOGGER.info("Uploaded artifact to " + targetPath);
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

    private Artifactory buildArtifactory() {
        return ArtifactoryClientBuilder.create()
                .setUrl(config.getServerUrl())
                .setUsername(credentials.getUsername())
                .setPassword(credentials.getPassword().getPlainText())
                .build();
    }
}
