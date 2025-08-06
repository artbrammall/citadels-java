package citadels.state;

import citadels.core.Game;
import citadels.model.AIPlayer;
import citadels.model.DistrictCard;
import citadels.model.HumanPlayer;
import citadels.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scaffold for unit tests of the Board class.
 * Board manages each player's city (list of built districts).
 */
public class BoardTest {

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
     * Sanity check to confirm that the game loads correctly and includes the expected number of players.
     */
    @Test
    void exampleTest() {
        assertNotNull(game);
        assertEquals(7, game.getPlayers().size(), "Should have 7 players loaded");
    }

    /**
     * Verifies that getCityForPlayer() correctly returns Player 1’s built city as defined in the test save file.
     * Also checks that the built districts match the expected names and order.
     */
    @Test
    void testGetCityForPlayer_Player1CityCorrectlyReturned() {
        // Given the game was loaded from a save file
        Player player1 = game.getPlayers().get(0); // Player 1 is always index 0

        // When retrieving their city
        List<DistrictCard> player1City = game.getBoard().getCityForPlayer(player1);

        // Then it should return the correct number of built districts and match known names
        assertEquals(6, player1City.size(), "Player 1 should have built 6 districts");

        // Optional: check names and order if needed
        assertEquals("Quarry", player1City.get(0).getName());
        assertEquals("Castle", player1City.get(1).getName());
        assertEquals("Castle", player1City.get(2).getName());
        assertEquals("Tavern", player1City.get(3).getName());
        assertEquals("Temple", player1City.get(4).getName());
        assertEquals("Museum", player1City.get(5).getName());
    }

    /**
     * Ensures that querying getCityForPlayer() with an unregistered player returns a valid but empty list.
     * Prevents NullPointerException or crash from unauthorized queries.
     */
    @Test
    void testGetCityForPlayer_UnregisteredPlayer_ReturnsEmptyList() {
        // Create a player that was not part of the game
        Player externalPlayer = new HumanPlayer("Ghost");

        // Query the board — this player was never registered
        List<DistrictCard> city = game.getBoard().getCityForPlayer(externalPlayer);

        // Validate fallback behavior
        assertNotNull(city, "Returned list should not be null");
        assertTrue(city.isEmpty(), "City should be empty for unregistered player");
    }

    /**
     * Tests that getPlayerCityMap() returns a complete and correct mapping of each player to their built city.
     * Confirms the map includes all 7 players and that Player 1’s city matches expectations.
     */
    @Test
    void testGetPlayerCityMap_ReturnsCorrectMapping() {
        // Get the board from the loaded game
        Board board = game.getBoard();
        Map<Player, List<DistrictCard>> map = board.getPlayerCityMap();

        // Assert map is not null or empty
        assertNotNull(map, "City map should not be null");
        assertEquals(7, map.size(), "There should be 7 players in the map");

        // Check one of the actual players
        Player player1 = game.getPlayers().get(0); // "Player 1"
        assertTrue(map.containsKey(player1), "Player 1 should be in the city map");

        // Verify contents of Player 1's city
        List<DistrictCard> player1City = map.get(player1);
        assertEquals(6, player1City.size(), "Player 1 should have built 6 districts");

        // Optional: Check a specific district
        assertEquals("Quarry", player1City.get(0).getName());
    }
}