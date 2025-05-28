import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sys import argv
import logging
from src import getopts
from src.classdiagram import process_class_diagram
from src.activitydiagram import process_activity_diagram 
import tkinter as tk
from tkinter import messagebox

def print_usage():
    print("Usage: python main.py -t <diagramtype> -i <input_file> -o <output_file> [-v]")
    sys.exit(1)

if __name__ == '__main__':
    myargs = getopts(argv)

    if len(myargs) == 0:
        print_usage()

    if '-i' in myargs and '-o' in myargs and '-t' in myargs:
        input_file = myargs['-i']
        output_file = myargs['-o']
        diagram_type = myargs['-t']
        

        try:
            if diagram_type == "classdiagram":
                process_class_diagram(input_file, output_file)
            elif diagram_type == "activitydiagram":
                process_activity_diagram(input_file, output_file)  # Handle activity diagrams
            else:
                print(f"Unsupported diagram type: {diagram_type}")
                print_usage()
        except Exception as e:
            root = tk.Tk()
            root.withdraw()  # Hide the root window
            messagebox.showerror("Error", f"An error occurred while processing the diagram: {e}")

        

    if '-v' in myargs:
        logging.basicConfig(level=logging.INFO)