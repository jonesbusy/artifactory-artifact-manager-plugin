package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WireMockTest(httpPort = 18081)
public class PipelineTest {

    @Test
    public void testPipelineWithPrefix(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        final String pipelineName = "testPipelineWithPrefix";

        ArtifactoryArtifactManagerTest.configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        String pipeline = IOUtils.toString(
                Objects.requireNonNull(PipelineTest.class.getResourceAsStream("/pipelines/archiveController.groovy")),
                StandardCharsets.UTF_8);

        // Setup wiremock stubs
        setupWireMockStubs(pipelineName, wmRuntimeInfo, "jenkins/", "artifact.txt");

        // Run job
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, pipelineName);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = Objects.requireNonNull(workflowJob.scheduleBuild2(0)).waitForStart();
        jenkinsRule.waitForCompletion(run1);

        // Job success
        assertThat(run1.getResult(), equalTo(hudson.model.Result.SUCCESS));

        // Check 1 artifact
        assertThat(run1.getArtifacts(), hasSize(1));
    }

    @Test
    public void testPipelineWithoutPrefix(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        final String pipelineName = "testPipelineWithoutPrefix";

        ArtifactoryArtifactManagerTest.configureConfig(jenkinsRule, wmRuntimeInfo, "");
        String pipeline = IOUtils.toString(
                Objects.requireNonNull(PipelineTest.class.getResourceAsStream("/pipelines/archiveController.groovy")),
                StandardCharsets.UTF_8);

        // Setup wiremock stubs
        setupWireMockStubs(pipelineName, wmRuntimeInfo, "", "artifact.txt");

        // Run job
        WorkflowJob workflowJob = jenkinsRule.createProject(WorkflowJob.class, pipelineName);
        workflowJob.setDefinition(new CpsFlowDefinition(pipeline, true));
        WorkflowRun run1 = Objects.requireNonNull(workflowJob.scheduleBuild2(0)).waitForStart();
        jenkinsRule.waitForCompletion(run1);

        // Job success
        assertThat(run1.getResult(), equalTo(hudson.model.Result.SUCCESS));

        // Check 1 artifact
        assertThat(run1.getArtifacts(), hasSize(1));
    }

    /**
     * Setup WireMock stubs
     * @param wmRuntimeInfo the WireMock runtime info
     */
    private void setupWireMockStubs(
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
