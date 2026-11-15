package com.lumbylabs.routes;

import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class KingsGuideTest {
    @Test public void pressureDoorFollowsObservedActionsAndRetrievesRock() {
        assertSame(KingsGuide.WALK_PAD, KingsGuide.pressure(Route.point("2488 10162 0"), false, false, false, true));
        assertSame(KingsGuide.DROP, KingsGuide.pressure(KingsGuide.SOUTH, false, false, false, true));
        assertSame(KingsGuide.STAND_NORTH, KingsGuide.pressure(KingsGuide.SOUTH, true, false, false, false));
        assertSame(KingsGuide.OPEN, KingsGuide.pressure(KingsGuide.NORTH, true, false, false, false));
        assertSame(KingsGuide.TAKE_SOUTH, KingsGuide.pressure(KingsGuide.NORTH, true, false, true, false));
        assertSame(KingsGuide.CROSS, KingsGuide.pressure(KingsGuide.SOUTH, false, false, true, true));
        // If the door closes before pickup, put both pads back into use.
        assertSame(KingsGuide.STAND_NORTH, KingsGuide.pressure(KingsGuide.SOUTH, true, false, false, false));
    }
    @Test public void alternatePadAndMidRouteEntryDoNotRequireClickHistory() {
        assertSame(KingsGuide.STAND_SOUTH, KingsGuide.pressure(KingsGuide.NORTH, false, true, false, false));
        assertSame(KingsGuide.OPEN, KingsGuide.pressure(KingsGuide.SOUTH, false, true, false, false));
        assertSame(KingsGuide.TAKE_NORTH, KingsGuide.pressure(KingsGuide.SOUTH, false, true, true, false));
        assertSame(KingsGuide.CROSS, KingsGuide.pressure(KingsGuide.SOUTH, false, false, true, false));
        assertSame(KingsGuide.MISSING_ROCK, KingsGuide.pressure(KingsGuide.SOUTH, false, false, false, false));
    }
    @Test public void axeHighlightsInventoryThenSpecialThenSupport() {
        assertSame(KingsGuide.WIELD, KingsGuide.axe(false, true, false, 1000, false));
        assertEquals(ItemID.RUNE_THROWNAXE, KingsGuide.inventoryItem(KingsGuide.WIELD));
        assertSame(KingsGuide.SPECIAL, KingsGuide.axe(false, false, true, 300, false));
        assertSame(KingsGuide.ATTACK, KingsGuide.axe(false, false, true, 300, true));
        assertSame(KingsGuide.ENERGY, KingsGuide.axe(false, false, true, 299, true));
        assertSame(KingsGuide.MISSING_AXE, KingsGuide.axe(false, false, false, 1000, true));
        // The last axe may have been consumed. A broken barrier must still advance.
        assertSame(KingsGuide.PASS_SUPPORT, KingsGuide.axe(true, false, false, 0, false));
        assertEquals(-1, KingsGuide.inventoryItem(KingsGuide.STAND_NORTH));
        assertEquals(ItemID.VT_USELESS_ROCK, KingsGuide.inventoryItem(KingsGuide.DROP));
    }
    @Test public void prayerChangesAtLadderNotAcrossTheWholeRoom() {
        Route.Stage room = Route.KINGS.stages.get(5);
        assertEquals(Prayer.PROTECT_FROM_MAGIC, KingsGuide.prayer(room, Route.point("1795 4405 3")));
        assertEquals(Prayer.PROTECT_FROM_MISSILES, KingsGuide.prayer(room, room.next.target));
        for (int i = 4; i <= 16; i++) assertNotNull(Route.KINGS.stages.get(i).prayer);
        Route.Stage corridor = Route.KINGS.stages.get(16);
        assertEquals(Prayer.PROTECT_FROM_MELEE, KingsGuide.prayer(corridor, Route.point("1890 4407 0")));
        assertEquals(Prayer.PROTECT_FROM_MAGIC, KingsGuide.prayer(corridor, Route.point("1919 4378 0")));
        assertNull(KingsGuide.prayer(Route.KINGS.stages.get(17), Route.point("2913 4449 0")));
    }
}
