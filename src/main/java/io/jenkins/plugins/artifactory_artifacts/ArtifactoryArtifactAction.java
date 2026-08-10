package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Functions;
import hudson.model.Action;
import hudson.model.Run;
import java.io.IOException;
import net.sf.json.JSONObject;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.verb.GET;

/**
 * A small, per-build action exposing the Artifactory external URL of each file archived through
 * {@link ArtifactoryArtifactManager}
 */
@Restricted(NoExternalUse.class)
public class ArtifactoryArtifactAction implements Action {

    /** The URL segment this action is reachable under, relative to the build's own URL. */
    static final String URL_NAME = "artifactory-artifact-manager";

    private final Run<?, ?> run;
    private final ArtifactoryArtifactManager manager;

    ArtifactoryArtifactAction(Run<?, ?> run, ArtifactoryArtifactManager manager) {
        this.run = run;
        this.manager = manager;
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return URL_NAME;
    }

    /**
     * Return the Artifactory external URL of the archived file at the given path, as JSON,
     * or a 404 if no such file was archived (or the caller lacks permission to see it).
     */
    @GET
    public void doReference(StaplerRequest2 req, StaplerResponse2 rsp, @QueryParameter String path) throws IOException {
        if (Functions.isArtifactsPermissionEnabled()) {
            run.checkPermission(Run.ARTIFACTS);
        }
        if (path == null || path.isBlank()) {
            rsp.sendError(400, "Missing 'path' parameter");
            return;
        }
        ArtifactoryGenericArtifactConfig config = Utils.getArtifactConfig();
        if (config == null) {
            rsp.sendError(500, "Artifactory configuration not found");
            return;
        }
        String artifactPath = manager.getArtifactPath(path);
        String externalUrl = Utils.getUrl(artifactPath);
        ArtifactoryArtifactReference reference = new ArtifactoryArtifactReference(
                config.getServerUrl(), config.getRepository(), artifactPath, externalUrl);
        JSONObject json = reference.toJson();
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.getWriter().write(json.toString());
    }

    @NonNull
    Run<?, ?> getRun() {
        return run;
    }
}
