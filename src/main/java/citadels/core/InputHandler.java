package citadels.core;

import citadels.app.App;
import citadels.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Handles user input and command processing during the game.
 * Also manages the 'null turn' flag used in situations where a turn is skipped or no valid action occurs.
 */
public class InputHandler {
    private final Game game; // reference to the main Game instance
    private boolean nullTurnProcessed = false; // flag to track if a null turn has been processed

    /**
     * Constructor that attaches this input handler to the current game.
     *
     * @param game the main Game object
     */
    public InputHandler(Game game) {
        this.game = game;
    }

    /**
     * Returns a new Scanner that reads from the current App.input stream.
     * This ensures test input is correctly picked up instead of System.in.
     *
     * @return a fresh Scanner bound to App.input
     */
    protected Scanner getScanner() {
        return new Scanner(App.input);
    }

    /**
     * Resets the null turn flag so that it's ready for the next use.
     * This should be called before a new phase or turn begins.
     */
    public void resetNullTurnFlag() {
        nullTurnProcessed = false;
    }

    /**
     * Checks whether a null turn has already been processed.
     * Used to avoid repeating actions during a turn that has already been skipped.
     *
     * @return true if the null turn was already processed this round, false otherwise
     */
    public boolean isNullTurnProcessed() {
        return nullTurnProcessed;
    }

    /**
     * Handles a single line of input entered by the player (or tester).
     * This will respond to game commands typed during a player's turn,
     * and supports both human and AI logic handling.
     *
     * @param scanner the scanner used to read user input
     */
    public void processInput(Scanner scanner) {
        Player humanPlayer = game.getPlayerByNumber(1); // player 1 is always the human
        Player currentPlayer = game.getCurrentPlayer();

        boolean isAIorNull = (currentPlayer == null) || (currentPlayer instanceof AIPlayer);
        boolean isHuman = (currentPlayer != null) && !(currentPlayer instanceof AIPlayer);

        System.out.print("> ");
        String input = scanner.nextLine().trim(); // raw input from player
        String lowerInput = input.toLowerCase();  // normalize for easier matching

        // For AI players or when currentPlayer is null, only allow 't' to proceed
        if (isAIorNull) {
            if (lowerInput.equals("t")) {
                if (currentPlayer != null) {
                    currentPlayer.setTurnCompleted(true);
                }
                nullTurnProcessed = true; // flag so we don’t run a null turn more than once
                return;
            }
        } else {
            // Human commands

            // Ends the current player's turn when typed
            if (lowerInput.equals("end")) {
                currentPlayer.setTurnCompleted(true);
                System.out.println("You ended your turn.");
                GameUtils.delay(1000);
                System.out.println();
                return;
            }

            if (lowerInput.equals("t")) {
                System.out.println("Your turn.");
                return;
            }

            // Purple district actions - only available to human player
            if (lowerInput.equals("discard") || lowerInput.startsWith("discard ")) {
                handleLaboratory(currentPlayer, lowerInput); // LABORATORY
                return;
            }

            if (lowerInput.equals("draw") || lowerInput.startsWith("draw ")) {
                handleSmithy(currentPlayer); // SMITHY
                return;
            }

            if (lowerInput.startsWith("place") || lowerInput.startsWith("place ")) {
                handleMuseum(currentPlayer, lowerInput); // MUSEUM
                return;
            }

            if (lowerInput.equals("armory") || lowerInput.startsWith("armory ")) {
                handleArmory(currentPlayer, scanner); // ARMORY
                return;
            }
        }

        // Common commands for all players
        switch (lowerInput.split(" ")[0]) {
            case "hand":
                System.out.print(GameUtils.getDebugStatusString(humanPlayer));
                break;

            case "gold":
                String[] goldTokens = input.split(" ");
                Player goldPlayer;

                // No player number provided, assume the command is targeting yourself
                if (goldTokens.length == 1) {
                    goldPlayer = humanPlayer; // no player number provided
                } else {
                    try {
                        int playerNum = Integer.parseInt(goldTokens[1]);
                        goldPlayer = game.getPlayerByNumber(playerNum);
                        // Invalid player number — no matching player found
                        if (goldPlayer == null) {
                            System.out.println("Player " + playerNum + " not found.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid player number.");
                        break;
                    }
                }

                System.out.println(goldPlayer.getName() + ": gold = " + goldPlayer.getGold());
                break;

            case "citadel":
            case "list":
            case "city":
                String[] tokens = input.split(" ");
                Player targetPlayer;

                // Default to human player if no player number is specified
                if (tokens.length == 1) {
                    targetPlayer = humanPlayer; // default to player 1
                } else {
                    try {
                        int playerNum = Integer.parseInt(tokens[1]);
                        targetPlayer = game.getPlayerByNumber(playerNum);
                        if (targetPlayer == null) {
                            System.out.println("Player " + playerNum + " not found.");
                            break;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid player number.");
                        break;
                    }
                }

                System.out.println(targetPlayer.getName() + ":");
                System.out.println("City:");

                List<DistrictCard> cityDistricts = game.getCityForPlayer(targetPlayer);
                if (cityDistricts.isEmpty()) {
                    System.out.println("(No districts built)");
                } else {
                    for (int i = 0; i < cityDistricts.size(); i++) {
                        GameUtils.printDistrictCard(cityDistricts.get(i), i);
                    }
                }
                break;

            case "action":
                String[] parts = input.split(" ", 3); // limit to 3 parts max
                // Just show ability info without doing anything
                if (parts.length == 1) {
                    System.out.println(game.getCharacterAbilityInfo(humanPlayer));
                } else {
                    if (!(currentPlayer instanceof HumanPlayer)) {
                        System.out.println("You can only do character actions on your turn.");
                        break;
                    }

                    String subCommand = parts[1];
                    // Handles Magician ability to swap hands or discard and redraw
                    if (subCommand.equals("swap") || subCommand.equals("redraw")) {
                        handleMagician(currentPlayer, parts); // Magician actions
                        // Handles Warlord's destroy ability targeting another player's district
                    } else if (subCommand.equals("destroy") && parts.length == 3) {
                        handleWarlordDestroy(currentPlayer, parts[2]); // Warlord destroy
                    } else {
                        System.out.println("Invalid action sub-command. Use 'swap <player_number>', 'redraw <cardID1,cardID2,...>', or 'destroy <player_number> <districtID>'.");
                    }
                }
                break;

            case "info":
                String[] infoParts = input.split(" ", 2);
                if (infoParts.length < 2) {
                    System.out.println("Use 'info <character_name>' or 'info <districtID>'.");
                    break;
                }

                String param = infoParts[1].trim().toLowerCase();

                // Attempt to parse the district number for info command
                try {
                    int districtIdx = Integer.parseInt(param) - 1; // shift from 1-indexed
                    handleDistrictInfo(humanPlayer, districtIdx);
                } catch (NumberFormatException e) {
                    handleCharacterInfo(param); // not a number? treat as character name
                }
                break;

            case "all":
                List<Player> allPlayers = game.getPlayers();

                // Show built districts for each player
                for (Player player : allPlayers) {
                    String youText = player == humanPlayer ? " (you)" : "";
                    System.out.printf("%s%s: cards = %d gold = %d%n",
                            player.getName(), youText, player.getHand().size(), player.getGold());

                    System.out.println("City:");
                    List<DistrictCard> built = game.getCityForPlayer(player);
                    if (built.isEmpty()) {
                        System.out.println("(No districts built)");
                    } else {
                        for (int i = 0; i < built.size(); i++) {
                            GameUtils.printDistrictCard(built.get(i), i);
                        }
                    }
                    System.out.println();
                }
                break;

            case "save": {
                // Split input into "save" and the filename
                String[] saveParts = input.split(" ", 2);
                if (saveParts.length < 2) {
                    System.out.println("Usage: save <filename.json>");
                    break;
                }

                String filename = saveParts[1].trim();

                // strip "quotes" if used
                if (filename.startsWith("\"") && filename.endsWith("\"")) {
                    filename = filename.substring(1, filename.length() - 1);
                }

                // Disallow filenames with spaces or unsafe characters
                if (filename.contains(" ") || filename.contains("(") || filename.contains("{")) {
                    System.out.println("Error: Invalid filename. Do not paste code or use special characters.");
                    break;
                }

                // Automatically add .json extension if it's missing
                if (!filename.toLowerCase().endsWith(".json")) {
                    System.out.println("Error: Filename must end with '.json'");
                    break;
                }

                try {
                    game.saveGame(filename);
                    System.out.println("Game saved successfully to " + filename);
                } catch (Exception e) {
                    System.out.println("Failed to save game: " + e.getMessage());
                }
                break;
            }

            case "load": {
                // Split input into "load" and the filename
                String[] loadParts = input.split(" ", 2);
                if (loadParts.length < 2) {
                    System.out.println("Usage: load <filename.json>");
                    break;
                }

                String filename = loadParts[1].trim();

                // Require .json extension for loading
                if (!filename.toLowerCase().endsWith(".json")) {
                    System.out.println("Error: Filename must end with '.json'");
                    break;
                }

                try {
                    game.loadGame(filename);
                    System.out.println("Save loaded successfully.");

                    // Only run game loop if we're not testing
                    if (!GameUtils.isTestMode()) {
                        game.runPhase();
                    }

                    return;
                } catch (Exception e) {
                    System.out.println("Failed to load game: " + e.getMessage());
                    return;
                }
            }

            case "help":
                System.out.println("Available commands:");
                System.out.println("info : Show information about a character or building");
                System.out.println("t : Processes turns");
                System.out.println("all : Shows all current game info");
                System.out.println("citadel/list/city : Shows districts built by a player");
                System.out.println("hand : Shows cards in hand");
                System.out.println("gold [p] : Shows gold of a player");
                System.out.println("build <place in hand> : Builds a building into your city");
                System.out.println("action : Gives info about your special action and how to perform it");
                System.out.println("end : Ends your turn");
                System.out.println("save <file> : Saves your game");
                System.out.println("load <file> : Loads your game");
                break;

            case "debug":
                GameUtils.toggleDebugMode(); // show/hide AI player hands
                break;

            case "build":
                handleBuild(currentPlayer, input.split(" "), isHuman);
                break;

            default:
                System.out.println("Invalid command. Type 'help' to see available commands.");
                break;
        }
    }

    /**
     * Uses the Laboratory to discard a card and gain 1 gold.
     * Only works if the player owns a Laboratory and hasn't used it this turn.
     *
     * @param currentPlayer the player trying to use the Laboratory
     * @param input         the full user command, e.g. "discard 2"
     */
    private void handleLaboratory(Player currentPlayer, String input) {
        HumanPlayer player = (HumanPlayer) currentPlayer;

        // Make sure player actually has the Laboratory built
        if (!player.hasLaboratory()) {
            System.out.println("You don't have the Laboratory.");
            return;
        }

        // Laboratory can only be used once per turn
        if (player.hasUsedLaboratory()) {
            System.out.println("You've already used the Laboratory this turn.");
            return;
        }

        String[] parts = input.split(" ");
        if (parts.length != 2) {
            System.out.println("Use: discard <number>");
            return;
        }

        try {
            int cardIndex = Integer.parseInt(parts[1]) - 1; // Convert from 1-based to 0-based index
            List<DistrictCard> hand = player.getHand();

            // Prevent out-of-bounds errors from bad input
            if (cardIndex < 0 || cardIndex >= hand.size()) {
                System.out.println("Invalid card number.");
                return;
            }

            DistrictCard card = hand.get(cardIndex);
            player.useLaboratory(cardIndex);
            System.out.println("You discarded " + card.getName() + " and gained 1 gold using the Laboratory.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Use: discard <number>");
        }
    }

    /**
     * Uses the Smithy to draw 3 cards at the cost of 2 gold.
     * Only works if the player owns the Smithy and has enough gold.
     *
     * @param currentPlayer the player attempting to use the Smithy
     */
    private void handleSmithy(Player currentPlayer) {
        HumanPlayer player = (HumanPlayer) currentPlayer;

        // Check that Smithy is built before allowing ability use
        if (!player.hasSmithy()) {
            System.out.println("You don't have the Smithy.");
            return;
        }

        // Smithy requires 2 gold to activate
        if (player.hasUsedSmithy()) {
            System.out.println("You've already used the Smithy this turn.");
            return;
        }

        if (player.getGold() < 2) {
            System.out.println("You need at least 2 gold to use the Smithy.");
            return;
        }

        int initialHandSize = player.getHand().size();
        player.useSmithy();
        int drawn = player.getHand().size() - initialHandSize;

        System.out.println("You paid 2 gold and drew " + drawn + " card" + (drawn == 1 ? "" : "s") + " using the Smithy.");
    }

    /**
     * Places a card from hand into the Museum for bonus points.
     * Card is removed from hand and stored — cannot be used again.
     *
     * @param currentPlayer the player using the Museum
     * @param input         the full command like "place 3"
     */
    private void handleMuseum(Player currentPlayer, String input) {
        HumanPlayer player = (HumanPlayer) currentPlayer;

        // Don’t allow Museum ability if it hasn't been built
        if (!player.hasMuseum()) {
            System.out.println("You don't have the Museum.");
            return;
        }

        // Museum can only be used once per turn
        if (player.hasUsedMuseum()) {
            System.out.println("You've already used the Museum this turn.");
            return;
        }

        String[] parts = input.split(" ");
        if (parts.length != 2) {
            System.out.println("Use: place <number>");
            return;
        }

        try {
            int cardIndex = Integer.parseInt(parts[1]) - 1;
            List<DistrictCard> hand = player.getHand();

            // Avoid invalid card index errors
            if (cardIndex < 0 || cardIndex >= hand.size()) {
                System.out.println("Invalid card number.");
                return;
            }

            DistrictCard chosen = hand.remove(cardIndex);
            player.storeInMuseum(chosen);
            System.out.println("You placed " + chosen.getName() + " in the Museum. It will score 1 point at the end of the game.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format. Use: place <number>");
        }
    }

    /**
     * Activates the Armory effect.
     * Destroys your own Armory to destroy another player’s district of your choice.
     *
     * @param currentPlayer the player using the Armory
     */
    private void handleArmory(Player currentPlayer, Scanner scanner) {
        HumanPlayer player = (HumanPlayer) currentPlayer;
        List<DistrictCard> city = game.getCityForPlayer(player);

        DistrictCard armory = null;
        for (DistrictCard d : city) {
            if (d.getName().equalsIgnoreCase("Armory")) {
                armory = d;
                break;
            }
        }

        // If no Armory is built, abort the action
        if (armory == null) {
            System.out.println("You don't have the Armory built.");
            return;
        }

        // Must be at least one other targetable player
        boolean foundTarget = game.getPlayers().stream()
                .anyMatch(other -> other != player && !game.getCityForPlayer(other).isEmpty());

        // Player didn’t select a valid target
        if (!foundTarget) {
            System.out.println("No other player has districts you can destroy.");
            return;
        }

        // Destroy Armory from own city
        city.remove(armory);
        System.out.println("You destroyed your Armory.");
        System.out.println("You may now destroy any district card in another player's city.");
        System.out.println("Use: <player_number> <districtID>");

        // Keep looping until valid destroy input is given or cancelled
        while (true) {
            System.out.print("> ");
            String destroyInput = scanner.nextLine().trim();
            String[] tokens = destroyInput.split(" ");

            // Make sure both player number and district number are provided
            if (tokens.length != 2) {
                System.out.println("Invalid format. Use: <player_number> <districtID>");
                continue;
            }

            // Try parsing the player and district numbers
            try {
                int targetPlayerNum = Integer.parseInt(tokens[0]);
                int districtIndex = Integer.parseInt(tokens[1]) - 1;

                Player target = game.getPlayerByNumber(targetPlayerNum);
                // Ensure the target is valid and not the current player
                if (target == null || target == player) {
                    System.out.println("Invalid target player.");
                    continue;
                }

                List<DistrictCard> targetCity = game.getCityForPlayer(target);
                // Can't destroy from an empty city
                if (targetCity.isEmpty()) {
                    System.out.println("That player has no districts to destroy.");
                    continue;
                }

                // Check if the specified district index is within bounds
                if (districtIndex < 0 || districtIndex >= targetCity.size()) {
                    System.out.println("Invalid district index.");
                    continue;
                }

                // true = Armory ignores cost restrictions
                player.destroyDistrict(target, districtIndex, true);
                break;

                // If parsing failed, show an error and loop again
            } catch (NumberFormatException e) {
                System.out.println("Invalid number format. Use: <player_number> <districtID>");
            }
        }
    }

    /**
     * Builds a district from the player's hand, if valid.
     * Only allowed for human players during their turn.
     *
     * @param currentPlayer the player attempting to build
     * @param inputParts    the split command string, e.g. ["build", "2"]
     * @param isHuman       true if player is human, false if AI or null
     */
    void handleBuild(Player currentPlayer, String[] inputParts, boolean isHuman) {
        // Only human players can use manual build input
        if (!isHuman) {
            System.out.println("Build command is not available for AI or when no player has turn.");
            return;
        }

        // Command should be: build <number>
        if (inputParts.length != 2) {
            System.out.println("Invalid command format. Use: build <number>");
            return;
        }

        // Try to parse which card to build
        try {
            int idx = Integer.parseInt(inputParts[1]) - 1;
            List<DistrictCard> cards = currentPlayer.getHand();

            // Check if the card index is valid
            if (idx < 0 || idx >= cards.size()) {
                System.out.println("Invalid card number.");
                return;
            }

            DistrictCard toBuild = cards.get(idx);
            String error = currentPlayer.canBuildDistrict(toBuild);

            // If no error, proceed with building the district
            if (error == null) {
                // Human player builds the selected district
                ((HumanPlayer) currentPlayer).buildDistrict(toBuild);
            } else {
                System.out.println("Cannot build this district: " + error);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid command format. Use: build <number>");
        }
    }


    /**
     * Handles the Magician's action command.
     * Supports both swapping hands with another player or discarding and redrawing cards.
     *
     * @param currentPlayer the current player (should be the Magician)
     * @param parts         the input command split by spaces, e.g. ["action", "swap", "2"]
     */
    void handleMagician(Player currentPlayer, String[] parts) {
        // Magician ability requires a human player
        if (!(currentPlayer instanceof HumanPlayer)) {
            System.out.println("You can only do character actions on your turn.");
            return;
        }

        HumanPlayer player = (HumanPlayer) currentPlayer;

        // Magician check: character 3
        if (player.getChosenCharacter() == null || player.getChosenCharacter().getTurnOrder() != 3) {
            System.out.println("You are not the Magician, cannot use this action.");
            return;
        }

        // Prevent using the Magician ability more than once per turn
        if (player.usedMagicianThisTurn()) {
            System.out.println("You have already used your Magician ability this turn.");
            return;
        }

        // Command should be: use magician <swap/redraw> <player/#,#,...>
        if (parts.length != 3) {
            System.out.println("Invalid action command. Use: 'action swap <player_number>' or 'action redraw <cardID1,cardID2,...>'");
            return;
        }

        String subCommand = parts[1];
        String argument = parts[2];

        // Handle swapping hands with another player
        if (subCommand.equals("swap")) {
            try {
                int targetPlayerNum = Integer.parseInt(argument);
                Player target = game.getPlayerByNumber(targetPlayerNum);
                // You can't swap with yourself or a non-existent player
                if (target == null || target == player) {
                    System.out.println("Invalid target player for swapping.");
                    return;
                }

                player.swapHandWith(target);
                player.setUsedMagicianThisTurn(true);
                // Catch invalid player number format
            } catch (NumberFormatException e) {
                System.out.println("Invalid player number.");
            }

            // action redraw <cardID1,cardID2,...>
        } else if (subCommand.equals("redraw")) {
            String[] indices = argument.split(",");
            List<DistrictCard> toDiscard = new ArrayList<>();

            // Go through each card index to discard
            for (String idxStr : indices) {
                try {
                    int idx = Integer.parseInt(idxStr.trim()) - 1;
                    // Ignore out-of-bounds card selections
                    if (idx < 0 || idx >= player.getHand().size()) {
                        System.out.println("Invalid card index: " + (idx + 1));
                        return;
                    }
                    toDiscard.add(player.getHand().get(idx));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid card index format.");
                    return;
                }
            }

            player.redrawCards(toDiscard);
            // Mark the ability as used for this turn
            player.setUsedMagicianThisTurn(true);
        } else {
            System.out.println("Invalid magician sub-command. Use 'swap' or 'redraw'.");
        }
    }

    /**
     * Used in testing to directly trigger the Warlord's destroy action
     * with custom input parameters. This lets test cases simulate destroying
     * a district using the Warlord's ability without user input.
     *
     * @param player       the player who is acting as the Warlord
     * @param paramsString the input string that specifies which district to destroy
     */
    public void testInvokeWarlordDestroy(Player player, String paramsString) {
        handleWarlordDestroy(player, paramsString);
    }

    /**
     * Handles Warlord's destroy ability.
     * Validates the command and destroys the chosen district if legal.
     *
     * @param currentPlayer the player trying to destroy a district
     * @param paramsString  a string with target player number and district index, e.g. "2 1"
     */
    private void handleWarlordDestroy(Player currentPlayer, String paramsString) {
        // Warlord check: character 8
        if (currentPlayer.getChosenCharacter() == null || currentPlayer.getChosenCharacter().getTurnOrder() != 8) {
            System.out.println("You are not the Warlord, cannot use destroy action.");
            return;
        }

        String[] params = paramsString.split(" ");
        // Check if both player and district numbers are provided
        if (params.length != 2) {
            System.out.println("Invalid destroy command format. Use: action destroy <player_number> <districtID>");
            return;
        }

        // Try parsing the input into usable numbers
        try {
            int targetPlayerNum = Integer.parseInt(params[0]);
            int districtIndex = Integer.parseInt(params[1]) - 1;

            Player target = game.getPlayerByNumber(targetPlayerNum);
            // Player number was invalid
            if (target == null) {
                System.out.println("Invalid target player.");
                return;
            }

            CharacterCard targetCharacter = target.getChosenCharacter();
            // Can't destroy district if player is the Bishop and not killed
            if (targetCharacter != null && targetCharacter.getTurnOrder() == 5 && !target.isKilled()) {
                System.out.println("Cannot destroy districts in an alive Bishop's city.");
                return;
            }

            List<DistrictCard> targetBuilt = game.getCityForPlayer(target);

            // Bell Tower means city is full at 7 instead of 8
            int limit = game.isBellTowerActive() ? 7 : 8;
            // Can't destroy a district in a city that's already completed
            if (targetBuilt.size() >= limit) {
                System.out.println("Warlord cannot destroy districts in a full city.");
                return;
            }

            // Make sure the district index is valid
            if (districtIndex < 0 || districtIndex >= targetBuilt.size()) {
                System.out.println("Invalid district index.");
                return;
            }

            DistrictCard toDestroy = targetBuilt.get(districtIndex);
            int destroyCost = target.getDestroyCost(toDestroy);

            // Prevent destroying if you can't afford the cost
            if (currentPlayer.getGold() < destroyCost) {
                System.out.println("Not enough gold to destroy this district. Need " + destroyCost + " gold.");
                return;
            }

            // Keep is immune to destruction
            if (toDestroy.getName().equalsIgnoreCase("Keep")) {
                System.out.println("You cannot destroy the Keep. It is indestructible.");
                return;
            }

            currentPlayer.destroyDistrict(target, districtIndex, false);
            // If any number input is bad, exit silently
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format in command.");
        }
    }

    /**
     * Displays the ability info for a purple district card in the player's hand.
     *
     * @param humanPlayer the player whose hand we're checking
     * @param districtIdx index of the district card (0-based)
     */
    void handleDistrictInfo(Player humanPlayer, int districtIdx) {
        List<DistrictCard> hand = humanPlayer.getHand();

        // Make sure the requested card index is valid
        if (districtIdx < 0 || districtIdx >= hand.size()) {
            System.out.println("Invalid district number.");
            return;
        }

        DistrictCard district = hand.get(districtIdx);

        // Only purple districts have ability text
        if (!"purple".equalsIgnoreCase(district.getColor())
                || district.getAbilityDescription() == null
                || district.getAbilityDescription().isEmpty()) {
            System.out.println("This district has no special ability.");
            // If the card has a description, show it
        } else {
            System.out.println((districtIdx + 1) + ". " + district.getName() +
                    " [" + district.getColor() + district.getCost() + "] (Ability: " + district.getAbilityDescription() + ")");
        }
    }

    /**
     * Displays the ability info of a character by name.
     *
     * @param nameLower character name, already lowercased
     */
    private void handleCharacterInfo(String nameLower) {
        List<CharacterCard> characters = GameUtils.createCharacterCards();

        // Search for a matching character by name (case-insensitive)
        for (CharacterCard c : characters) {
            if (c.getName().toLowerCase().equals(nameLower)) {
                System.out.println(c.getTurnOrder() + ". " + c.getName() + " – " + c.getAbilityDescription());
                return;
            }
        }

        System.out.println("Character or district '" + nameLower + "' not found.");
    }
}