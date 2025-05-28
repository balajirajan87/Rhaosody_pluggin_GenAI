"""Module for parsing PlantUML diagrams to an AST"""

# Copyright 2024 René Fischer - renefischer@fischer-homenet.de
# Copyright 2018 Pedro Cuadra - pjcuadra@gmail.com
# Licensed under the Apache License, Version 2.0


from src.classdiagram.ClassDiagramTransformer import ClassDiagramTransformer


def getopts(argvalues):
    """Function parsing command line options"""
    opts = {}  # Empty dictionary to store key-value pairs.
    while argvalues:  # While there are arguments left to parse...
        if argvalues[0][0] == '-':  # Found a "-name value" pair.
            if len(argvalues) > 1:
                if argvalues[1][0] != '-':
                    opts[argvalues[0]] = argvalues[1]
                else:
                    opts[argvalues[0]] = True
            elif len(argvalues) == 1:
                opts[argvalues[0]] = True

        # Reduce the argument list by copying it starting from index 1.
        argvalues = argvalues[1:]
    return opts