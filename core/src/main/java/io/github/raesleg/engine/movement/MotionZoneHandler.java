package io.github.raesleg.engine.movement;

import io.github.raesleg.demo.MotionProfile;

public class MotionZoneHandler {

    public boolean handle(Object aUserData, Object bUserData, boolean begin) {
        if (handleOneWay(aUserData, bUserData, begin)) return true;
        return handleOneWay(bUserData, aUserData, begin);
    }

    private boolean handleOneWay(Object entityObj, Object zoneObj, boolean begin) {
        if (entityObj instanceof MovableEntity me && zoneObj instanceof MotionProfile mp) {
            if (begin) me.onEnterZone(mp);
            else me.onExitZone();
            return true;
        }
        return false;
    }
}
