# OOP Violations Report

## INF1009 — Object-Oriented Programming Project

**Date updated:** 2026-03-24 (Session 3 — System-Wide SRP Overhaul)
**Previous analysis:** 2026-03-23 (6 active violations, 21 fixed)
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
**Severity:** HIGH | **Status:** FIXED

**Resolution:** Speed/scroll logic extracted to `SpeedScrollController`, fuel lifecycle to `FuelController`, BGM/sound management to `AudioController` — all composed via `IGameSystem`. Cross-system communication uses `EventBus`. Base class reduced from ~760 to ~540 LOC with only 3 remaining responsibilities: scene lifecycle orchestration, physics/player construction (template infrastructure), and win/lose detection.

---

### 1.2 `NPCCarSpawner` — spawning + update + lane-query + excessive debug output

**File:** `core/.../game/factory/NPCCarSpawner.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** Lane-occupancy query extracted to `ILaneOccupancy` interface — both `NPCCarSpawner` and `RoadHazardSpawner` implement it, decoupling the spawners from each other. `System.out.println` debug calls removed. The `hazardSpawner` field now depends on the `ILaneOccupancy` abstraction (`hazardOccupancy`) instead of the concrete `RoadHazardSpawner`.

---

### 1.3 `RoadHazardSpawner` — spawning + rendering + per-entity update

**File:** `core/.../game/factory/RoadHazardSpawner.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `render(SpriteBatch)` method removed from the spawner. A `getActiveHazards()` accessor (returning `Collections.unmodifiableList`) was added so that the scene can iterate and call each hazard's own `drawHazard(batch)` — rendering responsibility now belongs to the entity. The spawner also implements `ILaneOccupancy` and depends on the `ILaneOccupancy` abstraction instead of concrete `NPCCarSpawner`.

---

### 1.4 `DashboardUI` — HUD model state + pop-up lifecycle + rendering + resource management

**File:** `core/.../game/state/DashboardUI.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** Score pop-up lifecycle (spawn, animation, rendering, disposal) extracted to `ScorePopupManager` in `game/state/`. `DashboardUI` now delegates `showScorePopup()` → `popupManager.show()`, `act()` → `popupManager.update()`, `draw()` → `popupManager.render()`, and `dispose()` → `popupManager.dispose()`. The `ScorePopup` inner class, `popups` list, and `popupFont` field were removed from `DashboardUI`.

---

### 1.5 `GameCollisionHandler` — inline NPC-hits-pedestrian block not delegated to sub-handler

**File:** `core/.../game/collision/GameCollisionHandler.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** A dedicated `NPCPedestrianCollisionHandler` sub-handler was created and wired into the facade. The inline `instanceof` NPC-pedestrian block was removed from `GameCollisionHandler.onCollisionBegin()`. All collision types are now fully delegated to sub-handlers.

---

### 1.6 `PoliceCar` — entity state + undelegated chase AI + siren animation

**File:** `core/.../game/entities/vehicles/PoliceCar.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** Chase AI math (speed approach, X lerp, Y advance) fully delegated to `PoliceMovement` strategy class. `PoliceCar` now calls `movement.advance()` and `movement.lerpX()`. Siren flash textures use `TextureObject.getOrLoadTexture()` (Flyweight-compliant). The entity implements `IChaseEntity` for DIP compliance. The old duplicate `game/entities/PoliceCar.java` (with inline chase AI) was deleted.

---

### 1.7 `PoliceMovement` — entire implementation commented out (dead code Strategy class)

**File:** `core/.../game/movement/PoliceMovement.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `PoliceMovement` is now a fully implemented strategy class with `advance(dt, playerSpeed, maxSpeed, aggression)` and `lerpX(currentX, targetX, dt)` methods. The class contains the chase AI constants (`BASE_APPROACH_SPEED`, `AGGRESSION_BONUS`, `LANE_TRACK_SPEED`) and `PoliceCar` delegates all movement math to it. No dead code remains.

---

### 1.8 `Level1Scene.updateGame()` — 100-line god method

**File:** `core/.../game/scene/Level1Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `updateGame()` now delegates to `CrosswalkEncounterSystem` and `TrafficSpawningSystem` (both `IGameSystem` implementations). The method body is ~20 lines — purely orchestrating system updates. Crosswalk zone management, pedestrian encounter lifecycle, and spawner coordination are fully encapsulated in their respective system classes.

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
**Severity:** HIGH | **Status:** FIXED

**Resolution:** All collision types are now delegated to dedicated sub-handler classes (8 total). The remaining `instanceof MovableEntity` checks in the player-identification helper are structural (identifying which entity is the player) — not behavioural branching. No collision-type-specific `instanceof` cascades remain in the routing logic.

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
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `RuleManager` now exposes a single generic `recordViolation()` method (plus an overload `recordViolation(String type)` for logging). The hardcoded per-violation methods (`recordRedLightViolation`, `recordCrosswalkViolation`, etc.) were replaced. `BreakRuleCommand` correctly calls the generic `recordViolation()` for all violation types — the semantic misuse is resolved.

---

### 2.4 `PhysicsWorld.createBody()` — single shape overload forces modification for new shapes

**File:** `core/.../engine/physics/PhysicsWorld.java`
**Severity:** LOW | **Status:** STILL PRESENT

Only one `createBody()` signature exists (axis-aligned box). Adding a circular or polygon body requires **modifying `PhysicsWorld`** — a violation of OCP for the physics facade. This also means the facade must grow a new method for every new shape, rather than accepting a polymorphic shape descriptor.

---

## 3. General OOP Violations (DIP, ISP, Encapsulation, Engine/Game Boundary)

---

### 3.1 Engine boundary — `BodyDef.BodyType` leaking into game layer (spreading)

**Files:** (previously `BaseGameScene.java`, `Level1Scene.java`, `Level2Scene.java`, `NPCCarSpawner.java`, `PickupableSpawner.java`, `RoadHazard.java`, `MotionZone.java`)
**Severity:** HIGH | **Status:** FIXED

**Resolution:** An engine-layer `BodyType` enum (`engine.physics.BodyType` — `STATIC`, `DYNAMIC`, `KINEMATIC`) was created. All game-layer files now use this enum instead of importing `com.badlogic.gdx.physics.box2d.BodyDef.BodyType`. Box2D imports are confined exclusively to the engine package (`PhysicsWorld`, `PhysicsBody`, `CollisionManager`, `GameMaster.Box2D.init()`). Zero game-layer files import any Box2D types.

---

### 3.2 DIP — `Level2Scene` directly depends on concrete `PoliceCar`

**File:** `core/.../game/scene/Level2Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `Level2Scene` now depends on the `IChaseEntity` interface rather than the concrete `PoliceCar`. The field is declared as `private IChaseEntity policeCar`, and all chase-related calls (`updateChase()`, `getScreenY()`, `hasCaughtPlayer()`) go through the abstraction. Swapping in a helicopter or drone chaser requires only a new `IChaseEntity` implementation — the scene needs no modification.

---

### 3.3 DIP / Observer — `CollisionManager` only accepts a single `ICollisionListener`

**File:** `core/.../engine/collision/CollisionManager.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `CollisionManager` now maintains a `List<ICollisionListener>` (backed by `CopyOnWriteArrayList`) and provides `addListener(ICollisionListener)`. The legacy single-listener constructor delegates to `addListener()`. Multiple observers can now subscribe to collision events without modifying the engine class.

---

### 3.4 ISP — `TrafficViolationListener` forces unrelated methods onto implementers

**File:** `core/.../game/collision/listeners/TrafficViolationListener.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** The `onPickup()` method was extracted to a separate `PickupListener` functional interface (ISP split). `TrafficViolationListener` now contains only traffic violation callbacks (`onTrafficCrash()`, `onBoundaryHit()`, `onCrosswalkViolation()`, `onPedestrianHit()`) — all with default no-op implementations so `Level2Scene` only overrides `onTrafficCrash()`. `GameCollisionHandler` exposes `setPickupListener(PickupListener)` alongside `setTrafficViolationListener()`. Both Level1Scene and Level2Scene wire the pickup listener separately via lambda.

---

### 3.5 ISP — `MovableEntity` dual movement abstraction inflates the API

**File:** `core/.../engine/movement/MovableEntity.java`
**Severity:** LOW | **Status:** STILL PRESENT

`MovableEntity` exposes both `MovementModel` and `MovementStrategy` on every movable entity. Only `PlayerCar` actually uses `MovementStrategy`; all other movable entities do not. Every `MovableEntity` subclass therefore inherits `setMovementStrategy()`/`getMovementStrategy()` methods that are irrelevant to them — the API surface is inflated beyond necessity.

**Why ISP is violated:** Subclasses that never use `MovementStrategy` are still dependent on it through their superclass. The two parallel movement abstractions (`MovementModel.step()` for AI and `MovementStrategy.getX/getY()` for player input) also have no clear documented rule distinguishing when to use each, which adds cognitive burden to every new entity implementation.

---

### 3.6 Encapsulation — `PoliceCar` `static Texture[] flashTextures` bypasses Flyweight

**File:** `core/.../game/entities/vehicles/PoliceCar.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `PoliceCar` now uses `TextureObject.getOrLoadTexture(FLASH_FRAMES[flashIndex])` for siren frames instead of a hand-managed `static Texture[]` array. All textures are tracked by the Flyweight cache and properly disposed on `TextureObject.disposeAllTextures()`. The old duplicate `game/entities/PoliceCar.java` was deleted.

---

### 3.7 Encapsulation — `RoadHazard` static mutable `Texture` fields bypass Flyweight

**File:** `core/.../game/zone/RoadHazard.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `RoadHazard` now uses `TextureObject.getOrLoadTexture(texturePath)` in its constructor. No static mutable texture fields remain — all hazard textures are tracked by the Flyweight cache and properly disposed.

---

### 3.8 Hierarchy — `Pickupable` extends `Entity` instead of `TextureObject`

**File:** `core/.../game/entities/misc/Pickupable.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** `Pickupable` now extends `TextureObject` (participating in the Flyweight texture cache) and implements `IExpirable` for automatic removal. The old `game/entities/Pickupable.java` (which extended bare `Entity` with a raw `static Texture`) was deleted.

---

### 3.9 Engine purity — `CollisionManager.beginContact()` contains `System.out.println`

**File:** `core/.../engine/collision/CollisionManager.java`
**Severity:** HIGH | **Status:** FIXED

**Resolution:** All `System.out.println` calls removed from `CollisionManager`. The engine layer contains zero debug print statements. All NPCCarSpawner debug prints were also removed project-wide.

---

### 3.10 OCP — `AIPerceptionService` uses `instanceof` cascade for entity type classification

**File:** `core/.../game/movement/AIPerceptionService.java`
**Severity:** MEDIUM | **Status:** FIXED

**Resolution:** Entities now implement `IPerceivable` and expose their perception category via `getPerceptionCategory()` returning a `PerceptionCategory` enum (`VEHICLE`, `PEDESTRIAN`, `OBSTACLE`, `SIGN`, `DECORATION`). `AIPerceptionService` uses a single `instanceof IPerceivable` check followed by a `switch` on the enum — no entity-specific `instanceof` cascades remain. Adding new perceivable entity types only requires implementing `IPerceivable` on the new class.

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

**Note:** Cannot use `GameConstants` here because `GameMaster` is in the engine package — importing a game-layer class would violate the engine/game boundary (which is a worse violation). The proper fix requires a configuration injection layer from the composition root, which is out of scope for this project.

---

## Session 3 — System-Wide SRP Overhaul (2026-03-24)

This session performed a comprehensive SRP audit and extraction pass across all scene classes.
Seven new violations were identified and immediately resolved.

---

### S3.1 `Level1Scene` — anonymous `TrafficViolationListener` inner class with game logic

**File:** `core/.../game/scene/Level1Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** `Level1Scene.initLevelData()` created an anonymous `TrafficViolationListener` containing score manipulation (`addScore()`), command execution (`commandHistory.executeAndRecord()`), and encounter system coordination (`crosswalkSystem.completeEncounter()`). This violated SRP by embedding violation-reaction logic directly inside the scene class.

**Resolution:** Extracted to standalone `Level1TrafficListener` class (`game/collision/listeners/`). Dependencies (RuleManager, CommandHistory, SoundDevice, CrosswalkEncounterSystem) are constructor-injected. Functional interfaces `ScoreCallback` and `CrashCallback` decouple the listener from `BaseGameScene` — no scene reference needed.

---

### S3.2 `Level2Scene` — anonymous `TrafficViolationListener` inner class with game logic

**File:** `core/.../game/scene/Level2Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** Same pattern as S3.1 — `Level2Scene.initLevelData()` contained an anonymous `TrafficViolationListener` with inline `commandHistory.executeAndRecord(new BreakRuleCommand(...))`, `incrementCrashCount()`, and `addScore(-100)`.

**Resolution:** Extracted to standalone `Level2TrafficListener` class (`game/collision/listeners/`). Only overrides `onTrafficCrash()` (no crosswalks/pedestrians in Level 2). Uses same `ScoreCallback`/`CrashCallback` functional interfaces for DIP compliance.

---

### S3.3 `Level1Scene` — inline crosswalk rendering in `renderLevelEffects()`

**File:** `core/.../game/scene/Level1Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** `renderLevelEffects()` contained OpenGL blend state management (`Gdx.gl.glEnable(GL20.GL_BLEND)`) and a loop iterating `crosswalkSystem.getCrosswalkZones()` to render translucent overlays. The scene was acting as both orchestrator and renderer.

**Resolution:** Extracted to `CrosswalkRenderer` class (`game/scene/`). Single method `render(ShapeRenderer, List<CrosswalkZone>)` encapsulates blend enable/disable and zone drawing. Scene calls `crosswalkRenderer.render(sr, zones)` — one line.

---

### S3.4 `Level2Scene` — 100+ lines of inline rain/weather rendering in `renderLevelEffects()`

**File:** `core/.../game/scene/Level2Scene.java`
**Severity:** HIGH | **Status:** FIXED

**Before:** `renderLevelEffects()` contained ~120 lines of rain rendering: 150-drop particle arrays (`dropX[]`, `dropY[]`, `dropLen[]`, `dropSpd[]`), atmosphere overlay, 5-pass "wet lens blur" effect, animated vignette with side/top/bottom strips, and rain streak drawing with secondary glow passes. This was the single largest SRP violation in the codebase — the scene was a full rendering engine.

**Resolution:** Extracted to `RainEffectSystem` class (`game/scene/`). Contains `initDropsIfNeeded()`, `advanceDrops()`, `drawAtmosphere()`, `drawWetLensBlur()`, `drawVignette()`, and `drawRainStreaks()`. Single entry point: `render(ShapeRenderer, SpriteBatch)`. Uses `GameConstants.RAIN_DROP_COUNT`. Scene call reduced to `rainEffect.render(sr, batch)`.

---

### S3.5 `Level2Scene` — inline `spawnPolice()` with raw physics body creation

**File:** `core/.../game/scene/Level2Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** `spawnPolice()` directly called `getWorld().createBody()` with hardcoded PPM conversions (`80f / Constants.PPM / 2f`, `140f / Constants.PPM / 2f`), position calculations (`ROAD_CENTRE_X / Constants.PPM`), and `new PoliceCar(policeBody)`. The scene was acting as a factory.

**Resolution:** Extracted to `PoliceCarFactory` class (`game/factory/`). Uses `GameConstants` for police dimensions. Returns `IChaseEntity` abstraction. Scene calls `policeFactory.spawn()`. No raw physics calculations in the scene.

---

### S3.6 `Level1Scene` + `Level2Scene` — DIP violation: locally creating `RuleManager` and `CommandHistory`

**Files:** `core/.../game/scene/Level1Scene.java`, `core/.../game/scene/Level2Scene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** Both scene classes contained `ruleManager = new RuleManager()` and `commandHistory = new CommandHistory()` in `initLevelData()`. Each scene was responsible for creating and managing its own rule tracking infrastructure — violating DIP (depending on concretions) and duplicating creation logic.

**Resolution:** Creation moved to `BaseGameScene.show()`, which instantiates both objects before calling the `initLevelData()` hook. Protected accessors `getRuleManager()` and `getCommandHistory()` expose them to subclasses. Scenes are no longer responsible for lifecycle management of these objects. `dispose()` in the base class handles `commandHistory.clear()`.

---

### S3.7 All scenes — magic numbers and hardcoded string literals scattered throughout

**Files:** `Level1Scene.java`, `Level2Scene.java`, `BaseGameScene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** Level parameters (`LEVEL_LENGTH = 60000f`, `MAX_SPEED = 80f`), score values (`-100`, `200`, `50`), sound file paths (`"bgm.mp3"`, `"hit_sound.wav"`), entity dimensions (`80f`, `140f`), spawn intervals, wanted star thresholds — all hardcoded as local `static final` fields or anonymous literals. Changes required modifying multiple files.

**Resolution:** Created `GameConstants` class (`game/`) centralizing all game-layer constants with descriptive names: `SCORE_PICKUP`, `SCORE_PENALTY`, `MAX_WANTED_STARS`, `L1_LEVEL_LENGTH`, `L2_MAX_SPEED`, `SFX_HIT_SOUND`, `BGM_DEFAULT`, `PLAYER_CAR_WIDTH`, `POLICE_WIDTH`, `RAIN_DROP_COUNT`, etc. All scenes reference `GameConstants.*` — no magic numbers remain in scene code.

---

## 5. Summary Table

| #    | File                               | Principle Violated                                                   | Severity | Status        |
| ---- | ---------------------------------- | -------------------------------------------------------------------- | -------- | ------------- |
| 1.1  | `BaseGameScene`                    | SRP — 5+ mixed responsibilities                                      | HIGH     | **FIXED**     |
| 1.2  | `NPCCarSpawner`                    | SRP — spawning + updating + querying + debug prints                  | MEDIUM   | **FIXED**     |
| 1.3  | `RoadHazardSpawner`                | SRP — spawning + updating + rendering                                | MEDIUM   | **FIXED**     |
| 1.4  | `DashboardUI`                      | SRP — model + pop-up lifecycle                                       | MEDIUM   | **FIXED**     |
| 1.5  | `GameCollisionHandler`             | SRP — inline NPC-ped block not delegated to sub-handler              | MEDIUM   | **FIXED**     |
| 1.6  | `PoliceCar`                        | SRP — entity + undelegated AI + animation                            | MEDIUM   | **FIXED**     |
| 1.7  | `PoliceMovement`                   | SRP/LSP — dead code Strategy (entire file commented out)             | MEDIUM   | **FIXED**     |
| 1.8  | `Level1Scene`                      | SRP — `updateGame()` god method (~100 lines)                         | MEDIUM   | **FIXED**     |
| 1.9  | `NPCCar`                           | SRP — entity + position update + expiry tracking                     | LOW      | STILL PRESENT |
| 2.1  | `GameCollisionHandler`             | OCP — `instanceof` chains partially remain                           | HIGH     | **FIXED**     |
| 2.2  | `BaseGameScene.checkLevelEnd()`    | OCP — hardcoded end conditions                                       | MEDIUM   | STILL PRESENT |
| 2.3  | `RuleManager` + `BreakRuleCommand` | OCP — hardcoded violation methods + Command semantic misuse          | MEDIUM   | **FIXED**     |
| 2.4  | `PhysicsWorld.createBody()`        | OCP — single-shape overload forces modification for new shapes       | LOW      | STILL PRESENT |
| 3.1  | (previously 7 game-layer files)    | Engine boundary / Box2D leakage                                      | HIGH     | **FIXED**     |
| 3.2  | `Level2Scene`                      | DIP — depends on concrete `PoliceCar`                                | MEDIUM   | **FIXED**     |
| 3.3  | `CollisionManager`                 | DIP / Observer — single listener support                             | MEDIUM   | **FIXED**     |
| 3.4  | `TrafficViolationListener`         | ISP — forces unused methods onto implementers                        | MEDIUM   | **FIXED**     |
| 3.5  | `MovableEntity`                    | ISP — dual movement abstraction inflates API for non-player entities | LOW      | STILL PRESENT |
| 3.6  | `PoliceCar`                        | Encapsulation — `static Texture[]` bypasses Flyweight + leaks        | MEDIUM   | **FIXED**     |
| 3.7  | `RoadHazard`                       | Encapsulation — `static Texture` fields bypass Flyweight + leak      | MEDIUM   | **FIXED**     |
| 3.8  | `Pickupable`                       | Hierarchy — extends `Entity` not `TextureObject`; raw `new Texture`  | MEDIUM   | **FIXED**     |
| 3.9  | `CollisionManager`                 | Engine purity — `System.out.println` in engine `beginContact()`      | HIGH     | **FIXED**     |
| 3.10 | `AIPerceptionService`              | OCP — `instanceof` cascade for entity type perception                | MEDIUM   | **FIXED**     |
| 3.11 | `NpcDrivingStrategy`               | DIP — casts `MovableEntity` to concrete `NPCCar`                     | LOW      | STILL PRESENT |
| 3.12 | `GameMaster`                       | Engine purity — game-specific asset names in engine class            | LOW      | STILL PRESENT |
| S3.1 | `Level1Scene`                      | SRP — anonymous `TrafficViolationListener` with game logic           | MEDIUM   | **FIXED**     |
| S3.2 | `Level2Scene`                      | SRP — anonymous `TrafficViolationListener` with game logic           | MEDIUM   | **FIXED**     |
| S3.3 | `Level1Scene`                      | SRP — inline crosswalk rendering in `renderLevelEffects()`           | MEDIUM   | **FIXED**     |
| S3.4 | `Level2Scene`                      | SRP — 100+ lines inline rain/weather rendering                       | HIGH     | **FIXED**     |
| S3.5 | `Level2Scene`                      | SRP — inline `spawnPolice()` factory with raw physics calls          | MEDIUM   | **FIXED**     |
| S3.6 | `Level1Scene` + `Level2Scene`      | DIP — locally creating `RuleManager` / `CommandHistory`              | MEDIUM   | **FIXED**     |
| S3.7 | All scenes                         | Code smell — magic numbers and hardcoded strings                     | MEDIUM   | **FIXED**     |
| —    | `PlayerCar`                        | Encapsulation — `System.out.println` in `triggerDamageFlash()`       | LOW      | **FIXED**     |
| —    | `PedestrianMovement`               | SRP/OCP — movement Strategy was commented out / non-functional       | LOW      | **FIXED**     |

**Total violations catalogued: 33**
**Total active violations: 6** (unchanged — all new violations discovered this session were immediately resolved)

- HIGH: 0 (all 5 resolved, including the new S3.4)
- MEDIUM: 1 (2.2)
- LOW: 5 (1.9, 2.4, 3.5, 3.11, 3.12)

**Fixed this session (Session 3): 7** (S3.1–S3.7)
**Fixed cumulative: 28 of 33**

---

## 6. Remaining Items (Low Priority / Architectural Choices)

These remaining items are either low-severity, architectural trade-offs acceptable for the project scope, or require disproportionate effort relative to their risk:

1. **1.9 `NPCCar` SRP** (LOW) — Entity lifecycle management is tightly coupled to its state by nature; extracting would add indirection with minimal benefit.
2. **2.2 `BaseGameScene.checkLevelEnd()` hardcoded conditions** (MEDIUM) — The Template Method already allows subclass override via `isGameOver()`. The 3 base conditions (progress, crash count, instant fail) are shared invariants.
3. **2.4 `PhysicsWorld.createBody()` single shape** (LOW) — Only box shapes are used in the entire project. Adding a polymorphic shape descriptor is over-engineering for the current scope.
4. **3.5 `MovableEntity` dual movement** (LOW) — Both `MovementModel` and `MovementStrategy` serve distinct roles (AI step vs. player input); the dual API is an intentional architectural choice.
5. **3.11 `NpcDrivingStrategy` downcast** (LOW) — The strategy is inherently NPC-specific; the downcast is a pragmatic coupling that correctly uses pattern matching.
6. **3.12 `GameMaster` asset names** (LOW) — Cannot use `GameConstants` (engine→game dependency would be worse). Requires a configuration injection layer from the composition root, which is out of scope.

---

## 7. Session 3 — Extraction Architecture Map

New classes created and their integration points:

```
BaseGameScene (MODIFIED)
├── RuleManager      ← created in show(), exposed via getRuleManager()
├── CommandHistory   ← created in show(), exposed via getCommandHistory()
└── GameConstants.*  ← fuel rates, player dimensions, crash threshold, SFX paths

Level1Scene (REWRITTEN)
├── TrafficSpawningSystem     (existing — unchanged)
├── CrosswalkEncounterSystem  (existing — unchanged)
├── CrosswalkRenderer         ← NEW — renders crosswalk zone overlays
├── Level1TrafficListener     ← NEW — handles crosswalk/crash/pedestrian violations
└── GameConstants.L1_*        ← level length, max speed, spawn intervals

Level2Scene (REWRITTEN)
├── TrafficSpawningSystem     (existing — unchanged)
├── RainEffectSystem          ← NEW — rain particles, atmosphere, vignette
├── PoliceCarFactory          ← NEW — creates IChaseEntity via PhysicsWorld
├── Level2TrafficListener     ← NEW — handles traffic crash violations
├── RoadHazardSpawner[]       (existing — unchanged)
└── GameConstants.L2_*        ← level length, max speed, spawn intervals

GameConstants (NEW)
└── Centralizes ALL magic numbers: scores, dimensions, intervals, audio paths, thresholds
```

**Files created this session:** 6

- `game/GameConstants.java`
- `game/scene/RainEffectSystem.java`
- `game/scene/CrosswalkRenderer.java`
- `game/collision/listeners/Level1TrafficListener.java`
- `game/collision/listeners/Level2TrafficListener.java`
- `game/factory/PoliceCarFactory.java`

**Files modified this session:** 3

- `game/scene/BaseGameScene.java` — DIP injection, GameConstants usage
- `game/scene/Level1Scene.java` — Full rewrite using extracted classes
- `game/scene/Level2Scene.java` — Full rewrite using extracted classes

---

## Session 5 — System-Wide SRP Overhaul & Feature Upgrade

This session performed a comprehensive 110-file audit and resolved all remaining God Class
violations in `BaseGameScene` and `CrosswalkEncounterSystem`, extracted 5 new classes,
consolidated 30+ magic numbers, and implemented 4 new Level 2 features.

---

### S5.1 `BaseGameScene` — boundary wall body creation in `show()`

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** HIGH | **Status:** FIXED

**Before:** `show()` called `world.createBody(BodyType.STATIC, ...)` four times with inline PPM math.

**Resolution:** Extracted to `BoundaryFactory.createBoundaries(world, worldW, worldH)` static factory method (`game/factory/`). Wall thickness uses `GameConstants.BOUNDARY_WALL_THICKNESS`.

---

### S5.2 `BaseGameScene` — player car body creation in `show()`

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** HIGH | **Status:** FIXED

**Before:** `show()` called `world.createBody(BodyType.DYNAMIC, ...)` for the player car, then constructed `PlayerCar` with inline PPM math, `PlayerMovementStrategy`, `CarMovementModel`, and `VehicleProfile.playerArcade()`.

**Resolution:** Extracted to `PlayerFactory.create(world, entityManager, controls)` static factory method (`game/factory/`). All constants sourced from `GameConstants`.

---

### S5.3 `BaseGameScene` — `triggerExplosionGameOver()` spawns entities directly

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** `triggerExplosionGameOver()` called `ExplosionParticle.spawnExplosion()` and `new ExplosionOverlay()` then `sound.playSound("explosion_big", 0.5f)`.

**Resolution:** Extracted to `ExplosionSystem.trigger(entityManager, sound, centreX, centreY)` static utility (`game/scene/`). BaseGameScene delegates to it.

---

### S5.4 `BaseGameScene` — `checkCrashExplosion` hardcoded in `show()`

**File:** `core/.../game/scene/BaseGameScene.java`
**Severity:** HIGH (OCP) | **Status:** FIXED

**Before:** `show()` called `addEndCondition(this::checkCrashExplosion)` unconditionally, meaning all levels exploded after 3 crashes — including Level 2 which should not.

**Resolution:** `checkCrashExplosion` is no longer registered in `show()`. It is now `protected` and subclasses opt in explicitly. `Level1Scene.initLevelData()` calls `addEndCondition(this::checkCrashExplosion)`. `Level2Scene` does not — crashes add a star and halve speed instead.

---

### S5.5 `CrosswalkEncounterSystem` — body creation methods

**File:** `core/.../game/scene/CrosswalkEncounterSystem.java`
**Severity:** HIGH | **Status:** FIXED

**Before:** `createCrosswalkZone()` and `createEncounter()` called `world.createBody()` with inline PPM math and hardcoded pedestrian dimensions (`80f`).

**Resolution:** Both methods removed. Body creation delegated to `CrosswalkFactory.createZone()` and `CrosswalkFactory.createEncounter()` (`game/factory/`). Constants: `GameConstants.CROSSWALK_ZONE_HEIGHT`, `CROSSING_SPEED`, `PEDESTRIAN_WIDTH/HEIGHT`, `PEDESTRIAN_HITBOX_SCALE`.

---

### S5.6 `PoliceMovement` — magic numbers

**File:** `core/.../game/movement/PoliceMovement.java`
**Severity:** MEDIUM | **Status:** FIXED

**Before:** `BASE_APPROACH_SPEED=70f`, `AGGRESSION_BONUS=130f`, `LANE_TRACK_SPEED=4f` as local constants.

**Resolution:** Replaced with `GameConstants.POLICE_BASE_APPROACH_SPEED`, `POLICE_AGGRESSION_BONUS`, `POLICE_LANE_TRACK_SPEED`.

---

### S5.7 `PoliceCar` — magic numbers

**File:** `core/.../game/entities/vehicles/PoliceCar.java`
**Severity:** LOW | **Status:** FIXED

**Before:** `FLASH_INTERVAL=0.15f` and hardcoded `screenY=-50f`.

**Resolution:** Replaced with `GameConstants.POLICE_FLASH_INTERVAL` and `GameConstants.POLICE_START_Y`.

---

### S5.8 `SpeedScrollController` — magic number

**File:** `core/.../game/state/SpeedScrollController.java`
**Severity:** LOW | **Status:** FIXED

**Before:** `PASSIVE_DECEL=18f` as local constant.

**Resolution:** Replaced with `GameConstants.PASSIVE_DECEL`. Added `applySpeedPenalty(float factor)` method for Level 2 crash handling.

---

### S5.9 `CrosswalkEncounterSystem` — hardcoded star counts

**File:** `core/.../game/scene/CrosswalkEncounterSystem.java`
**Severity:** LOW | **Status:** FIXED

**Before:** `BreakRuleCommand(ruleManager, "CROSSWALK", 2)` and `"CROSSWALK_STOP", 2)` with magic `2`. Also `ScoreChangedEvent(-100)`.

**Resolution:** Replaced with `GameConstants.CROSSWALK_VIOLATION_STARS` and `GameConstants.SCORE_PENALTY`.

---

### New Level 2 Features

**S5.10 — Crash → star + speed penalty (instead of explosion)**

Level2Scene does NOT register `checkCrashExplosion`. `Level2TrafficListener` now accepts a `SpeedPenaltyCallback` — on crash, calls `speedScroll.applySpeedPenalty(GameConstants.L2_CRASH_SPEED_PENALTY)` to halve the player's speed.

**S5.11 — PoliceLightSystem (red/blue glow)**

New `IGameSystem` renders oscillating red/blue glow at the bottom screen edge. Intensity scales with `1 - normalisedPoliceDistance`. Flash frequency ranges from 3Hz (far) to 10Hz (caught). Uses `ShapeRenderer` with alpha blending.

**S5.12 — Dynamic vertical player Y movement**

`Level2Scene.updateGame()` lerps `playerCar.Y` between `GameConstants.PLAYER_MIN_Y` (80px) and `GameConstants.PLAYER_MAX_Y` (280px) based on `speedRatio = simulatedSpeed / maxSpeed` with 3×dt smoothing. At full speed the car shifts upward; when braking it slides back down.

---

### Session 5 Summary

**Files created this session:** 5

- `game/factory/BoundaryFactory.java` — SRP extraction from BaseGameScene
- `game/factory/PlayerFactory.java` — SRP extraction from BaseGameScene
- `game/factory/CrosswalkFactory.java` — SRP extraction from CrosswalkEncounterSystem
- `game/scene/ExplosionSystem.java` — SRP extraction from BaseGameScene
- `game/scene/PoliceLightSystem.java` — New Level 2 visual system

**Files modified this session:** 10

- `game/GameConstants.java` — +80 lines of consolidated constants
- `game/scene/BaseGameScene.java` — Factory delegation, OCP fix, explosion extraction
- `game/scene/CrosswalkEncounterSystem.java` — Factory delegation, magic numbers
- `game/scene/Level1Scene.java` — Explicit crash explosion registration
- `game/scene/Level2Scene.java` — PoliceLightSystem, dynamic Y, speed penalty wiring
- `game/collision/listeners/Level2TrafficListener.java` — SpeedPenaltyCallback
- `game/state/SpeedScrollController.java` — applySpeedPenalty(), GameConstants
- `game/movement/PoliceMovement.java` — GameConstants migration
- `game/entities/vehicles/PoliceCar.java` — GameConstants migration
- `OOP_VIOLATIONS_REPORT.md` — This report

**Build verification:** `.\gradlew.bat :core:compileJava --rerun-tasks` → EXIT CODE 0, 0 IDE errors
