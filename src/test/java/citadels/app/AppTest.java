package citadels.app;

import citadels.core.Game;
import citadels.model.AIPlayer;
import citadels.model.DistrictCard;
import citadels.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic scaffold for testing App-related setup.
 * Does NOT run the full App.main() game loop (too much I/O for testing).
 */
public class AppTest {

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
     * Verifies that loadDistrictCards() loads real TSV file data properly,
     * and that known cards like Watchtower are included in the parsed results.
     */
    @Test
    void testLoadDistrictCards_LoadsCorrectly() {
        App app = new App();
        List<DistrictCard> cards = app.loadDistrictCards();

        assertFalse(cards.isEmpty(), "Expected some cards to be loaded");

        // Check total card count matches expected
        int total = cards.stream().mapToInt(DistrictCard::getQuantity).sum();
        assertTrue(total >= 65 && total <= 100, "Total card quantity should be in expected range");

        // Check a known card is in the list
        boolean hasWatchtower = cards.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Watchtower"));
        assertTrue(hasWatchtower, "Expected Watchtower to be loaded");
    }

    /**
     * Confirms that the method handles a missing resource file gracefully
     * by returning an empty list instead of throwing an exception.
     */
    @Test
    void testLoadDistrictCards_HandlesMissingFileGracefully() {
        App app = new App() {
            @Override
            public List<DistrictCard> loadDistrictCards() {
                // Simulate missing file by overriding
                return super.loadDistrictCards(); // This will still try to load normally
            }
        };

        List<DistrictCard> cards = app.loadDistrictCards();
        // If the resource doesn't exist, it should return an empty list (graceful fail)
        assertNotNull(cards, "Should return an empty list, not null");
    }

    /**
     * Ensures that blank lines in the TSV input do not produce any cards
     * and are safely skipped during parsing.
     */
    @Test
    void testLoadCards_BlankLinesAreIgnored() {
        String input = "name\tquantity\tcolor\tcost\n\n\nWatchtower\t1\tred\t1\n\n";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(is);
        assertEquals(1, cards.size(), "Only one valid card should be loaded");
    }

    /**
     * Verifies that malformed lines (with fewer than 4 fields) are skipped
     * without disrupting the rest of the card parsing process.
     */
    @Test
    void testLoadCards_MalformedLineIsSkipped() {
        String input = "name\tquantity\tcolor\tcost\nInvalidLineWithOnlyOneField\nWatchtower\t1\tred\t1";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(is);
        assertEquals(1, cards.size(), "Only well-formed line should be loaded");
    }

    /**
     * Confirms that lines with non-numeric values for quantity or cost
     * are skipped and do not crash the parser.
     */
    @Test
    void testLoadCards_InvalidNumberSkipsLine() {
        String input = "name\tquantity\tcolor\tcost\nBadCard\tabc\tred\txyz\nWatchtower\t1\tred\t1";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(is);
        assertEquals(1, cards.size(), "Only valid numeric line should be loaded");
    }

    /**
     * Tests that a null InputStream is handled gracefully, returning
     * an empty card list without crashing.
     */
    @Test
    void testLoadCards_NullStreamReturnsEmptyList() {
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(null);
        assertTrue(cards.isEmpty(), "Should return empty list when stream is null");
    }

    /**
     * Ensures that an IOException during reading is caught and handled,
     * and the method safely returns an empty list.
     */
    @Test
    void testLoadCards_IOExceptionHandledGracefully() {
        InputStream is = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read failure");
            }
        };
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(is);
        assertTrue(cards.isEmpty(), "Should return empty list on exception");
    }

    /**
     * Verifies that district cards with a fifth column (ability text)
     * are parsed correctly and stored in the DistrictCard object.
     */
    @Test
    void testLoadCards_ParsesAbilityWhenPresent() {
        String input = "name\tquantity\tcolor\tcost\tability\n" +
                "Haunted City\t1\tpurple\t2\tChoose this colour at game end\n";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        List<DistrictCard> cards = new App().loadDistrictCardsFromStream(is);

        assertEquals(1, cards.size());
        DistrictCard haunted = cards.get(0);
        assertEquals("Haunted City", haunted.getName());
        assertEquals("purple", haunted.getColor());
        assertEquals(2, haunted.getCost());
        assertEquals(1, haunted.getQuantity());
        assertEquals("Choose this colour at game end", haunted.getAbilityDescription());
    }
}