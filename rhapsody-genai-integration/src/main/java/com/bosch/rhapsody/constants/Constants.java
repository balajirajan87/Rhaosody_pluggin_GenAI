package com.bosch.rhapsody.constants;

import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPProject;

/**
 * @author DHP4COB
 */
public class Constants {

        public static final String VERSION = " v1.4.4_2025-06-27";

        public static final String RHP_VERSION = " 10.0.1";

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

        public static IRPProject project;

        public static String[] reqExtension = { "*.pdf" };
        public static String[] refExtension = { "*.pdf" };
        public static String[] refCodeExtension = {
                        "*.c", "*.cpp", "*.h", "*.hpp", "*.xml", "*.arxml", "*.json", "*.py", "*.ipynb", "*.yaml",
                        "*.sh", "*.bat", "*.puml", "*.xmi", "*.md", "*.j2", "*.yml", "*.java" };
        public static String[] guideExtension = { "*.pdf" };

        public static final String LOG_TITLE_GEN_AI_PLUGIN = "GenAIPlugin";

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
        public static final String JSON_END1_MULTIPLICITY = "end1_multiplicity";
        public static final String JSON_END2_MULTIPLICITY = "end2_multiplicity";
        public static final String JSON_VISIBILITY = "visibility";
        public static final String JSON_RETURN_TYPE = "return_type";
        public static final String JSON_DESCRIPTION = "description";
        public static final String JSON_TARGET = "target";
        public static final String JSON_SOURCE = "source";
        public static final String JSON_TITLE = "title";
        public static final String JSON_IMPLEMENTS = "implements";
        public static final String JSON_EXTENDS = "extends";
        public static final String JSON_STEREOTYPE = "stereotype";

        public static final String RHAPSODY_CLASS_DIAGRAM = "ClassDiagram";
        public static final String RHAPSODY_STEREOTYPE = "Stereotype";
        public static final String RHAPSODY_ACTIVITY_DIAGRAM = "ActivityDiagram";
        public static final String RHAPSODY_CLASS_DIAGRAM_STEREOTYPE = "Class Diagram";
        public static final String RHAPSODY_OBJECT_MODEL_DIAGRAM = "ObjectModelDiagram";
        public static final String RHAPSODY_ALL_RELATIONS = "AllRelations";
        public static final String RHAPSODY_POPULATE_MODE = "fromto";
        public static final String RHAPSODY_INTERFACE = "Interface";
        public static final String RHAPSODY_CLASS = "Class";
        public static final String RHAPSODY_OPERATIONS_DISPLAY = "OperationsDisplay";
        public static final String RHAPSODY_ATTRIBUTES_DISPLAY = "AttributesDisplay";
        public static final String RHAPSODY_LITERAL_DISPLAY = "EnumerationLiteralsDisplay";
        public static final String RHAPSODY_DISPLAY_ALL = "All";
        public static final String RHAPSODY_PACKAGE = "Package";
        public static final String RHAPSODY_TYPE = "Type";
        public static final String RHAPSODY_STRUCTURE = "Structure";
        public static final String RHAPSODY_ENUMERATION = "Enumeration";
        public static final String RHAPSODY_COMMENT = "Comment";
        public static final String RHAPSODY_NOTE = "Note";
        public static final String RHAPSODY_ASSOCIATION = "association";
        public static final String RHAPSODY_DIRECTED_ASSOCIATION = "directed_association";
        public static final String RHAPSODY_REVERSE_DIRECTED_ASSOCIATION = "reverse_directed_association";
        public static final String RHAPSODY_DEPENDENCY = "dependency";
        public static final String RHAPSODY_REVERSE_DEPENDENCY = "reverse_dependency";
        public static final String RHAPSODY_DOTTED_DEPENDENCY = "dotted_dependency";
        public static final String RHAPSODY_REALIZATION = "realization";
        public static final String RHAPSODY_REVERSE_REALIZATION = "reverse_realization";
        public static final String RHAPSODY_INHERITANCE = "inheritance";
        public static final String RHAPSODY_REVERSE_INHERITANCE = "reverse_inheritance";
        public static final String RHAPSODY_AGGREGATION = "aggregation";
        public static final String RHAPSODY_COMPOSITION = "composition";

        public static final String RHAPSODY_ASSOCIATION_TYPE = "Association";
        public static final String RHAPSODY_DEPENDENCY_TYPE = "Dependency";
        public static final String RHAPSODY_REALIZATION_TYPE = "Realization";
        public static final String RHAPSODY_AGGREGATION_TYPE = "Aggregation";
        public static final String RHAPSODY_COMPOSITION_TYPE = "Composition";

        public static final String NAME_ONLY = "name_only";
        public static final String NAME = "Name";
        public static final String RECTILINEAR_ARROWS = "rectilinear_arrows";
        public static final String OBJECT_MODEL_GE_REALIZATION_SHOW_NAME = "ObjectModelGe.Realization.ShowName";
        public static final String OBJECT_MODEL_GE_DEPENDS_SHOW_NAME = "ObjectModelGe.Depends.ShowName";
        public static final String OBJECT_MODEL_GE_COMPOSITION_SHOW_NAME = "ObjectModelGe.Composition.ShowName";
        public static final String OBJECT_MODEL_GE_ASSOCIATION_SHOW_NAME = "ObjectModelGe.Association.ShowName";
        public static final String OBJECT_MODEL_GE_AGGREGATION_SHOW_NAME = "ObjectModelGe.Aggregation.ShowName";
        public static final String OBJECT_MODEL_GE_REALIZATION_LINE_STYLE = "ObjectModelGe.Realization.line_style";
        public static final String OBJECT_MODEL_GE_DEPENDS_LINE_STYLE = "ObjectModelGe.Depends.line_style";
        public static final String OBJECT_MODEL_GE_COMPOSITION_LINE_STYLE = "ObjectModelGe.Composition.line_style";
        public static final String OBJECT_MODEL_GE_ASSOCIATION_LINE_STYLE = "ObjectModelGe.Association.line_style";
        public static final String OBJECT_MODEL_GE_AGGREGATION_LINE_STYLE = "ObjectModelGe.Aggregation.line_style";
        public static final String OBJECT_MODEL_GE_CLASS_SHOW_OPERATIONS = "ObjectModelGe.Class.ShowOperations";
        public static final String OBJECT_MODEL_GE_CLASS_SHOW_ATTRIBUTES = "ObjectModelGe.Class.ShowAttributes";
        public static final String OBJECT_MODEL_GE_CLASS_SHOW_NAME = "ObjectModelGe.Class.ShowName";
        public static final String OBJECT_MODEL_GE_TYPE_COMPARTMENTS = "ObjectModelGe.Type.Compartments";


        public static final String NEW_LINE = "\n";
}
