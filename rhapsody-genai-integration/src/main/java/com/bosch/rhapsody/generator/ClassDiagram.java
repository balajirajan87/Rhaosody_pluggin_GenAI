package com.bosch.rhapsody.generator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
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

    public void createClassDiagram(String outputFile, Shell shell) {
        try {
            rhapsodyApp = Constants.rhapsodyApp;
            String jsonString = readJsonFile(outputFile);
            if (jsonString == null)
                return;
            JSONObject json = new JSONObject(jsonString);
            IRPProject project = RhapsodyUtil.getActiveProject(rhapsodyApp);
            if (project == null) {
                rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Rhapsody project not found.Hence diagram will not be generated");
            }
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

                // Handle implements and extends
                createRealizationsAndGeneralizations();

                // Add notes relations
                createNoteRelation();

                // Create class diagram
                createBDD(basePackage, json);

                rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nClass Diagram generated successfully");
                MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
                messageBox.setMessage("Class Diagram generated successfully");
                messageBox.open();

            } else {
                MessageBox messageBox = new MessageBox(shell, SWT.ERROR | SWT.OK);
                rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nExpected Rhapsody project type is \"C\" but found \""
                        + language + "\". Hence diagram will not be generated.");
                messageBox.setMessage("Expected Rhapsody project type is \"C\" but found \"" + language
                        + "\". Hence diagram will not be generated.");
                messageBox.open();
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                java.nio.file.Path outputPath = java.nio.file.Paths.get(outputFile);
                if (java.nio.file.Files.exists(outputPath)) {
                    java.nio.file.Files.delete(outputPath);
                }
            } catch (Exception ex) {
                rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nERROR: Could not delete output file: " + ex.getMessage());
            }
        }
    }

    private String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nError reading json file.");
            e.printStackTrace();
            return null;
        }
    }

    private IRPPackage createBasePackage(IRPProject project, Shell shell) {
        IRPModelElement newPackage = project.findNestedElementRecursive(Constants.RHAPSODY_CLASS_DIAGRAM,
                Constants.RHAPSODY_PACKAGE);
        if (newPackage != null) {
            newPackage.locateInBrowser();
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setText("Package Exists");
            messageBox.setMessage("The package '" + Constants.RHAPSODY_CLASS_DIAGRAM
                    + "' already exists. Do you want to overwrite it?");
            rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                    "\nThe package '" + Constants.RHAPSODY_CLASS_DIAGRAM + "' already exists.");
            int response = messageBox.open();
            if (response == SWT.YES) {
                newPackage.deleteFromProject();
                return RhapsodyUtil.addPackage(project, Constants.RHAPSODY_CLASS_DIAGRAM);
            } else {
                return null;
            }
        } else {
            return RhapsodyUtil.addPackage(project, Constants.RHAPSODY_CLASS_DIAGRAM);
        }
    }

    private void createPackage(JSONObject jsonObject) {
        JSONArray packages = jsonObject.optJSONArray(Constants.JSON_PACKAGES);
        if (packages != null) {
            for (int i = 0; i < packages.length(); i++) {
                try {
                    JSONObject packageObject = packages.getJSONObject(i);
                    String packageName = packageObject.getString(Constants.JSON_NAME).replace(" ", "_");
                    if (packageName != null) {
                        IRPPackage rhapsodyPackage = RhapsodyUtil.addPackage(basePackage, packageName, basePackage);
                        if (rhapsodyPackage != null) {
                            elementMap.put(packageName, rhapsodyPackage);
                            createElements(rhapsodyPackage, packageObject);
                        }
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating package " + e.getMessage());
                }

            }
        }
    }

    private void createElements(IRPModelElement container, JSONObject obj) {
        // Classes
        createClass(container, obj);

        // Interfaces
        createInterface(container, obj);

        // Enums
        createEnum(container, obj);

        // Structs
        createStruct(container, obj);

        // Notes
        createNote(container, obj);

    }

    private void createClass(IRPModelElement container, JSONObject obj) {
        JSONArray classes = obj.optJSONArray(Constants.JSON_CLASSES);
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                try {
                    JSONObject classObject = classes.getJSONObject(i);
                    String className = classObject.getString(Constants.JSON_NAME).replace(" ", "_");
                    IRPClass rhapsodyClass = RhapsodyUtil.addClass((IRPPackage) container, className, basePackage);
                    if (rhapsodyClass != null) {
                        elementMap.put(className, rhapsodyClass);
                        elementsToPopulate.addItem(rhapsodyClass);
                        addAttributesAndMethods(rhapsodyClass, classObject);
                        setRealizationsAndGeneralizations(rhapsodyClass, classObject);
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating class " + e.getMessage());
                }
            }
        }

    }

    private void createInterface(IRPModelElement container, JSONObject obj) {
        JSONArray interfaces = obj.optJSONArray(Constants.JSON_INTERFACES);
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length(); i++) {
                try {
                    JSONObject interfaceObject = interfaces.getJSONObject(i);
                    String interfaceName = interfaceObject.getString(Constants.JSON_NAME).replace(" ", "_");
                    IRPClass rhapsodyInterface = RhapsodyUtil.addInterface((IRPPackage) container, interfaceName,
                            basePackage);
                    if (rhapsodyInterface != null) {
                        elementMap.put(interfaceName, rhapsodyInterface);
                        elementsToPopulate.addItem(rhapsodyInterface);
                        addAttributesAndMethods(rhapsodyInterface, interfaceObject);
                        setRealizationsAndGeneralizations(rhapsodyInterface, interfaceObject);
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating interface " + e.getMessage());
                }
            }
        }

    }

    private void setRealizationsAndGeneralizations(IRPModelElement element, JSONObject jsonObject) {
        JSONArray implementsArray = jsonObject.optJSONArray(Constants.JSON_IMPLEMENTS);
        if (implementsArray != null) {
            for (int i = 0; i < implementsArray.length(); i++) {
                String elementName = implementsArray.getString(i);
                realizations.put(element, elementName);
            }
        }
        JSONArray extendsArray = jsonObject.optJSONArray(Constants.JSON_EXTENDS);
        if (extendsArray != null) {
            for (int i = 0; i < extendsArray.length(); i++) {
                String elementName = extendsArray.getString(i);
                generalizations.put(element, elementName);
            }
        }
    }

    private void addAttributes(IRPModelElement element, JSONObject classOrInterfaceObject) {
        JSONArray attributes = classOrInterfaceObject.optJSONArray(Constants.JSON_ATTRIBUTES);
        if (attributes != null) {
            for (int i = 0; i < attributes.length(); i++) {
                try {
                    JSONObject attribute = attributes.getJSONObject(i);
                    String visibility = attribute.optString(Constants.JSON_VISIBILITY, "public");
                    String attrName = attribute.getString(Constants.JSON_NAME).replace(" ", "_");
                    String attrType = attribute.optString(Constants.JSON_TYPE, "int");
                    if (element instanceof IRPClass) {
                        RhapsodyUtil.addAttributeToClass((IRPClass) element, attrName, attrType, visibility);
                    } else if (element instanceof IRPType) {
                        RhapsodyUtil.addAttributeToType((IRPType) element, attrName, attrType, visibility);
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating attributes " + e.getMessage());
                }
            }
        }
    }

    private void addAttributesAndMethods(IRPClass rhapsodyClass, JSONObject classOrInterfaceObject) {
        // Attributes
        addAttributes(rhapsodyClass, classOrInterfaceObject);

        // Methods
        JSONArray methods = classOrInterfaceObject.optJSONArray(Constants.JSON_METHODS);
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                try {
                    JSONObject method = methods.getJSONObject(i);
                    String visibility = method.optString(Constants.JSON_VISIBILITY, "public");
                    String methodName = method.getString(Constants.JSON_NAME).replace(" ", "_");
                    String returnType = method.optString(Constants.JSON_RETURN_TYPE, "void");
                    IRPOperation newOperation = RhapsodyUtil.addOperation(rhapsodyClass, methodName, returnType,
                            visibility);
                    JSONArray params = method.optJSONArray(Constants.JSON_PARAMS);
                    if (params != null) {
                        for (int j = 0; j < params.length(); j++) {
                            JSONObject param = params.getJSONObject(j);
                            String paramName = param.getString(Constants.JSON_NAME).replace(" ", "_");
                            String paramType = param.optString(Constants.JSON_TYPE, "Object");
                            RhapsodyUtil.addArgument(newOperation, paramName, paramType);
                        }
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating methods " + e.getMessage());
                }
            }
        }
    }

    private void createEnum(IRPModelElement container, JSONObject obj) {
        JSONArray enums = obj.optJSONArray(Constants.JSON_ENUMS);
        if (enums != null) {
            for (int i = 0; i < enums.length(); i++) {
                try {
                    JSONObject enumObject = enums.getJSONObject(i);
                    String enumName = enumObject.getString(Constants.JSON_NAME).replace(" ", "_");
                    IRPType rhapsodyEnum = RhapsodyUtil.addEnum((IRPPackage) container, enumName, basePackage);
                    if (rhapsodyEnum != null) {
                        elementMap.put(enumName, rhapsodyEnum);
                        JSONArray literals = enumObject.optJSONArray(Constants.JSON_VALUES);
                        if (literals != null) {
                            for (int j = 0; j < literals.length(); j++) {
                                String literal = literals.getString(j);
                                RhapsodyUtil.addEnumLiteral(rhapsodyEnum, literal, j);
                            }
                        }
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating enum " + e.getMessage());
                }
            }
        }

    }

    private void createStruct(IRPModelElement container, JSONObject obj) {
        JSONArray structs = obj.optJSONArray(Constants.JSON_STRUCTS);
        if (structs != null) {
            for (int i = 0; i < structs.length(); i++) {
                try {
                    JSONObject structObject = structs.getJSONObject(i);
                    String structName = structObject.getString(Constants.JSON_NAME).replace(" ", "_");
                    IRPType rhapsodyStruct = RhapsodyUtil.addStruct((IRPPackage) container, structName, basePackage);
                    if (rhapsodyStruct != null) {
                        elementMap.put(structName, rhapsodyStruct);
                        addAttributes(rhapsodyStruct, structObject);
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating struct " + e.getMessage());
                }
            }
        }
    }

    private void createNote(IRPModelElement container, JSONObject obj) {
        JSONArray Notes = obj.optJSONArray(Constants.JSON_NOTES);
        if (Notes != null) {
            for (int i = 0; i < Notes.length(); i++) {
                try {
                    JSONObject noteObject = Notes.getJSONObject(i);
                    String noteText = noteObject.optString(Constants.JSON_DESCRIPTION, "");
                    String target = noteObject.optString(Constants.JSON_TARGET, "");
                    IRPComment comment = (IRPComment) container.addNewAggr(Constants.RHAPSODY_COMMENT,
                            Constants.RHAPSODY_COMMENT + "_" + i);
                    if (comment != null) {
                        comment.setDescription(noteText);
                        anchors.put(comment, target);
                        elementsToPopulate.addItem(comment);
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating note " + e.getMessage());
                }
            }
        }

    }

    private void createNoteRelation() {
        for (Entry<IRPComment, String> entry : anchors.entrySet()) {
            try {
                IRPComment fromElem = entry.getKey();
                String targetElement = anchors.get(fromElem);
                if (elementMap.containsKey(targetElement)) {
                    IRPModelElement toElem = elementMap.get(targetElement);
                    fromElem.addAnchor(toElem);
                }
            } catch (Exception e) {
                rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Error while creating note relations " + e.getMessage());
            }
        }
    }

    private void createRelation(JSONObject json) {
        JSONArray calls = json.optJSONArray(Constants.JSON_RELATIONSHIPS);
        if (calls != null) {
            for (int i = 0; i < calls.length(); i++) {
                try {
                    JSONObject call = calls.getJSONObject(i);
                    String from = call.getString(Constants.JSON_SOURCE);
                    String to = call.getString(Constants.JSON_TARGET);
                    String type = call.optString(Constants.JSON_TYPE, Constants.RHAPSODY_ASSOCIATION);
                    String description = call.optString(Constants.JSON_DESCRIPTION, "");
                    IRPModelElement fromElem = elementMap.get(from);
                    IRPModelElement toElem = elementMap.get(to);
                    if (fromElem != null && toElem != null && fromElem instanceof IRPClass
                            && toElem instanceof IRPClass) {
                        if (Constants.RHAPSODY_ASSOCIATION.equals(type)) {
                            RhapsodyUtil.createAssociation((IRPClass) fromElem, (IRPClass) toElem, description);
                        } else if (Constants.RHAPSODY_DIRECTED_ASSOCIATION.equals(type)) {
                            RhapsodyUtil.createDirectedAssociation((IRPClass) fromElem, (IRPClass) toElem, description);
                        } else if (Constants.RHAPSODY_DEPENDENCY.equals(type)
                                || Constants.RHAPSODY_DOTTED_DEPENDENCY.equals(type)) {
                            RhapsodyUtil.createDependency(fromElem, toElem, description);
                        } else if (Constants.RHAPSODY_REALIZATION.equals(type)) {
                            RhapsodyUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
                        } else if (Constants.RHAPSODY_REVERSE_REALIZATION.equals(type)) {
                            RhapsodyUtil.createRealization((IRPClass) toElem, (IRPClassifier) fromElem);
                        } else if (Constants.RHAPSODY_INHERITANCE.equals(type)) {
                            RhapsodyUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem, description);
                        } else if (Constants.RHAPSODY_REVERSE_INHERITANCE.equals(type)) {
                            RhapsodyUtil.createInheritance((IRPClass) toElem, (IRPClassifier) fromElem, description);
                        } else if (Constants.RHAPSODY_AGGREGATION.equals(type)) {
                            RhapsodyUtil.createAggregation((IRPClass) toElem, (IRPClass) fromElem, description);
                        } else if (Constants.RHAPSODY_COMPOSITION.equals(type)) {
                            RhapsodyUtil.createComposition((IRPClass) fromElem, (IRPClass) toElem, description);
                        }
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating relations " + e.getMessage());
                }
            }
        }
    }

    private void createRealizationsAndGeneralizations() {
        // Realizations (implements)
        for (Map.Entry<IRPModelElement, String> entry : realizations.entrySet()) {
            try {
                IRPModelElement fromElem = entry.getKey();
                String toName = entry.getValue();
                IRPModelElement toElem = elementMap.get(toName);
                if (fromElem instanceof IRPClass && toElem instanceof IRPClassifier) {
                    RhapsodyUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
                }
            } catch (Exception e) {
                rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Error while creating realizations " + e.getMessage());
            }
        }
        // Generalizations (extends)
        for (Map.Entry<IRPModelElement, String> entry : generalizations.entrySet()) {
            try {
                IRPModelElement fromElem = entry.getKey();
                String toName = entry.getValue();
                IRPModelElement toElem = elementMap.get(toName);
                if (fromElem instanceof IRPClass && toElem instanceof IRPClassifier) {
                    RhapsodyUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem, "");
                }
            } catch (Exception e) {
                rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Error while creating generalization " + e.getMessage());
            }
        }
    }

    private void createBDD(IRPPackage basePackage, JSONObject jsonObject) {
        try {
            String title = Constants.RHAPSODY_CLASS_DIAGRAM;
            Object titleObject = jsonObject.get(Constants.JSON_TITLE);
            if (titleObject != null && titleObject != JSONObject.NULL) {
                title = titleObject.toString();
            }
            IRPObjectModelDiagram diagram = RhapsodyUtil.addClassDiagram(basePackage, title);
            if (diagram != null) {
                RhapsodyUtil.addStereotype(diagram, Constants.RHAPSODY_CLASS_DIAGRAM_STEREOTYPE,
                        Constants.RHAPSODY_OBJECT_MODEL_DIAGRAM);
                IRPCollection relTypes = RhapsodyUtil.createNewCollection(rhapsodyApp);
                if (relTypes != null) {
                    RhapsodyUtil.setCollectionSize(relTypes, 1);
                    RhapsodyUtil.setCollectionString(relTypes, 1, Constants.RHAPSODY_ALL_RELATIONS);
                    RhapsodyUtil.populateDiagram(diagram, elementsToPopulate, relTypes,
                            Constants.RHAPSODY_POPULATE_MODE);
                    setDiagramProperties(diagram);
                    diagram.openDiagram();
                }
            }
        } catch (Exception e) {
            rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nERROR: Error while creating diagram " + e.getMessage());
        }
    }

    private void setDiagramProperties(IRPObjectModelDiagram diagram) {
        java.util.List<?> graphicalElements = RhapsodyUtil.getGraphicalElements(diagram);
        if (graphicalElements != null) {
            for (Object diagramElement : graphicalElements) {
                try {
                    if (diagramElement instanceof IRPGraphNode) {
                        IRPGraphElement diagramGraphElement = (IRPGraphElement) diagramElement;
                        IRPModelElement element = diagramGraphElement.getModelObject();
                        if (element != null) {
                            String metaClass = element.getMetaClass();
                            if (metaClass.equals(Constants.RHAPSODY_INTERFACE)
                                    || metaClass.equals(Constants.RHAPSODY_CLASS)) {
                                RhapsodyUtil.setGraphicalProperty(diagramGraphElement,
                                        Constants.RHAPSODY_OPERATIONS_DISPLAY, Constants.RHAPSODY_DISPLAY_ALL);
                                RhapsodyUtil.setGraphicalProperty(diagramGraphElement,
                                        Constants.RHAPSODY_ATTRIBUTES_DISPLAY, Constants.RHAPSODY_DISPLAY_ALL);
                            }
                        }
                    }
                } catch (Exception e) {
                    rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                            "\nERROR: Error while creating diagram properties " + e.getMessage());
                }
            }
        }
    }

}