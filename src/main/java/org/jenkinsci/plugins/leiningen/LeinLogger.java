package org.jenkinsci.plugins.leiningen;

import hudson.model.TaskListener;

import java.io.Serializable;

/**
 * @author Tom Denley
 */
public class LeinLogger implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private TaskListener listener;

    public LeinLogger(TaskListener listener) {
        this.listener = listener;
    }

    public void info(String message) {
        listener.getLogger().println("[Lein] - " + message);
    }

    public void error(String message) {
        listener.getLogger().println("[Lein] - [ERROR] " + message);
    }

}
