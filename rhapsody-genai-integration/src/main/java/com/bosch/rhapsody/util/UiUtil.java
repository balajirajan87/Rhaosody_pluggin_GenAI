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
        UiUtil.setShellLocation(display, shell);
        UiUtil.toggleAlwaysOnTop(shell, true);
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
