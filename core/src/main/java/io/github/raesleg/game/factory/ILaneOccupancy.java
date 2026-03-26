package io.github.raesleg.game.factory;

import java.util.Set;

/**
 * ILaneOccupancy — Query interface for lane-occupancy information.
 * <p>
 * Spawners implement this so other spawners can check which lanes
 * are occupied near a given Y position, preventing visual overlap.
 * <p>
 * Extracted to satisfy SRP: lane-query is a service contract,
 * separate from the spawning responsibility.
 */
public interface ILaneOccupancy {

    /**
     * Returns the set of lane indices (0-2) that have an active entity
     * whose Y position is within {@code range} pixels of {@code nearY}.
     *
     * @param nearY centre Y of the query region (pixels)
     * @param range half-height of the query region (pixels)
     * @return set of occupied lane indices
     */
    Set<Integer> getOccupiedLanesNear(float nearY, float range);
}
