package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import jenkins.model.ArtifactManager;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ArtifactoryArtifactManager extends ArtifactManager implements StashManager.StashAwareArtifactManager {

    private static final Logger LOGGER = Logger.getLogger(ArtifactManager.class.getName());
    private transient Run<?, ?> build;
    private final ArtifactoryGenericArtifactConfig config;

    public ArtifactoryArtifactManager(Run<?, ?> build, ArtifactoryGenericArtifactConfig config) {
        this.config = config;
        this.build = build;
        onLoad(build);
    }

    @Override
    public void onLoad(@NonNull Run<?, ?> build) {}

    @Override
    public void archive(FilePath workspace, Launcher launcher, BuildListener listener, Map<String, String> artifacts)
            throws IOException, InterruptedException {}

    @Override
    public boolean delete() throws IOException, InterruptedException {
        return false;
    }

    @Override
    public VirtualFile root() {
        return new ArtifactoryVirtualFile(config.getRepository(), "artifacts", build);
    }

    @Override
    public void stash(
            @NonNull String name,
            @NonNull FilePath workspace,
            @NonNull Launcher launcher,
            @NonNull EnvVars env,
            @NonNull TaskListener listener,
            String includes,
            String excludes,
            boolean useDefaultExcludes,
            boolean allowEmpty)
            throws IOException, InterruptedException {}

    @Override
    public void unstash(
            @NonNull String name,
            @NonNull FilePath workspace,
            @NonNull Launcher launcher,
            @NonNull EnvVars env,
            @NonNull TaskListener listener)
            throws IOException, InterruptedException {}

    @Override
    public void clearAllStashes(@NonNull TaskListener listener) throws IOException, InterruptedException {}

    @Override
    public void copyAllArtifactsAndStashes(@NonNull Run<?, ?> to, @NonNull TaskListener listener)
            throws IOException, InterruptedException {}
}
