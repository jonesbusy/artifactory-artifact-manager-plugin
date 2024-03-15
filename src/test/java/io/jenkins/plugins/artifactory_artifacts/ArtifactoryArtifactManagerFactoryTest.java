package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import jenkins.model.ArtifactManagerConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
public class ArtifactoryArtifactManagerFactoryTest {

    @Test
    public void testConfigRoundtrip(JenkinsRule jenkinsRule) throws Exception {

        // Set config
        ArtifactoryGenericArtifactConfig config = new ArtifactoryGenericArtifactConfig();
        config.setPrefix("jenkins/");
        config.setServerUrl("http://localhost:7001");
        config.setRepository("my-generic-repo");
        config.setStorageCredentialId("the-credentials-id");
        ArtifactoryArtifactManagerFactory artifactManagerFactory = new ArtifactoryArtifactManagerFactory(config);
        ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(artifactManagerFactory);

        // Save config
        jenkinsRule.configRoundtrip();

        // Assert config
        assertThat(config.getStorageCredentialId(), is("the-credentials-id"));
        assertThat(config.getServerUrl(), is("http://localhost:7001"));
        assertThat(config.getRepository(), is("my-generic-repo"));
        assertThat(config.getPrefix(), is("jenkins/"));
    }
}
