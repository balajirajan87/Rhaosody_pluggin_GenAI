# Building and Testing the Executable

## Build Instructions

1. **Set Up Environment**
   - Create a new conda environment with Nuitka installed, or install Nuitka in your existing environment.
     ```
     conda create -n myvenv python=3.XX
     conda activate myvenv
     pip install nuitka
     ```
   - Ensure any other dependencies required by your project are installed in this environment.

2. **Configure the Batch Script**
   - If you use a different conda environment name, update the `call conda activate myvenv` line in `exe_generator.bat` accordingly.

3. **Run the Executable Generator**
   - From the `buildspec` directory, run:
     ```
     exe_generator.bat
     ```
   - The executable will be generated in the `buildspec` directory as `pumlparser.exe`.

## Testing the Executable

1. **Edit `test_run.bat`**
   - Update the following parameters in `test_run.bat`:
     - `-i` : Full path to your input `.puml` file
     - `-o` : Full path where the output `.json` should be saved
     - `-t` : Diagram name

2. **Run the Test Script**
   - Execute:
     ```
     test_run.bat
     ```
   - The output JSON will be created at the specified path.

---
**Note:**  
- The build process uses Nuitka with the `--standalone` and `--onefile` options, and includes the `grammar` file.
- Make sure all paths in the batch files are correct relative to your project structure.