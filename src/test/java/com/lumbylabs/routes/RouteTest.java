package com.lumbylabs.routes;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouteTest {
    @Test public void brassKeyPickupWaitsAtSpawnUntilCarried() {
        Route.Step stage = Route.HILL_GIANTS.at(new net.runelite.api.coords.WorldPoint(3131, 9862, 0));
        assertNotSame(Route.HILL_GIANTS.start, stage);
        assertSame(HillGiants.PICKUP, HillGiants.dungeon(stage, false));
        assertSame(stage, HillGiants.dungeon(stage, true));
        assertTrue(HillGiants.list(HillGiants.trapdoorSteps(), false).contains(HillGiants.PICKUP_LABEL));
        assertFalse(HillGiants.list(HillGiants.trapdoorSteps(), true).contains(HillGiants.PICKUP_LABEL));
    }
    @Test public void freeRouteRecoversFromPipeAndReachesChamberUsingWalkingWaypoints() {
        Route route = Route.HILL_GIANTS;
        assertFalse(route.members);
        assertEquals(0, route.minimumAgility);
        Route.Step step = route.at(Route.point("3122 9883 0"));
        assertTrue(step.text.contains("Go west"));
        java.util.Set<Route.Step> seen = new java.util.HashSet<>();
        while (!step.text.contains("reached")) {
            assertTrue("Walking waypoints must progress", seen.add(step));
            assertNotSame(route.start, step);
            assertFalse(step.target.equals(Route.point("3122 9880 0")));
            step = route.at(step.target);
        }
    }
    @Test public void previewsReflectTransportAndSkipRepeatedLadders() {
        Route r = Route.KINGS;
        assertEquals(java.util.List.of("Sail with Jarvald","Enter the island cave"),
            r.upcoming(r.start, RellekkaApproach.Travel.WALK.steps, true).subList(0,2));
        assertEquals(java.util.List.of("Teleport to Waterbirth","Enter the island cave"),
            r.upcoming(r.start, RellekkaApproach.Travel.TABLET.steps, true).subList(0,2));
        assertTrue(
            r.upcoming(r.at(Route.point("1808 4405 3")), java.util.List.of(), false).contains("Reach the Kings’ entrance"));
        assertTrue(r.upcoming(r.at(Route.point("2913 4449 0")), java.util.List.of(), false).isEmpty());
    }
    @Test public void startingMidDungeonResolvesByFloor() {
        assertEquals(Route.point("1808 4405 3"), Route.KINGS.at(Route.point("1808 4405 3")).target);
        assertEquals(Route.point("1823 4404 2"), Route.KINGS.at(Route.point("1808 4405 2")).target);
    }
    @Test public void enteringTheLairDoesNotSendPlayerBackToTheFerry() {
        assertEquals("Arrived in the Kings’ lair.", Route.KINGS.at(Route.point("2913 4449 0")).text);
    }
    @Test public void islandAndFinalCorridorResolveWithoutEarlierSteps() {
        assertEquals(Route.point("2521 3740 0"), Route.KINGS.at(Route.point("2549 3758 0")).target);
        assertEquals(Route.point("1911 4367 0"), Route.KINGS.at(Route.point("1890 4407 0")).target);
    }
}
