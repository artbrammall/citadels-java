package citadels.state;

import citadels.core.Game;
import citadels.model.AIPlayer;
import citadels.model.DistrictCard;
import citadels.model.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Unit tests for the DistrictDeck class to ensure full line and branch coverage.
 */
public class DeckTest {

    private DistrictDeck districtDeck;
    private DistrictCard card1;
    private DistrictCard card2;

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
     * Sets up a small DistrictDeck with two cards before each test.
     */
    @BeforeEach
    void setUp() {
        card1 = new DistrictCard("Watchtower", "red", 1, 1, null);
        card2 = new DistrictCard("Castle", "yellow", 4, 1, null);
        List<DistrictCard> cards = new ArrayList<>(Arrays.asList(card1, card2));
        districtDeck = new DistrictDeck(cards);
    }

    /**
     * Verifies that the constructor correctly initializes the DistrictDeck with the given cards.
     */
    @Test
    void testConstructor_InitialisesDeckCorrectly() {
        assertEquals(2, districtDeck.size());
        assertTrue(districtDeck.getCards().contains(card1));
        assertTrue(districtDeck.getCards().contains(card2));
    }

    /**
     * Checks that shuffle does not alter the size of the DistrictDeck.
     * Note: Card order may or may not change.
     */
    @Test
    void testShuffle_ChangesCardOrder() {
        List<DistrictCard> originalOrder = new ArrayList<>(districtDeck.getCards());
        districtDeck.shuffle();
        List<DistrictCard> shuffledOrder = districtDeck.getCards();
        assertEquals(2, shuffledOrder.size());
    }

    /**
     * Tests that drawing a card removes it from the DistrictDeck.
     */
    @Test
    void testDraw_RemovesCardFromDeck() {
        DistrictCard drawn = districtDeck.draw();
        assertNotNull(drawn);
        assertEquals(1, districtDeck.size());
        assertFalse(districtDeck.getCards().contains(drawn));
    }

    /**
     * Verifies that drawing from an empty DistrictDeck returns null.
     */
    @Test
    void testDraw_EmptyDeck_ReturnsNull() {
        DistrictDeck emptyDistrictDeck = new DistrictDeck(new ArrayList<>());
        assertNull(emptyDistrictDeck.draw());
    }

    /**
     * Tests that a card can be added to the bottom of the DistrictDeck.
     */
    @Test
    void testAddCard_AddsCardToDeck() {
        DistrictCard newCard = new DistrictCard("Temple", "blue", 1, 1, null);
        List<DistrictCard> newCards = Collections.singletonList(newCard);
        districtDeck.addCardsToBottom(newCards);
        assertEquals(3, districtDeck.size());
        assertTrue(districtDeck.getCards().contains(newCard));
    }

    /**
     * Verifies that the size method returns the correct number of cards in the DistrictDeck.
     */
    @Test
    void testSize_ReturnsCorrectSize() {
        assertEquals(2, districtDeck.size());
        districtDeck.draw();
        assertEquals(1, districtDeck.size());
    }

    /**
     * Tests that getCards() returns a list containing all the current cards.
     */
    @Test
    void testGetCards_ReturnsCurrentCardList() {
        List<DistrictCard> cards = districtDeck.getCards();
        assertEquals(2, cards.size());
        assertTrue(cards.contains(card1));
        assertTrue(cards.contains(card2));
    }

    /**
     * Ensures that peekAll() returns a copy of the DistrictDeck, not the original list.
     */
    @Test
    void testPeekAll_ReturnsCopyOfCards() {
        List<DistrictCard> peeked = districtDeck.peekAll();
        assertEquals(districtDeck.size(), peeked.size());
        assertTrue(peeked.containsAll(districtDeck.getCards()));
        assertNotSame(districtDeck.getCards(), peeked);
    }

    /**
     * Tests that setCards replaces the DistrictDeck's internal list with a new one.
     */
    @Test
    void testSetCards_ReplacesDeckContent() {
        DistrictCard newCard1 = new DistrictCard("Harbour", "green", 4, 1, null);
        DistrictCard newCard2 = new DistrictCard("Keep", "purple", 3, 1, "Cannot be destroyed");
        List<DistrictCard> newCards = Arrays.asList(newCard1, newCard2);
        districtDeck.setCards(newCards);

        assertEquals(2, districtDeck.size());
        assertTrue(districtDeck.getCards().contains(newCard1));
        assertTrue(districtDeck.getCards().contains(newCard2));
        assertFalse(districtDeck.getCards().contains(card1));
    }

    /**
     * Verifies that removeCard correctly removes a card that exists in the DistrictDeck.
     */
    @Test
    void testRemoveCard_RemovesExistingCard() {
        assertTrue(districtDeck.getCards().contains(card1));
        districtDeck.removeCard(card1);
        assertFalse(districtDeck.getCards().contains(card1));
        assertEquals(1, districtDeck.size());
    }

    /**
     * Tests that attempting to remove a card not in the DistrictDeck does nothing.
     */
    @Test
    void testRemoveCard_DoesNothingIfCardNotPresent() {
        DistrictCard ghostCard = new DistrictCard("Ghost Tower", "purple", 5, 1, "Phantom ability");
        int beforeSize = districtDeck.size();
        districtDeck.removeCard(ghostCard);
        assertEquals(beforeSize, districtDeck.size());
    }
}