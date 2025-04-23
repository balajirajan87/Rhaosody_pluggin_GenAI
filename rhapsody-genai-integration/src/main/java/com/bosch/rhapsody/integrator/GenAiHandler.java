package com.bosch.rhapsody.integrator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.constants.ProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

/**
 * @author DHP4COB
 */
public class GenAiHandler {

  private Process pythonBackendProcess;
  private String sessionId = null; // Store the session ID
  private IRPApplication rhapsodyApp;

  public static void main(String[] args) {
    GenAiHandler plugin = new GenAiHandler(RhapsodyAppServer.getActiveRhapsodyApplication());
    try {
      plugin.startPythonBackend();
    } catch (ProcessingException e) {
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
    } catch (IOException e) {
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
      ProcessBuilder processBuilder = new ProcessBuilder(Constants.DECRYPT_SCRIPT_PATH, encryptedKey,
          Constants.SECRET_KEY_FILE_PATH);
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
    } catch (Exception e) {
      throw new ProcessingException("Error decrypting the API key: " + e.getMessage());
    } finally {
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
      // String apiKey = readKey();
      // if (apiKey.isEmpty()) {
      // throw new ProcessingException("API key is missing or invalid.");
      // }

      ProcessBuilder processBuilder = new ProcessBuilder("python", Constants.BACKEND_SCRIPT_PATH);
      processBuilder.redirectErrorStream(true);

      // Set environment variables
      Map<String, String> environment = processBuilder.environment();
      // environment.put("OPENAI_API_KEY", apiKey);
      environment.put("OPENAI_API_KEY", System.getenv("OPENAI_API_KEY"));

      pythonBackendProcess = processBuilder.start();

      // Wait for a few seconds to allow the backend to initialize
      try {
        Thread.sleep(2000); // Wait for 2 seconds
      } catch (InterruptedException e) {
        LoggerUtil.error("Thread interrupted while waiting for backend initialization: " + e.getMessage());
        Thread.currentThread().interrupt(); // Restore the interrupted status
      }

      new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonBackendProcess.getInputStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            LoggerUtil.info("[Python Backend] " + line);
          }
        } catch (IOException e) {
          LoggerUtil.error(e.getMessage());
        }
      }).start();

      new Thread(() -> {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonBackendProcess.getErrorStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            LoggerUtil.error("[Python Backend] " + line);
          }
        } catch (IOException e) {
          LoggerUtil.error(e.getMessage());
        }
      }).start();

      return "Background process started";
    } catch (IOException e) {
      throw new ProcessingException("Error starting Python backend: " + e.getMessage());
    }
  }

  String checkConnection() throws ProcessingException {

    try {
      HttpURLConnection connection = (HttpURLConnection) new URL("http://127.0.0.1:5000").openConnection();
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
      } else {
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
    } catch (IOException e) {
      throw new ProcessingException("Server is not running or unreachable: " + e.getMessage());
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
        case "ReferenceCode_Docs": // New case for code files
          docTypeApi = "embed_code_documents";
          break;
        case "Guideline_Docs":
          docTypeApi = "embed_guideline_documents";
          break;
        default:
          LoggerUtil.error("Invalid docType: " + docType);
          return "00: Invalid document type"; // Bad Request
      }

      jsonInputString = "[\"" + docType + "\"]";

      String urlFinal = Constants.urlTemp + docTypeApi;

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
      } else {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8"))) {
          String line;
          while ((line = reader.readLine()) != null) {
            response.append(line);
          }
        }
      }

      // Close the connection
      connection.disconnect();

      return responseCode + ": " + response.toString().trim();
    } catch (SocketTimeoutException e) {
      throw new ProcessingException("Connection timed out: " + e.getMessage());
    } catch (Exception e) {
      throw new ProcessingException(e.getMessage());
    }
  }

  // Method to generate a new session ID
  private void generateSessionId(boolean reset) {
    if (reset || sessionId == null) {
      sessionId = UUID.randomUUID().toString(); // Generate a unique session ID
      // LoggerUtil.info("Generated new session ID: " + sessionId);
    }
  }

  public String sendRequestToBackend(String docType, String message) throws ProcessingException {
    try {
      // Reset session ID only for summarize_requirements
      boolean resetSession = "summarize_requirements".equals(docType);
      generateSessionId(resetSession); // Generate a new session ID if needed
      String jsonInputString = "";
      String queryKey = "";
      switch (docType) {
        case "summarize_requirements":
          queryKey = "feature_query";
          break;
        case "extract_design_information":
          queryKey = "task_input";
          break;
        case "extract_code_information":
          queryKey = "task_input";
          break;
        case "create_uml_design":
          queryKey = "task_input";
          break;
        default:
          LoggerUtil.error("Invalid docType: " + docType);
          return "Invalid request type"; // Bad Request
      }

      jsonInputString = "{\"" + queryKey + "\": \"" + message + "\", \"session_id\": \"" + sessionId + "\"}";

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
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // Extract the "message" field
            String res = jsonNode.get("requirements_summary").asText();
            if (res == null || res.isEmpty()) {
              return responseBody;
            }
            return res;
          } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            LoggerUtil.error("Response is not in valid JSON format: " + e.getMessage());
            return responseBody; // Return the raw response if not JSON
          }
        }
      }

      // Close the connection
      connection.disconnect();

      return "couldn't fetch related data";
      
    } catch (SocketTimeoutException e) {
      throw new ProcessingException("Connection timed out: " + e.getMessage());
    } catch (Exception e) {
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
