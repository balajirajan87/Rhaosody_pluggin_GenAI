package com.bosch.rhapsody.ui;

import java.io.File;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.bosch.rhapsody.constants.Constants;
import com.bosch.rhapsody.constants.LoggerUtil;
import com.bosch.rhapsody.constants.ProcessingException;
import com.bosch.rhapsody.file.ProcessFiles;
import com.bosch.rhapsody.integrator.GenAiHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.telelogic.rhapsody.core.IRPApplication;
import com.telelogic.rhapsody.core.RhapsodyAppServer;

/**
 * @author AI generated
 */
public class UI {

  GenAiHandler genAiHandler = null;
  String startPythonBackend;

  private Text chatArea;

  public UI(GenAiHandler genAiHandler, String startPythonBackend) {
    this.startPythonBackend = startPythonBackend;
    this.genAiHandler = genAiHandler;
  }

  public static void main(String[] args) {
    IRPApplication app = RhapsodyAppServer.getActiveRhapsodyApplication();
    GenAiHandler aiHandler = new GenAiHandler(app);
    // String startPythonBackend2 = aiHandler.startPythonBackend();

    UI ui = new UI(aiHandler, "abc");
    ui.createUI();
  }

  /**
   * @param shell   - shell input
   * @param isOnTop - boolean to show shell in the top of the display
   */
  public void toggleAlwaysOnTop(Shell shell, boolean isOnTop) {
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

  public void createUI() {

    Map<String, ArrayList<String>> dropdownFileMapping = new HashMap<>();

    Display display = new Display();
    Shell shell = new Shell(display);
    setShellLocation(display, shell);
    toggleAlwaysOnTop(shell, true);

    shell.setText("UML diagram generator");
    shell.setSize(1000, 700);
    shell.setLayout(new GridLayout(1, false));

    File iconFile = new File(Constants.PROFILEPATH + File.separator + "getstarted.gif");
    if (iconFile.exists()) {
      Image icon = new Image(display, iconFile.getAbsolutePath());
      shell.setImage(icon);
    }
   
    CTabFolder tabFolder = new CTabFolder(shell, SWT.BORDER);
    tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    tabFolder.setSimple(false);

    // Tab 1: Text Input Section
    CTabItem tab1 = new CTabItem(tabFolder, SWT.NONE);
    tab1.setText("Data set");

    Composite tab1Composite = new Composite(tabFolder, SWT.NONE);
    tab1Composite.setLayout(new GridLayout(1, false));
    tab1.setControl(tab1Composite);

    Composite comboRow = new Composite(tab1Composite, SWT.NONE);
    GridLayout comboRowLayout = new GridLayout(1, false); // Single column for the combo box
    comboRow.setLayout(comboRowLayout);
    comboRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Label comboLabel = new Label(comboRow, SWT.NONE);
    comboLabel.setText("Select an option:");

    Combo dropdownCombo = new Combo(comboRow, SWT.DROP_DOWN | SWT.READ_ONLY);
    dropdownCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Add three inputs to the combo list
    dropdownCombo.setItems(Constants.options);

    // Initialize mapping for each dropdown option
    for (String option : dropdownCombo.getItems()) {
      dropdownFileMapping.put(option, new ArrayList<>());
    }

    Composite dragDropArea = new Composite(tab1Composite, SWT.BORDER);
    dragDropArea.setLayout(new GridLayout(1, false));
    dragDropArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    dragDropArea.setBackground(display.getSystemColor(SWT.COLOR_GRAY));
    dragDropArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false)); // Adjusted height
    dragDropArea.setEnabled(false);

    // Select File Button
    Button selectFileButton = new Button(dragDropArea, SWT.PUSH);
    selectFileButton.setText("Select File");
    selectFileButton.setFont(new Font(display, "Arial", 12, SWT.BOLD)); // Bold font
    selectFileButton.setForeground(display.getSystemColor(SWT.COLOR_WHITE)); // White text
    selectFileButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    selectFileButton.setEnabled(false);

    GridData dragDropAreaData = new GridData(SWT.FILL, SWT.FILL, true, false);
    dragDropAreaData.heightHint = 80; // Decreased height to 100 pixels
    dragDropArea.setLayout(new GridLayout(2, false)); // Set layout with 2 columns

    Label fileListLabel = new Label(tab1Composite, SWT.NONE);
    fileListLabel.setText("Selected Files:");
    fileListLabel.setFont(new Font(display, "Arial", 14, SWT.BOLD));
    fileListLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
    fileListLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    List fileList = new List(tab1Composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    fileList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fileList.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    fileList.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
    fileList.setFont(new Font(display, "Courier New", 12, SWT.NORMAL));

    Composite buttonRow1 = new Composite(tab1Composite, SWT.NONE);
    GridLayout buttonRowLayout1 = new GridLayout(2, true); // Two columns for two buttons
    buttonRow1.setLayout(buttonRowLayout1);
    buttonRow1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Add "Remove Selected File" button
    Button removeFileButton = new Button(buttonRow1, SWT.PUSH);
    removeFileButton.setText("Remove Selected File");
    removeFileButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Button goToOutputButton = new Button(buttonRow1, SWT.PUSH);
    goToOutputButton.setText("Go to chat");
    goToOutputButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    goToOutputButton.addListener(SWT.Selection, event -> tabFolder.setSelection(1));

    // Move "Submit Text" button to the bottom middle
    Button uploadTextButton = new Button(tab1Composite, SWT.PUSH);
    uploadTextButton.setText("Upload files");
    uploadTextButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
    GridData submitButtonData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
    submitButtonData.widthHint = 120; // Increased width
    submitButtonData.heightHint = 40; // Increased height
    uploadTextButton.setLayoutData(submitButtonData);
    // Add style
    uploadTextButton.setFont(new Font(display, "Arial", 12, SWT.BOLD)); // Bold font
    uploadTextButton.setBackground(display.getSystemColor(SWT.COLOR_BLUE)); // Blue background
    uploadTextButton.setForeground(display.getSystemColor(SWT.COLOR_WHITE)); // White text

    // Add a status text box below the file list
    Text statusTextBoxTab1 = new Text(tab1Composite, SWT.BORDER | SWT.READ_ONLY | SWT.WRAP);
    statusTextBoxTab1.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

    if (!startPythonBackend.contains("error")) {
      statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
      statusTextBoxTab1.setText("Status: " + startPythonBackend);
    } else {
      statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_RED));
      statusTextBoxTab1.setText("Status: Server not ready. " + startPythonBackend);
      uploadTextButton.setEnabled(false);
    }

    // Add a listener to handle selection events
    dropdownCombo.addListener(SWT.Selection, e -> {
      String selectedOption = dropdownCombo.getText();
      selectFileButton.setEnabled(dropdownCombo.getSelectionIndex() != -1);
      dragDropArea.setEnabled(true);
      statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
      statusTextBoxTab1.setText("Status: Option selected - " + selectedOption);
      fileList.removeAll();
    });

    selectFileButton.addListener(SWT.Selection, e -> {
      FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
      // Update the file selection logic
      fileDialog.setText("Select a File");
      fileDialog.setFilterPath(System.getProperty("user.home")); // Default to user's home directory
      fileDialog.setFilterExtensions(new String[] { "*.pdf" }); // Restrict to PDF file type
      String selectedFile = fileDialog.open();
      if (selectedFile != null) {
        String selectedOption = dropdownCombo.getText();
        if (selectedOption.isEmpty()) {
          MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
          messageBox.setMessage("Please select an option from the dropdown before selecting a file.");
          messageBox.open();
        } else {
          fileList.add(selectedFile);
          dropdownFileMapping.get(selectedOption).add(selectedFile);
          String fileName = new java.io.File(selectedFile).getName();
          statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
          statusTextBoxTab1.setText("Status: Seleted file " + fileName);
        }
      }
    });

    removeFileButton.addListener(SWT.Selection, e -> {
      int selectedIndex = fileList.getSelectionIndex();
      if (selectedIndex != -1) {
        String[] selectedItems = fileList.getSelection();
        fileList.remove(selectedIndex);

        String selectedOption = dropdownCombo.getText();
        ArrayList<String> arrayList = dropdownFileMapping.get(selectedOption);
        ArrayList<String> removedFileNames = new ArrayList<>();
        for (String selectedItem : selectedItems) {
          arrayList.remove(selectedItem);// Remove from the dropdownFileMapping
          String fileName = new java.io.File(selectedItem).getName();
          removedFileNames.add(fileName);
        }
        statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_RED));
        statusTextBoxTab1.setText("Status: File removed " + String.join(", ", removedFileNames));

        // Highlight the immediate next item if any
        if (selectedIndex < fileList.getItemCount()) {
          fileList.select(selectedIndex); // Select the next item
        } else if (fileList.getItemCount() > 0) {
          fileList.select(fileList.getItemCount() - 1); // Select the last item if no next item
        }
      } else {
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
        messageBox.setMessage("No file selected to remove.");
        messageBox.open();
      }
    });

    uploadTextButton.addListener(SWT.Selection, e -> {
      if (fileList.getItemCount() == 0) { // Check if no files are uploaded
        MessageBox messageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
        messageBox.setMessage("Please upload/select at least one file before submitting.");
        messageBox.open();
        return; // Stop further execution
      }

      ProcessFiles fileHandler = new ProcessFiles();
      String selectedOption = dropdownCombo.getText();
      if ("ReferenceCode_Docs".equals(selectedOption)) {
        fileHandler.copyFiles("ReferenceCode_Docs", selectedOption, fileList); // Place in ReferenceCode_Docs
      } else {
        fileHandler.copyPdfFile(Constants.ROOTDIR, selectedOption, fileList);
      }

      statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_DARK_YELLOW));
      statusTextBoxTab1.setText("Status: Uploading documents, please wait...");
      statusTextBoxTab1.update();
      String response = uploadDoc(dropdownCombo.getText(), fileList);

      String[] responseParts = response.split(":", 2);

      String responseCode = responseParts[0];
      String responseMessage = "";

      try {
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode jsonResponse = objectMapper.readTree(responseParts[1]);
        JsonNode node = jsonResponse.get("message");
        if (null != node) {
          responseMessage = node.textValue();
        } else {
          responseMessage = responseParts[1];
        }
        if (Integer.valueOf(responseCode) == HttpURLConnection.HTTP_OK) {
          statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_DARK_CYAN));
          statusTextBoxTab1.setText("Status: " + responseMessage);
          LoggerUtil.info(responseMessage);
        } else {
          statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_RED));
          statusTextBoxTab1.setText("Status: " + responseMessage);
          LoggerUtil.error(responseMessage);
        }

      } catch (Exception e1) {
        statusTextBoxTab1.setForeground(display.getSystemColor(SWT.COLOR_RED));
        statusTextBoxTab1.setText("Status: Error parsing server response.");
        LoggerUtil.error("JSON Parsing Error: " + e1.getMessage());
      }
    });

    // Add padding and spacing for better layout
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 10;
    layout.marginHeight = 10;
    layout.verticalSpacing = 10;
    shell.setLayout(layout);

    CTabItem tab2 = new CTabItem(tabFolder, SWT.NONE);
    tab2.setText("Chatbot");

    Composite tab2Composite = new Composite(tabFolder, SWT.NONE);
    tab2Composite.setLayout(new GridLayout(1, false));
    tab2.setControl(tab2Composite);

    // Font for chat area and input field
    Font font = new Font(display, "Arial", 12, SWT.NORMAL); // Adjust font size as needed

    // Chat area with vertical scrollbar occupying top 3/4 of the UI
    chatArea = new Text(tab2Composite, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
    chatArea.setBackground(display.getSystemColor(SWT.COLOR_WHITE));
    chatArea.setFont(font);
    GridData chatAreaData = new GridData(SWT.FILL, SWT.FILL, true, true);
    chatArea.setLayoutData(chatAreaData);

    Composite inputRow1 = new Composite(tab2Composite, SWT.NONE);
    inputRow1.setLayout(new GridLayout(2, false));
    inputRow1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    Combo dropdown = new Combo(inputRow1, SWT.DROP_DOWN | SWT.READ_ONLY);
    dropdown.setItems(Constants.requestType); // Add your options here
    dropdown.select(0); // Select the first entry by default

    // Set layout data for the dropdown to position it above the userInput text
    // field
    dropdown.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // User input area
    Composite inputRow = new Composite(tab2Composite, SWT.NONE);
    inputRow.setLayout(new GridLayout(2, false));
    inputRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Input text box
    Text userInput = new Text(inputRow, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
    GridData userInputLayoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
    userInputLayoutData.widthHint = 500; // Increase width
    userInputLayoutData.heightHint = 60; // Increase height
    userInput.setLayoutData(userInputLayoutData);
    userInput.setFont(new Font(display, "Arial", 11, SWT.NORMAL)); // Slightly larger font

    userInput.setFocus();

    // Send button
    Button sendButton = new Button(inputRow, SWT.PUSH);
    sendButton.setText("Send");
    GridData sendButtonLayoutData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
    sendButtonLayoutData.widthHint = 100; // Increase width
    sendButtonLayoutData.heightHint = 50; // Increase height
    sendButton.setLayoutData(sendButtonLayoutData);
    sendButton.setFont(new Font(display, "Arial", 12, SWT.BOLD)); // Slightly larger font
    sendButton.setEnabled(false);

    userInput.addModifyListener(event -> {
      String inputText = userInput.getText().trim();
      sendButton.setEnabled(!inputText.isEmpty());
    });

    // Add functionality to send messages
    sendButton.addListener(SWT.Selection, event -> {
      String userMessage = userInput.getText();
      if (!userMessage.isEmpty()) {
        sendMessage(dropdown.getText(), userInput);
        // Clear user input
        userInput.setText("");
      }
    });

    // Set the first tab as selected
    tabFolder.setSelection(0);

    Composite buttonRow = new Composite(tab2Composite, SWT.NONE);
    buttonRow.setLayout(new GridLayout(2, false)); // Two columns for two buttons
    buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // Go to Requirement Input button
    Button goToInputButton = new Button(buttonRow, SWT.PUSH);
    goToInputButton.setText("Go to Data set");
    goToInputButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    goToInputButton.addListener(SWT.Selection, event -> tabFolder.setSelection(0));

    // "Clear Chat" button
    Button clearChatButton = new Button(buttonRow, SWT.PUSH);
    clearChatButton.setText("Clear Chat");
    clearChatButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

    // Add functionality to clear chat messages
    clearChatButton.addListener(SWT.Selection, event -> {
      // Clear the chat messages list
      chatArea.setText("");
    });

    // Open the Shell
    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep();
      }
    }
    display.dispose();
  }

  // Simulated bot response generation
  private String generateBotResponse(String userMessage, String requestType) {
    try {
      return genAiHandler.sendRequestToBackend(requestType, userMessage);
    } catch (ProcessingException e) {
      LoggerUtil.error("Error: " + e.getMessage());
    }
    return userMessage;
  }

  private String uploadDoc(String docType, List fileList) {
    StringBuilder filePaths = new StringBuilder();
    for (String filePath : fileList.getItems()) {
      filePaths.append(filePath).append(",");
    }
    try {
      return genAiHandler.uploadDocToBackend(docType, filePaths);
    } catch (ProcessingException e) {
      LoggerUtil.error("Error: " + e.getMessage());
    }
    return "00: Error uploading doc";
  }

  private void sendMessage(String requestType, Text userInput) {
    String userMessage = userInput.getText().trim();
    if (!userMessage.isEmpty()) {
      // Display user message with distinct formatting
      chatArea.append("User Request: \n" + userMessage + "\n" +
          "***********************************************************************************\n\n");

      // Get the bot response
      String botResponse = generateBotResponse(userMessage, requestType);

      // Append bot response with distinct formatting
      chatArea.append("Response: \n" + botResponse + "\n\n" +
          "***********************************************************************************\n\n");

      // Clear input field
      userInput.setText("");

      // Scroll to the bottom
      chatArea.setTopIndex(chatArea.getLineCount() - 1);
    }
  }

}
