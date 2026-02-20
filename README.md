# INF1009 OOP ABSTRACT ENGINE

This is a Java 2D game/engine project built using **LibGDX** and **Box2D**.  
The repository contains the engine code along with a demo application that shows how the systems work together.

The project is structured to separate:
- Engine code (movement, physics, IO, entities, scenes)
- Game-specific code 


## Requirements

- Java JDK 8 or newer (JDK 21 recommended)
- Gradle (or use the Gradle wrapper included in the project)
- A desktop environment that supports OpenGL (for LibGDX)


## Installation

To set up and run the project locally, follow these steps:

### 1. Clone the repository:
```bash
git clone https://github.com/raesleg/OOP.git
```

### 2. Running Gradle
cd <your-repo-folder) before running:

```bash
.\gradlew lwjgl3:run
```

### 3. (OPTIONAL) Build Gradle without running
```bash
.\gradlew build
```
