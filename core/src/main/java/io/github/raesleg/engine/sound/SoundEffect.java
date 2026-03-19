package io.github.raesleg.engine.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class SoundEffect {

  // Declare sound variable to hold the sound effect
  private Sound sound;

  // Declare loopId to manage looping state (default - not looping)
  private long loopId = -1;

  public SoundEffect(String filePath) {
    this.sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
  }

  // Play the sound effect
  public void play(float volume) {
    sound.play(volume);
  }

  // Loop the sound effect
  public void loop() {
    if (loopId == -1) {
      loopId = sound.loop();
    }
  }

  // Stop the sound effect
  public void stop() {
    if (loopId != -1) {
      sound.stop(loopId);
      loopId = -1;
    } else {
      sound.stop();
    }
  }

  public void dispose() {
    sound.dispose();
  }

  public void setVolume(float volume) {
    if (loopId != -1) {
      sound.setVolume(loopId, volume);
    }
  }

  public boolean isLooping() {
    return loopId != -1;
  }
}
