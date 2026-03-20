package io.github.raesleg.engine.io;

public interface SoundDevice {
    void playSound(String name, float volume);

    void addSound(String name, String filePath);

    void loopSound(String name);

    void stopSound(String name);

    void setSoundVolume(String name, float volume);

    void stopAllSounds();

    void toggleMute();

    boolean isMuted();

    boolean isLooping(String name);

    float getMasterVolume();

    void setMasterVolume(float volume);

    void dispose();
}