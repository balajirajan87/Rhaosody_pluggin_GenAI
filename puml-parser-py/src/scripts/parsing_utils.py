from lark import Token, Tree

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
            elif isinstance(child, Tree) and child.data == 'type':
                # Extract the type
                type_value = child.children[0].value if isinstance(child.children[0], Token) else None

        if var_value and type_value:
            return f"{var_value}:{type_value}"
        else:
            raise ValueError("Invalid tree structure. Missing 'var' or 'type'.")
        
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
                            if param:
                                if param.data == 'variable':
                                    var_name = None
                                    var_type = None
                                    for var_child in param.children:
                                        if var_child.data == 'var':
                                            var_name = var_child.children[0].value if isinstance(var_child.children[0], Token) else None
                                        elif var_child.data == 'type':
                                            var_type = var_child.children[0].value if isinstance(var_child.children[0], Token) else None
                                    if var_name and var_type:
                                        params.append({"name": var_name, "type": var_type})
                elif child.data == 'type':
                    # Extract the return type
                    return_type = child.children[0].value if isinstance(child.children[0], Token) else None

        if not method_name:
            raise ValueError("Invalid tree structure. Missing 'method_name'.")
        
        return {
            "name": method_name,
            "params": params,
            "return_type": return_type
        }
    
    def parse_class(tree):
        """
        Parse a Tree object representing a class and extract its name, stereotype, attributes, and methods.

        :param tree: The Tree object to parse.
        :return: A dictionary with 'name', 'stereotype', 'attributes', and 'methods'.
        """
        if not isinstance(tree, Tree) or tree.data != 'class':
            raise ValueError("Invalid tree structure. Expected a 'class' tree.")

        class_name = None
        stereotype = None
        attributes = []
        methods = []

        for child in tree.children:
            if isinstance(child, Tree):
                if child.data == 'class_name':
                    # Extract the class name
                    class_name = child.children[0].value if isinstance(child.children[0], Token) else None
                elif child.data == 'stereotype':
                    # Extract the stereotype
                    stereotype = child.children[0].value if isinstance(child.children[0], Token) else None
            elif isinstance(child, dict):
                # Extract attributes and methods
                if 'attribute' in child:
                    attributes.append(child['attribute'])
                elif 'method' in child:
                    methods.append(child['method'])

        if not class_name:
            raise ValueError("Invalid tree structure. Missing 'class_name'.")

        return {
            "name": class_name,
            "stereotype": stereotype,
            "attributes": attributes,
            "methods": methods
        }
    

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