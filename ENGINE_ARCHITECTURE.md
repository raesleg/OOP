# Engine Architecture Reference

## INF1009 — Abstract Engine Layer (`io.github.raesleg.engine`)

**Project:** LibGDX + Box2D Top-Down Arcade Car Game
**Engine Package:** `core/src/main/java/io/github/raesleg/engine/`
**Last updated:** 2026-03-23

> **Design contract:** The engine package has **zero knowledge of the game layer**. No class in `engine/` imports anything from `game/`. All game-specific behaviour is injected at the composition root (`Lwjgl3Launcher`) and further injected by `GameMaster` and `SceneManager` at runtime.

---

## 1. Package Overview

```
engine/
├── Constants.java              — shared numeric/string constants (PPM, action names)
├── GameMaster.java             — application entry point; composition root
├── collision/
│   ├── ICollisionListener.java — Observer subscriber contract
│   └── CollisionManager.java   — Box2D ContactListener; fans out to observers
├── entity/
│   ├── Entity.java             — abstract base with position/size/lifecycle
│   ├── EntityManager.java      — double-buffered entity list with dirty-flag snapshot
│   ├── IExpirable.java         — capability: entity can signal self-removal
│   ├── IFlashable.java         — capability: entity can play a damage-flash effect
│   ├── Shape.java              — primitive geometry entity (colour + draw hook)
│   └── TextureObject.java      — textured entity; Flyweight texture cache
├── event/
│   ├── GameEvent.java          — marker interface (engine stays type-agnostic)
│   └── EventBus.java           — typed generic Publish-Subscribe bus
├── io/
│   ├── ActionInput.java        — query interface: isHeld / justPressed
│   ├── CommandHistory.java     — undo stack (Command pattern)
│   ├── ControlSource.java      — Strategy: user input vs AI axes
│   ├── ICommand.java           — Command pattern: execute + undo
│   ├── InputDevice.java        — hardware abstraction with context-stack key bindings
│   ├── IOManager.java          — Facade over input devices + sound output
│   └── SoundDevice.java        — Interface: full sound lifecycle contract
├── movement/
│   ├── AIControlled.java       — sinusoidal ControlSource (AI demo)
│   ├── IMovable.java           — single-method interface: move(dt)
│   ├── MovableEntity.java      — TextureObject + IMovable; bridges controls → physics
│   ├── MovementManager.java    — drives all IMovable entities each frame + physics step
│   ├── MovementModel.java      — Strategy: locomotion algorithm (step + zone hooks)
│   ├── MovementStrategy.java   — Strategy: scripted/AI override for axis values
│   └── UserControlled.java     — keyboard ControlSource (adapts ActionInput)
├── physics/
│   ├── BodyType.java           — enum STATIC/DYNAMIC/KINEMATIC (hides Box2D from game)
│   ├── PhysicsBody.java        — Facade/Wrapper over Box2D Body
│   └── PhysicsWorld.java       — Facade over Box2D World; reuses BodyDef/FixtureDef
├── scene/
│   ├── Scene.java              — abstract Template Method; owns all managers
│   └── SceneManager.java       — Stack-FSM scene lifecycle controller
├── sound/
│   ├── SoundEffect.java        — LibGDX Sound wrapper with loop-state tracking
│   └── SoundManager.java       — implements SoundDevice; named-sound registry
└── system/
    └── IGameSystem.java        — Strategy interface: update(dt) + dispose()
```

---

## 2. Root Classes

### `Constants`

Non-instantiable `final` class holding project-wide numeric and string literals used by the engine.

| Constant                   | Value      | Purpose                                                                    |
| -------------------------- | ---------- | -------------------------------------------------------------------------- |
| `PPM`                      | `100f`     | Pixels-per-meter — scale factor between Box2D (metres) and LibGDX (pixels) |
| `LEFT / RIGHT / UP / DOWN` | `"left"` … | Named action keys wired via `InputDevice.addBind()`                        |
| `ACTION`                   | `"action"` | Confirm / interact action name                                             |

---

### `GameMaster` (extends `ApplicationAdapter`)

**Role:** Application entry point and **Composition Root**.

Wired exclusively by `Lwjgl3Launcher` — the only class in the project that imports concrete types from both the game and engine layers.

**Lifecycle:**
| Method | Responsibility |
|---|---|
| `create()` | `Box2D.init()`, builds `SpriteBatch`, registers UI sounds, creates `IOManager`, creates `SceneManager`, pushes initial scene |
| `render()` | Polls `IOManager`; calls `SceneManager.update()` then `SceneManager.render()` |
| `resize()` | Forwards to `SceneManager` |
| `dispose()` | Tears down `SceneManager`, `IOManager`, `SpriteBatch`, shared texture cache |

**DI chain:** `Lwjgl3Launcher` → `new GameMaster(initialScene, inputDevice, soundDevice)` → `SceneManager.push(scene)` → `scene.setIOManager(ioManager)`

---

## 3. Collision Package

### `ICollisionListener` (Interface)

Observer contract. Every game-layer collision handler implements this.

```java
void onCollisionBegin(Entity a, Entity b)
void onCollisionEnd(Entity a, Entity b)
void onImpact(Entity a, Entity b, float force, Vector2 point)
```

### `CollisionManager` (implements `ContactListener`)

Facade over Box2D's `ContactListener`. Maintains a `CopyOnWriteArrayList<ICollisionListener>` so multiple handlers can subscribe without engine modification.

| Method                               | Behaviour                                                                |
| ------------------------------------ | ------------------------------------------------------------------------ |
| `addListener(ICollisionListener)`    | Registers a new observer                                                 |
| `removeListener(ICollisionListener)` | Unregisters an observer                                                  |
| `beginContact(Contact)`              | Extracts entity pair from fixture user-data; broadcasts to all listeners |
| `endContact(Contact)`                | Broadcasts separation event                                              |
| `postSolve(Contact, ContactImpulse)` | Computes impact force; broadcasts `onImpact` (sensors skipped)           |

**Pattern:** Observer — the engine never hard-codes which game classes react to collisions.

---

## 4. Entity Package

The entity hierarchy follows a **Template Method** pattern. Base classes define the lifecycle contract; subclasses specialise behaviour.

```
Entity  (abstract — position, size, update/draw/dispose hooks)
├── Shape  (abstract — adds Color, ShapeRenderer draw hook)
└── TextureObject  (abstract — Flyweight texture cache, SpriteBatch draw)
    └── MovableEntity  (abstract — added physics body + ControlSource + movement strategies)
```

### `Entity` (abstract)

Root of the entity tree. Stores `x, y, w, h` and defines the three lifecycle hooks all entities share.

| Method              | Default | Purpose              |
| ------------------- | ------- | -------------------- |
| `update(float dt)`  | no-op   | Per-frame logic      |
| `draw(SpriteBatch)` | no-op   | Sprite drawing       |
| `dispose()`         | no-op   | GPU resource release |

### `EntityManager`

Manages the entity list for a single `Scene`. Handles concurrent modification safely via a **pending-add buffer** — entities added mid-frame are deferred to the next `update()` flush.

| Feature                     | Mechanism                                                      |
| --------------------------- | -------------------------------------------------------------- |
| Add during iteration        | `pendingEntities` buffer; flushed at start of `update()`       |
| Auto-removal                | Checks `IExpirable.isExpired()` after each entity's `update()` |
| Snapshot for read-only uses | `cachedSnapshot` rebuilt only when `snapshotDirty = true`      |
| Rendering                   | `render(SpriteBatch)` iterates the live list calling `draw()`  |

### `TextureObject` (abstract, extends `Entity`)

**Flyweight pattern.** All textures are loaded once into a `static Map<String, Texture>` keyed by filename. Multiple entities sharing the same texture file get the same `Texture` object.

| Method                            | Behaviour                                                                    |
| --------------------------------- | ---------------------------------------------------------------------------- |
| Constructor                       | Loads or retrieves texture from cache; sets instance reference               |
| `static getOrLoadTexture(String)` | Cache access without constructing an entity (used by factories)              |
| `dispose()`                       | **No-op per instance** — textures are freed only via `disposeAllTextures()`  |
| `static disposeAllTextures()`     | Frees all cached textures; called once at shutdown in `GameMaster.dispose()` |

### `IExpirable`

Capability interface. `EntityManager` calls `isExpired()` after every update and removes returning-`true` entities automatically.

### `IFlashable`

Capability interface. Any entity that can display a damage-blink effect implements this. Checked by collision handlers that need to suppress repeated damage.

---

## 5. Event Package

A **typed Publish-Subscribe bus** that decouples game systems without introducing cross-system imports.

### `GameEvent` (Marker Interface)

All game-layer event records implement this. The engine only knows the marker — it never imports concrete events.

### `EventBus`

```java
subscribe(Class<T extends GameEvent>, Consumer<T>)  // register handler
publish(T event)                                     // dispatch to matching handlers
clear()                                              // remove all (called on scene dispose)
```

**How it works:** Handlers are stored in a `Map<Class, List<Consumer>>`. On `publish()`, the bus looks up the event's runtime class and calls every matching consumer. The unchecked cast is localised inside `EventBus` — callers are fully type-safe.

**Used for cross-system communication:** `FuelDepletedEvent`, `ScoreChangedEvent`, `PickupCollectedEvent` — game-layer events that travel through the engine bus without the engine knowing their types.

---

## 6. IO Package

### `ICommand` / `CommandHistory`

Standard **Command pattern** implementation.

- `ICommand` — two-method interface: `execute()` and `undo()`
- `CommandHistory` — `ArrayDeque`-backed undo stack; `executeAndRecord()` calls `execute()` then pushes; `undoLast()` pops and calls `undo()`

Used in the game layer for rule violations (`BreakRuleCommand`) — changes can be undone programmatically.

### `ControlSource` (Strategy Interface)

Uniform axis/button interface driving both human and AI movement.

```java
float getX(float dt)       // horizontal axis  [-1, +1]
float getY(float dt)       // vertical axis    [-1, +1]
boolean isAction(float dt) // confirm/fire button
boolean isUserControlled() // distinguishes human from AI
```

Concrete implementations:

- `UserControlled` — adapts `ActionInput` (keyboard) → `ControlSource`
- `AIControlled` — sinusoidal demo path (overridable in game layer with `MovementStrategy`)

### `InputDevice` (Interface)

Hardware abstraction with a **context-stack key binding system**.

| Method                            | Purpose                                                         |
| --------------------------------- | --------------------------------------------------------------- |
| `handleInput()`                   | Polls hardware; fires bound `Runnable` actions                  |
| `addBind(int, Runnable, boolean)` | Maps a key code to an action (pressed or held)                  |
| `removeBind(int)`                 | Removes a key mapping                                           |
| `pushContext()`                   | Saves current bindings; new bindings apply to the new context   |
| `popContext()`                    | Restores previous bindings (SceneManager calls this on `pop()`) |
| `resetToBase()`                   | Clears all contexts; returns to root bindings                   |

### `IOManager` (Facade)

Composite facade over a `List<InputDevice>` and a single `SoundDevice`. The engine and scenes never interact with devices directly — they go through `IOManager`.

| Method                                     | Delegates to                               |
| ------------------------------------------ | ------------------------------------------ |
| `update()`                                 | `handleInput()` on all devices             |
| `getSound()`                               | Returns the `SoundDevice`                  |
| `getInputs(Class<T>)`                      | Type-safe retrieval of a registered device |
| `pushInputContext()` / `popInputContext()` | Broadcasts to all `InputDevice` instances  |

### `SoundDevice` (Interface)

Full 11-method sound contract; implemented by `SoundManager`. Scenes and systems use only `SoundDevice` — DIP compliant.

---

## 7. Movement Package

### Class Relationships

```
ControlSource  (Strategy)
├── UserControlled   — reads keyboard via ActionInput
└── AIControlled     — sinusoidal demo path

MovableEntity  (abstract, extends TextureObject)
├── uses ControlSource  — primary axis source
├── uses MovementStrategy (optional override — NPC scripted paths)
└── uses MovementModel   — actual physics application algorithm

MovementModel  (Strategy Interface)
└── step(PhysicsBody, x, y, dt)   — applies velocity/force to body
    onEnterZone / onExitZone      — surface effect hooks

MovementStrategy  (Strategy Interface)
└── getX/getY(MovableEntity, dt)  — override input axes per-frame
```

### `MovableEntity` (abstract, extends `TextureObject`, implements `IMovable`)

Central bridge between input, physics, and rendering.

| Field                               | Role                              |
| ----------------------------------- | --------------------------------- |
| `ControlSource controls`            | Primary input source (user or AI) |
| `PhysicsBody body`                  | Box2D body wrapper                |
| `MovementModel movementModel`       | Locomotion algorithm              |
| `MovementStrategy movementStrategy` | Optional scripted override        |

**Frame flow:**

1. `move(dt)` — if `MovementStrategy` is set, uses it for axes; else uses `ControlSource`
2. `movementModel.step(body, x, y, dt)` — applies physics
3. `update(dt)` calls `syncPosition()` — converts Box2D metres back to screen pixels

### `MovementManager`

System class. Each frame: iterates every `IMovable` in the `EntityManager` snapshot and calls `move(dt)`, then steps the `PhysicsWorld`.

### `IGameSystem` (Interface — `system/` package)

Minimal Strategy contract composable into any `Scene`.

```java
void update(float deltaTime)
void dispose()
```

Game-layer systems (`SpeedScrollController`, `FuelController`, `AudioController`, `TrafficSpawningSystem`, `CrosswalkEncounterSystem`) all implement this — they are composed into scenes at runtime, not hardcoded.

---

## 8. Physics Package

The physics package provides a **complete Box2D facade**. No class outside `engine/physics/` imports anything from `com.badlogic.gdx.physics.box2d`.

### `BodyType` (Enum)

```
STATIC    — immovable (road boundaries, crosswalk zones)
DYNAMIC   — physically simulated (player car, NPC cars, pedestrians)
KINEMATIC — script-driven position (police car, motion zones)
```

Maps to `BodyDef.BodyType` inside `PhysicsWorld.toBox2D()` — hidden from the game layer.

### `PhysicsBody` (Facade/Wrapper)

Wraps a raw LibGDX `Body`. All physics interactions go through this class.

| Method                        | Purpose                                               |
| ----------------------------- | ----------------------------------------------------- |
| `setVelocity(float, float)`   | Direct velocity assignment                            |
| `applyLinearImpulse(Vector2)` | Force impulse                                         |
| `setPosition(float, float)`   | Teleport (NPC repositioning, police spawn)            |
| `getPosition()`               | Position in Box2D meters                              |
| `setUserData(Object)`         | Stores entity reference (used by collision detection) |
| `destroy()`                   | Safe destruction with `isDestroyed()` guard           |
| `setBullet(boolean)`          | Enables continuous collision detection                |

### `PhysicsWorld` (Facade)

Creates and steps the Box2D simulation. Reuses `BodyDef`, `FixtureDef`, and `PolygonShape` instances across `createBody()` calls to avoid per-frame allocations.

```java
PhysicsBody createBody(
    BodyType type,
    float xMetres, float yMetres,
    float halfW, float halfH,
    float density, float friction,
    boolean isSensor,
    Object userData
)
```

Sensors (`isSensor = true`) are used for crosswalk zones, pickup areas, and boundary triggers — they generate collision events without applying physical forces.

---

## 9. Scene Package

### `Scene` (abstract — Template Method)

Each scene **owns** its own `EntityManager`, `MovementManager`, `CollisionManager`, and camera pair. There is no shared global state between scenes.

**Dual camera setup:**
| Camera | Viewport type | Usage |
|---|---|---|
| `camera` + `viewport` | `FitViewport` (default) or `ExtendViewport` (override hook) | World / gameplay drawing |
| `uiCamera` + `uiViewport` | Always `FitViewport` | HUD / overlay drawing — ignores camera scroll |

**Virtual resolution:** `VIRTUAL_WIDTH = 1280`, `VIRTUAL_HEIGHT = 720` — constant across all scenes.

**Abstract methods subclasses must implement:**

```java
void show()               // scene pushed — create managers, load assets
void update(float dt)     // per-frame logic
void render(SpriteBatch)  // per-frame drawing
```

**Lifecycle hooks (optional override):**

```java
void hide()               // scene going off-screen (another pushed on top)
void pause() / resume()   // application focus events
void handleInput(float dt) // per-frame input beyond key-binding system
protected Viewport createViewport(OrthographicCamera) // swap viewport type
```

### `SceneManager` (Stack FSM)

Manages a `Stack<Scene>` for overlay and transition support.

| Method        | Behaviour                                                                      |
| ------------- | ------------------------------------------------------------------------------ |
| `push(Scene)` | Pauses current top; injects `IOManager`; calls `show()`; pushes input context  |
| `pop()`       | Hides + disposes top; pops input context; resumes the scene below              |
| `set(Scene)`  | Disposes entire stack; resets all input contexts; pushes a fresh base scene    |
| `render()`    | If top scene is `transparent`, renders the scene below first, then renders top |

**Transparent scenes** enable overlay effects (pause menus, game-over panels) rendered on top of a frozen game view.

---

## 10. Sound Package

### `SoundEffect`

Thin wrapper over LibGDX `Sound`. Tracks loop state via a `loopId` field (`-1` = not looping). Exposes `play()`, `loop()`, `stop()`, `setVolume()`, `isLooping()`, `dispose()`.

### `SoundManager` (implements `SoundDevice`)

Registry of named `SoundEffect` instances. All volume operations scale by `masterVolume` and respect the global `muted` flag.

| Feature        | Detail                                                                                |
| -------------- | ------------------------------------------------------------------------------------- |
| Sound registry | `Map<String, SoundEffect>` keyed by logical name (e.g. `"engine"`, `"policesiren"`)   |
| Master volume  | Clamped `[0, 1]`; applied as multiplier on every `play/loop/setVolume` call           |
| Mute           | Flag checked on `playSound()` / `setSoundVolume()` — silences output without stopping |
| Dispose        | Calls `dispose()` on every registered `SoundEffect`                                   |

---

## 11. Design Patterns Summary

| Pattern                  | Engine Class(es)                                                                       |
| ------------------------ | -------------------------------------------------------------------------------------- |
| **Flyweight**            | `TextureObject` — shared GPU texture cache keyed by filename                           |
| **Observer**             | `CollisionManager` → `ICollisionListener` list (CopyOnWriteArrayList)                  |
| **Publish-Subscribe**    | `EventBus` — typed generic bus; engine only knows `GameEvent` marker                   |
| **Strategy**             | `ControlSource`, `MovementModel`, `MovementStrategy`, `IGameSystem`, `SoundDevice`     |
| **Command + Undo**       | `ICommand` + `CommandHistory` — execute/undo stack                                     |
| **Template Method**      | `Scene` (abstract show/update/render; `createViewport` hook)                           |
| **Facade**               | `PhysicsWorld`, `PhysicsBody`, `IOManager`, `SoundManager` (over Box2D / LibGDX)       |
| **Stack FSM**            | `SceneManager` — push/pop/set scene lifecycle                                          |
| **Dependency Injection** | `GameMaster` constructor; `SceneManager` injects `IOManager` into every scene          |
| **Dirty-flag Caching**   | `EntityManager.getSnapshot()` — only rebuilds when modified                            |
| **Composition Root**     | `GameMaster` + `Lwjgl3Launcher` — only these classes import both engine and game types |
| **Marker Interface**     | `GameEvent` — engine type-bound without knowing concrete event types                   |
| **Double Buffer**        | `EntityManager.pendingEntities` — prevents ConcurrentModificationException             |

---

## 12. Key Invariants

1. **No game imports in engine.** The engine package compiles with zero knowledge of `io.github.raesleg.game.*`.
2. **No Box2D imports outside `engine/physics/`.** Game classes use `BodyType`, `PhysicsBody`, `PhysicsWorld` — never raw `com.badlogic.gdx.physics.box2d.*`.
3. **Scene sovereignty.** Each `Scene` owns its own `EntityManager`, `MovementManager`, `CollisionManager`, and camera. No shared mutable global state between scenes.
4. **Flyweight texture ownership.** GPU textures are never disposed per-entity. `TextureObject.disposeAllTextures()` is the single teardown point, called only from `GameMaster.dispose()`.
5. **ControlSource contract.** All entity movement — whether player-driven or AI-driven — flows through the `ControlSource` → `MovementModel` pipeline. Direct velocity manipulation is a last resort (kinematic bodies only).
6. **IGameSystem composition.** Behaviour that would otherwise grow the scene class is extracted into `IGameSystem` implementations and composed at `show()` time. Scenes orchestrate; systems execute.
