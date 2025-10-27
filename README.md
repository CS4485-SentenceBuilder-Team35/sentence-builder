# Sentence Builder

## Repo Setup

### Structure (not final):

-   `application/` - JavaFX project
    -   `src/main/java` - Where Java classes are located
        -   `org.utdteamthreefive.backend` - Package for backend stuff
        -   `org.utdteamthreefive.ui` - Package for UI stuff
    -   `src/main/resources` - Where FXML files are located for JavaFX

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

1.  Clone the repo in terminal (If using GitHub Desktop, just copy url provided below)

    ```bash
    git clone https://github.com/CS4485-SentenceBuilder-Team35/sentence-builder.git
    ```

2. Create Environment File


Move into the **application directory**:


```bash
cd application
```


Copy the example environment file:


```bash
cp .env.example .env
```


Open `.env` in your code editor and add your own environment variables:


```bash
DB_HOST=localhost
DB_PORT=3306
DB_NAME=SentenceBuilder
DB_USER=root
DB_PASS=yourpassword
```


✅ **Note:**


* Do **not** commit your `.env` file to GitHub — it’s private.
* Only `.env.example` should be tracked in Git for teammates to copy.


3.  To build:  
    Gradle automatically has Build tasks defined

    -   Method 1:

        -   Click on the Gradle icon (Elephant)
        -   Under `Tasks`
        -   In `application`
        -   Click play on `run`

    -   Method 2:
        -   In terminal (UNIX)
        ```bash
        ./gradlew
        ```
        -   In terminal (Windows)
        ```powershell
        .\gradlew.bat
        ```

4.  To run:
    Gradle automatically has Build tasks defined

    -   Method 1:

        -   Click on the Gradle icon (Elephant)
        -   Under `Tasks`
        -   In `application`
        -   Click play on `run`

    -   Method 2:
        -   In terminal (UNIX)
        ```bash
        ./gradlew run
        ```
        -   In terminal (Windows)
        ```powershell
        .\gradlew.bat run
        ```

        