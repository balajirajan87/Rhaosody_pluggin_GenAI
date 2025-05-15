
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
        package_dict = {"name": package_name, "classes": [], "interfaces": [], "enums": [], "notes": []}

        for item in contents:
            if isinstance(item, Tree):
                self._process_tree_item(item, package_dict)
            elif isinstance(item, dict):
                self._process_dict_item(item, package_dict)

        return {"package": package_dict}

    @v_args(inline=True)
    def class_(self, name, stereotype=None, *members):
        return {
            "class": {
                "name": str(name),
                "stereotype": str(stereotype) if stereotype else None,
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
        return {
            "interface": {
                "name": str(ParsingUtil.parse_tree(name)),
                "methods": list(methods),
            }
        }

    @v_args(inline=True)
    def relationship(self, source, relation_type, target, description=None):
        return {
            "relationship": {
                "source": str(source),
                "type": str(ParsingUtil.parse_tree(relation_type)),
                "target": str(target),
                "description": str(ParsingUtil.parse_tree(description)).replace("\"", ""),
            }
        }

    @v_args(inline=True)
    def note(self, position, target, description):
        return {
            "note": {
                "position": str(ParsingUtil.parse_tree(position)),
                "target": str(ParsingUtil.parse_tree(target)),
                "description": str(ParsingUtil.parse_tree(description)).replace("\"", ""),
            }
        }

    @v_args(inline=True)
    def attribute(self, visibility, name):
        var_name, type_ = ParsingUtil.parse_attribute(name).split(":")
        return {
            "attribute": {
                "visibility": str(visibility.data),
                "name": var_name.strip(),
                "type": type_.strip(),
            }
        }

    @v_args(inline=True)
    def method(self, visibility, name, params=None, return_type=None):
        method = ParsingUtil.parse_method(name)
        return {
            "method": {
                "visibility": str(visibility.data),
                "name": str(method["name"]),
                "params": method["params"],
                "return_type": str(method["return_type"]),
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
            result["classes"].append(item["class_"])
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
        elif rule == "enum":
            result["enums"].append(self._parse_enum_tree(item))
        elif rule == "note":
            result["notes"].append(self._parse_note_tree(item))

    def _parse_class_tree(self, item):
        class_data = ParsingUtil.parse_class(item)
        return {
            "name": class_data["name"],
            "stereotype": class_data["stereotype"],
            "attributes": class_data["attributes"],
            "methods": class_data["methods"],
        }

    def _parse_interface_tree(self, item):
        return {
            "name": str(item.children[0]),
            "methods": [str(child) for child in item.children[1:]] if len(item.children) > 1 else [],
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

     # Getter for title
    def get_title(self):
        return self.result_template["title"]

    # Getter for skinparam
    def get_skinparam(self):
        return self.result_template["skinparam"]

    # Getter for packages
    def get_packages(self):
        return self.result_template["packages"]

    # Getter for classes
    def get_classes(self):
        return self.result_template["classes"]

    # Getter for interfaces
    def get_interfaces(self):
        return self.result_template["interfaces"]

    # Getter for enums
    def get_enums(self):
        return self.result_template["enums"]

    # Getter for relationships
    def get_relationships(self):
        return self.result_template["relationships"]

    # Getter for notes
    def get_notes(self):
        return self.result_template["notes"]