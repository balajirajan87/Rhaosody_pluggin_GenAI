package com.bosch.rhapsody.util;

import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.IRPActor;
import com.telelogic.rhapsody.core.IRPComponent;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPPackage;

public class ComponentDiagramUtil {

    public static IRPComponent addComponent(IRPPackage pkg, String componentName, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(componentName, Constants.RHAPSODY_COMPONENT);
            if (element != null && element instanceof IRPComponent) {
                return (IRPComponent) element;
            } else {
                return (IRPComponent) pkg.addNewAggr( Constants.RHAPSODY_COMPONENT,componentName);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addComponent: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPActor addActor(IRPPackage pkg, String actorName, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(actorName, Constants.RHAPSODY_ACTOR);
            if (element != null && element instanceof IRPActor) {
                return (IRPActor) element;
            } else {
                return (IRPActor) pkg.addActor(actorName);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addActor: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    
}
