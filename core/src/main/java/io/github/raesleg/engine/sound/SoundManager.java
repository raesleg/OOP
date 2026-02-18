package io.github.raesleg.engine.sound;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.SoundDevice;

public class SoundManager implements SoundDevice {

    // Static variable to track mute state
    private boolean muted = false;

    public void toggleMute() {
        muted = !muted;
        Gdx.app.log("SoundManager", muted ? "Muted" : "Unmuted");
    }

    public boolean isMuted() {
        return muted;
    }

    // Map to hold sound effects
    private Map<String, SoundEffect> sounds = new HashMap<>();

    // Add sound into the list
    public void addSound(String name, String filePath) {
        sounds.put(name, new SoundEffect(filePath));
    }

    // Play sound by their name
    public void playSound(String name, float volume) {
        if (muted) {
            return;
        }
        if (sounds.containsKey(name)) {
            sounds.get(name).play(volume);
        }
    }

    // Loop sound by their name
    public void loopSound(String name) {
        if (muted) {
            return;
        }
        if (sounds.containsKey(name)) {
            sounds.get(name).loop();
        }
    }

    // Stop sound by their name
    public void stopSound(String name) {
        if (sounds.containsKey(name)) {
            sounds.get(name).stop();
        }
    }

    // Check if a sound is currently looping
    public boolean isLooping(String name) {
        if (sounds.containsKey(name)) {
            return sounds.get(name).isLooping();
        }
        return false;
    }

    public void dispose() {
        for (SoundEffect sound : sounds.values()) {
            sound.dispose();
        }
    }
}