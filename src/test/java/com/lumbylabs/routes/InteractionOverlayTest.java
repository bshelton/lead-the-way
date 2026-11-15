package com.lumbylabs.routes;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InteractionOverlayTest {
    private Widget widget(Client client, int id, String name, Rectangle bounds) {
        Widget widget = mock(Widget.class);
        when(widget.getBounds()).thenReturn(bounds);
        when(widget.getName()).thenReturn(name);
        when(client.getWidget(id)).thenReturn(widget);
        return widget;
    }
    private BufferedImage render(InteractionOverlay overlay) {
        BufferedImage image = new BufferedImage(765, 503, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { overlay.render(g); } finally { g.dispose(); }
        return image;
    }
    @Test public void hiddenControlsHighlightCorrectTabsInAllDesktopLayouts() {
        for (int mode = 0; mode < 3; mode++) {
            Client client = mock(Client.class);
            when(client.isResized()).thenReturn(mode > 0);
            when(client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT)).thenReturn(mode == 2 ? 1 : 0);
            int base = mode == 0 ? InterfaceID.Toplevel.STONE0 : mode == 1 ? InterfaceID.ToplevelOsrsStretch.STONE0 : InterfaceID.ToplevelPreEoc.STONE0;
            widget(client, base, "", new Rectangle(550, 450, 30, 30));
            widget(client, base + 3, "", new Rectangle(590, 450, 30, 30));
            widget(client, base + 5, "", new Rectangle(630, 450, 30, 30));
            AtomicReference<Route.Step> step = new AtomicReference<>(KingsGuide.DROP);
            InteractionOverlay overlay = new InteractionOverlay(client, step::get, () -> Prayer.PROTECT_FROM_MELEE);
            BufferedImage image = render(overlay);
            assertNotEquals(0, image.getRGB(600, 460));
            assertNotEquals(0, image.getRGB(640, 460));
            assertEquals(0, image.getRGB(560, 460));
            step.set(KingsGuide.SPECIAL);
            image = render(overlay);
            assertNotEquals(0, image.getRGB(560, 460));
            assertEquals(0, image.getRGB(600, 460));
            step.set(null);
            assertEquals(0, render(overlay).getRGB(640, 460));
        }
    }
    @Test public void visiblePrayerAndSpecialControlsAreHighlightedAndActivePrayerClears() throws Exception {
        Client client = mock(Client.class);
        // Deliberately place the named prayer at a non-default position.
        widget(client, InterfaceID.Prayerbook.PRAYER30, "<col=ff9040>Protect from Magic</col>", new Rectangle(580, 280, 35, 35));
        widget(client, InterfaceID.CombatInterface.SPECIAL_ATTACK, "", new Rectangle(550, 400, 150, 20));
        InteractionOverlay overlay = new InteractionOverlay(client, () -> KingsGuide.SPECIAL, () -> Prayer.PROTECT_FROM_MAGIC);
        BufferedImage image = render(overlay);
        assertNotEquals(0, image.getRGB(590, 290));
        assertNotEquals(0, image.getRGB(560, 410));
        when(client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)).thenReturn(true);
        assertEquals(0, render(overlay).getRGB(590, 290));
        ImageIO.write(image, "png", new File("build/interaction-overlay-test.png"));
    }
}
