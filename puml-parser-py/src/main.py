import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sys import argv
import logging
from __init__ import process_diagram
import tkinter as tk

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

def show_error_messagebox(message):
    # Custom error dialog with copyable text
    root = tk.Tk()
    root.withdraw()
    root.attributes('-topmost', True)
    root.update()
    # Calculate height based on number of lines in the message (min 6, max 30)
    num_lines = message.count('\n') + 1
    max_height = 20
    text_height = min(max(num_lines, 6), max_height)
    text_width = 80  # Increased width for better readability

    # Create a new Toplevel window for the error dialog
    error_win = tk.Toplevel(root)
    error_win.title("Puml Validation Error")
    error_win.attributes('-topmost', True)
    error_win.resizable(False, False)


    # Frame for text and scrollbar
    text_frame = tk.Frame(error_win)
    text_frame.pack(padx=10, pady=5)

    # Add a Text widget for the message (read-only, selectable)
    text = tk.Text(text_frame, wrap="word", height=text_height, width=text_width)
    text.insert("1.0", message)
    text.config(state="disabled")  # Make text read-only but selectable

    # Add vertical scrollbar if content exceeds max_height
    if num_lines > max_height:
        scrollbar = tk.Scrollbar(text_frame, command=text.yview)
        text.config(yscrollcommand=scrollbar.set)
        scrollbar.pack(side="right", fill="y")
    text.pack(side="left", fill="both", expand=True)

    # Add a larger OK button
    btn = tk.Button(error_win, text="OK", command=error_win.destroy, width=7, height=2, font=("Arial", 11, "bold"))
    btn.pack(pady=(0, 10))

    # Center the dialog
    error_win.update_idletasks()
    x = (error_win.winfo_screenwidth() // 2) - (error_win.winfo_width() // 2)
    y = (error_win.winfo_screenheight() // 2) - (error_win.winfo_height() // 2)
    error_win.geometry(f"+{x}+{y}")
    error_win.grab_set()
    error_win.focus_force()
    root.wait_window(error_win)
    root.destroy()

if __name__ == '__main__':
    myargs = getopts(argv)

    if len(myargs) == 0:
        print_usage()

    if '-i' in myargs and '-o' in myargs and '-t' in myargs:
        input_file = myargs['-i']
        output_file = myargs['-o']
        diagram_type = myargs['-t']
        diagram_type = diagram_type.lower()
        

        try:
            if "class" in diagram_type or "activity" in diagram_type or "component" in diagram_type:
                process_diagram(input_file, output_file,diagram_type)
            else:
                show_error_messagebox(f"Unsupported diagram type: {diagram_type}")
        except Exception as e:
            show_error_messagebox(f"An error occurred while processing the diagram: {e}")

        

    if '-v' in myargs:
        logging.basicConfig(level=logging.INFO)