package com.lumbylabs.routes;

import net.runelite.api.coords.WorldPoint;

/** Approach choices for the Waterbirth ferry. All coordinates are on the surface. */
final class RellekkaApproach {
    private static final WorldPoint DOCK = Route.point("2620 3686 0");
    private static final WorldPoint DKS = Route.point("2744 3719 0");
    private static final WorldPoint EDGE = Route.point("3129 3496 0");
    private static final WorldPoint[] RINGS = {
        Route.point("2900 3111 0"), EDGE, Route.point("2700 3247 0"),
        Route.point("2412 4434 0"), Route.point("2644 3495 0")
    };
    static final String FERRY = "Sail with Jarvald", RING = "Use fairy ring DKS";
    enum Travel {
        WALK(FERRY), TABLET("Teleport to Waterbirth"), RING(RellekkaApproach.RING, FERRY), GLORY("Teleport to Edgeville", RellekkaApproach.RING, FERRY);
        final java.util.List<String> steps;
        Travel(String... steps) { this.steps = java.util.List.of(steps); }
    }
    static Travel travel(WorldPoint p, boolean ringReady, boolean glory, boolean tablet) {
        if (p.distanceTo(DOCK) <= 60 || (p.distanceTo(DKS) <= 95 && p.getX() <= DKS.getX()+16)) return Travel.WALK;
        if (tablet) return Travel.TABLET;
        if (ringReady) {
            for (WorldPoint ring : RINGS) if (p.distanceTo(ring) <= 24) return Travel.RING;
            if (p.distanceTo(EDGE) <= 70) return Travel.RING;
            if (glory && p.getPlane() == 0 && p.getY() < 3520) return Travel.GLORY;
        }
        return Travel.WALK;
    }
    static Route.Step choose(WorldPoint p, boolean ringAccess, boolean wielded, boolean carried,
                             boolean diary, boolean glory, boolean tablet, Route.Step fallback) {
        switch (travel(p, ringAccess && (wielded || carried || diary), glory, tablet)) {
            case TABLET:
                return new Route.Step(null, "Break your Waterbirth teleport tablet to reach the island.");
            case GLORY:
                return new Route.Step(null, "Use your glory to reach Edgeville for the DKR fairy ring.", RING, FERRY);
            case RING:
                for (WorldPoint ring : RINGS) {
                    if (p.distanceTo(ring) <= 24) return new Route.Step(ring, !wielded && !diary
                        ? "Equip your staff, then configure the marked fairy ring to DKS."
                        : "Configure the marked fairy ring to DKS (Polar Hunter area).", FERRY);
                }
                return new Route.Step(EDGE, "Walk to fairy ring DKR, east of the river near Edgeville.", RING, FERRY);
            default:
                if (p.distanceTo(DOCK) <= 10) return new Route.Step(DOCK, "Speak to Jarvald and sail to Waterbirth Island.");
                if (p.distanceTo(DOCK) <= 60) return new Route.Step(DOCK, "Head to the western docks and find Jarvald.", FERRY);
                if (p.distanceTo(DKS) <= 95 && p.getX() <= DKS.getX()+16)
                    return new Route.Step(DOCK, "Head west into Rellekka, then to Jarvald at the western docks.", FERRY);
                return fallback;
        }
    }
}
