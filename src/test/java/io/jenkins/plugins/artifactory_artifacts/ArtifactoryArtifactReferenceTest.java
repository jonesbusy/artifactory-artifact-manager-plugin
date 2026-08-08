package io.jenkins.plugins.artifactory_artifacts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sf.json.JSONObject;
import org.junit.jupiter.api.Test;

class ArtifactoryArtifactReferenceTest {

    @Test
    void toJsonContainsAllFields() {
        ArtifactoryArtifactReference ref = new ArtifactoryArtifactReference(
                "https://artifactory.example.com",
                "my-generic-repo",
                "jenkins/my-job/1/artifacts/out.txt",
                "https://artifactory.example.com/my-generic-repo/jenkins/my-job/1/artifacts/out.txt");

        JSONObject json = ref.toJson();
        assertEquals("https://artifactory.example.com", json.getString("serverUrl"));
        assertEquals("my-generic-repo", json.getString("repository"));
        assertEquals("jenkins/my-job/1/artifacts/out.txt", json.getString("path"));
        assertEquals(
                "https://artifactory.example.com/my-generic-repo/jenkins/my-job/1/artifacts/out.txt",
                json.getString("externalUrl"));
    }

    @Test
    void externalUrlIsIncludedInJson() {
        ArtifactoryArtifactReference ref = new ArtifactoryArtifactReference(
                "http://localhost:8080",
                "repo",
                "job/1/artifacts/file.txt",
                "http://localhost:8080/repo/job/1/artifacts/file.txt");

        JSONObject json = ref.toJson();
        assertTrue(json.getString("externalUrl").startsWith("http://localhost:8080"), json.toString());
    }
}
