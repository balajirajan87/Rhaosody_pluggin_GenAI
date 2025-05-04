package com.bosch.rhapsody.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import org.eclipse.swt.widgets.List;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.integrator.RhpPlugin;

public class ProcessFiles {

  /**
   * Creates a folder at the specified path if it doesn't already exist.
   *
   * @param folderPath The path of the folder to create.
   * @return True if the folder was created or already exists, false otherwise.
   */
  public boolean createFolder(String folderPath) {
    File folder = new File(folderPath);
    if (!folder.exists()) {
      return folder.mkdirs(); // Creates the folder and any necessary parent directories
    }
    return true; // Folder already exists
  }

  /**
   * Saves a PDF file to the specified file path.
   *
   * @param rootDirectory The root directory where the folder will be created.
   * @param folderName    The name of the folder where the PDF file will be saved.
   * @param pdfFilePath   The full file path of the PDF file (e.g.,
   *                      "/path/to/document.pdf").
   * @return True if the file was successfully saved, false otherwise.
   */
  public void copyPdfFile(String destinationPath, String docType, org.eclipse.swt.widgets.List fileList) {
    for (String filePath : fileList.getItems()) {
      File sourceFile = new File(filePath);
      if (!sourceFile.exists()) {
        LoggerUtil.error("Source file does not exist: " + filePath);
        continue;
      }

      File destinationFolder = new File(destinationPath, docType);
      if (!destinationFolder.exists()) {
        destinationFolder.mkdirs();
      }

      File destinationFile = new File(destinationFolder, sourceFile.getName());
      try {
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LoggerUtil.info("Copied file: " + sourceFile.getName() + " to " + destinationFolder.getAbsolutePath());
      } catch (IOException e) {
        e.printStackTrace();
        LoggerUtil.error("Failed to copy file: " + sourceFile.getName());
      }
    }
  }

  /**
   * Copies files to the specified target directory.
   *
   * @param targetDir The directory where files should be copied.
   * @param docType   The type of document being uploaded.
   * @param fileList  The list of files selected by the user.
   */
  public void copyFiles(String targetDir, String docType, List fileList) {
    // Ensure the target directory exists
    File targetDirectory = new File(targetDir);
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs(); // Create the directory if it doesn't exist
    }

    // Iterate through the selected files and copy them
    for (String filePath : fileList.getItems()) {
      File sourceFile = new File(filePath);
      File destinationFile = new File(targetDirectory, sourceFile.getName());

      try {
        // Copy the file to the target directory
        Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LoggerUtil.info("File copied: " + sourceFile.getName() + " to " + targetDir);
      } catch (IOException e) {
        LoggerUtil.error("Failed to copy file: " + sourceFile.getName() + ". Error: " + e.getMessage());
      }
    }
  }

  public void deleteDirectories(String directoryPath) {
    Path directory = Paths.get(directoryPath);

    try {
      Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          Files.delete(file); // Delete each file
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
          Files.delete(dir); // Delete the directory after its contents
          return FileVisitResult.CONTINUE;
        }
      });
      LoggerUtil.info("Directory deleted successfully: " + directoryPath);
    } catch (IOException e) {
      LoggerUtil.error("Failed to delete directory: " + directoryPath);
      e.printStackTrace();
    }
  }

  public String findFilePath(String baseDir, String fileName, int depth) {
    Path basePath = Paths.get(baseDir);

    try {
      // Search for the file within the specified depth
      Path result = Files.walk(basePath, depth)
          .filter(path -> path.getFileName().toString().equals(fileName)) // Match the file name
          .findFirst() // Get the first match
          .orElseThrow(() -> new RuntimeException("File not found: " + fileName));

      // Return the absolute path as a string
      return result.toAbsolutePath().toString();
    } catch (IOException e) {
      throw new RuntimeException("Error while searching for file: " + fileName, e);
    }
  }

  public void getChatLogFile() {
    File chatLogFile = new File(Constants.CHAT_LOG_FILE_PATH);
    if (chatLogFile.exists()) {
      try (java.io.PrintWriter writer = new java.io.PrintWriter(chatLogFile)) {
        writer.print("");
        LoggerUtil.info("Chat log file content cleared: " + Constants.CHAT_LOG_FILE_PATH);
      } catch (Exception e) {
        LoggerUtil.error("Failed to clear the chat log file content: " + e.getMessage());
      }
    } else {
      try {
        if (chatLogFile.createNewFile()) {
          LoggerUtil.info("Chat log file created: " + Constants.CHAT_LOG_FILE_PATH);
        } else {
          LoggerUtil.error("Failed to create chat log file: " + Constants.CHAT_LOG_FILE_PATH);
        }
      } catch (Exception e) {
        LoggerUtil.error("Error while creating chat log file: " + e.getMessage());
      }
    }
  }

  public boolean validatePaths() {
    // Validate paths
    if (!new File(Constants.BACKEND_SCRIPT_PATH).exists()) {
      LoggerUtil.error("Backend script not found at: " + Constants.BACKEND_SCRIPT_PATH);
      return false;
    }
    if (!new File(Constants.CHAT_LOG_FILE_PATH).exists()) {
      LoggerUtil.error("Log file not found at: " + Constants.CHAT_LOG_FILE_PATH);
      return false;
    }
    if (!new File(Constants.ROOTDIR).exists()) {
      LoggerUtil.error("Directory not found at: " + Constants.ROOTDIR);
      return false;
    }
    if (!new File(Constants.PROFILEPATH).exists()) {
      LoggerUtil.error("Directory not found at: " + Constants.PROFILEPATH);
      return false;
    }
    return true;
  }

  /**
   * @return String
   */
  public String getJarPath() {
    try {
      // Get the location of the JAR file
      File jarFile = new File(RhpPlugin.class.getProtectionDomain().getCodeSource().getLocation().toURI());

      // Return the absolute path of the JAR file
      return jarFile.getParent();
    } catch (URISyntaxException e) {
      LoggerUtil.error(e.getMessage());
      return null;
    }
  }
}