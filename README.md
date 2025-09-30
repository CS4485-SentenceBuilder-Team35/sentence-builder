# Sentence Builder

## Repo Setup

### Structure (not final):

-   `ui` - JavaFX project

### Pre-requisites:

-   Git
-   VS Code / IntelliJ
-   [OpenJDK 25](https://www.oracle.com/java/technologies/downloads/#java25)  
    **Do these steps after cloning repo if you have multiple JDKs already:**
    -   IntelliJ:
        -   Go to `Settings`
        -   Under `Build, Execution, Deployment`
        -   Under `Build Tools`, click on `Gradle`
        -   Next to `Gradle JVM`, either `Download JDK` directly here or `Add JDK` if you downloaded from link above
    -   VS Code:
        -   Download installer from above and install in a path
        -   Go to Command Palette (**Windows**: `Ctrl + Shift + P` or **Mac**: `Shift + Command + P`)
        -   Search up for `Java: Open Project Settings`
        -   There should be a `JDK Runtime` tab
        -   Click on `Find a local JDK` and select the folder of where you installed openjdk-25
-   Java Extension Pack (if using VS Code)

### Steps

1. Clone the repo in terminal (If using GitHub Desktop, just copy url provided below)

    ```bash
    git clone https://github.com/CS4485-SentenceBuilder-Team35/sentence-builder.git
    ```

2. To run:  
   Gradle automatically has Build and Run tasks defined
    - Click on the Gradle icon (Elephant)
    - Under `Tasks`
    - In `application`
    - Click play on `run`
