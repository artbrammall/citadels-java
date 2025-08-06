package citadels.model;

import citadels.core.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Scaffold for unit tests of the DistrictCard class.
 * DistrictCard represents a city building with a name, color, cost, and optional ability.
 */
public class DistrictCardTest {

    private DistrictCard red;
    private DistrictCard blue;
    private DistrictCard green;

    private DistrictCard dragonGate;
    private DistrictCard university;
    private DistrictCard wishingWell;
    private DistrictCard imperialTreasury;
    private DistrictCard mapRoom;
    private DistrictCard museum;

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
     * Initializes a variety of DistrictCard instances for testing.
     * Includes one card of each main color (red, blue, green) and several purple cards
     * with unique abilities to support tests involving bonus point logic and color-based effects.
     */
    @BeforeEach
    void setUp() {
        red = new DistrictCard("Watchtower", "red", 1, 1, "");
        blue = new DistrictCard("Temple", "blue", 1, 1, "");
        green = new DistrictCard("Market", "green", 2, 1, "");

        dragonGate = new DistrictCard("Dragon Gate", "purple", 6, 1, "Counts as a 6-point district.");
        university = new DistrictCard("University", "purple", 6, 1, "Counts as a 6-point district.");
        wishingWell = new DistrictCard("Wishing Well", "purple", 3, 1, "Grants 1 point if your city has another district with the same name.");
        imperialTreasury = new DistrictCard("Imperial Treasury", "purple", 5, 1, "Grants 1 point for every gold you have at game end.");
        mapRoom = new DistrictCard("Map Room", "purple", 3, 1, "Grants 1 point if any other player has a district with the same name.");
        museum = new DistrictCard("Museum", "purple", 3, 1, "Grants 1 point per card stored under it.");
    }

    /**
     * Tests that the constructor sets all fields correctly.
     */
    @Test
    void testConstructorSetsFields() {
        DistrictCard card = new DistrictCard("Temple", "blue", 1, 2, "Religious district");
        assertEquals("Temple", card.getName());
        assertEquals("blue", card.getColor());
        assertEquals(1, card.getCost());
        assertEquals(2, card.getQuantity());
        assertEquals("Religious district", card.getAbilityDescription());
        assertEquals(-1, card.getBuiltRound()); // Default
    }

    /**
     * Tests that getName returns the correct name.
     */
    @Test
    void testGetName() {
        assertEquals("Watchtower", red.getName());
    }

    /**
     * Tests that getColor returns the correct color.
     */
    @Test
    void testGetColor() {
        assertEquals("red", red.getColor());
    }

    /**
     * Tests that getCost returns the correct cost.
     */
    @Test
    void testGetCost() {
        assertEquals(1, red.getCost());
    }

    /**
     * Tests that getQuantity returns the correct quantity.
     */
    @Test
    void testGetQuantity() {
        assertEquals(1, red.getQuantity());
    }

    /**
     * Tests that getAbilityDescription returns the correct description.
     */
    @Test
    void testGetAbilityDescription() {
        assertEquals("", red.getAbilityDescription());
    }

    /**
     * Tests that builtRound defaults to -1 before being set.
     */
    @Test
    void testGetBuiltRoundDefaultsToNegativeOne() {
        assertEquals(-1, red.getBuiltRound());
    }

    /**
     * Tests setting and getting the built round value.
     */
    @Test
    void testSetAndGetBuiltRound() {
        red.setBuiltRound(3);
        assertEquals(3, red.getBuiltRound());
    }

    /**
     * Tests that the toString method returns a non-null and non-empty string.
     */
    @Test
    void testToStringNotNullOrEmpty() {
        String output = red.toString();

        assertNotNull(output, "toString() should not return null");
        assertFalse(output.trim().isEmpty(), "toString() should not return an empty string");
    }

    /**
     * Tests that Dragon Gate returns 2 bonus points as intended (worth 8 instead of 6).
     */
    @Test
    void testGetBonusPoints_DragonGateReturnsTwo() {
        assertEquals(2, dragonGate.getBonusPoints(new HumanPlayer("Tester"), Collections.emptyList()));
    }

    /**
     * Tests that University returns 2 bonus points as intended (worth 8 instead of 6).
     */
    @Test
    void testGetBonusPoints_UniversityReturnsTwo() {
        assertEquals(2, university.getBonusPoints(new HumanPlayer("Tester"), Collections.emptyList()));
    }

    /**
     * Tests that Wishing Well gives 0 bonus when there are no other purple cards in the city.
     */
    @Test
    void testGetBonusPoints_WishingWellWithNoOtherPurples() {
        List<DistrictCard> city = Arrays.asList(wishingWell, red, blue);
        assertEquals(0, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that Wishing Well gives 1 bonus point for one other purple card in the city.
     */
    @Test
    void testGetBonusPoints_WishingWellWithOneOtherPurple() {
        List<DistrictCard> city = Arrays.asList(wishingWell, museum); // museum is purple
        assertEquals(1, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that Wishing Well returns 1 bonus point when there is one other purple district in the city.
     * Confirms that other qualifying purple districts are counted correctly.
     */
    @Test
    void testGetBonusPoints_WishingWellWithOtherPurple_ReturnsOne() {
        List<DistrictCard> city = Arrays.asList(wishingWell, dragonGate); // both purple
        assertEquals(1, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that Imperial Treasury grants 1 point per gold the player has.
     */
    @Test
    void testGetBonusPoints_ImperialTreasuryReflectsGold() {
        Player p = new HumanPlayer("Tester");
        p.setGold(5);
        List<DistrictCard> city = Collections.singletonList(imperialTreasury);
        assertEquals(5, imperialTreasury.getBonusPoints(p, city));
    }

    /**
     * Tests that Map Room grants 1 point per card in the player's hand.
     */
    @Test
    void testGetBonusPoints_MapRoomReflectsHandSize() {
        Player p = new HumanPlayer("Tester");
        p.addCardToHand(red);
        p.addCardToHand(blue);
        List<DistrictCard> city = Collections.singletonList(mapRoom);
        assertEquals(2, mapRoom.getBonusPoints(p, city));
    }

    /**
     * Tests that regular non-special cards like Watchtower return 0 bonus points.
     */
    @Test
    void testGetBonusPoints_NonSpecialDistrictReturnsZero() {
        Player p = new HumanPlayer("Tester");
        List<DistrictCard> city = Collections.singletonList(red);
        assertEquals(0, red.getBonusPoints(p, city));
    }

    /**
     * Tests that Museum grants 1 bonus point per stored card under it.
     */
    @Test
    void testGetBonusPoints_MuseumCountsStoredCards() {
        Player p = new HumanPlayer("Tester");
        p.getMuseumStoredCards().add(red);
        p.getMuseumStoredCards().add(green);
        List<DistrictCard> city = Collections.singletonList(museum);
        assertEquals(2, museum.getBonusPoints(p, city));
    }

    /**
     * Tests that Museum returns 0 bonus points when no cards are stored under it.
     */
    @Test
    void testGetBonusPoints_MuseumWithNoStoredCards_ReturnsZero() {
        Player player = new HumanPlayer("Tester");
        List<DistrictCard> city = Collections.singletonList(museum); // museum from @BeforeEach setup

        assertEquals(0, museum.getBonusPoints(player, city));
    }

    /**
     * Tests that toString includes the ability description if present.
     */
    @Test
    void testToStringIncludesAbilityDescription() {
        DistrictCard special = new DistrictCard("Library", "blue", 5, 1, "Draw extra cards");
        String output = special.toString();

        assertTrue(output.contains("Ability: Draw extra cards"), "Ability description should appear in toString()");
    }

    /**
     * Tests toString with a null ability description (should not crash).
     */
    @Test
    void testToStringHandlesNullAbilityDescription() {
        DistrictCard card = new DistrictCard("NullAbility", "blue", 3, 1, null);
        String result = card.toString();

        assertFalse(result.contains("Ability:"), "toString() should not include 'Ability' for null description");
    }

    /**
     * Tests that Wishing Well correctly identifies purple districts but ignores itself.
     */
    @Test
    void testGetBonusPoints_WishingWellWithSelfAndOtherPurple() {
        // Create a second Wishing Well
        DistrictCard secondWishingWell = new DistrictCard("Wishing Well", "purple", 3, 1,
                "Grants 1 point if your city has another district with the same name.");

        // Set up a city with both Wishing Wells and another purple district
        List<DistrictCard> city = Arrays.asList(wishingWell, secondWishingWell, dragonGate);

        // Should count dragonGate and secondWishingWell but not itself
        assertEquals(2, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that Wishing Well correctly handles case insensitivity in color comparison.
     */
    @Test
    void testGetBonusPoints_WishingWellWithDifferentCaseColors() {
        // Create a purple district with mixed case color
        DistrictCard mixedCasePurple = new DistrictCard("Special District", "PuRpLe", 3, 1, "");

        List<DistrictCard> city = Arrays.asList(wishingWell, mixedCasePurple);

        // Should count mixedCasePurple
        assertEquals(1, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that Map Room returns 0 when player has no cards in hand.
     */
    @Test
    void testGetBonusPoints_MapRoomWithEmptyHand() {
        Player p = new HumanPlayer("Tester");
        // Ensure hand is empty
        List<DistrictCard> city = Collections.singletonList(mapRoom);
        assertEquals(0, mapRoom.getBonusPoints(p, city));
    }

    /**
     * Tests that Imperial Treasury returns 0 when player has no gold.
     */
    @Test
    void testGetBonusPoints_ImperialTreasuryWithZeroGold() {
        Player p = new HumanPlayer("Tester");
        p.setGold(0);
        List<DistrictCard> city = Collections.singletonList(imperialTreasury);
        assertEquals(0, imperialTreasury.getBonusPoints(p, city));
    }

    /**
     * Tests toString with an empty ability description (should not include ability).
     */
    @Test
    void testToStringOmitsEmptyAbilityDescription() {
        DistrictCard card = new DistrictCard("NoAbility", "blue", 3, 1, "");
        String result = card.toString();
        assertFalse(result.contains("Ability:"), "toString() should omit empty ability descriptions");
    }

    @Test
    void testGetBonusPoints_WishingWellWithNonPurpleDistrict_ReturnsZero() {
        List<DistrictCard> city = Arrays.asList(wishingWell, red); // red = non-purple
        assertEquals(0, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that a regular non-special card like Watchtower returns 0 bonus points.
     */
    @Test
    void testGetBonusPoints_WishingWellWithEmptyBuiltDistrictsList() {
        List<DistrictCard> city = new ArrayList<>();
        // Note: WishingWell is NOT in the city
        assertEquals(0, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }

    /**
     * Tests that an unknown red card returns 0 bonus points using default switch fallback.
     */
    @Test
    void testGetBonusPoints_NonPurpleUnhandledName_ReturnsZero() {
        DistrictCard card = new DistrictCard("Unknown", "red", 2, 1, "");
        assertEquals(0, card.getBonusPoints(new HumanPlayer("Tester"), Collections.singletonList(card)));
    }

    /**
     * Tests that Wishing Well returns 0 bonus points when it is the only card in the city.
     * Verifies that a district does not count itself toward the bonus.
     */
    @Test
    void testGetBonusPoints_WishingWellAlone_ReturnsZero() {
        List<DistrictCard> city = Collections.singletonList(wishingWell);
        assertEquals(0, wishingWell.getBonusPoints(new HumanPlayer("Tester"), city));
    }
}
