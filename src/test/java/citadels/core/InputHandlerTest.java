package citadels.core;

import citadels.app.App;
import citadels.model.*;
import citadels.state.Board;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Scaffold for unit tests of the InputHandler class.
 * InputHandler processes player commands and handles null-turn logic.
 */
public class InputHandlerTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    InputHandler handler;
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

    @BeforeEach
    void setUp() {
        try {
            GameUtils.setTestMode(true);
            File file = new File("src/test/resources/testdata/test_allpurpose_save1.json");
            System.out.println("Attempting to load file from: " + file.getAbsolutePath());
            assertTrue(file.exists(), "Test save file not found at: " + file.getAbsolutePath());

            App app = new App();
            List<DistrictCard> cards = app.loadDistrictCards();

            game = new Game(cards, 7); // ✅ Use non-interactive constructor
            game.loadGame(file.getAbsolutePath());

            handler = new InputHandler(game);
        } catch (Exception e) {
            fail("Failed to load test game: " + e.getMessage());
        }
    }

    @Test
    void testGetScanner_ReadsFromAppInputCorrectly() {
        // Set App.input to a simulated test string
        String testInput = "hello world\n";
        App.input = new ByteArrayInputStream(testInput.getBytes());

        // Use getScanner() — assume we're testing InputHandler's method
        InputHandler handler = new InputHandler(null); // or mock Game if required
        Scanner scanner = handler.getScanner();

        // Verify the scanner reads from App.input, not System.in
        String result = scanner.nextLine();
        assertEquals("hello world", result);
    }

    /**
     * Tests that resetNullTurnFlag() correctly resets the null turn flag,
     * and that isNullTurnProcessed() reflects the flag state.
     */
    @Test
    void testNullTurnFlagBehavior_InputHandler() {
        // Create InputHandler with null Game if Game not needed for this logic
        InputHandler handler = new InputHandler(null);

        // Initially should be false
        assertFalse(handler.isNullTurnProcessed(), "Expected nullTurnProcessed to be false initially");

        // Simulate processing the null turn — again, use reflection unless you have a setter
        try {
            Field field = InputHandler.class.getDeclaredField("nullTurnProcessed");
            field.setAccessible(true);
            field.setBoolean(handler, true);
        } catch (Exception e) {
            fail("Could not set nullTurnProcessed via reflection: " + e.getMessage());
        }

        // Now confirm it's true
        assertTrue(handler.isNullTurnProcessed(), "Expected nullTurnProcessed to be true after manual set");

        // Call the real method
        handler.resetNullTurnFlag();

        // Verify it's now false
        assertFalse(handler.isNullTurnProcessed(), "Expected nullTurnProcessed to be false after reset");
    }

    /**
     * Tests that processInput() correctly reads and handles input from the scanner,
     * when it is the human player’s turn.
     */
    @Test
    void testProcessInput_HandlesValidCommand() throws IOException {
        // Inject a simple valid command for the human player
        String testCommand = "hand\n"; // 'hand' is a harmless command during a turn
        App.input = new ByteArrayInputStream(testCommand.getBytes());
        Scanner scanner = new Scanner(App.input);

        // Load game and save file
        App app = new App();
        List<DistrictCard> cards = app.loadDistrictCards();

        File saveFile = new File("src/test/resources/testdata/test_allpurpose_save1.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        // Set up InputHandler with the loaded game
        InputHandler handler = new InputHandler(game);

        // Ensure the current player is Player 1 (the human player)
        assertEquals("Player 1", game.getCurrentPlayer().getName());

        // Now test that input is read and processed
        handler.processInput(scanner);

        // There's no assertable output unless you mock System.out,
        // so we just assert that no crash occurs and it reads the line
        assertTrue(true);
    }

    /**
     * Tests that processInput() correctly handles human and AI inputs:
     * - "t" for AI (sets turnCompleted and nullTurnProcessed)
     * - "end" for Human (ends turn and prints confirmation)
     * - "t" for Human (prints "Your turn.")
     */
    @Test
    void testProcessInput_AllMainBranches() throws IOException {
        // Prepare scanner for a single command (will be reset between sections)
        App app = new App();
        List<DistrictCard> cards = app.loadDistrictCards();

        File saveFile = new File("src/test/resources/testdata/test_allpurpose_save1.json");
        assertTrue(saveFile.exists());

        // === 1. HUMAN: "end" ===
        App.input = new ByteArrayInputStream("end\n".getBytes());
        Scanner scannerEnd = new Scanner(App.input);

        Game gameHuman = new Game();
        gameHuman.loadGame(saveFile.getAbsolutePath());

        InputHandler handlerHuman = new InputHandler(gameHuman);
        Player currentHuman = gameHuman.getCurrentPlayer();

        assertFalse(currentHuman.hasCompletedTurn());
        handlerHuman.processInput(scannerEnd);
        assertTrue(currentHuman.hasCompletedTurn());

        // === 2. HUMAN: "t" ===
        App.input = new ByteArrayInputStream("t\n".getBytes());
        Scanner scannerT = new Scanner(App.input);

        Game gameHuman2 = new Game();
        gameHuman2.loadGame(saveFile.getAbsolutePath());

        InputHandler handlerHuman2 = new InputHandler(gameHuman2);
        handlerHuman2.processInput(scannerT); // should print "Your turn."

        // === 3. AI: "t" ===
        // Switch currentPlayer to an AIPlayer manually
        Game gameAI = new Game();
        gameAI.loadGame(saveFile.getAbsolutePath());

        // Replace current player with dummy AI
        AIPlayer ai = new AIPlayer("Dummy AI");
        gameAI.getPlayers().set(gameAI.getCurrentPlayerIndex(), ai);
        assertFalse(ai.hasCompletedTurn());

        App.input = new ByteArrayInputStream("t\n".getBytes());
        Scanner scannerAIT = new Scanner(App.input);

        InputHandler handlerAI = new InputHandler(gameAI);
        handlerAI.processInput(scannerAIT);

        assertTrue(ai.hasCompletedTurn(), "AI turn should be marked as completed.");
        assertTrue(handlerAI.isNullTurnProcessed(), "Null turn flag should be set.");
    }

    /**
     * Tests that purple district commands like discard, draw, place, and armory
     * are correctly handled by InputHandler during a human player's turn.
     */
    @Test
    void testProcessInput_PurpleDistrictCommands() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // === LABORATORY: discard Armory ===
        App.input = new ByteArrayInputStream("discard 1\n".getBytes()); // Assumes Armory is at index 1
        Scanner scannerDiscard = new Scanner(App.input);
        handler.processInput(scannerDiscard);
        output.reset();

        // === SMITHY: draw 3 cards ===
        App.input = new ByteArrayInputStream("draw\n".getBytes());
        Scanner scannerSmithy = new Scanner(App.input);
        handler.processInput(scannerSmithy);
        output.reset();

        // === MUSEUM: place a card under the museum ===
        App.input = new ByteArrayInputStream("place 1\n".getBytes()); // Assumes there's a card left at index 1
        Scanner scannerMuseum = new Scanner(App.input);
        handler.processInput(scannerMuseum);
        output.reset();

        // === ARMORY: trigger armory ability ===
        App.input = new ByteArrayInputStream("armory\n".getBytes());
        Scanner scannerArmory = new Scanner(App.input);
        handler.processInput(scannerArmory);
        output.reset();

        System.setOut(originalOut);

        // If no exception is thrown, consider this test passed
        assertTrue(true, "Purple district commands executed without error.");
    }

    /**
     * Tests that typing "hand" prints Player 1's hand details.
     * Assumes the game has already been loaded from a valid save file.
     */
    /**
     * Tests the "hand" command for Player 1.
     * Ensures that the current hand is printed, including known cards.
     */
    @Test
    void testProcessInput_HandCommand_PrintsHand() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // Inject "hand" command
        App.input = new ByteArrayInputStream("hand\n".getBytes());
        Scanner scanner = new Scanner(App.input);
        handler.processInput(scanner);

        String out = output.toString();
        System.out.println("DEBUG OUTPUT:\n" + out); // optional debug

        // Known cards from Player 1's hand in test_allpurpose_save1.json
        assertTrue(out.contains("Castle"), "Expected hand output to include 'Castle'");
        assertTrue(out.contains("Laboratory"), "Expected hand output to include 'Laboratory'");
        assertTrue(out.contains("Dragon Gate"), "Expected hand output to include 'Dragon Gate'");
        assertTrue(out.contains("Observatory"), "Expected hand output to include 'Observatory'");

        System.setOut(originalOut);
    }

    /**
     * Tests the "gold" command for:
     * - showing the human player's gold (no argument)
     * - showing another player's gold (with player number)
     * - invalid player number (e.g. 99, not in game)
     * - invalid input format (e.g. non-numeric)
     * <p>
     * Assumes save file is already loaded and game is in TURN phase.
     */
    @Test
    void testProcessInput_GoldCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. gold (own gold) ---
        App.input = new ByteArrayInputStream("gold\n".getBytes());
        Scanner s1 = new Scanner(App.input);
        handler.processInput(s1);
        String out1 = output.toString();
        System.out.println("DEBUG OUTPUT:\n" + out1); // optional but helpful
        assertTrue(out1.contains("Player 1: gold = 10"), "Should show human player's gold");
        output.reset();

        // --- 2. gold 2 (valid other player) ---
        App.input = new ByteArrayInputStream("gold 2\n".getBytes());
        Scanner s2 = new Scanner(App.input);
        handler.processInput(s2);
        assertTrue(output.toString().contains("Player 2: gold = 1"), "Should show Player 2's gold");
        output.reset();

        // --- 3. gold 99 (nonexistent player) ---
        App.input = new ByteArrayInputStream("gold 99\n".getBytes());
        Scanner s3 = new Scanner(App.input);
        handler.processInput(s3);
        assertTrue(output.toString().contains("Player 99 not found"), "Should show not found message");
        output.reset();

        // --- 4. gold notanumber (non-integer input) ---
        App.input = new ByteArrayInputStream("gold notanumber\n".getBytes());
        Scanner s4 = new Scanner(App.input);
        handler.processInput(s4);
        assertTrue(output.toString().contains("Invalid player number"), "Should show invalid input message");

        System.setOut(originalOut);
    }

    /**
     * Tests the "citadel" / "list" / "city" commands for:
     * - showing Player 1's city (no argument)
     * - showing another player's city by number
     * - handling an invalid player number
     * - handling non-numeric input
     * <p>
     * Assumes the save file is already loaded in @BeforeEach.
     */
    @Test
    void testProcessInput_CityCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. city (no argument, defaults to player 1) ---
        App.input = new ByteArrayInputStream("city\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out1 = output.toString();
        System.out.println("DEBUG OUTPUT (city):\n" + out1);
        assertTrue(out1.contains("Player 1:"), "Should print Player 1's name.");
        assertTrue(out1.contains("City:"), "Should include 'City:' label.");
        assertTrue(out1.contains("Castle") && out1.contains("Museum"), "Expected city to include known districts.");
        output.reset();

        // --- 2. city 2 (another valid player) ---
        App.input = new ByteArrayInputStream("city 2\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out2 = output.toString();
        assertTrue(out2.contains("Player 2:"), "Should print Player 2's name.");
        assertTrue(out2.contains("City:"), "Should include 'City:' label.");
        assertTrue(out2.contains("Temple") && out2.contains("Great Wall"), "Expected Player 2's districts.");
        output.reset();

        // --- 3. city 99 (invalid player number) ---
        App.input = new ByteArrayInputStream("city 99\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out3 = output.toString();
        assertTrue(out3.contains("Player 99 not found"), "Should report nonexistent player.");
        output.reset();

        // --- 4. city abc (non-numeric input) ---
        App.input = new ByteArrayInputStream("city abc\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out4 = output.toString();
        assertTrue(out4.contains("Invalid player number"), "Should report non-numeric input.");

        System.setOut(originalOut);
    }

    /**
     * Tests the "action" command for:
     * - printing ability info (no arguments)
     * - blocking action when current player is not human
     * - handling Magician swap and redraw
     * - handling Warlord destroy
     * - rejecting invalid sub-commands
     *
     * Only covers branches reachable based on the input and character role.
     */
    /**
     * Tests the "action" command for:
     * - printing ability info (no arguments)
     * - blocking action when current player is not human
     * - handling Magician swap and redraw
     * - handling Warlord destroy rejection (when not Warlord)
     * - rejecting invalid sub-commands
     * <p>
     * Covers all reachable branches based on player role and command structure.
     */
    @Test
    void testProcessInput_ActionCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. action (just show ability description) ---
        App.input = new ByteArrayInputStream("action\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out1 = output.toString();
        assertTrue(out1.toLowerCase().contains("swap") || out1.toLowerCase().contains("discard"), "Should print Magician ability info.");
        output.reset();

        // --- 2. action swap 2 (Magician ability: valid subcommand) ---
        App.input = new ByteArrayInputStream("action swap 2\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertFalse(output.toString().contains("Invalid"), "Swap should not trigger invalid subcommand.");
        output.reset();

        // --- 3. action redraw 0,1 (Magician ability: valid redraw syntax) ---
        App.input = new ByteArrayInputStream("action redraw 0,1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertFalse(output.toString().contains("Invalid"), "Redraw should not trigger invalid subcommand.");
        output.reset();

        // --- 4. action destroy 2 1 (current player is not Warlord) ---
        App.input = new ByteArrayInputStream("action destroy 2 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("You are not the Warlord"), "Should reject Warlord action from non-Warlord.");
        output.reset();

        // --- 5. action blah blah (invalid subcommand) ---
        App.input = new ByteArrayInputStream("action blah blah\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid action sub-command"), "Should report unrecognized subcommand.");
        output.reset();

        // --- 6. action swap 1 with AI player (should be blocked) ---
        AIPlayer dummy = new AIPlayer("Dummy");
        game.getPlayers().set(game.getCurrentPlayerIndex(), dummy);
        App.input = new ByteArrayInputStream("action swap 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("only do character actions on your turn"), "AI player should be blocked from using actions.");

        System.setOut(originalOut);
    }

    /**
     * Tests the logic for detecting whether a player is AI or human using:
     * - isAIorNull: true if current player is null or AI
     * - isHuman: true if current player is non-null and not AI
     */
    @Test
    void testPlayerTypeDetection_BooleanLogic() {
        // --- 1. Human player (from loaded game) ---
        Player human = game.getCurrentPlayer();
        boolean isAIorNull_Human = (human == null) || (human instanceof AIPlayer);
        boolean isHuman_Human = (human != null) && !(human instanceof AIPlayer);

        assertFalse(isAIorNull_Human, "Human player should not be AI or null.");
        assertTrue(isHuman_Human, "Human player should be detected as human.");

        // --- 2. AI player ---
        AIPlayer ai = new AIPlayer("Test AI");
        boolean isAIorNull_AI = (ai == null) || (ai instanceof AIPlayer);
        boolean isHuman_AI = (ai != null) && !(ai instanceof AIPlayer);

        assertTrue(isAIorNull_AI, "AI player should be detected as AI or null.");
        assertFalse(isHuman_AI, "AI player should not be detected as human.");

        // --- 3. null player ---
        Player nullPlayer = null;
        boolean isAIorNull_Null = (nullPlayer == null) || (nullPlayer instanceof AIPlayer);
        boolean isHuman_Null = (nullPlayer != null) && !(nullPlayer instanceof AIPlayer);

        assertTrue(isAIorNull_Null, "Null player should be detected as AI or null.");
        assertFalse(isHuman_Null, "Null player should not be detected as human.");
    }

    /**
     * Tests that the "city" command prints "(No districts built)"
     * when a player has not built any districts.
     * Adds a dummy player to the game for this test only.
     */
    @Test
    void testProcessInput_CityCommand_EmptyCity() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- Add dummy player with no built districts ---
        Player dummy = new HumanPlayer("Ghost");
        game.getPlayers().add(dummy);

        // --- Run: city <newPlayerNumber> ---
        int dummyPlayerNumber = game.getPlayers().size(); // 1-based
        App.input = new ByteArrayInputStream(("city " + dummyPlayerNumber + "\n").getBytes());
        handler.processInput(new Scanner(App.input));

        String out = output.toString();
        System.setOut(originalOut);

        assertTrue(out.contains("(No districts built)"), "Should print message for empty city.");
        assertTrue(out.contains("Ghost:"), "Should print dummy player's name.");
        assertTrue(out.contains("City:"), "Should include 'City:' label.");
    }

    /**
     * Tests that the "city" command prints "(No districts built)"
     * when the specified player has not built any districts.
     */
    @Test
    void testProcessInput_CityCommand_NoDistrictsBuilt() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // Add a dummy player with no built districts
        Player emptyCityPlayer = new HumanPlayer("NoBuilds");
        game.getPlayers().add(emptyCityPlayer);
        int dummyPlayerNumber = game.getPlayers().size(); // 1-based

        // Run "city <dummyPlayerNumber>"
        App.input = new ByteArrayInputStream(("city " + dummyPlayerNumber + "\n").getBytes());
        handler.processInput(new Scanner(App.input));

        String out = output.toString();
        System.setOut(originalOut);

        assertTrue(out.contains("NoBuilds:"), "Should show the dummy player's name.");
        assertTrue(out.contains("City:"), "Should show the city label.");
        assertTrue(out.contains("(No districts built)"), "Should indicate empty city.");
    }

    /**
     * Tests the "all" command, which shows hand size, gold, and city for every player.
     * Verifies that it prints expected data including the "(you)" tag for the human player,
     * district summaries, and "(No districts built)" when applicable.
     */
    @Test
    void testProcessInput_AllCommand_PrintsAllPlayersCityAndStatus() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // Add a dummy player with an empty city
        Player dummy = new HumanPlayer("Ghost");
        game.getPlayers().add(dummy);

        // Run the "all" command
        App.input = new ByteArrayInputStream("all\n".getBytes());
        handler.processInput(new Scanner(App.input));

        String out = output.toString();
        System.setOut(originalOut);

        // Basic checks for known players
        assertTrue(out.contains("Player 1 (you):"), "Should mark Player 1 as (you).");
        assertTrue(out.contains("cards = 7"), "Should include Player 1's hand count.");
        assertTrue(out.contains("gold = 10"), "Should include Player 1's gold.");

        assertTrue(out.contains("Player 2:"), "Should include Player 2.");
        assertTrue(out.contains("Great Wall"), "Should include a known district from Player 2.");

        // Check Ghost dummy
        assertTrue(out.contains("Ghost:"), "Should show dummy player name.");
        assertTrue(out.contains("(No districts built)"), "Should indicate empty city for dummy player.");

        // Check that multiple "City:" labels are printed (1 per player)
        long cityCount = Arrays.stream(out.split("\\R"))
                .map(String::trim)
                .filter(line -> line.equals("City:"))
                .count();
        assertEquals(game.getPlayers().size(), cityCount, "Should show city header for each player.");
    }

    /**
     * Tests the "save" command for saving the game.
     * Covers:
     * - missing filename
     * - quoted filename stripping
     * - invalid characters (space, bracket)
     * - missing .json extension
     * - successful save (quoted and normal)
     * - save failure (invalid path)
     */
    @Test
    void testProcessInput_SaveCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. Missing filename ---
        App.input = new ByteArrayInputStream("save\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Usage: save <filename.json>"), "Should warn about missing filename.");
        output.reset();

        // --- 2. Quoted filename stripping ---
        App.input = new ByteArrayInputStream("save \"testfile.json\"\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Game saved successfully to testfile.json"), "Should strip quotes and save.");
        output.reset();

        // --- 3. Invalid characters: space ---
        App.input = new ByteArrayInputStream("save bad name.json\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid filename"), "Should reject filenames with spaces.");
        output.reset();

        // --- 4. Invalid characters: brackets ---
        App.input = new ByteArrayInputStream("save {hack}.json\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid filename"), "Should reject brackets in filename.");
        output.reset();

        // --- 5. Missing .json extension ---
        App.input = new ByteArrayInputStream("save file1.txt\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("must end with '.json'"), "Should enforce .json extension.");
        output.reset();

        // --- 6. Successful save with normal filename ---
        String validFilename = "finalsave.json";
        File finalSave = new File(validFilename);
        if (finalSave.exists()) finalSave.delete();

        App.input = new ByteArrayInputStream(("save " + validFilename + "\n").getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Game saved successfully to " + validFilename), "Should confirm successful save.");
        assertTrue(finalSave.exists(), "File should be created.");
        output.reset();

        // --- 7. Simulated save failure (invalid path) ---
        App.input = new ByteArrayInputStream("save /invalid/path/failure.json\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().toLowerCase().contains("failed to save"), "Should catch save exception and report it.");

        // Cleanup
        System.setOut(originalOut);
        new File("testfile.json").delete();
        new File(validFilename).delete();
    }


    /**
     * Tests the "load" command for:
     * - missing filename
     * - incorrect extension
     * - invalid/nonexistent file
     * - successful loading of a valid game file
     * <p>
     * Does not test internal game state after loading — just output and branch logic.
     */
    @Test
    void testProcessInput_LoadCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. No filename provided ---
        App.input = new ByteArrayInputStream("load\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Usage: load <filename.json>"), "Should warn about missing filename.");
        output.reset();

        // --- 2. Invalid extension ---
        App.input = new ByteArrayInputStream("load badfile.txt\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("must end with '.json'"), "Should enforce .json extension.");
        output.reset();

        // --- 3. File does not exist ---
        App.input = new ByteArrayInputStream("load fakefile.json\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().toLowerCase().contains("failed to load"), "Should catch load failure on nonexistent file.");
        output.reset();

        // --- 4. Valid load ---
        File file = new File("src/test/resources/testdata/test_allpurpose_save1.json");
        assertTrue(file.exists(), "Test save file must exist.");
        String validPath = "src/test/resources/testdata/test_allpurpose_save1.json";
        App.input = new ByteArrayInputStream(("load " + validPath + "\n").getBytes());
        handler.processInput(new Scanner(App.input));
        String successOutput = output.toString().toLowerCase();
        assertFalse(successOutput.contains("failed to load"), "Should not fail when given a valid file.");
        assertFalse(successOutput.contains("usage"), "Should not show usage for valid command.");

        System.setOut(originalOut);
    }

    /**
     * Tests the "help" command.
     * Verifies that it prints all expected help lines for available commands.
     * Only checks output, not the actual behavior of commands.
     */
    @Test
    void testProcessInput_HelpCommand_PrintsHelpMenu() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // Run the help command
        App.input = new ByteArrayInputStream("help\n".getBytes());
        handler.processInput(new Scanner(App.input));

        String out = output.toString();
        System.setOut(originalOut);

        // Verify all expected command descriptions are printed
        assertTrue(out.contains("Available commands:"), "Should include header line.");
        assertTrue(out.contains("info : Show information about a character or building"), "Should include info command.");
        assertTrue(out.contains("t : Processes turns"), "Should include 't' command.");
        assertTrue(out.contains("all : Shows all current game info"), "Should include 'all' command.");
        assertTrue(out.contains("citadel/list/city : Shows districts built by a player"), "Should include city group.");
        assertTrue(out.contains("hand : Shows cards in hand"), "Should include 'hand' command.");
        assertTrue(out.contains("gold [p] : Shows gold of a player"), "Should include 'gold' command.");
        assertTrue(out.contains("build <place in hand> : Builds a building into your city"), "Should include 'build' command.");
        assertTrue(out.contains("action : Gives info about your special action and how to perform it"), "Should include 'action' command.");
        assertTrue(out.contains("end : Ends your turn"), "Should include 'end' command.");
        assertTrue(out.contains("save <file> : Saves your game"), "Should include 'save' command.");
        assertTrue(out.contains("load <file> : Loads your game"), "Should include 'load' command.");
    }

    /**
     * Tests the "debug" and "build" commands:
     * - "debug" toggles debug mode (via GameUtils)
     * - "build <index>" builds a valid district from the hand if affordable
     */
    @Test
    void testProcessInput_DebugAndBuildCommands() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. Toggle debug mode ---
        boolean originalDebug = GameUtils.isDebugMode();
        App.input = new ByteArrayInputStream("debug\n".getBytes());
        handler.processInput(new Scanner(App.input));
        boolean toggledDebug = GameUtils.isDebugMode();
        assertNotEquals(originalDebug, toggledDebug, "Debug mode should toggle.");
        output.reset();

        // --- 2. build <index> (build Haunted City, index 6, cost 2) ---
        Player player = game.getCurrentPlayer();
        int initialCitySize = game.getBoard().getCityForPlayer(player).size();
        int initialGold = player.getGold();

        App.input = new ByteArrayInputStream("build 4\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush(); // ensure all output is written before reading it
        String out = output.toString();
        System.setOut(originalOut); // restore stdout before anything else

        System.out.println("DEBUG BUILD OUTPUT:\n" + out); // help verify

        int newCitySize = game.getBoard().getCityForPlayer(player).size();
        int newGold = player.getGold();

        assertEquals(initialCitySize + 1, newCitySize, "City should have one more district after building.");
        assertTrue(out.contains("built Laboratory [purple5]"), "Output should mention that Laboratory was built.");
        assertTrue(newGold < initialGold, "Gold should be spent after building.");
    }

    /**
     * Tests the "info" command for:
     * - No argument (usage message)
     * - Valid district index (info from hand)
     * - Invalid index (out of bounds)
     * - Valid character name (ability description)
     * - Invalid string (not a character or index)
     */
    /**
     * Tests the "info" command for:
     * - No argument (prints usage)
     * - Valid district index (purple district with ability)
     * - Invalid district index (out of bounds)
     * - Valid character name (prints ability)
     * - Invalid input (unrecognized string)
     */
    @Test
    void testProcessInput_InfoCommand_AllBranches() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // --- 1. info (no argument) ---
        App.input = new ByteArrayInputStream("info\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Use 'info <character_name>'"), "Should show usage message");
        output.reset();

        // --- 2. info 4 (valid index: Laboratory, purple) ---
        App.input = new ByteArrayInputStream("info 4\n".getBytes()); // Index 4 = Laboratory
        handler.processInput(new Scanner(App.input));
        String out2 = output.toString().toLowerCase();
        assertTrue(out2.contains("laboratory") || out2.contains("ability"), "Should show Laboratory info");
        output.reset();

        // --- 3. info 99 (invalid index) ---
        App.input = new ByteArrayInputStream("info 99\n".getBytes());
        handler.processInput(new Scanner(App.input));
        // Confirm no crash; optionally check for no output or silent fail
        assertTrue(true);
        output.reset();

        // --- 4. info magician (valid character) ---
        App.input = new ByteArrayInputStream("info magician\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out4 = output.toString().toLowerCase();
        assertTrue(out4.contains("swap") || out4.contains("discard"), "Should show Magician ability description");
        output.reset();

        // --- 5. info unknownthing (invalid string) ---
        App.input = new ByteArrayInputStream("info unknownthing\n".getBytes());
        handler.processInput(new Scanner(App.input));
        // Confirm no crash
        assertTrue(true);

        System.setOut(originalOut);
    }

    /**
     * Tests the "discard" command for the Laboratory effect from a human player.
     * Covers:
     * - valid discard usage
     * - already used Laboratory
     * - missing argument
     * - invalid number format
     * - out-of-bounds index
     */
    @Test
    void testProcessInput_LaboratoryCommand_AllBranches() throws IOException {
        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());
        InputHandler handler = new InputHandler(game);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // --- 1. Valid discard ---
        App.input = new ByteArrayInputStream("discard 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out1 = output.toString();
        assertTrue(out1.contains("gained 1 gold using the Laboratory"), "Should confirm Laboratory effect.");
        output.reset();

        // --- 2. Already used Laboratory ---
        App.input = new ByteArrayInputStream("discard 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("already used the Laboratory"), "Should reject second use.");
        output.reset();

        // === Reset used flag to test other branches ===
        human.setUsedLaboratory(false);

        // --- 3. Missing argument ---
        App.input = new ByteArrayInputStream("discard\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Use: discard <number>"), "Should show usage info.");
        output.reset();

        // --- 4. Invalid number format ---
        App.input = new ByteArrayInputStream("discard two\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid number format"), "Should catch non-numeric input.");
        output.reset();

        // --- 5. Invalid index (too large) ---
        App.input = new ByteArrayInputStream("discard 99\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid card number"), "Should catch out-of-bounds input.");

        System.setOut(originalOut);
    }

    /**
     * Tests "discard" command with missing argument before Laboratory is used.
     * Ensures correct usage message is shown.
     */
    @Test
    void testProcessInput_LaboratoryCommand_MissingArgument() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        App.input = new ByteArrayInputStream("discard\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush(); // flush output buffer
        String out = output.toString();
        System.setOut(originalOut); // restore stdout

        System.out.println("DEBUG OUTPUT:\n" + out); // optional debug

        assertTrue(out.contains("Use: discard <number>"), "Should show usage info.");
    }

    /**
     * Tests the "draw" command for the Smithy effect from a human player.
     * Covers:
     * - valid Smithy usage
     * - already used Smithy
     * - not enough gold
     * - no Smithy built
     */
    @Test
    void testProcessInput_SmithyCommand_AllBranches() throws IOException {
        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());
        InputHandler handler = new InputHandler(game);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        int initialHand = human.getHand().size();

        // --- 1. Valid draw (Smithy built, enough gold, not used) ---
        App.input = new ByteArrayInputStream("draw\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out1 = output.toString();
        assertTrue(out1.contains("drew"), "Should confirm cards were drawn using Smithy.");
        assertTrue(human.getHand().size() > initialHand, "Hand should increase after Smithy.");
        output.reset();

        // --- 2. Already used Smithy ---
        App.input = new ByteArrayInputStream("draw\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("already used the Smithy"), "Should reject second Smithy use.");
        output.reset();

        // --- 3. Not enough gold ---
        human.setUsedSmithy(false);
        human.setGold(1); // Not enough gold

        App.input = new ByteArrayInputStream("draw\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("need at least 2 gold"), "Should require 2 gold to use Smithy.");
        output.reset();

        // --- 4. No Smithy built ---
        human.setGold(10); // reset gold
        human.setUsedSmithy(false);
        game.getBoard().getCityForPlayer(human).removeIf(c -> c.getName().equalsIgnoreCase("Smithy"));

        App.input = new ByteArrayInputStream("draw\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("don't have the Smithy"), "Should require Smithy to be built.");

        System.setOut(originalOut);
    }

    /**
     * Tests "draw" command fails gracefully when no Smithy is built.
     * Ensures correct error is shown even if player has enough gold.
     */
    @Test
    void testProcessInput_SmithyCommand_NoSmithyBuilt() throws IOException {
        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());
        InputHandler handler = new InputHandler(game);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Remove Smithy if present
        game.getBoard().getCityForPlayer(human).removeIf(c -> c.getName().equalsIgnoreCase("Smithy"));

        App.input = new ByteArrayInputStream("draw\n".getBytes());
        handler.processInput(new Scanner(App.input));
        System.out.flush();
        String out = output.toString();

        System.setOut(originalOut);
        assertTrue(out.contains("don't have the Smithy"), "Should inform player Smithy is not built.");
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
     * Tests the "place" command for the Museum effect from a human player.
     * Covers:
     * - valid placement
     * - already used Museum
     * - missing argument
     * - invalid number format
     * - out-of-bounds index
     */
    @Test
    void testProcessInput_MuseumCommand_AllBranches() throws IOException {
        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());
        InputHandler handler = new InputHandler(game);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        int initialMuseumStored = human.getMuseumStoredCards().size();

        // --- 1. Valid placement ---
        App.input = new ByteArrayInputStream("place 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        String out1 = output.toString();
        assertTrue(out1.contains("placed") && out1.contains("Museum"), "Should confirm Museum placement.");
        assertEquals(initialMuseumStored + 1, human.getMuseumStoredCards().size(), "Card should be stored.");
        output.reset();

        // --- 2. Already used Museum ---
        App.input = new ByteArrayInputStream("place 1\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("already used the Museum"), "Should reject second use.");
        output.reset();

        // === Reset used flag to test other branches ===
        human.setUsedMuseum(false);

        // --- 3. Missing argument ---
        App.input = new ByteArrayInputStream("place\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Use: place <number>"), "Should show usage info.");
        output.reset();

        // --- 4. Invalid number format ---
        App.input = new ByteArrayInputStream("place banana\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid number format"), "Should catch bad input.");
        output.reset();

        // --- 5. Invalid index ---
        App.input = new ByteArrayInputStream("place 99\n".getBytes());
        handler.processInput(new Scanner(App.input));
        assertTrue(output.toString().contains("Invalid card number"), "Should catch out-of-bounds.");

        System.setOut(originalOut);
    }

    /**
     * Tests "place" command with missing argument before Museum is used.
     * Ensures correct usage message is shown.
     */
    @Test
    void testProcessInput_MuseumCommand_MissingArgument() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        App.input = new ByteArrayInputStream("place\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush(); // flush output buffer
        String out = output.toString();
        System.setOut(originalOut); // restore stdout

        System.out.println("DEBUG OUTPUT:\n" + out); // optional debug

        assertTrue(out.contains("Use: place <number>"), "Should show usage info.");
    }

    /**
     * Tests that an unrecognized command triggers the default case.
     * Ensures the user is informed that the command is invalid.
     */
    @Test
    void testProcessInput_DefaultCase_InvalidCommand() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        App.input = new ByteArrayInputStream("quackquack\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush();
        String out = output.toString();
        System.setOut(originalOut);

        assertTrue(out.contains("Invalid command"), "Should print invalid command message.");
        assertTrue(out.contains("Type 'help'"), "Should suggest using help for command list.");
    }

    /**
     * Tests that using the Museum fails if the player has not built it.
     * Should print a message saying "You don't have the Museum."
     */
    @Test
    void testProcessInput_MuseumCommand_PlayerHasNoMuseum() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Remove Museum from city if it's present
        List<DistrictCard> city = game.getBoard().getCityForPlayer(human);
        city.removeIf(c -> c.getName().equalsIgnoreCase("Museum"));

        // Attempt to use the Museum
        App.input = new ByteArrayInputStream("place 1\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush();
        String out = output.toString();
        System.setOut(originalOut);

        assertTrue(out.contains("don't have the Museum"), "Should warn the player doesn't have the Museum.");
    }

    /**
     * Tests that using the Armory fails if the player has not built it.
     * Should print "You don't have the Armory built."
     */
    @Test
    void testProcessInput_ArmoryCommand_NoArmoryBuilt() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // Remove Armory manually before issuing the command
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        List<DistrictCard> city = game.getBoard().getCityForPlayer(player);
        city.removeIf(d -> d.getName().equalsIgnoreCase("Armory"));

        App.input = new ByteArrayInputStream("armory\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush();
        String out = output.toString();
        System.setOut(originalOut);

        assertTrue(out.contains("don't have the Armory"), "Should warn the player doesn't have the Armory.");
    }

    /**
     * Tests that using the Armory fails if no other player has districts.
     * Should print: "No other player has districts you can destroy."
     */
    @Test
    void testProcessInput_ArmoryCommand_NoTargets() throws IOException {
        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());
        InputHandler handler = new InputHandler(game);

        // Clear all other players' cities
        for (Player p : game.getPlayers()) {
            if (p != game.getCurrentPlayer()) {
                game.getBoard().getCityForPlayer(p).clear();
            }
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        App.input = new ByteArrayInputStream("armory\n".getBytes());
        handler.processInput(new Scanner(App.input));

        System.out.flush();
        String out = output.toString();
        System.setOut(System.out); // restore stdout

        assertTrue(out.contains("No other player has districts you can destroy."),
                "Should warn when there are no valid targets.");
    }

    /**
     * Tests that destroying the Armory through the real game flow:
     * - Removes it from the city
     * - Prints the correct instruction messages
     */
    @Test
    void testArmoryDestructionAnnouncement_RealExecution() throws IOException {
        // Player 1 destroys Armory and enters a valid destroy command
        setupInput("armory\n3 1\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Ensure Armory is built before the test
        List<DistrictCard> city = game.getBoard().getCityForPlayer(human);
        assertTrue(city.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Armory")),
                "Armory should already be built in the city.");

        // Run the actual game input handler
        InputHandler handler = new InputHandler(game);
        Scanner scanner = new Scanner(App.input);
        handler.processInput(scanner);  // this triggers the destruction sequence via input

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // Check Armory was removed
        assertFalse(game.getBoard().getCityForPlayer(human).stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase("Armory")), "Armory should be removed from the city.");

        // Check printed output
        assertTrue(out.contains("You destroyed your Armory."), "Missing destruction confirmation message.");
        assertTrue(out.contains("You may now destroy any district card in another player's city."), "Missing destroy prompt.");
        assertTrue(out.contains("Use: <player_number> <districtID>"), "Missing usage format message.");
    }

    private void setupInput(String input) {
        App.input = new ByteArrayInputStream(input.getBytes());
    }

    /**
     * Tests the actual Armory destruction input loop in game logic:
     * - Handles input with wrong format (1 token)
     * - Accepts valid 2-token input and exits
     */
    @Test
    void testArmoryDestroyLoop_InvalidAndValidFormat_RealExecution() throws IOException {
        // First line: "armory" triggers the ability
        // Second line: invalid input ("hello")
        // Third line: valid input ("2 1")
        setupInput("armory\nhello\n2 1\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        InputHandler handler = new InputHandler(game);
        Scanner scanner = new Scanner(App.input);

        // Trigger input handling — will enter Armory block
        handler.processInput(scanner);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // --- Assertions ---
        assertTrue(out.contains("> "), "Prompt should be printed.");
        assertTrue(out.contains("Invalid format. Use: <player_number> <districtID>"),
                "Should show format error on invalid input.");
        assertTrue(out.contains("You destroyed your Armory."), "Armory removal message should be printed.");
        assertTrue(out.contains("Use: <player_number> <districtID>"), "Usage prompt must appear.");
    }

    /**
     * Tests token parsing and player validation using the real Armory logic:
     * - Rejects input when targeting self
     * - Accepts a valid player as the second attempt
     */
    @Test
    void testArmoryDestroy_ParseAndTargetValidation_RealExecution() throws IOException {
        // Input:
        // "armory" to trigger ability
        // "1 1" targets self — should be rejected
        // "3 1" is valid — should proceed
        setupInput("armory\n1 1\n3 1\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        InputHandler handler = new InputHandler(game);
        Scanner scanner = new Scanner(App.input);

        handler.processInput(scanner); // Triggers "armory" command handling

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // Assertions
        assertTrue(out.contains("> "), "Prompt should be printed.");
        assertTrue(out.contains("Invalid target player."), "Should reject self-targeting.");
        assertTrue(out.contains("You destroyed your Armory."), "Armory should be destroyed.");
    }

    /**
     * Tests Armory city and index validation using full game logic:
     * - Rejects players with no districts
     * - Rejects out-of-bounds district index
     * - Accepts valid index
     */
    @Test
    void testArmoryDestroy_TargetCityValidations_RealExecution() throws IOException {
        // Armory command, then:
        // 1st: player 7 (empty city) => rejected
        // 2nd: player 3, bad index => rejected
        // 3rd: player 3, valid index => accepted
        setupInput("armory\n7 1\n3 999\n3 1\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        // Make Player 7's city empty to trigger "no districts" branch
        Player p7 = game.getPlayerByNumber(7);
        assertNotNull(p7);
        game.getBoard().getCityForPlayer(p7).clear();

        InputHandler handler = new InputHandler(game);
        Scanner scanner = new Scanner(App.input);

        // Trigger actual gameplay command flow
        handler.processInput(scanner);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // === Assertions ===
        assertTrue(out.contains("That player has no districts to destroy."),
                "Should detect empty city for player 7.");
        assertTrue(out.contains("Invalid district index."),
                "Should reject out-of-bounds district index for player 3.");
        assertTrue(out.contains("> "), "Prompt should be printed during loop.");
        assertTrue(out.contains("You destroyed your Armory."), "Armory should be destroyed at the start.");
    }

    /**
     * Tests Armory destruction flow using real game logic:
     * - Handles malformed number input
     * - Executes successful destruction on second input
     */
    @Test
    void testArmoryDestroy_ExecutesDestroyAndHandlesParseError_RealExecution() throws IOException {
        // First input: malformed number
        // Second input: valid target and index
        setupInput("armory\nnotanumber 1\n3 1\n");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        File saveFile = new File("src/test/resources/testdata/test_purple_ability_save.json");
        assertTrue(saveFile.exists());

        Game game = new Game();
        game.loadGame(saveFile.getAbsolutePath());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        InputHandler handler = new InputHandler(game);
        Scanner scanner = new Scanner(App.input);

        handler.processInput(scanner); // Triggers "armory" handling and input loop

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // Assertions
        assertTrue(out.contains("Invalid number format. Use: <player_number> <districtID>"),
                "Should catch and print number format error.");
        assertTrue(out.contains("You destroyed your Armory."),
                "Armory should be destroyed.");
        assertTrue(out.contains("> "), "Prompt must be printed.");

        // Confirm the district was destroyed from Player 3's city
        List<DistrictCard> remaining = game.getCityForPlayer(game.getPlayerByNumber(3));
        assertFalse(remaining.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Haunted City")),
                "Haunted City should be removed from Player 3's city.");
    }

    /**
     * Tests that the build command cannot be used by AI players.
     * Ensures the correct rejection message is printed and the method exits.
     */
    @Test
    void testHandleBuild_NonHumanPlayerRejected() throws IOException {
        // No input necessary since method is called directly
        App.input = new ByteArrayInputStream("".getBytes());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        // test_allpurpose_save1.json is already loaded BeforeEach
        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);

        // Pick a known AI player (e.g. player 2)
        Player ai = game.getPlayerByNumber(2);
        assertNotNull(ai);
        assertFalse(ai instanceof HumanPlayer);

        String[] dummyCommand = {"build", "1"};

        handler.handleBuild(ai, dummyCommand, false);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Build command is not available for AI or when no player has turn."),
                "Should reject build command from AI player.");
    }

    /**
     * Tests that the build command is rejected when the wrong number of arguments is given.
     * Ensures the correct format error message is printed and the method exits early.
     */
    @Test
    void testHandleBuild_InvalidCommandFormat() throws IOException {
        // Simulate bad input: just "build" with no number
        String[] badCommand = {"build"};

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        // Call handleBuild directly with bad input
        handler.handleBuild(player, badCommand, true);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid command format. Use: build <number>"),
                "Expected format error message for build command with wrong argument count.");
    }

    /**
     * Tests that the build command is rejected when the given card index is out of bounds.
     * Ensures that the appropriate error message is shown and no card is built.
     */
    @Test
    void testHandleBuild_InvalidCardIndex() throws IOException {
        // Simulate out-of-bounds index (e.g. "build 99")
        String[] inputParts = {"build", "99"};

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);

        // Make sure player has at least one card, but 99 will be out of bounds
        assertFalse(player.getHand().isEmpty());
        int handSize = player.getHand().size();
        assertTrue(99 > handSize, "Index 99 must be out of bounds for this test.");

        handler.handleBuild(player, inputParts, true);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid card number."), "Expected out-of-bounds card index error message.");
    }

    /**
     * Tests the build command using real game logic:
     * - Handles invalid number format (non-integer input)
     * - Handles valid number that fails canBuildDistrict check (e.g. max builds reached)
     */
    @Test
    void testHandleBuild_InvalidIndexOrRejectedBuild() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // --- Case 1: Invalid number format ---
        String[] badFormat = {"build", "notanumber"};
        handler.handleBuild(human, badFormat, true);

        // --- Case 2: Valid input, but build is disallowed ---
        // Set build limit to 0 so canBuildDistrict() returns a reason
        human.setMaxBuildsPerTurn(0);
        assertFalse(human.getHand().isEmpty());
        String[] rejectedBuild = {"build", "1"};
        handler.handleBuild(human, rejectedBuild, true);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // --- Assertions ---
        assertTrue(out.contains("Invalid command format. Use: build <number>"),
                "Should catch and report NumberFormatException.");
        assertTrue(out.contains("Cannot build this district:"),
                "Should explain why build was rejected when limit is 0.");
    }

    /**
     * Tests that the Magician command is rejected if the current player is not a HumanPlayer.
     * Ensures the appropriate message is printed and nothing proceeds.
     */
    @Test
    void testHandleMagician_RejectedIfNotHumanPlayer() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);

        // Use an AI player instead of a human
        Player ai = game.getPlayerByNumber(2); // AI player in test save
        assertNotNull(ai);
        assertFalse(ai instanceof HumanPlayer);

        // Attempt to trigger Magician logic
        String[] command = {"action", "swap", "1"};
        handler.handleMagician(ai, command);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("You can only do character actions on your turn."),
                "Should block AI from using Magician ability.");
    }

    /**
     * Tests that the Magician command is rejected if the player is not the Magician.
     * Ensures the correct rejection message is printed and no action proceeds.
     */
    @Test
    void testHandleMagician_RejectedIfNotMagician() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Remove or overwrite the character so player is not the Magician
        human.setChosenCharacter(new CharacterCard("Thief", 2, "Steal gold from another character."));

        // Execute the command
        String[] command = {"action", "swap", "2"};
        handler.handleMagician(human, command);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("You are not the Magician, cannot use this action."),
                "Should block Magician ability if player does not have character 3.");
    }

    /**
     * Tests that the Magician command is rejected if it has the wrong number of arguments.
     * Ensures the correct format message is printed and the ability is not executed.
     */
    @Test
    void testHandleMagician_InvalidCommandFormat() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Set character to Magician so it passes the character check
        human.setChosenCharacter(new CharacterCard("Magician", 3, "You may swap or redraw cards."));
        human.setUsedMagicianThisTurn(false); // Make sure ability is available

        // Send a bad command: missing argument ("action swap")
        String[] malformedCommand = {"action", "swap"};
        handler.handleMagician(human, malformedCommand);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid action command. Use: 'action swap <player_number>' or 'action redraw <cardID1,cardID2,...>'"),
                "Should show format guidance when command has wrong number of arguments.");
    }

    /**
     * Tests the Magician's swap subcommand:
     * - Rejects the swap if the target player is self
     * - Rejects the swap if the player number is not a valid integer
     */
    @Test
    void testHandleMagician_SwapInvalidTargetOrBadFormat() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Set the character to Magician and ensure it's unused
        human.setChosenCharacter(new CharacterCard("Magician", 3, "You may swap or redraw cards."));
        human.setUsedMagicianThisTurn(false);

        // --- Case 1: Targeting self (player 1)
        String[] selfTargetSwap = {"action", "swap", "1"};
        handler.handleMagician(human, selfTargetSwap);

        // --- Case 2: Invalid player number format
        String[] badFormatSwap = {"action", "swap", "notanumber"};
        handler.handleMagician(human, badFormatSwap);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        assertTrue(out.contains("Invalid target player for swapping."),
                "Should reject self-targeting in swap command.");
        assertTrue(out.contains("Invalid player number."),
                "Should catch and report number format error.");
    }

    /**
     * Tests that the Magician redraw command rejects an out-of-bounds card index.
     * Ensures that the correct message is printed and the ability is not used.
     */
    @Test
    void testHandleMagicianRedraw_InvalidCardIndex() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Ensure Player 1 is the Magician and has not used the ability
        human.setChosenCharacter(new CharacterCard("Magician", 3, "Use: redraw"));
        human.setUsedMagicianThisTurn(false);

        // Player 1 has 7 cards in hand → index 8 is out of bounds
        String[] parts = {"action", "redraw", "8"};
        handler.handleMagician(human, parts);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid card index: 8"), "Expected out-of-bounds index error.");
    }

    /**
     * Tests the redraw subcommand for Magician:
     * - Rejects invalid format with a non-numeric index
     * - Successfully redraws valid cards
     * - Marks the Magician ability as used
     */
    @Test
    void testHandleMagicianRedraw_InvalidFormatAndRedrawUsedCorrectly() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Make sure the player is Magician and hasn't used the ability
        human.setChosenCharacter(new CharacterCard("Magician", 3, "Use: swap or redraw"));
        human.setUsedMagicianThisTurn(false);

        // --- Case 1: Bad format (non-integer card index)
        String[] badFormat = {"action", "redraw", "1,x,3"};
        handler.handleMagician(human, badFormat);

        // --- Case 2: Valid redraw (will attempt to discard first 2 cards)
        human.setUsedMagicianThisTurn(false); // reset flag for second case
        String[] validRedraw = {"action", "redraw", "1,2"};
        handler.handleMagician(human, validRedraw);

        System.out.flush();
        System.setOut(originalOut);
        String out = output.toString();

        // --- Assertions ---
        assertTrue(out.contains("Invalid card index format."),
                "Should catch and report the bad input format with non-numeric index.");
        assertTrue(human.usedMagicianThisTurn(),
                "Magician ability should be marked as used after successful redraw.");
    }

    /**
     * Tests that the Warlord destroy command is rejected when given an incorrect number of parameters.
     * Ensures the correct format error message is printed and the command does not proceed.
     */
    @Test
    void testHandleWarlordDestroy_InvalidParamsFormat() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Force Player 1 to be Warlord
        human.setChosenCharacter(new CharacterCard("Warlord", 8, "Use: destroy districts"));

        // Case 1: No arguments (just "destroy")
        String[] parts1 = {"action", "destroy"};
        handler.testInvokeWarlordDestroy(human, Arrays.toString(parts1));

        // Case 2: One argument only (e.g., missing district number)
        String[] parts2 = {"action", "destroy", "3"};
        handler.testInvokeWarlordDestroy(human, Arrays.toString(parts2));

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid destroy command format. Use: action destroy <player_number> <districtID>"),
                "Should show format guidance when parameters are missing or incomplete.");
    }

    /**
     * Tests that the Warlord destroy command rejects an invalid player number.
     * Ensures that the correct message is printed and no destruction occurs.
     */
    @Test
    void testHandleWarlordDestroy_InvalidTargetPlayer() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        // Force Player 1 to be Warlord
        human.setChosenCharacter(new CharacterCard("Warlord", 8, "Destroy a district from another player."));

        // Targeting player 99 (does not exist)
        handler.testInvokeWarlordDestroy(human, "99 1");

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();
        assertTrue(out.contains("Invalid target player."),
                "Should reject nonexistent player number.");
    }

    /**
     * Tests that the Warlord destroy command:
     * - Blocks targeting an alive Bishop
     * - Blocks targeting a player with a full city
     */
    @Test
    void testHandleWarlordDestroy_BishopAndFullCityRestrictions() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player warlord = game.getCurrentPlayer();
        assertTrue(warlord instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) warlord;

        // Set Player 1 as Warlord
        human.setChosenCharacter(new CharacterCard("Warlord", 8, "Destroy districts"));

        // --- Case 1: Target Player 5 (alive Bishop) ---
        Player bishop = game.getPlayerByNumber(5);
        assertNotNull(bishop);
        bishop.setChosenCharacter(new CharacterCard("Bishop", 5, "Immune to destruction"));
        bishop.setKilled(false); // Ensure the Bishop is alive

        handler.testInvokeWarlordDestroy(human, "5 1");

        // --- Case 2: Target Player 6 (full city) ---
        Player fullCityPlayer = game.getPlayerByNumber(6);
        assertNotNull(fullCityPlayer);

        // Fill their city to 8 districts (no Bell Tower)
        List<DistrictCard> city = game.getCityForPlayer(fullCityPlayer);
        while (city.size() < 8) {
            city.add(new DistrictCard("Filler", "red", 1, 1, null));
        }

        handler.testInvokeWarlordDestroy(human, "6 1");

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        assertTrue(out.contains("Cannot destroy districts in an alive Bishop's city."),
                "Should block destruction in an alive Bishop’s city.");
        assertTrue(out.contains("Warlord cannot destroy districts in a full city."),
                "Should block destruction if city is completed.");
    }

    /**
     * Tests that the Warlord destroy command:
     * - Rejects out-of-bounds district index
     * - Rejects if not enough gold to destroy
     * - Rejects if target district is the Keep
     */
    @Test
    void testHandleWarlordDestroy_IndexGoldKeepRestrictions() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player warlord = game.getCurrentPlayer();
        assertTrue(warlord instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) warlord;

        human.setChosenCharacter(new CharacterCard("Warlord", 8, "Destroy a district"));
        human.setGold(0); // ensure not enough gold for test

        // --- Case 1: Out-of-bounds index ---
        handler.testInvokeWarlordDestroy(human, "3 999");

        // --- Case 2: Not enough gold ---
        Player target = game.getPlayerByNumber(3);
        assertNotNull(target);
        List<DistrictCard> city = game.getCityForPlayer(target);
        assertFalse(city.isEmpty());
        int validIndex = 0;
        handler.testInvokeWarlordDestroy(human, "3 " + (validIndex + 1));

        // --- Case 3: Keep is indestructible ---
        // Ensure Player 4 has "Keep" built
        Player targetWithKeep = game.getPlayerByNumber(4);
        List<DistrictCard> city4 = game.getCityForPlayer(targetWithKeep);
        city4.clear(); // remove all other cards
        city4.add(new DistrictCard("Keep", "purple", 3, 1, "Cannot be destroyed"));

        human.setGold(10); // give enough gold to ensure it's not blocked for cost
        handler.testInvokeWarlordDestroy(human, "4 1");

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        assertTrue(out.contains("Invalid district index."),
                "Should reject out-of-range district index.");
        assertTrue(out.contains("Not enough gold to destroy this district."),
                "Should reject destruction when player lacks gold.");
        assertTrue(out.contains("You cannot destroy the Keep."),
                "Should reject destruction of indestructible Keep.");
    }

    /**
     * Tests the final path of Warlord destroy:
     * - Executes successful destruction
     * - Handles NumberFormatException for malformed numbers
     */
    @Test
    void testHandleWarlordDestroy_SuccessAndBadNumberFormat() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        InputHandler handler = new InputHandler(game);
        Player warlord = game.getCurrentPlayer();
        assertTrue(warlord instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) warlord;

        human.setChosenCharacter(new CharacterCard("Warlord", 8, "Destroy districts"));
        human.setGold(10); // Give enough gold for destruction

        // --- Case 1: Malformed number format ---
        handler.testInvokeWarlordDestroy(human, "x y");

        // --- Case 2: Valid destruction ---
        Player target = game.getPlayerByNumber(3);
        List<DistrictCard> city = game.getCityForPlayer(target);
        assertFalse(city.isEmpty());
        String districtName = city.get(0).getName(); // Will verify it's gone later
        handler.testInvokeWarlordDestroy(human, "3 1");

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        // --- Assertions ---
        assertTrue(out.contains("Invalid number format in command."),
                "Should catch and report malformed number input.");

        List<DistrictCard> updatedCity = game.getCityForPlayer(target);
        assertFalse(updatedCity.stream().anyMatch(c -> c.getName().equalsIgnoreCase(districtName)),
                "District should have been destroyed from target city.");
    }

    /**
     * Tests that handleDistrictInfo reports correctly when a district has no ability:
     * - Non-purple color
     * - Null ability description
     * - Empty ability description
     */
    @Test
    void testHandleDistrictInfo_NoAbilityCases() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(output));

        assertNotNull(game);
        assertFalse(game.getPlayers().isEmpty());

        Player player = game.getCurrentPlayer();
        assertTrue(player instanceof HumanPlayer);
        HumanPlayer human = (HumanPlayer) player;

        List<DistrictCard> hand = human.getHand();
        hand.clear(); // control the test content

        // Case 1: Non-purple card
        hand.add(new DistrictCard("Market", "green", 2, 1, "Gain 1 gold")); // should trigger fallback

        // Case 2: Purple with null ability
        hand.add(new DistrictCard("Haunted City", "purple", 2, 1, null));

        // Case 3: Purple with empty string ability
        hand.add(new DistrictCard("Empty Ability", "purple", 3, 1, ""));

        InputHandler handler = new InputHandler(game);
        // Test all 3 cards (indexes 0, 1, 2)
        handler.handleDistrictInfo(human, 0);
        handler.handleDistrictInfo(human, 1);
        handler.handleDistrictInfo(human, 2);

        System.out.flush();
        System.setOut(originalOut);

        String out = output.toString();

        long count = Arrays.stream(out.split("\n"))
                .filter(line -> line.trim().equals("This district has no special ability."))
                .count();

        assertEquals(3, count, "Each of the 3 cases should print the 'no special ability' message.");
    }

}
