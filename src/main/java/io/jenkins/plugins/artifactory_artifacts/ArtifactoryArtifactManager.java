package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import jenkins.MasterToSlaveFileCallable;
import jenkins.model.ArtifactManager;
import jenkins.util.VirtualFile;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class ArtifactoryArtifactManager extends ArtifactManager {

    private static final Logger LOGGER = Logger.getLogger(ArtifactManager.class.getName());
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
        LOGGER.info(String.format("Deleting all files under %s", virtualPath));
        return true;
    }

    @Override
    public VirtualFile root() {
        return new ArtifactoryVirtualFile("artifacts", build);
    }

    private String getFilePath(String path) {
        return getFilePath(defaultKey, path);
    }

    private String getFilePath(String key, String path) {
        return String.format("%s%s/%s", config.getPrefix(), key, path);
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
                LOGGER.info(String.format("Uploading %s to %s", file.getName(), file.getUrl()));
                client.uploadArtifact(new File(f, file.getName()).toPath(), file.getUrl());
            }
            return null;
        }
    }
}
