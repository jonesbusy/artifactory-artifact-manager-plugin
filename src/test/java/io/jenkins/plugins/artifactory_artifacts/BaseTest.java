package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import jenkins.model.ArtifactManagerConfiguration;
import org.jvnet.hudson.test.JenkinsRule;

public class BaseTest {

    protected ArtifactoryGenericArtifactConfig configureConfig(
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

    /**
     * Setup WireMock stubs
     * @param wmRuntimeInfo the WireMock runtime info
     */
    protected void setupWireMockStubs(
            final String jobName, WireMockRuntimeInfo wmRuntimeInfo, String prefix, String artifact) {
        // WireMock stub
        WireMock wireMock = wmRuntimeInfo.getWireMock();

        // PUT to upload artifact
        wireMock.register(WireMock.put(WireMock.urlMatching("/my-generic-repo/" + prefix + ".*"))
                .willReturn(WireMock.okJson("{}")));

        // Define the base URL
        String basePath = "/api/storage/my-generic-repo/" + prefix + jobName + "/1/artifacts";

        // JSON response for folder with children
        String artifactsResponse = "{"
                + "\"children\": [{\"folder\": false, \"uri\": \"/" + artifact + "\"}],"
                + "\"created\": \"2024-03-17T13:20:19.836Z\","
                + "\"createdBy\": \"admin\","
                + "\"lastModified\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastUpdated\": \"2024-03-17T13:20:19.836Z\","
                + "\"modifiedBy\": \"admin\","
                + "\"path\": \"" + basePath + "\","
                + "\"repo\": \"my-generic-repo\","
                + "\"uri\": \"http://localhost:18081/artifactory" + basePath + "\""
                + "}";

        // JSON response for single artifact
        String artifactResponse = "{"
                + "\"created\": \"2024-03-17T13:20:19.836Z\","
                + "\"createdBy\": \"admin\","
                + "\"lastModified\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastUpdated\": \"2024-03-17T13:20:19.836Z\","
                + "\"modifiedBy\": \"admin\","
                + "\"path\": \"" + basePath + "/" + artifact + "\","
                + "\"repo\": \"my-generic-repo\","
                + "\"uri\": \"http://localhost:18081/artifactory" + basePath + "/" + artifact + "\""
                + "}";

        // AQL response
        String aqlResponse = "{\"results\": [{\"name\": \"" + artifact
                + "\", \"repo\": \"my-generic-repo\", \"path\": \"" + prefix + "/" + jobName + "/1/artifacts\"}]}";

        // Register GET requests
        wireMock.register(
                WireMock.get(WireMock.urlEqualTo(basePath + "/")).willReturn(WireMock.okJson(artifactsResponse)));
        wireMock.register(WireMock.get(WireMock.urlEqualTo(basePath + "/" + artifact))
                .willReturn(WireMock.okJson(artifactResponse)));

        // Register POST request
        wireMock.register(
                WireMock.post(WireMock.urlMatching("/api/search/aql")).willReturn(WireMock.okJson(aqlResponse)));
    }
}
