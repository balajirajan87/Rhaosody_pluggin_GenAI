package com.bosch.rhapsody.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.generator.*;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

public class PUMLParser {

    public static void main(String[] args) throws IOException {
        IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
        Constants.rhapsodyApp = app;
        Constants.project = Constants.rhapsodyApp.activeProject();
        // new PUMLParser().generateJsonFromPuml("@startuml.....@enduml", shell,
        // "classdiagram");
        // String outputFile = "";
        // ClassDiagram diagramHandler = new ClassDiagram();
        // diagramHandler.createClassDiagram(outputFile);
        // String outputFileActivity = "";
        // ActivityDiagram diagramHandler = new ActivityDiagram();
        // ActivityTransitionAdder.swimlane = new HashSet<>();
        // ActivityTransitionAdder.AddMergeNode(outputFileActivity);
        // diagramHandler.createActivityDiagram(outputFileActivity);
    }

    public void generateJsonFromPuml(String chatContent, String diagramType) throws IOException {
        if (!chatContent.isEmpty() && chatContent.contains("@startuml") && chatContent.contains("@enduml")) {
            String inputFile = "C:\\temp\\GenAI\\chatContent.puml";
            String outputFile = "C:\\temp\\GenAI\\generatedJson.json";
            extractLastPumlBlock(chatContent, inputFile);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        Constants.PUML_PARSER_PATH,
                        "-i", inputFile,
                        "-o", outputFile,
                        "-t", diagramType);
                processBuilder.redirectErrorStream(true);
                Process pythonBackendProcess = processBuilder.start();
                int exitCode = pythonBackendProcess.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Python parser failed with exit code " + exitCode);
                }
                if (!Files.exists(Paths.get(outputFile))) {
                    throw new IOException("Output file was not generated: " + outputFile);
                }
                createDiagram(diagramType, outputFile);
                if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
                    pythonBackendProcess.destroy();
                }
            } catch (IOException io) {
                throw io;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: PUML not found, Make sure valid PUML exist in chat window." + Constants.NEW_LINE);
            UiUtil.showErrorPopup("PUML not found, Make sure valid PUML exist in chat window");
        }
    }

    private void createDiagram(String diagramType, String outputFile) {
        ActivityTransitionAdder.swimlane = new HashSet<>();
        ActivityTransitionAdder.AddMergeNode(outputFile);
        if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("class")) {
            ClassDiagram diagramHandler = new ClassDiagram();
            diagramHandler.createClassDiagram(outputFile);
        } else if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("activity")) {
            ActivityDiagram diagramHandler = new ActivityDiagram();
            diagramHandler.createActivityDiagram(outputFile);
        }
    }

    private void extractLastPumlBlock(String input, String outputFilePath) throws IOException {
        Pattern pattern = Pattern.compile("@startuml[\\s\\S]*?@enduml", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);
        String lastBlock = null;
        while (matcher.find()) {
            lastBlock = matcher.group();
        }
        if (lastBlock != null) {
            getParentDir(outputFilePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(lastBlock);
            }
        } else {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: No @startuml ... @enduml block found." + Constants.NEW_LINE);
            throw new IOException("No @startuml ... @enduml block found.");
        }
    }

    private void getParentDir(String outputFilePath) throws IOException {
        // Ensure parent directories exist
        java.nio.file.Path parentDir = java.nio.file.Paths.get(outputFilePath).getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}