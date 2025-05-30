package com.bosch.rhapsody.integrator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.file.ProcessFiles;
import com.bosch.rhapsody.ui.UI;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RPUserPlugin;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

/**
 * @author DHP4COB
 */
public class RhpPlugin extends RPUserPlugin {

  private static boolean isStandalone = false;
  ProcessFiles fileHandler = null;
  private IRPApplication rhapsodyApp;
  GenAiHandler genAiHandler = null;

  public static void main(String[] args) {
    isStandalone = true;
    RhpPlugin plugin = new RhpPlugin();
    plugin.RhpPluginInit(RhapsodyAppServer.getActiveRhapsodyApplication());
    plugin.OnMenuItemSelect("Rhapsody GenAI");

  }

  @Override
  public void RhpPluginInit(IRPApplication rpyApplication) {
    fileHandler = new ProcessFiles();
    rhapsodyApp = rpyApplication;
    LoggerUtil.setRhapsodyApp(rhapsodyApp);
    String temp = fileHandler.getJarPath();

    if (RhpPlugin.isStandalone) {
      Constants.ROOTDIR = temp.replace("\\rhapsody-genai-integration\\target", "");
      Constants.PROFILEPATH = Constants.ROOTDIR + File.separator
          + "rhapsody-genai-integration\\src\\main\\resources";
      Constants.BACKEND_SCRIPT_PATH = Constants.ROOTDIR + File.separator + "openai.py";
      Constants.CHAT_LOG_FILE_PATH = Constants.ROOTDIR + File.separator + "rhp-genai-chat_log.txt";
    } else {
      Constants.PROFILEPATH = temp;
      Constants.ROOTDIR = Paths.get(Constants.PROFILEPATH).getParent().getParent().toString();
      Constants.BACKEND_SCRIPT_PATH = Constants.PROFILEPATH + File.separator + "openai.py";
      Constants.CHAT_LOG_FILE_PATH = "C:\\Temp\\rhp-genai-chat_log.txt";
    }

    fileHandler.getChatLogFile();

    LoggerUtil.info("GenAI Plugin loaded " + Constants.VERSION + ". Use the menu to \"Rhapsody GenAI\".");
  }

  @Override
  public void OnMenuItemSelect(String menuItem) {
    if (menuItem.equals("Rhapsody GenAI")) {
      if (fileHandler.validatePaths()) {
        try {
          LoggerUtil.info("Running GenAI...");
          genAiHandler = new GenAiHandler(rhapsodyApp);
          if (!genAiHandler.isPythonCommandAccessible()) {
            LoggerUtil.error(
                "Python command is not accessible. Please ensure Python is installed and added to the system PATH.");
            return;
          }
          String response = genAiHandler.startPythonBackend();

          UI ui = new UI(genAiHandler, response);
          try {
            ui.createUI();
          } catch (Exception e) {
            LoggerUtil.error(e.getMessage());
          }
          genAiHandler.shutdown();
          // files.deleteDirectories();

        } catch (Exception e) {
          LoggerUtil.error(e.getMessage());
        }
      }
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
    return false;
  }

  @Override
  public void RhpPluginFinalCleanup() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginFinalCleanup'");
  }

}
