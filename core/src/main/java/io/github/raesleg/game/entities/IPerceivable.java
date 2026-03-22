package io.github.raesleg.game.entities;

/**
 * Implemented by entities that NPC AI can perceive.
 * Replaces instanceof cascades with polymorphic dispatch.
 */
public interface IPerceivable {
    PerceptionCategory getPerceptionCategory();
}
