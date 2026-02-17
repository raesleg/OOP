package io.github.raesleg.demo;

import java.util.Stack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectMap;

import io.github.raesleg.engine.io.ActionInput;
import io.github.raesleg.engine.io.InputDevice;

public class Keyboard implements InputDevice, ActionInput {

    private static final class Bind {
        final Runnable action;
        final boolean justPressed;
        Bind(Runnable action, boolean justPressed) {
            this.action = action;
            this.justPressed = justPressed;
        }
        boolean shouldFire(int key) {
            return justPressed ? Gdx.input.isKeyJustPressed(key) : Gdx.input.isKeyPressed(key);
        }
    }

    private static final class Context {
        final IntMap<Bind> commandBinds = new IntMap<>();
        final ObjectMap<String, IntArray> actionBinds = new ObjectMap<>();
    }

    private final Stack<Context> contexts = new Stack<>();

    public Keyboard() {
        pushContext(); // base context
    }

    @Override
    public void pushContext() {
        contexts.push(new Context());
    }

    public void popContext() {
        if (contexts.size() <= 1) return; // keep base context
        contexts.pop();
    }

    @Override
    public void resetToBase() {
        while (contexts.size() > 1) contexts.pop();
    }

    private Context active() {
        return contexts.peek();
    }

    /* ---------- InputDevice (commands) ---------- */

    @Override
    public void addBind(int keyOrButton, Runnable action, boolean isJustPressed) {
        active().commandBinds.put(keyOrButton, new Bind(action, isJustPressed));
    }

    @Override
    public void removeBind(int keyOrButton) {
        active().commandBinds.remove(keyOrButton);
    }

    @Override
    public void handleInput() {
        IntMap<Bind> binds = active().commandBinds;
        for (IntMap.Entry<Bind> e : binds.entries()) {
            if (e.value.shouldFire(e.key)) e.value.action.run();
        }
    }

    /* ---------- ActionInput ---------- */

    /** Bind a key to a logical action name (e.g. "LEFT"). */
    public void bindAction(int key, String actionName) {
        Context c = active();
        IntArray keys = c.actionBinds.get(actionName);
        if (keys == null) {
            keys = new IntArray();
            c.actionBinds.put(actionName, keys);
        }
        // prevent duplicates
        for (int i = 0; i < keys.size; i++) {
            if (keys.get(i) == key) return;
        }
        keys.add(key);
    }

    /** Optional: remove one key from an action in current context. */
    public void unbindAction(int key, String actionName) {
        Context c = active();
        IntArray keys = c.actionBinds.get(actionName);
        if (keys == null) return;
        for (int i = keys.size - 1; i >= 0; i--) {
            if (keys.get(i) == key) keys.removeIndex(i);
        }
    }

    @Override
    public boolean isHeld(String action) {
        IntArray keys = active().actionBinds.get(action);
        if (keys == null) return false;
        for (int i = 0; i < keys.size; i++) {
            if (Gdx.input.isKeyPressed(keys.get(i))) return true;
        }
        return false;
    }

    @Override
    public boolean justPressed(String action) {
        IntArray keys = active().actionBinds.get(action);
        if (keys == null) return false;
        for (int i = 0; i < keys.size; i++) {
            if (Gdx.input.isKeyJustPressed(keys.get(i))) return true;
        }
        return false;
    }
}