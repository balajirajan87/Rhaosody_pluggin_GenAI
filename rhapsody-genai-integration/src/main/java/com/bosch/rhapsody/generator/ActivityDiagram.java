package com.bosch.rhapsody.generator;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.util.ActivityDiagramUtil;
import com.bosch.rhapsody.util.CommonUtil;
import com.telelogic.rhapsody.core.*;
import org.json.JSONObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ActivityDiagram {

    private int actionNameIndex = 1;
    IRPConnector connector = null;
    Object state = null;
    private Map<String,IRPConnector> MergeNodeMap = new HashMap<>();

    public void createActivityDiagram(String outputFile, Shell shell) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(outputFile)));
            JSONObject json = new JSONObject(content);
            IRPPackage basePackage = CommonUtil.createBasePackage(Constants.project, shell);
            if (basePackage == null) {
                Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Could not create/find base package for activity diagram.");
                return;
            }
            ActivityDiagramUtil.getActivitySpecificStereotypes(Constants.project);
            String diagramName = json.optString("title", "ActivityDiagram").replaceAll("[^a-zA-Z0-9]", "_");
            IRPFlowchart fc = ActivityDiagramUtil.createActivityDiagram(basePackage, diagramName);
            JSONArray sections = json.optJSONArray("sections");
            if (sections != null) {
                int swimlaneCount = 0;
                for (int j = 0; j < sections.length(); j++) {
                    JSONObject section_val = sections.getJSONObject(j);
                    JSONObject swimlane_val = section_val.optJSONObject("swimlane");
                    if (swimlane_val != null) {
                        swimlaneCount++;
                        if (swimlaneCount >= 2)
                            break;
                    }
                }
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = sections.getJSONObject(i);
                    JSONObject swimlane = section.optJSONObject("swimlane");
                    String swimlaneId = swimlane != null
                            ? swimlane.optString("identifier", "Swimlane" + i).replaceAll("[^a-zA-Z0-9]", "_")
                            : "Swimlane" + i;
                    IRPSwimlane swimlaneElem = null;
                    if (swimlaneCount >= 2) {
                        swimlaneElem = ActivityDiagramUtil.createSwimlane(fc, swimlaneId);
                        state = null;
                    }
                    JSONArray statements = section.optJSONArray("statements");
                    if (statements != null) {
                        createStatementsRecursive(fc, swimlaneElem, statements, false, null);
                    }
                }
            }
            JSONArray partitions = json.optJSONArray("partitions");
            if (partitions != null) {
                for (int i = 0; i < partitions.length(); i++) {
                    JSONObject partition = partitions.getJSONObject(i);
                    JSONArray statements = partition.optJSONArray("statements");
                    if (statements != null) {
                        state = null;
                        createStatementsRecursive(fc, null, statements, false, null);
                    }
                }
            }
            ActivityDiagramUtil.createDiagramGraphics(fc);
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin", "\nActivity Diagram generated successfully.");
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
            messageBox.setMessage(
                    "Activity Diagram generated successfully. \n\nTo view the generated diagram in Rhapsody, please close the close the Chat UI.\n");
            messageBox.open();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                java.nio.file.Path outputPath = java.nio.file.Paths.get(outputFile);
                if (java.nio.file.Files.exists(outputPath)) {
                    java.nio.file.Files.delete(outputPath);
                }
            } catch (Exception ex) {
                Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Could not delete output file: " + ex.getMessage());
            }

        }
    }

    // Helper method to handle nested statements (actions, decisions, etc.)
    private void createStatementsRecursive(IRPFlowchart flowChart, IRPSwimlane swimlaneElem, JSONArray statements,
            Boolean hasStart, String gaurd) {
        IRPState newState = null;
        for (int i = 0; i < statements.length(); i++) {
            try {
                JSONObject stmt = statements.getJSONObject(i);
                String type = stmt.optString("type", "");
                switch (type) {
                    case "start":
                        hasStart = true;
                        break;
                    case "stop":
                        newState = ActivityDiagramUtil.createActivityFinal(flowChart, swimlaneElem);
                        ActivityDiagramUtil.createTransition(state, newState, gaurd, ActivityDiagramUtil.controlFlow);
                        state = newState;
                        break;
                    case "action":
                        String actionText = stmt.optString("text", "");
                        newState = ActivityDiagramUtil.createAction(flowChart, actionText, "action_" + actionNameIndex,
                                swimlaneElem);
                        actionNameIndex++;
                        if (hasStart) {
                            ActivityDiagramUtil.createDefaultTransition(flowChart, newState);
                            hasStart = false;
                        }
                        if (state != null)
                            ActivityDiagramUtil.createTransition(state, newState, gaurd,
                                    ActivityDiagramUtil.controlFlow);
                        state = newState;
                        break;
                    case "transition":
                        String text = stmt.optString("text", "transition");
                        IRPConnector transition = ActivityDiagramUtil.createConnector(flowChart, "MergeNode", text,
                                swimlaneElem);
                        if (state != null)
                            ActivityDiagramUtil.createTransition(state, transition, gaurd,
                                    ActivityDiagramUtil.controlFlow);
                        state = transition;
                        break;
                    case "MergeNode":
                        String MergeNodeText = stmt.optString("text");
                        if(MergeNodeText != null && !MergeNodeText.isEmpty())
                        {
                            IRPConnector MergeNode;
                            if(MergeNodeMap.containsKey(MergeNodeText)){
                                MergeNode = MergeNodeMap.get(MergeNodeText);
                            }else{
                                MergeNode = ActivityDiagramUtil.createConnector(flowChart, "MergeNode", MergeNodeText,
                                    swimlaneElem);
                                MergeNodeMap.put(MergeNodeText, MergeNode); 
                            }  
                            ActivityDiagramUtil.createTransition(state, MergeNode, gaurd,ActivityDiagramUtil.controlFlow);
                            state = MergeNode;
                        }
                        break;
                    case "decision":
                        String condition = stmt.optString("condition", "decision");
                        String then_label = stmt.optString("then_label", "yes");
                        IRPConnector cond = ActivityDiagramUtil.createConnector(flowChart, "Condition", condition,
                                swimlaneElem);
                        ActivityDiagramUtil.createTransition(state, cond, gaurd, ActivityDiagramUtil.controlFlow);
                        JSONArray thenArr = stmt.optJSONArray("then_statements");
                        if (thenArr != null) {
                            state = cond;
                            createStatementsRecursive(flowChart, swimlaneElem, thenArr, false, then_label);
                            state = cond;
                        }
                        JSONArray elseifArr = stmt.optJSONArray("else_ifs");
                        if (elseifArr != null) {
                            for (int j = 0; j < elseifArr.length(); j++) {
                                JSONObject elseif = elseifArr.getJSONObject(j);
                                String elsif_condition = elseif.optString("condition", "decision");
                                String elsif_then_label = elseif.optString("then_label", "yes");
                                IRPConnector elsif_cond = ActivityDiagramUtil.createConnector(flowChart, "Condition",
                                        elsif_condition, swimlaneElem);
                                ActivityDiagramUtil.createTransition(state, elsif_cond, gaurd,
                                        ActivityDiagramUtil.controlFlow);
                                JSONArray elsif_thenArr = elseif.optJSONArray("statements");
                                if (elsif_thenArr != null) {
                                    state = elsif_cond;
                                    createStatementsRecursive(flowChart, swimlaneElem, elsif_thenArr, false,
                                            elsif_then_label);
                                }
                                state = elsif_cond;
                            }
                        }
                        JSONObject elseObj = stmt.optJSONObject("else_block");
                        if (elseObj != null) {
                            String else_label = elseObj.optString("else_label", "no");
                            JSONArray elseArr = elseObj.optJSONArray("statements");
                            createStatementsRecursive(flowChart, swimlaneElem, elseArr, false, else_label);
                        }

                        break;
                    case "repeat_loop":
                        IRPConnector loop = ActivityDiagramUtil.createConnector(flowChart, "MergeNode", null,
                                swimlaneElem);
                        ActivityDiagramUtil.createTransition(state, loop, gaurd, ActivityDiagramUtil.controlFlow);
                        JSONArray body = stmt.optJSONArray("body");
                        if (body != null) {
                            state = loop;
                            createStatementsRecursive(flowChart, swimlaneElem, body, false, null);
                        }
                        String loopCondition = stmt.optString("condition", "decision");
                        String loop_then_label = stmt.optString("then_label", "yes");
                        String loop_else_label = stmt.optString("else_label", "no");
                        IRPConnector loopcond = ActivityDiagramUtil.createConnector(flowChart, "Condition",
                                loopCondition, swimlaneElem);
                        ActivityDiagramUtil.createTransition(state, loopcond, loop_then_label,
                                ActivityDiagramUtil.controlFlow);
                        ActivityDiagramUtil.createTransition(loopcond, loop, loop_then_label,
                                ActivityDiagramUtil.controlFlow);
                        state = loopcond;
                        gaurd = loop_else_label;
                        break;
                }
            } catch (Exception e) {
                Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                        "\nERROR: Error while creating element " + e.getMessage());
            }
            gaurd = null;
        }
    }
}
