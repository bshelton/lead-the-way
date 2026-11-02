package com.lumbylabs.routes;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.SpriteID.IconBoss25x25;

final class Route {
    static final class Step {
        final WorldPoint target;
        final String text;
        final List<String> after;
        Step(WorldPoint target, String text, String... after) {
            this.target = target; this.text = text; this.after = List.of(after);
        }
    }
    static final class Stage {
        final WorldPoint a, b;
        final String milestone;
        final Step next;
        Stage(String line) {
            String[] fields = line.split("[|]", -1);
            milestone = fields.length > 4 ? fields[4] : fields[3];
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
    enum Length {
        SHORT("Short"), MEDIUM("Medium"), LONG("Long"), VERY_LONG("Very long");
        final String label;
        Length(String label) { this.label = label; }
    }
    final Length length;
    final boolean members;
    final int minimumAgility;
    final int[] portraits;
    final Map<Integer,String> items;
    final List<Stage> stages = new ArrayList<>();
    final Step start;
    Route(String name, String aliases, Map<Integer,String> items, Step start, String resource, boolean members, int minimumAgility, Length length, int... portraits) {
        this.portraits = portraits.clone();
        this.members = members;
        this.length = length;
        this.minimumAgility = minimumAgility;
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
    String milestone(Step stage, Step active) {
        if (active == HillGiants.EDGEVILLE) return "Climb down the trapdoor";
        if (active == HillGiants.SHED_DOOR) return "Climb down in the shed";
        for (Stage entry : stages) {
            if (entry.next == stage && !entry.milestone.isEmpty()) return entry.milestone;
        }
        return active.text;
    }
    List<String> upcoming(Step stage, List<String> approach, boolean includeCurrent) {
        List<String> result = new ArrayList<>(approach);
        boolean started = stage == start;
        String previous = "";
        for (Stage entry : stages) {
            if (entry.next == stage) {
                started = true;
                if (!includeCurrent) previous = entry.milestone;
            }
            if (started && !entry.milestone.isEmpty() && !entry.milestone.equals(previous)) {
                result.add(entry.milestone);
                previous = entry.milestone;
            }
        }
        return List.copyOf(result);
    }
    static final Route KINGS = new Route("Dagannoth Kings", "dks dag kings dagg kings rex prime supreme waterbirth",
        Map.of(ItemID.VT_USELESS_ROCK,"Pet rock",ItemID.RUNE_THROWNAXE,"Rune thrownaxe"),
        new Step(point("2620 3686 0"),"Travel to Rellekka docks and sail with Jarvald.", RellekkaApproach.FERRY), "/kings.route", true, 0, Length.VERY_LONG,
        IconBoss25x25.DAGANNOTH_REX, IconBoss25x25.DAGANNOTH_PRIME, IconBoss25x25.DAGANNOTH_SUPREME);
    static final Route HILL_GIANTS = new Route("Hill Giants", "edgeville dungeon brass key giants f2p",
        Map.of(), HillGiants.EDGEVILLE, "/hill-giants.route", false, 0, Length.MEDIUM);
    static final List<Route> CATALOG = List.of(KINGS, HILL_GIANTS);
}
