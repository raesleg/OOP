package io.github.raesleg.engine.sound;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

public class SoundEffect {

  // Declare sound variable to hold the sound effect
  private Sound sound;

  public SoundEffect(String filePath) {
    this.sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
  }

  // Method to play the sound effect
  public void play() {
    sound.play();
  }

  public void dispose() {
    sound.dispose();
  }
}
