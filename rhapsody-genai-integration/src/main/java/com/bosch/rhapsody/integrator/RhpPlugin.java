package com.bosch.rhapsody.integrator;

import java.io.File;
import java.nio.file.Paths;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.file.ProcessFiles;
import com.bosch.rhapsody.ui.UI;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.RPUserPlugin;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

/**
 * @author DHP4COB
 */
public class RhpPlugin extends RPUserPlugin {

  public static boolean isStandaloneJar = false;
  public static boolean isValidLanguage = false;
  ProcessFiles fileHandler = null;
  private IRPApplication rhapsodyApp;
  GenAiHandler genAiHandler = null;
  private UI ui = null;

    public static void main(String[] args) {
    isStandaloneJar = true;
    RhpPlugin plugin = new RhpPlugin();
    plugin.RhpPluginInit(RhapsodyAppServer.getActiveRhapsodyApplication());
    plugin.OnMenuItemSelect("Rhapsody GenAI");

  }
  
  @Override
  public void RhpPluginInit(IRPApplication rpyApplication) {
    fileHandler = new ProcessFiles();
    rhapsodyApp = rpyApplication;
    LoggerUtil.setRhapsodyApp(rhapsodyApp);
    Constants.rhapsodyApp = rhapsodyApp;
    String temp = fileHandler.getJarPath();

    if (RhpPlugin.isStandaloneJar) {
      Constants.ROOTDIR = temp.replace("\\rhapsody-genai-integration\\target", "");
      Constants.PROFILEPATH = Constants.ROOTDIR + File.separator
          + "RhapsodyPlugin\\RhapsodyPlugin_rpy\\GenAiIntegrationProfile";

      Constants.BACKEND_SCRIPT_PATH = Constants.PROFILEPATH + File.separator + "openai.py";
      Constants.CHAT_LOG_FILE_PATH = "C:\\Temp\\rhp-genai-chat_log.txt";
      Constants.PUML_PARSER_PATH = Constants.PROFILEPATH + File.separator + "pumlparser.exe";

     
    } else {
      Constants.PROFILEPATH = temp;
      Constants.ROOTDIR = Paths.get(Constants.PROFILEPATH).getParent().getParent().toString();
      Constants.BACKEND_SCRIPT_PATH = Constants.PROFILEPATH + File.separator + "openai.py";
      Constants.CHAT_LOG_FILE_PATH = "C:\\Temp\\rhp-genai-chat_log.txt";
      Constants.PUML_PARSER_PATH = Constants.PROFILEPATH + File.separator + "pumlparser.exe";
    }

    fileHandler.getChatLogFile();
    LoggerUtil.info("GenAI Plugin loaded " + Constants.VERSION + ". Use the menu to \"Rhapsody GenAI\".");
  }

  @Override
  public void OnMenuItemSelect(String menuItem) {
    if (!run()) {
      return;
    }
    Constants.rhapsodyApp = RhapsodyAppServer.getActiveRhapsodyApplication();
    LoggerUtil.setRhapsodyApp(Constants.rhapsodyApp);
    if (menuItem.equals("Rhapsody GenAI")) {

      if (fileHandler.validatePaths()) {
        try {
          LoggerUtil.info("Running GenAI...");
          genAiHandler = new GenAiHandler();
          if (!genAiHandler.isPythonCommandAccessible()) {
            LoggerUtil.error(
                "Python command is not accessible. Please ensure Python is installed and added to the system PATH.");
            return;
          }
          LoggerUtil.info("Python command is accessible. Proceeding with GenAI...");
          String response = genAiHandler.startPythonBackend();
          LoggerUtil.info("Python backend started successfully.");
          if (response == null || response.isEmpty()) {
            LoggerUtil.error("Failed to start Python backend. Please check the logs for details.");
            return;
          }
          ui = new UI(genAiHandler, response);
          try {
            LoggerUtil.info("Creating UI...");
            ui.createUI();
          } catch (Exception e) {
            LoggerUtil.error(e.getMessage());
          }
          if (ui.display.isDisposed()) {
            RhpPlugin.isStandaloneJar = false;
            RhpPlugin.isValidLanguage = false;
            genAiHandler.shutdown();
          }
        } catch (Exception e) {
          LoggerUtil.error(e.getMessage());
        }
      }
    }
  }

  public boolean run() {
    try {
      IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
      if (app == null) {
        UiUtil.showErrorPopup("Couldn't find running instance of Rhapsody.");
        return false;
      }

      IRPProject project = app.activeProject();
      if (project == null) {
        UiUtil.showErrorPopup(
            "Rhapsody " + app.version() + " is running. Couldn't find active Rhapsody project.");
        return false;
      }

      RhpPlugin.isValidLanguage = false;
      String language = project.getLanguage();
      if ("C".equals(language)) {
        RhpPlugin.isValidLanguage = true;
        return true;
      } else {
        boolean response = UiUtil.showQuestionPopup(
            String.format(
                "Expected Rhapsody project type is \"C\" but found \"%s\". \nUML diagram generation in rhapsody will be disabled.\n\n Do you want to continue?",
                language));
        if (response) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      String msg = "Can't get active object".equals(e.getMessage())
          ? "Couldn't find running instance of Rhapsody. \n\nHence terminating GenAI plugin."
          : e.getMessage();
      UiUtil.showErrorPopup(msg);
      return false;
    }
  }

  @Override
  public void RhpPluginInvokeItem() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginInvokeItem'");
  }

  @Override
  public void OnTrigger(String trigger) {
    throw new UnsupportedOperationException("Unimplemented method 'OnTrigger'");
  }

  @Override
  public boolean RhpPluginCleanup() {
    if (!ui.display.isDisposed()) {
      ui.display.dispose();
      RhpPlugin.isStandaloneJar = false;
      RhpPlugin.isValidLanguage = false;
      genAiHandler.shutdown();
    }
    return false;
  }

  @Override
  public void RhpPluginFinalCleanup() {
    if (!ui.display.isDisposed()) {
      ui.display.dispose();
      RhpPlugin.isStandaloneJar = false;
      RhpPlugin.isValidLanguage = false;
      genAiHandler.shutdown();
    }
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginFinalCleanup'");
  }

}
