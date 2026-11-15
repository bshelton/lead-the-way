package com.lumbylabs.routes;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.*;

import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@PluginDescriptor(name="Lead the Way", description="Step-by-step destination directions", tags={"travel","route"})
public class LeadTheWayPlugin extends Plugin {
    @Inject private LeadTheWayConfig config;
    @Inject private ConfigManager configManager;
    private volatile boolean showBanner;
    boolean canvasHelperEnabled() { return showBanner; }
    void setCanvasHelperEnabled(boolean enabled) {
        configManager.setConfiguration("leadtheway", "showCanvasHelper", enabled);
    }

    @Provides LeadTheWayConfig provideConfig(ConfigManager manager) {
        return manager.getConfig(LeadTheWayConfig.class);
    }

    @Subscribe public void onConfigChanged(ConfigChanged event) {
        if ("leadtheway".equals(event.getGroup()) && "showCanvasHelper".equals(event.getKey())) {
            showBanner = config.showCanvasHelper();
        }
    }

    @Inject private Client client;
    @Inject private ClientToolbar toolbar;
    @Inject private net.runelite.client.game.SpriteManager sprites;
    @Inject private OverlayManager overlays;
    @Inject private WorldMapPointManager mapPoints;
    private WorldMapPoint marker;
    private MiniGuide miniGuide;
    private NavigationButton nav;
    private GuidePanel panel;
    private SceneGuide overlay;
    private InstructionBanner banner;
    private InventoryGuide inventoryGuide;
    private InteractionOverlay interactionOverlay;
    // Identity distinguishes two visits to the same route, including queued Swing updates.
    static final class View {
        final Route route;
        final boolean active;
        volatile Route.Step target;
        volatile Prayer prayer;
        View(Route route, boolean active) {
            this.route = route;
            this.active = active;
        }
    }
    private volatile View view;
    private View renderedView;
    private Route.Step renderedStep;
    private boolean suppliesDirty;
    private boolean bankOpen;
    private boolean showingBankPlan;
    private java.util.List<String> previousUpcoming = java.util.List.of();
    private String previousText;
    private synchronized void mark(View owner, Route.Step step) {
        if (owner != view) return;
        if (marker != null) {
            mapPoints.remove(marker);
            marker = null;
        }
        if (step == null || step.target == null) return;
        marker = new WorldMapPoint(step.target, nav.getIcon());
        marker.setName(owner.route.name);
        marker.setTooltip(step.text);
        marker.setSnapToEdge(true);
        marker.setJumpOnClick(true);
        mapPoints.add(marker);
    }

    synchronized void select(Route route, boolean active) {
        mark(view, null);
        view = route == null ? null : new View(route, active);
    }

    @Override protected void startUp() {
        showBanner = config.showCanvasHelper();
        panel = new GuidePanel(this);
        panel.loadPortraits(sprites);
        nav = NavigationButton.builder().tooltip("Lead the Way")
            .icon(net.runelite.client.util.ImageUtil.loadImageResource(getClass(), "/brand/sign-16.png"))
            .panel(panel).build();
        toolbar.addNavigation(nav); overlay = new SceneGuide(); overlays.add(overlay);
        banner = new InstructionBanner();
        overlays.add(banner);
        miniGuide = new MiniGuide(); overlays.add(miniGuide);
        inventoryGuide = new InventoryGuide(); overlays.add(inventoryGuide);
        interactionOverlay = new InteractionOverlay(client, this::interactionStep, () -> view == null ? null : view.prayer);
        overlays.add(interactionOverlay);
    }
    @Override protected void shutDown() {
        toolbar.removeNavigation(nav); overlays.remove(overlay); overlays.remove(banner); overlays.remove(miniGuide); overlays.remove(inventoryGuide); overlays.remove(interactionOverlay); select(null, false); renderedView = null;
    }
    private String supplies(View current, WorldPoint location) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (inventory == null) return "Waiting for inventory…";
        if (current.route == Route.HILL_GIANTS) {
            Route.Step stage = current.route.at(location);
            boolean carried = inventory.count(ItemID.EDGEVILLEDUNGEONKEY) > 0;
            ItemContainer bank = bankOpen ? client.getItemContainer(InventoryID.BANK) : null;
            if (bankOpen && bank == null) return "Loading bank…";
            boolean shed = stage == current.route.start && HillGiants.useShed(location,
                carried || bank != null && bank.count(ItemID.EDGEVILLEDUNGEONKEY) > 0);
            String title = stage != current.route.start ? "Continue through the dungeon"
                : shed ? "Cooks’ Guild shed" : "Edgeville trapdoor · No key needed";
            java.util.Map<String,String> rows = shed ? java.util.Map.of(HillGiants.KEY, carried ? "Packed" : "Withdraw 1")
                : java.util.Map.of();
            java.util.List<String> upcoming = HillGiants.list(stage != current.route.start ? current.route.upcoming(stage, java.util.List.of(), true)
                : shed ? java.util.List.of("Climb down in the shed", "Enter the giant chamber")
                : HillGiants.trapdoorSteps(), carried);
            boolean showBank = bankOpen;
            previousText = null;
            SwingUtilities.invokeLater(() -> {
                if (view == current) {
                    if (showBank) panel.showPacking(title, rows);
                    else panel.showDetail(shed ? "Use your brass key at the shed." : stage == current.route.start
                        ? "Edgeville trapdoor · No key needed" : title, false);
                    panel.setUpcoming(upcoming);
                }
            });
            return null;
        }
        if (bankOpen && current.route == Route.KINGS) {
            ItemContainer bank = client.getItemContainer(InventoryID.BANK);
            if (bank == null) return "Loading bank…";
            KingsPacking packing = new KingsPacking(id -> inventory.count(id) + (equipment == null ? 0 : equipment.count(id)), bank::count)
                .plan(location, client.getVarbitValue(VarbitID.FAIRYRING_USE) > 0,
                    client.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0,
                    Quest.LUNAR_DIPLOMACY.getState(client) == QuestState.FINISHED,
                    Quest.THE_FREMENNIK_TRIALS.getState(client) == QuestState.FINISHED);
            previousText = null;
            String title = packing.title();
            java.util.List<String> upcoming = packing.upcoming;
            java.util.Map<String,String> rows = packing.items();
            SwingUtilities.invokeLater(() -> { if (view == current) { panel.showPacking(title, rows); panel.setUpcoming(upcoming); } });
            return null;
        }
        StringBuilder status = new StringBuilder("<html><table cellpadding='0' cellspacing='0'>");
        current.route.items.forEach((id, name) -> {
            boolean packed = inventory.contains(id) || equipment != null && equipment.contains(id);
            status.append("<tr><td width='98' height='16'>").append(name).append("</td><td align='right'><font color='").append(packed ? "#a9db83" : "#f2be65")
                .append("'>").append(packed ? "Packed" : "Missing").append("</font></td></tr>");
        });
        return status.append("</table></html>").toString();
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()
            || event.getContainerId() == InventoryID.BANK.getId() || event.getContainerId() == InventoryID.EQUIPMENT.getId()) {
            suppliesDirty = true;
            gearDirty = true;
        }
    }

    private boolean hillKeyCarried;
    private Route.Step approach;
    private WorldPoint approachLocation;
    private boolean gearDirty = true, wieldedStaff, carriedStaff, hasGlory, hasTablet;

    private Route.Step approach(WorldPoint p, Route.Step fallback) {
        if (!gearDirty && !suppliesDirty && p.equals(approachLocation)) return approach;
        if (gearDirty) {
            ItemContainer worn = client.getItemContainer(InventoryID.EQUIPMENT);
            ItemContainer bag = client.getItemContainer(InventoryID.INVENTORY);
            wieldedStaff = hasStaff(worn);
            carriedStaff = hasStaff(bag);
            hasGlory = java.util.Arrays.stream(KingsPacking.GLORIES).anyMatch(id -> contains(worn, id) || contains(bag, id));
            hasTablet = contains(bag, ItemID.LUNAR_TABLET_WATERBIRTH_TELEPORT);
            gearDirty = false;
        }
        Route.Step candidate = RellekkaApproach.choose(p, client.getVarbitValue(VarbitID.FAIRYRING_USE) > 0,
            wieldedStaff, carriedStaff, client.getVarbitValue(VarbitID.LUMBRIDGE_DIARY_ELITE_COMPLETE) > 0,
            hasGlory, hasTablet && Quest.LUNAR_DIPLOMACY.getState(client) == QuestState.FINISHED, fallback);
        if (approach == null || !java.util.Objects.equals(candidate.target, approach.target)
            || !candidate.text.equals(approach.text)) approach = candidate;
        approachLocation = p;
        suppliesDirty = false;
        return approach;
    }

    private static boolean contains(ItemContainer container, int id) {
        return container != null && container.contains(id);
    }

    private static boolean hasStaff(ItemContainer c) {
        return contains(c, ItemID.DRAMEN_STAFF) || contains(c, ItemID.LUNAR_MOONCLAN_LIMINAL_STAFF);
    }

    static Route.Step padInstruction(WorldPoint player, boolean southRock, boolean northRock, Route.Step drop) {
        return southRock || northRock ? KingsGuide.pressure(player, southRock, northRock, false, false) : drop;
    }

    Route.Step interactionStep() {
        View current = view;
        return current != null && current.active && current.route == Route.KINGS && !bankOpen
            && client.getGameState() == GameState.LOGGED_IN ? current.target : null;
    }

    private final class InventoryGuide extends WidgetItemOverlay {
        InventoryGuide() { showOnInterfaces(InterfaceID.INVENTORY); }
        @Override public void renderItemOverlay(Graphics2D graphics, int itemId, net.runelite.api.widgets.WidgetItem item) {
            Route.Step current = interactionStep();
            if (current == null || KingsGuide.inventoryItem(current) != itemId) return;
            Graphics2D g = (Graphics2D) graphics.create();
            try { InteractionOverlay.highlight(g, item.getCanvasBounds(), current == KingsGuide.DROP ? "Drop" : "Wield"); }
            finally { g.dispose(); }
        }
    }

    private void publish(View current, String text) {
        publish(current, text, java.util.List.of());
    }
    private void publish(View current, String text, java.util.List<String> upcoming) {
        publish(current, text, upcoming, null, null);
    }
    private void publish(View current, String text, java.util.List<String> upcoming, String milestone, String instruction) {
        if (text.equals(previousText) && upcoming.equals(previousUpcoming)) return;
        previousUpcoming = upcoming;
        previousText = text;
        SwingUtilities.invokeLater(() -> {
            if (view == current) {
                panel.showDetail(text, current.active);
                if (milestone == null) panel.setUpcoming(upcoming);
                else panel.showProgress(milestone, instruction, upcoming);
            }
        });
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) return;
        renderedView = null;
        View current = view;
        if (current != null) {
            current.target = null;
            mark(current, null);
            previousText = null;
            publish(current, "Waiting for your character…");
        }
    }

    @Subscribe public void onGameTick(GameTick event) {
        View current = view;
        if (current == null) return;
        if (current != renderedView) {
            gearDirty = true;
            renderedView = current;
            renderedStep = null;
            previousText = null;
            suppliesDirty = true;
        }
        Player player = client.getLocalPlayer();
        if (client.getGameState() != GameState.LOGGED_IN || player == null) {
            current.target = null;
            mark(current, null);
            publish(current, "Waiting for your character…");
            return;
        }
        net.runelite.api.widgets.Widget bankWidget = client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS);
        boolean open = bankWidget != null && !bankWidget.isHidden();
        if (open != bankOpen) {
            bankOpen = open;
            suppliesDirty = true;
        }
        if (!current.active || bankOpen) {
            showingBankPlan = bankOpen;
            if (suppliesDirty) {
                suppliesDirty = false;
                current.target = null;
            mark(current, null);
                String message = supplies(current, player.getWorldLocation());
                if (message != null) {
                    Route.Step stage = current.route.at(player.getWorldLocation());
                    java.util.List<String> prefix = java.util.List.of();
                    if (current.route == Route.KINGS && stage == current.route.start && !bankOpen) {
                        Route.Step travel = approach(player.getWorldLocation(), stage);
                        java.util.ArrayList<String> approachSteps = new java.util.ArrayList<>();
                        approachSteps.add(travel.text);
                        if (travel != stage) approachSteps.addAll(travel.after);
                        prefix = approachSteps;
                    }
                    publish(current, message, current.route.upcoming(stage, prefix, true));
                }
            }
            return;
        }
        if (showingBankPlan) {
            renderedStep = null;
            showingBankPlan = false;
        }
        WorldPoint location = player.getWorldLocation();
        Route.Step stage = current.route.at(location);
        Route.Step next = stage;
        if (current.route == Route.HILL_GIANTS) {
            if (suppliesDirty) {
                renderedStep = null;
                hillKeyCarried = contains(client.getItemContainer(InventoryID.INVENTORY), ItemID.EDGEVILLEDUNGEONKEY);
                suppliesDirty = false;
            }
            next = stage == current.route.start ? HillGiants.surface(location, hillKeyCarried) : HillGiants.dungeon(stage, hillKeyCarried);
        }
        if (current.route == Route.KINGS && next == current.route.start) next = approach(location, next);
        if (current.route == Route.KINGS) {
            current.prayer = null;
            for (Route.Stage room : current.route.stages) {
                if (room.next == stage) { current.prayer = KingsGuide.prayer(room, location); break; }
            }
            next = KingsGuide.resolve(client, player.getWorldView(), location, next);
            if (next == KingsGuide.PASS_SUPPORT) current.prayer = Prayer.PROTECT_FROM_MAGIC;
            // Keep the entrance prayer and ladder marker visible until the player enters the lair.
        }
        if (view != current) return;
        current.target = next;
        if (next != renderedStep) {
            renderedStep = next;
            mark(current, next);
            publish(current, "<html><body width='124'>" + next.text + "</body></html>",
                next == HillGiants.SHED_WALK ? java.util.List.of("Climb down in the shed", "Enter the giant chamber")
                    : next == HillGiants.SHED_DOOR ? java.util.List.of("Enter the giant chamber")
                    : next == HillGiants.PICKUP ? current.route.upcoming(stage, java.util.List.of(), true)
                    : next == HillGiants.EDGEVILLE ? HillGiants.list(HillGiants.trapdoorSteps(), hillKeyCarried)
                    : current.route == Route.HILL_GIANTS ? HillGiants.list(current.route.upcoming(stage, next.after, false), hillKeyCarried)
                    : current.route.upcoming(stage, next.after, false),
                next == HillGiants.PICKUP ? HillGiants.PICKUP_LABEL : current.route.milestone(stage, next), next.text);
        }
    }
    private final class MiniGuide extends Overlay {
        MiniGuide() {
            setPosition(OverlayPosition.DYNAMIC);
            setLayer(OverlayLayer.ABOVE_WIDGETS);
        }

        @Override public Dimension render(Graphics2D g) {
            View active = view;
            Route.Step step = active == null ? null : active.target;
            if (step == null || step.target == null || client.getGameState() != GameState.LOGGED_IN) return null;
            WorldView world = client.getTopLevelWorldView();
            CameraFocusableEntity focus = client.getCameraFocusEntity();
            if (world == null || world.getPlane() != step.target.getPlane() || focus == null) return null;
            LocalPoint center = focus.getCameraFocus();
            if (center == null || center.getWorldView() != WorldView.TOPLEVEL) return null;
            int widgetId = !client.isResized() ? InterfaceID.Toplevel.MINIMAP
                : client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
                    ? InterfaceID.ToplevelPreEoc.MINIMAP : InterfaceID.ToplevelOsrsStretch.MINIMAP;
            Widget widget = client.getWidget(widgetId);
            if (widget == null || widget.isHidden()) return null;
            int radius = Math.min(widget.getWidth(), widget.getHeight()) / 2 - 13;
            double zoom = client.getMinimapZoom();
            if (radius <= 0 || zoom <= 0) return null;
            double dx = (step.target.getX() - world.getBaseX()) * 128.0 + 64 - center.getX();
            double dy = (step.target.getY() - world.getBaseY()) * 128.0 + 64 - center.getY();
            double pixels = Math.hypot(dx, dy) * zoom / 128;
            double scale = pixels > radius ? radius / pixels : 1;
            LocalPoint projected = new LocalPoint(center.getX() + (int)(dx * scale),
                center.getY() + (int)(dy * scale), world);
            net.runelite.api.Point point = Perspective.localToMinimap(client, projected, 32000);
            if (point == null) return null;
            int x = point.getX(), y = point.getY();
            if (pixels <= radius) {
                g.setColor(Color.BLACK); g.fillOval(x-4,y-4,9,9);
                g.setColor(Color.CYAN); g.fillOval(x-3,y-3,7,7);
            } else {
                Rectangle bounds = widget.getBounds();
                double angle = Math.atan2(y-bounds.getCenterY(), x-bounds.getCenterX());
                Graphics2D arrow = (Graphics2D)g.create();
                try {
                    arrow.translate(x,y); arrow.rotate(angle);
                    Polygon shape = new Polygon(new int[]{7,-5,-2,-5},new int[]{0,-5,0,5},4);
                    arrow.setColor(Color.CYAN); arrow.fill(shape);
                    arrow.setColor(Color.BLACK); arrow.draw(shape);
                } finally { arrow.dispose(); }
            }
            return null;
        }
    }

    private static final Color BACKGROUND = new Color(24,24,24,222);
    private final class SceneGuide extends Overlay {
        SceneGuide() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE); }
        public Dimension render(Graphics2D g) {
            View current = view;
            if (current == null || !current.active || client.getGameState() != GameState.LOGGED_IN) return null;
            Route.Step s = current.target;
            if (s == null) return null;
            WorldView world = client.getTopLevelWorldView();
            if (world == null) return null;
            LocalPoint l=s.target == null ? null : LocalPoint.fromWorld(world,s.target);
            if(l!=null) {
                Polygon tile=Perspective.getCanvasTilePoly(client,l);
                if(tile!=null) OverlayUtil.renderPolygon(g,tile,Color.CYAN);
                if (current.route == Route.KINGS) {
                    String label = s == KingsGuide.DROP ? "Drop pet rock here"
                        : s == KingsGuide.TAKE_SOUTH || s == KingsGuide.TAKE_NORTH ? "Take pet rock"
                        : s == KingsGuide.WALK_PAD || s == KingsGuide.STAND_NORTH || s == KingsGuide.STAND_SOUTH ? "Stand here"
                        : s == KingsGuide.OPEN ? "Open door"
                        : s == KingsGuide.ATTACK ? "Attack (special)" : "Next step";
                    net.runelite.api.Point text = Perspective.getCanvasTextLocation(client, g, l, label, 30);
                    if (text != null) OverlayUtil.renderTextLocation(g, text, label, Color.CYAN);
                    if (s == KingsGuide.ATTACK) {
                        for (NPC npc : world.npcs()) {
                            if (npc.getId() == net.runelite.api.gameval.NpcID.DAGANNOTH_WEAK_DOOR_WEST) {
                                Shape hull = npc.getConvexHull();
                                if (hull != null) { g.setColor(Color.CYAN); g.draw(hull); }
                            }
                        }
                    } else {
                        for (TileObject object : KingsGuide.objects(KingsGuide.tile(world, s.target))) {
                            Shape hull = object instanceof GameObject ? ((GameObject) object).getConvexHull()
                                : object instanceof WallObject ? ((WallObject) object).getConvexHull()
                                : object instanceof GroundObject ? ((GroundObject) object).getConvexHull()
                                : object instanceof DecorativeObject ? ((DecorativeObject) object).getConvexHull() : null;
                            if (hull != null) { g.setColor(Color.CYAN); g.draw(hull); }
                        }
                    }
                }
            }
            return null;
        }
    }

    private final class InstructionBanner extends Overlay {
        InstructionBanner() {
            setPosition(OverlayPosition.TOP_LEFT);
            setLayer(OverlayLayer.ABOVE_WIDGETS);
        }

        @Override public String getName() { return "Lead the Way instruction"; }

        @Override public Dimension render(Graphics2D g) {
            View current = view;
            if (current == null || !current.active || client.getGameState() != GameState.LOGGED_IN) return null;
            if (!showBanner) return null;
            Route.Step instruction = current.target;
            if (instruction == null) return null;

            FontMetrics font = g.getFontMetrics();
            int textWidth = Math.min(360, Math.max(100, client.getCanvasWidth() - 36));
            java.text.AttributedString text = new java.text.AttributedString(instruction.text);
            text.addAttribute(java.awt.font.TextAttribute.FONT, g.getFont());
            java.awt.font.LineBreakMeasurer measurer = new java.awt.font.LineBreakMeasurer(
                text.getIterator(), g.getFontRenderContext());
            java.util.List<java.awt.font.TextLayout> lines = new java.util.ArrayList<>();
            float widest = 0;
            while (measurer.getPosition() < instruction.text.length()) {
                java.awt.font.TextLayout line = measurer.nextLayout(textWidth);
                lines.add(line);
                widest = Math.max(widest, line.getVisibleAdvance());
            }
            int width = (int) Math.ceil(widest) + 18;
            int height = font.getHeight() * lines.size() + 10;
            g.setColor(BACKGROUND);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.WHITE);
            int baseline = 5 + font.getAscent();
            for (java.awt.font.TextLayout line : lines) {
                line.draw(g, 9, baseline);
                baseline += font.getHeight();
            }
            return new Dimension(width, height);
        }
    }
}
