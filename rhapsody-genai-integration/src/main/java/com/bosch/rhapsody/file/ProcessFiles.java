package com.bosch.rhapsody.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import com.bosch.rhapsody.constants.LoggerUtil;

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
   * @param pdfFilePath   The full file path of the PDF file (e.g., "/path/to/document.pdf").
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
      }
      catch (IOException e) {
        e.printStackTrace();
        LoggerUtil.error("Failed to copy file: " + sourceFile.getName());
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
    }
    catch (IOException e) {
      LoggerUtil.error("Failed to delete directory: " + directoryPath);
      e.printStackTrace();
    }
  }

}