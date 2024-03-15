package io.jenkins.plugins.artifactory_artifacts;

import hudson.util.DescribableList;
import jenkins.model.ArtifactManagerConfiguration;
import jenkins.model.ArtifactManagerFactory;
import jenkins.model.ArtifactManagerFactoryDescriptor;

public final class Utils {

    public static ArtifactoryGenericArtifactConfig getArtifactConfig() {
        ArtifactManagerConfiguration artifactManagerConfiguration = ArtifactManagerConfiguration.get();
        DescribableList<ArtifactManagerFactory, ArtifactManagerFactoryDescriptor> artifactManagerFactories =
                artifactManagerConfiguration.getArtifactManagerFactories();
        ArtifactoryArtifactManagerFactory artifactoryArtifactManagerFactory =
                artifactManagerFactories.get(ArtifactoryArtifactManagerFactory.class);
        return artifactoryArtifactManagerFactory.getConfig();
    }
}
