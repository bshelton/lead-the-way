# Lead the Way
A RuneLite destination guide by Lumby Labs. The first development route follows the northern solo path from Rellekka docks through Waterbirth Island Dungeon to the Dagannoth Kings entrance.

Open the signpost sidebar, search for **DKs** or **Rex**, select **Dagannoth Kings**, check the pet rock and rune thrownaxe checklist, and choose **Guide me**. The overlay gives one instruction at a time and marks the target tile when it comes into view. Close the sidebar to keep following the route. Use **Close route** in the panel to stop. Bring your own combat supplies.

## Run locally
Use JDK 21. Run `./gradlew build` to compile and test, then `./gradlew run` to launch the development client. On Windows use `gradlew.bat`. [Development client login](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

## Route development
Register a destination in Route.CATALOG with a name, search aliases, optional item requirements, start instruction and a route resource. Each resource line contains two opposite corners of a rectangular room, a target tile and an instruction separated by vertical bars. Coordinates are x, y and plane. The player’s current room selects the step, including when starting mid-route. The Kings route also checks for a dropped rock on the marked pad.

Dungeon coordinates reference [Quest Helper](https://github.com/Zoinkwiz/quest-helper). Its license is included in notices. This prototype needs a live traversal. Targets mark tiles, and guidance begins at the docks. It does not calculate walking paths from arbitrary starting locations.
