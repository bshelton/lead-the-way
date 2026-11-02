package com.lumbylabs.routes;
import org.junit.Test;
import static org.junit.Assert.*;
public class RellekkaApproachTest {
    private Route.Step at(String tile, boolean access, boolean equipped, boolean carried) {
        return RellekkaApproach.choose(Route.point(tile), access, equipped, carried, false, true, false, Route.KINGS.start);
    }
    @Test public void fishingSpotUsesLocalRingBeforeGlory() {
        assertTrue(at("2901 3113 0",true,true,false).text.contains("DKS"));
        assertTrue(at("2901 3113 0",true,false,true).text.contains("Equip"));
        assertSame(Route.KINGS.start,at("2901 3113 0",false,true,false));
    }
    @Test public void arrivalAtDksPointsWestAndDockOffersBoat() {
        assertTrue(at("2744 3719 0",true,true,false).text.contains("west"));
        assertTrue(at("2620 3686 0",true,true,false).text.contains("sail"));
    }
}
