# Comprehensive OOP/SOLID Audit Report

> **Project:** LibGDX + Box2D Driving Game  
> **Total Java files audited:** 114 (34 engine, 78 game, 2 lwjgl3)  
> **Architecture:** Two-layer — `engine/` (framework) → `game/` (application)

---

## Table of Contents

1. [Architecture Overview](#1-architecture-overview)
2. [Engine Layer Inventory](#2-engine-layer-inventory)
3. [Game Layer Inventory](#3-game-layer-inventory)
4. [LWJGL3 Layer Inventory](#4-lwjgl3-layer-inventory)
5. [SOLID Violations Summary](#5-solid-violations-summary)
6. [Specific Anti-Pattern Checks](#6-specific-anti-pattern-checks)
7. [Refactoring Priority Matrix](#7-refactoring-priority-matrix)

---

## 1. Architecture Overview

### Design Patterns Used

| Pattern             | Where                                                                    | Implementation                                                                       |
| ------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------ |
| **Template Method** | `BaseGameScene`                                                          | Abstract hooks: `getLevelLength`, `getMaxSpeed`, `initLevelData`, `updateGame`, etc. |
| **Strategy**        | `MovementStrategy`, `MovementModel`, `SurfaceEffect`                     | Pluggable movement/physics behaviors                                                 |
| **Observer**        | `EventBus`, `IDashboardObserver`, `TrafficViolationListener`             | Decoupled score/fuel/rule events                                                     |
| **Command**         | `ICommand`, `CommandHistory`, `BreakRuleCommand`, `PedestrianHitCommand` | Reversible rule violations                                                           |
| **Flyweight**       | `TextureObject`                                                          | Static texture cache via `getOrLoadTexture()`                                        |
| **Factory Method**  | `PoliceCarFactory`, `NPCCarSpawner`, `PickupableSpawner`                 | Entity creation delegation                                                           |
| **Facade**          | `PhysicsWorld`, `PhysicsBody`, `BodyType`                                | Wraps Box2D API                                                                      |
| **Composition**     | `TrafficSpawningSystem`, `CrosswalkEncounterSystem`                      | `IGameSystem` composition in scenes                                                  |

### Layer Boundary Rule

**Engine** (`engine/`) must NEVER import from `game/`. Game imports from engine freely.

**Boundary Verdict:** ✅ CLEAN — no engine→game imports found.

---

## 2. Engine Layer Inventory

### `engine/Constants.java`

- **Purpose:** Pixels-per-meter conversion, input action name strings
- **Key fields:** `PPM=100f`, `LEFT/RIGHT/UP/DOWN/ACTION` string constants
- **SOLID:** ✅ Clean

### `engine/GameMaster.java`

- **Purpose:** `ApplicationAdapter` entry point. Constructor-injected Scene, InputDevice, SoundDevice
- **Key methods:** `create()`, `render()`, `resize()`, `dispose()`
- **SOLID Violations:**
  - ⚠️ **OCP/DIP:** Hardcodes `"menu"` and `"selected"` sound registration in `create()` — engine shouldn't know about game-specific sound names
- **Recommendation:** Move sound registration to a callback or the initial Scene's `show()`

### `engine/entity/Entity.java`

- **Purpose:** Abstract base entity with position (x,y) and dimensions (w,h)
- **Key methods:** `draw(SpriteBatch)` (non-abstract, empty), `dispose()` (non-abstract, empty)
- **SOLID:** ✅ Clean — empty defaults enable LSP

### `engine/entity/EntityManager.java`

- **Purpose:** Entity lifecycle management with pending-add queue, dirty-flag cached snapshots
- **Key methods:** `addEntity()`, `update()`, `getEntities()`, `getSnapshot()`
- **SOLID Violations:**
  - ⚠️ **OCP:** `instanceof IExpirable` check in `update()` for entity cleanup
- **Recommendation:** Could use entity lifecycle callback instead of instanceof

### `engine/entity/TextureObject.java`

- **Purpose:** Entity with texture. Flyweight cache via static `HashMap<String, Texture>`
- **Key methods:** `getOrLoadTexture(String)` (static), `draw(SpriteBatch)`, `dispose()` (no-op for shared textures)
- **SOLID:** ✅ Clean — Flyweight works well here

### `engine/entity/Shape.java`

- **Purpose:** Abstract entity with `Color`, drawn via `ShapeRenderer`
- **Key methods:** `draw(ShapeRenderer)` — abstract
- **SOLID:** ✅ Clean

### `engine/entity/IFlashable.java`

- **Purpose:** Interface — `triggerDamageFlash()`, `isFlashing()`
- **SOLID:** ✅ Clean, ISP-compliant

### `engine/entity/IExpirable.java`

- **Purpose:** Interface — `isExpired()`
- **SOLID:** ✅ Clean, ISP-compliant

### `engine/physics/PhysicsWorld.java`

- **Purpose:** Facade wrapping Box2D `World`. Reuses single `BodyDef`/`FixtureDef`/`PolygonShape`
- **Key methods:** `createBody()` returns `PhysicsBody`, `step()`, `dispose()`
- **SOLID:** ✅ Clean facade — hides Box2D behind engine API

### `engine/physics/PhysicsBody.java`

- **Purpose:** Facade wrapping Box2D `Body`. All methods null-check the body
- **Key methods:** `setPosition()`, `applyImpulseAtCenter()`, `getLinearVelocity()`, `destroy()`, `setBullet()`
- **SOLID:** ✅ Clean — no Box2D types leak except `Vector2`

### `engine/physics/BodyType.java`

- **Purpose:** Enum (STATIC, DYNAMIC, KINEMATIC) — facade over `BodyDef.BodyType`
- **SOLID:** ✅ Clean

### `engine/movement/MovementManager.java`

- **Purpose:** Iterates `IMovable` entities, calls `move()`, then `world.step()`
- **SOLID Violations:**
  - ⚠️ **OCP:** `instanceof IMovable` check to filter entities

### `engine/movement/MovableEntity.java`

- **Purpose:** `TextureObject` + `IMovable`. Composes `ControlSource`, `PhysicsBody`, `MovementModel`, `MovementStrategy`
- **Key methods:** `move()` delegates to strategy or controls, `syncPosition()` (physics→pixel)
- **SOLID:** ✅ Clean composition

### `engine/movement/IMovable.java`

- **Purpose:** Interface — `move(float deltaTime)`
- **SOLID:** ✅ Clean

### `engine/movement/AIControlled.java`

- **Purpose:** `ControlSource` with sine/cosine wave movement
- **SOLID:** ⚠️ Appears to be **legacy/unused** by the actual NPC system (NPCs use `NpcDrivingStrategy` instead)
- **Recommendation:** Verify usage; delete if dead code

### `engine/movement/MovementStrategy.java`

- **Purpose:** Interface — `getX(MovableEntity, dt)`, `getY(MovableEntity, dt)`
- **SOLID:** ✅ Clean

### `engine/movement/MovementModel.java`

- **Purpose:** Interface — `step(PhysicsBody, x, y, dt)`, default `onEnterZone/onExitZone`
- **SOLID:** ✅ Clean

### `engine/movement/UserControlled.java`

- **Purpose:** `ControlSource` wrapping `ActionInput`. Reads LEFT/RIGHT/UP/DOWN/ACTION
- **SOLID:** ✅ Clean

### `engine/scene/Scene.java` (~330 lines)

- **Purpose:** Abstract base with dual viewport system (world `ExtendViewport` + UI `FitViewport`)
- **Key constants:** `VIRTUAL_WIDTH=1280`, `VIRTUAL_HEIGHT=720`
- **Key methods:** `show()`, `update()`, `render()` (abstract), `resize()`, `dispose()`, `createViewport()` hook
- **SOLID:** ✅ Clean — good Template Method base

### `engine/scene/SceneManager.java`

- **Purpose:** Stack-based scene management. `push/pop/set` with `IOManager` injection
- **Key methods:** `push()`, `pop()`, `set()`, `render()`, `resize()`, `dispose()`
- **SOLID:** ✅ Clean

### `engine/collision/CollisionManager.java`

- **Purpose:** Box2D `ContactListener`. Extracts `Entity` from body `userData`. Delegates to `List<ICollisionListener>`
- **SOLID Violations:**
  - ⚠️ **OCP:** `instanceof Entity` check in contact extraction (necessary for Box2D integration)

### `engine/collision/ICollisionListener.java`

- **Purpose:** Interface — `onCollisionBegin`, `onCollisionEnd`, `onImpact`
- **SOLID:** ✅ Clean

### `engine/system/IGameSystem.java`

- **Purpose:** Interface — `update(deltaTime)`, `dispose()`
- **SOLID:** ✅ Clean

### `engine/event/EventBus.java`

- **Purpose:** `publish/subscribe` with `Class<T>` key. `HashMap<Class, List<Consumer>>`
- **Key methods:** `subscribe()`, `publish()`, `clear()`
- **SOLID:** ✅ Clean

### `engine/event/GameEvent.java`

- **Purpose:** Marker interface for all events
- **SOLID:** ✅ Clean

### `engine/io/IOManager.java`

- **Purpose:** Holds `List<InputDevice>` + `SoundDevice`
- **SOLID Violations:**
  - ⚠️ **OCP:** `getInputs(Class<T>)` uses `instanceof` to filter input devices

### `engine/io/InputDevice.java`

- **Purpose:** Interface — `handleInput`, `addBind`, `removeBind`, `pushContext`, `popContext`, `resetToBase`
- **SOLID:** ✅ Clean

### `engine/io/SoundDevice.java`

- **Purpose:** Interface — `playSound`, `addSound`, `loopSound`, `stopSound`, `setSoundVolume`, `stopAllSounds`, `toggleMute`, `isMuted`, `isLooping`, `get/setMasterVolume`, `dispose`
- **SOLID:** ⚠️ **ISP concern** — 12+ methods is a large interface. Could split into `SoundPlayer` and `SoundConfig`

### `engine/io/ActionInput.java`

- **Purpose:** Interface — `isHeld(String)`, `justPressed(String)`
- **SOLID:** ✅ Clean

### `engine/io/ControlSource.java`

- **Purpose:** Interface — `getX`, `getY`, `isAction`, `isUserControlled`
- **SOLID:** ✅ Clean

### `engine/io/ICommand.java`

- **Purpose:** Interface — `execute()`, `undo()`
- **SOLID:** ✅ Clean

### `engine/io/CommandHistory.java`

- **Purpose:** `Deque<ICommand>` undo stack — `executeAndRecord()`, `undoLast()`
- **SOLID:** ✅ Clean

### `engine/sound/SoundEffect.java`

- **Purpose:** Wraps LibGDX `Sound` — play, loop, stop, setVolume, dispose. Tracks `loopId`
- **SOLID:** ✅ Clean

### `engine/sound/SoundManager.java`

- **Purpose:** Implements `SoundDevice`. `HashMap<String, SoundEffect>`. Mute state, master volume
- **SOLID:** ✅ Clean

---

## 3. Game Layer Inventory

### `game/GameConstants.java` (~180 lines)

- **Purpose:** All gameplay tuning constants: scoring, dimensions, level parameters, fuel rates, audio paths, NPC perception thresholds
- **SOLID:** ✅ Clean — centralises magic numbers

### `game/scene/BaseGameScene.java` (~600+ lines)

- **Purpose:** Template Method base for all gameplay levels
- **Abstract hooks:** `getLevelLength`, `getMaxSpeed`, `getAcceleration`, `getBrakeRate`, `getBgmPath`, `initLevelData`, `updateGame`, `isGameOver`, `getLevelName`, `createRetryScene`, `getGameOverReason`, `getMaxScrollPixelsPerSecond`
- **SOLID Violations:**
  - 🔴 **SRP:** Manages physics world creation, player car creation, boundary walls, input binds, scroll logic, explosion handling, level-end checks, rendering pipeline — too many responsibilities
  - 🔴 **OCP:** `world.createBody()` calls for boundary walls and player car body directly (non-factory)
  - ⚠️ **Magic Numbers:** `CAMERA_LOOK_AHEAD=120f`, `EXPLOSION_DELAY=1.5f` as class constants (not in GameConstants)
  - ⚠️ **Raw OpenGL:** `Gdx.gl.glClearColor()`/`Gdx.gl.glClear()` in `render()`
  - ⚠️ **new Entity:** Creates `new Vector2` in `triggerExplosionGameOver`

### `game/scene/Level1Scene.java` (~170 lines)

- **Purpose:** Clean orchestrator/composition root for Level 1. Delegates to `TrafficSpawningSystem`, `CrosswalkEncounterSystem`, `CrosswalkRenderer`
- **SOLID:** ✅ Clean — proper delegation

### `game/scene/Level2Scene.java` (~250 lines)

- **Purpose:** Clean orchestrator for Level 2. Delegates to `TrafficSpawningSystem`, `PoliceCarFactory`, `RainEffectSystem`, `RoadHazardSpawner`
- **SOLID:** ✅ Clean — references police car as `IChaseEntity` (DIP)

### `game/scene/CrosswalkEncounterSystem.java` (~320 lines)

- **Purpose:** `IGameSystem` managing crosswalk zones, pedestrian encounters, stop signs
- **SOLID Violations:**
  - 🔴 **OCP/SRP:** `createCrosswalkZone()` and `createEncounter()` both call `world.createBody()` directly — body creation in non-factory class
  - ⚠️ **Magic Numbers:** `CROSSWALK_HEIGHT=80f`, `CROSSING_SPEED=160f`, `VIRTUAL_HEIGHT=720f` (duplicated from Scene), `pedHalfW * 0.4f` multipliers

### `game/scene/RoadRenderer.java` (~100 lines)

- **Purpose:** Pure `ShapeRenderer` road drawing
- **Key constants:** `ROAD_LEFT=340`, `ROAD_RIGHT=940`, `ROAD_WIDTH=600`, `LANE_COUNT=3`
- **SOLID:** ✅ Clean

### `game/scene/TrafficSpawningSystem.java` (~90 lines)

- **Purpose:** `IGameSystem` composing `NPCCarSpawner`, `PickupableSpawner`, `TreeSpawner`
- **SOLID:** ✅ Clean delegation

### `game/scene/ResultsScene.java` (~240 lines)

- **Purpose:** Scene2D Stage for results display. Constructor takes `LevelResult` + `Supplier<Scene>` for retry (DIP)
- **SOLID Violations:**
  - ⚠️ **DIP:** Uses `new StartScene()` directly for menu button — concrete coupling

### `game/scene/LevelSelectScene.java` (~210 lines)

- **Purpose:** Scene2D buttons for level selection
- **SOLID Violations:**
  - ⚠️ **DIP:** Uses `new Level1Scene()` and `new Level2Scene()` directly — concrete scene coupling
  - ⚠️ **DIP:** Uses `new StartScene()` directly for back button

### `game/scene/StartScene.java` (~170 lines)

- **Purpose:** Scene2D title screen
- **SOLID Violations:**
  - ⚠️ **DIP:** Uses `new LevelSelectScene()` directly — concrete coupling

### `game/scene/PauseScene.java` (~230 lines)

- **Purpose:** Transparent overlay with pause menu. Scene2D Stage. Volume control, navigation
- **SOLID Violations:**
  - ⚠️ **DIP:** `confirm()` uses `new StartScene()` directly — concrete coupling
  - ⚠️ **OCP:** Switch on `selectedOption` index — fragile if menu items change

### `game/scene/LevelResult.java` (~70 lines)

- **Purpose:** Immutable value object carrying level outcome (score, time, rulesBroken, levelName, violations)
- **SOLID:** ✅ Clean — all fields final, `Collections.unmodifiableList` for violations

### `game/scene/ILevelEndCondition.java`

- **Purpose:** `@FunctionalInterface` — `evaluate()` returns boolean
- **SOLID:** ✅ Clean — OCP enabler for end-condition checks

### `game/scene/CrosswalkRenderer.java` (~50 lines)

- **Purpose:** Renders crosswalk zone overlays for Level 1
- **SOLID Violations:**
  - ⚠️ **Raw OpenGL:** `Gdx.gl.glEnable(GL20.GL_BLEND)` / `Gdx.gl.glBlendFunc()` / `Gdx.gl.glDisable()`

### `game/scene/RainEffectSystem.java` (~150 lines)

- **Purpose:** Level 2 rain overlay (atmosphere, wet-lens blur, vignette, animated rain streaks)
- **Key constants:** Uses `Scene.VIRTUAL_WIDTH` / `Scene.VIRTUAL_HEIGHT` properly
- **SOLID Violations:**
  - ⚠️ **Raw OpenGL:** `Gdx.gl.glEnable/glDisable(GL20.GL_BLEND)`, `Gdx.gl.glBlendFunc()`
  - ⚠️ **Magic Numbers:** Numerous rendering magic numbers (`0.10f`, `0.13f`, `0.22f`, `45f`, `55f`, etc.) — rendering constants not in GameConstants

### `game/state/SpeedScrollController.java` (~95 lines)

- **Purpose:** `IGameSystem` managing `simulatedSpeed` and `scrollOffset`
- **SOLID Violations:**
  - ⚠️ **Magic Number:** `PASSIVE_DECEL=18f` — not in GameConstants
  - ⚠️ **DIP:** `setKeyboard()` method needs concrete `Keyboard` reference (though it accepts `ActionInput`)

### `game/state/DashboardUI.java` (~400 lines)

- **Purpose:** Scene2D Stage for HUD. Implements `IDashboardObserver`. Score, wanted stars, progress bar, police distance, fuel bar, speed display
- **SOLID Violations:**
  - ⚠️ **SRP:** Handles score display, wanted stars, progress bar, police distance bar, fuel bar, speed display, popups — many rendering responsibilities
  - ⚠️ **Magic Numbers:** Hardcoded `1280f`, `720f` layout positions (should reference `Scene.VIRTUAL_WIDTH/HEIGHT`)
  - ⚠️ **Magic Numbers:** Numerous layout constants not centralised: `BAR_WIDTH`, `ICON_SIZE`, `STAR_SIZE`, `LABEL_SCALE`, `MAX_WANTED_STARS`

### `game/state/AudioController.java` (~110 lines)

- **Purpose:** `IGameSystem` managing BGM (`Music`) and drive sound loop
- **Key constants:** `BGM_BASE_VOLUME=0.2f`
- **SOLID:** ✅ Mostly clean

### `game/state/FuelController.java` (~70 lines)

- **Purpose:** `IGameSystem` wrapping `FuelSystem`. EventBus integration
- **SOLID:** ✅ Clean — subscribes to `PickupCollectedEvent`, publishes `FuelDepletedEvent`

### `game/state/FuelSystem.java` (~50 lines)

- **Purpose:** Pure fuel state: drain/recharge/isEmpty
- **SOLID:** ✅ Clean — pure state management

### `game/state/ScorePopupManager.java` (~95 lines)

- **Purpose:** Floating "+50"/"-100" popup animations. `BitmapFont` properly disposed
- **SOLID:** ✅ Clean

### `game/state/IDashboardObserver.java`

- **Purpose:** Observer interface with 5 methods: `onScoreUpdated`, `onProgressUpdated`, `onRuleBroken`, `onSpeedChanged`, `onFuelUpdated`(default)
- **SOLID Violations:**
  - ⚠️ **ISP:** 5 methods — all observers must implement all 4 (+ 1 default). Could split into smaller observer interfaces

### `game/state/CrosswalkZoneState.java` (~65 lines)

- **Purpose:** Pure state object for crosswalk zone tracking (playerInside, crossingActive, violationFired, passed, expired)
- **SOLID:** ✅ Clean

### `game/collision/GameCollisionHandler.java` (~180 lines)

- **Purpose:** Facade/coordinator. Implements `ICollisionListener`. Delegates to 8 specialised handlers
- **Key helpers:** `getPlayerEntity()` (uses `instanceof MovableEntity`), `extractEntity()` (uses `type.isInstance()`)
- **SOLID Violations:**
  - ⚠️ **OCP:** Multiple `instanceof` checks for entity type detection in static helpers
  - ✅ Good delegation to specialised handlers

### `game/collision/handlers/CrosswalkCollisionHandler.java` (~50 lines)

- **SOLID:** ✅ Clean — single responsibility

### `game/collision/handlers/NPCCarCollisionHandler.java` (~90 lines)

- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `CRASH_COOLDOWN_MS=1500`, `CRASH_KNOCKBACK_MULTIPLIER=8f` — not in GameConstants

### `game/collision/handlers/PedestrianCollisionHandler.java` (~60 lines)

- **SOLID:** ✅ Clean

### `game/collision/handlers/PickupCollisionHandler.java` (~40 lines)

- **SOLID:** ✅ Clean

### `game/collision/handlers/BoundaryCollisionHandler.java` (~30 lines)

- **SOLID:** ✅ Clean

### `game/collision/handlers/ExplosionCollisionHandler.java` (~30 lines)

- **SOLID:** ✅ Clean

### `game/collision/handlers/ZoneCollisionHandler.java` (~60 lines)

- **SOLID Violations:**
  - ⚠️ **OCP:** Uses `instanceof` for `MotionZone` and `CrosswalkZone` type checking

### `game/collision/handlers/NPCPedestrianCollisionHandler.java` (~30 lines)

- **SOLID:** ✅ Clean

### `game/collision/PedestrianHitReaction.java` (~95 lines)

- **Purpose:** Hit animation state machine
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `HIT_DURATION=2.5f`, `SPIN_SPEED=720f` — not in GameConstants

### `game/collision/listeners/TrafficViolationListener.java`

- **Purpose:** Interface with 3 default methods: `onCrosswalkViolation`, `onPedestrianHit`, `onTrafficCrash`
- **SOLID:** ✅ Clean — defaults allow partial implementation

### `game/collision/listeners/PickupListener.java`

- **Purpose:** Single method interface: `onPickup()`
- **SOLID:** ✅ Clean

### `game/collision/listeners/Level1TrafficListener.java` (~90 lines)

- **Purpose:** Standalone class with functional callbacks. Uses `BreakRuleCommand` + `CommandHistory`
- **SOLID:** ✅ Clean

### `game/collision/listeners/Level2TrafficListener.java` (~60 lines)

- **Purpose:** Only overrides `onTrafficCrash`. No crosswalks/pedestrians
- **SOLID:** ✅ Clean

### `game/entities/vehicles/PlayerCar.java` (~90 lines)

- **Purpose:** `MovableEntity` + `IFlashable`. Flash timer for damage effect
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `FLASH_DURATION=0.8f`, `FLASH_FREQUENCY=8f` — not in GameConstants

### `game/entities/vehicles/NPCCar.java` (~120 lines)

- **Purpose:** `MovableEntity` + `IExpirable` + `IPerceivable`. Lane-based lifecycle (preview→scroll→expire)
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `MAX_LIFETIME=10f`, `PREVIEW_DURATION=0.8f` — not in GameConstants

### `game/entities/vehicles/PoliceCar.java` (~130 lines)

- **Purpose:** `TextureObject` + `IChaseEntity`. Delegates chase to `PoliceMovement`. Siren flash animation
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `FLASH_INTERVAL=0.15f`, 4 texture frame names hardcoded

### `game/entities/IChaseEntity.java`

- **Purpose:** Interface — `updateChase`, `hasCaughtPlayer`, `getScreenY`
- **SOLID:** ✅ Clean

### `game/entities/IPerceivable.java`

- **Purpose:** Interface — `getPerceptionCategory()`
- **SOLID:** ✅ Clean

### `game/entities/PerceptionCategory.java`

- **Purpose:** Enum — `PEDESTRIAN`, `VEHICLE`, `OBSTACLE`
- **SOLID:** ✅ Clean

### `game/entities/misc/Pedestrian.java` (~130 lines)

- **Purpose:** `TextureObject` + `IExpirable` + `IPerceivable`. Has `PhysicsBody`, `relativeY`, `renderRotation`
- **SOLID:** ✅ Clean

### `game/entities/misc/Pickupable.java` (~90 lines)

- **Purpose:** `TextureObject` + `IExpirable`. DYNAMIC sensor body. `updatePosition` syncs body
- **SOLID:** ✅ Clean

### `game/entities/misc/ExplosionParticle.java` (~110 lines)

- **Purpose:** `TextureObject` + `IExpirable`. Static factory `spawnExplosion()` creates ring of 12 particles
- **SOLID:** ✅ Clean — good factory method

### `game/entities/misc/ExplosionOverlay.java` (~50 lines)

- **Purpose:** `TextureObject` + `IExpirable`. Fading overlay sprite
- **SOLID:** ✅ Clean

### `game/entities/misc/Tree.java` (~55 lines)

- **Purpose:** `TextureObject` + `IExpirable` + `IPerceivable` (OBSTACLE). Scrolls with road
- **SOLID:** ✅ Clean

### `game/entities/misc/Trees.java` (~85 lines)

- **Purpose:** Contains inner `private static class Tree extends TextureObject`
- **SOLID Violations:**
  - 🔴 **Naming:** Inner class `Tree` **shadows** the outer `game/entities/misc/Tree.java` class — confusing
  - ⚠️ References `Scene.VIRTUAL_HEIGHT` / `Scene.VIRTUAL_WIDTH` statically

### `game/entities/misc/StopSign.java` (~70 lines)

- **Purpose:** `TextureObject` + `IExpirable` + `IPerceivable` (OBSTACLE). Decorative, no physics body
- **SOLID:** ✅ Clean

### `game/factory/NPCCarSpawner.java` (~230 lines)

- **Purpose:** Implements `ILaneOccupancy`. Creates `NPCCar` with `AIControlled` + `NpcDrivingStrategy` + `CarMovementModel` + `SensorComponent`
- **SOLID Violations:**
  - 🔴 **OCP:** `world.createBody()` KINEMATIC bodies — acceptable as factory, but creates multiple entity types
  - ⚠️ **Magic Numbers:** `PREVIEW_PEEK=35f`, `APPROACH_SPEED=250f`, `DOUBLE_SPAWN_CHANCE=0.30f` — not in GameConstants

### `game/factory/PoliceCarFactory.java` (~60 lines)

- **Purpose:** Creates `PoliceCar` + KINEMATIC `PhysicsBody`. Returns `IChaseEntity`
- **SOLID:** ✅ Clean — proper factory returning interface type

### `game/factory/PickupableSpawner.java` (~130 lines)

- **Purpose:** Spawns `Pickupable` with DYNAMIC sensor body. Avoids occupied lanes via `NPCCarSpawner` occupancy
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `PICKUP_SIZE=100f` — not in GameConstants

### `game/factory/TreeSpawner.java` (~80 lines)

- **Purpose:** Spawns `Tree` entities on road shoulders
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `TREE_W=60`, `TREE_H=80`, `MIN_Y_SPACING=250`
  - 🔴 **Magic Number:** Hardcoded `1280f` instead of `Scene.VIRTUAL_WIDTH` constant

### `game/factory/RoadHazardSpawner.java` (~160 lines)

- **Purpose:** Implements `ILaneOccupancy`. Spawns `RoadHazard` zones with configurable `SurfaceEffect`
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `HAZARD_W=100`, `HAZARD_H=40` — not in GameConstants

### `game/factory/ILaneOccupancy.java`

- **Purpose:** Interface — `getOccupiedLanesNear(nearY, range)`
- **SOLID:** ✅ Clean

### `game/movement/CarMovementModel.java` (~180 lines)

- **Purpose:** Implements `MovementModel`. `VehicleProfile`-driven. `SurfaceEffect` handling (puddle slip, oil sticky)
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `REVERSE_MAX_SPEED=3f`, `REVERSE_ACCEL=8f` — not in GameConstants

### `game/movement/PlayerMovementStrategy.java` (~55 lines)

- **Purpose:** Implements `MovementStrategy`. Sensitivity multipliers
- **SOLID:** ✅ Clean

### `game/movement/NpcDrivingStrategy.java` (~55 lines)

- **Purpose:** Implements `MovementStrategy`. Uses `AIPerceptionService` + `SensorComponent`. References `GameConstants` thresholds
- **SOLID:** ✅ Clean — proper DIP via strategy interface

### `game/movement/PoliceMovement.java` (~55 lines)

- **Purpose:** Chase algorithm for police car
- **SOLID Violations:**
  - ⚠️ **Magic Numbers:** `BASE_APPROACH_SPEED=70f`, `AGGRESSION_BONUS=130f`, `LANE_TRACK_SPEED=4f` — not in GameConstants

### `game/movement/PedestrianMovement.java` (~90 lines)

- **Purpose:** Scene-owned pedestrian crossing movement state
- **SOLID Violations:**
  - ⚠️ **Coupling:** References `RoadRenderer.ROAD_LEFT` / `ROAD_RIGHT` for finish positions — cross-package coupling

### `game/movement/PedestrianIntent.java` (~20 lines)

- **Purpose:** Direction holder (+1 or -1)
- **SOLID:** ✅ Clean

### `game/movement/AIPerceptionService.java` (~75 lines)

- **Purpose:** Scans `EntityManager` snapshot for nearby `IPerceivable` entities
- **SOLID Violations:**
  - ⚠️ **OCP:** `switch` on `PerceptionCategory` values — needs updating if new categories added

### `game/movement/SensorComponent.java` (~40 lines)

- **Purpose:** Configuration data: `forwardRange`, `sideRange`, `stopDistance`, `followDistance`
- **SOLID:** ✅ Clean

### `game/movement/VehicleProfile.java` (~100 lines)

- **Purpose:** Immutable value object with vehicle movement stats. Static factory methods `playerArcade()`, `npcTraffic()`
- **SOLID:** ✅ Clean

### `game/movement/SurfaceEffect.java` (~90 lines)

- **Purpose:** Immutable value object with surface physics multipliers. Static constants: `DEFAULT`, `PUDDLE`, `MUD`, `CROSSWALK`, `SCHOOL_ZONE`
- **SOLID:** ✅ Clean — good use of immutable presets

### `game/movement/PerceptionSnapshot.java` (~25 lines)

- **Purpose:** Java `record` with perception data. Factory method `clear(float defaultDistance)`
- **SOLID:** ✅ Clean

### `game/rules/RuleManager.java` (~80 lines)

- **Purpose:** Tracks traffic-rule violations. Generic `recordViolation()`, violation log, police aggression calculation
- **SOLID:** ✅ Clean — OCP-compliant (new violation types are new ICommands)

### `game/rules/BreakRuleCommand.java` (~70 lines)

- **Purpose:** Concrete `ICommand` incrementing rule-break counter. Supports weighted violations
- **SOLID:** ✅ Clean Command Pattern

### `game/rules/PedestrianHitCommand.java` (~45 lines)

- **Purpose:** Concrete `ICommand` for instant-fail pedestrian hit penalty
- **SOLID:** ✅ Clean

### `game/io/Keyboard.java` (~130 lines)

- **Purpose:** Implements `InputDevice` + `ActionInput`. Stack-based input contexts. Command binds + action binds
- **SOLID Violations:**
  - ⚠️ **DIP:** Uses `Gdx.input.isKeyPressed()` / `Gdx.input.isKeyJustPressed()` directly — polling LibGDX statics. Acceptable for an I/O adapter

### `game/zone/CrosswalkZone.java` (~120 lines)

- **Purpose:** `Shape` + `IExpirable`. Composes `CrosswalkZoneState`, `SurfaceEffect.CROSSWALK`. ShapeRenderer-drawn with stripe pattern
- **SOLID:** ✅ Clean — delegates state management to `CrosswalkZoneState`

### `game/zone/MotionZone.java` (~70 lines)

- **Purpose:** `Shape` with `SurfaceEffect` physics. Creates `PhysicsBody` directly OR accepts externally-created body for subclasses
- **SOLID Violations:**
  - ⚠️ **SRP:** Primary constructor calls `world.createBody()` — body creation in entity, not factory

### `game/zone/RoadHazard.java` (~90 lines)

- **Purpose:** Scrollable `MotionZone` + `IExpirable`. Uses `TextureObject.getOrLoadTexture()` for rendering
- **SOLID Violations:**
  - ⚠️ **SRP:** Constructor calls `world.createBody()` via parent — body creation in entity

### `game/event/ScoreChangedEvent.java`

- **Purpose:** `GameEvent` with `int delta`
- **SOLID:** ✅ Clean

### `game/event/PickupCollectedEvent.java`

- **Purpose:** `GameEvent` singleton — `instance()`
- **SOLID:** ✅ Clean

### `game/event/InstantFailEvent.java`

- **Purpose:** `GameEvent` with `String reason`
- **SOLID:** ✅ Clean

### `game/event/FuelDepletedEvent.java`

- **Purpose:** `GameEvent` singleton — `instance()`
- **SOLID:** ✅ Clean

---

## 4. LWJGL3 Layer Inventory

### `lwjgl3/Lwjgl3Launcher.java`

- **Purpose:** Composition root. Wires `GameMaster(new StartScene(), new Keyboard(), new SoundManager())`
- **SOLID:** ✅ Clean — expected to know concrete types as composition root

### `lwjgl3/StartupHelper.java`

- **Purpose:** macOS `-XstartOnFirstThread` workaround + Windows non-ASCII username fix
- **SOLID:** ✅ N/A — third-party utility (Apache 2.0 license)

---

## 5. SOLID Violations Summary

### S — Single Responsibility Principle

| Severity | File                            | Issue                                                                                                                                            |
| -------- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| 🔴 HIGH  | `BaseGameScene.java`            | 600+ lines managing physics world, player car, boundaries, input, scroll, explosion, level-end, rendering — at least 4 distinct responsibilities |
| ⚠️ MED   | `DashboardUI.java`              | 400 lines rendering 6 different HUD components (score, stars, progress, police, fuel, speed)                                                     |
| ⚠️ MED   | `CrosswalkEncounterSystem.java` | Manages zone creation + pedestrian spawning + collision tracking + body creation                                                                 |
| ⚠️ LOW   | `MotionZone.java`               | Entity creates its own physics body                                                                                                              |

### O — Open/Closed Principle

| Severity | File                        | Issue                                                       |
| -------- | --------------------------- | ----------------------------------------------------------- |
| ⚠️ MED   | `EntityManager.java`        | `instanceof IExpirable` in `update()`                       |
| ⚠️ MED   | `MovementManager.java`      | `instanceof IMovable` in entity filter                      |
| ⚠️ MED   | `GameCollisionHandler.java` | `instanceof MovableEntity` + `type.isInstance()` in helpers |
| ⚠️ MED   | `ZoneCollisionHandler.java` | `instanceof MotionZone`/`CrosswalkZone`                     |
| ⚠️ MED   | `CollisionManager.java`     | `instanceof Entity` in contact extraction                   |
| ⚠️ MED   | `IOManager.java`            | `instanceof` in `getInputs()`                               |
| ⚠️ MED   | `AIPerceptionService.java`  | `switch(PerceptionCategory)`                                |
| ⚠️ MED   | `PauseScene.java`           | `switch(selectedOption)` index matching                     |

### L — Liskov Substitution Principle

| Severity | File         | Issue                      |
| -------- | ------------ | -------------------------- |
| ✅       | (none found) | No LSP violations detected |

### I — Interface Segregation Principle

| Severity | File                      | Issue                                                  |
| -------- | ------------------------- | ------------------------------------------------------ |
| ⚠️ MED   | `SoundDevice.java`        | 12+ methods — large interface                          |
| ⚠️ LOW   | `IDashboardObserver.java` | 5 methods — observers must implement all 4 non-default |

### D — Dependency Inversion Principle

| Severity | File                    | Issue                                                        |
| -------- | ----------------------- | ------------------------------------------------------------ |
| ⚠️ MED   | `LevelSelectScene.java` | `new Level1Scene()`, `new Level2Scene()` — concrete coupling |
| ⚠️ MED   | `StartScene.java`       | `new LevelSelectScene()` — concrete coupling                 |
| ⚠️ MED   | `PauseScene.java`       | `new StartScene()` — concrete coupling                       |
| ⚠️ MED   | `ResultsScene.java`     | `new StartScene()` — concrete coupling                       |
| ⚠️ LOW   | `GameMaster.java`       | Hardcodes `"menu"`/`"selected"` sound names                  |

---

## 6. Specific Anti-Pattern Checks

### 6.1 `world.createBody()` in Non-Factory Classes

| File                            | Method                                       | Severity |
| ------------------------------- | -------------------------------------------- | -------- |
| `BaseGameScene.java`            | `show()` — boundary walls + player car body  | 🔴 HIGH  |
| `CrosswalkEncounterSystem.java` | `createCrosswalkZone()`, `createEncounter()` | 🔴 HIGH  |
| `MotionZone.java`               | Primary constructor                          | ⚠️ MED   |
| `RoadHazard.java`               | Constructor (via super)                      | ⚠️ MED   |

**Factory classes (correct):** `NPCCarSpawner`, `PoliceCarFactory`, `PickupableSpawner`, `RoadHazardSpawner`

### 6.2 Raw OpenGL Calls (`Gdx.gl.*`)

| File                     | Calls                                                            |
| ------------------------ | ---------------------------------------------------------------- |
| `BaseGameScene.java`     | `Gdx.gl.glClearColor()`, `Gdx.gl.glClear()`                      |
| `CrosswalkRenderer.java` | `Gdx.gl.glEnable(GL20.GL_BLEND)`, `glBlendFunc()`, `glDisable()` |
| `RainEffectSystem.java`  | `Gdx.gl.glEnable(GL20.GL_BLEND)`, `glBlendFunc()`, `glDisable()` |

### 6.3 Magic Numbers NOT in GameConstants

| File                            | Constants                                                                 |
| ------------------------------- | ------------------------------------------------------------------------- |
| `BaseGameScene.java`            | `CAMERA_LOOK_AHEAD=120f`, `EXPLOSION_DELAY=1.5f`                          |
| `CrosswalkEncounterSystem.java` | `CROSSWALK_HEIGHT=80f`, `CROSSING_SPEED=160f`, `VIRTUAL_HEIGHT=720f`      |
| `NPCCarSpawner.java`            | `PREVIEW_PEEK=35f`, `APPROACH_SPEED=250f`, `DOUBLE_SPAWN_CHANCE=0.30f`    |
| `NPCCarCollisionHandler.java`   | `CRASH_COOLDOWN_MS=1500`, `CRASH_KNOCKBACK_MULTIPLIER=8f`                 |
| `PedestrianHitReaction.java`    | `HIT_DURATION=2.5f`, `SPIN_SPEED=720f`                                    |
| `PlayerCar.java`                | `FLASH_DURATION=0.8f`, `FLASH_FREQUENCY=8f`                               |
| `NPCCar.java`                   | `MAX_LIFETIME=10f`, `PREVIEW_DURATION=0.8f`                               |
| `PoliceCar.java`                | `FLASH_INTERVAL=0.15f`                                                    |
| `PoliceMovement.java`           | `BASE_APPROACH_SPEED=70f`, `AGGRESSION_BONUS=130f`, `LANE_TRACK_SPEED=4f` |
| `CarMovementModel.java`         | `REVERSE_MAX_SPEED=3f`, `REVERSE_ACCEL=8f`                                |
| `SpeedScrollController.java`    | `PASSIVE_DECEL=18f`                                                       |
| `DashboardUI.java`              | `1280f`, `720f` hardcoded layout positions                                |
| `TreeSpawner.java`              | `1280f` hardcoded (should be `Scene.VIRTUAL_WIDTH`)                       |
| `PickupableSpawner.java`        | `PICKUP_SIZE=100f`                                                        |
| `RoadHazardSpawner.java`        | `HAZARD_W=100`, `HAZARD_H=40`                                             |
| `RainEffectSystem.java`         | Numerous rendering magic numbers                                          |

### 6.4 `instanceof` Checks

| Location                                 | Check                                    | Purpose                     |
| ---------------------------------------- | ---------------------------------------- | --------------------------- |
| `EntityManager.update()`                 | `instanceof IExpirable`                  | Cleanup expired entities    |
| `MovementManager.update()`               | `instanceof IMovable`                    | Filter movable entities     |
| `CollisionManager.beginContact()`        | `instanceof Entity`                      | Extract from Box2D userData |
| `GameCollisionHandler.getPlayerEntity()` | `instanceof MovableEntity`               | Find player                 |
| `GameCollisionHandler.extractEntity()`   | `type.isInstance()`                      | Generic entity extraction   |
| `ZoneCollisionHandler`                   | `instanceof MotionZone`, `CrosswalkZone` | Zone type dispatch          |
| `IOManager.getInputs()`                  | `instanceof T`                           | Filter input devices        |

### 6.5 Box2D Imports in Game Layer

The **PhysicsWorld/PhysicsBody/BodyType** facade successfully shields most game code from Box2D. However:

| File     | Direct Box2D Usage                                                        |
| -------- | ------------------------------------------------------------------------- |
| **None** | ✅ No direct `com.badlogic.gdx.physics.box2d` imports found in game layer |

The facade is working correctly — game code only uses `engine.physics.*`.

### 6.6 `Gdx.input` Polling

| File            | Usage                                            | Acceptable?                   |
| --------------- | ------------------------------------------------ | ----------------------------- |
| `Keyboard.java` | `Gdx.input.isKeyPressed()`, `isKeyJustPressed()` | ✅ Yes — it's the I/O adapter |

No other game code polls `Gdx.input` directly — all input goes through the `ActionInput`/`InputDevice` abstraction.

### 6.7 Dispose Leaks

| File                      | Issue                                                                                                     |
| ------------------------- | --------------------------------------------------------------------------------------------------------- |
| `TextureObject.dispose()` | No-op for shared textures — relies on application shutdown. Could leak if not all textures used until end |
| **All others**            | ✅ Proper dispose chains observed                                                                         |

### 6.8 Mutable Public Arrays/Collections

| File                            | Issue                                                             |
| ------------------------------- | ----------------------------------------------------------------- |
| `RainEffectSystem.java`         | `dropX[]`, `dropY[]`, `dropLen[]`, `dropSpd[]` — private, no leak |
| `RuleManager.getViolationLog()` | ✅ Returns `Collections.unmodifiableList()`                       |
| `LevelResult.getViolations()`   | ✅ Returns unmodifiable list                                      |
| **Overall**                     | ✅ No mutable public arrays found                                 |

### 6.9 Class Name Shadowing

| Issue                           | Details                                                                                                          |
| ------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `Trees.java` inner class `Tree` | Private inner `Tree extends TextureObject` shadows `game/entities/misc/Tree.java` — confusing during development |

### 6.10 Potentially Dead Code

| File                                | Issue                                                                                                                                      |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `engine/movement/AIControlled.java` | Sine/cosine wave controller. NPCs actually use `NpcDrivingStrategy` + `CarMovementModel`. Verify if `AIControlled` is still wired anywhere |

---

## 7. Refactoring Priority Matrix

### 🔴 HIGH PRIORITY

1. **Extract body creation from `BaseGameScene.show()`**
   - Move boundary wall + player car body creation into a dedicated factory (e.g., `LevelPhysicsFactory`)
   - Reduces `BaseGameScene` from 600+ to ~400 lines
2. **Extract body creation from `CrosswalkEncounterSystem`**
   - Move `createCrosswalkZone()` body and `createEncounter()` body into a factory method or parameter injection

3. **Consolidate magic numbers into `GameConstants`**
   - 15+ files have scattered numeric constants that belong in `GameConstants`
   - Priority: `BaseGameScene`, `CrosswalkEncounterSystem`, `NPCCarSpawner`, `PoliceMovement`

4. **Fix `Trees.java` inner class name shadowing**
   - Rename inner class to `TreeSprite` or `TreeInstance` to avoid confusion with `Tree.java`

### ⚠️ MEDIUM PRIORITY

5. **Reduce `instanceof` usage in `EntityManager`/`MovementManager`**
   - Consider entity capability flags or a component system
6. **Decouple scene navigation** (`LevelSelectScene`, `StartScene`, `PauseScene`, `ResultsScene`)
   - Use `Supplier<Scene>` or scene factory callbacks instead of `new XScene()`
   - `ResultsScene` already uses `Supplier<Scene>` for retry — extend pattern to all navigation

7. **Split `DashboardUI`** into smaller renderer components
   - `ScoreRenderer`, `WantedStarsRenderer`, `ProgressBarRenderer`, `FuelBarRenderer`, `PoliceDistanceRenderer`

8. **Remove `GameMaster` hardcoded sound names**
   - Move `"menu"`/`"selected"` registration to `StartScene.show()` or a game-layer initialiser

### ✅ LOW PRIORITY / ACCEPTABLE

9. **Raw OpenGL calls** — Acceptable for alpha blending in LibGDX. Consider wrapping if they proliferate
10. **`Gdx.input` in `Keyboard.java`** — Correct: it's the adapter boundary
11. **`SoundDevice` ISP** — 12 methods is large but cohesive for a sound API
12. **Box2D facade** — Working correctly, no leaks
13. **`instanceof Entity` in `CollisionManager`** — Necessary for Box2D userData extraction

---

## File Count Summary

| Package           | Files   | Clean  | Minor Issues | Major Issues |
| ----------------- | ------- | ------ | ------------ | ------------ |
| `engine/`         | 34      | 30     | 4            | 0            |
| `game/scene/`     | 14      | 8      | 4            | 2            |
| `game/state/`     | 8       | 5      | 3            | 0            |
| `game/collision/` | 12      | 10     | 2            | 0            |
| `game/entities/`  | 12      | 10     | 1            | 1            |
| `game/factory/`   | 6       | 2      | 4            | 0            |
| `game/movement/`  | 11      | 8      | 3            | 0            |
| `game/rules/`     | 3       | 3      | 0            | 0            |
| `game/io/`        | 1       | 0      | 1            | 0            |
| `game/zone/`      | 3       | 1      | 2            | 0            |
| `game/event/`     | 4       | 4      | 0            | 0            |
| `lwjgl3/`         | 2       | 2      | 0            | 0            |
| **TOTAL**         | **110** | **83** | **24**       | **3**        |

**Overall Assessment:** The codebase demonstrates strong OOP fundamentals with well-applied design patterns (Strategy, Template Method, Observer, Command, Facade, Flyweight). The engine/game boundary is cleanly maintained. The primary issues are scattered magic numbers, a few `world.createBody()` calls outside factory classes, and `BaseGameScene` carrying too many responsibilities. These are all addressable through targeted refactoring without architectural changes.
