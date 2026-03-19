package io.github.raesleg.engine.entity;

/**
 * IFlashable — Interface for entities that can display a damage flash effect.
 * 
 * Entities implementing this interface can trigger a visual "blink" or "flash" animation
 * 
 * @see Entity
 */
public interface IFlashable {
    
    /**
     * Triggers the damage flash effect.
     */
    void triggerDamageFlash();
    
    /**
     * Checks if the entity is currently in a flash state.
     */
    boolean isFlashing();
}