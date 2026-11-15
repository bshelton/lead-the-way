# Development

Use JDK 21 and the checked-in Gradle wrapper. Run `./gradlew clean build` for the Java 11 plugin jar and JUnit suite. Run `./gradlew run` to start the development client. On Windows use `.\gradlew.bat`. [Jagex Account login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Route data

The catalog in `Route.java` defines search aliases, route length, membership, minimum Agility and portraits. Each line of a route resource contains two opposite corners of a room, a target tile, an instruction and a milestone, separated by vertical bars. Each coordinate is x, y and plane. Room bounds are inclusive. Earlier entries take precedence when rooms overlap.

Use an object’s tile for an interaction marker. A walking dataset may instead identify the tile where the player stands. Some stages need an additional signal, such as the brass key appearing in inventory. Keep the suggested entrance, bank checklist, step list and marker consistent when adding a branch.

## Checks before submitting a change

Run the build and check the sidebar at 170 and 225 logical pixels. Exercise each changed route from the start and from a mid-route login. Check bank-open and bank-closed behavior, switching destinations, logout, and cleanup after ending guidance. The automated suite covers route selection, item requirements, lifecycle and sidebar layout. In-game checks are still needed for scene targets and transport behavior.

## Sources

Waterbirth room coordinates reference [Quest Helper](https://github.com/Zoinkwiz/quest-helper), with its license in [notices](../notices/LICENSE). Hill Giants waypoints were checked against [Shortest Path walking data](https://github.com/Skretzo/shortest-path) and the [brass key spawn](https://oldschool.runescape.wiki/w/Brass_key). The Hill Giants icon uses the ensouled giant head [item image](https://static.runelite.net/cache/item/icon/13475.png). Dagannoth portraits load from RuneLite’s game sprite cache.

Kings route instructions use the [Waterbirth dungeon walkthrough](https://oldschool.runescape.wiki/w/Waterbirth_Island_Dungeon) for room prayers and the [Waterbirth Island guide](https://oldschool.runescape.wiki/w/Waterbirth_Island) for recovering the pet rock before passing through the pressure door. Ladder instructions distinguish the current room prayer from the prayer to activate at the next ladder. Protect from Missiles assumes running through mixed dagannoth rooms.

Kings stages optionally include two extra fields: the current-room prayer and the next-room prayer (RuneLite `Prayer` names). The next-room cue applies within one tile of the ladder. `KingsGuide` resolves pressure-door and thrownaxe actions from ground items, the open-door object, equipment, special energy, and support NPC state. Inventory highlights use `WidgetItemOverlay`; prayer and special-attack controls use `InteractionOverlay`, with tab cues when their controls are hidden. Ending guidance, opening the bank, and logout suppress interaction highlights. The final corridor changes to Magic near the southwest bend; these scene boundaries still need in-game verification.
