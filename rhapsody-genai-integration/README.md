# Rhapsody GenAI Integration

## Overview
This project integrates IBM Rhapsody with GenAI, providing a framework for generating UML designs and enhancing application capabilities through AI-driven features. It includes a plugin that interacts with Rhapsody's API and leverages GenAI for automated design generation.

## Project Structure
```
rhapsody-genai-integration
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── RhpPlugin.java  # Main plugin implementation
│   │   └── resources                   # Configuration and resource files
│   └── test
│       ├── java
│       │   └── com
│       │       └── example
│       │           └── RhpPluginTest.java  # Unit tests for the plugin
│       └── resources                   # Test resource files
├── lib                                 # External libraries (e.g., rhapsody.jar)
│   └── rhapsody.jar
├── pom.xml                             # Maven configuration file
└── README.md                           # Project documentation
```

## Features
- **Rhapsody Plugin**: Extends Rhapsody with custom menu items and triggers.
- **GenAI Integration**: Automates UML design generation using GenAI.
- **Clean Shutdown**: Ensures proper cleanup of resources during plugin shutdown.

## Prerequisites
- Java 1.8 or higher
- Maven 3.6.0 or higher
- IBM Rhapsody installed
- `rhapsody.jar` placed in the `lib` directory

## Build and Run
1. Build the project:
   ```bash
   mvn clean install
   ```
2. Deploy the plugin to Rhapsody's plugin directory.
3. Launch Rhapsody and use the "Generate UML Design" menu item to trigger GenAI integration.

## License
This project is licensed under the MIT License. See the `LICENSE` file for details.