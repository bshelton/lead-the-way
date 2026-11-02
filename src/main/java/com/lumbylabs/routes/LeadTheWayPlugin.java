package com.lumbylabs.routes;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.image.BufferedImage;
import net.runelite.api.*;
import net.runelite.api.coords.*;
import net.runelite.api.events.GameTick;
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
    volatile Route selected;
    volatile boolean guiding;
    volatile Route.Step step;
    private String previous = "";
    private static final WorldPoint PAD = Route.point("2490 10162 0"), OTHER = Route.point("2490 10164 0");
    private static final WorldPoint ENTRANCE = Route.point("1911 4367 0");
    @Override protected void startUp() {
        panel = new GuidePanel(this);
        BufferedImage icon = new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics(); g.setColor(new Color(246,189,76));
        g.fillRect(6,2,2,13); g.fillPolygon(new int[]{1,11,15,11,1},new int[]{3,3,6,9,9},5); g.dispose();
        nav = NavigationButton.builder().tooltip("Lead the Way").icon(icon).panel(panel).build();
        toolbar.addNavigation(nav); overlay = new SceneGuide(); overlays.add(overlay);
    }
    @Override protected void shutDown() {
        toolbar.removeNavigation(nav); overlays.remove(overlay); selected=null; guiding=false; step=null; previous="";
    }
    boolean has(int id) {
        for (InventoryID inv : new InventoryID[]{InventoryID.INVENTORY,InventoryID.EQUIPMENT}) {
            ItemContainer c = client.getItemContainer(inv);
            if (c != null && c.contains(id)) return true;
        }
        return false;
    }
    private boolean rockOnPad() {
        LocalPoint local = LocalPoint.fromWorld(client,PAD);
        if(local == null) return false;
        Tile tile = client.getScene().getTiles()[0][local.getSceneX()][local.getSceneY()];
        return tile != null && tile.getGroundItems() != null && tile.getGroundItems().stream().anyMatch(i -> i.getId()==ItemID.VT_USELESS_ROCK);
    }
    @Subscribe public void onGameTick(GameTick event) {
        Route route=selected;
        if(route==null || client.getLocalPlayer()==null) { previous=""; step=null; return; }
        WorldPoint p=client.getLocalPlayer().getWorldLocation();
        Route.Step next=route.at(p);
        if(route==Route.KINGS && next.target.equals(PAD) && rockOnPad()) {
            next=p.equals(OTHER) ? new Route.Step(OTHER,"Open the pressure door.") : new Route.Step(OTHER,"Stand on the other marked pad.");
        }
        if(route==Route.KINGS && p.distanceTo(ENTRANCE)<=2) next=new Route.Step(ENTRANCE,"Arrived at the Kings’ entrance.");
        step=guiding ? next : null;
        StringBuilder status=new StringBuilder("<html>");
        for(var entry : route.items.entrySet()) status.append(has(entry.getKey()) ? "✓ Packed · " : "Missing · ").append(entry.getValue()).append("<br>");
        String text=guiding ? "<html>"+next.text+"</html>" : status.append("</html>").toString();
        if(!text.equals(previous)) {
            previous=text; boolean active=guiding;
            SwingUtilities.invokeLater(() -> { if(selected==route && active==guiding) panel.detail.setText(text); });
        }
    }
    private final class SceneGuide extends Overlay {
        SceneGuide() { setPosition(OverlayPosition.DYNAMIC); setLayer(OverlayLayer.ABOVE_SCENE); }
        public Dimension render(Graphics2D g) {
            Route.Step s=step;
            if(!guiding || s==null || client.getGameState()!=GameState.LOGGED_IN) return null;
            LocalPoint l=LocalPoint.fromWorld(client,s.target);
            if(l!=null && client.getPlane()==s.target.getPlane()) {
                Polygon tile=Perspective.getCanvasTilePoly(client,l);
                if(tile!=null) OverlayUtil.renderPolygon(g,tile,Color.CYAN);
            }
            int x=client.getViewportXOffset()+12,y=client.getViewportYOffset()+18;
            g.setColor(new Color(24,24,24,222)); g.fillRect(x,y,g.getFontMetrics().stringWidth(s.text)+18,27);
            g.setColor(Color.WHITE); g.drawString(s.text,x+9,y+18); return null;
        }
    }
}
