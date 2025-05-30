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
import com.bosch.rhapsody.generator.*;
 
public class PUMLParser {
 
     public void generatePUML(String chatContent,Shell shell,String diagramType) throws IOException {
        if (!chatContent.isEmpty() && chatContent.contains("@startuml") && chatContent.contains("@enduml")) {
            String inputFile ="C:\\temp\\GenAI\\chatContent.puml";
            String outputFile = "C:\\temp\\GenAI\\generatedJson.json";
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
                    Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","ERROR: Python parser failed with exit code " + exitCode);
                    throw new IOException("Python parser failed with exit code " + exitCode);
                }
                if (!Files.exists(Paths.get(outputFile))) {
                    Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","ERROR: Output file was not generated: " + outputFile);
                    throw new IOException("Output file was not generated: " + outputFile);
                }
                if (!diagramType.isEmpty() && diagramType.toLowerCase().contains("class")) {
                    ClassDiagram diagramHandler = new ClassDiagram();
                    diagramHandler.createClassDiagram(outputFile,shell);
                }          
            }
            catch (IOException io) {
                Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","ERROR: IO exception while generating class diagram"+io.getMessage());
                throw io;
            }
            catch (Exception e) {
                Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","ERROR: Error while generating class diagram"+e.getMessage());
                throw new RuntimeException(e);
            }
        }
        else {
            Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","PUML not found, Make sure valid PUML exist in chat window");
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
            getParentDir(outputFilePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(lastBlock);
            }
        } else {
            Constants.rhapsodyApp .writeToOutputWindow("GenAIPlugin","ERROR: No @startuml ... @enduml block found.");
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