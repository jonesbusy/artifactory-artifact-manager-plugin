package io.jenkins.plugins.artifactory_artifacts;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
@WireMockTest
public class UtilsTest extends BaseTest {

    @Test
    public void shouldGetUrl(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        assertThat(
                Utils.getUrl("artifact.txt"),
                is("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/my-generic-repo/artifact.txt"));

        assertThat(
                Utils.getUrl("item#1.txt"),
                is("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/my-generic-repo/item%231.txt"));
    }

    @Test
    public void shouldGetCredentials(JenkinsRule jenkinsRule, WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        configureConfig(jenkinsRule, wmRuntimeInfo, "jenkins/");
        assertThat(Utils.getCredentials().getUsername(), is("sample"));
        assertThat(Utils.getCredentials().getPassword().getPlainText(), is("sample"));
    }
}
