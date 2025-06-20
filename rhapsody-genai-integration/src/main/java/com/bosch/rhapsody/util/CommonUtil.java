package com.bosch.rhapsody.util;

import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPCollection;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPProject;
import com.telelogic.rhapsody.core.IRPStereotype;

import java.util.Map;

public class CommonUtil {

    public static IRPProject getActiveProject(IRPApplication app) {
        try {
            return app.activeProject();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: getActiveProject: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static String getProjectLanguage(IRPProject project) {
        try {
            return project.getLanguage();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: getProjectLanguage: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPPackage addPackage(IRPProject project, String packageName) {
        try {
            return project.addPackage(packageName);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addPackage (project): " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPPackage createOrGetPackage(IRPModelElement project, String packageName) {
        try {
            IRPModelElement newPackage = project.findNestedElement(packageName,
                    Constants.RHAPSODY_PACKAGE);
            if (newPackage != null) {
                return (IRPPackage) newPackage;
            } else {
                return (IRPPackage) project.addNewAggr(Constants.RHAPSODY_PACKAGE, packageName);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addPackage (pkg): " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static Map<String, IRPStereotype> getStereotypes(IRPModelElement pkg) {
        Map<String, IRPStereotype> stereotypeMap = new java.util.HashMap<>();
        try {
            IRPCollection stereotypes = pkg.getNestedElementsByMetaClass(Constants.RHAPSODY_STEREOTYPE, 1);
            if (stereotypes != null) {
                for (int i = 1; i <= stereotypes.getCount(); i++) {
                    IRPStereotype stereotype = (IRPStereotype) stereotypes.getItem(i);
                    if (stereotype != null && stereotype.getName() != null) {
                        stereotypeMap.put(stereotype.getName(), stereotype);
                    }
                }
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: getStereotypes: " + e.getMessage() + Constants.NEW_LINE);
        }
        return stereotypeMap;
    }

    public static IRPPackage createBasePackage(IRPProject project, String packageName) {
        IRPModelElement newPackage = project.findNestedElementRecursive(packageName,
                Constants.RHAPSODY_PACKAGE);
        if (newPackage != null) {
            newPackage.locateInBrowser();
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "INFO: The package '" + packageName + "' already exists." + Constants.NEW_LINE);
            boolean response = UiUtil.showQuestionPopup("The package '" + packageName
                    + "' already exists. Do you want to overwrite it?");
            if (response) {
                newPackage.deleteFromProject();
                return CommonUtil.addPackage(project, packageName);
            } else {
                return null;
            }
        } else {
            return CommonUtil.addPackage(project, packageName);
        }
    }

}
