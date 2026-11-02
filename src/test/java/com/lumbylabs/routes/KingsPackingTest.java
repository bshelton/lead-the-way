package com.lumbylabs.routes;

import java.util.Map;
import static net.runelite.api.gameval.ItemID.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class KingsPackingTest {
    private KingsPacking plan(Map<Integer,Integer> packed, Map<Integer,Integer> bank, boolean rings, boolean diary, boolean lunar) {
        return new KingsPacking(id -> packed.getOrDefault(id,0), id -> bank.getOrDefault(id,0))
            .plan(Route.point("3092 3245 0"), rings, diary, lunar, false);
    }
    @Test public void tabletWinsAndEquippedAxeNeedsNoWithdrawal() {
        KingsPacking text = plan(Map.of(RUNE_THROWNAXE,1),Map.of(LUNAR_TABLET_WATERBIRTH_TELEPORT,2,VT_USELESS_ROCK,1),true,false,true);
        assertTrue(text.title().contains("tablet → island")); assertEquals("Withdraw 1", text.items().get("Waterbirth tablet"));
        assertEquals("Packed", text.items().get("Rune thrownaxe")); assertFalse(text.items().containsKey("Ferry coins"));
    }
    @Test public void onlyEligibleRingAndChargedJewelleryAreUsed() {
        Map<Integer,Integer> bank = Map.of(LUNAR_TABLET_WATERBIRTH_TELEPORT,1, AMULET_OF_GLORY_3,1, DRAMEN_STAFF,1, COINS,1000);
        KingsPacking text=plan(Map.of(COINS,300),bank,true,false,false);
        assertTrue(text.title().contains("Edgeville → fairy")); assertTrue(text.items().containsValue("Withdraw 700")); assertTrue(text.items().containsKey("Dramen staff"));
        assertFalse(plan(Map.of(),bank,false,false,false).title().contains("fairy ring"));
        assertFalse(plan(Map.of(),bank,true,true,false).items().containsKey("Dramen staff"));
        assertFalse(plan(Map.of(),Map.of(AMULET_OF_GLORY_3,0),true,true,false).title().contains("Edgeville"));
    }
}
