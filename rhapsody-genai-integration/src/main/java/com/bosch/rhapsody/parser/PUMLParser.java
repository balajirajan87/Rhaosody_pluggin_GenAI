package com.bosch.rhapsody.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.generator.*;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;
import com.bosch.rhapsody.util.PlantUMLValidator;

public class PUMLParser {

    public static void main(String[] args) throws IOException {
        IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
        Constants.rhapsodyApp = app;
        Constants.project = Constants.rhapsodyApp.activeProject();
        Constants.PROFILEPATH = "";
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
        ComponentDiagram componentDiagram=new ComponentDiagram();
        componentDiagram.createComponentDiagram("C:\\MyDir\\01_Common\\02_CrowdSourc\\Rhapsody_GenAI\\repo\\Rhapsody_Pluggin_UML_Designs_to_Project_Window\\Rhaosody_pluggin_GenAI\\puml-parser-py\\data\\processed\\component.json", 1, false);
    }

    public void generateJsonFromPuml(String chatContent, String diagramType) throws IOException {
        if (!chatContent.isEmpty() && chatContent.contains("@startuml") && chatContent.contains("@enduml")) {
            String outputFile = "C:\\temp\\GenAI\\generatedJson.json";
            List<String> inputFiles = extractAllPumlBlocks(chatContent);
            int fileCount = 1;
            Boolean hasMultipleFiles = false;
            if(inputFiles.size() > 1)
                hasMultipleFiles = true;
            for(String inputFile:inputFiles){
                try {
                    boolean isValid = PlantUMLValidator.isValidPuml(inputFile);
                    if(isValid){
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
                        createDiagram(diagramType, outputFile,fileCount,hasMultipleFiles);
                        fileCount++;
                        if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
                            pythonBackendProcess.destroy();
                        }
                    }     
                } catch (IOException io) {
                   Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error creating diagram " + io.getMessage()+ Constants.NEW_LINE);
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error creating diagram " + e.getMessage()+ Constants.NEW_LINE);
                }
            }  
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                "INFO: Diagram generation completed." + Constants.NEW_LINE);
            UiUtil.showInfoPopup(
                "Diagram generation completed. \n\nTo view the generated diagram in Rhapsody, please close the close the Chat UI.\n");     
        } else {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: PUML not found, Make sure valid PUML exist in chat window." + Constants.NEW_LINE);
            UiUtil.showErrorPopup("PUML not found, Make sure valid PUML exist in chat window");
        }
    }

    private void createDiagram(String diagramType, String outputFile,int fileCount,Boolean hasMultipleFiles) {
        ActivityTransitionAdder.swimlane = new HashSet<>();
        ActivityTransitionAdder.AddMergeNode(outputFile);
        if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("class")) {
            ClassDiagram diagramHandler = new ClassDiagram();
            diagramHandler.createClassDiagram(outputFile,fileCount,hasMultipleFiles);
        } else if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("activity")) {
            ActivityDiagram diagramHandler = new ActivityDiagram();
            diagramHandler.createActivityDiagram(outputFile,fileCount,hasMultipleFiles);
        }else if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("component")) {
            ComponentDiagram diagramHandler = new ComponentDiagram();
            diagramHandler.createComponentDiagram(outputFile, fileCount, hasMultipleFiles);
        }
    }

    private List<String> extractAllPumlBlocks(String input) throws IOException {
        int idx = input.lastIndexOf("Response:");
        if (idx == -1) {
            throw new IOException("\"Response:\" not found in input.");
        }
        String afterResponse = input.substring(idx + "Response:".length());
        String outputFilePath = "C:\\temp\\GenAI\\chatContent.puml";
        Pattern pattern = Pattern.compile("@startuml[\\s\\S]*?@enduml", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(afterResponse);
        int count = 0;
        boolean found = false;
        List<String> filePaths = new ArrayList<>();
        while (matcher.find()) {
            found = true;
            String block = matcher.group();
            String thisFilePath = outputFilePath.replace(".puml", "_" + (++count) + ".puml");
            getParentDir(thisFilePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(thisFilePath))) {
                writer.write(block);
            }
            filePaths.add(thisFilePath);
        }
        if (!found) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: No @startuml ... @enduml block found." + Constants.NEW_LINE);
            throw new IOException("No @startuml ... @enduml block found.");
        }
        return filePaths;
    }


    private void getParentDir(String outputFilePath) throws IOException {
        // Ensure parent directories exist
        java.nio.file.Path parentDir = java.nio.file.Paths.get(outputFilePath).getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }
}