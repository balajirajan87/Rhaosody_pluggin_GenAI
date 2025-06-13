package com.bosch.rhapsody.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.generator.*;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;


public class PUMLParser {

    public static void main(String[] args) throws IOException {
        IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
        Constants.rhapsodyApp = app;
        Display display = new Display();
        Shell shell = new Shell(display);
        //new PUMLParser().generateJsonFromPuml("@startuml.....@enduml", shell, "classdiagram");
        //String outputFile = "";
        //ClassDiagram diagramHandler = new ClassDiagram();
        //diagramHandler.createClassDiagram(outputFile,shell);
        String outputFileActivity = "";
        ActivityDiagram diagramHandler = new ActivityDiagram();
        diagramHandler.createActivityDiagram(outputFileActivity,shell);
    }

    public void generateJsonFromPuml(String chatContent, Shell shell, String diagramType) throws IOException {
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
                createDiagram(shell, diagramType, outputFile);
                if (pythonBackendProcess != null && pythonBackendProcess.isAlive()) {
                    pythonBackendProcess.destroy();
                }
            } catch (IOException io) {
                throw io;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                    "PUML not found, Make sure valid PUML exist in chat window.\n");
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            messageBox.setMessage("PUML not found, Make sure valid PUML exist in chat window");
            messageBox.open();
        }
    }

    private void createDiagram(Shell shell, String diagramType, String outputFile) {
        if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("class")) {
            ClassDiagram diagramHandler = new ClassDiagram();
            diagramHandler.createClassDiagram(outputFile, shell);
        }else if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("activity")) {
             ActivityDiagram diagramHandler = new ActivityDiagram();
             diagramHandler.createActivityDiagram(outputFile,shell);
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
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin", "ERROR: No @startuml ... @enduml block found.");
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