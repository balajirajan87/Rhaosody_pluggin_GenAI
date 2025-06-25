package com.bosch.rhapsody.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class UiUtil {

    private UiUtil() {
        // Private constructor to prevent instantiation
    }

    private static int showPopup(String msg, int style) {
        Display display = Display.getCurrent();
        boolean createdDisplay = false;
        if (display == null) {
            display = new Display();
            createdDisplay = true;
        }
        Shell shell = new Shell(display);
        UiUtil.setShellLocation(display, shell);
        UiUtil.toggleAlwaysOnTop(shell, true);
        try {
            MessageBox messageBox = new MessageBox(shell, style);
            messageBox.setMessage(msg);
            return messageBox.open();
        } finally {
            shell.dispose();
            if (createdDisplay) {
                display.dispose();
            }
        }
    }

    public static void showErrorPopup(String msg) {
        showPopup(msg, SWT.ICON_ERROR | SWT.OK);
    }

    public static void showInfoPopup(String msg) {
        showPopup(msg, SWT.ICON_INFORMATION | SWT.OK);
    }

    public static void showWarnPopup(String msg) {
        showPopup(msg, SWT.ICON_WARNING | SWT.OK);
    }

    public static boolean showQuestionPopup(String msg) {
        int response = showPopup(msg, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
        return response == SWT.YES;
    }

    /**
     * @param shell   - shell input
     * @param isOnTop - boolean to show shell in the top of the display
     */
    public static void toggleAlwaysOnTop(Shell shell, boolean isOnTop) {
        long handle = shell.handle;
        org.eclipse.swt.graphics.Point location = shell.getLocation();
        org.eclipse.swt.graphics.Point dimension = shell.getSize();
        OS.SetWindowPos(handle, isOnTop ? OS.HWND_TOPMOST : OS.HWND_NOTOPMOST, location.x, location.y, dimension.x,
                dimension.y, 0);
    }

    /**
     * @param display - display input
     * @param shell   - shell input
     */
    public static void setShellLocation(Display display, Shell shell) {
        org.eclipse.swt.widgets.Monitor primary = display.getPrimaryMonitor();
        org.eclipse.swt.graphics.Rectangle bounds = primary.getBounds();
        int centerX = bounds.x + (bounds.width - shell.getSize().x) / 2;
        int centerY = bounds.y + (bounds.height - shell.getSize().y) / 2;
        shell.setLocation(centerX, centerY);
    }
}
