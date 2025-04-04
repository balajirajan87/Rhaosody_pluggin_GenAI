import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

private java.util.List<String> loadedDocuments = new ArrayList<>();

public class GenAIPlugin {
    private IRPApplication rhapsodyApp;
    private Process pythonBackendProcess;

    public static void main(String[] args) {
        GenAIPlugin plugin = new GenAIPlugin();
        plugin.init();
    }

    public void init() {
        // Connect to Rhapsody
        rhapsodyApp = RhapsodyAppServer.getActiveRhapsodyApplication();
        if (rhapsodyApp == null) {
            System.out.println("Rhapsody application not found.");
            return;
        }

        // Start the Python backend
        startPythonBackend();

        // Add a menu item to Rhapsody (alternative approach)
        rhapsodyApp.writeToOutputWindow("GenAIPlugin", "GenAI Plugin initialized. Use the menu to generate UML diagrams.");
    }

    private void startPythonBackend() {
        try {
            // Command to start the Python backend
            String pythonCommand = "python"; // Use "python3" if required
            String backendScript = "../../../openai.py"; // Replace with the actual path to your Python script

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to start Python backend: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadDocuments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        int result = fileChooser.showOpenDialog(null);
    
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File file : selectedFiles) {
                loadedDocuments.add(file.getAbsolutePath());
            }
            JOptionPane.showMessageDialog(null, "Documents loaded successfully.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    public void embedRequirementDocuments() {
        if (loadedDocuments.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No documents loaded. Please load documents first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        try {
            // Backend URL
            String backendUrl = "http://localhost:5000/embed_requirement_documents";
    
            // Create HTTP connection
            URL url = new URL(backendUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
    
            // Create JSON payload
            String payload = new Gson().toJson(loadedDocuments);
    
            // Send JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
    
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JOptionPane.showMessageDialog(null, " Requirement Documents embedded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to embed Requirement documents. Backend returned error: " + responseCode, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void embedReferenceDocuments() {
        if (loadedDocuments.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No documents loaded. Please load documents first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        try {
            // Backend URL
            String backendUrl = "http://localhost:5000/embed_reference_documents";
    
            // Create HTTP connection
            URL url = new URL(backendUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
    
            // Create JSON payload
            String payload = new Gson().toJson(loadedDocuments);
    
            // Send JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
    
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JOptionPane.showMessageDialog(null, " Reference Documents embedded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to embed Reference documents. Backend returned error: " + responseCode, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void embedGuideLineDocuments() {
        if (loadedDocuments.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No documents loaded. Please load documents first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    
        try {
            // Backend URL
            String backendUrl = "http://localhost:5000/embed_guideline_documents";
    
            // Create HTTP connection
            URL url = new URL(backendUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
    
            // Create JSON payload
            String payload = new Gson().toJson(loadedDocuments);
    
            // Send JSON payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
    
            // Read response
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JOptionPane.showMessageDialog(null, " guideline Documents embedded successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to embed guideline documents. Backend returned error: " + responseCode, "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void generateUMLDesign() {
        try {
            // Prompt user to input requirement text
            String requirementText = JOptionPane.showInputDialog(null, "Enter Requirement Text:", "Generate UML Design", JOptionPane.PLAIN_MESSAGE);

            if (requirementText == null || requirementText.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Requirement text cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Send the requirement text to the backend and get the UML diagram
            String umlDiagram = sendRequirementToBackend(requirementText);

            if (umlDiagram == null || umlDiagram.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Failed to generate UML diagram. Please check the backend.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Display the UML diagram code
            JOptionPane.showMessageDialog(null, "Generated UML Diagram:\n" + umlDiagram, "UML Diagram", JOptionPane.INFORMATION_MESSAGE);

            // Optionally, add a new diagram to the model
            IRPModelElement activeElement = rhapsodyApp.getSelectedElement();
            if (activeElement != null) {
                IRPModelElement newDiagram = activeElement.addNewAggr("Sequence Diagram", "GeneratedDiagram");
                rhapsodyApp.writeToOutputWindow("GenAIPlugin", "New diagram created: " + newDiagram.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String sendRequirementToBackend(String requirementText) {
        try {
            // Backend URL (update this to match your Python backend's URL)
            String backendUrl = "http://localhost:5000/summarize_requirements";

            // Create HTTP connection
            URL url = new URL(backendUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Create JSON payload
            String payload = String.format("{\"feature_query\": \"%s\", \"uml_type\": \"Sequence Diagram\"}", requirementText);

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
            } else {
                System.out.println("Backend returned error: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
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