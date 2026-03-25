package io.github.raesleg.game;

import java.util.List;

/**
 * GameConstants — Centralised repository for all game-layer magic numbers,
 * score values, spawn intervals, audio paths, and dimensional constants.
 * <p>
 * <b>Clean Code:</b> Eliminates scattered magic numbers and strings from
 * scenes, spawners, and listeners. Any gameplay tuning knob lives here.
 * <p>
 * <b>OCP:</b> New constants are added without modifying existing classes.
 */
public final class GameConstants {

    private GameConstants() {
    }

    /*
     * ════════════════════════════════════════════════════════════
     * Road geometry (pixels, 1280×720 virtual coords)
     * ════════════════════════════════════════════════════════════
     */

    public static final float ROAD_LEFT = 340f;
    public static final float ROAD_RIGHT = 940f;
    public static final float ROAD_WIDTH = ROAD_RIGHT - ROAD_LEFT; // 600

    /*
     * ════════════════════════════════════════════════════════════
     * Scoring
     * ════════════════════════════════════════════════════════════
     */

    public static final int SCORE_PICKUP = 50;
    public static final int SCORE_CROSSWALK_SAFE = 200;
    public static final int SCORE_PENALTY = -100;

    /** Passive score accumulation rate while driving (points per second). */
    public static final float SCORE_RATE_PER_SECOND = 10f;

    /*
     * ════════════════════════════════════════════════════════════
     * Wanted stars
     * ════════════════════════════════════════════════════════════
     */

    public static final int MAX_WANTED_STARS = 5;

    /** Number of crash-into-NPC events before explosive game-over. */
    public static final int CRASH_EXPLOSION_THRESHOLD = 3;

    /*
     * ════════════════════════════════════════════════════════════
     * Player car dimensions (pixels)
     * ════════════════════════════════════════════════════════════
     */

    public static final float PLAYER_CAR_WIDTH = 100f;
    public static final float PLAYER_CAR_HEIGHT = 185f;
    public static final float PLAYER_CAR_START_Y = 100f;

    /*
     * ════════════════════════════════════════════════════════════
     * NPC car dimensions (pixels)
     * ════════════════════════════════════════════════════════════
     */

    public static final float NPC_WIDTH = 70f;
    public static final float NPC_HEIGHT = 120f;

    /*
     * ════════════════════════════════════════════════════════════
     * Police car dimensions (pixels)
     * ════════════════════════════════════════════════════════════
     */

    public static final float POLICE_WIDTH = 80f;
    public static final float POLICE_HEIGHT = 140f;
    public static final float POLICE_START_Y = 20f;

    /*
     * ════════════════════════════════════════════════════════════
     * Level 1 parameters
     * ════════════════════════════════════════════════════════════
     */

    public static final float L1_LEVEL_LENGTH = 50000f;
    public static final float L1_MAX_SPEED = 60f;
    public static final float L1_ACCELERATION = 95f;
    public static final float L1_BRAKE_RATE = 160f;
    public static final float L1_MAX_SCROLL = 850f;
    public static final float L1_NPC_SPAWN_SEC = 2.0f;
    public static final float L1_CROSSWALK_HEIGHT = 80f;
    public static final List<Float> L1_CROSSING_POSITIONS = List.of(5000f, 15000f, 28000f, 40000f);

    /*
     * ════════════════════════════════════════════════════════════
     * Level 2 parameters
     * ════════════════════════════════════════════════════════════
     */

    public static final float L2_LEVEL_LENGTH = 80000f;
    public static final float L2_MAX_SPEED = 90f;
    public static final float L2_ACCELERATION = 50f;
    public static final float L2_BRAKE_RATE = 70f;
    public static final float L2_MAX_SCROLL = 1050f;
    public static final float L2_NPC_SPAWN_SEC = 1.4f;

    /** Puddle hazard spawn interval (seconds). */
    public static final float L2_PUDDLE_INTERVAL = 3.5f;
    /** Mud hazard spawn interval (seconds). */
    public static final float L2_MUD_INTERVAL = 6.0f;

    /** Pickup (battery) spawn interval in Level 2 (seconds). */
    public static final float L2_PICKUP_SPAWN_SEC = 5.0f;

    /*
     * ════════════════════════════════════════════════════════════
     * Fuel
     * ════════════════════════════════════════════════════════════
     */

    public static final float FUEL_DRAIN_RATE = 0.033f;
    public static final float FUEL_RECHARGE_AMOUNT = 0.25f;

    /*
     * ════════════════════════════════════════════════════════════
     * Rain effect (Level 2)
     * ════════════════════════════════════════════════════════════
     */

    public static final int RAIN_DROP_COUNT = 150;

    /*
     * ════════════════════════════════════════════════════════════
     * Siren distance (Level 2 police)
     * ════════════════════════════════════════════════════════════
     */

    public static final float POLICE_MAX_DISTANCE = 600f;
    public static final float SIREN_MIN_VOLUME = 0.05f;

    /*
     * ════════════════════════════════════════════════════════════
     * Violation stars per type
     * ════════════════════════════════════════════════════════════
     */

    public static final int CROSSWALK_VIOLATION_STARS = 2;
    public static final int TRAFFIC_CRASH_STARS = 1;

    /*
     * ════════════════════════════════════════════════════════════
     * Audio asset paths
     * ════════════════════════════════════════════════════════════
     */

    public static final String SFX_BOUNDARY_HIT = "crash_sound.wav";
    public static final String SFX_CRASH = "crash_sound.wav";
    public static final String SFX_PEDESTRIAN_HIT = "pedestrain_hit.wav";
    public static final String SFX_SCREAM = "scream.mp3";
    public static final String SFX_POLICE_SIREN = "policesiren.mp3";
    public static final String SFX_RAIN = "rainsound.wav";
    public static final String SFX_DRIVE = "car_sound.wav";
    public static final String SFX_EXPLOSION = "crash_sound.wav";
    public static final String SFX_EXPLOSION_BIG = "explosion.wav";
    public static final String SFX_REWARD = "rewardsound.mp3";
    public static final String SFX_NEGATIVE = "negativesound.mp3";
    public static final String SFX_GAMEOVER = "gameover_sound.wav";
    public static final String SFX_WIN = "winning_sound.wav";
    public static final String SFX_MENU = "uiMenu_sound.wav";
    public static final String SFX_SELECTED = "uiSelected_sound.wav";
    public static final String SFX_HIT_SOUND = "hit_sound.wav";

    /** Default BGM for both levels. */
    public static final String BGM_DEFAULT = "bgm.mp3";

    /*
     * ════════════════════════════════════════════════════════════
     * NPC perception thresholds (used by NpcDrivingStrategy)
     * ════════════════════════════════════════════════════════════
     */

    public static final float NPC_PEDESTRIAN_STOP_DIST = 90f;
    public static final float NPC_VEHICLE_SLOW_DIST = 110f;
    public static final float NPC_OBSTACLE_SLOW_DIST = 85f;
    public static final float NPC_SLOW_SPEED = 0.25f;
    public static final float NPC_OBSTACLE_SPEED = 0.3f;
    public static final float NPC_DEFAULT_SPEED = 0.45f;

    /*
     * ════════════════════════════════════════════════════════════
     * Explosion game-over delay
     * ════════════════════════════════════════════════════════════
     */

    /** Seconds between triggering the explosion and showing results. */
    public static final float EXPLOSION_DELAY = 1.5f;

    /*
     * ════════════════════════════════════════════════════════════
     * Camera
     * ════════════════════════════════════════════════════════════
     */

    /** Pixels the camera shifts upward so the player sees further ahead. */
    public static final float CAMERA_LOOK_AHEAD = 120f;

    /** Camera zoom factor — values > 1 zoom out to show more world space. */
    public static final float CAMERA_ZOOM = 1.3f;

    /** Level 2 uses a wider zoom so the player can see the police car. */
    public static final float L2_CAMERA_ZOOM = 1.6f;

    /*
     * ════════════════════════════════════════════════════════════
     * Dynamic player Y (Level 2 vertical movement)
     * ════════════════════════════════════════════════════════════
     */

    /** Lowest on-screen Y the player car can reach (braking / idle). */
    public static final float PLAYER_MIN_Y = 80f;
    /** Highest on-screen Y the player car can reach. */
    public static final float PLAYER_MAX_Y = 180f;

    /*
     * ════════════════════════════════════════════════════════════
     * Police chase movement
     * ════════════════════════════════════════════════════════════
     */

    /** Max pixel distance between police and player at 0 stars. */
    public static final float POLICE_STAR_MAX_DISTANCE = 300f;
    /** Lerp speed for police approaching its star-based target Y. */
    public static final float POLICE_LERP_SPEED = 3f;
    /** Horizontal lane-tracking lerp speed (units/sec). */
    public static final float POLICE_LANE_TRACK_SPEED = 4f;
    /** Police speed-awareness factor — how much low speed shrinks the gap. */
    public static final float POLICE_SPEED_FACTOR = 0.55f;
    /** Police siren flash interval (seconds). */
    public static final float POLICE_FLASH_INTERVAL = 0.15f;

    /*
     * ════════════════════════════════════════════════════════════
     * Crosswalk encounter (Level 1)
     * ════════════════════════════════════════════════════════════
     */

    /** Crosswalk zone half-height used for the sensor body (pixels). */
    public static final float CROSSWALK_ZONE_HEIGHT = 80f;
    /** Horizontal crossing speed of pedestrians (pixels/sec). */
    public static final float CROSSING_SPEED = 160f;

    /*
     * ════════════════════════════════════════════════════════════
     * Speed scroll passive deceleration
     * ════════════════════════════════════════════════════════════
     */

    /** Speed lost per second when the player releases UP key. */
    public static final float PASSIVE_DECEL = 18f;

    /*
     * ════════════════════════════════════════════════════════════
     * Level 2 crash speed penalty
     * ════════════════════════════════════════════════════════════
     */

    /** Fraction of current speed retained after a crash in Level 2. */
    public static final float L2_CRASH_SPEED_PENALTY = 0.5f;

    /*
     * ════════════════════════════════════════════════════════════
     * Boundary wall thickness (metres)
     * ════════════════════════════════════════════════════════════
     */

    /** Half-thickness of road boundary walls in physics metres. */
    public static final float BOUNDARY_WALL_THICKNESS = 0.5f;

    /*
     * ════════════════════════════════════════════════════════════
     * Pedestrian dimensions (pixels)
     * ════════════════════════════════════════════════════════════
     */

    public static final float PEDESTRIAN_WIDTH = 80f;
    public static final float PEDESTRIAN_HEIGHT = 80f;
    /** Hitbox scaling factor relative to sprite size. */
    public static final float PEDESTRIAN_HITBOX_SCALE = 0.4f;

    /*
     * ════════════════════════════════════════════════════════════
     * Level 2 player vertical movement
     * ════════════════════════════════════════════════════════════
     */

    /** Vertical movement speed from player input in Level 2 (px/sec). */
    public static final float L2_PLAYER_VERTICAL_SPEED = 80f;

    /*
     * ════════════════════════════════════════════════════════════
     * Spawn screen heights (account for camera zoom + look-ahead)
     * ════════════════════════════════════════════════════════════
     */

    /** Effective visible-top Y for Level 1 spawning (zoom 1.3). */
    public static final float SPAWN_SCREEN_HEIGHT = 960f;

    /** Effective visible-top Y for Level 2 spawning (zoom 1.6). */
    public static final float L2_SPAWN_SCREEN_HEIGHT = 1080f;

    // Surface effect parameters for different terrain types (Level 2)
    public static final float REVERSE_MAX_SPEED = 3f;
    public static final float REVERSE_ACCEL = 8f;
    public static final float EFFECT_INTERVAL = 0.08f;
}
