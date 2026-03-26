package io.github.raesleg.engine.system;

/**
 * IGameSystem — Engine-level interface for modular game systems.
 * <p>
 * Each system encapsulates one responsibility (SRP) — speed control,
 * fuel management, audio coordination, etc. Systems are updated every
 * frame by the owning scene and disposed when the scene shuts down.
 * <p>
 * <b>Design pattern:</b> Strategy — scenes compose behaviour by
 * selecting which systems to instantiate.
 */
public interface IGameSystem {

    /** Called once per frame with the elapsed time since last frame. */
    void update(float deltaTime);

    /** Releases any resources held by this system. */
    void dispose();
}
