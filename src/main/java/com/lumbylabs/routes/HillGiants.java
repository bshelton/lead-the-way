package com.lumbylabs.routes;

import net.runelite.api.coords.WorldPoint;

final class HillGiants {
    static final WorldPoint TRAP = new WorldPoint(3097, 3468, 0);
    static final WorldPoint SHED = new WorldPoint(3115, 3452, 0);
    static final String KEY = "Brass key";
    static final Route.Step EDGEVILLE = new Route.Step(TRAP, "Go south of Edgeville bank. Open the trapdoor in the ruins and climb down.");
    static final Route.Step SHED_WALK = new Route.Step(SHED, "Go to the locked shed west of the Cooks’ Guild.");
    static final Route.Step SHED_DOOR = new Route.Step(SHED, "Open the shed with your brass key and climb down the ladder.");

    static java.util.List<String> trapdoorSteps() {
        Route route = Route.HILL_GIANTS;
        // Start at the ladder landing. The pipe recovery detour is off this path.
        return route.upcoming(route.at(new WorldPoint(3096, 9867, 0)),
            java.util.List.of("Climb down the trapdoor"), true);
    }

    static final String PICKUP_LABEL = "Pick up the brass key";
    static final Route.Step PICKUP = new Route.Step(new WorldPoint(3131, 9862, 0),
        "Pick up the brass key in the small round room. If the tile is empty, wait for it to respawn.");
    private static final WorldPoint LOWER_PASSAGE = new WorldPoint(3117, 9852, 0);

    static Route.Step dungeon(Route.Step stage, boolean carried) {
        return !carried && LOWER_PASSAGE.equals(stage.target) ? PICKUP : stage;
    }

    static java.util.List<String> list(java.util.List<String> steps, boolean carried) {
        int lower = steps.indexOf("Follow the lower passage");
        if (carried || lower < 0) return steps;
        java.util.List<String> result = new java.util.ArrayList<>(steps);
        result.add(lower, PICKUP_LABEL);
        return result;
    }

    static boolean useShed(WorldPoint p, boolean hasKey) {
        return hasKey && p.distanceTo2D(SHED) < p.distanceTo2D(TRAP);
    }

    static Route.Step surface(WorldPoint p, boolean carriedKey) {
        if (!useShed(p, carriedKey)) return EDGEVILLE;
        return p.distanceTo(SHED) <= 5 ? SHED_DOOR : SHED_WALK;
    }
}
