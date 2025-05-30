import os
import json
from lark import Lark, exceptions
from transformer.ClassDiagramTransformer import ClassDiagramTransformer

def process_class_diagram(input_file, output_file):
    dir_path = os.path.dirname(os.path.realpath(__file__))
    grammar_file_path = os.path.join(dir_path,  "grammar","classdiagram.lark")

    with open(grammar_file_path, encoding="utf-8") as grammar_file:
        try:
            parser = Lark(
                grammar_file.read(),
                parser='lalr',
                transformer=ClassDiagramTransformer(),
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