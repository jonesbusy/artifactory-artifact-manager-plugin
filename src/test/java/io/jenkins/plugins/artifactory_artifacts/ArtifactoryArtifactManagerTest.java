package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import hudson.util.FormValidation;
import jenkins.model.ArtifactManagerConfiguration;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WireMockTest(httpPort = 18081)
public class ArtifactoryArtifactManagerTest {

    @Test
    public void shouldDoValidateArtifactoryConfig(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo, "/jenkins");
        ArtifactoryGenericArtifactConfig.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryGenericArtifactConfig.DescriptorImpl.class);

        // WireMock stub
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // PUT and delete to validate
        wireMock.register(WireMock.put(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                .willReturn(WireMock.okJson("{}")));
        wireMock.register(
                WireMock.delete(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                        .willReturn(WireMock.okJson("{}")));

        // Test
        FormValidation validation = descriptor.doValidateArtifactoryConfig(
                config.getServerUrl(), config.getStorageCredentialId(), config.getRepository(), config.getPrefix());

        // Assert
        assertThat(validation.kind, is(FormValidation.Kind.OK));
    }

    @Test
    public void shouldFailToDoValidateArtifactoryConfig(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        ArtifactoryGenericArtifactConfig.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryGenericArtifactConfig.DescriptorImpl.class);

        // WireMock stub
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // 404 not found
        wireMock.register(WireMock.put(WireMock.urlMatching("/my-generic-repo/jenkins/tmp-.*-artifactory-plugin-test"))
                .willReturn(WireMock.notFound()));

        // Test
        FormValidation validation = descriptor.doValidateArtifactoryConfig(
                config.getServerUrl(), config.getStorageCredentialId(), config.getRepository(), config.getPrefix());

        // Assert
        assertThat(validation.kind, is(FormValidation.Kind.ERROR));
        assertThat(
                validation.getMessage(),
                startsWith("Unable to connect to Artifactory. Please check the server url and credentials"));
    }

    @Test
    public void shouldCreteCorrectFactory(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        ArtifactoryArtifactManagerFactory factory = new ArtifactoryArtifactManagerFactory(config);
        ArtifactoryArtifactManagerFactory.DescriptorImpl descriptor =
                jenkinsRule.getInstance().getDescriptorByType(ArtifactoryArtifactManagerFactory.DescriptorImpl.class);
        assertThat(factory.getConfig(), is(config));
        assertThat(descriptor.getDisplayName(), is("Artifactory Artifact Storage"));
    }

    @Test
    public void testConfigRoundtrip(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        // Set config
        ArtifactoryGenericArtifactConfig config = configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");

        // Save config
        jenkinsRule.configRoundtrip();

        // Assert config
        assertThat(config.getStorageCredentialId(), is("the-credentials-id"));
        assertThat(config.getServerUrl(), is("http://localhost:18081"));
        assertThat(config.getRepository(), is("my-generic-repo"));
        assertThat(config.getPrefix(), is("jenkins/"));
    }

    public static ArtifactoryGenericArtifactConfig configureConfig(
            JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo, String prefix) throws Exception {

        // Create generic config
        ArtifactoryGenericArtifactConfig config = new ArtifactoryGenericArtifactConfig();
        config.setPrefix(prefix);
        config.setServerUrl("http://localhost:18081");
        config.setRepository("my-generic-repo");
        config.setStorageCredentialId("the-credentials-id");

        // Add credentials to the store
        UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
                CredentialsScope.GLOBAL, "the-credentials-id", "sample", "sample", "sample");
        CredentialsProvider.lookupStores(jenkinsRule.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), credentials);

        ArtifactoryArtifactManagerFactory artifactManagerFactory = new ArtifactoryArtifactManagerFactory(config);
        ArtifactManagerConfiguration.get().getArtifactManagerFactories().add(artifactManagerFactory);
        return config;
    }
}
