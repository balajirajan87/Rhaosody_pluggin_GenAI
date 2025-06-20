package com.bosch.rhapsody.generator;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.parser.ActivityTransitionAdder;
import com.bosch.rhapsody.util.ActivityDiagramUtil;
import com.bosch.rhapsody.util.CommonUtil;
import com.bosch.rhapsody.util.UiUtil;
import com.telelogic.rhapsody.core.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ActivityDiagram {

    private int actionNameIndex = 1;
    IRPConnector connector = null;
    Object state = null;
    private Map<String, IRPConnector> MergeNodeMap = new HashMap<>();
    private Map<String, IRPSwimlane> swimlaneMap = new HashMap<>();
    private Object MergeNodeState = null;

    public void createActivityDiagram(String outputFile) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(outputFile)));
            JSONObject json = new JSONObject(content);
            IRPPackage basePackage = CommonUtil.createBasePackage(Constants.project,
                    Constants.RHAPSODY_ACTIVITY_DIAGRAM);
            if (basePackage == null) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Could not create/find base package for activity diagram." + Constants.NEW_LINE);
                return;
            }
            ActivityDiagramUtil.getActivitySpecificStereotypes(Constants.project);
            String diagramName = json.optString("title", "ActivityDiagram").replaceAll("[^a-zA-Z0-9]", "_");
            IRPFlowchart fc = ActivityDiagramUtil.createActivityDiagram(basePackage, diagramName);
            JSONArray sections = json.optJSONArray("sections");
            if (sections != null) {
                if (ActivityTransitionAdder.swimlane.size() >= 2) {
                    state = null;
                }
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = sections.getJSONObject(i);
                    JSONObject swimlaneobj = section.optJSONObject("swimlane");
                    IRPSwimlane firstSwimlaneElem = null;
                    if (swimlaneobj != null) {
                        String identifier = swimlaneobj.optString("identifier", "").replaceAll("[^a-zA-Z0-9]", "_");
                        if (identifier != null && !identifier.isEmpty()) {
                            firstSwimlaneElem = swimlaneMap.get(identifier);
                            if (null == firstSwimlaneElem) {
                                firstSwimlaneElem = ActivityDiagramUtil.createSwimlane(fc, identifier);
                                swimlaneMap.put(identifier, firstSwimlaneElem);
                            }
                        }
                    }
                    JSONArray statements = section.optJSONArray("statements");
                    if (statements != null) {
                        createStatementsRecursive(fc, firstSwimlaneElem, statements, false, null);
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
            CommonUtil.pause(3000);
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "INFO: Activity Diagram generated successfully." + Constants.NEW_LINE);
            UiUtil.showInfoPopup(
                    "Activity Diagram generated successfully. \n\nTo view the generated diagram in Rhapsody, please close the close the Chat UI.\n");
            fc.getFlowchartDiagram().openDiagram();
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
                        state = MergeNodeState;
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
                        if (MergeNodeText != null && !MergeNodeText.isEmpty()) {
                            IRPConnector MergeNode;
                            if (MergeNodeMap.containsKey(MergeNodeText)) {
                                MergeNode = MergeNodeMap.get(MergeNodeText);
                            } else {
                                MergeNode = ActivityDiagramUtil.createConnector(flowChart, "MergeNode", MergeNodeText,
                                        swimlaneElem);
                                MergeNodeMap.put(MergeNodeText, MergeNode);
                            }
                            MergeNodeState = state;
                            ActivityDiagramUtil.createTransition(state, MergeNode, gaurd,
                                    ActivityDiagramUtil.controlFlow);
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
                    case "switch":
                        String caseCondition = stmt.optString("condition", "");
                        IRPConnector caseConnector = ActivityDiagramUtil.createConnector(flowChart, "Condition",
                                caseCondition, swimlaneElem);
                        ActivityDiagramUtil.createTransition(state, caseConnector, gaurd,
                                ActivityDiagramUtil.controlFlow);
                        JSONArray cases = stmt.optJSONArray("cases");
                        if (cases != null) {
                            for (int j = 0; j < cases.length(); j++) {
                                JSONObject case_obj = cases.getJSONObject(j);
                                String value = case_obj.optString("value", "");
                                JSONArray caseStatements = case_obj.optJSONArray("statements");
                                state = caseConnector;
                                createStatementsRecursive(flowChart, swimlaneElem, caseStatements, false, value);
                            }
                        }
                        break;
                    case "partition":
                        String partictionName = stmt.optString("name", "");
                        JSONArray stmtPartition = stmt.optJSONArray("statements");
                        createStatementsRecursive(flowChart, swimlaneElem, stmtPartition, hasStart, null);
                        break;
                    case "swimlane":
                        String swimlane_name = stmt.optString("identifier", "").replaceAll("[^a-zA-Z0-9]", "_");
                        IRPSwimlane swim = swimlaneMap.get(swimlane_name);
                        if (null == swim) {
                            swim = ActivityDiagramUtil.createSwimlane(flowChart, swimlane_name);
                            swimlaneMap.put(swimlane_name, swim);
                        }
                        if (null != swim) {
                            swimlaneElem = swim;
                        }
                }
            } catch (Exception e) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Error while creating element " + e.getMessage() + Constants.NEW_LINE);
            }
            gaurd = null;
        }
    }
}
