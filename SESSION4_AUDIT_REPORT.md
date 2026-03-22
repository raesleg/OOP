# Session 4 — SRP / OCP / DIP Overhaul + Crash Fix Report

**Date:** Session 4  
**Project:** INF1009 LibGDX + Box2D Arcade Car Game  
**Scope:** System-wide compliance audit, crash fix, and targeted refactors

---

## 1. Critical Crash Fix

| File                         | Line | Issue                                                                                                                                                                        | Resolution                                                                                    |
| ---------------------------- | ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| `TrafficSpawningSystem.java` | 49   | 6-param constructor passed `null` for `crosswalkExclusions` → `PickupableSpawner.spawnPickup()` threw `NullPointerException` at line 75 when iterating `crosswalkExclusions` | Changed `null` to `List.of()`                                                                 |
| `PickupableSpawner.java`     | 50   | 6-param constructor blindly stored the parameter without null-guard                                                                                                          | Added defensive null-guard: `(crosswalkExclusions != null) ? crosswalkExclusions : List.of()` |

**Root cause:** Level 2 has no crosswalks, so it used the 6-param `TrafficSpawningSystem` constructor (omitting crosswalk exclusions). That constructor delegated to the 7-param version passing `null`. The null flowed through to `PickupableSpawner`, which iterated it unconditionally — causing the `NullPointerException` every time a pickup spawn was attempted.

---

## 2. PauseScene — Scene2D Rewrite

**Before:** Raw `ShapeRenderer` for overlay/panel, manual `BitmapFont` + `GlyphLayout` positioning, raw `Gdx.gl.glEnable(GL20.GL_BLEND)` / `glBlendFunc` / `glDisable` calls, `Matrix4` projection matrix management.

**After:** Scene2D `Stage` + `Table` layout. Overlay is a `Table` with tinted `TextureRegionDrawable` background. Menu items are `Label` widgets updated dynamically via `refreshLabels()`. No raw GL calls. Proper `resize()` + `dispose()` lifecycle.

**Preserved behaviour:**

- Transparent overlay via `setTransparent(true)`
- WASD / arrow navigation, Enter to select, ESC to resume, M to mute
- A/D volume adjustment with live percentage display
- Sound effects on every interaction

---

## 3. ILevelEndCondition — OCP Extraction

**Before:** `BaseGameScene.checkLevelEnd()` contained 4 hardcoded win/lose conditions in a monolithic if-chain. Adding new universal conditions required modifying the base class.

**After:**

- Created `ILevelEndCondition` functional interface (`boolean evaluate()`)
- `BaseGameScene` maintains `List<ILevelEndCondition>` — iterated in `checkLevelEnd()`
- Four built-in conditions registered via method references: `checkWinCondition`, `checkCrashExplosion`, `checkInstantFail`, `checkSubclassGameOver`
- Subclasses can add custom conditions via `addEndCondition()` in `initLevelData()` — no modification of the base class needed

---

## 4. GameConstants — Mutable Array Fix + New Constants

| Change                     | Detail                                                                    |
| -------------------------- | ------------------------------------------------------------------------- |
| `L1_CROSSING_POSITIONS`    | Changed from mutable `float[]` to immutable `List<Float>` via `List.of()` |
| `SCORE_RATE_PER_SECOND`    | New constant (was magic number `10f` in `BaseGameScene`)                  |
| `NPC_PEDESTRIAN_STOP_DIST` | New constant (was `90f` in `NpcDrivingStrategy`)                          |
| `NPC_VEHICLE_SLOW_DIST`    | New constant (was `110f` in `NpcDrivingStrategy`)                         |
| `NPC_OBSTACLE_SLOW_DIST`   | New constant (was `85f` in `NpcDrivingStrategy`)                          |
| `NPC_SLOW_SPEED`           | New constant (was `0.25f` in `NpcDrivingStrategy`)                        |
| `NPC_OBSTACLE_SPEED`       | New constant (was `0.3f` in `NpcDrivingStrategy`)                         |
| `NPC_DEFAULT_SPEED`        | New constant (was `0.45f` in `NpcDrivingStrategy`)                        |

**Consumer updates:**

- `CrosswalkEncounterSystem.initCrosswalks()` — parameter changed from `float[]` to `List<Float>`
- `Level1Scene` — no code changes needed (for-each autoboxing)
- `BaseGameScene` — `deltaTime * 10f` → `deltaTime * GameConstants.SCORE_RATE_PER_SECOND`
- `NpcDrivingStrategy` — all 6 magic numbers replaced with `GameConstants.*`

---

## 5. Font Dispose Leaks Fixed

| File                    | Font        | Issue                                                        | Fix                                                                                                   |
| ----------------------- | ----------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------- |
| `ResultsScene.java`     | `smallFont` | Created as local variable inside an if-block; never disposed | Promoted to class field; disposed in `dispose()` with null-guard (only created when violations exist) |
| `LevelSelectScene.java` | `subFont`   | Created as local variable in `show()`; never disposed        | Promoted to class field; added `subFont.dispose()` to `dispose()`                                     |

---

## 6. NPCCarSpawner — Constant Deduplication

Removed local `NPC_WIDTH = 70f` and `NPC_HEIGHT = 120f` constants. All references now use `GameConstants.NPC_WIDTH` / `GameConstants.NPC_HEIGHT` (single source of truth).

---

## 7. Level2Scene — Dispose Cleanup

Added `policeFactory = null` and `rainEffect = null` in `disposeLevelData()` to prevent stale references after scene disposal.

---

## 8. NpcDrivingStrategy — Downcast Status

The `instanceof NPCCar` downcast was **already resolved** in Session 3 when the class was refactored to use `AIPerceptionService` and `SensorComponent`. No action required this session.

---

## 10-Point Compliance Check (Post-Fix)

| #   | Check                             | Status                                                   |
| --- | --------------------------------- | -------------------------------------------------------- |
| 1   | No `instanceof` cascades          | **PASS** — none found                                    |
| 2   | No raw OpenGL in scenes           | **PASS** — PauseScene rewritten with Scene2D             |
| 3   | No `Gdx.input` in entities        | **PASS** — input handled via engine `IOManager`          |
| 4   | No Box2D imports in `game/` layer | **PASS** — all via engine `PhysicsWorld` / `PhysicsBody` |
| 5   | No `System.out.println`           | **PASS** — all logging via `Gdx.app.log`                 |
| 6   | No mutable public arrays          | **PASS** — `L1_CROSSING_POSITIONS` is now `List.of(...)` |
| 7   | No font/texture dispose leaks     | **PASS** — `smallFont` and `subFont` now disposed        |
| 8   | No magic numbers in strategies    | **PASS** — all in `GameConstants`                        |
| 9   | No constant duplication           | **PASS** — `NPCCarSpawner` uses `GameConstants.NPC_*`    |
| 10  | OCP on level-end conditions       | **PASS** — `ILevelEndCondition` strategy pattern         |

---

## Files Modified This Session

| File                            | Change Type                                        |
| ------------------------------- | -------------------------------------------------- |
| `TrafficSpawningSystem.java`    | Bug fix (null → List.of)                           |
| `PickupableSpawner.java`        | Defensive null-guard                               |
| `PauseScene.java`               | Full Scene2D rewrite                               |
| `ILevelEndCondition.java`       | **NEW** — OCP interface                            |
| `BaseGameScene.java`            | OCP refactor (checkLevelEnd) + score rate constant |
| `GameConstants.java`            | Immutable list + 7 new constants                   |
| `CrosswalkEncounterSystem.java` | Signature change (float[] → List&lt;Float&gt;)     |
| `ResultsScene.java`             | Font dispose leak fix                              |
| `LevelSelectScene.java`         | Font dispose leak fix                              |
| `NpcDrivingStrategy.java`       | Magic numbers → GameConstants                      |
| `NPCCarSpawner.java`            | Constant deduplication                             |
| `Level2Scene.java`              | Dispose cleanup                                    |

**Total: 12 files touched, 1 new file created, 0 violations remaining.**
