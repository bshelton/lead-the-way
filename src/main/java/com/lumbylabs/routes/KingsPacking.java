package com.lumbylabs.routes;

import java.util.function.IntUnaryOperator;
import net.runelite.api.coords.WorldPoint;
import static net.runelite.api.gameval.ItemID.*;

final class KingsPacking {
    static final int[] GLORIES = { AMULET_OF_GLORY_INF, AMULET_OF_GLORY_1, AMULET_OF_GLORY_2,
        AMULET_OF_GLORY_3, AMULET_OF_GLORY_4, AMULET_OF_GLORY_5, AMULET_OF_GLORY_6,
        TRAIL_AMULET_OF_GLORY_1, TRAIL_AMULET_OF_GLORY_2, TRAIL_AMULET_OF_GLORY_3,
        TRAIL_AMULET_OF_GLORY_4, TRAIL_AMULET_OF_GLORY_5, TRAIL_AMULET_OF_GLORY_6 };
    private final IntUnaryOperator packed, bank;
    private final java.util.LinkedHashMap<String,String> items = new java.util.LinkedHashMap<>();
    private String title;
    java.util.List<String> upcoming;
    String title() { return title; }
    java.util.Map<String,String> items() { return java.util.Collections.unmodifiableMap(items); }
    KingsPacking(IntUnaryOperator packed, IntUnaryOperator bank) {
        this.packed = packed;
        this.bank = bank;
    }
    private int pick(int... ids) {
        for (int id : ids) if (packed.applyAsInt(id) > 0) return id;
        for (int id : ids) if (bank.applyAsInt(id) > 0) return id;
        return -1;
    }
    private void need(int id, int amount, String name) {
        int remaining = amount - packed.applyAsInt(id);
        String action = remaining <= 0 ? "Packed" : bank.applyAsInt(id) >= remaining ? "Withdraw " + java.text.NumberFormat.getIntegerInstance(java.util.Locale.ENGLISH).format(remaining) : "Missing";
        items.put(name, action);
    }
    KingsPacking plan(WorldPoint location, boolean rings, boolean diary, boolean lunar, boolean freeFerry) {
        int staff = pick(DRAMEN_STAFF, LUNAR_MOONCLAN_LIMINAL_STAFF);
        int glory = pick(GLORIES);
        boolean tablet = lunar && pick(LUNAR_TABLET_WATERBIRTH_TELEPORT) >= 0;
        boolean mainland = Route.KINGS.at(location) == Route.KINGS.start;
        RellekkaApproach.Travel travel = RellekkaApproach.travel(location, rings && (diary || staff >= 0), glory >= 0, tablet);
        upcoming = Route.KINGS.upcoming(Route.KINGS.at(location), mainland ? travel.steps : java.util.List.of(), true);
        items.clear();
        if (!mainland) title = "Continue to the Kings";
        else switch (travel) {
            case TABLET: title = "Waterbirth tablet → island"; break;
            case GLORY: title = "Edgeville → fairy ring DKS → ferry"; break;
            case RING: title = "Fairy ring DKS → ferry"; break;
            default: title = "Rellekka ferry";
        }
        if (mainland) {
            if (travel == RellekkaApproach.Travel.TABLET) need(LUNAR_TABLET_WATERBIRTH_TELEPORT, 1, "Waterbirth tablet");
            else {
                if (travel == RellekkaApproach.Travel.GLORY) need(glory, 1, "Charged glory");
                if (!diary && (travel == RellekkaApproach.Travel.RING || travel == RellekkaApproach.Travel.GLORY))
                    need(staff, 1, staff == DRAMEN_STAFF ? "Dramen staff" : "Lunar staff");
                if (!freeFerry) need(COINS, 1000, "Ferry coins");
            }
        }
        need(VT_USELESS_ROCK, 1, "Pet rock");
        need(RUNE_THROWNAXE, 1, "Rune thrownaxe");
        return this;
    }
}
