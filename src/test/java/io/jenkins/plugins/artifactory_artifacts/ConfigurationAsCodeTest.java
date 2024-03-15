package io.jenkins.plugins.artifactory_artifacts;

import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
public class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("configuration-as-code.yml")
    public void shouldSupportConfigurationAsCode(JenkinsConfiguredWithCodeRule jenkinsRule) throws Exception {
        ArtifactoryGenericArtifactConfig.DescriptorImpl extension = jenkinsRule
                .getInstance()
                .getExtensionList(ArtifactoryGenericArtifactConfig.DescriptorImpl.class)
                .get(0);
    }
}
