package io.github.raesleg.game.scene;

/**
 * ILevelEndCondition — Strategy for evaluating level-end conditions (OCP).
 * <p>
 * Each implementation encapsulates a single win/lose check. Conditions are
 * registered via {@link BaseGameScene#addEndCondition(ILevelEndCondition)}
 * and evaluated in order every frame. The first condition that returns
 * {@code true} halts further evaluation.
 * <p>
 * <b>Open/Closed Principle:</b> New end conditions can be added by
 * subclasses without modifying {@code BaseGameScene.checkLevelEnd()}.
 */
@FunctionalInterface
public interface ILevelEndCondition {

    /**
     * Evaluates whether this end condition is met.
     *
     * @return {@code true} if the condition fired and was handled
     *         (game-ending transition initiated), {@code false} to
     *         continue checking remaining conditions.
     */
    boolean evaluate();
}
