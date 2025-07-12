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


    public static void createAggregation(IRPClass from, IRPClass to, String description, String end1_multiplicity,
            String end2_multiplicity) {

        try {
            from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    CommonUtil.isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_AGGREGATION_TYPE,
                    CommonUtil.isValidMultiplicity(end2_multiplicity) ? end2_multiplicity : "", description);
        } catch (Exception e) {
            Constants.rhapsodyApp.writeToOutputWindow(Constants.LOG_TITLE_GEN_AI_PLUGIN,
                    "ERROR: createAggregation: " + e.getMessage() + Constants.NEW_LINE);
        }
    }

    public static void createComposition(IRPClass from, IRPClass to, String description, String end1_multiplicity,
            String end2_multiplicity) {
        try {
            IRPRelation comp = from.addRelationTo(to, "", Constants.RHAPSODY_ASSOCIATION_TYPE,
                    CommonUtil.isValidMultiplicity(end1_multiplicity) ? end1_multiplicity : "", "",
                    Constants.RHAPSODY_AGGREGATION_TYPE,
                    CommonUtil.isValidMultiplicity(end2_multiplicity) ? end2_multiplicity : "", description);
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

}