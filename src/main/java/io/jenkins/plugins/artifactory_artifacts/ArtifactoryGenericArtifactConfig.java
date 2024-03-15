package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import java.util.Collections;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ArtifactoryGenericArtifactConfig extends AbstractDescribableImpl<ArtifactoryGenericArtifactConfig>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private String storageCredentialId;
    private String serverUrl;
    private String repository;
    private String prefix;

    @DataBoundConstructor
    public ArtifactoryGenericArtifactConfig() {}

    public String getStorageCredentialId() {
        return storageCredentialId;
    }

    @DataBoundSetter
    public void setStorageCredentialId(String storageCredentialId) {
        this.storageCredentialId = storageCredentialId;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getRepository() {
        return repository;
    }

    @DataBoundSetter
    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getPrefix() {
        return prefix;
    }

    @DataBoundSetter
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public static ArtifactoryGenericArtifactConfig get() {
        return ExtensionList.lookupSingleton(ArtifactoryGenericArtifactConfig.class);
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<ArtifactoryGenericArtifactConfig> {

        public DescriptorImpl() {
            load();
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return "Generic";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return super.configure(req, json);
        }

        public ListBoxModel doFillStorageCredentialIdItems(@AncestorInPath Item item) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(get().getStorageCredentialId());
                }
            } else {
                if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(get().getStorageCredentialId());
                }
            }
            return result.includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM2,
                            item,
                            StandardUsernameCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StandardUsernameCredentials.class))
                    .includeCurrentValue(get().getStorageCredentialId());
        }
    }
}
