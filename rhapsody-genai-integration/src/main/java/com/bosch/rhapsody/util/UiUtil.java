package com.bosch.rhapsody.util;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class UiUtil {

    private UiUtil() {
        // Private constructor to prevent instantiation
    }

    public static void showPngPopup(String pngFilePath, String msg) {
        Display display = Display.getCurrent();
        boolean createdDisplay = false;
        if (display == null) {
            display = new Display();
            createdDisplay = true;
        }
        Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE);
        shell.setText("PlantUML execution error");
        UiUtil.setShellLocation(display, shell);
        UiUtil.toggleAlwaysOnTop(shell, true);

        // Use a composite with GridLayout
        Composite mainComposite = new Composite(shell, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 20;
        layout.marginHeight = 20;
        layout.verticalSpacing = 15;
        mainComposite.setLayout(layout);
        org.eclipse.swt.graphics.Image image = null;
        try {
            pngFilePath = pngFilePath.replace(".puml", ".png");
            File file = new File(pngFilePath);
            int width = 700;
            int msgAreaHeight = 60;

            // Message area with scroll
            ScrolledComposite scrolledComposite = new ScrolledComposite(mainComposite, SWT.V_SCROLL | SWT.BORDER);
            scrolledComposite.setExpandHorizontal(true);
            scrolledComposite.setExpandVertical(true);
            GridData scrolledData = new GridData(SWT.FILL, SWT.TOP, true, false);
            scrolledData.heightHint = msgAreaHeight;
            scrolledComposite.setLayoutData(scrolledData);

            Label msgLabel = new Label(scrolledComposite, SWT.WRAP);
            msgLabel.setText(msg != null ? msg : "");
            int msgWidth = width - 40;
            msgLabel.setSize(msgWidth, SWT.DEFAULT);
            Point labelSize = msgLabel.computeSize(msgWidth, SWT.DEFAULT);
            msgLabel.setSize(labelSize);
            scrolledComposite.setContent(msgLabel);
            scrolledComposite.setMinSize(labelSize);

            // Image (if exists)
            Label imageLabel = null;
            if (file.exists() && file.isFile()) {
                image = new org.eclipse.swt.graphics.Image(display, pngFilePath);
                imageLabel = new Label(mainComposite, SWT.NONE);
                imageLabel.setImage(image);
                GridData imgData = new GridData(SWT.CENTER, SWT.TOP, true, false);
                imgData.widthHint = image.getBounds().width;
                imgData.heightHint = image.getBounds().height;
                imageLabel.setLayoutData(imgData);
                width = Math.max(width, image.getBounds().width + 40);
            }

            // OK Button
            Button okButton = new Button(mainComposite, SWT.PUSH);
            okButton.setText("OK");
            GridData btnData = new GridData(SWT.CENTER, SWT.BOTTOM, true, false);
            btnData.widthHint = 80;
            okButton.setLayoutData(btnData);
            okButton.addListener(SWT.Selection, e -> shell.close());
            mainComposite.setSize(width, msgAreaHeight + (image != null ? image.getBounds().height : 0) + 100);
            shell.setSize(width, msgAreaHeight + (image != null ? image.getBounds().height : 0) + 150);
            shell.open();
            while (!shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            if (image != null) {
                image.dispose();
            }
        } finally {
            shell.dispose();
            if (createdDisplay) {
                display.dispose();
            }
        }
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
