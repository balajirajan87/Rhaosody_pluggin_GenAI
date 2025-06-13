import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sys import argv
import logging
from __init__ import process_diagram
import tkinter as tk
from tkinter import messagebox

def print_usage():
    print("Usage: python main.py -t <diagramtype> -i <input_file> -o <output_file> [-v]")
    sys.exit(1)

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

if __name__ == '__main__':
    myargs = getopts(argv)

    if len(myargs) == 0:
        print_usage()

    if '-i' in myargs and '-o' in myargs and '-t' in myargs:
        input_file = myargs['-i']
        output_file = myargs['-o']
        diagram_type = myargs['-t']
        

        try:
            if "class" in diagram_type or "activity" in diagram_type:
                process_diagram(input_file, output_file,diagram_type)
            else:
                root = tk.Tk()
                root.withdraw()  # Hide the root window
                messagebox.showerror("Error", f"Unsupported diagram type: {diagram_type}")
        except Exception as e:
            root = tk.Tk()
            root.withdraw()  # Hide the root window
            messagebox.showerror("Error", f"An error occurred while processing the diagram: {e}")

        

    if '-v' in myargs:
        logging.basicConfig(level=logging.INFO)