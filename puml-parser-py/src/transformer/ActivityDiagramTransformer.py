from lark import Transformer, Tree, v_args
from utils.parsing_utils import ParsingUtil

from lark import Transformer, v_args
from utils.parsing_utils import ParsingUtil

class ActivityDiagramTransformer(Transformer):
    def __init__(self):
        self.result_template = {
            "title": None,
            "skinparam": {},
            "sections": []
        }

    @v_args(inline=True)
    def start(self, *sections):
        result = self.result_template.copy()
        result["skinparam"] = {}
        result["sections"] = []
        result["partitions"] = []
        for section in sections:
            if "title" in section:
                result["title"] = section["title"]
            # elif "skinparam" in section:
            #     param = section["skinparam"]["param"]
            #     value = section["skinparam"]["value"]
            #     result["skinparam"][param] = value
            elif "type" in section and section["type"] == "section":
                result["sections"].append(section)
            elif "type" in section and section["type"] == "partition":
                result["partitions"].append(section)

        return result
    

    @v_args(inline=True)
    def title(self, description):
        return {"title": ParsingUtil.parse_tree(description)}

    @v_args(inline=True)
    def skinparam(self,*args):
        return {
            "skinparam": {
                # "param": str(ParsingUtil.parse_tree(sparam)),
                # "value": str(ParsingUtil.parse_tree(value)),
            }
        }

    @v_args(inline=True)
    def diagram(self, *sections):
        return {"type": "diagram", "sections": list(sections)}

    @v_args(inline=True)
    def section(self, *items):
        if items and isinstance(items[0], dict) and items[0].get("type") == "swimlane":
            swimlane = items[0]
            statements = list(items[1:])
        else:
            swimlane = None
            statements = list(items)
        return {
            "type": "section",
            "swimlane": swimlane,
            "statements": statements
        }

    @v_args(inline=True)
    def swimlane(self, *args):
        if len(args) == 2:
            color, identifier = args
        elif len(args) == 1:
            color = None
            identifier = args[0]
        else:
            color = None
            identifier = None
        return {
            "type": "swimlane",
            "color": str(color) if color else None,
            "identifier": str(identifier) if identifier else None
        }

    @v_args(inline=True)
    def action(self, text):
        return {
            "type": "action",
            "text": str(ParsingUtil.parse_tree(text))
        }

    @v_args(inline=True)
    def decision(self, condition, then_label, *rest):
        """
        Handles nested decisions and statements for if/else if/else blocks.
        """
        # Helper to extract else_if and else blocks
        else_ifs = []
        else_block = None
        idx = 0
        then_statements = []

        while idx < len(rest):
            item = rest[idx]
            if isinstance(item, Tree) and item.data == "else_if_block":
                break
            if isinstance(item, Tree) and item.data == "else_block":
                break
            then_statements.append(item)
            idx += 1

        while idx < len(rest):
            item = rest[idx]
            if isinstance(item, Tree) and item.data == "else_if_block":
                c, t, *stmts = item.children
                else_ifs.append({
                    "condition": str(ParsingUtil.parse_tree(c)),
                    "then_label": str(ParsingUtil.parse_tree(t)),
                    "statements": [self.transform(s) if isinstance(s, Tree) else s for s in stmts]
                })
                idx += 1
            else:
                break

        if idx < len(rest):
            item = rest[idx]
            if isinstance(item, Tree) and item.data == "else_block":
                children = item.children
                if children and not isinstance(children[0], Tree):
                    else_label = None
                    else_statements = [self.transform(s) if isinstance(s, Tree) else s for s in children]
                else:
                    else_label = str(ParsingUtil.parse_tree(children[0]))
                    else_statements = [self.transform(s) if isinstance(s, Tree) else s for s in children[1:]]
                else_block = {
                    "else_label": else_label,
                    "statements": else_statements
                }

        return {
            "type": "decision",
            "condition": str(ParsingUtil.parse_tree(condition)),
            "then_label": str(ParsingUtil.parse_tree(then_label)),
            "then_statements": then_statements,
            "else_ifs": else_ifs,
            "else_block": else_block
        }

    @v_args(inline=True)
    def else_if_block(self, condition, then_label, *statements):
        return Tree("else_if_block", [condition, then_label, *statements])

    @v_args(inline=True)
    def else_block(self, *args):
        return Tree("else_block", list(args))

    @v_args(inline=True)
    def while_loop(self, while_condition, *statements):
        body = []
        then_label = None
        else_label = None

        for item in statements:
            if isinstance(item, Tree) and getattr(item, 'data', None) == 'then_label':
                then_label = str(item.children[0])
            elif isinstance(item, Tree) and getattr(item, 'data', None) == 'else_label':
                else_label = str(item.children[0])
            else:
                body.append(item)

        return {
            "type": "while_loop",
            "condition": str(ParsingUtil.parse_tree(while_condition)),
            "then_label": then_label,
            "else_label": else_label,
            "body": body
            
        }

    @v_args(inline=True)
    def repeat_loop(self, *args):
        
        statements = []
        else_label = None
        then_label = None
        condition = None
        for idx, item in enumerate(args):
            # Check for else_label as Tree or string
            if (isinstance(item, Tree) and getattr(item, 'data', None) == 'else_label'):
                else_label = str(item.children[0])
            elif (isinstance(item, Tree) and getattr(item, 'data', None) == 'then_label'):
                then_label = str(item.children[0])
            elif (isinstance(item, Tree) and getattr(item, 'data', None) == 'while_condition'):
                condition = str(item.children[0])
            elif (isinstance(item, Tree) and getattr(item, 'data', None) == 'repeat_loop_labels'):
                # Iterate over item.children to find condition, then_label, else_label
                for child in item.children:
                    if isinstance(child, Tree) and getattr(child, 'data', None) == 'then_label':
                        then_label = str(child.children[0])
                    elif isinstance(child, Tree) and getattr(child, 'data', None) == 'else_label':
                        else_label = str(child.children[0])

            else:
                statements.append(item)

        return {
            "type": "repeat_loop",
            "body": list(statements),
            "condition": str(condition),
            "then_label": str(then_label) if then_label else None,
            "else_label": str(else_label) if else_label else None,
        }
    
    @v_args(inline=True)
    def switch(self, switch_condition, *case_blocks):
        """
        Parses a switch block.
        - switch_condition: the condition/expression for the switch
        - case_blocks: one or more case_block dicts
        """
        cases = []
        for case in case_blocks:
            if isinstance(case, dict):
                cases.append(case)
            elif isinstance(case, Tree) and hasattr(self, 'transform'):
                cases.append(self.transform(case))
        return {
            "type": "switch",
            "condition": str(ParsingUtil.parse_tree(switch_condition)),
            "cases": cases
        }

    @v_args(inline=True)
    def case_block(self, case_value, *statements):
        """
        Parses a case block inside a switch.
        - case_value: the value for the case
        - statements: statements inside the case, ending with a break
        """
        stmts = []
        for stmt in statements:
            # Exclude the break statement from the statements list, but mark if present
            if isinstance(stmt, dict) and stmt.get("type") == "break":
                continue
            stmts.append(stmt)
        return {
            "type": "case",
            "value": str(ParsingUtil.parse_tree(case_value)),
            "statements": stmts
        }

    @v_args(inline=True)
    def note(self, description=None):
        return {
            "type": "note",
            "description": str(ParsingUtil.parse_tree(description)).replace("\"", "") if description else None,
        }

    @v_args(inline=True)
    def partition(self, partition_name, *statements):
        return {
            "type": "partition",
            "name": str(ParsingUtil.parse_tree(partition_name)),
            "statements": list(statements)
        }

    @v_args(inline=True)
    def start_activity(self):
        return {"type": "start"}

    @v_args(inline=True)
    def statement(self, statement):
        return statement

    @v_args(inline=True)
    def break_statement(self):
        return {"type": "break"}
    
    @v_args(inline=True)
    def transition(self, text):
        return {
            "type": "transition",
            "text": str(ParsingUtil.parse_tree(text))
        }

    @v_args(inline=True)
    def stop(self):
        return {"type": "stop"}
    
    @v_args(inline=True)
    def end(self):
        return {"type": "end"}

    @v_args(inline=True)
    def COLOR(self, token):
        return str(ParsingUtil.parse_tree(token))

    @v_args(inline=True)
    def IDENTIFIER(self, token):
        return str(ParsingUtil.parse_tree(token))

    @v_args(inline=True)
    def TEXT(self, token):
        return str(ParsingUtil.parse_tree(token))