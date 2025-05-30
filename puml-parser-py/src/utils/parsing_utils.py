from lark import Token, Tree
import re

class ParsingUtil:
    
    # Function to parse the Tree objects and extract values
    @staticmethod
    def parse_tree(tree):

        # If the tree is a Token, return its value
        if isinstance(tree, Token):
            return tree.value
        
        # If the tree is a Tree, iterate through its children
        elif isinstance(tree, Tree):
            children = [ParsingUtil.parse_tree(child) for child in tree.children]
            # If the result is a list with one entry, return it as a string
            return children[0] if len(children) == 1 else " ".join(map(str, children))
        
        # If the input is a list, handle it similarly
        elif isinstance(tree, list):
            return tree[0] if len(tree) == 1 else " ".join(map(str, tree))

        return str(tree)  # Default case to ensure a string is returned

    @staticmethod
    def parse_attribute(tree):
        """
        Parse a Tree object representing an attribute and return it in the format 'var_value:type_value'.
        
        :param tree: The Tree object to parse.
        :return: A string in the format 'var_value:type_value'.
        """
        if not isinstance(tree, Tree) or tree.data != 'variable':
            raise ValueError("Invalid tree structure. Expected a 'variable' tree.")

        var_value = None
        type_value = None

        # Iterate through the children of the 'variable' tree
        for child in tree.children:
            if isinstance(child, Tree) and child.data == 'var':
                # Extract the variable name
                var_value = child.children[0].value if isinstance(child.children[0], Token) else None
            elif isinstance(child, Tree) and child.data == 'varaible_type':
                # Extract the type
                type_value = child.children[0].value if isinstance(child.children[0], Token) else None
                type_value = type_value.replace(":", "")  # Remove array brackets if present

        if var_value and type_value:
            return f"{var_value}:{type_value}"
        else:
            raise ValueError("Invalid tree structure. Missing 'var' or 'varaible_type'.")
    
    @staticmethod
    def parse_method(tree):
        """
        Parse a Tree object representing a method and extract its name, parameters, and return type.

        :param tree: The Tree object to parse.
        :return: A dictionary with 'name', 'params', and 'return_type'.
        """
        if not isinstance(tree, Tree) or tree.data != 'function':
            raise ValueError("Invalid tree structure. Expected a 'function' tree.")

        method_name = None
        params = []
        return_type = None

        for child in tree.children:
            if isinstance(child, Tree):
                if child.data == 'method_name':
                    # Extract the method name
                    method_name = child.children[0].value if isinstance(child.children[0], Token) else None
                elif child.data == 'param_list':
                    # Extract the parameters
                    if child.children:
                        for param in child.children:
                            param_obj = {}
                            if param:
                                param_obj["name"] =  ParsingUtil.extract_token_value_by_type(param['param']['name'],'CNAME')
                                param_obj["type"] = ParsingUtil.extract_token_value_by_type(param['param']['type'],'ALPHANUM_SPECIAL')

                            if "name" in param_obj and "type" in param_obj:
                                params.append(param_obj)
                elif child.data == 'return_type':
                    # Extract the return type
                    return_type = child.children[0].value if isinstance(child.children[0], Token) else None

        if not method_name:
            raise ValueError("Invalid tree structure. Missing 'method_name'.")
        
        return {
            "name": method_name,
            "params": params,
            "return_type": return_type
        }
    
    def extract_CNAME_token_value(tree_str):
        # Extracts the value inside Token('CNAME', '...')
        match = re.search(r"Token\('CNAME', '([^']+)'\)", tree_str)
        return match.group(1) if match else None
    
    def extract_ALPHANUM_SPECIAL_token_value(tree_str):
        # Extracts the value inside Token('CNAME', '...')
        match = re.search(r"Token\('ALPHANUM_SPECIAL', '([^']+)'\)", tree_str)
        return match.group(1) if match else None
    
    @staticmethod
    def extract_token_value_by_type(tree_str, token_type):
        """
        Extracts the value inside Token('<token_type>', '...') from a string.
        :param tree_str: The string representation of the token.
        :param token_type: The token type to extract (e.g., 'CNAME', 'ALPHANUM_SPECIAL').
        :return: The extracted value or None if not found.
        """
        pattern = rf"Token\('{re.escape(token_type)}', '([^']+)'\)"
        match = re.search(pattern, tree_str)
        return match.group(1) if match else None

    @staticmethod
    def parse_class(tree):
        """
        Parse a Tree object representing a class and extract its name, stereotype, attributes, and methods.

        :param tree: The Tree object to parse.
        :return: A dictionary with 'name', 'stereotype', 'attributes', and 'methods'.
        """
        if not isinstance(tree, Tree) or tree.data != 'class':
            raise ValueError("Invalid tree structure. Expected a 'class' tree.")

        isAbstract = False
        class_name = None
        stereotype = None
        attributes = []
        methods = []
        implements= []
        extends = []

        for child in tree.children:
            if isinstance(child, Tree):
                if child.data == 'class_name':
                    class_name = child.children[0].value if isinstance(child.children[0], Token) else None
                elif child.data == 'stereotype_name':
                    stereotype = child.children[0].value if isinstance(child.children[0], Token) else None
                elif child.data == 'class_implements':
                    implements = ParsingUtil.parse_interface_extends(child)
                elif child.data == 'class_extends':
                    extends = ParsingUtil.parse_class_extends(child)
                elif child.data == 'abstract':
                    isAbstract = True

            elif isinstance(child, dict):
                if 'attribute' in child:
                    attributes.append(child['attribute'])
                elif 'method' in child:
                    methods.append(child['method'])

        if not class_name:
            raise ValueError("Invalid tree structure. Missing 'class_name'.")

        return {
            "name": class_name,
            "isAbstract":isAbstract,
            "stereotype": stereotype,
            "extends": extends,
            "implements": implements,
            "attributes": attributes,
            "methods": methods
        }
    
    @staticmethod
    def parse_interface(tree):

        """
        Parse a Tree object representing an interface and extract its name, stereotype, and methods.

        :param tree: The Tree object to parse.
        :return: A dictionary with 'name', 'stereotype', and 'methods'.
        """

        methods = []
        extends = []

        for child in tree:
            if isinstance(child, Tree):
                if child.data == 'interface_extends':
                    extends = ParsingUtil.parse_interface_extends(child)
            elif(isinstance(child, dict)):
                if 'method' in child:
                    methods.append(child['method'])

        return {
            "extends": extends,
            "methods": methods
        }

    
    @staticmethod
    def parse_enum_values(tree):
        """
        Parse a Tree object representing enum values and return a list of values.

        :param tree: The Tree object to parse.
        :return: A list of enum values.
        """
        if not isinstance(tree, (list, tuple)):
            raise ValueError("Invalid tree structure. Expected a list or tuple of 'enum_value' trees.")

        enum_values = []

        for child in tree:
            if isinstance(child, Tree) and child.data == 'enum_value':
                # Extract the enum value
                value = child.children[0].value if isinstance(child.children[0], Token) else None
                if value:
                    enum_values.append(value)

        return enum_values
    
    @staticmethod
    def parse_interface_extends(tree):
        """
        Parse a Tree object representing interface_extends and return a list of interface names.
        :param tree: The Tree object to parse.
        :return: A list of interface names.
        """
        if tree is None:
            return []  # Return an empty list if the tree is None

        if not isinstance(tree, Tree) or (tree.data != 'interface_extends'  and  tree.data != 'class_implements'):
            raise ValueError("Invalid tree structure. Expected an 'interface_extends' tree.")

        interface_names = []
        for child in tree.children:
            if isinstance(child, Tree) and child.data == 'interface_list':
                for iface in child.children:
                    if isinstance(iface, Tree) and iface.data == 'interface_name':
                        # Extract the interface name
                        name = iface.children[0].value if isinstance(iface.children[0], Token) else None
                        if name:
                            interface_names.append(name)
        return interface_names
    
    @staticmethod
    def parse_class_extends(tree):
        """
        Parse a Tree object representing class_extends and return a list of interface names.
        :param tree: The Tree object to parse.
        :return: A list of interface names.
        """
        if tree is None:
            return []  # Return an empty list if the tree is None
    
        if not isinstance(tree, Tree) or tree.data != 'class_extends':
            raise ValueError("Invalid tree structure. Expected an 'class_extends' tree.")

        interface_names = []
        for child in tree.children:
            if isinstance(child, Tree) and child.data == 'class_name':
                for iface in child.children:
                    if iface:
                        interface_names.append(str(iface))
        return interface_names
    
    def attribute_unwrapped(attributes):
        elements = []
        for attribute in attributes:
            finalAttribute = attribute.get("attribute",None)
            if finalAttribute:
                isStatic = finalAttribute.get("isStatic", False)
                isAbstract = finalAttribute.get("isAbstract", False)
                visibility = finalAttribute.get("visibility", None)
                var_name = finalAttribute.get("name", "").strip()
                type_ = finalAttribute.get("type", "").strip()
                
                elements.append({
                    "isStatic": isStatic,
                    "isAbstract": isAbstract,
                    "visibility": str(visibility) if visibility else None,
                    "name": var_name,
                    "type": type_,
                })
            
        return elements

