package com.bosch.rhapsody.genertator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONArray;
import org.json.JSONObject;
import com.bosch.rhapsody.constants.Constants;
import com.telelogic.rhapsody.core.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassDiagram {

    private IRPApplication rhapsodyApp;
    private Map<String, IRPModelElement> elementMap = new HashMap<>();
    private IRPCollection elementstoPopulate;
    private String language;

    public void createClassDiagram(String chatContent,Shell shell) throws IOException {
        rhapsodyApp = Constants.rhapsodyApp;
        String currentDir = System.getProperty("user.dir");
        String inputFile = currentDir + "\\puml-parser-py\\data\\samples\\class_diagram.puml";
        String outputFile = currentDir + "\\puml-parser-py\\data\\processed\\class_diagram.json";
        extractLastPumlBlock(chatContent, inputFile);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "python",
                Constants.PUML_PARSER_PATH,
                "-i", inputFile,
                "-o", outputFile,
                "t","classdiagram"
            );
            processBuilder.redirectErrorStream(true);
            Process pythonBackendProcess = processBuilder.start();
            pythonBackendProcess.waitFor(); // Ensure process completes before reading output
            String jsonString = readJsonFile(outputFile);
            JSONObject json = new JSONObject(jsonString);
            createUML(json,shell);
        } catch (IOException io) {
            throw io;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally{
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(inputFile));
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(outputFile));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void extractLastPumlBlock(String input, String outputFilePath) throws IOException {
        Pattern pattern = Pattern.compile("@startuml[\\s\\S]*?@enduml", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(input);

        String lastBlock = null;
        while (matcher.find()) {
            lastBlock = matcher.group();
        }

        if (lastBlock != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {
                writer.write(lastBlock);
            }
        } else {
            throw new IOException("No @startuml ... @enduml block found.");
        }
    }

    private String readJsonFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createUML(JSONObject jsonObject,Shell shell) {
        IRPProject project = rhapsodyApp.activeProject();
        language = project.getLanguage();
        if(language.equals("C++") || language.equals("C") ){
            IRPPackage model;
            IRPModelElement newPackage = project.findNestedElementRecursive("ClassDiagramElements", "Package");
            if (newPackage != null) {
                // Show info dialog
                MessageBox messageBox = new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
                messageBox.setText("Package Exists");
                messageBox.setMessage("The package 'ClassDiagramElements' already exists. Do you want to overwrite it?");
                int response = messageBox.open();
                if (response == SWT.YES) {
                    // Delete the existing package
                    newPackage.deleteFromProject();
                    model = project.addPackage("ClassDiagramElements");
                } else {
                    // Stop further processing
                    return;
                }
            } else {
                model = project.addPackage("ClassDiagramElements");
            }
            elementstoPopulate = rhapsodyApp.createNewCollection();

            // Handle root-level elements (outside packages)
            createElements(model, jsonObject);

            // Handle packages
            JSONArray packages = jsonObject.optJSONArray("packages");
            if (packages != null) {
                for (int i = 0; i < packages.length(); i++) {
                    JSONObject packageObject = packages.getJSONObject(i);
                    String packageName = packageObject.getString("name").replace(" ", "_");
                    IRPPackage rhapsodyPackage = (IRPPackage) model.addNewAggr("Package", packageName);
                    elementMap.put(packageName, rhapsodyPackage);
                    createElements(rhapsodyPackage, packageObject);
                }
            }

            // Handle calls/relations if present
            JSONArray calls = jsonObject.optJSONArray("relationships");
            if (calls != null) {
                for (int i = 0; i < calls.length(); i++) {
                    JSONObject call = calls.getJSONObject(i);
                    createRelation(call);
                }
            }
            String title = "ClassDiagram";
            Object titleObject = jsonObject.get("title");
            if (titleObject != null && titleObject != JSONObject.NULL) {
                title = titleObject.toString();
            }
            IRPDiagram classDiagram = createBDD(model,title);
            createNote(model, jsonObject,classDiagram);
       }else{
          MessageBox messageBox = new MessageBox(shell, SWT.ERROR | SWT.OK);
          messageBox.setMessage("Expected Project type is C but found "+language);
          messageBox.open();
          return;
       }
    }

    // Handles classes, interfaces, enums, notes in a given container (project or package)
    private void createElements(IRPModelElement container, JSONObject obj) {
        // Classes
        JSONArray classes = obj.optJSONArray("classes");
        if (classes != null) {
            for (int i = 0; i < classes.length(); i++) {
                JSONObject classObject = classes.getJSONObject(i);
                createClass(container, classObject);
            }
        }
        // Interfaces
        JSONArray interfaces = obj.optJSONArray("interfaces");
        if (interfaces != null) {
            for (int i = 0; i < interfaces.length(); i++) {
                JSONObject interfaceObject = interfaces.getJSONObject(i);
                createInterface(container, interfaceObject);
            }
        }
        // Enums
            JSONArray enums = obj.optJSONArray("enums");
            if (enums != null) {
                for (int i = 0; i < enums.length(); i++) {
                    JSONObject enumObject = enums.getJSONObject(i);
                    createEnum(container, enumObject);
                }
            }
    }

    private void createClass(IRPModelElement container, JSONObject classObject) {
        String className = classObject.getString("name").replace(" ", "_");
        IRPClass rhapsodyClass = ((IRPPackage)container).addClass(className);
        elementMap.put(className, rhapsodyClass);
        elementstoPopulate.addItem(rhapsodyClass);

        // Attributes
        JSONArray attributes = classObject.optJSONArray("attributes");
        if (attributes != null) {
            for (int i = 0; i < attributes.length(); i++) {
                JSONObject attribute = attributes.getJSONObject(i);
                String visibility = attribute.optString("visibility", "public");
                String attrName = attribute.getString("name").replace(" ", "_");
                String attrType = attribute.optString("type", "int");
                IRPAttribute attributeElement = rhapsodyClass.addAttribute(attrName);
                attributeElement.setVisibility(visibility);
                attributeElement.setTypeDeclaration(attrType);
            }
        }
        // Methods
        JSONArray methods = classObject.optJSONArray("methods");
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                JSONObject method = methods.getJSONObject(i);
                String visibility = method.optString("visibility", "public");
                String methodName = method.getString("name").replace(" ", "_");
                String returnType = method.optString("return_type", "void");
                IRPOperation newOperation = rhapsodyClass.addOperation(methodName);
                newOperation.setVisibility(visibility);
                newOperation.setReturnTypeDeclaration(returnType);

                JSONArray params = method.optJSONArray("params");
                if (params != null) {
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String paramName = param.getString("name").replace(" ", "_");
                        String paramType = param.optString("type", "Object");
                        IRPArgument argument = newOperation.addArgument(paramName);
                        argument.setTypeDeclaration(paramType);
                    }
                }
            }
        }
    }

    private void createInterface(IRPModelElement container, JSONObject interfaceObject) {
        String interfaceName = interfaceObject.getString("name").replace(" ", "_");
        IRPClass rhapsodyInterface = (IRPClass)((IRPPackage)container).addNewAggr("Interface", interfaceName);

        elementMap.put(interfaceName, rhapsodyInterface);
        elementstoPopulate.addItem(rhapsodyInterface);

        // Methods
        JSONArray methods = interfaceObject.optJSONArray("methods");
        if (methods != null) {
            for (int i = 0; i < methods.length(); i++) {
                JSONObject methodObj = methods.getJSONObject(i);
                JSONObject method = methodObj.optJSONObject("method");
                if (method == null) continue;
                String visibility = method.optString("visibility", "public");
                String methodName = method.optString("name", "unnamed").replace(" ", "_");
                String returnType = method.optString("return_type", "void");

                IRPOperation op = rhapsodyInterface.addOperation(methodName);
                op.setVisibility(visibility);
                op.setReturnTypeDeclaration(returnType);

                JSONArray params = method.optJSONArray("params");
                if (params != null) {
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String paramName = param.optString("name", "param" + j).replace(" ", "_");
                        String paramType = param.optString("type", "Object");
                        IRPArgument argument = op.addArgument(paramName);
                        argument.setTypeDeclaration(paramType);
                    }
                }
            }
        }
    }

    private void createEnum(IRPModelElement container, JSONObject enumObject) {
        String enumName = enumObject.getString("name").replace(" ", "_");
        IRPType rhapsodyEnum = ((IRPPackage)container).addType(enumName);
        rhapsodyEnum.setKind("Enumeration");
        elementMap.put(enumName, rhapsodyEnum);

        JSONArray literals = enumObject.optJSONArray("values");
        if (literals != null) {
            for (int i = 0; i < literals.length(); i++) {
                String literal = literals.getString(i);
                IRPEnumerationLiteral enumerationLiteral = rhapsodyEnum.addEnumerationLiteral(literal);
                enumerationLiteral.setValue(Integer.toString(i));
            }
        }
    }

    private void createNote(IRPModelElement container, JSONObject jsonObject,IRPDiagram classDiagram) {
        JSONArray notes = jsonObject.optJSONArray("notes");
        if (notes != null) {
            int baseX = 100;
            int baseY = 100;
            int offsetY = 120; // vertical spacing between notes
            for (int i = 0; i < notes.length(); i++) {
                JSONObject noteObject = notes.getJSONObject(i);
                String noteText = noteObject.optString("description", "");
                String target = noteObject.optString("target", "");
                // Optionally, allow explicit position from JSON
                int x = noteObject.has("x") ? noteObject.getInt("x") : baseX;
                int y = noteObject.has("y") ? noteObject.getInt("y") : baseY + i * offsetY;
                IRPGraphNode noteNode = classDiagram.addNewNodeByType("Note", x, y, 200, 80);
                noteNode.setGraphicalProperty("Text", noteText);
                IRPModelElement targetElement = elementMap.get(target);
                IRPCollection elements = classDiagram.getCorrespondingGraphicElements(targetElement);
                IRPGraphElement IRPGraphElement = (IRPGraphElement)elements.getItem(1);
                //classDiagram.addNewEdgeByType("anchor", (IRPGraphElement)noteNode, x, i, IRPGraphElement, x, y);
            }
        }
    
    }

    // Example for handling relations/calls (associations, realizations, dependencies)
    private void createRelation(JSONObject call) {
        String from = call.getString("source");
        String to = call.getString("target");
        String type = call.optString("type", "association"); // e.g., association, realization
        String description = call.getString("description");
        IRPModelElement fromElem = elementMap.get(from);
        IRPModelElement toElem = elementMap.get(to);
        if ("association".equals(type)) {
            ((IRPClass)fromElem).addRelationTo((IRPClass)toElem, "", "Association", "", "", "Association", "", description);
        } else if ("dependency".equals(type) || "dotted_dependency".equals(type)) {
            IRPDependency dep = fromElem.addDependencyTo(toElem);
            dep.setName(description);  
        }
        else if("realization".equals(type)) {
             ((IRPClass)fromElem).addGeneralization((IRPClassifier)toElem);
             IRPGeneralization gen =  ((IRPClass)fromElem).findGeneralization(toElem.getName());
             gen.changeTo("Realization");
        }
        else if("inheritance".equals(type)) {
            ((IRPClass)fromElem).addGeneralization((IRPClassifier)toElem);
            IRPGeneralization gen =  ((IRPClass)fromElem).findGeneralization(toElem.getName());
            gen.setName(description);
        }
       /*else if("aggregation".equals(type)) {
            IRPRelation agg =((IRPClass)toElem).addRelationTo((IRPClass)fromElem, toElem.getName(), "Association", "", fromElem.getName(), "Aggregation", "", description);
        }
        else if("composition".equals(type)) {
           // IRPRelation comp =((IRPClass)fromElem).addRelationTo((IRPClass)toElem, toElem.getName(), "Association", "", fromElem.getName(), "Composition", "", description);
            ((IRPClassifier)fromElem).addUnidirectionalRelation(toElem.getName(), "","" , "composition", "", description);
        }*/
    }

    public IRPObjectModelDiagram createBDD(IRPPackage basePackage, String title) {
        IRPObjectModelDiagram NewDiagram = (IRPObjectModelDiagram) basePackage.addNewAggr("ObjectModelDiagram", title);
        NewDiagram.addStereotype("Class Diagram", "ObjectModelDiagram");
        IRPCollection relTypes = rhapsodyApp.createNewCollection();
        relTypes.setSize(1);
        relTypes.setString(1, "AllRelations");
        NewDiagram.populateDiagram(elementstoPopulate, relTypes, "fromto");
        for(Object diagramElement : NewDiagram.getGraphicalElements().toList()){
            if(diagramElement instanceof IRPGraphNode)
            {
                IRPGraphElement diagramGraphElement = (IRPGraphElement) diagramElement;
                IRPModelElement element = diagramGraphElement.getModelObject();
                String metaClass =element.getMetaClass();
                if(metaClass.equals("Interface") || metaClass.equals("Class")){
                    diagramGraphElement.setGraphicalProperty("OperationsDisplay", "All");
                    diagramGraphElement.setGraphicalProperty("AttributesDisplay", "All");
            }
            }
        }
        return NewDiagram;
    }
}