package citadels.core;

import citadels.app.App;
import citadels.model.CharacterCard;
import citadels.model.DistrictCard;
import citadels.model.Player;
import citadels.model.PlayerScore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Utility methods used throughout the Citadels game.
 * Includes helpers for debugging, delays, character creation, and displaying information.
 */
public class GameUtils {
    private static boolean debugMode = false; // Whether debug mode is currently on
    private static boolean testMode = false;

    /**
     * Pauses the game for the given number of milliseconds.
     *
     * @param milliseconds How long to delay the game in ms
     */
    public static void delay(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
            // If something interrupts the sleep, ignore it (not critical)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Make sure interrupted status is preserved
        }
    }

    /**
     * Waits for the user to type "t" before continuing.
     * Used to prevent input during other players' turns.
     *
     * @param scanner Scanner to read user input
     */
    public static void waitForTCommand(Scanner scanner) {
        // Keep looping until the player types 't'
        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim().toLowerCase();
            if ("t".equals(input)) break;
            System.out.println("It is not your turn. Press t to continue with other player turns.");
        }
    }

    /**
     * Toggles debug mode on/off and prints a message.
     * When on, hands of AI players will be shown on their turns.
     */
    public static void toggleDebugMode() {
        debugMode = !debugMode;
        System.out.println("Debug mode " + (debugMode ? "enabled" : "disabled") + ". You will " + (debugMode ? "" : "no longer ") + "see all player’s hands.");
    }

    /**
     * Checks whether debug mode is currently active.
     *
     * @return true if debug mode is on
     */
    public static boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets debug mode directly using a Boolean (e.g., when loading from save).
     *
     * @param debugMode Boolean to enable or disable debug mode
     */
    public static void applyDebugModeOverride(Boolean debugMode) {
        GameUtils.debugMode = debugMode != null && debugMode;
    }

    /**
     * Shows a player's hand and gold in debug mode format.
     *
     * @param player The player whose hand to show
     * @return String with player's name, gold, and card list
     */
    public static String getDebugStatusString(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(player.getName()).append(": gold = ").append(player.getGold()).append("\n");
        sb.append("Hand:\n");

        List<DistrictCard> hand = player.getHand();
        // Show special message if the player has no cards
        if (hand.isEmpty()) {
            sb.append("(No cards in hand)\n");
            // Otherwise, print out each card in the player's hand
        } else {
            // Number and list each card with its name and color/cost
            for (int i = 0; i < hand.size(); i++) {
                DistrictCard card = hand.get(i);
                sb.append(i + 1).append(". ").append(card.getName());
                // Include color and cost details if available
                if (card.getColor() != null) {
                    sb.append(" [").append(card.getColor().toLowerCase()).append(card.getCost()).append("]");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Returns the bonus color associated with a character,
     * based on their turn order. Only certain characters get bonus gold
     * from districts of a specific color.
     *
     * @param character the character to check
     * @return the bonus district color for that character, or null if none
     */
    static String getBonusColorForCharacter(CharacterCard character) {
        switch (character.getTurnOrder()) {
            case 4:
                return "yellow"; // King
            case 5:
                return "blue";   // Bishop
            case 6:
                return "green";  // Merchant
            case 8:
                return "red";    // Warlord
            default:
                return null;
        }
    }

    /**
     * Gets a character name based on its number (1-8).
     *
     * @param number Character number
     * @return Name of the character or "Unknown"
     */
    public static String getCharacterNameByNumber(int number) {
        // Map character number (1-8) to the corresponding character name
        switch (number) {
            case 1:
                return "Assassin";
            case 2:
                return "Thief";
            case 3:
                return "Magician";
            case 4:
                return "King";
            case 5:
                return "Bishop";
            case 6:
                return "Merchant";
            case 7:
                return "Architect";
            case 8:
                return "Warlord";
            default:
                return "Unknown";
        }
    }

    /**
     * Creates and returns the 8 main character cards for a game of Citadels.
     *
     * @return List of CharacterCard objects
     */
    public static List<CharacterCard> createCharacterCards() {
        List<CharacterCard> characters = new ArrayList<>();
        characters.add(new CharacterCard("Assassin", 1, "Choose a character. That character skips their turn this round."));
        characters.add(new CharacterCard("Thief", 2, "Choose a character (other than Assassin). When that character’s turn begins, you steal all their gold."));
        characters.add(new CharacterCard("Magician", 3, "On your turn, either swap your hand with another player or discard any number of cards and draw that many. Use: 'action swap <player_number>', 'redraw <cardID1,cardID2,...>'"));
        characters.add(new CharacterCard("King", 4, "Earn gold for each noble (yellow) district. Receives the crown and chooses first next round."));
        characters.add(new CharacterCard("Bishop", 5, "Earn gold for each religious (blue) district. Your city cannot be targeted by the Warlord."));
        characters.add(new CharacterCard("Merchant", 6, "Earn gold for each trade (green) district. Gain 1 extra gold this turn."));
        characters.add(new CharacterCard("Architect", 7, "Draw two extra cards. May build up to three districts this turn."));
        characters.add(new CharacterCard("Warlord", 8, "Earn gold for each military (red) district. May pay gold to destroy a district in another player’s city. Use: 'action destroy <player_number> <districtID>'."));
        return characters;
    }

    /**
     * Prints the character cards that were removed face-up at the start of the round.
     *
     * @param discardedCharacterCardsFaceUp List of character cards removed face-up
     */
    public static void printDiscardedCharacterCardsFaceUp(List<CharacterCard> discardedCharacterCardsFaceUp) {
        if (discardedCharacterCardsFaceUp.isEmpty()) return;

        // Handle special case where only one card was discarded face-up
        if (discardedCharacterCardsFaceUp.size() == 1) {
            System.out.println(discardedCharacterCardsFaceUp.get(0).getName() + " was removed.");
            // Otherwise, list multiple discarded characters nicely
        } else {
            StringBuilder names = new StringBuilder();
            // Join character names with commas and "and" at the end
            for (int i = 0; i < discardedCharacterCardsFaceUp.size(); i++) {
                names.append(discardedCharacterCardsFaceUp.get(i).getName());
                // Add "and" before the last item if needed
                if (i == discardedCharacterCardsFaceUp.size() - 2) {
                    names.append(" and ");
                    // Add a comma if we're still in the middle of the list
                } else if (i < discardedCharacterCardsFaceUp.size() - 2) {
                    names.append(", ");
                }
            }
            System.out.println(names + " were removed.");
        }
    }

    /**
     * Prints a breakdown of each player's score at game end.
     * Shows all components of the total score and congratulates the winner.
     *
     * @param scores  Map of Player to their PlayerScore
     * @param players List of players in turn order
     * @param winner  Player with the highest score
     */
    public static void printScoreBreakdown(Map<Player, PlayerScore> scores, List<Player> players, Player winner) {
        System.out.println("Game Over! Score breakdown:");
        // Show each player's final score details
        for (Player player : players) {
            PlayerScore ps = scores.get(player);

            System.out.println(player.getName() + ":");
            System.out.println("Base points from districts: " + ps.getBaseScore());
            System.out.println("Diverse city bonus: " + ps.getBonusDiverseCity());
            System.out.println("Complete city bonus: " + ps.getBonusCompleteCity());
            System.out.println("Extra points from districts: " + ps.getExtraPoints());
            System.out.println("Total points: " + ps.getTotalScore());
            delay(1000);
            System.out.println();
        }

        System.out.println("Congratulations to " + winner.getName() + ", the winner!");
        delay(1000);
    }

    /**
     * Prints a district card with its index, name, color, and cost.
     *
     * @param card  The DistrictCard to print
     * @param index The card's position in the list
     */
    public static void printDistrictCard(DistrictCard card, int index) {
        System.out.print((index + 1) + ". " + card.getName());
        // Show the card name and its color+cost if available
        if (card.getColor() != null) {
            System.out.print(" [" + card.getColor() + card.getCost() + "]");
        }
        System.out.println();
    }

    /**
     * Checks whether the game is currently running in test mode.
     *
     * @return true if test mode is enabled, false otherwise
     */
    public static boolean isTestMode() {
        return testMode;
    }

    /**
     * Enables or disables test mode for the game.
     * When test mode is on, random behavior is controlled for testing purposes.
     *
     * @param value true to enable test mode, false to disable it
     */
    public static void setTestMode(boolean value) {
        testMode = value;
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
}
