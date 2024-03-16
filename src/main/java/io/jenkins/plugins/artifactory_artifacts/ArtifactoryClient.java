package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.UploadableArtifact;

class ArtifactoryClient {

    public static final Logger LOGGER = Logger.getLogger(ArtifactoryGenericArtifactConfig.class.getName());

    private final ArtifactoryGenericArtifactConfig config;
    private final UsernamePasswordCredentials credentials;

    public ArtifactoryClient() {
        this(Utils.getArtifactConfig());
    }

    public ArtifactoryClient(ArtifactoryGenericArtifactConfig config) {
        this.config = config;
        credentials = Utils.getCredentials(this.config);
    }

    public void uploadArtifact(Path file, String targetPath) {
        try (Artifactory artifactory = buildArtifactory()) {
            UploadableArtifact artifact =
                    artifactory.repository(config.getRepository()).upload(targetPath, file.toFile());
            artifact.doUpload();
        }
    }

    public void deleteArtifact(String targetPath) {
        try (Artifactory artifactory = buildArtifactory()) {
            artifactory.repository(config.getRepository()).delete(targetPath);
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
