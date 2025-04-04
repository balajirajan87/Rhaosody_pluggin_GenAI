package com.bosch.rhapsody.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import com.telelogic.rhapsody.core.IRPApplication;

/**
 * @author DHP4COB
 */
public class GenAiHandler {

  String urlTemp = "http://127.0.0.1:5000/";

  private Process pythonBackendProcess;

  private IRPApplication rhapsodyApp;

  public static void main(String[] args) {
//    GenAiHandler plugin = new GenAiHandler(RhapsodyAppServer.getActiveRhapsodyApplication());
    // plugin.startPythonBackend();
    // plugin.generateUMLDesign();
  }

  /**
   * @param rhapsodyApp2
   */
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

  public int sendRequestToBackend(String docType, StringBuilder filePaths) {
    try {
      String docTypeApi = "";
      String jsonInputString = "";
      switch (docType) {
        case "Requirement doc":
          docTypeApi = "embed_requirement_documents";
          jsonInputString = "[\"Requirement_Docs\"]";
          break;
        case "Reference doc":
          docTypeApi = "embed_reference_documents";
          jsonInputString = "[\"Reference_Docs\"]";
          break;
        case "Guideline doc":
          docTypeApi = "embed_guideline_documents";
          jsonInputString = "[\"Guideline_Docs\"]";
          break;
        default:
          break;
      }

      String urlFinal = urlTemp + docTypeApi;


      // URL of the API endpoint
      URL url = new URL(urlFinal);

      System.out.println(url);
      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set request method to POST
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      connection.setDoOutput(true);

      // // Create JSON payload
      // String payload = new Gson().toJson(filePathList);

      connection.setConnectTimeout(20000); // Set connection timeout to 20 seconds
      connection.setReadTimeout(20000);

      // Send JSON payload
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

          connection.setConnectTimeout(60000); // Set connection timeout to 60 seconds
          connection.setReadTimeout(60000);
          // Attempt connection
          int responseCode = connection.getResponseCode();

          if (responseCode == HttpURLConnection.HTTP_OK) {
    System.out.println("Upload successful.");
} else {
    InputStream errorStream = connection.getErrorStream();
    if (errorStream != null) {
        String errorResponse = new BufferedReader(new InputStreamReader(errorStream))
            .lines().collect(Collectors.joining("\n"));
        System.err.println("Error Response: " + errorResponse);
    }
    System.err.println("HTTP Error Code: " + responseCode);
}

          return responseCode;

      // Close the connection
      // connection.disconnect();
    }
    catch (SocketTimeoutException e) {
      System.err.println("Connection timed out: " + e.getMessage());
      e.printStackTrace();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return 0;
  }


  public void shutdown() {
    // Stop the Python backend when the plugin is closed
    if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
      pythonBackendProcess.destroy();
      rhapsodyApp.writeToOutputWindow("GenAIPlugin", "Python backend stopped.");
    }
  }

}
