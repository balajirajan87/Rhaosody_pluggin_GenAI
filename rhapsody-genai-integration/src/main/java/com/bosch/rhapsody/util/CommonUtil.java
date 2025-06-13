package com.bosch.rhapsody.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.IRPModelElement;
import com.telelogic.rhapsody.core.IRPPackage;
import com.telelogic.rhapsody.core.IRPProject;

public class CommonUtil {

    public static IRPProject getActiveProject(IRPApplication app) {
        try {
            return app.activeProject();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin", "getActiveProject: " + e.getMessage());
        }
        return null;
    }

    public static String getProjectLanguage(IRPProject project) {
        try {
            return project.getLanguage();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin", "getProjectLanguage: " + e.getMessage());
        }
        return null;
    }

    public static IRPPackage addPackage(IRPProject project, String packageName) {
        try {
            return project.addPackage(packageName);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin", "addPackage (project): " + e.getMessage());
        }
        return null;
    }

    public static IRPPackage createBasePackage(IRPProject project, Shell shell, String packageName) {
        IRPModelElement newPackage = project.findNestedElementRecursive(packageName,
                Constants.RHAPSODY_PACKAGE);
        if (newPackage != null) {
            newPackage.locateInBrowser();
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setText("Package Exists");
            messageBox.setMessage("The package '" + packageName
                    + "' already exists. Do you want to overwrite it?");
            Constants.rhapsodyApp.writeToOutputWindow("GenAIPlugin",
                    "\nThe package '" + packageName + "' already exists.");
            int response = messageBox.open();
            if (response == SWT.YES) {
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
