package com.lumbylabs.routes;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.*;
import net.runelite.client.ui.overlay.*;

@PluginDescriptor(name="Lead the Way", description="Step-by-step destination directions", tags={"travel","route"})
public class LeadTheWayPlugin extends Plugin {
    @Inject private Client client;
    @Inject private ClientToolbar toolbar;
    @Inject private OverlayManager overlays;
    private NavigationButton nav;
    private GuidePanel panel;
    private SceneGuide overlay;
    // Identity distinguishes two visits to the same route, including queued Swing updates.
    static final class View {
        final Route route;
        final boolean active;
        volatile Route.Step target;
        View(Route route, boolean active) {
            this.route = route;
            this.active = active;
        }
    }
    private volatile View view;
    private View renderedView;
    private Route.Step renderedStep;
    private boolean suppliesDirty;
    private String previousText;
    private static final WorldPoint PAD = Route.point("2490 10162 0");
    private static final WorldPoint OTHER = Route.point("2490 10164 0");
    private static final WorldPoint ENTRANCE = Route.point("1911 4367 0");
    private static final Route.Step STAND_NORTH = new Route.Step(OTHER, "Stand on the northern pressure pad.");
    private static final Route.Step STAND_SOUTH = new Route.Step(PAD, "Stand on the southern pressure pad.");
    private static final Route.Step OPEN = new Route.Step(Route.point("2491 10163 0"), "Open the pressure door beside you.");
    private static final Route.Step ARRIVED = new Route.Step(ENTRANCE, "Arrived at the Kings’ entrance.");

    void select(Route route, boolean active) {
        view = route == null ? null : new View(route, active);
    }

    @Override protected void startUp() {
        panel = new GuidePanel(this);
        BufferedImage icon = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics(); g.setColor(new Color(246,189,76));
        g.fillRect(6,2,2,13); g.fillPolygon(new int[]{1,11,15,11,1},new int[]{3,3,6,9,9},5); g.dispose();
        nav = NavigationButton.builder().tooltip("Lead the Way").icon(icon).panel(panel).build();
        toolbar.addNavigation(nav); overlay = new SceneGuide(); overlays.add(overlay);
    }
    @Override protected void shutDown() {
        toolbar.removeNavigation(nav); overlays.remove(overlay); select(null, false); renderedView = null;
    }
    private String supplies(Route route) {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (inventory == null) return "Waiting for inventory…";
        StringBuilder status = new StringBuilder("<html>");
        route.items.forEach((id, name) -> {
            boolean packed = inventory.contains(id) || equipment != null && equipment.contains(id);
            status.append(packed ? "✓ Packed · " : "Missing · ").append(name).append("<br>");
        });
        return status.append("</html>").toString();
    }

    @Subscribe public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()
            || event.getContainerId() == InventoryID.EQUIPMENT.getId()) suppliesDirty = true;
    }

    private boolean rockOnPad(WorldView world, WorldPoint pad) {
        LocalPoint local = LocalPoint.fromWorld(world, pad);
        if (local == null) return false;
        Tile tile = world.getScene().getTiles()[pad.getPlane()][local.getSceneX()][local.getSceneY()];
        if (tile == null) return false;
        java.util.List<TileItem> items = tile.getGroundItems();
        if (items != null) {
            for (TileItem item : items) if (item.getId() == ItemID.VT_USELESS_ROCK) return true;
        }
        return false;
    }

    static Route.Step padInstruction(WorldPoint player, boolean southRock, boolean northRock, Route.Step drop) {
        if (southRock) return player.equals(OTHER) ? OPEN : STAND_NORTH;
        if (northRock) return player.equals(PAD) ? OPEN : STAND_SOUTH;
        return drop;
    }

    private void publish(View current, String text) {
        if (text.equals(previousText)) return;
        previousText = text;
        SwingUtilities.invokeLater(() -> {
            if (view == current) panel.detail.setText(text);
        });
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) return;
        renderedView = null;
        View current = view;
        if (current != null) {
            current.target = null;
            previousText = null;
            publish(current, "Waiting for your character…");
        }
    }

    @Subscribe public void onGameTick(GameTick event) {
        View current = view;
        if (current == null) return;
        if (current != renderedView) {
            renderedView = current;
            renderedStep = null;
            previousText = null;
            suppliesDirty = true;
        }
        Player player = client.getLocalPlayer();
        if (client.getGameState() != GameState.LOGGED_IN || player == null) {
            current.target = null;
            publish(current, "Waiting for your character…");
            return;
        }
        if (!current.active) {
            if (suppliesDirty) {
                suppliesDirty = false;
                publish(current, supplies(current.route));
            }
            return;
        }
        WorldPoint location = player.getWorldLocation();
        Route.Step next = current.route.at(location);
        if (current.route == Route.KINGS && next.target.equals(PAD)) {
            WorldView world = player.getWorldView();
            boolean southRock = rockOnPad(world, PAD);
            next = padInstruction(location, southRock, !southRock && rockOnPad(world, OTHER), next);
        }
        if (current.route == Route.KINGS && location.distanceTo(ENTRANCE) <= 2) next = ARRIVED;
        if (view != current) return;
        current.target = next;
        if (next != renderedStep) {
            renderedStep = next;
            publish(current, "<html><body width='145'>" + next.text + "</body></html>");
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
            LocalPoint l=LocalPoint.fromWorld(world,s.target);
            if(l!=null) {
                Polygon tile=Perspective.getCanvasTilePoly(client,l);
                if(tile!=null) OverlayUtil.renderPolygon(g,tile,Color.CYAN);
            }
            int x=client.getViewportXOffset()+12,y=client.getViewportYOffset()+18;
            g.setColor(BACKGROUND); g.fillRect(x,y,g.getFontMetrics().stringWidth(s.text)+18,27);
            g.setColor(Color.WHITE); g.drawString(s.text,x+9,y+18); return null;
        }
    }
}
