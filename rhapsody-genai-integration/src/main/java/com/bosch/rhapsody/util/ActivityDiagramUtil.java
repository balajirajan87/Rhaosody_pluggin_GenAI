package com.bosch.rhapsody.util;

import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.IRPConnector;
import com.telelogic.rhapsody.core.IRPFlowchart;
import com.telelogic.rhapsody.core.IRPObjectNode;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.IRPState;
import com.telelogic.rhapsody.core.IRPStereotype;
import com.telelogic.rhapsody.core.IRPSwimlane;
import com.telelogic.rhapsody.core.IRPTransition;

public class ActivityDiagramUtil {

    public static IRPStereotype controlFlow = null;

    /**
     * Creates a swimlane in a Rhapsody activity diagram.
     *
     * @param diagram      The IRPFlowchart (activity diagram) where the swimlane
     *                     will be created.
     * @param swimlaneName The name of the swimlane to create.
     * @return The created IRPSwimlane object, or null if creation failed.
     */
    public static IRPSwimlane createSwimlane(IRPFlowchart diagram, String swimlaneName) {

        IRPSwimlane swimlane = null;
        try {
            swimlane = diagram.addSwimlane(swimlaneName);
            if (swimlane == null) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: Failed to create swimlane: " + swimlaneName + Constants.NEW_LINE);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create swimlane " + swimlaneName + ex.getMessage() + Constants.NEW_LINE);
        }
        return swimlane;
    }

    /**
     * Retrieves the "ControlFlow" stereotype from the given project.
     *
     * @param project The IRPProject from which to retrieve the stereotype.
     */
    public static void getActivitySpecificStereotypes(IRPProject project) {
        controlFlow = (IRPStereotype) project.findNestedElementRecursive("ControlFlow", "Stereotype");
    }

    /**
     * Creates an activity diagram in the specified Rhapsody package.
     *
     * @param pkg         The IRPPackage object where the activity diagram will be
     *                    created.
     * @param diagramName The name to assign to the new activity diagram.
     * @return The created IRPFlowchart (activity diagram) object, or null if
     *         creation failed.
     */
    public static IRPFlowchart createActivityDiagram(IRPPackage pkg, String diagramName) {
        IRPFlowchart flowChart = null;
        try {
            flowChart = pkg.addActivityDiagram();
            flowChart.setName(diagramName);
            flowChart.setPropertyValue("Activity_diagram.ControlFlow.line_style", "rectilinear_arrows");
            flowChart.setPropertyValue("Activity_diagram.DefaultTransition.line_style", "rectilinear_arrows");
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create ActivityDiagram " + diagramName + ex.getMessage() + Constants.NEW_LINE);
        }
        return flowChart;
    }

    /**
     * Creates an action (state) in the given activity diagram and assigns it to a
     * swimlane.
     *
     * @param diagram    The IRPFlowchart (activity diagram) where the action will
     *                   be created.
     * @param actionName The name of the action.
     * @param swimlane   The IRPSwimlane to assign the action to (can be null).
     * @return The created IRPState object, or null if creation failed.
     */
    public static IRPState createAction(IRPFlowchart diagram, String entryAction, String actionName,
            IRPSwimlane swimlane) {
        IRPState action = null;
        try {
            action = diagram.getRootState().addState(actionName);
            action.setStateType("Action");
            if (entryAction != null && !entryAction.isEmpty())
                action.setEntryAction(entryAction);
            if (swimlane != null) {
                action.setItsSwimlane(swimlane);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create action " + actionName + ": " + ex.getMessage() + Constants.NEW_LINE);
        }
        return action;
    }

    /**
     * Creates an object node in the given activity diagram.
     *
     * @param diagram  The IRPFlowchart (activity diagram) where the object node
     *                 will be created.
     * @param nodeName The name of the object node.
     * @return The created IRPObjectNode object, or null if creation failed.
     */
    public static IRPObjectNode createObjectNode(IRPFlowchart diagram, String nodeName) {
        IRPObjectNode objNode = null;
        try {
            objNode = (IRPObjectNode) diagram.addNewAggr("ObjectNode", nodeName);
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create object node " + nodeName + ": " + ex.getMessage() + Constants.NEW_LINE);
        }
        return objNode;
    }

    /**
     * Creates a connector (e.g., Condition) in the activity diagram.
     * 
     * @param diagram       The IRPFlowchart (activity diagram) where the connector
     *                      will be created.
     * @param connectorType The type of connector (e.g., "Condition").
     * @param swimlane      The IRPSwimlane to assign the connector to (can be
     *                      null).
     * @return The created IRPConnector object, or null if creation failed.
     */
    public static IRPConnector createConnector(IRPFlowchart diagram, String connectorType, String name,
            IRPSwimlane swimlane) {
        IRPConnector connector = null;
        try {
            connector = diagram.getRootState().addConnector(connectorType);
            connector.setDisplayName(name);
            connector.setDescription(name);
            if (swimlane != null) {
                connector.setItsSwimlane(swimlane);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create connector " + connectorType + ": " + ex.getMessage() + Constants.NEW_LINE);
        }
        return connector;
    }

    /**
     * Creates a flow final node in the activity diagram.
     *
     * @param diagram  The IRPFlowchart (activity diagram) where the flow final node
     *                 will be created.
     * @param nodeName The name of the flow final node.
     * @param swimlane The IRPSwimlane to assign the flow final node to (can be
     *                 null).
     * @return The created IRPState object, or null if creation failed.
     */
    public static IRPState createFlowFinal(IRPFlowchart diagram, String nodeName, IRPSwimlane swimlane) {
        IRPState finalNode = null;
        try {
            finalNode = diagram.getRootState().addState(nodeName);
            finalNode.setStateType("Acti");
            if (swimlane != null) {
                finalNode.setItsSwimlane(swimlane);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create flow final node " + nodeName + ": " + ex.getMessage()
                            + Constants.NEW_LINE);
        }
        return finalNode;
    }

    public static IRPState createActivityFinal(IRPFlowchart diagram, IRPSwimlane swimlane) {
        IRPState finalNode = null;
        try {
            finalNode = diagram.getRootState().addActivityFinal();
            if (swimlane != null) {
                finalNode.setItsSwimlane(swimlane);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create flow final node  : " + ex.getMessage() + Constants.NEW_LINE);
        }
        return finalNode;
    }

    /**
     * Adds a transition between two elements (states or connectors) in the activity
     * diagram.
     *
     * @param fromElement    The source IRPState or IRPConnector where the
     *                       transition starts.
     * @param toElement      The target IRPState or IRPConnector where the
     *                       transition ends.
     * @param guard          (Optional) The guard condition for the transition (can
     *                       be null or empty).
     * @param transitionType (Optional) The IRPStereotype to assign to the
     *                       transition (can be null).
     * @return The created IRPTransition object, or null if creation failed.
     */
    public static IRPTransition createTransition(Object fromElement, Object toElement, String guard,
            IRPStereotype transitionType) {
        IRPTransition transition = null;
        try {
            // Convert toElement to IRPState or IRPConnector (typed, not generic Object)
            if (!(toElement instanceof IRPState) && !(toElement instanceof IRPConnector)) {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: toElement must be IRPState or IRPConnector");
            }

            // Check if fromElement has addTransition method and call with correct type
            if (fromElement instanceof IRPState && toElement instanceof IRPState) {
                transition = ((IRPState) fromElement).addTransition((IRPState) toElement);
            } else if (fromElement instanceof IRPState && toElement instanceof IRPConnector) {
                transition = ((IRPState) fromElement).addTransition((IRPConnector) toElement);
            } else if (fromElement instanceof IRPConnector && toElement instanceof IRPState) {
                transition = ((IRPConnector) fromElement).addTransition((IRPState) toElement);
            } else if (fromElement instanceof IRPConnector && toElement instanceof IRPConnector) {
                transition = ((IRPConnector) fromElement).addTransition((IRPConnector) toElement);
            } else {
                Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                        "ERROR: fromElement must be IRPState or IRPConnector" + Constants.NEW_LINE);
            }

            if (transitionType != null) {
                transition.addSpecificStereotype(transitionType);
            }
            if (guard != null && !guard.isEmpty()) {
                transition.setItsGuard(guard);
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to add transition: " + ex.getMessage() + Constants.NEW_LINE);
        }
        return transition;
    }

    /**
     * Creates diagram graphics for all elements in the given activity diagram.
     * This method should be called after adding all model elements to the diagram.
     * 
     * @param flowchart The IRPFlowchart (activity diagram) for which to create
     *                  graphics.
     */
    public static void createDiagramGraphics(IRPFlowchart flowchart) {
        if (flowchart != null) {
            flowchart.createGraphics();
            flowchart.setShowDiagramFrame(1);
        }
    }

    /**
     * Creates a default transition from the given source state to the target state
     * in the activity diagram.
     * 
     * @param diagram The IRPFlowchart (activity diagram)
     * @param toState The target IRPState where the transition ends.
     * @return The created IRPTransition object, or null if creation failed.
     */
    public static IRPTransition createDefaultTransition(IRPFlowchart diagram, IRPState toState) {
        IRPTransition transition = null;
        try {
            if (null != diagram) {
                IRPState rootState = diagram.getRootState();
                if (null != rootState) {
                    transition = toState.createDefaultTransition(rootState);
                }
            }
        } catch (Exception ex) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: Failed to create default transition: " + ex.getMessage() + Constants.NEW_LINE);
        }
        return transition;
    }

    // /**
    // * Main method for demonstration and testing purposes.
    // * Creates a sample activity diagram with swimlanes, actions, connectors,
    // transitions, and graphics.
    // *
    // * @param args Command-line arguments (not used).
    // */
    // public static void main(String[] args) {
    // IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
    // IRPPackage pkg = app.activeProject().addPackage("aPackage");
    // ActivityDiagramUtil createActivityDiagram = new ActivityDiagramUtil();
    // createActivityDiagram.getActivitySpecificStereotypes(app.activeProject());
    // IRPFlowchart fc = createActivityDiagram.createActivityDiagram(pkg, "new");
    // IRPSwimlane sw = createActivityDiagram.createSwimlane(fc, "New");
    // IRPSwimlane sw_2 = createActivityDiagram.createSwimlane(fc, "New_2");
    // IRPState ac = createActivityDiagram.createAction(fc, "action_new", sw);
    // createActivityDiagram.createDefaultTransition(fc, ac);

    // IRPConnector cond = createActivityDiagram.createConnector(fc,
    // "Condition",sw);
    // IRPState ff = createActivityDiagram.createFlowFinal(fc, "abc", sw);
    // createActivityDiagram.createTransition(ac, cond, "if",
    // createActivityDiagram.controlFlow);
    // createActivityDiagram.createTransition(cond, ff, "else",
    // createActivityDiagram.controlFlow);

    // createDiagramGraphics(fc);
    // }

}
