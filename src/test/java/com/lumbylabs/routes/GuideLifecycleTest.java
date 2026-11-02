package com.lumbylabs.routes;

import java.lang.reflect.Field;
import javax.swing.SwingUtilities;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class GuideLifecycleTest {
    private final Client client = mock(Client.class);
    private final LeadTheWayPlugin plugin = new LeadTheWayPlugin();
    private GuidePanel panel;

    @Before public void setup() throws Exception {
        inject("client", client);
        SwingUtilities.invokeAndWait(() -> panel = new GuidePanel(plugin));
        inject("panel", panel);
        Player player = mock(Player.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(Route.point("2549 3758 0"));
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(mock(ItemContainer.class));
        clearInvocations(client);
    }
    private void inject(String name, Object value) throws Exception {
        Field field = LeadTheWayPlugin.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(plugin, value);
    }
    private void ticks(int count) throws Exception {
        for (int i = 0; i < count; i++) plugin.onGameTick(new GameTick());
        SwingUtilities.invokeAndWait(() -> {});
    }
    @Test public void idleAndActiveGuideDoNotReadSupplies() throws Exception {
        ticks(100);
        verifyNoInteractions(client);
        plugin.select(Route.KINGS, true);
        ticks(100);
        verify(client, never()).getItemContainer(any(InventoryID.class));
        assertTrue(panel.detail.getText().contains("Enter the cave"));
    }
    @Test public void previewReadsOnceAndQuickReopenRestoresChecklist() throws Exception {
        plugin.select(Route.KINGS, false);
        ticks(100);
        verify(client, times(1)).getItemContainer(InventoryID.INVENTORY);
        SwingUtilities.invokeAndWait(() -> {
            plugin.select(null, false);
            plugin.select(Route.KINGS, false);
            panel.detail.setText("Waiting");
        });
        ticks(1);
        assertTrue(panel.detail.getText().contains("Pet rock"));
    }
    @Test public void staleSwingUpdateCannotReplaceNewView() throws Exception {
        plugin.select(Route.KINGS, false);
        SwingUtilities.invokeAndWait(() -> {
            plugin.onGameTick(new GameTick()); // Queue the old preview callback behind this event.
            plugin.select(null, false);
            plugin.select(Route.KINGS, false);
            panel.detail.setText("Fresh preview");
        });
        SwingUtilities.invokeAndWait(() -> assertEquals("Fresh preview", panel.detail.getText()));
        ticks(1);
        assertTrue(panel.detail.getText().contains("Pet rock"));
    }
    @Test public void inventoryChangesRefreshPreviewAndLogoutClearsOldDirections() throws Exception {
        plugin.select(Route.KINGS, false);
        ticks(1);
        net.runelite.api.events.ItemContainerChanged change =
            new net.runelite.api.events.ItemContainerChanged(InventoryID.EQUIPMENT.getId(), mock(ItemContainer.class));
        plugin.onItemContainerChanged(change);
        ticks(1);
        verify(client, times(2)).getItemContainer(InventoryID.INVENTORY);
        plugin.select(Route.KINGS, true);
        ticks(1);
        net.runelite.api.events.GameStateChanged logout = new net.runelite.api.events.GameStateChanged();
        logout.setGameState(GameState.LOGIN_SCREEN);
        plugin.onGameStateChanged(logout);
        SwingUtilities.invokeAndWait(() -> assertTrue(panel.detail.getText().contains("Waiting")));
        ticks(1);
        assertTrue(panel.detail.getText().contains("Enter the cave"));
    }
    @Test public void rockOnEitherPadGuidesPlayerToOppositePadThenDoor() {
        WorldPoint south = Route.point("2490 10162 0"), north = Route.point("2490 10164 0");
        Route.Step drop = Route.KINGS.at(south);
        assertSame(drop, LeadTheWayPlugin.padInstruction(south,false,false,drop));
        assertEquals(north, LeadTheWayPlugin.padInstruction(south,true,false,drop).target);
        assertEquals(south, LeadTheWayPlugin.padInstruction(north,false,true,drop).target);
        assertTrue(LeadTheWayPlugin.padInstruction(north,true,false,drop).text.contains("Open"));
        assertTrue(LeadTheWayPlugin.padInstruction(south,false,true,drop).text.contains("Open"));
    }
}
