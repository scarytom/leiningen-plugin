package org.jenkinsci.plugins.leiningen;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Launcher;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.remoting.Callable;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolProperty;
import hudson.tools.ToolInstallation;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Tom Denley
 */
public class LeinInstallation extends ToolInstallation implements EnvironmentSpecific<LeinInstallation>, NodeSpecific<LeinInstallation>, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String UNIX_LEIN_COMMAND = "lein";
    public static final String WINDOWS_LEIN_COMMAND = "lein.bat";

    private final String leinHome;

    @DataBoundConstructor
    public LeinInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, launderHome(home), properties);
        this.leinHome = super.getHome();
    }

    private static String launderHome(String home) {
        if (home.endsWith("/") || home.endsWith("\\")) {
            // see https://issues.apache.org/bugzilla/show_bug.cgi?id=26947
            // Ant doesn't like the trailing slash, especially on Windows
            return home.substring(0, home.length() - 1);
        }
        return home;
    }


    @Override
    public String getHome() {
        if (leinHome != null) {
            return leinHome;
        }
        return super.getHome();
    }

    public String getExecutable(Launcher launcher) throws IOException, InterruptedException {
        return launcher.getChannel().call(new Callable<String, IOException>() {
            private static final long serialVersionUID = 1L;
            public String call() throws IOException {
                File exe = getExeFile();
                if (exe.exists()) {
                    return exe.getPath();
                }
                return null;
            }
        });
    }

    private File getExeFile() {
        String execName = (Functions.isWindows()) ? WINDOWS_LEIN_COMMAND : UNIX_LEIN_COMMAND;
        String homeDir = Util.replaceMacro(leinHome, EnvVars.masterEnvVars);
        return new File(homeDir, "bin/" + execName);
    }

    public LeinInstallation forEnvironment(EnvVars environment) {
        return new LeinInstallation(getName(), environment.expand(leinHome), getProperties().toList());
    }

    public LeinInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new LeinInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<LeinInstallation> {

        public DescriptorImpl() {
        }

        @Override
        public String getDisplayName() {
            return org.jenkinsci.plugins.leiningen.Messages.installer_displayName();
        }

        @Override
        public List<? extends ToolInstaller> getDefaultInstallers() {
            return Collections.singletonList(new LeinInstaller(null));
        }

        // for compatibility reasons, the persistence is done by LeinBuilder.DescriptorImpl

        @Override
        public LeinInstallation[] getInstallations() {
            return Jenkins.getInstance().getDescriptorByType(LeinBuilder.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(LeinInstallation... installations) {
            Jenkins.getInstance().getDescriptorByType(LeinBuilder.DescriptorImpl.class).setInstallations(installations);
        }

    }

}
