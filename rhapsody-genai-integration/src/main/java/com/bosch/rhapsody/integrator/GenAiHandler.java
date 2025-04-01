package com.bosch.rhapsody.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

import javax.swing.JOptionPane;

import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

public class GenAiHandler {

  private Process pythonBackendProcess;

  private IRPApplication rhapsodyApp;

  public static void main(String[] args) {

    GenAiHandler plugin = new GenAiHandler(RhapsodyAppServer.getActiveRhapsodyApplication());
    // plugin.startPythonBackend();
    plugin.generateUMLDesign();
  }

  public GenAiHandler(IRPApplication rhapsodyApp2) {
    this.rhapsodyApp = rhapsodyApp2;
  }

  public void startPythonBackend() {
    try {
      // Command to start the Python backend
      String pythonCommand = "python"; // Use "python3" if required
      String backendScript = Paths.get("src", "main", "resources", "openai.py").toAbsolutePath().toString();
      // String backendScript = "../../../openai.py"; // Replace with the actual path to your Python script

      // Start the Python backend process
      ProcessBuilder processBuilder = new ProcessBuilder(pythonCommand, backendScript);
      processBuilder.redirectErrorStream(true); // Redirect error stream to standard output
      pythonBackendProcess = processBuilder.start();

      // Optionally, log backend output (useful for debugging)
      new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonBackendProcess.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            System.out.println("[Python Backend] " + line);
          }
        }
        catch (IOException e) {
          System.err.println(e.getStackTrace());
        }
      }).start();

    }
    catch (IOException e) {
      System.err.println(e.getStackTrace());
      JOptionPane.showMessageDialog(null, "Failed to start Python backend: " + e.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  public void generateUMLDesign() {
    try {
      // Prompt user to input requirement text
      // String requirementText = JOptionPane.showInputDialog(null, "Enter Requirement Text:", "Generate UML Design",
      //     JOptionPane.PLAIN_MESSAGE);

      String requirementText = "Extract all requirements related to Thermal Monitoring Unit";
      
      if (requirementText == null || requirementText.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Requirement text cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Send the requirement text to the backend and get the UML diagram
      String umlDiagram = sendRequirementToBackend(requirementText);

      rhapsodyApp.writeToOutputWindow("GenAIPlugin", "Respone : " + umlDiagram);

      if (umlDiagram == null || umlDiagram.isEmpty()) {
        JOptionPane.showMessageDialog(null, "Failed to generate UML diagram. Please check the backend.", "Error",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      // Display the UML diagram code
      JOptionPane.showMessageDialog(null, "Generated UML Diagram:\n" + umlDiagram, "UML Diagram",
          JOptionPane.INFORMATION_MESSAGE);

      // Optionally, add a new diagram to the model
      IRPModelElement activeElement = rhapsodyApp.getSelectedElement();
      if (activeElement != null) {
        IRPModelElement newDiagram = activeElement.addNewAggr("Sequence Diagram", "GeneratedDiagram");
        rhapsodyApp.writeToOutputWindow("GenAIPlugin", "New diagram created: " + newDiagram.getName());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private String sendRequirementToBackend(String requirementText) {
    try {
      // Backend URL (update this to match your Python backend's URL)
      String backendUrl = "http://10.169.242.230:5000/summarize_requirements";

      // Create HTTP connection
      URL url = new URL(backendUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setDoOutput(true);

      // Create JSON payload
      String payload =
          String.format("{\"feature_query\": \"%s\", \"uml_type\": \"Sequence Diagram\"}", requirementText);

      // Send JSON payload
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = payload.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      // Read response
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          // Parse the response (assuming the backend returns JSON with a "uml_design" key)
          return response.toString();
        }
      }
      else {
        System.err.println("Backend returned error: " + responseCode);
      }
    }
    catch (Exception e) {
      System.err.println(e.getLocalizedMessage());
    }
    return null;
  }

  public void shutdown() {
    // Stop the Python backend when the plugin is closed
    if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
      pythonBackendProcess.destroy();
      rhapsodyApp.writeToOutputWindow("GenAIPlugin", "Python backend stopped.");
    }
  }

}
