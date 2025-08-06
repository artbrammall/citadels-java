package citadels.model;

import citadels.core.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test scaffold for the CharacterCard class.
 * CharacterCard represents a character with name, turn order, and ability description.
 */
public class CharacterCardTest {

    private CharacterCard assassin;
    private CharacterCard thief;
    private CharacterCard duplicateAssassin;

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
     * Sets up sample CharacterCard instances before each test.
     * Ensures commonly used character cards like Assassin and Thief are initialized,
     * and validates that duplicate cards maintain consistent turn order.
     */
    @BeforeEach
    void setUp() {
        assassin = new CharacterCard("Assassin", 1, "Kill another character.");
        thief = new CharacterCard("Thief", 2, "Steal gold from another character.");
        duplicateAssassin = new CharacterCard("Assassin", 1, "Kill another character.");

        // Ensure turnOrder matches if constructor sets it to something else later
        assertEquals(assassin.getTurnOrder(), duplicateAssassin.getTurnOrder());
    }

    /**
     * Tests that the constructor correctly sets all fields: name, number, ability, and turnOrder.
     */
    @Test
    void testConstructor_SetsAllFieldsCorrectly() {
        assertEquals("Assassin", assassin.getName());
        assertEquals(1, assassin.getTurnOrder());
        assertEquals("Kill another character.", assassin.getAbilityDescription());
        assertEquals(1, assassin.getTurnOrder(), "Turn order should be initialized to character number");
    }

    /**
     * Tests that getName() returns the correct character name.
     */
    @Test
    void testGetName_ReturnsCorrectValue() {
        assertEquals("Assassin", assassin.getName());
    }

    /**
     * Tests that getTurnOrder() returns the current turn order (same as number unless modified).
     */
    @Test
    void testGetTurnOrder_ReturnsCorrectValue() {
        assertEquals(1, assassin.getTurnOrder());
    }

    /**
     * Tests that getAbilityDescription() returns the correct ability text.
     */
    @Test
    void testGetAbilityDescription_ReturnsCorrectValue() {
        assertEquals("Kill another character.", assassin.getAbilityDescription());
    }

    /**
     * Tests that equals() returns true when comparing the object to itself.
     */
    @Test
    void testEquals_SameObject_ReturnsTrue() {
        assertEquals(assassin, assassin);
    }

    /**
     * Tests that equals() returns false when comparing to an object of a different class.
     */
    @Test
    void testEquals_DifferentClass_ReturnsFalse() {
        assertNotEquals("NotACharacterCard", assassin);
    }

    /**
     * Tests that equals() returns false when the names of the CharacterCards are different.
     */
    @Test
    void testEquals_DifferentName_ReturnsFalse() {
        CharacterCard differentName = new CharacterCard("Wizard", 1, "Kill another character.");
        assertNotEquals(assassin, differentName);
    }

    /**
     * Tests that equals() returns false when the numbers of the CharacterCards are different.
     */
    @Test
    void testEquals_DifferentNumber_ReturnsFalse() {
        CharacterCard differentNumber = new CharacterCard("Assassin", 2, "Kill another character.");
        assertNotEquals(assassin, differentNumber);
    }

    /**
     * Tests that equals() returns false when the ability descriptions of the CharacterCards are different.
     */
    @Test
    void testEquals_DifferentAbility_ReturnsFalse() {
        CharacterCard differentAbility = new CharacterCard("Assassin", 1, "Silently eliminate a character.");
        assertNotEquals(assassin, differentAbility);
    }

    /**
     * Tests that different CharacterCard objects (with different fields) have different hash codes.
     */
    @Test
    void testHashCode_DifferentObjects_HaveDifferentHashCodes() {
        assertNotEquals(assassin.hashCode(), thief.hashCode());
    }

    /**
     * Tests that toString() returns the correct formatted string used in the game.
     */
    @Test
    void testToString_ReturnsExpectedFormat() {
        assertEquals("1. Assassin – Kill another character.", assassin.toString());
    }

    /**
     * Tests that setTurnOrder() correctly updates the turn order.
     */
    @Test
    void testSetTurnOrder_UpdatesValueCorrectly() {
        assassin.setTurnOrder(5);
        assertEquals(5, assassin.getTurnOrder());
    }

    /**
     * Tests that toString() includes the ability description when it's not null or empty.
     */
    @Test
    void testToString_IncludesAbilityDescription() {
        CharacterCard testCard = new CharacterCard("Wizard", 8, "Reveals the future.");
        String result = testCard.toString();
        assertTrue(result.contains("Reveals the future."));
    }

    /**
     * Tests that toString() omits the ability description when it is null.
     */
    @Test
    void testToString_WithNullAbilityDescription_DoesNotIncludeAbility() {
        CharacterCard silent = new CharacterCard("Ghost", 4, null);
        String result = silent.toString();
        assertEquals("4. Ghost", result); // ability description not shown
    }

    /**
     * Tests that toString() omits the ability description when it is an empty string.
     */
    @Test
    void testToString_WithEmptyAbilityDescription_HitsIsEmptyFalseBranch() {
        CharacterCard blank = new CharacterCard("Husk", 6, "");
        String result = blank.toString();
        assertEquals("6. Husk", result); // Ability should not appear
    }
}