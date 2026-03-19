package io.github.raesleg.game.state;
/**
 * Observer interface for the in-game HUD dashboard.
 * <p>
 * Game logic (e.g., {@link GameScene}) acts as the <b>subject</b> and calls
 * these methods whenever a gameplay value changes. The concrete observer
 * ({@link DashboardUI}) updates its visual labels in response.
 * <p>
 * <b>Design pattern:</b> Observer — decouples game-state changes from
 * UI presentation so either side can change independently.
 */
public interface IDashboardObserver {

    /** Called when the player's score changes. */
    void onScoreUpdated(int score);

    /** Called when the player's progress towards the finish changes (0.0 – 1.0). */
    void onProgressUpdated(float percentage);

    /**
     * Called when the player breaks a traffic rule; {@code totalBroken} is the
     * cumulative count.
     */
    void onRuleBroken(int totalBroken);

    /** Called when the player's speed changes (in KM/H). */
    void onSpeedChanged(int speed);
}
