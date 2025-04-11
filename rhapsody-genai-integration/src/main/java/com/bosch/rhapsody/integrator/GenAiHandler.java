package com.bosch.rhapsody.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.constants.ProcessingException;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

/**
 * @author DHP4COB
 */
public class GenAiHandler {


  private Process pythonBackendProcess;

  private IRPApplication rhapsodyApp;

  public static void main(String[] args) {
    GenAiHandler plugin = new GenAiHandler(RhapsodyAppServer.getActiveRhapsodyApplication());
    try {
      plugin.startPythonBackend();
    }
    catch (ProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    // plugin.generateUMLDesign();
  }

  /**
   * @param rhapsodyApp2
   */
  public GenAiHandler(IRPApplication rhapsodyApp2) {
    this.rhapsodyApp = rhapsodyApp2;
  }

  /**
   * Reads the API key from the file and decrypts it using the Python script.
   *
   * @return Decrypted API key or an empty string if an error occurs.
   * @throws ProcessingException
   */
  public String readKey() throws ProcessingException {
    try {
      String encryptedKey = readFileContent(Constants.API_KEY_FILE_PATH);
      if (encryptedKey != null && !encryptedKey.isEmpty()) {
        return decryptKey(encryptedKey);
      }
    }
    catch (IOException e) {
      throw new ProcessingException("Error reading the API key file: " + e.getMessage());
    }
    return "";
  }

  /**
   * Reads the content of a file.
   *
   * @param filePath Path to the file.
   * @return File content as a string.
   * @throws IOException If an error occurs while reading the file.
   */
  private String readFileContent(String filePath) throws IOException {
    return new String(Files.readAllBytes(Paths.get(filePath)));
  }

  /**
   * Decrypts the given encrypted key using the Python script.
   *
   * @param encryptedKey Encrypted API key.
   * @return Decrypted API key or an empty string if an error occurs.
   */
  private String decryptKey(String encryptedKey) throws ProcessingException {
    Process process = null;
    try {
      // Build the process to execute the decrypt script
      ProcessBuilder processBuilder =
          new ProcessBuilder(Constants.DECRYPT_SCRIPT_PATH, encryptedKey, Constants.SECRET_KEY_FILE_PATH);
      processBuilder.redirectErrorStream(true);

      // Start the process
      process = processBuilder.start();

      // Read the output from the process
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line);
        }

        // Wait for the process to complete and check the exit code
        int exitCode = process.waitFor();
        if (exitCode != 0) {
          throw new ProcessingException("Decrypt script exited with error code: " + exitCode + ". " + output);
        }

        return output.toString(); // Return the decrypted key
      }
    }
    catch (Exception e) {
      throw new ProcessingException("Error decrypting the API key: " + e.getMessage());
    }
    finally {
      if (process != null) {
        process.destroy(); // Ensure the process is terminated
      }
    }
  }

  /**
   * Starts the Python backend process with the decrypted API key.
   * 
   * @return
   */
  public String startPythonBackend() throws ProcessingException {
    try {
      String apiKey = readKey();
      if (apiKey.isEmpty()) {
        throw new ProcessingException("API key is missing or invalid.");
      }

      ProcessBuilder processBuilder = new ProcessBuilder("python", Constants.BACKEND_SCRIPT_PATH);
      processBuilder.redirectErrorStream(true);

      // Set environment variables
      Map<String, String> environment = processBuilder.environment();
      environment.put("OPENAI_API_KEY", apiKey);

      pythonBackendProcess = processBuilder.start();

      logBackendOutput(pythonBackendProcess);

      return checkConnection();
    }
    catch (IOException e) {
      throw new ProcessingException("Error starting Python backend: " + e.getMessage());
    }
  }

  String checkConnection() throws ProcessingException {

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL(Constants.urlTemp).openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(10000); // 5 seconds timeout
      connection.connect();

      // Get the response code
      int responseCode = connection.getResponseCode();

      // Read the response body
      BufferedReader reader;
      if (responseCode >= 200 && responseCode < 300) {
        // Success response
        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      }
      else {
        // Error response
        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
      }

      StringBuilder responseBody = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        responseBody.append(line).append("\n");
      }
      reader.close();
      return responseBody.toString();
    }
    catch (IOException e) {
      throw new ProcessingException("Server is not running or unreachable: " + e.getMessage());
    }
  }

  /**
   * Logs the output of the Python backend process.
   *
   * @param process The Python backend process.
   */
  private void logBackendOutput(Process process) {
    try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

      String line;

      // Read standard output
      while ((line = outputReader.readLine()) != null) {
        LoggerUtil.info("[Python Backend] " + line);
      }

      // Read error output
      while ((line = errorReader.readLine()) != null) {
        LoggerUtil.error("[Python Backend Error] " + line);
      }

      // Wait for the process to complete with a timeout of 10 seconds
      boolean completed = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
      if (completed) {
        int exitCode = process.exitValue();
        LoggerUtil.info("Process exited with code: " + exitCode);
      }

    }
    catch (IOException e) {
      LoggerUtil.error("Error reading backend output: " + e.getMessage());
    }
    catch (InterruptedException e) {
      LoggerUtil.error("Process was interrupted: " + e.getMessage());
      Thread.currentThread().interrupt(); // Restore interrupted status
    }
  }


  /**
   * @param docType
   * @param filePaths
   * @return int
   * @throws ProcessingException
   */
  public String uploadDocToBackend(String docType, StringBuilder filePaths) throws ProcessingException {
    try {
      String docTypeApi = "";
      String jsonInputString = "";
      switch (docType) {
        case "Requirement_Docs":
          docTypeApi = "embed_requirement_documents";
          break;
        case "Reference_Docs":
          docTypeApi = "embed_reference_documents";
          break;
        case "Guideline_Docs":
          docTypeApi = "embed_guideline_documents";
          break;
        default:
          LoggerUtil.error("Invalid docType: " + docType);
          return "00: Invalid document type"; // Bad Request
      }

      // Construct JSON payload
      // jsonInputString = String.format("{\"docType\": \"%s\", \"filePaths\": [%s]}", docType, filePaths.toString());

      jsonInputString = "[\"" + docType + "\"]";

      String urlFinal = Constants.urlTemp + docTypeApi;
      // LoggerUtil.info("API URL: " + urlFinal);

      // URL of the API endpoint
      URL url = new URL(urlFinal);

      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set request method to POST
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      connection.setDoOutput(true);

      // Send JSON payload
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      connection.setConnectTimeout(60000); // Set connection timeout to 60 seconds
      connection.setReadTimeout(60000);

      // Attempt connection
      int responseCode = connection.getResponseCode();

      StringBuilder response = new StringBuilder();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
          String line;
          while ((line = reader.readLine()) != null) {
            response.append(line);
          }
        }
        LoggerUtil.info("Upload successful.");
      }
      else {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
          String line;
          while ((line = reader.readLine()) != null) {
            response.append(line);
          }
        }
        LoggerUtil.info("Upload unsuccessful.");
      }

      // Close the connection
      connection.disconnect();

      return responseCode + ": " + response.toString().trim();
    }
    catch (SocketTimeoutException e) {
      throw new ProcessingException("Connection timed out: " + e.getMessage());
    }
    catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
  }


  public String sendRequestToBackend(String docType, String message) throws ProcessingException {
    try {

      String jsonInputString = "";
      String queryKey = "";
      switch (docType) {
        case "summarize_requirements":
          queryKey = "feature_query";
          break;
        case "extract_design_information":
          queryKey = "requirements_summary";
          break;
        case "extract_code_information":
          queryKey = "requirements_summary";
          break;
        case "create_uml_design":
          queryKey = "requirements_summary";
          break;
        default:
          LoggerUtil.error("Invalid docType: " + docType);
          return "Invalid request type"; // Bad Request
      }

      // Construct JSON payload
      // jsonInputString = String.format("{\"docType\": \"%s\", \"filePaths\": [%s]}", docType, filePaths.toString());


      jsonInputString = "{\"" + queryKey + "\": \"" + message + "\"}";

      String urlFinal = Constants.urlTemp + docType;
      // LoggerUtil.info("API URL: " + urlFinal);

      // URL of the API endpoint
      URL url = new URL(urlFinal);

      // Open connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();

      // Set request method to POST
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");
      connection.setDoOutput(true);

      // Send JSON payload
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonInputString.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      connection.setConnectTimeout(40000); // Set connection timeout to 40 seconds
      connection.setReadTimeout(40000);

      // Attempt connection
      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
          StringBuilder response = new StringBuilder();
          String line;
          while ((line = reader.readLine()) != null) {
            response.append(line);
          }
          String responseBody = response.toString();
          try {
            // Check if the response is in JSON format
            com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            Object json = objectMapper.readValue(responseBody, Object.class);
            // Format the JSON response
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
          }
          catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LoggerUtil.error("Response is not in valid JSON format: " + e.getMessage());
            return responseBody; // Return the raw response if not JSON
          }
        }
      }

      // Close the connection
      connection.disconnect();

      return "couldn't fetch related data";
    }
    catch (SocketTimeoutException e) {
      throw new ProcessingException("Connection timed out: " + e.getMessage());
    }
    catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  /**
   * void
   */
  public void shutdown() {
    // Stop the Python backend when the plugin is closed
    if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
      pythonBackendProcess.destroy();
      LoggerUtil.info("Python backend stopped.");
    }
  }

}
