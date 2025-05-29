package com.bosch.rhapsody.util;

import com.telelogic.rhapsody.core.*;

public class RhapsodyUtil {

    public static IRPProject getActiveProject(IRPApplication app) {
        return app.activeProject();
    }

    public static String getProjectLanguage(IRPProject project) {
        return project.getLanguage();
    }

    public static IRPPackage addPackage(IRPProject project, String packageName) {
        return project.addPackage(packageName);
    }

    public static IRPPackage addPackage(IRPPackage pkg, String packageName,IRPPackage basePackage) {
        IRPModelElement element = basePackage.findNestedElementRecursive(packageName, "Package");
        if (element != null && element instanceof IRPPackage) {
            return (IRPPackage) element;
        } else {
            return (IRPPackage) pkg.addNewAggr("Package", packageName);
        }
    }

    public static IRPClass addClass(IRPPackage pkg, String className, IRPPackage basePackage) {
        IRPModelElement element = basePackage.findNestedElementRecursive(className, "Class");
        if (element != null && element instanceof IRPClass) {
            return (IRPClass) element;
        } else {
            return pkg.addClass(className);
        }
    }

    public static IRPClass addInterface(IRPPackage pkg, String interfaceName,IRPPackage basePackage) {
        IRPModelElement element = basePackage.findNestedElementRecursive(interfaceName, "Interface");
        if (element != null && element instanceof IRPClass) {
            return (IRPClass) element;
        } else {
            return (IRPClass) pkg.addNewAggr("Interface", interfaceName);
        }
    }

    public static IRPType addEnum(IRPPackage pkg, String enumName,IRPPackage basePackage) {
        IRPModelElement element = basePackage.findNestedElementRecursive(enumName, "Type");
        if (element != null && element instanceof IRPType && ((IRPType) element).getKind().equals("Enumeration")) {
            return (IRPType) element;
        } else {
            IRPType type = pkg.addType(enumName);
            type.setKind("Enumeration");
            return type;
        }
    }

     public static IRPType addStruct(IRPPackage pkg, String structName,IRPPackage basePackage) {
        IRPModelElement element = basePackage.findNestedElementRecursive(structName, "Type");
        if (element != null && element instanceof IRPType && ((IRPType) element).getKind().equals("Structure")) {
            return (IRPType) element;
        } else {
            IRPType type = pkg.addType(structName);
            type.setKind("Structure");
            return type;
        }
    }

    public static IRPAttribute addAttributeToClass(IRPClass clazz, String name, String type, String visibility) {
        IRPAttribute attr = clazz.findAttribute(name);
        if (attr == null){
            attr = clazz.addAttribute(name);
        }
        attr.setTypeDeclaration(type);
        attr.setVisibility(visibility);
        return attr;
    }

    public static IRPAttribute addAttributeToType(IRPType rhapsodyStruct, String name, String type,String visibility) {
        IRPAttribute attr = rhapsodyStruct.findAttribute(name);
        if (attr == null){
            attr = rhapsodyStruct.addAttribute(name);
        }
        attr.setTypeDeclaration(type);
        attr.setVisibility(visibility);
        return attr;
    }

    public static IRPOperation addOperation(IRPClass clazz, String name, String returnType, String visibility) {
        IRPOperation op = clazz.addOperation(name);
        op.setReturnTypeDeclaration(returnType);
        op.setVisibility(visibility);
        return op;
    }

    public static IRPArgument addArgument(IRPOperation op, String name, String type) {
        IRPArgument arg = op.addArgument(name);
        arg.setTypeDeclaration(type);
        return arg;
    }

    public static IRPEnumerationLiteral addEnumLiteral(IRPType enumType, String literal, int value) {
        IRPEnumerationLiteral lit = enumType.addEnumerationLiteral(literal);
        lit.setValue(Integer.toString(value));
        return lit;
    }

    public static IRPObjectModelDiagram addClassDiagram(IRPPackage pkg, String title) {
        return (IRPObjectModelDiagram) pkg.addNewAggr("ObjectModelDiagram", title);
    }

    public static IRPGraphNode addNote(IRPObjectModelDiagram diagram, String text, int x, int y) {
        IRPGraphNode note = diagram.addNewNodeByType("Note", x, y, 200, 80);
        note.setGraphicalProperty("Text", text);
        return note;
    }

    public static void createAssociation(IRPClass from, IRPClass to, String description) {
        from.addRelationTo(to, "", "Association", "", "", "Association", "", description);
    }

    public static void createDependency(IRPModelElement from, IRPModelElement to, String description) {
        IRPDependency dep = from.addDependencyTo(to);
        dep.setName(description);
    }

    public static void createRealization(IRPClass from, IRPClassifier to) {
        from.addGeneralization(to);
        IRPGeneralization gen = from.findGeneralization(to.getName());
        if (gen != null) gen.changeTo("Realization");
    }

    public static void createInheritance(IRPClass from, IRPClassifier to, String description) {
        from.addGeneralization(to);
        IRPGeneralization gen = from.findGeneralization(to.getName());
        if (gen != null) gen.setName(description);
    }

    public static void addStereotype(IRPObjectModelDiagram diagram, String stereotype, String metaClass) {
        diagram.addStereotype(stereotype, metaClass);
    }

    public static IRPCollection createNewCollection(IRPApplication app) {
        return app.createNewCollection();
    }

    public static void setCollectionString(IRPCollection collection, int index, String value) {
        collection.setString(index, value);
    }

    public static void setCollectionSize(IRPCollection collection, int size) {
        collection.setSize(size);
    }

    public static void populateDiagram(IRPObjectModelDiagram diagram, IRPCollection elements, IRPCollection relTypes, String mode) {
        diagram.populateDiagram(elements, relTypes, mode);
    }

    public static java.util.List<?> getGraphicalElements(IRPObjectModelDiagram diagram) {
        return diagram.getGraphicalElements().toList();
    }

    public static void setGraphicalProperty(IRPGraphElement element, String property, String value) {
        element.setGraphicalProperty(property, value);
    }

    // ... Add more utility methods as needed for relations, dependencies, etc.
}