package com.bosch.rhapsody.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import com.google.gson.Gson;
import com.telelogic.rhapsody.core.IRPApplication;

/**
 * @author DHP4COB
 */
public class GenAiHandler {

  String urlTemp = "http://10.169.242.230:5000/";

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
      switch (docType) {
        case "Requirement doc":
          docTypeApi = "embed_requirement_documents";
          break;
        case "Reference doc":
          docTypeApi = "embed_reference_documents";
          break;
        case "Guideline doc":
          docTypeApi = "embed_guideline_documents";
          break;
        default:
          break;
      }

      String urlFinal = urlTemp + docTypeApi;


      // URL of the API endpoint
      URL url = new URL(urlFinal);

      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set request method to POST
      connection.setRequestMethod("POST");

      // Set headers
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");

      // Enable output for the request body
      connection.setDoOutput(true);

//      // Split the file paths using ";" as a delimiter
//      String[] pdfFilePaths = filePaths.toString().split(";");
//
//      // Iterate through each file path
//      for (String path : pdfFilePaths) {
//        File pdfFile = new File(path.trim());
//        if (pdfFile.exists() && pdfFile.isFile()) {
//          // Write the PDF file to the request body
//          try (FileInputStream fis = new FileInputStream(pdfFile); OutputStream os = connection.getOutputStream()) {
//            byte[] buffer = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = fis.read(buffer)) != -1) {
//              os.write(buffer, 0, bytesRead);
//            }
//          }
//        }
//        else {
//          System.err.println("File not found or invalid: " + path);
//        }
//      }

      // Create JSON payload
      // Convert ";" separated file paths to a list
      String[] filePathArray = filePaths.toString().split(";");
      List<String> filePathList = Arrays.asList(filePathArray);

      // Create JSON payload
      String payload = new Gson().toJson(filePathList);

      // Send JSON payload
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = payload.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      // Get the response code
      int responseCode = connection.getResponseCode();

      return responseCode;
      // Close the connection
      // connection.disconnect();
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
