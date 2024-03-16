package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.security.ACL;
import hudson.util.DescribableList;
import java.nio.file.Path;
import java.util.Collections;
import jenkins.model.ArtifactManagerConfiguration;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.ArtifactManagerFactoryDescriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.StringUtils;

public final class Utils {

    public static ArtifactoryGenericArtifactConfig getArtifactConfig() {
        ArtifactManagerConfiguration artifactManagerConfiguration = ArtifactManagerConfiguration.get();
        DescribableList<ArtifactManagerFactory, ArtifactManagerFactoryDescriptor> artifactManagerFactories =
                artifactManagerConfiguration.getArtifactManagerFactories();
        ArtifactoryArtifactManagerFactory artifactoryArtifactManagerFactory =
                artifactManagerFactories.get(ArtifactoryArtifactManagerFactory.class);
        return artifactoryArtifactManagerFactory.getConfig();
    }

    public static StandardUsernamePasswordCredentials getCredentials() {
        return getCredentials(getArtifactConfig());
    }

    public static StandardUsernamePasswordCredentials getCredentials(ArtifactoryGenericArtifactConfig config) {
        return CredentialsMatchers.firstOrNull(
                CredentialsProvider.lookupCredentialsInItemGroup(
                        StandardUsernamePasswordCredentials.class, Jenkins.get(), ACL.SYSTEM2, Collections.emptyList()),
                CredentialsMatchers.allOf(
                        CredentialsMatchers.withId(config.getStorageCredentialId()),
                        CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));
    }

    /**
     * Get the path with the prefix
     * @param prefix the prefix. Can be null or empty. Must end with a slash if not empty.
     * @param filePath the file path
     * @return the path with the prefix
     */
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    public static @NonNull String getPath(String prefix, @NonNull Path filePath) {
        String defaultPrefix =
                StringUtils.isBlank(prefix) ? "" : prefix.endsWith("/") ? prefix : String.format("%s/", prefix);
        return String.format("%s%s", defaultPrefix, filePath.getFileName().toString());
    }
}
