package io.jenkins.plugins.artifactory_artifacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.Serializable;
import java.util.logging.Level;
import jenkins.model.ArtifactManagerConfiguration;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNode;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;

/**
 * End-to-end UI test verifying that archived files are decorated with an Artifactory icon
 * (showing the external URL as a tooltip) when {@code disableDirectDownload} is enabled.
 */
class ArtifactoryArtifactReferenceUiRealJenkinsTest {

    @RegisterExtension
    private final RealJenkinsExtension extension =
            new RealJenkinsExtension().withLogger("io.jenkins.plugins.artifactory_artifacts", Level.FINE);

    private WireMockServer wireMockServer;

    private static final String JOB_NAME = "reference-ui-it";
    private static final String REPOSITORY = "my-generic-repo";
    private static final String ARCHIVED_FILE = "out.txt";
    private static final String PREFIX = "jenkins/";

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @AfterEach
    void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private void stubArtifactoryForArchive() {
        String artifactStoragePath = "/api/storage/" + REPOSITORY + "/" + PREFIX + JOB_NAME + "/1/artifacts";

        // Folder info response — needed by ArtifactoryClient#isFolder() so that list() proceeds
        String folderResponse = "{\"children\": [{\"folder\": false, \"uri\": \"/" + ARCHIVED_FILE + "\"}],"
                + "\"repo\": \"" + REPOSITORY + "\","
                + "\"path\": \"" + PREFIX + JOB_NAME + "/1/artifacts\","
                + "\"created\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastModified\": \"2024-03-17T13:20:19.836Z\","
                + "\"lastUpdated\": \"2024-03-17T13:20:19.836Z\","
                + "\"uri\": \"http://localhost/artifactory" + artifactStoragePath + "\"}";
        wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo(artifactStoragePath + "/"))
                .willReturn(WireMock.okJson(folderResponse)));

        // AQL search — used by ArtifactoryClient#list() to enumerate children
        String aqlResponse = "{\"results\": [{"
                + "\"name\": \"" + ARCHIVED_FILE + "\","
                + "\"type\": \"file\","
                + "\"modified\": \"2024-03-17T13:20:19.836Z\","
                + "\"size\": 13,"
                + "\"repo\": \"" + REPOSITORY + "\","
                + "\"path\": \"" + PREFIX + JOB_NAME + "/1/artifacts\"}]}";
        wireMockServer.stubFor(
                WireMock.post(WireMock.urlMatching("/api/search/aql")).willReturn(WireMock.okJson(aqlResponse)));

        // Accept any PUT (artifact upload during the pipeline)
        wireMockServer.stubFor(
                WireMock.put(WireMock.urlMatching("/" + REPOSITORY + "/.*")).willReturn(WireMock.okJson("{}")));

        // Stashes folder check — returns 404 (no stashes), prevents errors on build completion
        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("/api/storage/" + REPOSITORY + "/.*/stashes.*"))
                .willReturn(WireMock.notFound()));
    }

    @Test
    void archivedFileIsDecoratedWithArtifactoryIconWhenDirectDownloadDisabled() throws Throwable {
        stubArtifactoryForArchive();
        extension.then(new ArchiveAndCheckIconStep(wireMockServer.port(), true));
    }

    private record ArchiveAndCheckIconStep(int wireMockPort, boolean disableDirectDownload)
            implements RealJenkinsExtension.Step, Serializable {

        @Override
        public void run(JenkinsRule rule) throws Throwable {
            // Configure credentials
            UsernamePasswordCredentialsImpl credentials = new UsernamePasswordCredentialsImpl(
                    CredentialsScope.GLOBAL, "the-credentials-id", "sample", "admin", "password");
            CredentialsProvider.lookupStores(rule.jenkins)
                    .iterator()
                    .next()
                    .addCredentials(Domain.global(), credentials);

            // Configure the Artifactory artifact manager
            ArtifactoryGenericArtifactConfig config = new ArtifactoryGenericArtifactConfig();
            config.setPrefix(PREFIX);
            config.setServerUrl("http://localhost:" + wireMockPort);
            config.setRepository("my-generic-repo");
            config.setStorageCredentialId("the-credentials-id");
            config.setDisableDirectDownload(disableDirectDownload);

            ArtifactManagerConfiguration.get()
                    .getArtifactManagerFactories()
                    .add(new ArtifactoryArtifactManagerFactory(config));

            // Run a simple pipeline that archives one file
            WorkflowJob job = rule.jenkins.createProject(WorkflowJob.class, JOB_NAME);
            job.setDefinition(new CpsFlowDefinition("""
                    pipeline {
                        agent any
                        stages {
                            stage('Archive') {
                                steps {
                                    writeFile file: '%s', text: 'hello from artifactory\\n'
                                    archiveArtifacts artifacts: '%s'
                                }
                            }
                        }
                    }
                    """.formatted(ARCHIVED_FILE, ARCHIVED_FILE), true));
            rule.buildAndAssertSuccess(job);

            // Verify the JSON endpoint returns the expected external URL
            try (JenkinsRule.WebClient webClient = rule.createWebClient()) {
                org.htmlunit.Page response = webClient.goTo(
                        job.getUrl() + "1/artifactory-artifact-manager/reference?path=" + ARCHIVED_FILE,
                        "application/json");
                assertEquals(200, response.getWebResponse().getStatusCode());
                String body = response.getWebResponse().getContentAsString();
                assertTrue(body.contains("my-generic-repo"), "Response should contain the repository name: " + body);
                assertTrue(body.contains(ARCHIVED_FILE), "Response should contain the artifact file name: " + body);
                assertTrue(body.contains("externalUrl"), "Response should contain externalUrl field: " + body);
            }

            // Verify the build page's artifact list is (or is not) decorated with the icon
            try (JenkinsRule.WebClient webClient = rule.createWebClient()) {
                HtmlPage buildPage = webClient.goTo(job.getUrl() + "1/");
                webClient.waitForBackgroundJavaScript(15_000);
                assertIconPresent(buildPage);
            }

            // Verify the job overview page is (or is not) decorated too
            try (JenkinsRule.WebClient webClient = rule.createWebClient()) {
                HtmlPage jobPage = webClient.goTo(job.getUrl());
                webClient.waitForBackgroundJavaScript(15_000);
                assertIconPresent(jobPage);
            }
        }

        private void assertIconPresent(HtmlPage page) {
            DomNodeList<DomNode> icons = page.querySelectorAll(".artifactory-artifact-manager-icon");
            assertEquals(1, icons.size(), "Expected exactly one archived file to be decorated with an icon");

            DomElement icon = (DomElement) icons.get(0);
            assertEquals("svg", icon.getTagName());
            assertFalse(
                    icon.getAttribute("class").contains("jenkins-visually-hidden"),
                    "Icon must not be the visually-hidden accessibility helper");
            assertTrue(icon.getAttribute("class").contains("icon-sm"), icon.getAttribute("class"));

            // The icon must be placed directly after the *view* link
            DomNode previousSibling = icon.getPreviousSibling();
            assertTrue(previousSibling instanceof DomElement, "Icon must have a preceding sibling element");
            DomElement previousElement = (DomElement) previousSibling;
            assertEquals("a", previousElement.getTagName());
            assertTrue(previousElement.getAttribute("href").endsWith("/*view*/"), previousElement.getAttribute("href"));

            // The tooltip must contain the external Artifactory URL
            String tooltip = icon.getAttribute("tooltip");
            assertTrue(tooltip.contains("my-generic-repo"), "Tooltip should contain the repository name: " + tooltip);
            assertTrue(tooltip.contains(ARCHIVED_FILE), "Tooltip should contain the artifact file name: " + tooltip);

            // Color must use --text-color (not a link color) and be set as important
            String style = icon.getAttribute("style");
            assertTrue(style.contains("--text-color"), style);
            assertTrue(style.contains("important"), style);
        }
    }
}
