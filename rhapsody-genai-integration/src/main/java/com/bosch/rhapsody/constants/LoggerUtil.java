package com.bosch.rhapsody.constants;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.telelogic.rhapsody.core.IRPApplication;

public class LoggerUtil {

  private static final String LOG_FILE = "application.log"; // Log file name
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static IRPApplication rhapsodyApp;
  private static String path;

  /**
   * @param rhapsodyApp the rhapsodyApp to set
   */
  public static void setRhapsodyApp(IRPApplication rhapsodyApp) {
    LoggerUtil.rhapsodyApp = rhapsodyApp;
  }

  /**
   * @param rhapsodyApp the rhapsodyApp to set
   */
  public static void setRootDir(String path) {
    LoggerUtil.path = path;
  }

  /**
   * @param message Log an informational message
   */
  public static void info(String message) {
    log("INFO", message);
  }

  /**
   * @param message Log a warning message
   */
  public static void warn(String message) {
    log("WARN", message);
  }

  /**
   * @param message Log an error message
   */
  public static void error(String message) {
    log("ERROR", message);
  }

  // Core logging method
  private static void log(String level, String message) {
    String timestamp = LocalDateTime.now().format(DATE_FORMAT);
    String logMessage = String.format("[%s] [%s] %s", timestamp, level, message);

    // Print to console
    System.out.println(logMessage);

    rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN, logMessage  + Constants.NEW_LINE);

    // getLogFile(LoggerUtil.path+File.separator+LOG_FILE);

    // // Write to log file
    // try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
    // writer.write(logMessage + System.lineSeparator());
    // }
    // catch (IOException e) {
    // System.err.println("Failed to write to log file: " + e.getMessage());
    // }
  }

  private static void getLogFile(String string) {
    File chatLogFile = new File(string);
    if (chatLogFile.exists()) {
      try (java.io.PrintWriter writer = new java.io.PrintWriter(chatLogFile)) {
        writer.print("");
        LoggerUtil.info("Chat log file content cleared: " + string);
      } catch (Exception e) {
        LoggerUtil.error("Failed to clear the chat log file content: " + e.getMessage());
      }
    } else {
      try {
        if (chatLogFile.createNewFile()) {
          LoggerUtil.info("Chat log file created: " + string);
        } else {
          LoggerUtil.error("Failed to create chat log file: " + string);
        }
      } catch (Exception e) {
        LoggerUtil.error("Error while creating chat log file: " + e.getMessage());
      }
    }
  }
}