
from lark import Transformer, Tree, v_args
from src.scripts.parsing_utils import ParsingUtil


class ClassDiagramTransformer(Transformer):
    def __init__(self):
        self.result_template = {
            "title": None,
            "skinparam": {},
            "packages": [],
            "classes": [],
            "interfaces": [],
            "structs": [],
            "enums": [],
            "relationships": [],
            "notes": [],
        }

    @v_args(inline=True)
    def start(self, *items):
        result = self.result_template.copy()

        for item in items:
            if isinstance(item, dict):
                self._process_dict_item(item, result)
            elif isinstance(item, Tree):
                self._process_tree_item(item, result)

        return result


    @v_args(inline=True)
    def title(self, description):
        return {"title": ParsingUtil.parse_tree(description)}

    @v_args(inline=True)
    def skinparam(self, sparam, value):
        return {
            "skinparam": {
                "param": str(ParsingUtil.parse_tree(sparam)),
                "value": str(ParsingUtil.parse_tree(value)),
            }
        }

    @v_args(inline=True)
    def package(self, name, *contents):
        package_name = str(ParsingUtil.parse_tree(name)).replace("\"", "")
        package_dict = {
            "name": package_name,
            "classes": [],
            "interfaces": [], 
            "structs": [],
            "enums": [],
            "notes": [],
            "relationships": []
            }

        for item in contents:
            if isinstance(item, Tree):
                self._process_tree_item(item, package_dict)
            elif isinstance(item, dict):
                self._process_dict_item(item, package_dict)

        return {"package": package_dict}

    @v_args(inline=True)
    def class_(self, name, isAbstract=None,stereotype=None, class_extends=None, class_implements=None, *members):
        return {
            "class": {
                "name": str(name),
                "isAbstract": str(isAbstract),
                "stereotype": str(stereotype) if stereotype else None,
                "extends": ParsingUtil.parse_interface_extends(class_extends),
                "implements": ParsingUtil.parse_interface_extends(class_implements),
                "members": list(members),
            }
        }

    @v_args(inline=True)
    def enum(self, name, *values):
        return {
            "enum": {
                "name": str(ParsingUtil.parse_tree(name)),
                "values": ParsingUtil.parse_enum_values(values),
            }
        }

    @v_args(inline=True)
    def interface(self, name, *methods):
        interface_data = ParsingUtil.parse_interface(methods)
        return {
            "interface": {
                "name": str(ParsingUtil.parse_tree(name)),
                "extends": interface_data["extends"],
                "methods": interface_data["methods"]
            }
        }
    
    @v_args(inline=True)
    def struct(self, name,  *attributes):
        return {
            "struct": {
                "name": str(ParsingUtil.parse_tree(name)),
                "attributes": list(attributes)
            }
        }

    @v_args(inline=True)
    def relationship(self, source, relation_type, target, description=None):
        return {
            "relationship": {
                "source": str(ParsingUtil.parse_tree(source)),
                "type": str(relation_type.data),
                "target": str(ParsingUtil.parse_tree(target)),
                "description": str(ParsingUtil.parse_tree(description)).replace("\"", ""),
            }
        }

    @v_args(inline=True)
    def note(self, position, target, description):
        return {
            "note": {
                "position": str(position.data),
                "target": str(ParsingUtil.parse_tree(target)),
                "description": str(ParsingUtil.parse_tree(description)).replace("\"", ""),
            }
        }

    @v_args(inline=True)
    def attribute(self, first=None, second=None, third=None):
        visibility = variable = None
        isAbstract = isStatic = False

        args = [first, second, third]
        args = [a for a in args if a is not None]
        for a in args:
            if hasattr(a, "data"):
                if a.data == "static":
                    isStatic = True
                elif a.data == "abstract":
                    isAbstract = True
                elif a.data in ("public", "private", "protected", "package"):
                    visibility = a
            else:
                variable = a  # Should be the variable tree
        if variable is None:
            variable = args[-1]
        var_name, type_ = ParsingUtil.parse_attribute(variable).split(":")
        return {
            "attribute": {
                "isStatic": isStatic,
                "isAbstract": isAbstract,
                "visibility": str(visibility.data) if visibility else None,
                "name": var_name.strip(),
                "type": type_.strip(),
            }
        }

    @v_args(inline=True)
    def method(self, first=None, second=None, third=None):

        visibility = function = None
        isAbstract = isStatic = False

        args = [first, second, third]
        args = [a for a in args if a is not None]
        for a in args:
            if hasattr(a, "data"):
                if a.data == "static":
                    isStatic = True
                elif a.data == "abstract":
                    isAbstract = True
                elif a.data in ("public", "private", "protected", "package"):
                    visibility = a
            else:
                function = a  # Should be the function tree
        if function is None:
            function = args[-1]
        method_info = ParsingUtil.parse_method(function)
        return {
            "method": {
                "isStatic": isStatic,
                "isAbstract": isAbstract,
                "visibility": str(visibility.data) if visibility else None,
                "name": method_info["name"],
                "params": method_info["params"],
                "return_type": method_info["return_type"],
            }
        }

    @v_args(inline=True)
    def param(self, name, type_):
        return {"param": {"name": str(name), "type": str(type_)}}

    @v_args(inline=True)
    def description(self, *items):
        return " ".join(map(str, items))

    @v_args(inline=True)
    def stereotype(self, name):
        return str(name)
    

    def _process_dict_item(self, item, result):
        if "title" in item:
            result["title"] = item["title"]
        elif "skinparam" in item:
            param = item["skinparam"]["param"]
            value = item["skinparam"]["value"]
            result["skinparam"][param] = value
        elif "package" in item:
            result["packages"].append(item["package"])
        elif "class" in item:
            result["classes"].append(item["class"])
        elif "struct" in item:
            result["structs"].append(item["struct"])
        elif "interface" in item:
            result["interfaces"].append(item["interface"])
        elif "enum" in item:
            result["enums"].append(item["enum"])
        elif "relationship" in item:
            result["relationships"].append(item["relationship"])
        elif "note" in item:
            result["notes"].append(item["note"])

    def _process_tree_item(self, item, result):
        rule = item.data
        if rule == "class":
            result["classes"].append(self._parse_class_tree(item))
        elif rule == "interface":
            result["interfaces"].append(self._parse_interface_tree(item))
        elif rule == "struct":
            result["structs"].append(self._parse_struct_tree(item))
        elif rule == "enum":
            result["enums"].append(self._parse_enum_tree(item))
        elif rule == "note":
            result["notes"].append(self._parse_note_tree(item))

    def _parse_class_tree(self, item):
        class_data = ParsingUtil.parse_class(item)
        return {
            "name": class_data["name"],
            "isAbstract": class_data["isAbstract"],
            "stereotype": class_data["stereotype"],
            "extends": class_data["extends"],
            "implements": class_data["implements"],
            "attributes": class_data["attributes"],
            "methods": class_data["methods"],
        }

    def _parse_interface_tree(self, item):
        return {
            "name": str(item.children[0]),
            "methods": [str(child) for child in item.children[1:]] if len(item.children) > 1 else [],
        }
    
    def _parse_struct_tree(self, item):
        return {
            "name": str(item.children[0]),
            "attributes": [str(child) for child in item.children[1:]] if len(item.children) > 1 else [],
        }

    def _parse_enum_tree(self, item):
        return {
            "name": str(item.children[0]),
            "values": [str(child) for child in item.children[1:]] if len(item.children) > 1 else [],
        }

    def _parse_note_tree(self, item):
        return {
            "position": str(item.children[0]),
            "target": str(item.children[1]),
            "description": str(item.children[2]) if len(item.children) > 2 else None,
        }