package com.bosch.rhapsody.integrator;

import java.io.File;
import java.net.URISyntaxException;

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

  private IRPApplication rhapsodyApp;
  GenAiHandler genAiHandler = null;

  public static void main(String[] args) {
    RhpPlugin plugin = new RhpPlugin();
    plugin.RhpPluginInit(RhapsodyAppServer.getActiveRhapsodyApplication());
    plugin.OnMenuItemSelect("Generate UML Design");

  }

  @Override
  public void RhpPluginInit(IRPApplication rpyApplication) {
    String temp = getJarPath();
    Constants.ROOTDIR = temp.replace("\\rhapsody-genai-integration\\target", "");
    Constants.API_KEY_FILE_PATH = Constants.ROOTDIR + File.separator + "api.key";
    Constants.DECRYPT_SCRIPT_PATH = Constants.ROOTDIR + File.separator + "dist" + File.separator + "decrypt.exe";
    Constants.BACKEND_SCRIPT_PATH = Constants.ROOTDIR + File.separator + "dist" + File.separator +"ollama.py";
    Constants.SECRET_KEY_FILE_PATH = Constants.ROOTDIR + File.separator + "secret.key";

    // Validate paths
    // if (!new File(Constants.DECRYPT_SCRIPT_PATH).exists()) {
    //     LoggerUtil.error("Decrypt script not found at: " + Constants.DECRYPT_SCRIPT_PATH);
    // }
    // if (!new File(Constants.BACKEND_SCRIPT_PATH).exists()) {
    //     LoggerUtil.error("Backend script not found at: " + Constants.BACKEND_SCRIPT_PATH);
    // }

    rhapsodyApp = rpyApplication;
    LoggerUtil.setRhapsodyApp(rhapsodyApp);
    LoggerUtil.info("GenAI Plugin initialized. Use the menu to generate UML diagrams.");
}

  @Override
  public void RhpPluginInvokeItem() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginInvokeItem'");
  }

  @Override
  public void OnMenuItemSelect(String menuItem) {
    if (menuItem.equals("Generate UML Design")) {
      try {
        genAiHandler = new GenAiHandler(rhapsodyApp);
        // String startPythonBackend = genAiHandler.startPythonBackend();
        String startPythonBackend = "Already server is running";
        UI ui = new UI(genAiHandler, startPythonBackend);
        try {
          ui.createUI();
        }
        catch (Exception e) {
          LoggerUtil.error(e.getMessage());
        }
        genAiHandler.shutdown();
        ProcessFiles files = new ProcessFiles();
        // files.deleteDirectories();

      }
      catch (Exception e) {
        LoggerUtil.error(e.getMessage());
      }
    }
  }


  /**
   * @return String
   */
  public static String getJarPath() {
    try {
      // Get the location of the JAR file
      File jarFile = new File(RhpPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      // Return the absolute path of the JAR file
      return jarFile.getParent();
    }
    catch (URISyntaxException e) {
      LoggerUtil.error(e.getMessage());
      return null;
    }
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
