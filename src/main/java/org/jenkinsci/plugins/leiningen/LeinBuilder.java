package org.jenkinsci.plugins.leiningen;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Extension;
import hudson.Util;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * @author Tom Denley
 */
public class LeinBuilder extends Builder {

    private final String leiningenName;
    private final String tasks;
    
    @DataBoundConstructor
    public LeinBuilder(String leiningenName, String tasks) {
        this.leiningenName = leiningenName;
        this.tasks = tasks;
    }

    public String getTasks() {
        return tasks;
    }

    public String getLeiningenName() {
        return leiningenName;
    }

    public LeinInstallation getLein() {
        for (LeinInstallation installation : getDescriptor().getInstallations()) {
            if (leiningenName != null && installation.getName().equals(leiningenName)) {
                return installation;
            }
        }
        return null;
    }

    @Override
    public boolean perform(AbstractBuild<?,?> build, Launcher launcher, BuildListener listener)  throws InterruptedException, IOException {
        LeinLogger leinLogger = new LeinLogger(listener);
        leinLogger.info("Launching build.");

        EnvVars env = build.getEnvironment(listener);

        //Tasks
        String normalizedTasks = tasks.replaceAll("[\t\r\n]+", " ");
        normalizedTasks = Util.replaceMacro(normalizedTasks, env);
        normalizedTasks = Util.replaceMacro(normalizedTasks, build.getBuildVariables());

        //Build arguments
        ArgumentListBuilder args = new ArgumentListBuilder();
        LeinInstallation installation = getLein();
        if (installation == null) {
            args.add(launcher.isUnix() ? LeinInstallation.UNIX_LEIN_COMMAND : LeinInstallation.WINDOWS_LEIN_COMMAND);
        } else {
            installation = installation.forNode(Computer.currentComputer().getNode(), listener);
            installation = installation.forEnvironment(env);
            String exe = installation.getExecutable(launcher);
            if (exe == null) {
                leinLogger.error("Can't retrieve the Leiningen executable.");
                return false;
            }
            args.add(exe);
        }
        args.addKeyValuePairs("-D", build.getBuildVariables());
        args.addTokenized(normalizedTasks);
        if (installation != null) {
            env.put("LEIN_HOME", installation.getHome());
        }

        if (!launcher.isUnix()) {
            // on Windows, executing batch file can't return the correct error code,
            // so we need to wrap it into cmd.exe.
            // double %% is needed because we want ERRORLEVEL to be expanded after
            // batch file executed, not before. This alone shows how broken Windows is...
            args.prepend("cmd.exe", "/C");
            args.add("&&", "exit", "%%ERRORLEVEL%%");
        }

        FilePath rootLauncher = build.getWorkspace();

        //Not call from an Executor
        if (rootLauncher == null) {
            rootLauncher = build.getProject().getSomeWorkspace();
        }

        try {
            LeinConsoleAnnotator gca = new LeinConsoleAnnotator(listener.getLogger(), build.getCharset());
            int returnCode;
            try {
                returnCode = launcher.launch().cmds(args).envs(env).stdout(gca).pwd(rootLauncher).join();
            } finally {
                gca.forceEol();
            }
            final boolean result = (returnCode == 0);
            build.setResult(result ? Result.SUCCESS : Result.FAILURE);
            return result;
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            e.printStackTrace(listener.fatalError("command execution failed"));
            build.setResult(Result.FAILURE);
            return false;
        }
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link LeinBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a name");
            if (value.length() < 4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Say hello world";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         *
         * The method name is bit awkward because global.jelly calls this method to determine
         * the initial state of the checkbox by the naming convention.
         */
        public boolean getUseFrench() {
            return useFrench;
        }

        public LeinInstallation[] getInstallations() {
            // TODO Auto-generated method stub
            return null;
        }

        public void setInstallations(LeinInstallation[] installations) {
            // TODO Auto-generated method stub
            
        }
    }
}

