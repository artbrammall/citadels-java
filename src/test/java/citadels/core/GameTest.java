package citadels.core;

import citadels.app.App;
import citadels.model.AIPlayer;
import citadels.model.DistrictCard;
import citadels.model.Player;
import citadels.state.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Scaffold for unit tests of the Game class.
 * Game manages full game state, flow, and player progression.
 */
public class GameTest {

    private static boolean skipSetup = false;
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private Board board;
    private Player player;
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
        if (skipSetup) return;

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

    @AfterEach
    void resetSkipFlag() {
        skipSetup = false;
    }


    /**
     * Verifies that setting a fixed Random seed results in consistent card choices.
     * This ensures that randomness-dependent logic produces repeatable outcomes
     * when seeded with the same value (seed = 42).
     */
    @Test
    void testFixedRandomCardChoice() {
        Player.setRandom(new Random(42));
        // assert card drawn is consistent every time with seed 42
    }

    /**
     * Tests promptNumberOfPlayers by feeding invalid input twice:
     * - First input is non-numeric (should trigger NumberFormatException)
     * - Second input is a number out of bounds (should show "between 4 and 7")
     * Then it feeds a valid number to exit the loop.
     */
    @Test
    void testPromptNumberOfPlayers_InvalidInputsHandled() {
        skipSetup = true; // skip @BeforeEach if used

        // Set up fake input: "abc" (invalid), "3" (out of bounds), "5" (valid)
        App.input = new ByteArrayInputStream("abc\n3\n5\n".getBytes(StandardCharsets.UTF_8));
        System.setOut(new PrintStream(outContent)); // capture System.out

        Game game = new Game();

        try {
            Method method = Game.class.getDeclaredMethod("promptNumberOfPlayers");
            method.setAccessible(true);
            int result = (int) method.invoke(game);

            assertEquals(5, result, "Expected valid player count of 5");

            String output = outContent.toString();
            assertTrue(output.contains("Invalid input"), "Should warn about non-numeric input");
            assertTrue(output.contains("Please enter a number between 4 and 7"), "Should warn about out-of-bounds input");

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            cause.printStackTrace();
            fail("Method failed during execution: " + cause.getMessage());
        } catch (Exception e) {
            fail("Reflection failed: " + e.getMessage());
        }
    }

    /**
     * Tests that the Game constructor correctly prompts for number of players,
     * initializes the correct number of players, and sets up character and board decks.
     */
    @Test
    void testGameConstructor_PromptsAndInitializesCorrectly() {
        skipSetup = true;

        // Provide fake user input: "abc" → invalid, "3" → out of bounds, "5" → accepted
        App.input = new ByteArrayInputStream("abc\n3\n5\n".getBytes(StandardCharsets.UTF_8));

        // Use test district card templates
        List<DistrictCard> testTemplates = Arrays.asList(
                new DistrictCard("TestCard1", "red", 1, 1, null),
                new DistrictCard("TestCard2", "blue", 2, 1, null),
                new DistrictCard("TestCard3", "green", 3, 1, null),
                new DistrictCard("TestCard4", "yellow", 4, 1, null)
        );

        // Capture output
        System.setOut(new PrintStream(outContent));

        Game testGame = new Game(testTemplates);

        // Assertions
        assertEquals(5, testGame.getPlayers().size(), "Expected 5 players to be created");
        assertNotNull(testGame.getDeck(), "District deck should be initialized");
        assertNotNull(testGame.getCharacterDeck(), "Character deck should be initialized");
        assertNotNull(testGame.getBoard(), "Board should be initialized");

        String output = outContent.toString();
        assertTrue(output.contains("Invalid input"), "Should warn about invalid non-numeric input");
        assertTrue(output.contains("Please enter a number between 4 and 7"), "Should warn about out-of-bounds input");
    }
}
