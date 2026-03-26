# LibGDX Arcade Game - Comprehensive Architecture Analysis

**Date:** March 2026  
**Project:** OOP (Pedestrian Crossing Simulator + Police Chase)  
**Engine:** LibGDX + Box2D Physics  
**Scope:** Engine Layer + Game Layer Analysis

---

## Executive Summary

This is a **layered architecture** project with strict separation between **engine** (reusable, game-agnostic) and **game** (domain-specific) code. The engine provides foundational abstractions for 2D rendering, physics, collision, input, and lifecycle management. The game layer builds specific mechanics: traffic rules, pedestrian encounters, police AI, and level progression.

**Key Architectural Achievement:** Zero coupling from engine to game — the engine knows nothing about cars, pedestrians, or traffic violations. All game behaviour is injected at the composition root (`Lwjgl3Launcher`).

---

# PART 1: ENGINE LAYER ARCHITECTURE

## 1.1 Entity & Lifecycle Management

### `Entity` (Abstract Base Class)
- **Purpose:** Foundation for all renderable, updateable objects in the world
- **Pattern:** Template Method + Strategy composition
- **Key Design:** 
  - Default no-op implementations of `update()` and `draw()` (not abstract) — satisfies Liskov: subclasses only override what they need
  - Immutable position/dimension accessors + setters
  - Dispose hook for resource cleanup (GPU textures, audio)

### `EntityManager` (Double-Buffered Entity Lifecycle)
- **Purpose:** Safe entity iteration during entity addition/removal
- **Pattern:** Double-buffered list + dirty-flag snapshot caching
- **Significant Design Decisions:**
  1. **Pending entities list:** New entities go to a staging area, merged into the main list during `update()`. This prevents `ConcurrentModificationException` if entities are added during collision callbacks.
  2. **Cached snapshot:** Calls to `getSnapshot()` return the same unmodifiable list until the entity set changes. This **eliminates thousands of short-lived ArrayList allocations per second**, reducing GC pressure — critical for arcade gameplay at 60+ FPS.
  3. **Automatic expiry removal:** Implements `IExpirable` interface to let entities signal self-removal without polling.
  4. **Cascade dispose:** When removing expired entities, immediately calls `dispose()` to free GPU resources (textures, audio samples).

### `IExpirable` & `IFlashable` (Capability Interfaces)
- **Purpose:** Decoupled optional features without forcing dummy implementations
- **Example:** Particles and NPCs are expirable; playerCar is flashable (damage feedback). Unrelated entities don't implement these.
- **Benefit:** Avoids the "interface bloat" problem where one interface has 30 methods and most subclasses ignore most of them.

---

## 1.2 Collision Detection Architecture

### `CollisionManager` (Box2D Observer)
- **Purpose:** Bridge between Box2D physics and application-level collision handling
- **Pattern:** Observer (listeners) + Facade (hides Box2D complexity)
- **Lifecycle:**
  1. Implements `ContactListener` — Box2D calls `beginContact()`, `endContact()`, `postSolve()`
  2. Extracts entity references from Box2D body user-data pointers
  3. Fans out events to all registered `ICollisionListener` implementations
  4. Captures impact force and contact normal for physics feedback (knockback, particle spawning)

### `ICollisionListener` (Extracted as Observer Contract)
- **Purpose:** Game layer can subscribe to collisions without modifying the engine
- **Methods:** `onCollisionBegin()`, `onCollisionEnd()`, `onImpact()` (with force + contact point)
- **Significance:** Multiple listeners can coexist (e.g., one for pedestrian hits, one for boundaries, one for pickups) — **Observer pattern enables extension without modification (OCP).**

---

## 1.3 Physics Abstraction Layer

### `PhysicsWorld` (Box2D Facade)
- **Purpose:** Hide Box2D API from the game layer — engine is the sole Box2D user
- **Key Encapsulations:**
  1. Reuses `BodyDef`, `FixtureDef`, `PolygonShape` to avoid per-body allocations
  2. Converts game-layer `BodyType` enum to Box2D's type system
  3. One-point control of gravity, timestep, solver iterations
  4. `PPM` (pixels-per-meter) conversion centralized: game works in pixels, Box2D in meters

### `PhysicsBody` (Body Wrapper)
- **Purpose:** Friendly facade over Box2D Body
- **Why it matters:** Hides implementation details (velocity getters, setters, position sync)
- **Pattern:** **Adapter** — adapts Box2D's verbose API to a lightweight domain API

### Engine Constants
- `PPM = 100.0f` (100 pixels = 1 Box2D meter)
- Action names: `"left"`, `"right"`, `"up"`, `"down"`, `"action"`
- Globally accessible, preventing magic numbers throughout the codebase

---

## 1.4 Movement Architecture

### `MovementModel` (Strategy Interface)
- **Purpose:** Encapsulate movement algorithm without tying it to input source
- **Methods:**
  - `step(PhysicsBody, float x, float y, float dt)` — apply throttle/steer inputs to physics
  - `onEnterZone()` / `onExitZone()` — optional hooks for zone-aware movement (e.g., slip physics on puddles)
  
### `MovementManager` (Orchestrator)
- **Purpose:** Update all movable entities + physics world in lockstep
- **Significance:** 
  1. Takes a **snapshot** of entities before iteration — prevents mid-loop additions from affecting this frame
  2. Calls `m.move(deltaTime)` on each `IMovable` entity
  3. Synchronously steps the physics world after all entities have applied their forces
  4. **This ordering is critical:** entities move first (applying throttle/steer), then physics resolves collisions and applies friction

### `IMovable` (Single-Method Interface)
- **Purpose:** Minimal contract — any entity that needs per-frame movement implements this
- **Design win:** Players can choose which entities participate in the movement step (e.g., trees don't move, pedestrians do)

### Movement Strategy Pattern (User vs AI Control)
- **`ControlSource`** (Strategy interface) — abstracts how X/Y axes are supplied
  - `UserControlled` — polls keyboard via `ActionInput`
  - `AIControlled` — sinusoidal demo for testing
  - Garden path for custom strategies (turret AI, patrol patterns, etc.)
  
- **`MovableEntity`** — bridges controls → physics:
  - Takes injected `ControlSource` (strategy) + `MovementModel` (algorithm)
  - Decoupled from input device — testing just swaps in a mock `ControlSource`

---

## 1.5 Input Architecture

### `InputDevice` (Hardware Abstraction)
- **Purpose:** Hide keyboard/controller library behind clean interface
- **Key Feature: Context Stack** — enables pause menus / overlays:
  ```
  pushContext()  // suspend game controls, enable pause menu controls
  popContext()   // resume game
  ```
- **Methods:**
  - `addBind(keyCode, action, isJustPressed)` — map key to action in current context
  - `handleInput()` — poll keyboard, trigger registered actions
  - `resetToBase()` — clear all bindings

### `ActionInput` (Query Interface)
- **Purpose:** Let gameplay code ask "is jump held?" or "was jump just pressed?"
- **Pattern:** **Polling** — more game-friendly than event callbacks for continuous actions

### `CommandHistory` (Undo Stack)
- **Purpose:** Record all game-modifying actions for undo
- **Pattern:** **Command Pattern** — each action is a reversible object
- **Methods:** `executeAndRecord(command)`, `undoLast()`
- **Significance:** Even-level violations can be undone (useful for testing, tutorial feedback)

---

## 1.6 Event System

### `EventBus` (Typed Publish-Subscribe)
- **Purpose:** Decouple systems that need to communicate
- **Pattern:** **Observer** (generalized) — type-safe via Java generics
- **Usage:**
  ```java
  eventBus.subscribe(PickupCollectedEvent.class, event -> score += event.points);
  eventBus.publish(new PickupCollectedEvent(10));
  ```
- **Why it matters:** 
  - Systems don't hold direct references to each other
  - New event types don't require modifying the bus
  - Game systems can listen from anywhere, at any time

### `GameEvent` (Marker Interface)
- **Purpose:** Base type for all events — allows type-safe publish/subscribe
- **Implementations created by game layer:** `PickupCollectedEvent`, `FuelDepletedEvent`, `ScoreChangedEvent`, `InstantFailEvent`

---

## 1.7 Scene Management

### `Scene` (Abstract Template Method Base)
- **Purpose:** Define the contract every scene must follow
- **Lifecycle:**
  1. `show()` — initialize managers, load assets, push initial UI
  2. `update(dt)` — update all subsystems
  3. `render(batch)` — draw to screen
  4. `resize(w, h)` — handle window resize, update viewports
  5. `hide()` — cleanup before transition
  
- **Key Principle: Scene Sovereignty** — each scene owns its own:
  - `EntityManager`
  - `CollisionManager`
  - `MovementManager`
  - `PhysicsWorld`
  - This ensures scenes don't interfere with each other

- **Dual Viewports:**
  - **World viewport:** Flexible (FitViewport in menus, ExtendViewport in gameplay) — player sees more when window is enlarged
  - **UI viewport:** Always FitViewport — HUD text is pixel-stable, never distorted

### `SceneManager` (Stack-Based FSM)
- **Purpose:** Manage scene transitions (push/pop stack)
- **Pattern:** **State Machine** — supports overlay scenes (pause menu on top of gameplay)
- **Operations:**
  - `push(scene)` — transition to new scene (old scene remains in memory, hidden)
  - `pop()` — return to previous scene
  - Supports semi-transparent overlays (pause menu with gameplay visible behind)

---

## 1.8 Sound Abstraction

### `SoundDevice` (Interface)
- **Purpose:** Hide LibGDX audio API
- **Methods:** `playSound(name, volume)`, `playMusic(name, loop)`, `stopMusic()`, `dispose()`

### `SoundManager` (Named Sound Registry)
- **Purpose:** Load sounds once, play by name
- **Pattern:** **Registry** — maps names to LibGDX `Sound`/`Music` objects
- **Benefit:** No runtime filename lookups; prevents loading the same audio file twice

---

## 1.9 Game Systems Interface

### `IGameSystem` (Composable Update Loop Contract)
- **Purpose:** Enable systems to be plugged into scenes without modifying the scene class
- **Methods:** `update(float dt)`, `dispose()`
- **Pattern:** **Strategy** — each system encapsulates one responsibility
- **Significance:** Solved SRP violations in original code where scenes had 9+ responsibilities. Now:
  - `BaseGameScene` orchestrates
  - `SpeedScrollController` handles speed simulation
  - `FuelController` handles fuel drain/recharge
  - `AudioController` handles music lifecycle
  - Each system is testable in isolation

---

## Engine Layer: Architectural Principles

| Principle | Implementation |
|-----------|-----------------|
| **Zero Game Knowledge** | No engine class imports from `game/` package |
| **Composition over Inheritance** | Strategy patterns (ControlSource, MovementModel, MovementStrategy) injected at construction |
| **Interface Segregation** | Minimal interfaces (IMovable = 1 method, IGameSystem = 2 methods) |
| **Dependency Inversion** | Depend on abstractions (ControlSource, ICollisionListener, SoundDevice) not concrete types |
| **Single Responsibility** | Each class has one reason to change (EntityManager = entity lifecycle, CollisionManager = collision events) |
| **Cache-Aware Design** | Double-buffered entities + snapshot caching eliminates GC pressure |
| **Escape Hatch Pattern** | Optional interfaces (IExpirable, IFlashable) prevent bloated base class |

---

# PART 2: GAME LAYER ARCHITECTURE

## 2.1 Vehicle Systems

### Vehicle Hierarchy
```
MovableEntity (inherits TextureObject, IMovable)
├── PlayerCar (adds IFlashable for damage feedback)
├── NPCCar (AI-controlled)
└── PoliceCar (IChaseEntity for Level 2)
```

### `PlayerCar` (User-Controlled Vehicle)
- **Features:**
  - Damage flash effect on collision (sinusoidal alpha blending)
  - Injected movement strategy (plays custom control source)
  - Access to underlying `CarMovementModel` for speed queries (UI, sound effects)
  
### `NPCCar` (Traffic Entity)
- **AI:** Injected `NpcDrivingStrategy` + `AIPerceptionService`
- **Perception:** Scans nearby entities (pedestrians, obstacles, other vehicles) using `PerceptionCategory`
- **Lane Awareness:** Spawned to avoid player lane via `ILaneOccupancy` interface
- **Hazard Avoidance:** Slows down when detecting obstacles ahead

### `PoliceCar` (Level 2 Chase Entity)
- **Pattern:** `IChaseEntity` interface — encapsulates pursuit AI
- **Behavior:** Intercepts player, closes distance based on rule violations
- **Aggression:** Ramps up with violation count (faster, more aggressive steering)

---

## 2.2 Pedestrian & Crosswalk Systems

### `Pedestrian` (Walking NPC)
- **Features:**
  - Implements `IPerceivable` (category = PEDESTRIAN)
  - Physics body for collision + knockback
  - Relative Y positioning + scroll sync for level scrolling
  - Hit reaction on collision (ragdoll knockback)

### `CrosswalkZone` (Sensor Entity)
- **Purpose:** Detects player crossing during active pedestrian crossing
- **State Machine:** Tracks player entry, pedestrian activity, violation firing
- **Visual:** Semi-transparent stripe overlay with animation
- **Significance:** Road overlay that doesn't collide — uses sensor fixture (no physical response)

### `StopSign` (Decorative + Semantic Marker)
- **Purpose:** Visual cue for crosswalk; helps NPC path planning avoid crosswalks

### `CrosswalkEncounterSystem` (Complex Orchestrator)
- **Extracted Responsibility:** Manages all crosswalk zones, pedestrian encounters, violation detection
- **Key Features:**
  1. **Encounter Pooling:** Pre-allocates pedestrians with movement + reaction components
  2. **Screen-Based Lifecycle:** Creates encounters as they scroll into view, expires them when off-screen
  3. **Event Publishing:** Fires `ScoreChangedEvent` (reward for avoiding), `InstantFailEvent` (hit pedestrian)
  4. **Command Integration:** Records violations via `BreakRuleCommand` (undo-able)
  5. **Multi-Stage Collision:** Handles enter/cross/exit phases to prevent repeated violations

**Architectural Insight:** This system was **extracted from `Level1Scene`** to satisfy SRP. It's a 200-line orchestrator that could have been 600-line god method. Reusable only in Level 1 (Level 2 has no pedestrians).

---

## 2.3 Traffic & Spawning Architecture

### Factory Pattern for Entity Creation
```
PlayerFactory      → creates player car with injected controls
NPCCarFactory      → creates NPC with AI + perception
PoliceCarFactory   → creates police car with chase AI
CrosswalkFactory   → creates zone + pedestrian encounter bundles
PickupableSpawner  → creates charge pickups
TreeSpawner        → creates scenery
RoadHazardSpawner  → creates puddle/mud motion zones
```

### `NPCCarSpawner` (Smart Traffic Generation)
- **Lane Awareness (`ILaneOccupancy`):** Queries occupied lanes, avoids spawning in player's path
- **Exclusion Zones:** Respects crosswalk regions (Level 1) or hazard zones (Level 2)
- **Spawning Gate:** Can suppress spawning during critical events (active crosswalk on screen)
- **Lifecycle Tracking:** Maintains list of active NPCs, removes off-screen vehicles
- **Perception Wiring:** Each NPC has AI perception tuned for its driving strategy

### `TrafficSpawningSystem` (Facade Orchestrator)
- **Pattern:** **Facade** — coordinates three related spawners:
  - Wraps `NPCCarSpawner`, `PickupableSpawner`, `TreeSpawner`
  - Implements `IGameSystem` interface
  - Accepts per-frame frame state (scroll offset, player position, speed)
  - Can suppress NPC spawning on demand (see: crosswalk encounters)
  
- **Two Constructors:**
  - Full 7-param: for Level 1 (with crosswalk exclusions)
  - 6-param: for Level 2 (no crosswalks) delegates to full constructor with empty list

---

## 2.4 Movement Models & Surface Physics

### `CarMovementModel` (Shared Vehicle Physics)
- **Purpose:** Implements `MovementModel` — handles both player and NPC movement
- **Key Physics Features:**
  1. **Forward Velocity Computation:** Converts throttle input to acceleration/braking
  2. **Lateral Velocity (Steering):** Applies steer input with momentum-based sliding
  3. **Slip Physics:** On slippery surfaces, steering has reduced effect; lateral velocity bleeds off
  4. **Surface Effect Multipliers:** Modifies speed, acceleration, grip, damping based on zone
  5. **Hazard Visual Effects:** Emits particles when in mud/puddles (timer-based)

- **Injected Dependency:** `VehicleProfile` (encapsulates acceleration, max speed, steering response)

### `SurfaceEffect` (Strategy-Like Enum)
- **Surfaces:**
  - `DEFAULT` — normal asphalt
  - `PUDDLE` — aquaplaning (high lateral bleed, slow recovery)
  - `MUD` — low traction, speed limited to 5% of normal
  - `CROSSWALK` — slowed speed (rule compliance) but normal steering
  - `SCHOOL_ZONE` — gentle speed reduction

- **Per-Surface Parameters:**
  - `forwardSpeedMultiplier` — caps max velocity
  - `accelerationMultiplier` — how fast speed builds/falls
  - `lateralGripMultiplier` — steering responsiveness
  - `dampingMultiplier` — physics engine resistance
  - `slippery` boolean — triggers alternate physics path
  - `momentumRetention` — lateral velocity bleed rate
  
- **Design Insight:** Single parametric enum replaces 5+ separate zone classes. Trivial to add new surface (one line in enum).

---

## 2.5 AI Perception & Decision Making

### `IPerceivable` (Entity Capability)
- **Purpose:** Mark entities as detectable by NPC AI
- **Categories:** `PEDESTRIAN`, `VEHICLE`, `OBSTACLE`
- **Implementation:** Replaces instanceof cascades with polymorphic dispatch

### `AIPerceptionService` (Sensor Fusion)
- **Purpose:** Look at nearby entities and extract summarized information for driving decisions
- **Scan Algorithm:**
  1. Iterate `EntityManager.getSnapshot()`
  2. Filter by distance + angle (forward-facing cone)
  3. Classify by `PerceptionCategory`
  4. Return nearest of each type + overall nearest
  
- **Result:** `PerceptionSnapshot` — structured data for decision making
- **Benefit:** NPC driver can make branching decisions without complex collision queries

### `NpcDrivingStrategy` (Decision Engine)
- **Inputs:** Current speed, perception snapshot (obstacles ahead, pedestrians, hazards)
- **Outputs:** Throttle + steer values
- **Logic:** State machine (normal driving → approaches obstacle → swerves → normal)
- **Extensibility:** Custom strategies can be swapped in for different NPC behaviors

---

## 2.6 Rules & Command System

### `RuleManager` (Violation Counter)
- **Single Responsibility:** Track violation count + log types
- **Generic Design:** Records violations without hard-coded violation types
- **Methods:**
  - `recordViolation(String type)` — log named violation
  - `undoLastViolation()` — called by undo command
  - `getPoliceAggression()` — scale 0..1 based on count (for Level 2 police AI)

### `BreakRuleCommand` (Undo-able Violation)
- **Pattern:** **Command Pattern** — encapsulates violation action
- **Execute:** Increments `RuleManager.recorded` counter
- **Undo:** Decrements, removes from log
- **Significance:** Violations are reversible (useful for testing, tutorial feedback)

### `PedestrianHitCommand` (Specific Violation Type)
- Records hit + publishes `InstantFailEvent`
- Undo reverts to pre-hit state

---

## 2.7 Game State Management

### `SpeedScrollController` (Velocity Simulation)
- **Responsibility:** Simulate car speed, scroll offset, braking
- **Key State:**
  - `currentSpeedKmh` — 0 to max (level-specific)
  - `scrollOffset` — accumulated Y position from start
  - `isMoving` — flag for fuel drain calculations
  
- **Extracted from `BaseGameScene`** to satisfy SRP

### `FuelSystem` (Resource Management)
- **State:** Fuel 0.0 (empty) to 1.0 (full)
- **Rules:**
  - Drain per second while vehicle is moving
  - Restore on pickup collection
  - Game over when fuel = 0
  
- **Encapsulation:** `FuelController` wraps `FuelSystem` + publishes `FuelDepletedEvent` on depletion

### `AudioController` (Sound Lifecycle)
- **Manages:**
  - Background music (looping, per-scene)
  - Engine sound (loops while moving)
  - Mute toggle (persist across scenes)
  
- **Extracted from `BaseGameScene`** to centralize audio state

### `DashboardUI` (Head-Up Display)
- **Displays:** Speed, fuel, score, rules broken, time
- **Observer Pattern:** Implements `IDashboardObserver` interface
- **Updates:** Listens to fuel/score changes via event bus
- **Rendering:** Text rendering via LibGDX `BitmapFont`

---

## 2.8 Level-Specific Rendering Systems

### `CrosswalkRenderer` (Line-Art Overlay)
- **Visual:** Diagonal stripes for crosswalk road marking
- **Significance:** Purely visual — doesn't affect gameplay, uses `ShapeRenderer` not textures

### `RainEffectSystem` (Level 2 Weather)
- **Pattern:** Implements `IGameSystem`
- **Logic:** Random diagonal lines scrolling down screen
- **Optimization:** Per-frame generation (cheap) vs pre-computed atlas (heavy memory)

### `PoliceLightSystem` (Chase Intensity Feedback)
- **Visual:** Red/blue oscillating glow at screen edge
- **Intensity:** Scales with distance to police car (0 = caught, 1 = far away)
- **Frequency:** Blink rate increases as police closes in (visceral feedback)
- **SRP:** Purely visual — mutation is `setNormalisedDistance()` no game-state changes

---

## 2.9 Zones & Hazards

### `MotionZone` (Physics Sensor + Tuning)
- **Purpose:** Trigger surface effect changes (puddle, mud) + visual marker
- **Implementation:** Sensor fixture (no physical response) with `SurfaceEffect` tuning
- **Scrolling Support:** Can be kinematic (repositioned each frame) for level scrolling

### `RoadHazard` (Hazard Entity Pooling)
- **Visual:** Sprite or shape
- **Physics:** Sensor (no collision response)
- **Effect:** Triggers surface slipping when player enters

---

## 2.10 Particle & Visual Effects

### `ExplosionSystem` (Static Utility Factory)
- **Pattern:** **Factory** — centralized spawn point for explosion VFX
- **Spawns:**
  1. Particle cloud (using `Particle.spawnExplosion()`)
  2. Explosion overlay sprite (animated, expiry-based)
  3. Audio (explosion sound effect)

### `Particle` (Pool-Friendly Effect Entities)
- **Pattern:** Object pooling via `List<Particle>`
- **Lifetime:** Expires after duration (implements `IExpirable`)
- **Types:** Explosion burst, smoke trail, skid marks

---

## 2.11 Level-Specific Implementations

### `Level1Scene` (Pedestrian Crossing Challenge)
- **Mechanic:** Avoid pedestrians at crosswalks + hit speed limits, collect pickups
- **Composition:** Delegates to `CrosswalkEncounterSystem` + `TrafficSpawningSystem` + `CrosswalkRenderer`
- **End Condition:** Win by reaching level end OR lose by hitting 3+ pedestrians
- **Role:** Pure orchestrator — no game logic, just wiring

### `Level2Scene` (Police Chase Arcade)
- **Mechanic:** Outrun police car while dodging heavy traffic + hazardous puddles/mud
- **Composition:** Delegates to `PoliceCarFactory` + `TrafficSpawningSystem` + `RainEffectSystem` + `RoadHazardSpawner`
- **Police AI:** Chases player with aggression scaled by rule violations
- **Unique Feature:** Road hazards (puddles, mud) modify vehicle physics
- **End Condition:** Win by outrunning to level end OR lose by police collision

### `BaseGameScene` (Abstract Template)
- **Common Setup:**
  1. Create physics world, collision manager, movement manager
  2. Create player car
  3. Create all controllers (speed, fuel, audio)
  4. Wire event bus subscriptions
  5. Initialize rule manager + command history
  
- **Template Methods for Override:**
  - `getMaxSpeed()`, `getAcceleration()`, `getBrakeRate()` — level tuning
  - `initLevelData()` — spawn level-specific entities
  - `updateGame()` — per-frame orchestration (delegates to systems)
  - Lifecycle: `show()` → `update()` → `render()` → `hide()`

---

## 2.12 Collision Handler Hierarchy

```
GameCollisionHandler (Main dispatcher)
├── CrosswalkCollisionHandler → fires violations
├── NPCCarCollisionHandler   → NPC crash handling
├── PedestrianCollisionHandler → player-ped hits → instant fail
├── PickupCollisionHandler   → fuel recharges
├── BoundaryCollisionHandler → off-road handling
├── NPCPedestrianCollisionHandler → traffic simulation
└── ZoneCollisionHandler     → motion zone entry/exit
```

### Each Handler
- **Pattern:** Strategy + Facade — encapsulates one collision type's logic
- **Interface:** `canHandle(a, b)` + `handleBegin()` + `handleEnd()`
- **DIP:** References listeners (`TrafficViolationListener`) not concrete scenes

---

## Game Layer: Architectural Principles

| Principle | Implementation |
|-----------|-----------------|
| **SRP Extraction** | 8 new system classes extracted from monolithic scenes |
| **Composition over Inheritance** | Systems injected into scenes; scenes wire together at runtime |
| **Strategy Pattern** | Movement models, driving strategies, surface effects — all pluggable |
| **Observer Pattern** | EventBus for decoupled system communication |
| **Factory Pattern** | Centralized entity creation (CrosswalkFactory, NPCCarSpawner, etc.) |
| **Command Pattern** | Violations are undo-able via BreakRuleCommand |
| **State Machine** | Scene stack (FSM), CrosswalkEncounterSystem state transitions |
| **Facade Pattern** | TrafficSpawningSystem wraps 3 spawners, BaseGameScene orchestrates systems |
| **Interface Segregation** | IPerceivable, IChaseEntity, IDashboardObserver — minimal contracts |
| **DIP** | Depend on abstractions (IGameSystem, ICollisionListener) not concrete types |

---

# PART 3: Cross-Cutting Concerns & Notable Patterns

## 3.1 Object Pooling & Memory Management

1. **Entity Snapshot Caching:** EntityManager caches unmodifiable copy (only rebuilt when list changes)
2. **Physics Object Reuse:** PhysicsWorld reuses BodyDef, FixtureDef, PolygonShape across all bodies
3. **Particle Pooling:** Explosions spawn pre-allocated particle objects
4. **Texture Flyweight:** TextureObject uses shared static texture cache (loaded once, reused)

**Rationale:** Arcade gameplay at 60+ FPS cannot afford GC spikes. Pooling + snapshot caching keep allocations minimal.

---

## 3.2 Double Dispatch (Visitor-Like Pattern)

**Collision detection** uses implicit double dispatch:
```
GameCollisionHandler.onCollisionBegin(EntityA, EntityB)
  → if (A is PlayerCar && B is Pedestrian) → handlePedestrianHit()
  → if (A is Pedestrian && B is PlayerCar) → handlePedestrianHit() (same)
  → if (A is NPCCar && B is Boundary) → handleBoundaryCollide()
  ...
```

**Why not explicit Visitor pattern?** Simpler, fewer classes. Box2D can fire collisions in either order; explicit dispatch handles symmetry.

---

## 3.3 Data-Driven Tuning

**Game Constants** centralized in `GameConstants.java`:
```java
L1_MAX_SPEED = 800f;
L1_ACCELERATION = 250f;
L1_BRAKE_RATE = 300f;
PEDESTRIAN_WIDTH = 40f;
CROSSWALK_ZONE_HEIGHT = 80f;
```

**Benefit:** Designers can tune difficulty without recompiling code. Surface effects are also data-driven (`SurfaceEffect` enum).

---

## 3.4 Scroll Offset Virtualization

**Level Design:** Levels are longer than screen height. Scrolling is simulated (camera doesn't move; world moves down).

```
scrollOffset += speed * dt
playerCar.y = VIRTUAL_HEIGHT / 2  (fixed on screen)
entity.screenY = entity.relativeY + scrollOffset
```

**Benefit:** Infinite level lengths without loading/unloading chunks. Entities have two Y positions (world Y, screen Y) updated each frame.

---

## 3.5 Sensor Fixtures vs Colliders

- **Sensor Zones:** Crosswalks, pickups, road hazards use **sensor fixtures** (trigger collisions but don't block movement)
- **Physical Objects:** Cars, boundaries use **physical fixtures** (block movement, respond to forces)
- **Mixed:** Pedestrians are physical (have mass, respond to knockback) but trigger "hit" events (not obstacle events)

**Design Win:** Separates "did I touch this?" (sensor) from "did I collide?" (physics).

---

## 3.6 Observer Pattern Variants

1. **CollisionManager:** Concrete-to-interface (notifies ICollisionListener implementations)
2. **EventBus:** Interface-to-interface (publishes GameEvent subtypes, any subscriber can listen)
3. **DashboardUI:** Direct listener interface (IDashboardObserver for speed/fuel updates)

---

# PART 4: Known Limitations & Design Tradeoffs

## 4.1 Box2D Complexity Hidden

- **Tradeoff:** PhysicsWorld hides Box2D details, but PPM conversion is manual in many places
- **Cost:** Some numeric errors possible if PPM isn't consistently applied
- **Benefit:** Engine code is simpler; game code is physics-aware but not Box2D-aware

## 4.2 No Asset Manager Abstraction

- **Current:** Textures loaded via LibGDX `Texture`, sounds via `Sound`/`Music`
- **Tradeoff:** No unified asset pipelines, no streaming
- **Acceptable for:** Small arcade game (fits in memory)

## 4.3 Scene Sovereignty = Memory Cost

- **Design:** Each scene owns full physics world, entity manager, collision system
- **Cost:** Level transitions require disposal + GC cleanup
- **Benefit:** Zero interference between scenes, trivial to test scenes in isolation

## 4.4 No Pooled Object System

- **Current:** Pedestrians, particles, pickups are created fresh each spawn
- **Acceptable for:** Arcade rates (max ~50 entities on screen)
- **Would need if:** 1000s of simultaneous entities

## 4.5 AI Perception is Linear Scan

- **Current:** AIPerceptionService scans full entity list each frame per NPC car
- **Cost:** O(n*m) where n = NPCs, m = total entities
- **Acceptable for:** Max ~20 NPCs on screen
- **Scalability:** Would need spatial partitioning (quadtree) for massive spawns

---

# PART 5: Extension Points & Future Capabilities

The architecture naturally supports:

1. **New Surfaces:** Add to `SurfaceEffect` enum
2. **New Violation Types:** Create new `ICommand` subclass, publish new `GameEvent` type
3. **New Collision Handlers:** Implement listener interface, register with `GameCollisionHandler`
4. **New Levels:** Extend `BaseGameScene`, override config hooks
5. **Custom Controls:** New `ControlSource` implementation for console gamepad, touch input, etc.
6. **Mod-Friendly Audio:** Add/replace sounds without recompiling (loaded from external files)
7. **Custom Movement Models:** Implement `MovementModel` for hovercraft, jetpack, etc.
8. **NPC Personalities:** Implement `NpcDrivingStrategy` for defensive, aggressive, cautious drivers

---

# PART 6: Summary Table

## Engine Layer (9 Systems)

| System | Purpose | Key Pattern | Significance |
|--------|---------|-------------|--------------|
| **Entity Management** | Lifecycle, rendering | Double-buffered, snapshot caching | Zero GC pressure |
| **Collision Detection** | Box2D→application | Observer, Facade | Extensible via listeners |
| **Physics Abstraction** | Hide Box2D | Adapter, Facade | Engine-only Box2D user |
| **Movement Orchestration** | Synchronized entity + physics updates | Update ordering | Collision-safe movement |
| **Input Handling** | Keyboard abstraction | Context stack, Strategy | Pause menu support |
| **Command History** | Undo stack | Command pattern | Reversible actions |
| **Event Bus** | Decoupled communication | Pub-Sub, Type-safe | Extensible messaging |
| **Scene Management** | Lifecycle + transitions | Stack FSM, Template Method | Overlay support |
| **Sound System** | Audio abstraction | Registry, Facade | LibGDX-independent |

## Game Layer (12 Systems)

| System | Purpose | Key Pattern | Significance |
|--------|---------|-------------|--------------|
| **Pedestrian Crossing** | Encounter lifecycle | Complex orchestration | Level 1 mechanic |
| **Traffic Spawning** | Smart entity generation | Facade, Factory | Lane awareness |
| **Vehicle Physics** | Movement + surface effects | Strategy, Parametric | Shared player/NPC model |
| **AI Perception** | Sensor fusion | Service layer | NPC decision-making |
| **Rules Manager** | Violation tracking | Generic counter | Open/closed principle |
| **State Controllers** | Speed, fuel, audio | Single-responsibility | Extracted from chaos |
| **Collision Handlers** | Type-specific responses | Strategy, Facade | Dispatcher pattern |
| **Level Design** | Scene composition | Template Method | Reusable base + overrides |
| **Zone Physics** | Surface effects | Parametric enum | Data-driven tuning |
| **Visual Effects** | Particles, explosions | Factory, Pooling | Feedback systems |
| **Level-Specific Rendering** | Weather, police lights | Custom controllers | Atmospheric depth |
| **HUD Management** | Dashboard display | Observer, Listener | Event-driven updates |

---

# Conclusion

This project demonstrates a **mature separation of concerns**:

- **Engine layer** is generic, testable, and reusable (could underpin any 2D physics game)
- **Game layer** is domain-specific, composed of small, focused systems
- **Zero coupling** from engine to game ensures the engine remains pure
- **Dependency Inversion** throughout (depend on abstractions, not concretions)
- **Performance-conscious design** (pooling, snapshot caching, asset reuse)
- **Extensible architecture** (new surfaces, violations, AI strategies via minimal interfaces)

The codebase is an exemplar of **SOLID principles** applied to game development.
