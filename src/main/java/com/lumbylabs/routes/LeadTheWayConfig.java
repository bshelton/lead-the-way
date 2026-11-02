package com.lumbylabs.routes;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("leadtheway")
public interface LeadTheWayConfig extends Config {
    @ConfigItem(keyName = "showCanvasHelper", name = "Show canvas helper",
        description = "Show the movable route instruction banner over the game.")
    default boolean showCanvasHelper() {
        return true;
    }
}
