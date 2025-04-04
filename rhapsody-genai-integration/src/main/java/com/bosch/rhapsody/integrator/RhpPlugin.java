package com.bosch.rhapsody.integrator;

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
    rhapsodyApp = rpyApplication;
    genAiHandler = new GenAiHandler(rhapsodyApp);
//    genAiHandler.startPythonBackend();
    rhapsodyApp.writeToOutputWindow("GenAIPlugin", "GenAI Plugin initialized. Use the menu to generate UML diagrams.");
  }

  @Override
  public void RhpPluginInvokeItem() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginInvokeItem'");
  }

  @Override
  public void OnMenuItemSelect(String menuItem) {
    if (menuItem.equals("Generate UML Design")) {
      UI ui = new UI(genAiHandler);
      try {
        ui.createUI();
      }
      catch (Exception e) {
        rhapsodyApp.writeToOutputWindow("GenAIPlugin", e.getMessage());
      }
      // Call the GenAIHandler class to generate UML design
      // genAiHandler.generateUMLDesign();
    }
  }

  @Override
  public void OnTrigger(String trigger) {
    throw new UnsupportedOperationException("Unimplemented method 'OnTrigger'");
  }

  @Override
  public boolean RhpPluginCleanup() {
    genAiHandler.shutdown();
    return false;
  }

  @Override
  public void RhpPluginFinalCleanup() {
    throw new UnsupportedOperationException("Unimplemented method 'RhpPluginFinalCleanup'");
  }

}
