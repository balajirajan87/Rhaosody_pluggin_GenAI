# Rhapsody plugin for GenAI

This Project demonstrates the plugin for Rhapsody that can offer Generative AI Support via a python Backend.

# Plugin Architechture

![alt text](images/image.png)

# Python Environment Setup for Windows
Step1: first setup your proxy by following the below link:
https://inside-docupedia.bosch.com/confluence/display/DEVCORNER/Px

Step2: Subscribe to the OPENAI AZURE Instance as below:
https://rb-xorder.bosch.com/o/xplatform-services/get-access-to-azure-openai. 

OpenAI Instance Link: 
<< to be updated>>

Step3: execute the provided bat script.
```
.\python_setup.bat
```
This script will install python and it will ask for OPENAI_API_KEY. Pass the OPENAI_API_KEY you had obtained from the step2. 


# Configuration in Rhapsody:
Step1: First Download our Release and extract the same in the Folder of your choice.

Step2: Create a New Rhapsody project or open an Existing Rhapsody Project.
<div align="left">
  <img src="images/New_Rhp_Project.png" alt="New Rhapsody Project" />
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