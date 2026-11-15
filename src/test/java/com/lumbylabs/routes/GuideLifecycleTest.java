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
        inject("mapPoints", mock(net.runelite.client.ui.overlay.worldmap.WorldMapPointManager.class));
        inject("nav", net.runelite.client.ui.NavigationButton.builder().tooltip("Guide").icon(
            new java.awt.image.BufferedImage(16,16,java.awt.image.BufferedImage.TYPE_INT_ARGB)).build());
        SwingUtilities.invokeAndWait(() -> panel = new GuidePanel(plugin));
        inject("panel", panel);
        Player player = mock(Player.class);
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        when(client.getLocalPlayer()).thenReturn(player);
        when(player.getWorldLocation()).thenReturn(Route.point("2549 3758 0"));
        when(client.getItemContainer(InventoryID.INVENTORY)).thenReturn(mock(ItemContainer.class));
        clearInvocations(client);
    }
    @Test public void hillGiantsBankedKeyIsUsableOnlyAfterWithdrawal() throws Exception {
        Player player = client.getLocalPlayer();
        when(player.getWorldLocation()).thenReturn(Route.point("3164 3487 0"));
        net.runelite.api.widgets.Widget widget = mock(net.runelite.api.widgets.Widget.class);
        int bankWidget = net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS;
        when(client.getWidget(bankWidget)).thenReturn(widget);
        ItemContainer bank = mock(ItemContainer.class);
        when(client.getItemContainer(InventoryID.BANK)).thenReturn(bank);
        when(bank.count(net.runelite.api.gameval.ItemID.EDGEVILLEDUNGEONKEY)).thenReturn(1);
        plugin.select(Route.HILL_GIANTS, true);
        ticks(1);
        when(client.getWidget(bankWidget)).thenReturn(null);
        ticks(1);
        assertTrue(panel.detail.getText().contains("trapdoor"));
        ItemContainer bag = client.getItemContainer(InventoryID.INVENTORY);
        when(bag.contains(net.runelite.api.gameval.ItemID.EDGEVILLEDUNGEONKEY)).thenReturn(true);
        plugin.onItemContainerChanged(new net.runelite.api.events.ItemContainerChanged(InventoryID.INVENTORY.getId(), bag));
        ticks(1);
        assertTrue(panel.detail.getText().contains("shed"));
        when(player.getWorldLocation()).thenReturn(Route.point("3094 3491 0"));
        ticks(1);
        assertTrue(panel.detail.getText().contains("trapdoor"));
        when(player.getWorldLocation()).thenReturn(Route.point("3096 9867 0"));
        ticks(1);
        assertTrue(panel.detail.getText().contains("north"));
        when(player.getWorldLocation()).thenReturn(Route.point("3110 9837 0"));
        ticks(1);
        assertTrue(panel.detail.getText().contains("reached"));
    }
    @Test public void detailedKingsInstructionsFitTheCanvasBanner() throws Exception {
        Class<?> type = Class.forName(LeadTheWayPlugin.class.getName() + "$InstructionBanner");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(LeadTheWayPlugin.class);
        constructor.setAccessible(true);
        net.runelite.client.ui.overlay.Overlay banner =
            (net.runelite.client.ui.overlay.Overlay) constructor.newInstance(plugin);
        inject("showBanner", true);
        LeadTheWayPlugin.View view = new LeadTheWayPlugin.View(Route.KINGS, true);
        inject("view", view);
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            800, 600, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        try {
            for (int canvasWidth : new int[]{240, 765}) {
                when(client.getCanvasWidth()).thenReturn(canvasWidth);
                for (Route.Stage stage : Route.KINGS.stages) {
                    view.target = stage.next;
                    java.awt.Dimension size = banner.render(g);
                    assertTrue(stage.milestone, size.width <= canvasWidth - 18);
                    assertTrue(stage.milestone, size.height <= image.getHeight());
                    if (stage.milestone.equals("Open the pressure door")) {
                        assertTrue("Detailed instructions must wrap", size.height > g.getFontMetrics().getHeight() + 10);
                    }
                }
            }
        } finally { g.dispose(); }
    }
    @Test public void livePadCueUpdatesFromSceneAndClearsOnBankLogoutAndEnd() throws Exception {
        WorldView world = mock(WorldView.class);
        Scene scene = mock(Scene.class);
        when(world.getScene()).thenReturn(scene);
        when(world.getBaseX()).thenReturn(2432);
        when(world.getBaseY()).thenReturn(10112);
        when(world.getSizeX()).thenReturn(104);
        when(world.getSizeY()).thenReturn(104);
        Tile[][][] tiles = new Tile[4][104][104];
        when(scene.getTiles()).thenReturn(tiles);
        Player player = client.getLocalPlayer();
        when(player.getWorldView()).thenReturn(world);
        when(player.getWorldLocation()).thenReturn(KingsGuide.SOUTH);
        ItemContainer bag = client.getItemContainer(InventoryID.INVENTORY);
        when(bag.contains(net.runelite.api.gameval.ItemID.VT_USELESS_ROCK)).thenReturn(true);
        plugin.select(Route.KINGS, true);
        ticks(1);
        assertSame(KingsGuide.DROP, plugin.interactionStep());
        Tile pad = mock(Tile.class);
        TileItem rock = mock(TileItem.class);
        when(rock.getId()).thenReturn(net.runelite.api.gameval.ItemID.VT_USELESS_ROCK);
        when(pad.getGroundItems()).thenReturn(java.util.List.of(rock));
        tiles[0][58][50] = pad;
        when(bag.contains(net.runelite.api.gameval.ItemID.VT_USELESS_ROCK)).thenReturn(false);
        ticks(1);
        assertSame(KingsGuide.STAND_NORTH, plugin.interactionStep());
        when(player.getWorldLocation()).thenReturn(KingsGuide.NORTH);
        ticks(1);
        assertSame(KingsGuide.OPEN, plugin.interactionStep());
        Tile door = mock(Tile.class);
        WallObject object = mock(WallObject.class);
        when(object.getId()).thenReturn(net.runelite.api.gameval.ObjectID.DAGANNOTH_PRESSURE_OPEN);
        when(door.getWallObject()).thenReturn(object);
        tiles[0][59][51] = door;
        ticks(1);
        assertSame(KingsGuide.TAKE_SOUTH, plugin.interactionStep());
        when(pad.getGroundItems()).thenReturn(java.util.List.of());
        when(bag.contains(net.runelite.api.gameval.ItemID.VT_USELESS_ROCK)).thenReturn(true);
        ticks(1);
        assertSame(KingsGuide.CROSS, plugin.interactionStep());
        net.runelite.api.widgets.Widget bank = mock(net.runelite.api.widgets.Widget.class);
        when(client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS)).thenReturn(bank);
        ticks(1);
        assertNull(plugin.interactionStep());
        when(client.getWidget(net.runelite.api.gameval.InterfaceID.Bankmain.ITEMS)).thenReturn(null);
        ticks(1);
        assertSame(KingsGuide.CROSS, plugin.interactionStep());
        when(client.getGameState()).thenReturn(GameState.LOGIN_SCREEN);
        ticks(1);
        assertNull(plugin.interactionStep());
        when(client.getGameState()).thenReturn(GameState.LOGGED_IN);
        plugin.select(null, false);
        assertNull(plugin.interactionStep());
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
