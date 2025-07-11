from lark import Transformer, Tree, v_args
from utils.parsing_utils import ParsingUtil


class ComponentDiagramTransformer(Transformer):
    def __init__(self):
        self.result_template = {
            "title": None,
            "skinparam": {},
            "actors":[],
            "components":[],
            "interfaces":[],
            "relationships": [],
            "notes": [],
            "packages":[],
            "databases":[],
            "clouds":[],
            "nodes":[]
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
    def skinparam(self, *args):
        return {
            "skinparam": {
                # "param": str(ParsingUtil.parse_tree(sparam)),
                # "value": str(ParsingUtil.parse_tree(value)),
            }
        }
    
    def _process_dict_item(self, item, result):
        if "title" in item:
            result["title"] = item["title"]
        elif "note" in item:
            result["notes"].append(item["note"])
        elif "actor" in item:
            result["actors"].append(item["actor"])
        elif "component" in item:
            result["components"].append(item["component"])
        elif "interface" in item:
            result["interfaces"].append(item["interface"])
        elif "relationship" in item:
            result["relationships"].append(item["relationship"])
        elif "package" in item:
            result["packages"].append(item["package"])
        elif "database" in item:
            result["databases"].append(item["database"])
        elif "cloud" in item:
            result["clouds"].append(item["cloud"])
        elif "node" in item:
            result["nodes"].append(item["node"])

    def _process_tree_item(self, item, result):
        rule = item.data
        if rule == "note":
            result["notes"].append(self._parse_note_tree(item))
        elif rule == "stereotype_name":
            result["stereotype"] = ParsingUtil.parse_tree(item)
    
    def _parse_note_tree(self, item):
        return {
            "position": str(item.children[0]),
            "target": str(item.children[1]),
            "description": str(item.children[2]) if len(item.children) > 2 else None,
        }
    
    @v_args(inline=True)
    def note(self, position, target, content):
        """
        Handles both inline and multiline notes.
        - position: note_position (Tree) or None
        - target: note_target (Token) or None
        - content: note_inline or note_multiline (Tree/Token)
        """
        note_dict = {
            "position": str(position.data) if position else None,
            "target": str(ParsingUtil.parse_tree(target)) if target else None,
            "description": None
        }
        if content is not None:
            # Preserve content as-is, including newlines and formatting
            if hasattr(content, 'children'):
                # Multiline: concatenate all children as they are, preserving newlines
                note_dict["description"] = "".join([
                    str(ParsingUtil.parse_tree(child)) + ("\n" if i < len(content.children) - 1 else "")
                    for i, child in enumerate(content.children)
                ])
            else:
                # Inline: preserve as-is
                note_dict["description"] = str(content)
        return {"note": note_dict}
    
    @v_args(inline=True)
    def actor(self, name):
        return {
            "actor": {
               "name" : str(name)
            }
        }
    
    @v_args(inline=True)
    def component1(self, *args):
        """
        Parses a component definition.
        Extracts:
        - component_name: required, from arg tree
        - alias_name: optional, from arg tree
        - stereotype: optional, from arg tree
        """
        component_dict = {
            "name": None,
            "alias_name": None,
            "stereotype": None
        }

        # Parse the Tree to extract component_name, alias_name, stereotype
        for child in args:
            if hasattr(child, 'data'):
                if child.data == "component_name":
                    component_dict["name"] = ParsingUtil.parse_tree(child)
                elif child.data == "alias_name":
                    component_dict["alias_name"] = ParsingUtil.parse_tree(child)
                elif child.data == "stereotype":
                    component_dict["stereotype"] = ParsingUtil.parse_tree(child)

        return {"component": component_dict}

    @v_args(inline=True)
    def component(self, *args):
        """
        Parses a component definition.
        Extracts:
        - component_name: required, from arg tree
        - alias_name: optional, from arg tree
        - stereotype: optional, from arg tree
        """
        component_dict = {
            "name": None,
            "alias_name": None,
            "stereotype": None,
            "color": None,
            "body": []
        }

        if args and isinstance(args[0], Tree):
            for child in args:
                if hasattr(child, 'data'):
                    if child.data == "component_name":
                        component_dict["name"] = ParsingUtil.parse_tree(child)
                    elif child.data == "alias_name":
                        component_dict["alias_name"] = ParsingUtil.parse_tree(child)
                    elif child.data == "stereotype":
                        component_dict["stereotype"] = ParsingUtil.parse_tree(child)
                    elif child.data == "color_name":
                        component_dict["color"] = ParsingUtil.parse_tree(child)
        else:
            # Fallback: positional parsing if not a Tree
            if len(args) > 0:
                if args[0] is not None:
                    component_dict["name"] = str(ParsingUtil.parse_tree(args[0])).replace("\"", "")
            # If the last argument is a list, treat it as the component body
            if len(args) > 0 and isinstance(args[-1], list):
                component_dict["body"] = args[-1]
            for child in args:
                if hasattr(child, 'data'):
                    if child.data == "alias_name":
                        component_dict["alias_name"] = ParsingUtil.parse_tree(child)
                    elif child.data == "stereotype":
                        component_dict["stereotype"] = ParsingUtil.parse_tree(child)
                    elif child.data == "color_name":
                        component_dict["color"] = ParsingUtil.parse_tree(child)
        
        return {"component": component_dict}

    @v_args(inline=True)
    def component_body(self, *elements):
        # Return a list of component elements
        return list(elements)

    @v_args(inline=True)
    def component_element(self, element_name):
        return ParsingUtil.parse_tree(element_name)
    
    @v_args(inline=True)
    def interface(self, name, alias_name=None):
        """
        Parses an interface definition.
        - name: The interface name (ESCAPED_STRING)
        - alias_name: Optional alias (CNAME)
        """
        interface_dict = {
            "name": ParsingUtil.parse_tree(name).replace("\"", ""),
            "alias_name": alias_name
        }
        if alias_name is not None:
            interface_dict["alias_name"] = ParsingUtil.parse_tree(alias_name)
        return {"interface": interface_dict}
    
    @v_args(inline=True)
    def relationship(self, source, rel_type, target, description=None):
        """
        Parses a relationship line.
        - source: relation_source
        - rel_type: relation_type
        - target: relation_target
        - description: optional description after colon
        """
        rel_dict = {
            "source": ParsingUtil.parse_tree(source),
            "type": rel_type.data if rel_type.data else None,
            "target": ParsingUtil.parse_tree(target)
        }
        if description is not None:
            rel_dict["description"] = ParsingUtil.parse_tree(description)
        return {"relationship": rel_dict}
    
    @v_args(inline=True)
    def package(self, name, *contents):
        package_name = str(ParsingUtil.parse_tree(name)).replace("\"", "")
        package_dict = {
            "name": package_name,
            "stereotype": None,
            "actors": [],
            "components": [], 
            "interfaces": [],
            "notes": [],
            "relationships": [],
            "databases":[],
            "clouds":[],
            "nodes":[]
            }

        for item in contents:
            if isinstance(item, Tree):
                self._process_tree_item(item, package_dict)
            elif isinstance(item, dict):
                self._process_dict_item(item, package_dict)

        return {"package": package_dict}
    
    @v_args(inline=True)
    def database(self, name, alias_name=None):
        """
        Parses a database definition.
        - name: The database name (ESCAPED_STRING or CNAME)
        - alias_name: Optional alias (CNAME)
        """
        db_dict = {
            "name": ParsingUtil.parse_tree(name).replace("\"", "")
        }
        if alias_name is not None:
            db_dict["alias_name"] = ParsingUtil.parse_tree(alias_name)
        return {"database": db_dict}

    @v_args(inline=True)
    def cloud(self, name, alias_name=None):
        """
        Parses a cloud definition.
        - name: The cloud name (ESCAPED_STRING or CNAME)
        - alias_name: Optional alias (CNAME)
        """
        cloud_dict = {
            "name": ParsingUtil.parse_tree(name).replace("\"", "")
        }
        if alias_name is not None:
            cloud_dict["alias_name"] = ParsingUtil.parse_tree(alias_name)
        return {"cloud": cloud_dict}

    @v_args(inline=True)
    def node(self, name, alias_name=None):
        """
        Parses a node definition.
        - name: The node name (ESCAPED_STRING or CNAME)
        - alias_name: Optional alias (CNAME)
        """
        node_dict = {
            "name": ParsingUtil.parse_tree(name).replace("\"", "")
        }
        if alias_name is not None:
            node_dict["alias_name"] = ParsingUtil.parse_tree(alias_name)
        return {"node": node_dict}