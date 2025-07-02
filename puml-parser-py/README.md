# PUML Parser Py

`puml-parser-py` is a Python-based utility designed to parse and process PlantUML (`.puml`) files. It provides tools to analyze, manipulate, and extract information from UML diagrams written in PlantUML syntax.

## Features

- Parse PlantUML files to extract UML elements.
- Support for class diagrams, sequence diagrams, and more.
- Generate structured data (e.g., JSON) from UML diagrams.
- Easy integration into Python projects.
- Lightweight and fast processing.

## Requirements

- Python 3.7 or higher
- Dependencies listed in `requirements.txt`

## Limitations

### Class Diagram

    | Limitation   | Puml Parser   | Json       | Rhapsodydiagram        |
    |--------------|---------------|------------|------------------------|
    | cardinality  |     yes       |    no      |       no               |
    | alias name   |     no        |    no      |       no               |