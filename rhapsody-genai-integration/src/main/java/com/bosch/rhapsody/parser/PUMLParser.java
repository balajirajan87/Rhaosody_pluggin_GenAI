package com.bosch.rhapsody.parser;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.generator.*;

public class PUMLParser {

     public void generatePUML(String chatContent,Shell shell,String diagramType) throws IOException {
        if (!chatContent.isEmpty() && chatContent.contains("@startuml") && chatContent.contains("@enduml")) {
            String inputFile ="C:\\temp\\classdiagram_2.puml";
            String outputFile = "C:\\temp\\classdiagram.json";
            extractLastPumlBlock(chatContent, inputFile);
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                    Constants.PUML_PARSER_PATH,
                    "-i", inputFile,
                    "-o", outputFile,
                    "-t", "classdiagram"
                );
                processBuilder.redirectErrorStream(true);
                Process pythonBackendProcess = processBuilder.start();
                int exitCode = pythonBackendProcess.waitFor();
                if (exitCode != 0) {
                    LoggerUtil.error("Python parser failed with exit code " + exitCode);
                    throw new IOException("Python parser failed with exit code " + exitCode);
                }
                if (!Files.exists(Paths.get(outputFile))) {
                    LoggerUtil.error("Output file was not generated: " + outputFile);
                    throw new IOException("Output file was not generated: " + outputFile);
                }
                if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("class")) {
                    ClassDiagram diagramHandler = new ClassDiagram();
                    diagramHandler.createClassDiagram(outputFile,shell);
                }           
            } 
            catch (IOException io) {
                LoggerUtil.error("IO exception while generating class diagram"+io.getMessage());
                throw io;
            } 
            catch (Exception e) {
                LoggerUtil.error("Error while generating class diagram"+e.getMessage());
                throw new RuntimeException(e);
            }finally{
                try {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(inputFile));
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(outputFile));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        else {
            LoggerUtil.error("PUML not found, Make sure valid PUML exist in chat window");
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
            messageBox.setMessage("PUML not found, Make sure valid PUML exist in chat window");
            messageBox.open();
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
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(lastBlock);
            }
        } else {
            LoggerUtil.error("No @startuml ... @enduml block found.");
            throw new IOException("No @startuml ... @enduml block found.");   
        }
    }
}