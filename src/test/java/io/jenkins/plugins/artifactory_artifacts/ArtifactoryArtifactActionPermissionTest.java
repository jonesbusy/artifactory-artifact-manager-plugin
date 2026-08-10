package io.jenkins.plugins.artifactory_artifacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Run;
import jenkins.model.ArtifactManagerConfiguration;
import jenkins.model.Jenkins;
import org.htmlunit.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Focused permission test for {@link ArtifactoryArtifactAction#doReference}.
 *
 * <p>{@link Run#ARTIFACTS} is opt-in: it is only enforced when the
 * {@code hudson.security.ArtifactsPermission} system property is set (see
 * {@link hudson.Functions#isArtifactsPermissionEnabled()}). When it isn't set, the permission
 * must be treated as if it doesn't exist - i.e. every user who can already see the build is
 * allowed - exactly like {@code Run#doArtifact()} does in Jenkins core.
 */
@WithJenkins
class ArtifactoryArtifactActionPermissionTest {

    private static final String ARTIFACTS_PERMISSION_PROPERTY = "hudson.security.ArtifactsPermission";
    private static final String ARCHIVED_FILE = "out.txt";
    private static final String REPOSITORY = "my-generic-repo";

    @AfterEach
    void resetArtifactsPermissionProperty() {
        System.clearProperty(ARTIFACTS_PERMISSION_PROPERTY);
    }

    @Test
    void deniesUserWithoutArtifactsPermissionWhenPermissionIsEnabled(JenkinsRule rule) throws Exception {
        System.setProperty(ARTIFACTS_PERMISSION_PROPERTY, "true");
        FreeStyleBuild build = createBuildWithArtifactoryManager(rule, "artifacts-permission-enabled-denied");
        grantEveryone(rule, "reader");

        try (JenkinsRule.WebClient wc = loginAs(rule, "reader")) {
            Page response = wc.goTo(referenceUrl(build), null);
            assertEquals(
                    403,
                    response.getWebResponse().getStatusCode(),
                    "a user without Run.ARTIFACTS must be denied once the permission is enabled");
        }
    }

    @Test
    void allowsUserWithArtifactsPermissionWhenPermissionIsEnabled(JenkinsRule rule) throws Exception {
        System.setProperty(ARTIFACTS_PERMISSION_PROPERTY, "true");
        FreeStyleBuild build = createBuildWithArtifactoryManager(rule, "artifacts-permission-enabled-allowed");
        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        rule.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ, Run.ARTIFACTS)
                .everywhere()
                .to("viewer"));

        try (JenkinsRule.WebClient wc = loginAs(rule, "viewer")) {
            Page response = wc.goTo(referenceUrl(build), null);
            assertEquals(
                    200, response.getWebResponse().getStatusCode(), "a user granted Run.ARTIFACTS must be allowed");
            String body = response.getWebResponse().getContentAsString();
            assertTrue(body.contains(REPOSITORY), body);
            assertTrue(body.contains(ARCHIVED_FILE), body);
        }
    }

    @Test
    void allowsAnyReaderWhenArtifactsPermissionIsDisabled(JenkinsRule rule) throws Exception {
        FreeStyleBuild build = createBuildWithArtifactoryManager(rule, "artifacts-permission-disabled");
        grantEveryone(rule, "reader");

        try (JenkinsRule.WebClient wc = loginAs(rule, "reader")) {
            Page response = wc.goTo(referenceUrl(build), null);
            assertEquals(
                    200,
                    response.getWebResponse().getStatusCode(),
                    "when Run.ARTIFACTS is disabled it must be treated as if it doesn't exist, "
                            + "i.e. every user who can see the build is allowed");
        }
    }

    @Test
    void deniesUserWithoutItemReadRegardlessOfArtifactsPermission(JenkinsRule rule) throws Exception {
        FreeStyleBuild build = createBuildWithArtifactoryManager(rule, "no-build-access");
        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        rule.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.DISCOVER)
                .everywhere()
                .to("outsider"));

        try (JenkinsRule.WebClient wc = loginAs(rule, "outsider")) {
            Page response = wc.goTo(referenceUrl(build), null);
            assertEquals(
                    403,
                    response.getWebResponse().getStatusCode(),
                    "a user without Item.READ on the job must be denied even though Run.ARTIFACTS is disabled");
        }
    }

    private static void grantEveryone(JenkinsRule rule, String username) {
        rule.jenkins.setSecurityRealm(rule.createDummySecurityRealm());
        rule.jenkins.setAuthorizationStrategy(new MockAuthorizationStrategy()
                .grant(Jenkins.READ, Item.READ)
                .everywhere()
                .to(username));
    }

    private static JenkinsRule.WebClient loginAs(JenkinsRule rule, String username) throws Exception {
        JenkinsRule.WebClient wc = rule.createWebClient();
        wc.setThrowExceptionOnFailingStatusCode(false);
        wc.login(username);
        return wc;
    }

    private static String referenceUrl(FreeStyleBuild build) {
        return build.getUrl() + "artifactory-artifact-manager/reference?path=" + ARCHIVED_FILE;
    }

    /**
     * Creates a build whose artifact manager is an {@link ArtifactoryArtifactManager}, which is
     * what makes {@link ArtifactoryArtifactActionFactory} attach the action under test. No actual
     * archiving is needed (and no network calls happen) - selecting the manager the same way
     * {@code ArtifactArchiver} would is enough.
     */
    private static FreeStyleBuild createBuildWithArtifactoryManager(JenkinsRule rule, String jobName) throws Exception {
        ArtifactoryGenericArtifactConfig config = new ArtifactoryGenericArtifactConfig();
        config.setServerUrl("http://localhost");
        config.setRepository(REPOSITORY);
        ArtifactManagerConfiguration.get()
                .getArtifactManagerFactories()
                .add(new ArtifactoryArtifactManagerFactory(config));

        FreeStyleProject project = rule.createFreeStyleProject(jobName);
        FreeStyleBuild build = rule.buildAndAssertSuccess(project);
        build.pickArtifactManager();
        return build;
    }
}
