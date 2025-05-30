package com.bosch.rhapsody.generator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.util.RhapsodyUtil;
import com.telelogic.rhapsody.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ClassDiagram {

    private IRPApplication rhapsodyApp;
    private Map<String, IRPModelElement> elementMap = new HashMap<>();
    private IRPCollection elementsToPopulate;
    private String language;
    private Map<IRPModelElement, String> realizations = new HashMap<>();
    private Map<IRPModelElement, String> generalizations = new HashMap<>();
    private Map<IRPComment, String> anchors = new HashMap<>();
    IRPPackage basePackage;

    public static void main(String[] args) throws IOException {
        IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
        Constants.rhapsodyApp = app;
        Display display = new Display();
        Shell shell = new Shell(display);
        Constants.PUML_PARSER_PATH = "C:\\Users\\xav1cob\\Rapsody\\Crowdsourcing\\Rhaosody_pluggin_GenAI\\puml-parser-py\\buildspec\\pumlparser.exe";
        new ClassDiagram().createClassDiagram("", shell);
    }

    public void createClassDiagram(String outputFile,Shell shell){  
        try{
            rhapsodyApp = Constants.rhapsodyApp;
            String jsonString = readJsonFile(outputFile);
            JSONObject json = new JSONObject(jsonString);
            IRPProject project = RhapsodyUtil.getActiveProject(rhapsodyApp);
            language = RhapsodyUtil.getProjectLanguage(project);
            if (language.equals("C")) {
                basePackage = createBasePackage(project, shell);
                if (basePackage == null) {
                    return;
                }
                elementsToPopulate = rhapsodyApp.createNewCollection();

                // Handle root-level elements (outside packages)
                createElements(basePackage, json);

                // Handle packages
                createPackage(json);

                // Handle calls/relations if present
                createRelation(json);

                //Handel implements and extends
                createRealizationsAndGeneralizations();

                //Add notes relations
                createNoteRelation();

                //Create class diagram
                createBDD(basePackage, json);

                LoggerUtil.info("Class Diagram generated successfully");
                MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                messageBox.setMessage("Class Diagram generated successfully");
                messageBox.open();

            } else {
                MessageBox messageBox = new MessageBox(shell, SWT.ERROR | SWT.OK);
                LoggerUtil.error("Expected Project type is C but found " + language);
                messageBox.setMessage("Expected Project type is C but found " + language);
                messageBox.open();
                return;
            } 
        } 
        catch (Exception e) {
            LoggerUtil.error("Error while generating class diagram"+e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
             LoggerUtil.error("Error reading json file.");
            e.printStackTrace();
            return null;
        }
    }

    private IRPPackage createBasePackage(IRPProject project, Shell shell) {
        IRPModelElement newPackage = project.findNestedElementRecursive("ClassDiagram", "Package");
        if (newPackage != null) {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setText("Package Exists");
            messageBox.setMessage("The package 'ClassDiagramElements' already exists. Do you want to overwrite it?");
            LoggerUtil.info("The package 'ClassDiagramElements' already exists.");
            int response = messageBox.open();
            if (response == SWT.YES) {
                newPackage.deleteFromProject();
                return RhapsodyUtil.addPackage(project, "ClassDiagram");
            } else {
                return null;
            }
        } else {
            return RhapsodyUtil.addPackage(project, "ClassDiagram");
        }
    }

    private void createPackage(JSONObject jsonObject) {
        JSONArray packages = jsonObject.optJSONArray(Constants.JSON_PACKAGES);
        if (packages != null) {
            for (int i = 0; i < packages.length(); i++) {
                JSONObject packageObject = packages.getJSONObject(i);
                String packageName = packageObject.getString(Constants.JSON_NAME).replace(" ", "_");
                IRPPackage rhapsodyPackage = RhapsodyUtil.addPackage(basePackage, packageName, basePackage);
                elementMap.put(packageName, rhapsodyPackage);
                createElements(rhapsodyPackage, packageObject);
            }
        }   
    }

    private void createElements(IRPModelElement container, JSONObject obj) {
        // Classes
        JSONArray classes = obj.optJSONArray(Constants.JSON_CLASSES);
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                JSONObject classObject = classes.getJSONObject(i);
                createClass(container, classObject);
            }
        }
        // Interfaces
        JSONArray interfaces = obj.optJSONArray(Constants.JSON_INTERFACES);
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length(); i++) {
                JSONObject interfaceObject = interfaces.getJSONObject(i);
                createInterface(container, interfaceObject);
            }
        }
        // Enums
        JSONArray enums = obj.optJSONArray(Constants.JSON_ENUMS);
        if (enums != null) {
            for (int i = 0; i < enums.length(); i++) {
                JSONObject enumObject = enums.getJSONObject(i);
                createEnum(container, enumObject);
            }
        }
        // Structs
        JSONArray structs = obj.optJSONArray(Constants.JSON_STRUCTS);
        if (structs != null) {
            for (int i = 0; i < structs.length(); i++) {
                JSONObject structObject = structs.getJSONObject(i);
                createStruct(container, structObject);
            }
        }

        // Notes
        JSONArray Notes = obj.optJSONArray(Constants.JSON_NOTES);
        if (Notes != null) {
            for (int i = 0; i < Notes.length(); i++) {
                JSONObject noteObject = Notes.getJSONObject(i);
                createNote(container, noteObject,i);
            }
        }
        
    }

    private void createClass(IRPModelElement container, JSONObject classObject) {
        String className = classObject.getString(Constants.JSON_NAME).replace(" ", "_");
        IRPClass rhapsodyClass = RhapsodyUtil.addClass((IRPPackage)container, className,basePackage);
        elementMap.put(className, rhapsodyClass);
        elementsToPopulate.addItem(rhapsodyClass);
        addAttributesAndMethods(rhapsodyClass, classObject);
        JSONArray implementsArray = classObject.optJSONArray(Constants.JSON_IMPLEMENTS);
        if (implementsArray != null) {
            for (int i = 0; i < implementsArray.length(); i++) {
                String elementName = implementsArray.getString(i);
                realizations.put(rhapsodyClass, elementName);
            }
        }
        JSONArray extendsArray = classObject.optJSONArray(Constants.JSON_EXTENDS);
         if (extendsArray != null) {
            for (int i = 0; i < extendsArray.length(); i++) {
                String elementName = extendsArray.getString(i);
                generalizations.put(rhapsodyClass, elementName);
            }   
        }    
    }

    private void createInterface(IRPModelElement container, JSONObject interfaceObject) {
        String interfaceName = interfaceObject.getString(Constants.JSON_NAME).replace(" ", "_");
        IRPClass rhapsodyInterface = RhapsodyUtil.addInterface((IRPPackage)container,interfaceName,basePackage);
        elementMap.put(interfaceName, rhapsodyInterface);
        elementsToPopulate.addItem(rhapsodyInterface);
        addAttributesAndMethods(rhapsodyInterface, interfaceObject);
        JSONArray implementsArray = interfaceObject.optJSONArray(Constants.JSON_IMPLEMENTS);
        if (implementsArray != null) {
            for (int i = 0; i < implementsArray.length(); i++) {
                String elementName = implementsArray.getString(i);
                realizations.put(rhapsodyInterface, elementName);
            }
        }
        JSONArray extendsArray = interfaceObject.optJSONArray(Constants.JSON_EXTENDS);
         if (extendsArray != null) {
            for (int i = 0; i < extendsArray.length(); i++) {
                String elementName = extendsArray.getString(i);
                generalizations.put(rhapsodyInterface, elementName);
            }   
        } 
    }

    private void addAttributes(IRPModelElement element, JSONObject classOrInterfaceObject) {
        JSONArray attributes = classOrInterfaceObject.optJSONArray(Constants.JSON_ATTRIBUTES);
        if (attributes != null) {
            for (int i = 0; i < attributes.length(); i++) {
                JSONObject attribute = attributes.getJSONObject(i);
                String visibility = attribute.optString(Constants.JSON_VISIBILITY, "public");
                String attrName = attribute.getString(Constants.JSON_NAME).replace(" ", "_");
                String attrType = attribute.optString(Constants.JSON_TYPE, "int");
                if(element instanceof IRPClass){
                    RhapsodyUtil.addAttributeToClass((IRPClass)element, attrName, attrType, visibility);
                }else if(element instanceof IRPType){
                    RhapsodyUtil.addAttributeToType((IRPType)element, attrName, attrType, visibility);
                }
                
            }
        }
    }

    private void addAttributesAndMethods(IRPClass rhapsodyClass, JSONObject classOrInterfaceObject) {
        // Attributes
        addAttributes(rhapsodyClass,classOrInterfaceObject);

        // Methods
        JSONArray methods = classOrInterfaceObject.optJSONArray(Constants.JSON_METHODS);
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                JSONObject method = methods.getJSONObject(i);
                String visibility = method.optString(Constants.JSON_VISIBILITY, "public");
                String methodName = method.getString(Constants.JSON_NAME).replace(" ", "_");
                String returnType = method.optString(Constants.JSON_RETURN_TYPE, "void");
                IRPOperation newOperation = RhapsodyUtil.addOperation(rhapsodyClass, methodName, returnType, visibility);

                JSONArray params = method.optJSONArray(Constants.JSON_PARAMS);
                if (params != null) {
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String paramName = param.getString(Constants.JSON_NAME).replace(" ", "_");
                        String paramType = param.optString(Constants.JSON_TYPE, "Object");
                        RhapsodyUtil.addArgument(newOperation, paramName, paramType);
                    }
                }
            }
        }
    }

    private void createEnum(IRPModelElement container, JSONObject enumObject) {
        String enumName = enumObject.getString(Constants.JSON_NAME).replace(" ", "_");
        IRPType rhapsodyEnum = RhapsodyUtil.addEnum((IRPPackage)container, enumName,basePackage);
        elementMap.put(enumName, rhapsodyEnum);

        JSONArray literals = enumObject.optJSONArray(Constants.JSON_VALUES);
        if (literals != null) {
            for (int i = 0; i < literals.length(); i++) {
                String literal = literals.getString(i);
                RhapsodyUtil.addEnumLiteral(rhapsodyEnum, literal, i);
            }
        }
    }

    private void createStruct(IRPModelElement container, JSONObject structObject) {
        String structName = structObject.getString(Constants.JSON_NAME).replace(" ", "_");
        IRPType rhapsodyStruct = RhapsodyUtil.addStruct((IRPPackage)container, structName,basePackage);
        elementMap.put(structName, rhapsodyStruct);
        addAttributes(rhapsodyStruct,structObject);
    }

    private void createNote(IRPModelElement container, JSONObject noteObject,int index) {
        String noteText = noteObject.optString(Constants.JSON_DESCRIPTION, "");
        String target = noteObject.optString(Constants.JSON_TARGET, "");
        IRPComment comment = (IRPComment) container.addNewAggr("Comment", "Comment_"+String.valueOf(index));
        comment.setDescription(noteText);
        anchors.put(comment, target);
        elementsToPopulate.addItem(comment);
    }

    private void createNoteRelation(){
        for (Entry<IRPComment, String> entry :anchors.entrySet()) {
            IRPComment fromElem = entry.getKey();
            String targetElement = anchors.get(fromElem);
            if(elementMap.containsKey(targetElement)){
                IRPModelElement toElem = elementMap.get(targetElement);
                fromElem.addAnchor(toElem);
            }
        }
    }

    private void createRelation(JSONObject json) {
        JSONArray calls = json.optJSONArray(Constants.JSON_RELATIONSHIPS);
        if (calls != null) {
            for (int i = 0; i < calls.length(); i++) {
                JSONObject call = calls.getJSONObject(i);
                String from = call.getString(Constants.JSON_SOURCE);
                String to = call.getString(Constants.JSON_TARGET);
                String type = call.optString(Constants.JSON_TYPE, "association");
                String description = call.getString(Constants.JSON_DESCRIPTION);
                IRPModelElement fromElem = elementMap.get(from);
                IRPModelElement toElem = elementMap.get(to);
                if (fromElem instanceof IRPClass && toElem instanceof IRPClass) {
                    if ("association".equals(type)) {
                        RhapsodyUtil.createAssociation((IRPClass)fromElem, (IRPClass)toElem, description);
                    } else if ("dependency".equals(type) || "dotted_dependency".equals(type)) {
                        RhapsodyUtil.createDependency(fromElem, toElem, description);
                    } else if ("realization".equals(type)) {
                        RhapsodyUtil.createRealization((IRPClass)fromElem, (IRPClassifier)toElem);
                    } else if ("inheritance".equals(type)) {
                        RhapsodyUtil.createInheritance((IRPClass)fromElem, (IRPClassifier)toElem, description);
                    }else if("aggregation".equals(type)) {
                        RhapsodyUtil.createAggregation((IRPClass)toElem, (IRPClass)fromElem, description);
                    }else if("composition".equals(type)) {
                        RhapsodyUtil.createComposition((IRPClass)fromElem, (IRPClass)toElem, description);
                    }
                }
            }
        }
    }

    private void createRealizationsAndGeneralizations() {
        // Realizations (implements)
        for (Map.Entry<IRPModelElement, String> entry : realizations.entrySet()) {
            IRPModelElement fromElem = entry.getKey();
            String toName = entry.getValue();
            IRPModelElement toElem = elementMap.get(toName);
            if (fromElem instanceof IRPClass && toElem instanceof IRPClassifier) {
                RhapsodyUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
            }
        }
        // Generalizations (extends)
        for (Map.Entry<IRPModelElement, String> entry : generalizations.entrySet()) {
            IRPModelElement fromElem = entry.getKey();
            String toName = entry.getValue();
            IRPModelElement toElem = elementMap.get(toName);
            if (fromElem instanceof IRPClass && toElem instanceof IRPClassifier) {
                RhapsodyUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem, "");
            }
        }
    }

    private void createBDD(IRPPackage basePackage, JSONObject jsonObject) {
        String title = "ClassDiagram";
        Object titleObject = jsonObject.get(Constants.JSON_TITLE);
        if (titleObject != null && titleObject != JSONObject.NULL) {
            title = titleObject.toString();
        }
        IRPObjectModelDiagram diagram = RhapsodyUtil.addClassDiagram(basePackage, title);
        RhapsodyUtil.addStereotype(diagram, "Class Diagram", "ObjectModelDiagram");
        IRPCollection relTypes = RhapsodyUtil.createNewCollection(rhapsodyApp);
        RhapsodyUtil.setCollectionSize(relTypes, 1);
        RhapsodyUtil.setCollectionString(relTypes, 1, "AllRelations");
        RhapsodyUtil.populateDiagram(diagram, elementsToPopulate, relTypes, "fromto");
        for (Object diagramElement : RhapsodyUtil.getGraphicalElements(diagram)) {
            if (diagramElement instanceof IRPGraphNode) {
                IRPGraphElement diagramGraphElement = (IRPGraphElement) diagramElement;
                IRPModelElement element = diagramGraphElement.getModelObject();
                String metaClass = element.getMetaClass();
                if (metaClass.equals("Interface") || metaClass.equals("Class")) {
                    RhapsodyUtil.setGraphicalProperty(diagramGraphElement, "OperationsDisplay", "All");
                    RhapsodyUtil.setGraphicalProperty(diagramGraphElement, "AttributesDisplay", "All");
                }
            }
        }
    }

}