package com.lumbylabs.routes;

import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;

/** Live interaction cues, resolved from the scene rather than from elapsed time or clicks. */
final class KingsGuide {
    static final WorldPoint SOUTH = Route.point("2490 10162 0"), NORTH = Route.point("2490 10164 0");
    static final WorldPoint DOOR = Route.point("2491 10163 0"), SUPPORT = Route.point("2543 10143 0");
    static final Route.Step WALK_PAD = step(SOUTH, "Stand on the highlighted pressure pad.");
    static final Route.Step DROP = step(SOUTH, "Drop the highlighted pet rock while standing on this pad.");
    static final Route.Step MISSING_ROCK = step(SOUTH, "Bring a pet rock to open this door solo, or have another player stand on the other pad.");
    static final Route.Step STAND_NORTH = step(NORTH, "Leave the rock on its pad. Stand on the highlighted northern pad.");
    static final Route.Step STAND_SOUTH = step(SOUTH, "Leave the rock on its pad. Stand on the highlighted southern pad.");
    static final Route.Step OPEN = step(DOOR, "Open the highlighted door while you and the rock occupy both pads.");
    static final Route.Step TAKE_SOUTH = step(SOUTH, "The door is open. Take your pet rock from the highlighted southern pad.");
    static final Route.Step TAKE_NORTH = step(NORTH, "The door is open. Take your pet rock from the highlighted northern pad.");
    static final Route.Step CROSS = step(Route.point("2493 10163 0"), "Walk through the open door into the northern passage.");
    static final Route.Step WIELD = step(null, "Wield the highlighted rune thrownaxe in your inventory.");
    static final Route.Step MISSING_AXE = step(SUPPORT, "You need a rune thrownaxe to break the supports solo.");
    static final Route.Step ENERGY = step(null, "Wait for at least 30% special attack energy for the three door-supports.");
    static final Route.Step SPECIAL = step(null, "Activate the highlighted special attack bar, then attack the middle door-support.");
    static final Route.Step ATTACK = step(SUPPORT, "Special attack is ready. Attack the highlighted western (middle) door-support.");
    static final Route.Step PASS_SUPPORT = step(Route.point("2546 10143 0"), "The northern support is down. Pass through, re-equip your combat weapon, and climb down with Protect from Magic.");

    private static Route.Step step(WorldPoint point, String text) { return new Route.Step(point, text); }

    static Route.Step pressure(WorldPoint player, boolean southRock, boolean northRock, boolean open, boolean carried) {
        if (open) return southRock ? TAKE_SOUTH : northRock ? TAKE_NORTH : CROSS;
        if (southRock) return player.equals(NORTH) ? OPEN : STAND_NORTH;
        if (northRock) return player.equals(SOUTH) ? OPEN : STAND_SOUTH;
        if (!carried) return MISSING_ROCK;
        return player.equals(SOUTH) ? DROP : WALK_PAD;
    }

    static Route.Step axe(boolean broken, boolean carried, boolean wielded, int energy, boolean special) {
        if (broken) return PASS_SUPPORT;
        if (!wielded) return carried ? WIELD : MISSING_AXE;
        if (energy < 300) return ENERGY;
        return special ? ATTACK : SPECIAL;
    }

    static int inventoryItem(Route.Step step) {
        return step == DROP ? ItemID.VT_USELESS_ROCK : step == WIELD ? ItemID.RUNE_THROWNAXE : -1;
    }

    static Tile tile(WorldView world, WorldPoint point) {
        if (world == null || world.getScene() == null) return null;
        LocalPoint local = LocalPoint.fromWorld(world, point);
        if (local == null) return null;
        return world.getScene().getTiles()[point.getPlane()][local.getSceneX()][local.getSceneY()];
    }

    static boolean rock(WorldView world, WorldPoint point) {
        Tile tile = tile(world, point);
        if (tile == null || tile.getGroundItems() == null) return false;
        return tile.getGroundItems().stream().anyMatch(item -> item.getId() == ItemID.VT_USELESS_ROCK);
    }

    static java.util.List<TileObject> objects(Tile tile) {
        java.util.List<TileObject> objects = new java.util.ArrayList<>();
        if (tile == null) return objects;
        if (tile.getWallObject() != null) objects.add(tile.getWallObject());
        if (tile.getDecorativeObject() != null) objects.add(tile.getDecorativeObject());
        if (tile.getGroundObject() != null) objects.add(tile.getGroundObject());
        if (tile.getGameObjects() != null) for (GameObject object : tile.getGameObjects()) if (object != null) objects.add(object);
        return objects;
    }

    static boolean doorOpen(Client client, WorldView world) {
        // The open door moves off its closed tile; inspect the small doorway area.
        for (int x = 2489; x <= 2493; x++) for (int y = 10161; y <= 10165; y++) {
            for (TileObject object : objects(tile(world, new WorldPoint(x, y, 0)))) {
                if (object.getId() == ObjectID.DAGANNOTH_PRESSURE_OPEN) return true;
                ObjectComposition definition = client.getObjectDefinition(object.getId());
                if (definition != null && definition.getImpostorIds() != null) {
                    definition = definition.getImpostor();
                    if (definition != null && definition.getId() == ObjectID.DAGANNOTH_PRESSURE_OPEN) return true;
                }
            }
        }
        return false;
    }

    static Route.Step resolve(Client client, WorldView world, WorldPoint player, Route.Step stage) {
        if (SOUTH.equals(stage.target) || player.distanceTo(DOOR) <= 4) {
            boolean open = doorOpen(client, world);
            boolean south = rock(world, SOUTH), north = rock(world, NORTH);
            if (SOUTH.equals(stage.target) || open && (south || north)) {
                return pressure(player, south, north, open, contains(client, InventoryID.INVENTORY, ItemID.VT_USELESS_ROCK));
            }
        }
        if (SUPPORT.equals(stage.target)) {
            boolean broken = false;
            if (world != null) for (NPC npc : world.npcs()) {
                if (npc.getId() == NpcID.DAGANNOTH_WEAK_DOOR_FALLEN_NORTH
                    || npc.getId() == NpcID.DAGANNOTH_WEAK_DOOR_FALLING_NORTH) broken = true;
            }
            return axe(broken, contains(client, InventoryID.INVENTORY, ItemID.RUNE_THROWNAXE),
                contains(client, InventoryID.EQUIPMENT, ItemID.RUNE_THROWNAXE),
                client.getVarpValue(VarPlayer.SPECIAL_ATTACK_PERCENT), client.getVarpValue(VarPlayer.SPECIAL_ATTACK_ENABLED) == 1);
        }
        return stage;
    }

    private static boolean contains(Client client, InventoryID container, int item) {
        ItemContainer items = client.getItemContainer(container);
        return items != null && items.contains(item);
    }

    static Prayer prayer(Route.Stage stage, WorldPoint player) {
        if (stage == null) return null;
        Prayer prayer = stage.prayer;
        if (stage.nextPrayer != null && player.distanceTo(stage.next.target) <= 1) prayer = stage.nextPrayer;
        // Final corridor: melee for the lobsters, magic at the southwest bend / wallasalkis.
        if (stage.prayer != null && stage.next.target.equals(Route.point("1911 4367 0")) && player.getY() <= 4380) prayer = Prayer.PROTECT_FROM_MAGIC;
        return prayer;
    }

    static String prayerName(Prayer prayer) {
        if (prayer == Prayer.PROTECT_FROM_MAGIC) return "Protect from Magic";
        if (prayer == Prayer.PROTECT_FROM_MISSILES) return "Protect from Missiles";
        return "Protect from Melee";
    }
}
