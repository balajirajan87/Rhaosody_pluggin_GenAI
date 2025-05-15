import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sys import argv
import logging
from src import getopts
from src.classdiagram import process_class_diagram

def print_usage():
    print("Usage: python main.py -i <input_file> -o <output_file> [-v]")
    sys.exit(1)

if __name__ == '__main__':
    myargs = getopts(argv)

    if len(myargs) == 0:
        print_usage()

    if '-i' in myargs:
        input_file = myargs['-i']
        output_file = myargs['-o']
        process_class_diagram(input_file, output_file)

    if '-v' in myargs:
        logging.basicConfig(level=logging.INFO)