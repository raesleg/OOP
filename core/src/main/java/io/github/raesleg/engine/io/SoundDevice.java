package io.github.raesleg.engine.io;

public interface SoundDevice {
    void playSound(String name, float volume);
    void addSound(String name, String filePath);
    void loopSound(String name);
    void stopSound(String name);
    void toggleMute();
    boolean isMuted();
    void dispose();
}