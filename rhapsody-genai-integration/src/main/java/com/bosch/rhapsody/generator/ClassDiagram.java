package com.bosch.rhapsody.generator;

import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.util.ClassDiagramUtil;
import com.bosch.rhapsody.util.CommonUtil;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ClassDiagram {

    private Map<String, IRPModelElement> elementMap = new HashMap<>();
    private IRPCollection elementsToPopulate;
    private Map<IRPModelElement, String> realizations = new HashMap<>();
    private Map<IRPModelElement, String> generalizations = new HashMap<>();
    private Map<IRPComment, String> anchors = new HashMap<>();
    private Map<String, IRPStereotype> stereotypeMap = new HashMap<>();
    IRPPackage basePackage;
    IRPPackage baseStereotypePackage;

    public void createClassDiagram(String outputFile) {
        try {
            String jsonString = readJsonFile(outputFile);
            if (jsonString == null)
                return;
            JSONObject json = new JSONObject(jsonString);

            basePackage = CommonUtil.createBasePackage(Constants.project, Constants.RHAPSODY_CLASS_DIAGRAM);
            if (basePackage == null) {
                return;
            }

            baseStereotypePackage = CommonUtil.createOrGetPackage(Constants.project, Constants.RHAPSODY_STEREOTYPE);
            stereotypeMap = CommonUtil.getStereotypes(baseStereotypePackage);

            elementsToPopulate = Constants.rhapsodyApp.createNewCollection();

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

            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "INFO: Class Diagram generated successfully" + Constants.NEW_LINE);
            UiUtil.showInfoPopup(
                    "Class Diagram generated successfully. \n\nTo view the generated diagram in Rhapsody, please close the close the Chat UI.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                java.nio.file.Path outputPath = java.nio.file.Paths.get(outputFile);
                if (java.nio.file.Files.exists(outputPath)) {
                    java.nio.file.Files.delete(outputPath);
                }
            } catch (Exception ex) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Could not delete output file: " + ex.getMessage() + Constants.NEW_LINE);
            }
        }
    }

    private String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "Error reading json file." + Constants.NEW_LINE);
            e.printStackTrace();
            return null;
        }
    }

    private void addStereotype(IRPModelElement element, String stereotypeName) {
        if (stereotypeName != null) {
            IRPStereotype stereotype = null;
            String metaType = element.getMetaClass();
            stereotype = stereotypeMap.get(stereotypeName);
            if (stereotype == null) {
                stereotype = (IRPStereotype) baseStereotypePackage.addNewAggr(Constants.RHAPSODY_STEREOTYPE,
                        stereotypeName);
                stereotypeMap.put(stereotypeName, stereotype);
            }
            stereotype.addMetaClass(metaType);
            element.addSpecificStereotype(stereotype);
        }
    }

    private void createPackage(JSONObject jsonObject) {
        JSONArray packages = jsonObject.optJSONArray(Constants.JSON_PACKAGES);
        if (packages != null) {
            for (int i = 0; i < packages.length(); i++) {
                try {
                    JSONObject packageObject = packages.getJSONObject(i);
                    String packageName = packageObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    String stereotype = packageObject.has(Constants.JSON_STEREOTYPE)
                            && !packageObject.isNull(Constants.JSON_STEREOTYPE)
                                    ? packageObject.getString(Constants.JSON_STEREOTYPE)
                                    : null;
                    if (packageName != null) {
                        IRPPackage rhapsodyPackage = CommonUtil.createOrGetPackage(basePackage, packageName);
                        if (rhapsodyPackage != null) {
                            addStereotype(rhapsodyPackage, stereotype);
                            elementMap.put(packageName, rhapsodyPackage);
                            createElements(rhapsodyPackage, packageObject);
                        }
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating package " + e.getMessage() + Constants.NEW_LINE);
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
                    String stereotype = classObject.has(Constants.JSON_STEREOTYPE)
                            && !classObject.isNull(Constants.JSON_STEREOTYPE)
                                    ? classObject.getString(Constants.JSON_STEREOTYPE)
                                    : null;
                    if (stereotype != null && ("enum".equals(stereotype.toLowerCase())
                            || "enumeration".equals(stereotype.toLowerCase()))) {
                        createSingleEnum(container, classObject);
                    } else if (stereotype != null && "struct".equals(stereotype.toLowerCase())) {
                        createSingleStruct(container, classObject);
                    } else {
                        String className = classObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                        IRPClass rhapsodyClass = ClassDiagramUtil.addClass((IRPPackage) container, className,
                                basePackage);
                        if (rhapsodyClass != null) {
                            addStereotype(rhapsodyClass, stereotype);
                            elementMap.put(className, rhapsodyClass);
                            elementsToPopulate.addItem(rhapsodyClass);
                            addAttributesAndMethods(rhapsodyClass, classObject);
                            setRealizationsAndGeneralizations(rhapsodyClass, classObject);
                        }
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating class " + e.getMessage() + Constants.NEW_LINE);
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
                    String stereotype = interfaceObject.has(Constants.JSON_STEREOTYPE)
                            && !interfaceObject.isNull(Constants.JSON_STEREOTYPE)
                                    ? interfaceObject.getString(Constants.JSON_STEREOTYPE)
                                    : null;
                    String interfaceName = interfaceObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]",
                            "_");
                    IRPClass rhapsodyInterface = ClassDiagramUtil.addInterface((IRPPackage) container, interfaceName,
                            basePackage);
                    if (rhapsodyInterface != null) {
                        addStereotype(rhapsodyInterface, stereotype);
                        elementMap.put(interfaceName, rhapsodyInterface);
                        elementsToPopulate.addItem(rhapsodyInterface);
                        addAttributesAndMethods(rhapsodyInterface, interfaceObject);
                        setRealizationsAndGeneralizations(rhapsodyInterface, interfaceObject);
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating interface " + e.getMessage() + Constants.NEW_LINE);
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
                    String attrName = attribute.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    String attrType = attribute.optString(Constants.JSON_TYPE, "int");
                    if (element instanceof IRPClass) {
                        ClassDiagramUtil.addAttributeToClass((IRPClass) element, attrName, attrType, visibility);
                    } else if (element instanceof IRPType) {
                        ClassDiagramUtil.addAttributeToType((IRPType) element, attrName, attrType, visibility);
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating attributes " + e.getMessage() + Constants.NEW_LINE);
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
                    String methodName = method.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    String returnType = method.optString(Constants.JSON_RETURN_TYPE, "void");
                    IRPOperation newOperation = ClassDiagramUtil.addOperation(rhapsodyClass, methodName, returnType,
                            visibility);
                    JSONArray params = method.optJSONArray(Constants.JSON_PARAMS);
                    if (params != null) {
                        for (int j = 0; j < params.length(); j++) {
                            JSONObject param = params.getJSONObject(j);
                            String paramName = param.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                            String paramType = param.optString(Constants.JSON_TYPE, "Object");
                            ClassDiagramUtil.addArgument(newOperation, paramName, paramType);
                        }
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating methods " + e.getMessage() + Constants.NEW_LINE);
                }
            }
        }
    }

    private void createSingleEnum(IRPModelElement container, JSONObject enumObject) {
        String enumName = enumObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
        IRPType rhapsodyEnum = ClassDiagramUtil.addEnum((IRPPackage) container, enumName, basePackage);
        if (rhapsodyEnum != null) {
            elementMap.put(enumName, rhapsodyEnum);
            elementsToPopulate.addItem(rhapsodyEnum);
            JSONArray literals = enumObject.optJSONArray(Constants.JSON_VALUES);
            if (literals != null) {
                for (int j = 0; j < literals.length(); j++) {
                    String literal = literals.getString(j);
                    ClassDiagramUtil.addEnumLiteral(rhapsodyEnum, literal, j);
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
                    createSingleEnum(container, enumObject);
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating enum " + e.getMessage() + Constants.NEW_LINE);
                }
            }
        }

    }

    private void createSingleStruct(IRPModelElement container, JSONObject structObject) {
        String structName = structObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
        IRPType rhapsodyStruct = ClassDiagramUtil.addStruct((IRPPackage) container, structName,
                basePackage);
        if (rhapsodyStruct != null) {
            elementMap.put(structName, rhapsodyStruct);
            addAttributes(rhapsodyStruct, structObject);
            elementsToPopulate.addItem(rhapsodyStruct);
        }
    }

    private void createStruct(IRPModelElement container, JSONObject obj) {
        JSONArray structs = obj.optJSONArray(Constants.JSON_STRUCTS);
        if (structs != null) {
            for (int i = 0; i < structs.length(); i++) {
                try {
                    JSONObject structObject = structs.getJSONObject(i);
                    createSingleStruct(container, structObject);
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating struct " + e.getMessage() + Constants.NEW_LINE);
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
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating note " + e.getMessage() + Constants.NEW_LINE);
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
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error while creating note relations " + e.getMessage() + Constants.NEW_LINE);
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
                    String end1_multiplicity = call.optString(Constants.JSON_END1_MULTIPLICITY, "1");
                    String end2_multiplicity = call.optString(Constants.JSON_END2_MULTIPLICITY, "1");

                    IRPModelElement fromElem = elementMap.get(from);
                    IRPModelElement toElem = elementMap.get(to);
                    if (fromElem != null && toElem != null && fromElem instanceof IRPClass
                            && toElem instanceof IRPClass) {
                        if (Constants.RHAPSODY_ASSOCIATION.equals(type)) {
                            ClassDiagramUtil.createAssociation((IRPClass) fromElem, (IRPClass) toElem, description,
                                    end1_multiplicity, end2_multiplicity);
                        } else if (Constants.RHAPSODY_DIRECTED_ASSOCIATION.equals(type)) {
                            ClassDiagramUtil.createDirectedAssociation((IRPClass) fromElem, (IRPClass) toElem,
                                    description, end1_multiplicity);
                        } else if (Constants.RHAPSODY_REVERSE_DIRECTED_ASSOCIATION.equals(type)) {
                            ClassDiagramUtil.createDirectedAssociation((IRPClass) toElem, (IRPClass) fromElem,
                                    description, end2_multiplicity);
                        } else if (Constants.RHAPSODY_DEPENDENCY.equals(type)) {
                            ClassDiagramUtil.createDependency(fromElem, toElem, description);
                        } else if (Constants.RHAPSODY_REVERSE_DEPENDENCY.equals(type)) {
                            ClassDiagramUtil.createDependency(toElem, fromElem, description);
                        } else if (Constants.RHAPSODY_REALIZATION.equals(type)) {
                            ClassDiagramUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
                        } else if (Constants.RHAPSODY_REVERSE_REALIZATION.equals(type)) {
                            ClassDiagramUtil.createRealization((IRPClass) toElem, (IRPClassifier) fromElem);
                        } else if (Constants.RHAPSODY_INHERITANCE.equals(type)) {
                            ClassDiagramUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem,
                                    description);
                        } else if (Constants.RHAPSODY_REVERSE_INHERITANCE.equals(type)) {
                            ClassDiagramUtil.createInheritance((IRPClass) toElem, (IRPClassifier) fromElem,
                                    description);
                        } else if (Constants.RHAPSODY_AGGREGATION.equals(type)) {
                            ClassDiagramUtil.createAggregation((IRPClass) toElem, (IRPClass) fromElem, description,
                                    end1_multiplicity, end2_multiplicity);
                        } else if (Constants.RHAPSODY_COMPOSITION.equals(type)) {
                            ClassDiagramUtil.createComposition((IRPClass) fromElem, (IRPClass) toElem, description,
                                    end1_multiplicity, end2_multiplicity);
                        }
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating relations " + e.getMessage() + Constants.NEW_LINE);
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
                    ClassDiagramUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
                }
            } catch (Exception e) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error while creating realizations " + e.getMessage() + Constants.NEW_LINE);
            }
        }
        // Generalizations (extends)
        for (Map.Entry<IRPModelElement, String> entry : generalizations.entrySet()) {
            try {
                IRPModelElement fromElem = entry.getKey();
                String toName = entry.getValue();
                IRPModelElement toElem = elementMap.get(toName);
                if (fromElem instanceof IRPClass && toElem instanceof IRPClassifier) {
                    ClassDiagramUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem, "");
                }
            } catch (Exception e) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error while creating generalization " + e.getMessage() + Constants.NEW_LINE);
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
            IRPObjectModelDiagram diagram = ClassDiagramUtil.addClassDiagram(basePackage, title);
            if (diagram != null) {
                ClassDiagramUtil.addStereotype(diagram, Constants.RHAPSODY_CLASS_DIAGRAM_STEREOTYPE,
                        Constants.RHAPSODY_OBJECT_MODEL_DIAGRAM);
                setRelationProperties(diagram);

                IRPCollection relTypes = ClassDiagramUtil.createNewCollection(Constants.rhapsodyApp);
                if (relTypes != null) {
                    ClassDiagramUtil.setCollectionSize(relTypes, 1);
                    ClassDiagramUtil.setCollectionString(relTypes, 1, Constants.RHAPSODY_ALL_RELATIONS);
                    ClassDiagramUtil.populateDiagram(diagram, elementsToPopulate, relTypes,
                            Constants.RHAPSODY_POPULATE_MODE);
                    diagram.openDiagram();
                }
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Error while creating diagram " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    private void setRelationProperties(IRPObjectModelDiagram diagram) {
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_CLASS_SHOW_NAME, Constants.NAME_ONLY);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_CLASS_SHOW_ATTRIBUTES, Constants.RHAPSODY_DISPLAY_ALL);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_CLASS_SHOW_OPERATIONS, Constants.RHAPSODY_DISPLAY_ALL);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_TYPE_COMPARTMENTS, "Attribute,EnumerationLiteral");

        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_AGGREGATION_LINE_STYLE, Constants.RECTILINEAR_ARROWS);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_ASSOCIATION_LINE_STYLE, Constants.RECTILINEAR_ARROWS);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_COMPOSITION_LINE_STYLE, Constants.RECTILINEAR_ARROWS);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_DEPENDS_LINE_STYLE, Constants.RECTILINEAR_ARROWS);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_REALIZATION_LINE_STYLE, Constants.RECTILINEAR_ARROWS);

        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_AGGREGATION_SHOW_NAME, Constants.NAME);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_ASSOCIATION_SHOW_NAME, Constants.NAME);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_COMPOSITION_SHOW_NAME, Constants.NAME);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_DEPENDS_SHOW_NAME, Constants.NAME);
        diagram.setPropertyValue(Constants.OBJECT_MODEL_GE_REALIZATION_SHOW_NAME, Constants.NAME);
    }

}