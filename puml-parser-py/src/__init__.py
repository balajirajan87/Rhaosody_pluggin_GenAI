import os
import json
from lark import Lark, exceptions
from transformer.ClassDiagramTransformer import ClassDiagramTransformer
from transformer.ActivityDiagramTransformer import ActivityDiagramTransformer
from transformer.ComponentDiagramTransformer import ComponentDiagramTransformer

def process_diagram(input_file, output_file, diagram_type):
    """
    Processes a diagram file (class or activity) and writes the parsed output as JSON.

    Args:
        input_file (str): Path to the input .puml file.
        output_file (str): Path to the output .json file.
        diagram_type (str): Type of diagram ('class' or 'activity').
    """
    dir_path = os.path.dirname(os.path.realpath(__file__))
    if  "class" in diagram_type:
        grammar_file = "classdiagram.lark"
        transformer = ClassDiagramTransformer()
    elif "activity" in diagram_type:
        grammar_file = "activitydiagram.lark"
        transformer = ActivityDiagramTransformer()
    elif "component" in diagram_type:
        grammar_file = "componentdiagram.lark"
        transformer = ComponentDiagramTransformer()
    else:
        raise ValueError("Unsupported diagram_type. Use 'class' or 'activity'.")

    grammar_file_path = os.path.join(dir_path, "grammar", grammar_file)

    with open(grammar_file_path, encoding="utf-8") as grammar_file:
        try:
            parser = Lark(
                grammar_file.read(),
                parser='lalr',
                transformer=transformer,
                start='start'
            )

            try:
                with open(input_file, encoding="utf-8") as puml:
                    parsed_data = parser.parse(puml.read())
                    try:
                        json_like_output = json.dumps(parsed_data, indent=4).replace("None", "\"\"")
                        with open(output_file, 'w', encoding="utf-8") as outfile:
                            outfile.write(json_like_output)
                    except (json.JSONDecodeError) as e:
                        raise Exception(e)
            except (OSError) as e:
                raise Exception(e)

        except exceptions.LarkError as e:
            raise exceptions.LarkError(e)
