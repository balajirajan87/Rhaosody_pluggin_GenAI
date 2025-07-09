package com.bosch.rhapsody.generator;

import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.util.ComponentDiagramUtil;
import com.bosch.rhapsody.util.CommonUtil;
import com.telelogic.rhapsody.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.io.IOException;


public class ComponentDiagram {

    IRPPackage basePackage;
    private Map<String, IRPModelElement> elementMap = new HashMap<>();
    private Map<String, IRPModelElement> elementLabelMap = new HashMap<>();
    private IRPCollection elementsToPopulate;
    private Map<IRPComment, String> anchors = new HashMap<>();
    private Map<String, IRPStereotype> stereotypeMap = new HashMap<>();
    IRPPackage baseStereotypePackage;
    JSONObject relations  = new JSONObject();
    int fileCount;
    
    public void createComponentDiagram(String outputFile,int fileCountTemp,Boolean hasMultipleFiles) {
        try {
            fileCount = fileCountTemp;
            String jsonString = readJsonFile(outputFile);
            if (jsonString == null)
                return;
            JSONObject json = new JSONObject(jsonString);
            if(fileCount == 1)
                basePackage = CommonUtil.createBasePackage(Constants.project, Constants.RHAPSODY_COMPONENT_DIAGRAM);
            else
                 basePackage = CommonUtil.createOrGetPackage(Constants.project, Constants.RHAPSODY_COMPONENT_DIAGRAM);
            if (basePackage == null) {
                return;
            }
            String title = Constants.RHAPSODY_COMPONENT_DIAGRAM;
            if(hasMultipleFiles){
                title = title + "_" + fileCount;
            }
            String diagramName = json.optString("title", title).replaceAll("[^a-zA-Z0-9]", "_");
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "INFO: Creating component diagram :" + diagramName+ Constants.NEW_LINE); 
            baseStereotypePackage = CommonUtil.createOrGetPackage(Constants.project, Constants.RHAPSODY_STEREOTYPE);
            stereotypeMap = CommonUtil.getStereotypes(baseStereotypePackage);
            elementsToPopulate = Constants.rhapsodyApp.createNewCollection();
            createElements(basePackage,json);
            createPackage(json);
            createRelation(json);
            createNoteRelation();
            // Handle calls/relations inside packages 
            createRelation(relations);
            IRPComponentDiagram diagram = basePackage.addComponentDiagram(diagramName);
            CommonUtil.populateDiagrams(diagram,elementsToPopulate);
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                "INFO: Component diagram created :" + diagramName+ Constants.NEW_LINE);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                "ERROR: Error creating component diagram " + e.getMessage()+ Constants.NEW_LINE);
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
                            collectRelations(packageObject);
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


    private void createElements(IRPModelElement container, JSONObject obj) {
        // Component
        JSONArray components = obj.optJSONArray(Constants.JSON_COMPONENTS);
        createComponent(container, components,null);

         // Component
        JSONArray database = obj.optJSONArray(Constants.JSON_DATABASE);
        createComponent(container, database,Constants.JSON_DATABASE);

         // Component
        JSONArray clouds = obj.optJSONArray(Constants.JSON_CLOUDS);
        createComponent(container, clouds,Constants.JSON_CLOUDS);

        JSONArray nodes = obj.optJSONArray(Constants.JSON_NODES);
        createComponent(container, nodes,Constants.JSON_NODES);

        //Actor
        createActor(container, obj);

        //Interface
        createInterface(container, obj);

        // Notes
        createNote(container, obj);

    }


    private void createComponent(IRPModelElement container, JSONArray components,String stereotype) {
        if (components != null) {
            for (int i = 0; i < components.length(); i++) {
                try {
                    JSONObject componentObject = components.getJSONObject(i);
                    String componentName = componentObject.optString(Constants.JSON_ALIAS_NAME,"").replaceAll("[^a-zA-Z0-9]", "_");
                    String componentLabel = componentObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    if(componentName == null || componentName.isEmpty()){
                        componentName = componentLabel;
                    }
                    IRPComponent rhapsodyComponent = ComponentDiagramUtil.addComponent((IRPPackage) container, componentName,basePackage);
                    if (rhapsodyComponent != null) {
                        addStereotype(rhapsodyComponent, stereotype);
                        rhapsodyComponent.setDisplayName(componentLabel);
                        elementMap.put(componentName, rhapsodyComponent);
                        elementLabelMap.put(componentLabel, rhapsodyComponent);
                        elementsToPopulate.addItem(rhapsodyComponent);
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating Component " + e.getMessage() + Constants.NEW_LINE);
                }
            }
        }

    }

    private void createActor(IRPModelElement container, JSONObject obj) {
        JSONArray actors = obj.optJSONArray(Constants.JSON_ACTORS);
        if (actors != null) {
            for (int i = 0; i < actors.length(); i++) {
                try {
                    JSONObject actorObject = actors.getJSONObject(i);
                    String actorName = actorObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    IRPActor rhapsodyActor = ComponentDiagramUtil.addActor((IRPPackage) container, actorName,basePackage);
                    if (rhapsodyActor != null) {
                        elementMap.put(actorName, rhapsodyActor);
                        elementsToPopulate.addItem(rhapsodyActor);
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating Actor " + e.getMessage() + Constants.NEW_LINE);
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
                    String interfaceName = interfaceObject.optString(Constants.JSON_ALIAS_NAME,"").replaceAll("[^a-zA-Z0-9]",
                            "_");
                     String interfaceLabel = interfaceObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    if(interfaceName == null || interfaceName.isEmpty()){
                        interfaceName = interfaceLabel;
                    }
                    IRPClass rhapsodyInterface = CommonUtil.addInterface((IRPPackage) container, interfaceName,
                            basePackage);
                    if (rhapsodyInterface != null) {
                        rhapsodyInterface.setDisplayName(interfaceLabel);
                        elementMap.put(interfaceName, rhapsodyInterface);
                        elementLabelMap.put(interfaceLabel, rhapsodyInterface);
                        elementsToPopulate.addItem(rhapsodyInterface);
                    }
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating interface " + e.getMessage() + Constants.NEW_LINE);
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
                            Constants.RHAPSODY_COMMENT +"_"+fileCount+ "_" + i);
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

     private void collectRelations(JSONObject obj) {
        JSONArray relationsArray = obj.optJSONArray(Constants.JSON_RELATIONSHIPS);
        if (relationsArray != null && relationsArray.length() > 0) {
            JSONArray targetArray = relations.optJSONArray(Constants.JSON_RELATIONSHIPS);
            if (targetArray == null) {
                targetArray = new JSONArray();
                relations.put(Constants.JSON_RELATIONSHIPS, targetArray);
            }
            // Use Java Streams for efficient appending
            java.util.stream.IntStream.range(0, relationsArray.length())
                .mapToObj(relationsArray::getJSONObject)
                .forEach(targetArray::put);
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
                    if(fromElem == null){
                        fromElem = elementLabelMap.get(from);
                    }
                    IRPModelElement toElem = elementMap.get(to);
                    if(toElem == null){
                        toElem = elementLabelMap.get(to);
                    }
                    if (fromElem != null && toElem != null && fromElem instanceof IRPClass
                            && toElem instanceof IRPClass) {
                        if (Constants.RHAPSODY_ASSOCIATION.equals(type)) {
                            CommonUtil.createAssociation((IRPClass) fromElem, (IRPClass) toElem, description,
                                    null, null);
                        } else if (Constants.RHAPSODY_DIRECTED_ASSOCIATION.equals(type)) {
                            CommonUtil.createDirectedAssociation((IRPClass) fromElem, (IRPClass) toElem,
                                    description, null);
                        } else if (Constants.RHAPSODY_REVERSE_DIRECTED_ASSOCIATION.equals(type)) {
                            CommonUtil.createDirectedAssociation((IRPClass) toElem, (IRPClass) fromElem,
                                    description, null);
                        } else if (Constants.RHAPSODY_DEPENDENCY.equals(type)) {
                            CommonUtil.createDependency(fromElem, toElem, description);
                        } else if (Constants.RHAPSODY_REVERSE_DEPENDENCY.equals(type)) {
                            CommonUtil.createDependency(toElem, fromElem, description);
                        } else if (Constants.RHAPSODY_REALIZATION.equals(type)) {
                            CommonUtil.createRealization((IRPClass) fromElem, (IRPClassifier) toElem);
                        } else if (Constants.RHAPSODY_REVERSE_REALIZATION.equals(type)) {
                            CommonUtil.createRealization((IRPClass) toElem, (IRPClassifier) fromElem);
                        } else if (Constants.RHAPSODY_INHERITANCE.equals(type)) {
                            CommonUtil.createInheritance((IRPClass) fromElem, (IRPClassifier) toElem,
                                    description);
                        } else if (Constants.RHAPSODY_REVERSE_INHERITANCE.equals(type)) {
                            CommonUtil.createInheritance((IRPClass) toElem, (IRPClassifier) fromElem,
                                    description);
                        }
                    }
                    else if (Constants.RHAPSODY_DEPENDENCY.equals(type)) {
                        CommonUtil.createDependency(fromElem, toElem, description);
                    }else if (Constants.RHAPSODY_REVERSE_DEPENDENCY.equals(type)) {
                        CommonUtil.createDependency(toElem, fromElem, description);
                    } else {
                        description = call.optString(Constants.JSON_DESCRIPTION, "flow_"+fileCount+"_"+i);
                        IRPFlow flow = basePackage.addFlows(description);
                        flow.setEnd1(fromElem);
                        flow.setEnd2(toElem);
                    } 
                } catch (Exception e) {
                    Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                            "ERROR: Error while creating relations " + e.getMessage() + Constants.NEW_LINE);
                }
            }
        }
    }



}