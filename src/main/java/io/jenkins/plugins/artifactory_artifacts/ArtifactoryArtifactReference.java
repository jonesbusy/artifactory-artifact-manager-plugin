package io.jenkins.plugins.artifactory_artifacts;

import java.io.Serial;
import java.io.Serializable;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * The information shown to a user for a single archived file: the Artifactory external URL.
 *
 * @param serverUrl   the Artifactory server URL, e.g. {@code "https://artifactory.example.com"}
 * @param repository  the Artifactory repository name
 * @param path        the artifact path within the repository
 * @param externalUrl the full external download URL for the artifact
 */
@Restricted(NoExternalUse.class)
record ArtifactoryArtifactReference(String serverUrl, String repository, String path, String externalUrl)
        implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("serverUrl", serverUrl);
        json.put("repository", repository);
        json.put("path", path);
        json.put("externalUrl", externalUrl);
        return json;
    }
}
