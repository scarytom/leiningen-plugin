package org.jenkinsci.plugins.leiningen;

import hudson.Extension;
import hudson.tools.DownloadFromUrlInstaller;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Tom Denley
 */
public class LeinInstaller extends DownloadFromUrlInstaller {
    @DataBoundConstructor
    public LeinInstaller(String id) {
        super(id);
    }

    @Extension
    public static final class DescriptorImpl extends DownloadFromUrlInstaller.DescriptorImpl<LeinInstaller> {
        public String getDisplayName() {
            return "Self-install from GitHub";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == LeinInstallation.class;
        }
    }
}
