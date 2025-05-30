package com.bosch.rhapsody.constants;

import com.telelogic.rhapsody.core.IRPApplication;

/**
 * @author DHP4COB
 */
public class Constants {

  public static final String VERSION = " v1.3.0_2025-05-30";

  public static String[] options = new String[] { "Requirement_Docs", "Reference_Docs", "ReferenceCode_Docs",
      "Guideline_Docs" };

  public static String[] requestType = new String[] { "summarize_requirements", "extract_design_information",
      "extract_code_information", "create_uml_design" };

  public static String ROOTDIR;

  public static String urlTemp = "http://127.0.0.1:5000/";

  public static String API_KEY_FILE_PATH = "";
  public static String DECRYPT_SCRIPT_PATH = "";
  public static String BACKEND_SCRIPT_PATH = "";
  public static String SECRET_KEY_FILE_PATH = "";
  public static String CHAT_LOG_FILE_PATH = "";
  public static String PUML_PARSER_PATH = "";

  public static String PROFILEPATH;

  public static String userMessageDiagramType;

  public static IRPApplication rhapsodyApp;

  public static String[] reqExtension = { "*.pdf" };
  public static String[] refExtension = { "*.pdf" };
  public static String[] refCodeExtension = {
      "*.c, *.cpp, *.h, *.hpp, *.xml, *.arxml, *.json, *.py, *.ipynb, *.yaml, *.sh, *.bat, *.puml, *.xmi, *.md, *.j2, *.yml, *.java" };
  public static String[] guideExtension = { "*.pdf" };

  public static final String JSON_CLASSES = "classes";
  public static final String JSON_INTERFACES = "interfaces";
  public static final String JSON_ENUMS = "enums";
  public static final String JSON_STRUCTS = "structs";
  public static final String JSON_PACKAGES = "packages";
  public static final String JSON_RELATIONSHIPS = "relationships";
  public static final String JSON_NOTES = "notes";
  public static final String JSON_ATTRIBUTES = "attributes";
  public static final String JSON_METHODS = "methods";
  public static final String JSON_PARAMS = "params";
  public static final String JSON_VALUES = "values";
  public static final String JSON_NAME = "name";
  public static final String JSON_TYPE = "type";
  public static final String JSON_VISIBILITY = "visibility";
  public static final String JSON_RETURN_TYPE = "return_type";
  public static final String JSON_DESCRIPTION = "description";
  public static final String JSON_TARGET = "target";
  public static final String JSON_SOURCE = "source";
  public static final String JSON_TITLE = "title";
  public static final String JSON_IMPLEMENTS = "implements";
  public static final String JSON_EXTENDS = "extends";

  public static final String RHAPSODY_CLASS_DIAGRAM = "ClassDiagram";
  public static final String RHAPSODY_CLASS_DIAGRAM_STEREOTYPE = "Class Diagram";
  public static final String RHAPSODY_OBJECT_MODEL_DIAGRAM = "ObjectModelDiagram";
  public static final String RHAPSODY_ALL_RELATIONS = "AllRelations";
  public static final String RHAPSODY_POPULATE_MODE = "fromto";
  public static final String RHAPSODY_INTERFACE = "Interface";
  public static final String RHAPSODY_CLASS = "Class";
  public static final String RHAPSODY_OPERATIONS_DISPLAY = "OperationsDisplay";
  public static final String RHAPSODY_ATTRIBUTES_DISPLAY = "AttributesDisplay";
  public static final String RHAPSODY_DISPLAY_ALL = "All";
  public static final String RHAPSODY_PACKAGE = "Package";
  public static final String RHAPSODY_TYPE = "Type";
  public static final String RHAPSODY_STRUCTURE = "Structure";
  public static final String RHAPSODY_ENUMERATION = "Enumeration";
  public static final String RHAPSODY_COMMENT = "Comment";
  public static final String RHAPSODY_NOTE = "Note";
  public static final String RHAPSODY_ASSOCIATION = "association";
  public static final String RHAPSODY_DEPENDENCY = "dependency";
  public static final String RHAPSODY_DOTTED_DEPENDENCY = "dotted_dependency";
  public static final String RHAPSODY_REALIZATION = "realization";
  public static final String RHAPSODY_INHERITANCE = "inheritance";
  public static final String RHAPSODY_AGGREGATION = "aggregation";
  public static final String RHAPSODY_COMPOSITION = "composition";

}
