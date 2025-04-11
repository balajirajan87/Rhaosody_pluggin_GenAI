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
    plugin.OnMenuItemSelect("Rhapsody GenAI");

  }

  @Override
  public void RhpPluginInit(IRPApplication rpyApplication) {
    String temp = getJarPath();
    Constants.ROOTDIR = temp.replace("\\rhapsody-genai-integration\\target", "");
    Constants.API_KEY_FILE_PATH = Constants.ROOTDIR + File.separator + "api.key";
    Constants.DECRYPT_SCRIPT_PATH = Constants.ROOTDIR + File.separator + "dist" + File.separator + "decrypt.exe";
    Constants.BACKEND_SCRIPT_PATH = Constants.ROOTDIR + File.separator + "dist" + File.separator + "ollama.py";
    Constants.SECRET_KEY_FILE_PATH = Constants.ROOTDIR + File.separator + "secret.key";
    Constants.CHAT_LOG_FILE_PATH = Constants.ROOTDIR + File.separator + "chat_log.txt";
    // Validate paths
    // if (!new File(Constants.DECRYPT_SCRIPT_PATH).exists()) {
    // LoggerUtil.error("Decrypt script not found at: " + Constants.DECRYPT_SCRIPT_PATH);
    // }
    // if (!new File(Constants.BACKEND_SCRIPT_PATH).exists()) {
    // LoggerUtil.error("Backend script not found at: " + Constants.BACKEND_SCRIPT_PATH);
    // }

    rhapsodyApp = rpyApplication;
    LoggerUtil.setRhapsodyApp(rhapsodyApp);
    getChatLogFile();
    LoggerUtil.info("GenAI Plugin initialized. Use the menu to generate UML diagrams.");
  }

  private void getChatLogFile() {
    File chatLogFile = new File(Constants.CHAT_LOG_FILE_PATH);
    if (chatLogFile.exists()) {
      try (java.io.PrintWriter writer = new java.io.PrintWriter(chatLogFile)) {
        writer.print("");
        LoggerUtil.info("Chat log file content cleared: " + Constants.CHAT_LOG_FILE_PATH);
      }
      catch (Exception e) {
        LoggerUtil.error("Failed to clear the chat log file content: " + e.getMessage());
      }
    }else{
      try {
        if (chatLogFile.createNewFile()) {
          LoggerUtil.info("Chat log file created: " + Constants.CHAT_LOG_FILE_PATH);
        } else {
          LoggerUtil.error("Failed to create chat log file: " + Constants.CHAT_LOG_FILE_PATH);
        }
      } catch (Exception e) {
        LoggerUtil.error("Error while creating chat log file: " + e.getMessage());
      }
    }
  }

  @Override
  public void RhpPluginInvokeItem() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginInvokeItem'");
  }

  @Override
  public void OnMenuItemSelect(String menuItem) {
    if (menuItem.equals("Rhapsody GenAI")) {
      try {
        genAiHandler = new GenAiHandler(rhapsodyApp);
        // String startPythonBackend = genAiHandler.startPythonBackend();
        String  response="";
        try{
          response= genAiHandler.checkConnection();
        }catch(Exception e){
          response = e.getMessage();
        }
        UI ui = new UI(genAiHandler, response);
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
