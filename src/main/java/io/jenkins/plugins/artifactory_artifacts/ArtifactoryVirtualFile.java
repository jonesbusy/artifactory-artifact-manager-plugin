package io.jenkins.plugins.artifactory_artifacts;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Run;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.logging.Logger;
import jenkins.util.VirtualFile;

public class ArtifactoryVirtualFile extends ArtifactoryAbstractVirtualFile {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ArtifactoryVirtualFile.class.getName());

    private final String repository;
    private final String key;

    private final transient Run<?, ?> build;

    public ArtifactoryVirtualFile(String repository, String key, Run<?, ?> build) {
        this.repository = repository;
        this.key = key;
        this.build = build;
    }

    public String getRepository() {
        return repository;
    }

    public String getKey() {
        return key;
    }

    @NonNull
    @Override
    public String getName() {
        return "";
    }

    @NonNull
    @Override
    public URI toURI() {
        return URI.create("artifactory://" + repository + "/" + key);
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
        return false;
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
        return this;
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
        return false;
    }

    @Override
    public InputStream open() throws IOException {
        return null;
    }
}
