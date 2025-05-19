package com.bosch.rhapsody.genertator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ClassDiagram {

    private static IRPApplication rhapsodyApp;

    private Process pythonBackendProcess;

    public void createClassDiagram(String chatContent) {
        rhapsodyApp = Constants.rhapsodyApp;
        
        //parse chatContent create PUML ,Parse PUML and generate json
        /*ProcessBuilder processBuilder = new ProcessBuilder("python", Constants.PUML_PARSER_PATH);
        processBuilder.redirectErrorStream(true);
        pythonBackendProcess = processBuilder.start();*/

        String currentDir = System.getProperty("user.dir");
        String jsonFilePath = currentDir + "\\puml-parser-py\\data\\processed\\classdiagram.json";
        try {
            // Read JSON file
            String jsonString = readJsonFile(jsonFilePath);
            JSONObject json = new JSONObject(jsonString);
            createUML(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

     private static String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void createUML(JSONObject jsonObject) {
        IRPProject model = rhapsodyApp.activeProject();

        // Create Packages
        JSONArray packages = jsonObject.getJSONArray("packages");
        for (int i = 0; i < packages.length(); i++) {
            JSONObject packageObject = packages.getJSONObject(i);
            String packageName = packageObject.getString("name");
            packageName = packageName.replace(" ", "_");
            IRPPackage rhapsodyPackage = model.addPackage(packageName);
            createPackageElements(rhapsodyPackage, packageObject);
        }

    }

    private static void createPackageElements(IRPPackage rhapsodyPackage, JSONObject packageObject) {
        // Handle classes
        JSONArray classes = packageObject.optJSONArray("classes");
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                JSONObject classObject = classes.getJSONObject(i);
                createClass(rhapsodyPackage, classObject);
            }
        }

        // Handle interfaces
        JSONArray interfaces = packageObject.optJSONArray("interfaces");
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length(); i++) {
                JSONObject interfaceObject = interfaces.getJSONObject(i);
                createInterface(rhapsodyPackage, interfaceObject);
            }
        }
    }

    private static void createClass(IRPPackage rhapsodyPackage, JSONObject classObject) {
        String className = classObject.getString("name");
        IRPClass rhapsodyClass = rhapsodyPackage.addClass(className);

        // Add attributes
        JSONArray attributes = classObject.optJSONArray("attributes");
        if (attributes != null) {
            for (int i = 0; i < attributes.length(); i++) {
                JSONObject attribute = attributes.getJSONObject(i);
                String visibility = attribute.getString("visibility");
                String attrName = attribute.getString("name");
                attrName = attrName.replace(" ", "_");
                String attrType = attribute.getString("type");
                IRPAttribute attributeElement = rhapsodyClass.addAttribute(attrName);
                attributeElement.setVisibility(visibility);
                attributeElement.setTypeDeclaration(attrType);
            }
        }

        // Add methods
        JSONArray methods = classObject.optJSONArray("methods");
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                JSONObject method = methods.getJSONObject(i);
                String visibility = method.getString("visibility");
                String methodName = method.getString("name");
                methodName = methodName.replace(" ", "_");
                String returnType = method.getString("return_type");

                IRPOperation newOperation = rhapsodyClass.addOperation(methodName);
                newOperation.setVisibility(visibility);
                newOperation.setReturnTypeDeclaration(returnType);

                JSONArray params = method.optJSONArray("params");
                String paramStr = "";
                if (params != null) {
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String paramName = param.getString("name");
                        paramName = paramName.replace(" ", "_");
                        String paramType = param.getString("type");
                        paramStr = paramType+" "+paramName;       
                        IRPArgument argument = newOperation.addArgument(paramName);
                        argument.setTypeDeclaration(paramType);
                    }
                } 

            }
        }


    }

    private static void createInterface(IRPPackage rhapsodyPackage, JSONObject interfaceObject) {
      /*   String interfaceName = interfaceObject.getString("name");
        IRPInterfa rhapsodyInterface = rhapsodyPackage.addInterface(interfaceName);

        // Add methods to the interface
        JSONArray methods = interfaceObject.optJSONArray("methods");
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                JSONObject method = methods.getJSONObject(i);
                String visibility = method.getString("visibility");
                String methodName = method.getString("name");
                String returnType = method.getString("return_type");

                JSONArray params = method.optJSONArray("params");
                StringBuilder paramStr = new StringBuilder();
                if (params != null) {
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String paramName = param.getString("name");
                        String paramType = param.getString("type");
                        paramStr.append(paramType).append(" ").append(paramName);
                        if (j < params.length() - 1) {
                            paramStr.append(", ");
                        }
                    }
                }

                rhapsodyInterface.addMethod(methodName, returnType, paramStr.toString());
            }
        }*/
    }
}