# OOP Violations Report

## INF1009 — Object-Oriented Programming Project

**Date updated:** 2026-03-22
**Previous analysis:** 2026-03-20 (19 violations)
**Scope:** All source files under `core/src/main/java/io/github/raesleg/`
**Principles assessed:** Single Responsibility Principle (SRP), Open/Closed Principle (OCP), and general OOP violations (ISP, DIP, Encapsulation, Engine/Game Boundary).

**Status legend:**
- `NEW` — not present in previous report
- `WORSENED` — was present and has gotten worse
- `STILL PRESENT` — unchanged since last report
- `PARTIALLY FIXED` — improvement made but violation not fully resolved
- `FIXED` — resolved (see summary table)

---

## 1. Single Responsibility Principle (SRP) Violations

A class violates SRP when it has **more than one reason to change** — i.e., it bundles two or more unrelated responsibilities.

---

### 1.1 `BaseGameScene` — 5+ responsibilities merged into one class

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** HIGH | **Status:** STILL PRESENT

`BaseGameScene` exceeds ~750 lines because it bundles:

| Responsibility                                    | Evidence                                                                                    |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| Scene lifecycle (show/update/render/dispose)      | `show()`, `update()`, `render()`, `dispose()`                                               |
| Physics world construction + boundary wall setup  | `show()`: creates `PhysicsWorld`, creates four wall bodies manually                         |
| Player entity construction + key bindings         | `show()`: creates `PlayerCar`, creates its `PhysicsBody`, binds all keyboard keys           |
| Audio registration + BGM lifecycle                | `show()`: calls `addSound(…)` for 5 sounds; owns `Music bgm`; `syncBgmVolume()`            |
| Game-over/win detection + scene transition        | `checkLevelEnd()`, `triggerExplosionGameOver()`: spawns overlay, sets scene                 |
| Score state management                            | `addScore()`, `scoreAccumulator`, `scoreBonus`                                              |
| Dashboard update orchestration                    | `update()` directly calls `dashboard.onScoreUpdated()`, `dashboard.onSpeedChanged()`, etc. |
| Move-loop sound management                        | `updateMoveLoop()`, `stopMoveLoop()`                                                        |
| Explosion overlay spawning                        | `triggerExplosionGameOver()` directly constructs `ExplosionOverlay` and `ExplosionParticle` |

**Why SRP is violated:** Each row is a separate reason to change. A change to audio logic, the score formula, physics wall layout, or the explosion effect each requires editing the same 750-line class. The Template Method pattern (`BaseGameScene` as base) is correct, but the base class has absorbed too many concrete responsibilities that belong in collaborator classes.

---

### 1.2 `NPCCarSpawner` — spawning + update + lane-query + excessive debug output

**File:** `core/.../game/factory/NPCCarSpawner.java`
**Severity:** MEDIUM | **Status:** WORSENED

The spawner now contains **five** `System.out.println` calls (increased from 1 in the previous revision).

| Responsibility                                             | Evidence                                                                    |
| ---------------------------------------------------------- | --------------------------------------------------------------------------- |
| Spawning new NPC cars on a timer                           | `spawnRandomCar()`                                                          |
| Updating every NPC car's screen position each frame        | `update()` calls `npc.updatePosition(scrollOffset, screenHeight)`           |
| Maintaining a parallel `activeNPCs` tracking list          | `private final List<NPCCar> activeNPCs` — duplicates `EntityManager` state  |
| Providing lane-occupancy query service                     | `getOccupiedLanesNear()`                                                    |
| Printing debug output                                      | 5× `System.out.println(…)` — violates guideline D-2                        |

**Why SRP is worsened:** Spawning, per-frame entity movement, and lane-query are three separate reasons to change. The five debug prints are an additional maintenance smell that ship into the build, further polluting the spawner's responsibilities.

---

### 1.3 `RoadHazardSpawner` — spawning + rendering + per-entity update

**File:** `core/.../game/factory/RoadHazardSpawner.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT *(formerly `PuddleSpawner`)*

| Responsibility                                    | Evidence                                                                         |
| ------------------------------------------------- | -------------------------------------------------------------------------------- |
| Spawning hazard zones on a timer                  | `spawnRandomHazard()`                                                            |
| Updating each hazard's scroll position per frame  | `update()` iterates hazards and repositions them                                 |
| Rendering all hazards (draw pass)                 | `render(SpriteBatch batch)` — rendering belongs in the entity                   |
| Maintaining parallel lane-state bookkeeping       | State list duplicated outside the entity                                         |

**Why SRP is violated:** A spawner should only spawn. Rendering and per-frame scroll updates belong in the entity's `draw()` and `update()`, driven by `EntityManager`. Having all three in the spawner means a change to hazard rendering, hazard movement, or hazard creation all require editing the same class.

---

### 1.4 `DashboardUI` — HUD model state + pop-up lifecycle + rendering + resource management

**File:** `core/.../game/state/DashboardUI.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

| Responsibility                                           | Evidence                                                                                              |
| -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| State model (score lerp, progress, rules broken, speed)  | `displayScore`, `targetScore`, `SCORE_LERP_SPEED` logic in `act()`                                   |
| Score pop-up spawn and lifetime management               | `ScorePopup` inner class, `popups` list, `showScorePopup()`                                           |
| Texture/font loading and resource disposal               | Constructor: `new Texture(…)` for 5 textures; `dispose()`                                            |
| HUD rendering (5 distinct draw methods)                  | `drawDashboardSpeed`, `drawWantedStars`, `drawProgressBar`, `drawPoliceDistanceBar`, `drawScorePopups`|

**Why SRP is violated:** The logic for *what to show* (model) is tangled with *how to render it* (view) and *managing GPU resources* (resource lifecycle). A change to score lerp speed requires touching the same class as a texture coordinate tweak.

---

### 1.5 `GameCollisionHandler` — inline NPC-hits-pedestrian block not delegated to sub-handler

**File:** `core/.../game/collision/GameCollisionHandler.java`
**Severity:** MEDIUM | **Status:** PARTIALLY FIXED

The refactoring into 7 sub-handler classes (`CrosswalkCollisionHandler`, `PickupCollisionHandler`, `BoundaryCrashHandler`, etc.) is a meaningful improvement. However, the NPC-hits-pedestrian block (approximately lines 128–138) is still handled inline inside `GameCollisionHandler.onCollisionBegin()` with direct `instanceof` checks rather than being delegated to a dedicated handler.

**Why SRP is still partially violated:** The top-level handler still contains inline collision-type branching logic instead of fully delegating all cases to sub-handlers. The `instanceof` checks at that location mean the class still has two responsibilities: routing collisions to handlers AND directly responding to one specific collision type.

---

### 1.6 `PoliceCar` — entity state + undelegated chase AI + siren animation

**File:** `core/.../game/entities/PoliceCar.java`
**Severity:** MEDIUM | **Status:** WORSENED

`PoliceMovement.java` now exists but **its entire implementation is commented out**, meaning the chase AI remains entirely inside `PoliceCar.updateChase()`.

| Responsibility                                   | Evidence                                                    |
| ------------------------------------------------ | ----------------------------------------------------------- |
| Entity rendering                                 | `draw()`, extends `TextureObject`                           |
| Chase AI (speed math, X lerp, Y advance)         | `updateChase()` — full algorithm still in entity class      |
| Siren flash animation (timer, frame cycling)     | `flashTimer`, `flashIndex`, `FLASH_FRAMES`, `FLASH_INTERVAL`|
| Static texture bypass                            | `static Texture[] flashTextures` — see violation 3.6       |

**Why SRP is worsened:** The Strategy class (`PoliceMovement`) was scaffolded but never completed — its body is entirely commented out. The AI logic has not moved. The entity now carries more unrelated concerns than it did at the previous analysis.

---

### 1.7 `PoliceMovement` — entire implementation commented out (dead code Strategy class)

**File:** `core/.../game/movement/PoliceMovement.java`
**Severity:** MEDIUM | **Status:** NEW

The entire body of `PoliceMovement.java` is commented out. The class compiles to an empty shell that extends `MovementModel` but provides no behaviour. This is a regression because:

1. `PoliceCar` was intended to delegate its AI movement to this Strategy class, but the delegation never happened
2. A dead code file in a submitted codebase indicates abandoned or incomplete work — the Strategy contract is declared but never fulfilled
3. An empty `MovementModel` subclass violates the spirit of the Strategy pattern: callers cannot rely on the Strategy working as intended

**Why this is a violation:** The class hierarchy claims that `PoliceMovement` is a valid movement strategy, but at runtime it provides no movement. The Liskov Substitution Principle is implicitly violated — substituting this Strategy for any other `MovementModel` produces completely different (broken) behaviour.

---

### 1.8 `Level1Scene.updateGame()` — 100-line god method

**File:** `core/.../game/scene/Level1Scene.java`
**Severity:** MEDIUM | **Status:** NEW

`Level1Scene.updateGame()` is approximately 100 lines long and bundles the following into a single method:

| Responsibility                                 | Evidence                                                                                |
| ---------------------------------------------- | --------------------------------------------------------------------------------------- |
| Spawner orchestration (4 spawners)             | `npcSpawner.update()`, `roadHazardSpawner.update()`, `pickupableSpawner.update()`, `treeSpawner.update()` |
| Per-entity scroll physics updates               | Separate scroll-offset update calls for each entity category                            |
| Crosswalk zone state management                | `crosswalkZone.update(deltaTime)` with inline pedestrian-spawning conditionals          |
| Player position polling                        | `getPlayerCar().getX()` and `.getY()` passed into multiple systems                     |
| Pickup and NPC/pedestrian interaction logic    | Inline logic for checking interactions between independently-spawned entities           |

**Why SRP is violated:** `updateGame()` is a new god method — it has the same structural problem as the old `BaseGameScene` update loop, now relocated into the level subclass. Each spawner coordination call, scroll-update concern, and zone-state management block is a separate reason to change.

---

### 1.9 `NPCCar` — entity + position update + expiry tracking

**File:** `core/.../game/entities/vehicles/NPCCar.java`
**Severity:** LOW | **Status:** STILL PRESENT

`NPCCar` manually manages its physics body position every frame via `updatePosition()`, which is called externally by the spawner. Expiration logic mixes off-screen Y checks with an external `markExpired()` setter. The `relativeY` field is spawn-time state that is reused as a lane-occupancy query key — because it never updates after spawn, occupancy comparisons against live scroll-adjusted positions are unreliable.

---

## 2. Open/Closed Principle (OCP) Violations

A class violates OCP when **adding a new feature requires modifying it** rather than extending it.

---

### 2.1 `GameCollisionHandler` — `instanceof` chains partially remain

**File:** `core/.../game/collision/GameCollisionHandler.java`
**Severity:** HIGH | **Status:** PARTIALLY FIXED

The sub-handler refactoring reduced but did not eliminate `instanceof` usage. Remaining instances:

1. **Inline NPC-pedestrian block (lines ~128–138):** `onCollisionBegin()` still contains direct `instanceof NPCCar` + `instanceof Pedestrian` checks instead of delegating to a handler
2. **`ExplosionCollisionHandler`:** still uses `instanceof` inside its own `handle()` body to branch on entity subtypes

Adding any new collision type still requires **editing existing handler code** rather than only adding a new implementing class.

**Architecture guideline violation:** *"Anti-Patterns: NEVER use `if/else` chains or `instanceof` cascades to select varying behaviors. You must use Strategy, State, or Polymorphism."*

---

### 2.2 `BaseGameScene.checkLevelEnd()` — hardcoded end conditions

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

```java
protected final void checkLevelEnd() {
    if (progress >= 1.0f)  { /* win */ return; }
    if (crashCount >= CRASH_EXPLOSION_THRESHOLD) { /* lose */ return; }
    if (instantFail) { /* lose */ return; }
    if (isGameOver()) { /* lose via hook */ }
}
```

The crash-count threshold and `instantFail` conditions are hardcoded in the base class. Adding a new universal end condition (e.g. a time limit) requires **modifying `BaseGameScene`** — violating OCP for the entire Template Method hierarchy.

---

### 2.3 `RuleManager` — hardcoded per-violation methods, plus semantic misuse in `BreakRuleCommand`

**File:** `core/.../game/rules/RuleManager.java`, `core/.../game/rules/BreakRuleCommand.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

Each violation type has its own method (`recordRedLightViolation`, `recordCrosswalkViolation`, `recordPedestrianHit`, `recordCurbHit`). Adding a new violation type requires adding a new method to `RuleManager` — a direct OCP violation.

**Additional semantic issue:** `BreakRuleCommand` calls `ruleManager.recordCrosswalkViolation()` for **all** violation types regardless of which violation is actually being recorded. This is a logic error introduced when `BreakRuleCommand` was added — the command hardcodes a single violation method rather than dispatching based on the type it was constructed with. The Command pattern exists to parameterize actions, but the action is currently hardcoded.

---

### 2.4 `PhysicsWorld.createBody()` — single shape overload forces modification for new shapes

**File:** `core/.../engine/physics/PhysicsWorld.java`
**Severity:** LOW | **Status:** STILL PRESENT

Only one `createBody()` signature exists (axis-aligned box). Adding a circular or polygon body requires **modifying `PhysicsWorld`** — a violation of OCP for the physics facade. This also means the facade must grow a new method for every new shape, rather than accepting a polymorphic shape descriptor.

---

## 3. General OOP Violations (DIP, ISP, Encapsulation, Engine/Game Boundary)

---

### 3.1 Engine boundary — `BodyDef.BodyType` leaking into game layer (spreading)

**Files:** `BaseGameScene.java`, `Level1Scene.java`, `Level2Scene.java`, `NPCCarSpawner.java`, `PickupableSpawner.java`, `RoadHazard.java`, `MotionZone.java`
**Severity:** HIGH | **Status:** WORSENED (was 3 files, now 7)

All seven files import from `com.badlogic.gdx.physics.box2d` and pass raw `BodyDef.BodyType` constants (`KinematicBody`, `StaticBody`, etc.) directly through or around the `PhysicsWorld` facade. The facade is supposed to hide all Box2D types from the game layer.

**Why this worsened:** Four new game-layer files (`NPCCarSpawner`, `PickupableSpawner`, `RoadHazard`, `MotionZone`) were added since the last analysis that import Box2D types directly, ignoring the established facade boundary. The leakage pattern is spreading to every new class that needs a physics body rather than being contained.

---

### 3.2 DIP — `Level2Scene` directly depends on concrete `PoliceCar`

**File:** `core/.../game/scene/Level2Scene.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

```java
policeCar.updateChase(deltaTime, getPlayerCar().getX(), getPlayerCar().getY(), …);
float distance = getPlayerCar().getY() - policeCar.getScreenY();
policeCar.hasCaughtPlayer();
```

`Level2Scene` depends on the **concrete** `PoliceCar` class rather than an abstraction. If the chase entity type changes (e.g. a helicopter, a drone, a different patrol type), the scene must be modified. There is no interface or abstraction separating the scene from the specific chasing entity.

---

### 3.3 DIP / Observer — `CollisionManager` only accepts a single `ICollisionListener`

**File:** `core/.../engine/collision/CollisionManager.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

```java
public CollisionManager(PhysicsWorld world, ICollisionListener listener) {
    this.listener = listener;
}
```

Only one listener can be registered. Adding a secondary observer (e.g. a debug collision logger, an analytics system, a sound-only listener) requires **modifying `CollisionManager`**. The engine's own Observer pattern implementation is incomplete — a proper Observer supports multiple subscribers.

---

### 3.4 ISP — `TrafficViolationListener` forces unrelated methods onto implementers

**File:** `core/.../game/collision/TrafficViolationListener.java`
**Severity:** MEDIUM | **Status:** NEW

`TrafficViolationListener` declares four methods:
- `onTrafficCrash()`
- `onBoundaryHit()`
- `onCrosswalkViolation()`
- `onPedestrianHit()`

`Level2Scene` implements this interface but has no crosswalk and no pedestrian system. It must provide empty stub implementations for `onCrosswalkViolation()` and `onPedestrianHit()`, which is the canonical ISP violation: *"a client is forced to implement methods it does not use."*

**Why ISP is violated:** The interface bundles methods that belong to two different subsystems (traffic/crash events vs. pedestrian/crosswalk events). Clients that only participate in one subsystem are forced to depend on the other.

---

### 3.5 ISP — `MovableEntity` dual movement abstraction inflates the API

**File:** `core/.../engine/movement/MovableEntity.java`
**Severity:** LOW | **Status:** STILL PRESENT

`MovableEntity` exposes both `MovementModel` and `MovementStrategy` on every movable entity. Only `PlayerCar` actually uses `MovementStrategy`; all other movable entities do not. Every `MovableEntity` subclass therefore inherits `setMovementStrategy()`/`getMovementStrategy()` methods that are irrelevant to them — the API surface is inflated beyond necessity.

**Why ISP is violated:** Subclasses that never use `MovementStrategy` are still dependent on it through their superclass. The two parallel movement abstractions (`MovementModel.step()` for AI and `MovementStrategy.getX/getY()` for player input) also have no clear documented rule distinguishing when to use each, which adds cognitive burden to every new entity implementation.

---

### 3.6 Encapsulation — `PoliceCar` `static Texture[] flashTextures` bypasses Flyweight

**File:** `core/.../game/entities/PoliceCar.java`
**Severity:** MEDIUM | **Status:** STILL PRESENT

```java
private static Texture[] flashTextures;

if (flashTextures == null) {
    flashTextures = new Texture[FLASH_FRAMES.length];
    for (int i = 0; i < FLASH_FRAMES.length; i++) {
        flashTextures[i] = new Texture(FLASH_FRAMES[i]); // bypasses TextureObject cache
    }
}
```

Two distinct issues:
1. **Bypasses Flyweight cache:** `new Texture(filename)` is called directly, violating the architectural rule that all textures must go through `TextureObject.textureCache`
2. **Memory leak on scene transition:** The static array is never registered with `textureCache`, so `TextureObject.disposeAllTextures()` does not clean it up — the GPU memory is leaked between scene loads

---

### 3.7 Encapsulation — `RoadHazard` static mutable `Texture` fields bypass Flyweight

**File:** `core/.../game/zones/RoadHazard.java`
**Severity:** MEDIUM | **Status:** NEW

```java
private static Texture puddleTexture;
private static Texture oilTexture;
// loaded lazily with direct new Texture(…) calls
```

Same pattern as `PoliceCar.flashTextures`:
1. Raw `new Texture(filename)` bypasses the `TextureObject` Flyweight cache entirely
2. The static fields are never tracked by `TextureObject.textureCache`, so they leak on `TextureObject.disposeAllTextures()`
3. Mutable static state creates implicit shared state across all `RoadHazard` instances — the Flyweight pattern already handles this correctly, making this duplication unnecessary and incorrect

---

### 3.8 Hierarchy — `Pickupable` extends `Entity` instead of `TextureObject`

**File:** `core/.../game/entities/Pickupable.java`
**Severity:** MEDIUM | **Status:** NEW

```java
public class Pickupable extends Entity {
    private static Texture sharedTex = new Texture("coinIconStar.png"); // raw Texture!
}
```

Two issues:
1. `Pickupable` extends bare `Entity` rather than `TextureObject`, bypassing the Flyweight texture management entirely
2. A `static Texture` field is directly initialized with `new Texture(…)` at class-load time — this breaks the Flyweight contract and leaks the texture when `TextureObject.disposeAllTextures()` is called (the field is unknown to the cache)

Every renderable game entity should extend `TextureObject` so that the cache owns and controls the full lifecycle of all GPU texture resources.

---

### 3.9 Engine purity — `CollisionManager.beginContact()` contains `System.out.println`

**File:** `core/.../engine/collision/CollisionManager.java`
**Severity:** HIGH | **Status:** NEW

```java
@Override
public void beginContact(Contact contact) {
    System.out.println("=== BOX2D CONTACT BEGIN ==="); // in engine layer!
    // …
}
```

Two violations in one:
1. **Guideline D-2:** `System.out.println` is banned throughout the project — the correct alternative is `Gdx.app.log(tag, message)`
2. **Engine purity (E-2):** Debug output in the engine layer couples it to game-layer debugging workflows. This print will fire for *every* physics contact in every scene unconditionally — the engine layer should be cleanly separable from game-specific needs

---

### 3.10 OCP — `AIPerceptionService` uses `instanceof` cascade for entity type classification

**File:** `core/.../game/movement/AIPerceptionService.java`
**Severity:** MEDIUM | **Status:** NEW

```java
if (entity instanceof Pedestrian p) { … }
else if (entity instanceof NPCCar npc) { … }
else if (entity instanceof Tree t) { … }
else if (entity instanceof StopSign s) { … }
```

Adding any new entity type that the AI can perceive requires **editing `AIPerceptionService`** — a direct OCP violation. The cascade also creates a strong coupling from the perception service to every specific game entity subtype.

**Architecture guideline violation:** *"NEVER use `if/else` chains or `instanceof` cascades to select varying behaviors. You must use Strategy, State, or Polymorphism."* Entities should expose their own perception data through a common interface (e.g. `IPerceivable.getPerceptionData()`), or a Visitor pattern should be used, so `AIPerceptionService` is closed for modification when new entity types are added.

---

### 3.11 DIP — `NpcDrivingStrategy` downcasts `MovableEntity` to concrete `NPCCar`

**File:** `core/.../game/movement/NpcDrivingStrategy.java`
**Severity:** LOW | **Status:** NEW

```java
if (!(entity instanceof NPCCar npc)) return new MovementResult(…);
// then uses npc.getRelativeY(), npc.getLane(), etc.
```

`NpcDrivingStrategy implements MovementStrategy` receives a `MovableEntity` through the strategy interface but immediately downcasts to the concrete `NPCCar`. This defeats the abstraction — `MovementStrategy` is supposed to be entity-agnostic. The hidden coupling means `NpcDrivingStrategy` can only function with `NPCCar` specifically, which is not expressed in the type signature and is not enforced by the compiler.

---

### 3.12 Engine purity — `GameMaster` hardcodes game-specific asset names

**File:** `core/.../engine/GameMaster.java`
**Severity:** LOW | **Status:** STILL PRESENT

```java
// GameMaster.create()
soundDevice.addSound("menu", "uiMenu_sound.wav");
soundDevice.addSound("selected", "uiSelected_sound.wav");
```

`GameMaster` is an engine class but it hardcodes game-specific asset filenames. The engine entry point should be context-free; asset names are a game-layer concern. This creates a direct dependency from engine code onto the game's asset naming convention.

---

## 4. Summary Table

| #   | File                                                                             | Principle Violated                                              | Severity   | Status           |
| --- | -------------------------------------------------------------------------------- | --------------------------------------------------------------- | ---------- | ---------------- |
| 1.1 | `BaseGameScene`                                                                  | SRP — 5+ mixed responsibilities                                 | HIGH       | STILL PRESENT    |
| 1.2 | `NPCCarSpawner`                                                                  | SRP — spawning + updating + querying + 5× debug prints          | MEDIUM     | WORSENED         |
| 1.3 | `RoadHazardSpawner`                                                              | SRP — spawning + updating + rendering                           | MEDIUM     | STILL PRESENT    |
| 1.4 | `DashboardUI`                                                                    | SRP — model + rendering + resource mgmt                         | MEDIUM     | STILL PRESENT    |
| 1.5 | `GameCollisionHandler`                                                           | SRP — inline NPC-ped block not delegated to sub-handler         | MEDIUM     | PARTIALLY FIXED  |
| 1.6 | `PoliceCar`                                                                      | SRP — entity + undelegated AI + animation                       | MEDIUM     | WORSENED         |
| 1.7 | `PoliceMovement`                                                                 | SRP/LSP — dead code Strategy (entire file commented out)        | MEDIUM     | NEW              |
| 1.8 | `Level1Scene`                                                                    | SRP — `updateGame()` god method (~100 lines)                    | MEDIUM     | NEW              |
| 1.9 | `NPCCar`                                                                         | SRP — entity + position update + expiry tracking                | LOW        | STILL PRESENT    |
| 2.1 | `GameCollisionHandler`                                                           | OCP — `instanceof` chains partially remain                      | HIGH       | PARTIALLY FIXED  |
| 2.2 | `BaseGameScene.checkLevelEnd()`                                                  | OCP — hardcoded end conditions                                  | MEDIUM     | STILL PRESENT    |
| 2.3 | `RuleManager` + `BreakRuleCommand`                                               | OCP — hardcoded violation methods + Command semantic misuse     | MEDIUM     | STILL PRESENT    |
| 2.4 | `PhysicsWorld.createBody()`                                                      | OCP — single-shape overload forces modification for new shapes  | LOW        | STILL PRESENT    |
| 3.1 | `BaseGameScene`, `Level1Scene`, `Level2Scene`, `NPCCarSpawner`, `PickupableSpawner`, `RoadHazard`, `MotionZone` | Engine boundary / Box2D leakage (3 → 7 files) | HIGH | WORSENED |
| 3.2 | `Level2Scene`                                                                    | DIP — depends on concrete `PoliceCar`                           | MEDIUM     | STILL PRESENT    |
| 3.3 | `CollisionManager`                                                               | DIP / Observer — single listener, no multi-subscriber support   | MEDIUM     | STILL PRESENT    |
| 3.4 | `TrafficViolationListener`                                                       | ISP — forces unused crosswalk/pedestrian methods onto `Level2Scene` | MEDIUM  | NEW              |
| 3.5 | `MovableEntity`                                                                  | ISP — dual movement abstraction inflates API for non-player entities | LOW    | STILL PRESENT    |
| 3.6 | `PoliceCar`                                                                      | Encapsulation — `static Texture[]` bypasses Flyweight + leaks   | MEDIUM     | STILL PRESENT    |
| 3.7 | `RoadHazard`                                                                     | Encapsulation — `static Texture` fields bypass Flyweight + leak | MEDIUM     | NEW              |
| 3.8 | `Pickupable`                                                                     | Hierarchy — extends `Entity` not `TextureObject`; raw `new Texture` | MEDIUM | NEW              |
| 3.9 | `CollisionManager`                                                               | Engine purity — `System.out.println` in engine `beginContact()` | HIGH       | NEW              |
| 3.10| `AIPerceptionService`                                                            | OCP — `instanceof` cascade for entity type perception           | MEDIUM     | NEW              |
| 3.11| `NpcDrivingStrategy`                                                             | DIP — casts `MovableEntity` to concrete `NPCCar`                | LOW        | NEW              |
| 3.12| `GameMaster`                                                                     | Engine purity — game-specific asset names in engine class       | LOW        | STILL PRESENT    |
| —   | `PlayerCar`                                                                      | Encapsulation — `System.out.println` in `triggerDamageFlash()`  | LOW        | **FIXED**        |
| —   | `PedestrianMovement`                                                             | SRP/OCP — movement Strategy was commented out / non-functional  | LOW        | **FIXED**        |

**Total active violations: 25**

- HIGH: 4 (1.1, 2.1, 3.1, 3.9)
- MEDIUM: 15
- LOW: 6

**Fixed since last report: 2**

---

## 5. Priority Order

Items ordered by architectural impact and grading risk. No implementation instructions are provided here — fix approach is to be determined separately.

1. **3.1 Box2D leakage (7 files)** — the most critical architectural boundary rule; the leakage is actively spreading to every new file that needs a physics body
2. **3.9 `CollisionManager` `System.out.println` in engine** — engine-layer debug output; violates both guideline D-2 and engine purity rule E-2; fires on every physics contact
3. **2.1 + 1.5 `GameCollisionHandler` `instanceof` chains** — explicitly banned anti-pattern per architecture guidelines; still present after partial refactor
4. **1.1 `BaseGameScene` god class** — highest grading risk; the largest single source of SRP violations; many downstream issues trace back here
5. **1.7 `PoliceMovement` dead code** — the Strategy scaffold exists but is unused; highest effort-to-reward ratio to activate
6. **1.6 `PoliceCar` undelegated chase AI** — completing 1.7 is a prerequisite; AI must move to the Strategy before `PoliceCar` can comply with SRP
7. **3.4 `TrafficViolationListener` ISP** — clean ISP example demonstrable in a targeted interface split; good signal for the ISP grading criterion
8. **3.2 `Level2Scene` → `PoliceCar` DIP** — an `IChaseEntity` interface is needed; partially dependent on fixing 1.6
9. **2.3 `BreakRuleCommand` semantic misuse** — logic error inside the Command pattern; breaks the pattern's invariant
10. **1.2 `NPCCarSpawner` debug prints** — 5× `System.out.println` cleanup; low-effort, high-cleanliness improvement
11. **3.7 + 3.8 `RoadHazard` / `Pickupable` Flyweight bypass** — ensures consistent texture lifecycle; important for memory correctness
12. Remaining MEDIUM/LOW items can be addressed incrementally without risk to gameplay
