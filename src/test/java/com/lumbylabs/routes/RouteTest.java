package com.lumbylabs.routes;
import org.junit.Test;
import static org.junit.Assert.*;

public class RouteTest {
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
