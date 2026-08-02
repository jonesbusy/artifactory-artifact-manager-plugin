package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Action;
import hudson.model.Run;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.ArtifactManager;
import jenkins.model.TransientActionFactory;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Attaches an {@link ArtifactoryArtifactAction} to every build whose artifact manager is an
 * {@link ArtifactoryArtifactManager} - and only such builds.
 */
@Restricted(NoExternalUse.class)
@Extension
public class ArtifactoryArtifactActionFactory extends TransientActionFactory<Run> {

    @Override
    public Class<Run> type() {
        return Run.class;
    }

    @NonNull
    @Override
    public Collection<? extends Action> createFor(@NonNull Run run) {
        ArtifactManager artifactManager = run.getArtifactManager();
        if (artifactManager instanceof ArtifactoryArtifactManager artifactoryArtifactManager) {
            return Collections.singleton(new ArtifactoryArtifactAction(run, artifactoryArtifactManager));
        }
        return Collections.emptySet();
    }
}
