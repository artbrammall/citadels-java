package citadels.model;

import citadels.core.Game;
import citadels.core.InputHandler;
import citadels.state.Board;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the HumanPlayer class.
 * Tests player interaction via console input (System.in) and verifies proper state and output (System.out).
 */
public class HumanPlayerTest {

    private final PrintStream originalOut = System.out;
    private final InputStream originalIn = System.in;
    InputHandler handler;
    private Game game;
    private HumanPlayer human;
    private ByteArrayOutputStream outContent;
    private Board board;
    private Player player;

    /**
     * Sets deterministic randomness before any tests run.
     */
    @BeforeAll
    static void setupRandom() {
        Random fixed = new Random(42);
        Player.setRandom(fixed);
        AIPlayer.setRandom(fixed);
        Game.setRandom(fixed);
    }

    /**
     * Loads the test save file and captures System.out before each test.
     */
    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        File file = new File("src/test/resources/testdata/test_allpurpose_save1.json");
        assertTrue(file.exists(), "Save file does not exist: " + file.getAbsolutePath());

        game = new Game();
        game.loadGame(file.getAbsolutePath());

        // Player 1 is always the human player
        assertTrue(game.getPlayers().get(0) instanceof HumanPlayer);
        human = (HumanPlayer) game.getPlayers().get(0);
    }

    /**
     * Resets System.out and System.in after each test.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    /**
     * Utility to simulate user input for a test.
     */
    private void provideInput(String input) {
        ByteArrayInputStream testIn = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        System.setIn(testIn);
    }

    /**
     * Returns the captured output printed to System.out.
     */
    private String getOutput() {
        return outContent.toString().trim();
    }
}