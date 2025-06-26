package com.bosch.rhapsody.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bosch.rhapsody.constants.Constants;

public class PlantUMLValidator {

    public static void main(String[] args) {
        Constants.PROFILEPATH = "C:\\MyDir\\01_Common\\02_CrowdSourc\\Rhapsody_GenAI\\repo\\Rhapsody_Pluggin_UML_Designs_to_Project_Window\\Rhaosody_pluggin_GenAI\\rhapsody-genai-integration\\src\\main\\resources";
        System.out.println(isValidPuml("C:\\MyDir\\01_Common\\02_CrowdSourc\\Rhapsody_GenAI\\repo\\Rhapsody_Pluggin_UML_Designs_to_Project_Window\\Rhaosody_pluggin_GenAI\\puml-parser-py\\data\\samples\\activity_13.puml"));
    }

    public static boolean isValidPuml(String inputFile) throws IllegalArgumentException {
        boolean isValid = false;
        String plantumlJarPath = new File(Constants.PROFILEPATH + File.separator + "plantuml.jar").getAbsolutePath();
        try {
            isValid = validatePUML(plantumlJarPath, inputFile);
        } catch (IOException fnfErr) {
            UiUtil.showErrorPopup("File not found: " + fnfErr.getMessage());
        } catch (RuntimeException rtErr) {
            UiUtil.showErrorPopup("PlantUML execution error: " + rtErr.getMessage());
        } catch (Exception e) {
            UiUtil.showErrorPopup("An unexpected error occurred while processing the diagram: " + e.getMessage());
        }
        return isValid;
    }

    public static boolean validatePUML(String plantumlJarPath, String pumlFilePath)
            throws IOException, InterruptedException, RuntimeException, IllegalArgumentException {
        // Check if the PlantUML jar file exists
        File plantumlJarFile = new File(plantumlJarPath);
        if (!plantumlJarFile.exists() || !plantumlJarFile.isFile()) {
            throw new IOException("Error: plantuml.jar not found at " + plantumlJarPath);
        }

        // Check if the PUML file exists
        File pumlFile = new File(pumlFilePath);
        if (!pumlFile.exists() || !pumlFile.isFile()) {
            throw new IOException("Error: PUML file not found at " + pumlFilePath);
        }

        // Prepare the command to execute
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(plantumlJarPath);
        command.add("-debugsvek");
        command.add(pumlFilePath);

        // Execute the command
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        // Capture the output
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();

        // Capture standard output
        new Thread(() -> {
            try (BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Capture error output
        new Thread(() -> {
            try (BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append(System.lineSeparator());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        // Wait for process to complete
        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new InterruptedException(e.getMessage());
        }

        // Handle the output and errors
        if (exitCode != 0) {
            if (errorOutput.length() > 0) {
                throw new RuntimeException(errorOutput.toString().trim());
            }
            if (output.toString().toLowerCase().contains("error")
                    || output.toString().toLowerCase().contains("syntax error")) {
                throw new IllegalArgumentException(output.toString().trim());
            }
        }

        return true; // Validation succeeded
    }
}