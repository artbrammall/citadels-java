package citadels.core;

import citadels.app.App;
import citadels.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for GameUtils methods.
 * This scaffold loads a standard save file before each test.
 */
public class GameUtilsTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private Game game;

    /**
     * Sets a fixed Random seed before all tests to ensure deterministic behavior.
     * This overrides the default randomness in Player, AIPlayer, and Game classes,
     * allowing predictable and repeatable test results for any logic involving randomness.
     */
    @BeforeAll
    static void setupRandom() {
        Random fixed = new Random(42);
        Player.setRandom(fixed);
        AIPlayer.setRandom(fixed);
        Game.setRandom(fixed);
    }

    /**
     * Loads the all-purpose save file before each test.
     * Ensures a consistent, preconfigured game state.
     */
    @BeforeEach
    void setUp() {
        try {
            File file = new File("src/test/resources/testdata/test_allpurpose_save1.json");
            System.out.println("Attempting to load file from: " + file.getAbsolutePath());

            assertTrue(file.exists(), "Test save file not found at: " + file.getAbsolutePath());

            game = new Game();
            game.loadGame(file.getAbsolutePath());
        } catch (Exception e) {
            fail("Failed to load test game: " + e.getMessage());
        }
    }

    /**
     * Just a basic sanity check to make sure the save loaded and contains the correct number of players.
     */
    @Test
    void exampleTest() {
        assertNotNull(game);
        assertEquals(7, game.getPlayers().size(), "Should have 7 players loaded");
    }

    /**
     * Checks that delay() actually sleeps the expected amount of time when not in test mode.
     */
    @Test
    void testDelay_NormalBehaviorSleeps() {

        long start = System.currentTimeMillis();
        GameUtils.delay(50); // Should sleep
        long end = System.currentTimeMillis();

        assertTrue(end - start >= 45, "Should sleep ~50ms in normal mode");
    }

    /**
     * Verifies that if a thread is interrupted before delay() is called, the interrupt flag is preserved.
     */
    @Test
    void testDelay_InterruptedExceptionPreservesStatus() throws InterruptedException {

        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt(); // Interrupt self before calling delay
            GameUtils.delay(100); // Should catch the interrupt and preserve the flag
            assertTrue(Thread.currentThread().isInterrupted(), "Interrupted status should be preserved");
        });

        testThread.start();
        testThread.join(); // Wait for the thread to finish
    }

    /**
     * Tests that waitForTCommand() exits right away when the user enters a single valid "t".
     */
    @Test
    void testWaitForTCommand_ImmediateT() {
        App.input = new ByteArrayInputStream("t\n".getBytes());

        // Use the same method your code does to get a Scanner on App.input
        Scanner scanner = new HumanPlayer("test").getScanner();

        GameUtils.waitForTCommand(scanner); // Should exit immediately with valid input
    }

    /**
     * Tests that waitForTCommand() loops through invalid inputs before accepting "t" and continuing.
     */
    @Test
    void testWaitForTCommand_MultipleInvalidBeforeT() {
        App.input = new ByteArrayInputStream("nope\nlater\nT\n".getBytes());

        // Use the same method the game uses to construct the Scanner
        Scanner scanner = new HumanPlayer("test").getScanner();

        GameUtils.waitForTCommand(scanner); // Should loop until a valid "T" is entered
    }

    /**
     * Resets debug mode to false after each test to prevent state from leaking across tests.
     */
    @AfterEach
    void resetDebugMode() {
        GameUtils.applyDebugModeOverride(false); // Ensure clean state
    }

    /**
     * Makes sure toggleDebugMode() switches the debug flag correctly and prints the right message.
     */
    @Test
    void testToggleDebugModeTogglesStateAndPrintsCorrectly() {
        GameUtils.applyDebugModeOverride(false);
        assertFalse(GameUtils.isDebugMode(), "Debug mode should start disabled");

        // Capture console output
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        GameUtils.toggleDebugMode();
        assertTrue(GameUtils.isDebugMode(), "Debug mode should now be enabled");
        assertTrue(output.toString().contains("Debug mode enabled"), "Should print enabled message");

        output.reset();
        GameUtils.toggleDebugMode();
        assertFalse(GameUtils.isDebugMode(), "Debug mode should now be disabled again");
        assertTrue(output.toString().contains("Debug mode disabled"), "Should print disabled message");

        System.setOut(originalOut); // Restore console
    }

    /**
     * Checks that applyDebugModeOverride(true) turns debug mode on.
     */
    @Test
    void testApplyDebugModeOverrideTrue() {
        GameUtils.applyDebugModeOverride(true);
        assertTrue(GameUtils.isDebugMode(), "Debug mode should be enabled after applying override true");
    }

    /**
     * Checks that applyDebugModeOverride(false) turns debug mode off.
     */
    @Test
    void testApplyDebugModeOverrideFalse() {
        GameUtils.applyDebugModeOverride(false);
        assertFalse(GameUtils.isDebugMode(), "Debug mode should be disabled after applying override false");
    }

    /**
     * If applyDebugModeOverride(null) is called, debug mode should default to false.
     */
    @Test
    void testApplyDebugModeOverrideNull() {
        GameUtils.applyDebugModeOverride(null);
        assertFalse(GameUtils.isDebugMode(), "Debug mode should be disabled when override is null");
    }

    /**
     * Fully tests getDebugStatusString() for both players with cards and empty hands.
     */
    @Test
    void testGetDebugStatusString_AllBranchesCovered() {
        // Use the fully-loaded game state
        Player player1 = game.getPlayers().get(0); // Player 1 has a full hand
        Player player7 = game.getPlayers().get(6); // Player 7 has empty hand

        // ✅ Case: Player with cards in hand
        String result1 = GameUtils.getDebugStatusString(player1);
        assertTrue(result1.contains("Player 1: gold = 10"), "Should show correct gold amount");
        assertTrue(result1.contains("Hand:"), "Should include hand section");
        assertTrue(result1.contains("1. Armory [purple3]"), "Should include first card properly formatted");
        assertTrue(result1.contains("7. Castle [yellow4]"), "Should include all 7 cards");
        assertFalse(result1.contains("(No cards in hand)"), "Should not say 'no cards' when cards are present");

        // ✅ Case: Player with empty hand
        String result2 = GameUtils.getDebugStatusString(player7);
        assertTrue(result2.contains("Player 7: gold = 0"), "Should show correct gold amount for Player 7");
        assertTrue(result2.contains("(No cards in hand)"), "Should correctly identify and display empty hand");
    }

    /**
     * Tests that each number 1–8 maps correctly to the right character name.
     */
    @Test
    void testGetCharacterNameByNumber_ValidRange() {
        assertEquals("Assassin", GameUtils.getCharacterNameByNumber(1));
        assertEquals("Thief", GameUtils.getCharacterNameByNumber(2));
        assertEquals("Magician", GameUtils.getCharacterNameByNumber(3));
        assertEquals("King", GameUtils.getCharacterNameByNumber(4));
        assertEquals("Bishop", GameUtils.getCharacterNameByNumber(5));
        assertEquals("Merchant", GameUtils.getCharacterNameByNumber(6));
        assertEquals("Architect", GameUtils.getCharacterNameByNumber(7));
        assertEquals("Warlord", GameUtils.getCharacterNameByNumber(8));
    }

    /**
     * Verifies that out-of-range numbers return "Unknown" as expected.
     */
    @Test
    void testGetCharacterNameByNumber_InvalidReturnsUnknown() {
        assertEquals("Unknown", GameUtils.getCharacterNameByNumber(0));
        assertEquals("Unknown", GameUtils.getCharacterNameByNumber(9));
        assertEquals("Unknown", GameUtils.getCharacterNameByNumber(-5));
        assertEquals("Unknown", GameUtils.getCharacterNameByNumber(999));
    }

    /**
     * Makes sure getCharacterNameByNumber() works correctly on a real character from the save file.
     */
    @Test
    void testGetCharacterNameByNumber_IntegrationWithSave() {
        // From save file: Player 1 has character number 3 (Magician)
        Player player1 = game.getPlayers().get(0);
        int number = player1.getChosenCharacter().getTurnOrder();

        assertEquals("Magician", GameUtils.getCharacterNameByNumber(number));
    }

    /**
     * Checks that createCharacterCards() always gives back 8 correct character cards with the right names and numbers.
     */
    @Test
    void testCreateCharacterCards_ReturnsStandardEight() {
        List<CharacterCard> cards = GameUtils.createCharacterCards();

        // ✅ Should always return exactly 8 cards
        assertEquals(8, cards.size(), "Expected 8 character cards");

        // ✅ Validate name + number mapping (full branch coverage of fixed list)
        assertEquals("Assassin", cards.get(0).getName());
        assertEquals(1, cards.get(0).getTurnOrder());

        assertEquals("Thief", cards.get(1).getName());
        assertEquals(2, cards.get(1).getTurnOrder());

        assertEquals("Magician", cards.get(2).getName());
        assertEquals(3, cards.get(2).getTurnOrder());

        assertEquals("King", cards.get(3).getName());
        assertEquals(4, cards.get(3).getTurnOrder());

        assertEquals("Bishop", cards.get(4).getName());
        assertEquals(5, cards.get(4).getTurnOrder());

        assertEquals("Merchant", cards.get(5).getName());
        assertEquals(6, cards.get(5).getTurnOrder());

        assertEquals("Architect", cards.get(6).getName());
        assertEquals(7, cards.get(6).getTurnOrder());

        assertEquals("Warlord", cards.get(7).getName());
        assertEquals(8, cards.get(7).getTurnOrder());
    }

    /**
     * Confirms that at least one character in the save file matches a character from the standard list.
     */
    @Test
    void testCreateCharacterCards_IntegrationMatchesSaveFilePlayer1() {
        // Verify at least one character card matches the saved character of a real player
        CharacterCard savedCharacter = game.getPlayers().get(0).getChosenCharacter(); // e.g. Magician
        List<CharacterCard> all = GameUtils.createCharacterCards();

        boolean found = all.stream().anyMatch(card ->
                card.getName().equals(savedCharacter.getName()) &&
                        card.getTurnOrder() == savedCharacter.getTurnOrder());

        assertTrue(found, "The character from the save should exist in the standard card list");
    }

    /**
     * Restores standard output after tests that redirect it.
     */
    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    /**
     * If printDiscardedCharacterCardsFaceUp() is given an empty list, it should print nothing.
     */
    @Test
    void testPrintDiscardedCharacterCardsFaceUp_EmptyListPrintsNothing() {
        System.setOut(new PrintStream(outContent));

        GameUtils.printDiscardedCharacterCardsFaceUp(Collections.emptyList());

        assertEquals("", outContent.toString().trim(), "No output should be printed for an empty list");
    }

    /**
     * If there's one discarded character, the printed output should reflect that correctly.
     */
    @Test
    void testPrintDiscardedCharacterCardsFaceUp_OneCard() {
        System.setOut(new PrintStream(outContent));

        CharacterCard card = new CharacterCard("King", 4, "...");
        GameUtils.printDiscardedCharacterCardsFaceUp(Collections.singletonList(card));

        String result = outContent.toString().trim();
        assertEquals("King was removed.", result);
    }

    /**
     * If there are two discarded characters, the output should join them with "and".
     */
    @Test
    void testPrintDiscardedCharacterCardsFaceUp_TwoCards() {
        System.setOut(new PrintStream(outContent));

        CharacterCard c1 = new CharacterCard("Assassin", 1, "...");
        CharacterCard c2 = new CharacterCard("Warlord", 8, "...");

        GameUtils.printDiscardedCharacterCardsFaceUp(Arrays.asList(c1, c2));

        String result = outContent.toString().trim();
        assertEquals("Assassin and Warlord were removed.", result);
    }

    /**
     * Checks the formatting when there are three discarded characters — should be comma-separated with "and" before the last.
     */
    @Test
    void testPrintDiscardedCharacterCardsFaceUp_ThreeCards() {
        System.setOut(new PrintStream(outContent));

        CharacterCard c1 = new CharacterCard("Assassin", 1, "...");
        CharacterCard c2 = new CharacterCard("Thief", 2, "...");
        CharacterCard c3 = new CharacterCard("Warlord", 8, "...");

        GameUtils.printDiscardedCharacterCardsFaceUp(Arrays.asList(c1, c2, c3));

        String result = outContent.toString().trim();
        assertEquals("Assassin, Thief and Warlord were removed.", result);
    }

    /**
     * Tests that the loaded save file (which has no discarded characters) results in no printed output.
     */
    @Test
    void testPrintDiscardedCharacterCardsFaceUp_FromLoadedSave() {
        System.setOut(new PrintStream(outContent));

        // Save file has an empty discarded list, test that explicitly
        List<CharacterCard> fromSave = game.getDiscardedCharacterCardsFaceUp();
        assertTrue(fromSave.isEmpty(), "Sanity check: save file's face-up list should be empty");

        GameUtils.printDiscardedCharacterCardsFaceUp(fromSave);
        assertEquals("", outContent.toString().trim(), "Should print nothing for empty face-up list in save");
    }

    /**
     * Fully tests printScoreBreakdown() and checks that the correct lines and totals are printed for each player.
     */
    @Test
    void testPrintScoreBreakdown_PrintsAllLinesCorrectly() {

        // Players from loaded save
        List<Player> players = game.getPlayers();
        Player winner = players.get(2); // Player 3 arbitrarily chosen as winner

        // Assign mock scores
        Map<Player, PlayerScore> scores = new HashMap<>();
        for (Player p : players) {
            scores.put(p, new PlayerScore(
                    10,  // baseScore
                    p.getName().equals("Player 3") ? 3 : 1,  // bonusDiverseCity
                    4,   // bonusCompleteCity
                    p.getName().equals("Player 3") ? 5 : 0,  // extraPoints
                    false  // bellTowerActive — not needed for this test
            ));
        }

        // Capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // Call method
        GameUtils.printScoreBreakdown(scores, players, winner);

        // Restore original output
        System.setOut(originalOut);
        String output = outContent.toString();

        // ✅ Validate key lines
        assertTrue(output.contains("Game Over! Score breakdown:"), "Should begin with game over notice");
        assertTrue(output.contains("Player 1:"), "Should print Player 1's name");
        assertTrue(output.contains("Base points from districts: 10"));
        assertTrue(output.contains("Diverse city bonus: 1"));
        assertTrue(output.contains("Complete city bonus: 4"));
        assertTrue(output.contains("Extra points from districts: 0"));
        assertTrue(output.contains("Total points: 15"));

        assertTrue(output.contains("Player 3:"), "Should print Player 3 (winner)");
        assertTrue(output.contains("Extra points from districts: 5"));
        assertTrue(output.contains("Diverse city bonus: 3"));

        assertTrue(output.contains("Congratulations to Player 3, the winner!"), "Should print winner announcement");
    }

    /**
     * Redirects output to capture printed text for later assertion in tests.
     */
    @BeforeEach
    void captureOutput() {
        System.setOut(new PrintStream(outContent));
    }

    /**
     * Tests that printDistrictCard() shows the right name, color, and cost formatting when printing a district.
     */
    @Test
    void testPrintDistrictCard_WithColorAndCost() {
        // Get a real card from Player 1’s hand
        DistrictCard card = game.getPlayers().get(0).getHand().get(0); // Armory [purple3]
        GameUtils.printDistrictCard(card, 0);

        String output = outContent.toString().trim();
        assertEquals("1. Armory [purple3]", output);
    }

    /**
     * Tests getBonusColorForCharacter for all valid character turn orders
     * and the default null return case.
     */
    @Test
    public void testGetBonusColorForCharacter_AllBranches() {
        CharacterCard king = new CharacterCard("King", 4, "");
        CharacterCard bishop = new CharacterCard("Bishop", 5, "");
        CharacterCard merchant = new CharacterCard("Merchant", 6, "");
        CharacterCard warlord = new CharacterCard("Warlord", 8, "");
        CharacterCard magician = new CharacterCard("Magician", 3, ""); // unmapped

        assertEquals("yellow", GameUtils.getBonusColorForCharacter(king),
                "King should return yellow");
        assertEquals("blue", GameUtils.getBonusColorForCharacter(bishop),
                "Bishop should return blue");
        assertEquals("green", GameUtils.getBonusColorForCharacter(merchant),
                "Merchant should return green");
        assertEquals("red", GameUtils.getBonusColorForCharacter(warlord),
                "Warlord should return red");
        assertNull(GameUtils.getBonusColorForCharacter(magician),
                "Unmapped character should return null");
    }
}
