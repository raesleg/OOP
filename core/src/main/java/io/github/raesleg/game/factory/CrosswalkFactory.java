package io.github.raesleg.game.factory;

import io.github.raesleg.engine.Constants;
import io.github.raesleg.engine.physics.BodyType;
import io.github.raesleg.engine.physics.PhysicsBody;
import io.github.raesleg.engine.physics.PhysicsWorld;
import io.github.raesleg.game.GameConstants;
import io.github.raesleg.game.collision.PedestrianHitReaction;
import io.github.raesleg.game.entities.misc.Pedestrian;
import io.github.raesleg.game.movement.PedestrianMovement;
import io.github.raesleg.game.scene.RoadRenderer;
import io.github.raesleg.game.zone.CrosswalkZone;

// Creates crosswalk zone and pedestrian encounter bodies with their associated entities
// Orchestrate encounters, not calculate PPM conversions or create Box2D bodies directly (SRP)
public final class CrosswalkFactory {

    private CrosswalkFactory() {}

    /*
        Creates crosswalk zone sensor body and returns zone entity
        @param world       physics world for body creation
        @param worldY      the absolute Y position of the crosswalk
        @param roadCentreX the horizontal centre of the road (pixels)
        @return the new CrosswalkZone entity
    */
    public static CrosswalkZone createZone(PhysicsWorld world, float worldY, float roadCentreX) {
        float zoneHalfW = (RoadRenderer.ROAD_WIDTH / Constants.PPM) / 2f;
        float zoneHalfH = (GameConstants.CROSSWALK_ZONE_HEIGHT / Constants.PPM) / 2f;

        PhysicsBody zoneBody = world.createBody(
                BodyType.DYNAMIC,
                roadCentreX / Constants.PPM,
                worldY / Constants.PPM,
                zoneHalfW, zoneHalfH,
                0f, 0f, true, null);

        return new CrosswalkZone(roadCentreX, worldY,
                RoadRenderer.ROAD_WIDTH, GameConstants.CROSSWALK_ZONE_HEIGHT, zoneBody);
    }

    // Holding all components of a pedestrian encounter together for easy passing between factory and scene
    public static final class EncounterComponents {
        private final Pedestrian pedestrian;
        private final PedestrianMovement movement;
        private final PedestrianHitReaction hitReaction;

        public EncounterComponents(Pedestrian pedestrian,
                PedestrianMovement movement, PedestrianHitReaction hitReaction) {
            this.pedestrian = pedestrian;
            this.movement = movement;
            this.hitReaction = hitReaction;
        }

        public Pedestrian getPedestrian() {
            return pedestrian;
        }

        public PedestrianMovement getMovement() {
            return movement;
        }

        public PedestrianHitReaction getHitReaction() {
            return hitReaction;
        }
    }

    /*
        Creates a pedestrian with its physics body and movement components.
        @param world  physics world for body creation
        @param index  encounter index (even → left-to-right, odd → right-to-left)
        @param worldY the absolute Y position of the crosswalk
        @return the encounter components (pedestrian + intent + movement + hit reaction)
    */
    public static EncounterComponents createEncounter(PhysicsWorld world, int index, float worldY) {
        float direction = (index % 2 == 0) ? 1f : -1f;
        float pedW = GameConstants.PEDESTRIAN_WIDTH;
        float pedH = GameConstants.PEDESTRIAN_HEIGHT;

        float pedStartX = (direction > 0f)
                ? RoadRenderer.ROAD_LEFT - pedW
                : RoadRenderer.ROAD_RIGHT + pedW;

        float pedHalfW = (pedW / Constants.PPM) / 2f;
        float pedHalfH = (pedH / Constants.PPM) / 2f;
        float hitboxHalfW = pedHalfW * GameConstants.PEDESTRIAN_HITBOX_SCALE;
        float hitboxHalfH = pedHalfH * GameConstants.PEDESTRIAN_HITBOX_SCALE;

        PhysicsBody pedBody = world.createBody(
                BodyType.DYNAMIC,
                (pedStartX + pedW / 2f) / Constants.PPM,
                (worldY + pedH / 2f) / Constants.PPM,
                hitboxHalfW, hitboxHalfH,
                0f, 0f, false, null);

        Pedestrian pedestrian = new Pedestrian(pedStartX, worldY, pedW, pedH, pedBody);
        PedestrianMovement movement = new PedestrianMovement(GameConstants.CROSSING_SPEED, pedW, direction);
        PedestrianHitReaction hit = new PedestrianHitReaction();

        return new EncounterComponents(pedestrian, movement, hit);
    }
}
