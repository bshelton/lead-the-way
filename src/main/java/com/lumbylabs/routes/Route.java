package com.lumbylabs.routes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;

final class Route {
    static final class Step {
        final WorldPoint target;
        final String text;
        Step(WorldPoint target, String text) { this.target = target; this.text = text; }
    }
    static final class Stage {
        final WorldPoint a, b;
        final Step next;
        Stage(String line) {
            String[] fields = line.split("[|]");
            a = point(fields[0]); b = point(fields[1]);
            next = new Step(point(fields[2]), fields[3]);
        }
        boolean contains(WorldPoint p) {
            return p.getPlane() == a.getPlane()
                && between(p.getX(), a.getX(), b.getX()) && between(p.getY(), a.getY(), b.getY());
        }
        private boolean between(int n, int a, int b) { return n >= Math.min(a,b) && n <= Math.max(a,b); }
    }
    final String name, aliases;
    final Map<Integer,String> items;
    final List<Stage> stages = new ArrayList<>();
    final Step start;
    Route(String name, String aliases, Map<Integer,String> items, Step start, String resource) {
        this.name = name; this.aliases = aliases; this.items = items; this.start = start;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Route.class.getResourceAsStream(resource)), StandardCharsets.UTF_8))) {
            reader.lines().forEach(line -> stages.add(new Stage(line)));
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    static WorldPoint point(String value) {
        String[] n = value.split(" ");
        return new WorldPoint(Integer.parseInt(n[0]), Integer.parseInt(n[1]), Integer.parseInt(n[2]));
    }
    Step at(WorldPoint p) {
        for (Stage stage : stages) { if (stage.contains(p)) return stage.next; }
        return start;
    }
    static final Route KINGS = new Route("Dagannoth Kings", "dks dag kings dagg kings rex prime supreme waterbirth",
        Map.of(ItemID.VT_USELESS_ROCK,"Pet rock",ItemID.RUNE_THROWNAXE,"Rune thrownaxe"),
        new Step(point("2620 3686 0"),"Travel to Rellekka docks and sail with Jarvald."), "/kings.route");
    static final List<Route> CATALOG = List.of(KINGS);
}
