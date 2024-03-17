package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.ItemListener;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.ArtifactManager;
import jenkins.util.VirtualFile;
import org.jenkinsci.plugins.workflow.flow.StashManager;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Restricted(NoExternalUse.class)
public class ArtifactoryArtifactManager extends ArtifactManager implements StashManager.StashAwareArtifactManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtifactoryArtifactManager.class);
    private transient Run<?, ?> build;
    private final ArtifactoryGenericArtifactConfig config;
    private transient String defaultKey;

    public ArtifactoryArtifactManager(Run<?, ?> build, ArtifactoryGenericArtifactConfig config) {
        this.config = config;
        this.build = build;
        onLoad(build);
    }

    @Override
    public void onLoad(@NonNull Run<?, ?> build) {
        this.build = build;
        this.defaultKey = String.format("%s/%s", build.getParent().getFullName(), build.getNumber())
                .replace("%2F", "/");
    }

    @Override
    public void archive(FilePath workspace, Launcher launcher, BuildListener listener, Map<String, String> artifacts)
            throws IOException, InterruptedException {
        if (artifacts.isEmpty()) {
            return;
        }
        List<UploadFile> files = new ArrayList<>();
        for (Map.Entry<String, String> entry : artifacts.entrySet()) {
            String path = "artifacts/" + entry.getKey();
            String filePath = getFilePath(path);
            files.add(new UploadFile(entry.getKey(), filePath));
        }

        workspace.act(new UploadToArtifactoryStorage(files));
    }

    @Override
    public boolean delete() throws IOException, InterruptedException {
        String virtualPath = getFilePath("");
        ArtifactoryClient client = new ArtifactoryClient();
        LOGGER.trace(String.format("Deleting %s...", virtualPath));
        try {
            client.deleteArtifact(virtualPath);
        } catch (Exception e) {
            LOGGER.error(String.format("Failed to delete %s", virtualPath), e);
            return false;
        }
        LOGGER.trace(String.format("Deleted %s", virtualPath));
        return true;
    }

    @Override
    public VirtualFile root() {
        return new ArtifactoryVirtualFile(getFilePath("artifacts"), build);
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
    public void clearAllStashes(@NonNull TaskListener listener) throws IOException, InterruptedException {
        String virtualPath = getFilePath("stashes");
        ArtifactoryClient client = new ArtifactoryClient();
        LOGGER.trace(String.format("Deleting %s...", virtualPath));
        try {
            if (client.isFolder(virtualPath)) {
                client.deleteArtifact(virtualPath);
                listener.getLogger().println("Deleted all stashes on Artifactory Storage");
                LOGGER.debug(String.format("Deleted stash %s", virtualPath));
            }
        } catch (Exception e) {
            listener.getLogger()
                    .printf("Failed to delete stashes on Artifactory Storage. Details %s%n", e.getMessage());
            LOGGER.error(String.format("Failed to delete stash on Artifactory at %s", virtualPath), e);
        }
    }

    @Override
    public void copyAllArtifactsAndStashes(@NonNull Run<?, ?> to, @NonNull TaskListener listener)
            throws IOException, InterruptedException {}

    private String getFilePath(String path) {
        return Utils.getFilePath(defaultKey, path);
    }

    private static class UploadFile implements Serializable {
        private final String name;
        private final String url;

        public UploadFile(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }

    /**
     * Master to slave callable that uploads files to Artifactory storage.
     */
    private static class UploadToArtifactoryStorage extends MasterToSlaveFileCallable<Void> {

        private final List<UploadFile> files;

        public UploadToArtifactoryStorage(List<UploadFile> files) {
            this.files = files;
        }

        @Override
        public Void invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
            ArtifactoryClient client = new ArtifactoryClient();
            for (UploadFile file : files) {
                LOGGER.debug(String.format("Uploading %s to %s", file.getName(), file.getUrl()));
                client.uploadArtifact(new File(f, file.getName()).toPath(), file.getUrl());
            }
            return null;
        }
    }

    /**
     * Item listener that listens to item deletion and location change events and updates the storage accordingly
     */
    @Extension
    public static final class ArtifactoryItemListener extends ItemListener {

        @Override
        public void onDeleted(Item item) {
            ArtifactoryClient client = new ArtifactoryClient();
            String path = Utils.stripTrailingSlash(Utils.getFilePath(item.getFullName(), ""));
            LOGGER.info(String.format("Checking if %s must be deleted on Artifactory Storage", path));
            try {
                if (client.isFolder(path)) {
                    LOGGER.debug(String.format("Deleting %s...", path));
                    client.deleteArtifact(path);
                    LOGGER.info(String.format("Deleted %s on Artifactory Storage", path));
                }
            } catch (IOException e) {
                LOGGER.error(String.format("Failed to delete %s", path), e);
            }
        }

        @Override
        public void onLocationChanged(Item item, String oldFullName, String newFullName) {
            ArtifactoryClient client = new ArtifactoryClient();
            String sourcePath = Utils.stripTrailingSlash(Utils.getFilePath(oldFullName, ""));
            String targetPath = Utils.stripTrailingSlash(Utils.getFilePath(newFullName, ""));
            LOGGER.info(
                    String.format("Checking if %s must be moved to %s on Artifactory Storage", sourcePath, targetPath));
            try {
                if (client.isFolder(sourcePath)) {
                    LOGGER.debug(String.format("Moving %s...", sourcePath));
                    client.move(sourcePath, targetPath);
                    LOGGER.info(String.format("Moving %s on Artifactory Storage", targetPath));
                }
            } catch (IOException e) {
                LOGGER.error(
                        String.format("Failed to move %s to %s. Artifactory Pro is needed", sourcePath, targetPath));
            }
        }
    }
}
