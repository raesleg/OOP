package io.github.raesleg.engine.movement;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * IOManager — Owns all key bindings and exposes only semantic-intent methods.
 *
 * <h3>Design Rules (SOLID / Encapsulation)</h3>
 * <ul>
 * <li>Scenes must <b>NEVER</b> import {@code com.badlogic.gdx.Input}.
 * They call semantic methods such as {@link #isPauseRequested()}.</li>
 * <li>Key codes live in a shared {@link InputConfig} so a rebind in one
 * scene (e.g.&nbsp;an Options menu) applies everywhere.</li>
 * <li>All public query methods delegate to the config's maps —
 * no key code is ever hard-coded inside a getter.</li>
 * </ul>
 *
 * <h3>Thread of Ownership</h3>
 * Each Scene still instantiates its own {@code IOManager} (Scene Sovereignty),
 * but every instance shares the same static {@link InputConfig} by default.
 */
public class IOManager {

    /*
     * ══════════════════════════════════════════════════════════════════
     * Semantic action names — use these constants, never raw strings.
     * ══════════════════════════════════════════════════════════════════
     */
    public static final String UP = "UP";
    public static final String DOWN = "DOWN";
    public static final String LEFT = "LEFT";
    public static final String RIGHT = "RIGHT";
    public static final String ACTION = "ACTION";
    public static final String PAUSE = "PAUSE";
    public static final String CONFIRM = "CONFIRM";

    /*
     * ══════════════════════════════════════════════════════════════════
     * InputConfig — shared, static key-binding configuration.
     * ══════════════════════════════════════════════════════════════════
     */

    /**
     * Holds the primary and alternate key-code maps.
     * A single instance is shared by every {@code IOManager} so that
     * rebinding a key in an Options scene is reflected game-wide.
     */
    public static final class InputConfig {

        private final Map<String, Integer> primary;
        private final Map<String, Integer> alt;

        private InputConfig() {
            primary = new HashMap<>();
            alt = new HashMap<>();
            defaults();
        }

        /** Resets every binding to the factory defaults. */
        public void defaults() {
            primary.clear();
            alt.clear();

            primary.put(UP, Input.Keys.W);
            primary.put(DOWN, Input.Keys.S);
            primary.put(LEFT, Input.Keys.A);
            primary.put(RIGHT, Input.Keys.D);
            primary.put(ACTION, Input.Keys.SPACE);
            primary.put(PAUSE, Input.Keys.ESCAPE);
            primary.put(CONFIRM, Input.Keys.ENTER);

            alt.put(UP, Input.Keys.UP);
            alt.put(DOWN, Input.Keys.DOWN);
            alt.put(LEFT, Input.Keys.LEFT);
            alt.put(RIGHT, Input.Keys.RIGHT);
            alt.put(CONFIRM, Input.Keys.NUMPAD_ENTER);
        }

        /** Changes the primary key for an action. Applies game-wide. */
        public void rebind(String action, int keyCode) {
            primary.put(action, keyCode);
        }

        /** Changes the alternate key for an action. Applies game-wide. */
        public void rebindAlt(String action, int keyCode) {
            alt.put(action, keyCode);
        }

        /* package-private accessors used by IOManager */
        Integer getPrimary(String action) {
            return primary.get(action);
        }

        Integer getAlt(String action) {
            return alt.get(action);
        }

        /** Returns every bound key code (primary + alt) for frame tracking. */
        Set<Integer> allKeyCodes() {
            Set<Integer> codes = new HashSet<>(primary.values());
            codes.addAll(alt.values());
            return codes;
        }
    }

    /* ── The single shared config instance ── */
    private static final InputConfig GLOBAL_CONFIG = new InputConfig();

    /** Returns the global InputConfig (for rebinding from an Options scene). */
    public static InputConfig globalConfig() {
        return GLOBAL_CONFIG;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Per-instance frame state
     * ══════════════════════════════════════════════════════════════════
     */
    private final InputConfig config;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> keysDown = new HashSet<>();
    private boolean leftClick;
    private boolean rightClick;

    /* ── Constructors ── */

    /** Creates an IOManager backed by the global shared InputConfig. */
    public IOManager() {
        this(GLOBAL_CONFIG);
    }

    /**
     * Creates an IOManager backed by a custom InputConfig.
     * Useful for unit testing or an isolated scene that must not
     * share its bindings.
     */
    public IOManager(InputConfig config) {
        this.config = config;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Frame update — call once per frame BEFORE querying
     * ══════════════════════════════════════════════════════════════════
     */

    public void update() {
        pressedKeys.clear();

        for (int key : config.allKeyCodes()) {
            trackKey(key);
        }

        leftClick = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        rightClick = Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
    }

    private void trackKey(int key) {
        boolean pressedNow = Gdx.input.isKeyPressed(key);
        if (pressedNow) {
            if (!keysDown.contains(key)) {
                pressedKeys.add(key);
            }
            keysDown.add(key);
        } else {
            keysDown.remove(key);
        }
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Semantic "just-pressed" queries (one-shot, true for one frame)
     * ══════════════════════════════════════════════════════════════════
     */

    public boolean isPauseRequested() {
        return justPressed(PAUSE);
    }

    public boolean isConfirmRequested() {
        return justPressed(CONFIRM);
    }

    public boolean isUpJustPressed() {
        return justPressed(UP);
    }

    public boolean isDownJustPressed() {
        return justPressed(DOWN);
    }

    public boolean isLeftJustPressed() {
        return justPressed(LEFT);
    }

    public boolean isRightJustPressed() {
        return justPressed(RIGHT);
    }

    public boolean isActionJustPressed() {
        return justPressed(ACTION);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Semantic "held" queries (continuous, true every frame key is down)
     * ══════════════════════════════════════════════════════════════════
     */

    public boolean isUpHeld() {
        return held(UP);
    }

    public boolean isDownHeld() {
        return held(DOWN);
    }

    public boolean isLeftHeld() {
        return held(LEFT);
    }

    public boolean isRightHeld() {
        return held(RIGHT);
    }

    public boolean isActionHeld() {
        return held(ACTION);
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Mouse queries
     * ══════════════════════════════════════════════════════════════════
     */

    public boolean isLeftClick() {
        return leftClick;
    }

    public boolean isRightClick() {
        return rightClick;
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Dynamic UI query — returns human-readable key name for an action
     * ══════════════════════════════════════════════════════════════════
     */

    /**
     * Returns the human-readable name of the primary key bound to the
     * given action (e.g. "Escape", "W", "Space").
     * <p>
     * Use this in HUD / UI text so prompts stay accurate when keys are
     * rebound at runtime.
     *
     * @param action one of the semantic action constants (e.g. {@link #PAUSE})
     * @return display name of the bound key, or "???" if unbound
     */
    public String getKeyName(String action) {
        Integer keyCode = config.getPrimary(action);
        if (keyCode != null) {
            return Input.Keys.toString(keyCode);
        }
        return "???";
    }

    /*
     * ══════════════════════════════════════════════════════════════════
     * Internal helpers — delegate entirely to the config maps
     * ══════════════════════════════════════════════════════════════════
     */

    private boolean justPressed(String action) {
        Integer p = config.getPrimary(action);
        Integer a = config.getAlt(action);
        return (p != null && pressedKeys.contains(p))
                || (a != null && pressedKeys.contains(a));
    }

    private boolean held(String action) {
        Integer p = config.getPrimary(action);
        Integer a = config.getAlt(action);
        return (p != null && keysDown.contains(p))
                || (a != null && keysDown.contains(a));
    }
}
