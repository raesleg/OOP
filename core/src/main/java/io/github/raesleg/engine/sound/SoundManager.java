package io.github.raesleg.engine.sound;

import java.util.HashMap;
import java.util.Map;

public class SoundManager {

    // Map to hold sound effects
    private Map<String, SoundEffect> sounds = new HashMap<>();

    // Add sound into the list
    public void addSound(String name, String filePath) {
        sounds.put(name, new SoundEffect(filePath));
    }

    // Play sound by their name
    public void playSound(String name) {
        if (sounds.containsKey(name)) {
          sounds.get(name).play();
        }
    }

    public void dispose() {
        for (SoundEffect sound : sounds.values()) {
            sound.dispose();
        }
    }
}