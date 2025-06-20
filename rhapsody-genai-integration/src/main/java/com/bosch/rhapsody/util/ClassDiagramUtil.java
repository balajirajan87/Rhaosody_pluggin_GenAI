package com.bosch.rhapsody.util;

import com.telelogic.rhapsody.core.*;
import com.bosch.rhapsody.constants.Constants;

public class ClassDiagramUtil {

    public static IRPClass addClass(IRPPackage pkg, String className, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(className, Constants.RHAPSODY_CLASS);
            if (element != null && element instanceof IRPClass) {
                return (IRPClass) element;
            } else {
                return pkg.addClass(className);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addClass: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPClass addInterface(IRPPackage pkg, String interfaceName, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(interfaceName,
                    Constants.RHAPSODY_INTERFACE);
            if (element != null && element instanceof IRPClass) {
                return (IRPClass) element;
            } else {
                return (IRPClass) pkg.addNewAggr(Constants.RHAPSODY_INTERFACE, interfaceName);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addInterface: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPType addEnum(IRPPackage pkg, String enumName, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(enumName, Constants.RHAPSODY_TYPE);
            if (element != null && element instanceof IRPType
                    && ((IRPType) element).getKind().equals(Constants.RHAPSODY_ENUMERATION)) {
                return (IRPType) element;
            } else {
                IRPType type = pkg.addType(enumName);
                if (null != type) {
                    type.setKind(Constants.RHAPSODY_ENUMERATION);
                    return type;
                }
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addEnum: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPType addStruct(IRPPackage pkg, String structName, IRPPackage basePackage) {
        try {
            IRPModelElement element = basePackage.findNestedElementRecursive(structName, Constants.RHAPSODY_TYPE);
            if (element != null && element instanceof IRPType
                    && ((IRPType) element).getKind().equals(Constants.RHAPSODY_STRUCTURE)) {
                return (IRPType) element;
            } else {
                IRPType type = pkg.addType(structName);
                if (type != null) {
                    type.setKind(Constants.RHAPSODY_STRUCTURE);
                    return type;
                }
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addStruct: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPAttribute addAttributeToClass(IRPClass clazz, String name, String type, String visibility) {
        try {
            IRPAttribute attr = clazz.findAttribute(name);
            if (attr == null) {
                attr = clazz.addAttribute(name);
            }
            if (attr != null) {
                attr.setTypeDeclaration(type);
                attr.setVisibility(visibility);
                return attr;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addAttributeToClass: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPAttribute addAttributeToType(IRPType rhapsodyStruct, String name, String type, String visibility) {
        try {
            IRPAttribute attr = rhapsodyStruct.findAttribute(name);
            if (attr == null) {
                attr = rhapsodyStruct.addAttribute(name);
            }
            if (attr != null) {
                attr.setTypeDeclaration(type);
                attr.setVisibility(visibility);
                return attr;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addAttributeToType: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPOperation addOperation(IRPClass clazz, String name, String returnType, String visibility) {
        try {
            IRPOperation op = clazz.addOperation(name);
            if (null != op) {
                op.setReturnTypeDeclaration(returnType);
                op.setVisibility(visibility);
                return op;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addOperation: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPArgument addArgument(IRPOperation op, String name, String type) {
        try {
            IRPArgument arg = op.addArgument(name);
            if (null != arg) {
                arg.setTypeDeclaration(type);
                return arg;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addArgument: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPEnumerationLiteral addEnumLiteral(IRPType enumType, String literal, int value) {
        try {
            IRPEnumerationLiteral lit = enumType.addEnumerationLiteral(literal);
            if (null != lit) {
                lit.setValue(Integer.toString(value));
                return lit;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addEnumLiteral: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPObjectModelDiagram addClassDiagram(IRPPackage pkg, String title) {
        try {
            return (IRPObjectModelDiagram) pkg.addNewAggr(Constants.RHAPSODY_OBJECT_MODEL_DIAGRAM, title);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addClassDiagram: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static IRPGraphNode addNote(IRPObjectModelDiagram diagram, String text, int x, int y) {
        try {
            IRPGraphNode note = diagram.addNewNodeByType(Constants.RHAPSODY_NOTE, x, y, 200, 80);
            if (null != note) {
                note.setGraphicalProperty("Text", text);
                return note;
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addNote: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static void createAssociation(IRPClass from, IRPClass to, String description, String end1_multiplicity,
            String end2_multiplicity) {
        try {
            from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_ASSOCIATION_TYPE,
                    isValidMultiplicity(end2_multiplicity) ? end2_multiplicity : "", description);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createAssociation: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createDirectedAssociation(IRPClass from, IRPClass to, String description,
            String end1_multiplicity) {
        try {
            IRPRelation association = from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_ASSOCIATION_TYPE, "", description);
            if (null != association) {
                association.makeUnidirect();
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createDirectedAssociation: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createDependency(IRPModelElement from, IRPModelElement to, String description) {
        try {
            IRPDependency dep = from.addDependencyTo(to);
            if (null != dep) {
                dep.setName(description);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createDependency: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createRealization(IRPClass from, IRPClassifier to) {
        try {
            from.addGeneralization(to);
            IRPGeneralization gen = from.findGeneralization(to.getName());
            if (gen != null)
                gen.changeTo(Constants.RHAPSODY_REALIZATION_TYPE);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createRealization: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createInheritance(IRPClass from, IRPClassifier to, String description) {
        try {
            from.addGeneralization(to);
            IRPGeneralization gen = from.findGeneralization(to.getName());
            if (gen != null)
                gen.setName(description);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createInheritance: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createAggregation(IRPClass from, IRPClass to, String description, String end1_multiplicity,
            String end2_multiplicity) {

        try {
            from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_AGGREGATION_TYPE,
                    isValidMultiplicity(end2_multiplicity) ? end2_multiplicity : "", description);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createAggregation: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createComposition(IRPClass from, IRPClass to, String description, String end1_multiplicity,
            String end2_multiplicity) {
        try {
            IRPRelation comp = from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_AGGREGATION_TYPE,
                    isValidMultiplicity(end2_multiplicity) ? end2_multiplicity : "", description);
            if (null != comp) {
                comp.setRelationType(Constants.RHAPSODY_COMPOSITION_TYPE);
            }
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createComposition: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void addStereotype(IRPObjectModelDiagram diagram, String stereotype, String metaClass) {
        try {
            diagram.addStereotype(stereotype, metaClass);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: addStereotype: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static IRPCollection createNewCollection(IRPApplication app) {
        try {
            return app.createNewCollection();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createNewCollection: " + e.getMessage() + Constants.NEW_LINE);
            return null;
        }
    }

    public static void setCollectionString(IRPCollection collection, int index, String value) {
        try {
            collection.setString(index, value);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: setCollectionString: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void setCollectionSize(IRPCollection collection, int size) {
        try {
            collection.setSize(size);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: setCollectionSize: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void populateDiagram(IRPObjectModelDiagram diagram, IRPCollection elements, IRPCollection relTypes,
            String mode) {
        try {
            diagram.populateDiagram(elements, relTypes, mode);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: populateDiagram: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static java.util.List<?> getGraphicalElements(IRPObjectModelDiagram diagram) {
        try {
            return diagram.getGraphicalElements().toList();
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: getGraphicalElements: " + e.getMessage() + Constants.NEW_LINE);
        }
        return null;
    }

    public static void setGraphicalProperty(IRPGraphElement element, String property, String value) {
        try {
            element.setGraphicalProperty(property, value);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: setGraphicalProperty: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    /**
     * Validates if the given multiplicity string is allowed.
     * Allowed values: "1", "*", "1..*", "0..*", or any whole number.
     * 
     * @param multiplicity the multiplicity string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidMultiplicity(String multiplicity) {
        if (multiplicity == null)
            return false;
        multiplicity = multiplicity.trim();
        // Allowed literals
        if (multiplicity.equals("1") || multiplicity.equals("*") ||
                multiplicity.equals("1..*") || multiplicity.equals("0..*")) {
            return true;
        }
        // Whole number
        if (multiplicity.matches("\\d+")) {
            return true;
        }
        return false;
    }

}