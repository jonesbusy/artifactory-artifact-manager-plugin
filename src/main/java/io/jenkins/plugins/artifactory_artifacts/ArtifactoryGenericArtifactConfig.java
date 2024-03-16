package io.jenkins.plugins.artifactory_artifacts;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.UploadableArtifact;
import org.kohsuke.stapler.*;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class ArtifactoryGenericArtifactConfig extends AbstractDescribableImpl<ArtifactoryGenericArtifactConfig>
        implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_URL_REGEXP =
            "^(http://|https://)[a-z0-9][a-z0-9-.]{0,}(?::[0-9]{1,5})?(/[0-9a-zA-Z_]*)*$";
    private static final Pattern endPointPattern = Pattern.compile(SERVER_URL_REGEXP, Pattern.CASE_INSENSITIVE);

    public static final Logger LOGGER = Logger.getLogger(ArtifactoryGenericArtifactConfig.class.getName());

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

        public FormValidation doCheckPrefix(@QueryParameter String prefix) {
            FormValidation ret;
            if (StringUtils.isBlank(prefix)) {
                ret = FormValidation.ok("Artifacts will be stored in the root folder of the Artifactory Repository.");
            } else if (prefix.endsWith("/")) {
                ret = FormValidation.ok();
            } else {
                ret = FormValidation.error("A prefix must end with a slash.");
            }
            return ret;
        }

        public FormValidation doCheckRepository(@QueryParameter String repository) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(repository)) {
                ret = FormValidation.error("Repository cannot be blank");
            }
            //
            return ret;
        }

        public FormValidation doCheckServerUrl(@QueryParameter String serverUrl) {
            FormValidation ret = FormValidation.ok();
            if (StringUtils.isBlank(serverUrl)) {
                ret = FormValidation.error("Server url cannot be blank");
            } else if (!endPointPattern.matcher(serverUrl).matches()) {
                ret = FormValidation.error("Server url doesn't seem valid. Should start with http:// or https://");
            }
            return ret;
        }

        @RequirePOST
        public FormValidation doValidateArtifactoryConfig(
                @QueryParameter("serverUrl") final String serverUrl,
                @QueryParameter("storageCredentialId") final String storageCredentialId,
                @QueryParameter("repository") final String repository,
                @QueryParameter("prefix") final String prefix) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            FormValidation ret = FormValidation.ok("Success");

            String defaultPrefix = StringUtils.isBlank(prefix) ? "" : prefix;

            // Retrieve credentials from storageCredentialsId
            StandardUsernamePasswordCredentials credentials = CredentialsMatchers.firstOrNull(
                    CredentialsProvider.lookupCredentialsInItemGroup(
                            StandardUsernamePasswordCredentials.class,
                            Jenkins.get(),
                            ACL.SYSTEM2,
                            Collections.emptyList()),
                    CredentialsMatchers.allOf(
                            CredentialsMatchers.withId(storageCredentialId),
                            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)));

            if (credentials == null) {
                return FormValidation.error("Credentials not found");
            }

            try (Artifactory artifactory = ArtifactoryClientBuilder.create()
                    .setUrl(serverUrl)
                    .setUsername(credentials.getUsername())
                    .addInterceptorLast((request, httpContext) -> {
                        LOGGER.info("Artifactory request: " + request.getRequestLine());
                    })
                    .setPassword(credentials.getPassword().getPlainText())
                    .build()) {
                LOGGER.info("Validating Artifactory configuration...");

                // Upload temporary file and delete it
                Path tmpFile = Files.createTempFile("tmp-", "jenkins-artifactory-plugin-test");
                UploadableArtifact artifact = artifactory
                        .repository(repository)
                        .upload(defaultPrefix + tmpFile.getFileName().toString(), tmpFile.toFile());
                artifact.doUpload();
                artifactory
                        .repository(repository)
                        .delete(defaultPrefix + tmpFile.getFileName().toString());

                LOGGER.info("Artifactory configuration validated");
            } catch (Exception e) {
                ret = FormValidation.error(
                        "Unable to connect to Artifactory. Please check the server url and credentials : "
                                + e.getMessage());
                LOGGER.warning(e.getMessage());
                return ret;
            }

            return ret;
        }
    }
}
