package com.bosch.rhapsody.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ActivityTransitionAdder {


public static Set<String> swimlane = new HashSet<>();
    public static void main(String[] args) throws Exception {
        String filePath = "";

        AddMergeNode(filePath);
        System.out.println();
    }

    @SuppressWarnings("unchecked")
    public static void AddMergeNode(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            // Read as LinkedHashMap to preserve order
            LinkedHashMap<String, Object> json = mapper.readValue(
                    Files.readAllBytes(Paths.get(filePath)),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            List<Map<String, Object>> sections = (List<Map<String, Object>>) json.get("sections");
            if (sections != null) {
                for (Map<String, Object> section : sections) {
                    List<Map<String, Object>> statements = (List<Map<String, Object>>) section.get("statements");
                    if (statements != null) {
                        processStatements(statements, null);
                    }

                    Object obj = section.get("swimlane");
                    if (obj instanceof Map) {
                        Object identifier = ((Map<?, ?>) obj).get("identifier");
                        swimlane.add(identifier.toString().replaceAll("[^a-zA-Z0-9]", "_"));
                    }
                }
            }
            // Write back to file (preserves object key order)
            mapper.writeValue(Paths.get(filePath).toFile(), json);
        } catch (Exception ex) {
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private static void processStatements(List<Map<String, Object>> statements, String decisionCondition) {
        if (statements == null || statements.isEmpty())
            return;

        for (Map<String, Object> stmt : statements) {
            String type = (String) stmt.get("type");
            if ("decision".equals(type)) {
                String condition = (String) stmt.get("condition");

                // Process then_statements
                List<Map<String, Object>> thenStmts = (List<Map<String, Object>>) stmt.get("then_statements");
                if (thenStmts != null) {
                    processStatements(thenStmts, condition);
                    setTransitionText(thenStmts, condition);
                }

                // Process else_ifs
                List<Map<String, Object>> elseIfs = (List<Map<String, Object>>) stmt.get("else_ifs");
                if (elseIfs != null) {
                    for (Map<String, Object> elseIf : elseIfs) {
                        List<Map<String, Object>> elseIfStmts = (List<Map<String, Object>>) elseIf.get("statements");
                        if (elseIfStmts != null) {
                            processStatements(elseIfStmts, condition);
                            setTransitionText(elseIfStmts, condition);
                        }
                    }
                }

                // Process else_block
                Map<String, Object> elseBlock = (Map<String, Object>) stmt.get("else_block");
                if (elseBlock != null) {
                    List<Map<String, Object>> elseStmts = (List<Map<String, Object>>) elseBlock.get("statements");
                    if (elseStmts != null) {
                        processStatements(elseStmts, condition);
                        setTransitionText(elseStmts, condition);
                    }
                } else {
                    // If else_block is null, create it and add a MergeNode inside
                    LinkedHashMap<String, Object> newElseBlock = new LinkedHashMap<>();
                    List<Map<String, Object>> elseStmts = new java.util.ArrayList<>();
                    LinkedHashMap<String, Object> mergeNode = new LinkedHashMap<>();
                    mergeNode.put("type", "MergeNode");
                    mergeNode.put("text", condition != null
                            ? "Transition after decision: " + condition
                            : "");
                    elseStmts.add(mergeNode);
                    newElseBlock.put("statements", elseStmts);
                    stmt.put("else_block", newElseBlock);
                }
            }
            if ("repeat_loop".equals(type) || "while_loop".equals(type)) {
                List<Map<String, Object>> body = (List<Map<String, Object>>) stmt.get("body");
                if (body != null)
                    processStatements(body, decisionCondition);
            }
            if ("switch".equals(type)) {
                String condition = (String) stmt.get("condition");
                List<Map<String, Object>> cases = (List<Map<String, Object>>) stmt.get("cases");
                if (cases != null) {
                    for (Map<String, Object> caseObj : cases) {
                        List<Map<String, Object>> caseStatements = (List<Map<String, Object>>) caseObj.get("statements");
                        if (caseStatements != null) {
                            //processStatements(caseStatements, condition);
                            // Add MergeNode at the end of each case's statements
                            if (!caseStatements.isEmpty()) {
                                Map<String, Object> last = caseStatements.get(caseStatements.size() - 1);
                                String lastType = (String) last.get("type");
                                if (!"MergeNode".equals(lastType) && !"stop".equals(lastType)) {
                                    LinkedHashMap<String, Object> mergeNode = new LinkedHashMap<>();
                                    mergeNode.put("type", "MergeNode");
                                    mergeNode.put("text", condition);
                                    caseStatements.add(mergeNode);
                                }
                            }
                        }
                    }
                }
            }
            if ("swimlane".equals(type)) {
                String iden = (String) stmt.get("identifier");
                swimlane.add(iden.replaceAll("[^a-zA-Z0-9]", "_"));
            }

        }

        // Add a new transition block if last is not stop
        Map<String, Object> last = statements.get(statements.size() - 1);
        String lastType = (String) last.get("type");
        if (!"stop".equals(lastType) && !"decision".equals(lastType)) {
            LinkedHashMap<String, Object> transitionBlock = new LinkedHashMap<>();
            transitionBlock.put("type", "MergeNode");
            transitionBlock.put("text", decisionCondition != null
                    ? "Transition after decision: " + decisionCondition
                    : "");
            statements.add(transitionBlock);
        }
    }

    // Helper to set the same transition text for all transitions in a list
    private static void setTransitionText(List<Map<String, Object>> statements, String condition) {
        for (Map<String, Object> stmt : statements) {
            if ("transition".equals(stmt.get("type"))) {
                stmt.put("text", "Transition after decision: " + condition);
            }
        }
    }
}