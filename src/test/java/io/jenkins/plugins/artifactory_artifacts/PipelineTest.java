package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import hudson.model.Label;
import hudson.slaves.DumbSlave;
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
@WireMockTest
public class PipelineTest extends BaseTest {

    @Test
    public void testPipelineWithPrefix(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {

        final String pipelineName = "testPipelineWithPrefix";

        configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
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

        configureConfig(jenkinsRule, wmRuntimeInfo, "");
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

    @Test
    public void testPipelineOnAgentWithPrefix(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo)
            throws Exception {

        final String pipelineName = "testPipelineWithPrefix";

        configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        String pipeline = IOUtils.toString(
                Objects.requireNonNull(PipelineTest.class.getResourceAsStream("/pipelines/archiveAgent.groovy")),
                StandardCharsets.UTF_8);

        DumbSlave s = jenkinsRule.createSlave(Label.get("agent"));

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
}
