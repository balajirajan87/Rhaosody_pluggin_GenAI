package com.bosch.rhapsody.constants;

import com.telelogic.rhapsody.core.IRPApplication;

/**
 * @author DHP4COB
 */
public class Constants {

  public static final String VERSION = " v1.2.0_2025-04-25";

  public static String[] options = new String[] { "Requirement_Docs", "Reference_Docs", "ReferenceCode_Docs", "Guideline_Docs" };

  public static String[] requestType = new String[] { "summarize_requirements", "extract_design_information", "extract_code_information", "create_uml_design" };

  public static String ROOTDIR;

  public static String urlTemp = "http://127.0.0.1:5000/";

  public static String API_KEY_FILE_PATH = "";
  public static String DECRYPT_SCRIPT_PATH = "";
  public static String BACKEND_SCRIPT_PATH = "";
  public static String SECRET_KEY_FILE_PATH = "";
  public static String CHAT_LOG_FILE_PATH ="";
  public static String PUML_PARSER_PATH = "";
  
  public static String PROFILEPATH;

  public static String userMessageDiagramType;

  public static IRPApplication rhapsodyApp;

}
