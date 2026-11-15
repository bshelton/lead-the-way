package com.lumbylabs.routes;

import java.awt.*;
import java.util.function.Supplier;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

/** Highlights visible controls, or the tab needed to reveal them. Never performs an action. */
final class InteractionOverlay extends Overlay {
    private final Client client;
    private final Supplier<Route.Step> step;
    private final Supplier<Prayer> prayer;
    static final Color ACTION = new Color(255, 200, 60);

    InteractionOverlay(Client client, Supplier<Route.Step> step, Supplier<Prayer> prayer) {
        this.client = client; this.step = step; this.prayer = prayer;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    static void highlight(Graphics2D g, Rectangle bounds, String label) {
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return;
        g.setColor(new Color(255, 200, 60, 45));
        g.fill(bounds);
        g.setStroke(new BasicStroke(2));
        g.setColor(ACTION); g.draw(bounds);
        if (!label.isEmpty()) {
            int width = g.getFontMetrics().stringWidth(label) + 6;
            int height = g.getFontMetrics().getHeight();
            int y = Math.max(height, bounds.y);
            g.setColor(new Color(24, 24, 24, 235));
            int x = Math.max(0, bounds.x + Math.min(0, bounds.width - width));
            g.fillRect(x, y - height, width, height);
            g.setColor(ACTION);
            g.drawString(label, x + 3, y - g.getFontMetrics().getDescent());
        }
    }

    private boolean control(Graphics2D g, Widget widget, String label) {
        if (widget == null || widget.isHidden()) return false;
        highlight(g, widget.getBounds(), label);
        return true;
    }

    private void tab(Graphics2D g, int index, String label) {
        int base = !client.isResized() ? InterfaceID.Toplevel.STONE0
            : client.getVarbitValue(net.runelite.api.gameval.VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
                ? InterfaceID.ToplevelPreEoc.STONE0 : InterfaceID.ToplevelOsrsStretch.STONE0;
        control(g, client.getWidget(base + index), label);
    }

    private Widget prayerWidget(Prayer needed) {
        String name = KingsGuide.prayerName(needed);
        // Prayer widgets retain their names when reordered; do not assume a screen position.
        for (int id = InterfaceID.Prayerbook.PRAYER1; id <= InterfaceID.Prayerbook.PRAYER30; id++) {
            Widget widget = client.getWidget(id);
            if (widget != null && !widget.isHidden() && widget.getName() != null
                && widget.getName().contains(name)) return widget;
        }
        return null;
    }

    @Override public Dimension render(Graphics2D graphics) {
        Route.Step current = step.get();
        if (current == null) return null;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Prayer needed = prayer.get();
            if (needed != null && !client.isPrayerActive(needed)) {
                if (!control(g, prayerWidget(needed), KingsGuide.prayerName(needed))) tab(g, 5, "Prayer");
            }
            if (KingsGuide.inventoryItem(current) >= 0) {
                Widget inventory = client.getWidget(InterfaceID.Inventory.ITEMS);
                if (inventory == null || inventory.isHidden()) tab(g, 3, "Inventory");
            }
            if (current == KingsGuide.SPECIAL) {
                if (!control(g, client.getWidget(InterfaceID.CombatInterface.SPECIAL_ATTACK), "Special attack")) tab(g, 0, "Combat options");
            }
        } finally { g.dispose(); }
        return null;
    }
}
