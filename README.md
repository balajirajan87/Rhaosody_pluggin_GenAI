# Rhapsody plugin for GenAI

This Project demonstrates the plugin for Rhapsody that can offer Generative AI Support.

# Plugin Architechture

![alt text](images/image.png)

# Python Environment Setup for Windows
Step1: 
install Anaconda Distribution for Windows from ITSP:
https://service-management.bosch.tech/sp?id=sc_cat_item&sys_id=b08ed16c1b83c91078087403dd4bcbb1
![alt text](images/Anaconda_installation.png)
Step2:
Activate your Conda to enable python:
```
Conda list <command to list your conda environments>
conda create --name <command to create your own conda env>
conda activate <your env / base>
```
Step3
create a python virtual Environment (local to your project)
```
python -m venv myvenv
```
Step2:
Activate your Virtual Environment:
```
./myvenv/Scripts/activate
```
Step4:
install the pip packages available as listed in the requirements.txt
```
pip install -r requirements.txt
```
Step4 (optional: only if you are using Jupyter notebook):
create a new Jupyter kernel to run your jupyter notebook.
```
python3 -m ipykernel install --user --name=<your_env_name>
```
Step5 (optional: only if you are using Jupyter notebook):
Select the newly created kernel in your notebook and run.

# Steps for JAVA Setup in Linux (for Rhapsody plugin Compilation), and Rhapsody Pluggin Execution.
Step1:
Copy the rhapsody.jar file located in your Rhapsody installation folder in windows:
```
C:/Program Files/IBM/Rhapsody/Share/JavaAPI
```
and paste it in 
```
rhapsody-plugin/lib/<your rhapsody.jar file>
```
Step2:
in Linux install Java Development Kit.
Install the same version of JDK that you plan to use on Windows (e.g., JDK 11).
```
sudo apt install openjdk-11-jdk
```
Step3:
Install Maven
(Required to build the plugin)
```
sudo apt install maven
```
Step4:
Install the following extensions in VSCode:
```
Java Extension Pack (for Java development).
Maven for Java (for Maven integration).
```
Step5:
Run the following command to build the plugin
```
cd rhapsody-plugin/
mvn clean install
```
The resulting JAR file will be in the target/ directory.

Step6:
Transfer to Windows:
Transfer the compiled JAR file (e.g., rhapsody-plugin-1.0-SNAPSHOT.jar) to the Windows machine.Place the JAR file in the Rhapsody plugins directory 
```
(e.g., C:\Program Files\IBM\Rhapsody\Plugins). 
```
Update the Rhapsody configuration to load the plugin. Open rhapsody.ini (usually located in the Rhapsody installation directory). Add the plugin JAR file to the AddIns section.
```
AddIns=rhapsody-plugin-1.0-SNAPSHOT.jar
```
# Steps for Python Backend Execution.
Step1:
Export the "OPENAI_API_KEY" in Windows System Environment Variables.
![alt text](images/OPENAI_KEY.png)
 Or alternatively use the below launch.json to execute the python file in Debug mode
```
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Python: Debug openai.py",
            "type": "python",
            "request": "launch",
            "program": "${workspaceFolder}/openai.py",
            "console": "integratedTerminal",
            "env": {
                "OPENAI_API_KEY": "<Your API Key>"
            },
            "args": [],
            "justMyCode": true
        }
    ]
}
```
ensure the backend starts without errors and listens on the expected port (e.g., http://localhost:5000).

Step8: Verify Backend Endpoints:
Use tools like Postman, cURL, or a Browser to test the backend endpoints.
```
curl -X POST http://localhost:5000/summarize_requirements -H "Content-Type: application/json" -d '{"feature_query": "Extract requirements related to a <feature>"}'
```

