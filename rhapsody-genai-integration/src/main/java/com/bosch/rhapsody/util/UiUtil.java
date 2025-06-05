package com.bosch.rhapsody.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class UiUtil {

    private UiUtil() {
        // Private constructor to prevent instantiation
    }

    public static void showErrorPopup(String msg) {
        Display display = Display.getCurrent();
        if (display == null) {
            display = new Display();
        }
        Shell shell = new Shell(display);
        try {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
            messageBox.setMessage(msg);
            messageBox.open();
        } finally {
            shell.dispose();
            display.dispose();
        }
    }

    public static boolean showQuestionPopup(String msg) {
        Display display = Display.getCurrent();
        if (display == null) {
            display = new Display();
        }
        Shell shell = new Shell(display);
        try {
            MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
            messageBox.setMessage(msg);
            int response = messageBox.open();
            if (response == SWT.YES) {
                return true;
            }
        } finally {
            shell.dispose();
            display.dispose();
        }
        return false;
    }
}
