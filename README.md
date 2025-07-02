# Rhapsody plugin for GenAI

This Project demonstrates the plugin for Rhapsody that can offer Generative AI Support via a python Backend.

# Plugin Architechture

![alt text](images/image.png)

# Python Environment Setup for Windows
Step1: first setup your proxy by following the below link:
https://inside-docupedia.bosch.com/confluence/display/DEVCORNER/Px

Step2: Subscribe to the OPENAI AZURE Instance as below:
https://rb-xorder.bosch.com/o/xplatform-services/get-access-to-azure-openai. 

OpenAI Instance Link: "MS-EPB1-XC OpenAI Instance"

Step3: execute the provided bat script.
```
.\python_setup.bat
```
This script will install python and it will ask for OPENAI_API_KEY. Pass the OPENAI_API_KEY you had obtained from the step2. 


# Configuration in Rhapsody:
Step1: Create a New Rhapsody project or open an Existing Rhapsody Project.
<div align="left">
  <img src="images/New_Rhp_Project.png" alt="New Rhapsody Project" />
</div>

Step2: Once you create this new project, in the File Explorer go to the path of your project. Within your project folder you will notice a folder with name: "<Project_name>_rpy" being created. Now download our Release and extract the contents within the above created folder.
<div align="left">
  <img src="images/Folder_Structure.png" alt="Folder Structure" />
</div>

Step3: Now you need to configure your project. For the created / available project go to file > Add to Model
<div align="left">
  <img src="images/Rhp_Configuration.png" alt="Rhapsody Configuration" />
</div>

This will ask to load .sbsx file. This file will be available from our release. Go to the release Download folder and select the "GenAiIntegrationProfile.sbsx" file. After selection you can see the Rhapsody plugin for GenAI being enabled. 
<div align="left">
  <img src="images/GenAI_Plugin_Enabled.png" alt="GenAI Plugin Enabled" />
</div>

Now you can see your plugin working:
<div align="left">
  <img src="images/Plugin_loaded.png" alt="Plugin Loaded" />
</div>

Cheers!!
Now you can feed in your Documents and start Chatting with your Documents. 

<div align="left">
  <img src="images/Document_Selection.png" alt="Document Selection" />
</div>

# Steps to generate the UML Design inside the Rhapsody:

Step1: Select the function: "Summarize_Requirements" to summarize all the requirements specific to a feature:
<div align="left">
  <img src="images/Summarize_Requirements.png" alt="Document Selection" />
</div>

Step2:
Select the Function "Extract Design Information" to extract all the relevant design information that are related to the summarized requirements. 
<div align="left">
  <img src="images/Extract_Design_Information.bmp" alt="Document Selection" />
</div>

Step3:
Optionally you can also choose to extract the design from the available code information by providing .c and .h files. 

Step4:
generate the plantUML code for the SWE.3 Diagram of your choice (Class or Activity Diagram).
<div align="left">
  <img src="images/Create_UML_Design.bmp" alt="Document Selection" />
</div>

Now your Diagram would appear in the Rhapsody Window with all the associated project elements. 
<div align="left">
  <img src="images/Rhapsody_Class_Diagram.bmp" alt="Document Selection" />
</div>

Additionally if you want any corrections to the previously generated Diagram you can prompt for the corrections and the tool would regenerate the same. 

Happy Designing !!