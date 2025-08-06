package citadels.state;

import citadels.core.Game;
import citadels.model.AIPlayer;
import citadels.model.CharacterCard;
import citadels.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Scaffold for unit tests of the CharacterDeck class.
 * CharacterDeck manages the set of character cards used during selection.
 */
public class CharacterDeckTest {

    private CharacterDeck deck;
    private List<CharacterCard> sampleCards;

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
     * Initializes a full CharacterDeck containing all 8 standard Citadels characters
     * in correct turn order. This setup is run before each test to ensure a consistent
     * starting state for tests involving character selection and deck operations.
     */
    @BeforeEach
    void setUp() {
        // Create individual CharacterCard instances representing each standard Citadels character
        CharacterCard assassin = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard thief = new CharacterCard("Thief", 2, "Steal gold from another character.");
        CharacterCard magician = new CharacterCard("Magician", 3, "Exchange hand or redraw cards.");
        CharacterCard king = new CharacterCard("King", 4, "Gain gold for yellow districts and receive the crown.");
        CharacterCard bishop = new CharacterCard("Bishop", 5, "Gain gold for blue districts and protect city.");
        CharacterCard merchant = new CharacterCard("Merchant", 6, "Gain gold for green districts and earn 1 extra gold.");
        CharacterCard architect = new CharacterCard("Architect", 7, "Draw 2 extra cards and build up to 3 districts.");
        CharacterCard warlord = new CharacterCard("Warlord", 8, "Gain gold for red districts and destroy buildings.");

        // Store all characters in a list in correct turn order
        List<CharacterCard> characters = new ArrayList<>(Arrays.asList(
                assassin, thief, magician, king, bishop, merchant, architect, warlord
        ));

        // Initialise the CharacterDeck with all 8 characters
        deck = new CharacterDeck(characters);
    }

    /**
     * Tests that the CharacterDeck constructor shuffles the deck by checking if
     * the order of cards differs from the original after repeated attempts.
     * To reduce flakiness due to chance, the test runs 5 times and passes if
     * any one attempt results in a different order.
     */
    @Test
    void testConstructor_CopiesListAndShufflesDeck() {
        CharacterCard c1 = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard c2 = new CharacterCard("Thief", 2, "Steal gold from another character.");
        CharacterCard c3 = new CharacterCard("Magician", 3, "Exchange hand or redraw cards.");
        CharacterCard c4 = new CharacterCard("King", 4, "Gain gold for yellow districts and receive the crown.");

        List<CharacterCard> original = new ArrayList<CharacterCard>();
        original.add(c1);
        original.add(c2);
        original.add(c3);
        original.add(c4);

        boolean anyShuffled = false;

        for (int i = 0; i < 5; i++) {
            CharacterDeck shuffledDeck = new CharacterDeck(original);
            List<CharacterCard> shuffled = shuffledDeck.getCards();

            // Check same contents, different reference
            assertNotSame(original, shuffled);
            assertTrue(shuffled.containsAll(original));
            assertEquals(original.size(), shuffled.size());

            // Check if order changed
            boolean orderChanged = false;
            for (int j = 0; j < original.size(); j++) {
                if (!original.get(j).equals(shuffled.get(j))) {
                    orderChanged = true;
                    break;
                }
            }

            if (orderChanged) {
                anyShuffled = true;
                break;
            }
        }

        assertTrue(anyShuffled, "DistrictDeck should be shuffled in at least one of 5 attempts");
    }

    @Test
    void testShuffle_ChangesCardOrder() {
        CharacterCard c1 = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard c2 = new CharacterCard("Thief", 2, "Steal gold from another character.");
        CharacterCard c3 = new CharacterCard("Magician", 3, "Exchange hand or redraw cards.");
        CharacterCard c4 = new CharacterCard("King", 4, "Gain gold for yellow districts and receive the crown.");

        List<CharacterCard> originalOrder = new ArrayList<>(Arrays.asList(c1, c2, c3, c4));
        CharacterDeck characterDeck = new CharacterDeck(originalOrder);

        List<CharacterCard> beforeShuffle = new ArrayList<>(characterDeck.getCards());
        boolean orderChanged = false;

        // Try multiple shuffles to avoid rare false negatives
        for (int attempt = 0; attempt < 5; attempt++) {
            characterDeck.shuffle();
            List<CharacterCard> afterShuffle = characterDeck.getCards();

            // Check if order changed
            for (int i = 0; i < beforeShuffle.size(); i++) {
                if (!beforeShuffle.get(i).equals(afterShuffle.get(i))) {
                    orderChanged = true;
                    break;
                }
            }

            if (orderChanged) break;
        }

        assertTrue(orderChanged, "Shuffle should eventually change the order of cards (repeated up to 5 times)");
    }

    /**
     * Tests that draw() removes and returns the top card from the deck.
     * Ensures that the returned card is the first in the list and that
     * the deck size decreases by one.
     */
    @Test
    void testDraw_RemovesAndReturnsTopCard() {
        CharacterCard c1 = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard c2 = new CharacterCard("Thief", 2, "Steal gold from another character.");
        List<CharacterCard> cards = new ArrayList<>(Arrays.asList(c1, c2));

        CharacterDeck deck = new CharacterDeck(cards);

        // Use getCards().get(0) to determine expected first card (already shuffled in constructor)
        CharacterCard expected = deck.getCards().get(0);
        CharacterCard drawn = deck.draw();

        assertEquals(expected, drawn);
        assertEquals(1, deck.size());
        assertFalse(deck.getCards().contains(drawn)); // ensure it's removed
    }

    /**
     * Tests that draw() returns null when the deck is empty.
     * This covers the edge case where no cards remain to draw.
     */
    @Test
    void testDraw_ReturnsNullWhenDeckEmpty() {
        List<CharacterCard> emptyList = new ArrayList<>();
        CharacterDeck emptyDeck = new CharacterDeck(emptyList);

        assertNull(emptyDeck.draw());
        assertEquals(0, emptyDeck.size());
    }

    /**
     * Tests that setCards(List<CharacterCard>) replaces the current deck with a new copy
     * of the provided list. Ensures it does not use the same reference and updates the contents correctly.
     */
    @Test
    void testSetCards_ReplacesDeckWithCopy() {
        // Original deck
        CharacterCard originalCard = new CharacterCard("Assassin", 1, "Kill another character.");
        List<CharacterCard> initialList = new ArrayList<CharacterCard>();
        initialList.add(originalCard);
        CharacterDeck deck = new CharacterDeck(initialList);

        // New list of cards to replace with
        CharacterCard newCard1 = new CharacterCard("Thief", 2, "Steal gold from another character.");
        CharacterCard newCard2 = new CharacterCard("Magician", 3, "Exchange hand or redraw cards.");
        List<CharacterCard> newList = new ArrayList<CharacterCard>();
        newList.add(newCard1);
        newList.add(newCard2);

        deck.setCards(newList);

        // Verify deck contents match the new list
        assertEquals(2, deck.size());
        assertTrue(deck.getCards().containsAll(newList));

        // Ensure it is not the same reference
        assertNotSame(newList, deck.getCards());

        // Ensure old card is no longer present
        assertFalse(deck.getCards().contains(originalCard));
    }

    /**
     * Tests that size() returns the correct number of cards in the deck.
     * Verifies that the value reflects the current number of cards remaining.
     */
    @Test
    void testSize_ReturnsCorrectNumberOfCards() {
        CharacterCard c1 = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard c2 = new CharacterCard("Thief", 2, "Steal gold from another character.");
        CharacterCard c3 = new CharacterCard("Magician", 3, "Exchange hand or redraw cards.");

        List<CharacterCard> cards = new ArrayList<>(Arrays.asList(c1, c2, c3));
        CharacterDeck deck = new CharacterDeck(cards);

        // Initial size should be 3
        assertEquals(3, deck.size());

        // After drawing one card, size should decrease to 2
        deck.draw();
        assertEquals(2, deck.size());

        // After drawing two more cards, size should be 0
        deck.draw();
        deck.draw();
        assertEquals(0, deck.size());
    }

    /**
     * Tests that getCards() returns a reference to the internal list used by the deck.
     * Verifies that modifying the returned list directly affects the deck, confirming shared reference.
     */
    @Test
    void testGetCards_ReturnsInternalListReference() {
        CharacterCard c1 = new CharacterCard("Assassin", 1, "Kill another character.");
        CharacterCard c2 = new CharacterCard("Thief", 2, "Steal gold from another character.");

        List<CharacterCard> cards = new ArrayList<CharacterCard>();
        cards.add(c1);
        cards.add(c2);

        CharacterDeck deck = new CharacterDeck(cards);

        List<CharacterCard> internal = deck.getCards();
        internal.clear(); // Clear the list returned by getCards()

        // The deck should now be empty, since both share the same reference
        assertEquals(0, deck.size());
        assertTrue(deck.getCards().isEmpty());
    }
}
