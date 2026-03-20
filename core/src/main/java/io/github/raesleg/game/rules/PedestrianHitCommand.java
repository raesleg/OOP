// Package: io.github.raesleg.game.rules
package io.github.raesleg.game.rules;

import com.badlogic.gdx.Gdx;

import io.github.raesleg.engine.io.ICommand;

/**
 * PedestrianHitCommand — Concrete command that triggers an instant-fail
 * penalty when the player collides with a pedestrian (Command Pattern).
 * <p>
 * Executing this command sets the instant-fail flag on the
 * {@link RuleManager}, which causes an immediate game-over on the next
 * frame via {@code BaseGameScene.checkLevelEnd()}.
 * <p>
 * <b>SRP:</b> Encapsulates exactly one reversible action — setting the
 * instant-fail flag.
 * <b>OCP:</b> New penalty severities become new {@link ICommand}
 * implementations — existing commands never change.
 * <b>DIP:</b> The engine's {@link io.github.raesleg.engine.io.CommandHistory}
 * depends only on {@link ICommand}, never on this concrete class.
 */
public class PedestrianHitCommand implements ICommand {

    private final RuleManager ruleManager;

    public PedestrianHitCommand(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
    }

    @Override
    public void execute() {
        ruleManager.setPedestrianHit();
        Gdx.app.log("PedestrianHitCommand",
                "Pedestrian hit! Instant fail triggered.");
    }

    @Override
    public void undo() {
        ruleManager.clearInstantFail();
        Gdx.app.log("PedestrianHitCommand", "Instant fail cleared.");
    }
}