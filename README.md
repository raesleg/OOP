# INF1009 OOP ABSTRACT ENGINE

This is a Java 2D game/engine project built using **LibGDX** and **Box2D**.  
The repository contains the engine code along with a demo application that shows how the systems work together.

The project is structured to separate:
- Engine code (movement, physics, IO, entities, scenes)
- Demo/game-specific code (scenes, test entities, tuning, assets)

---

## Requirements

- Java JDK 8 or newer (JDK 11+ recommended)
- Gradle (or use the Gradle wrapper included in the project)
- A desktop environment that supports OpenGL (for LibGDX)

---

## Getting the Project

Clone the repository:

```bash
git clone https://github.com/raesleg/OOP.git
cd <your-repo-folder>
```

## Running Gradle
```bash
./gradlew desktop:run
```