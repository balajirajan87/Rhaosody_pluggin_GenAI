package com.bosch.rhapsody.generator;

import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.util.ComponentDiagramUtil;
import com.bosch.rhapsody.util.CommonUtil;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;


public class ComponentDiagram {

    IRPPackage basePackage;
    private Map<String, IRPModelElement> elementMap = new HashMap<>();
    private IRPCollection elementsToPopulate;
    private Map<IRPComment, String> anchors = new HashMap<>();
    
    public void createComponentDiagram(String outputFile,int fileCount,Boolean hasMultipleFiles) {
        try {
            String jsonString = readJsonFile(outputFile);
            if (jsonString == null)
                return;
            JSONObject json = new JSONObject(jsonString);
            if(fileCount == 1)
                basePackage = CommonUtil.createOrGetPackage(Constants.project, Constants.RHAPSODY_COMPONENT_DIAGRAM);
            else
                 basePackage = CommonUtil.createBasePackage(Constants.project, Constants.RHAPSODY_COMPONENT_DIAGRAM);
            if (basePackage == null) {
                return;
            }
            createElements(basePackage,json);

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

    private void createElements(IRPModelElement container, JSONObject obj) {
        // Component
        createComponet(container, obj);

        //Actor
        createActor(container, obj);

        // Notes
        createNote(container, obj);

    }


    private void createComponet(IRPModelElement container, JSONObject obj) {
        JSONArray components = obj.optJSONArray(Constants.JSON_COMPONENTS);
        if (components != null) {
            for (int i = 0; i < components.length(); i++) {
                try {
                    JSONObject componentObject = components.getJSONObject(i);
                    String componentName = componentObject.getString(Constants.JSON_NAME).replaceAll("[^a-zA-Z0-9]", "_");
                    IRPComponent rhapsodyComponent = ComponentDiagramUtil.addComponent((IRPPackage) container, componentName,basePackage);
                    if (rhapsodyComponent != null) {
                        elementMap.put(componentName, rhapsodyComponent);
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




}