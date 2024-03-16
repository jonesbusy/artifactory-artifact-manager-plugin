package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;
import jenkins.util.VirtualFile;

public class ArtifactoryVirtualFile extends ArtifactoryAbstractVirtualFile {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryVirtualFile.class.getName());

    private final String key;

    private final transient Run<?, ?> build;

    public ArtifactoryVirtualFile(String key, Run<?, ?> build) {
        this.key = key;
        this.build = build;
    }

    public String getKey() {
        return key;
    }

    @NonNull
    @Override
    public String getName() {
        String localKey = stripTrailingSlash(key);
        return localKey.replaceFirst(".+/", "");
    }

    @NonNull
    @Override
    public URI toURI() {
        try {
            return new URI(Utils.getUrl(this.key));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public boolean isDirectory() throws IOException {
        return false;
    }

    @Override
    public boolean isFile() throws IOException {
        return true;
    }

    @Override
    public boolean exists() throws IOException {
        return isDirectory() || isFile();
    }

    @NonNull
    @Override
    public VirtualFile[] list() throws IOException {
        return new VirtualFile[0];
    }

    @NonNull
    @Override
    public VirtualFile child(@NonNull String name) {
        String joinedKey = stripTrailingSlash(this.key) + "/" + name;
        return new ArtifactoryVirtualFile(joinedKey, build);
    }

    @Override
    public long length() throws IOException {
        return 0;
    }

    @Override
    public long lastModified() throws IOException {
        return 0;
    }

    @Override
    public boolean canRead() throws IOException {
        return true;
    }

    @Override
    public InputStream open() throws IOException {
        if (isDirectory()) {
            throw new FileNotFoundException("Cannot open it because it is a directory.");
        }
        if (!isFile()) {
            throw new FileNotFoundException("Cannot open it because it is not a file.");
        }
        ArtifactoryClient client = new ArtifactoryClient();
        return client.downloadArtifact(this.key);
    }

    private String stripTrailingSlash(String key) {
        String localKey = key;
        if (key.endsWith("/")) {
            localKey = localKey.substring(0, localKey.length() - 1);
        }
        return localKey;
    }
}
