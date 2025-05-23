from lark import Transformer, Tree, v_args
from src.scripts.parsing_utils import ParsingUtil

from lark import Transformer, v_args
from src.scripts.parsing_utils import ParsingUtil

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
        for section in sections:
            if "title" in section:
                result["title"] = section["title"]
            elif "skinparam" in section:
                param = section["skinparam"]["param"]
                value = section["skinparam"]["value"]
                result["skinparam"][param] = value
            elif "type" in section and section["type"] == "section":
                result["sections"].append(section)

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
    def diagram(self, *sections):
        return {"type": "diagram", "sections": list(sections)}

    @v_args(inline=True)
    def section(self, *items):
        # If swimlane is present, it's the first item
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
    def swimlane(self, color, identifier):
        return {
            "type": "swimlane",
            "color": str(color),
            "identifier": str(identifier)
        }

    @v_args(inline=True)
    def action(self, text):
        return {
            "type": "action",
            "text": str(ParsingUtil.parse_tree(text))
        }

    @v_args(inline=True)
    def decision(self, condition, then_label, *args):
        then_branch = []
        else_label = None
        else_branch = []
        found_else = False

        for idx, item in enumerate(args):
            # Check for else_label as Tree or string
            if (isinstance(item, Tree) and getattr(item, 'data', None) == 'else_label'):
                else_label = str(item.children[0])
                else_branch = list(args[idx+1:])  # Everything after else_label
                found_else = True
                break
            elif isinstance(item, str) and idx < len(args)-1 and \
                 (isinstance(args[idx+1], dict) or isinstance(args[idx+1], Tree)):
                # Also handle string else_label (rare, but for robustness)
                else_label = item
                else_branch = list(args[idx+1:])
                found_else = True
                break
            else:
                then_branch.append(item)

        return {
            "type": "decision",
            "condition": str(ParsingUtil.parse_tree(condition)),
            "then_label": str(ParsingUtil.parse_tree(then_label)),
            "then": then_branch,
            "else_label": str(else_label) if else_label else None,
            "else": else_branch
        }

    @v_args(inline=True)
    def while_loop(self, condition, *statements):
        return {
            "type": "while_loop",
            "condition": str(ParsingUtil.parse_tree(condition)),
            "body": list(statements)
        }

    @v_args(inline=True)
    def repeat_loop(self, *args):
        # The last three args are condition, then_label, else_label; the rest are statements
        if len(args) < 3:
            raise ValueError("repeat_loop expects at least 3 arguments")
        
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
            elif (isinstance(item, Tree) and getattr(item, 'data', None) == 'condition'):
                condition = str(item.children[0])
            else:
                statements.append(item)

        return {
            "type": "repeat_loop",
            "body": list(statements),
            "condition": str(condition),
            "then_label": str(then_label),
            "else_label": str(else_label)
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