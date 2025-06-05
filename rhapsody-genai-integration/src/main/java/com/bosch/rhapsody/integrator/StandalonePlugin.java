package com.bosch.rhapsody.integrator;

import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

public class StandalonePlugin {

    public static void main(String[] args) {
        // RhpPlugin.isStandalone = true;
        run();
        // RhpPlugin.isStandalone = false;
    }

    private static void run() {
        RhpPlugin.isStandaloneJar = true;
        try {
            IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
            if (app == null) {
                UiUtil.showErrorPopup("Couldn't find running instance of Rhapsody.");
                return;
            }
    
            IRPProject project = app.activeProject();
            if (project == null) {
                UiUtil.showErrorPopup(
                    "Rhapsody " + app.version() + " is running. Couldn't find active Rhapsody project.");
                return;
            }
    
            RhpPlugin.isValidLanguage = false;
            String language = project.getLanguage();
            if ("C".equals(language)) {
                RhpPlugin.isValidLanguage = true;
                initializePlugin(app);
            } else {
                boolean response = UiUtil.showQuestionPopup(
                    String.format(
                        "Expected Rhapsody project type is \"C\" but found \"%s\". \nUML diagram generation in rhapsody will be disabled.\n\n Do you want to continue?",
                        language));
                if (response) {
                    initializePlugin(app);
                }
            }
        } catch (Exception e) {
            String msg = "Can't get active object".equals(e.getMessage())
                ? "Couldn't find running instance of Rhapsody. \n\nHence terminating GenAI plugin."
                : e.getMessage();
            UiUtil.showErrorPopup(msg);
        } finally {
            RhpPlugin.isStandaloneJar = false;
        }
    }
    
    private static void initializePlugin(IRPApplication app) {
        RhpPlugin plugin = new RhpPlugin();
        plugin.RhpPluginInit(app);
        plugin.OnMenuItemSelect("Rhapsody GenAI");
    }

}