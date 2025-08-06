package citadels.model;

/**
 * Represents a character card in the game, which has a name, turn order,
 * and a special ability description.
 */
public class CharacterCard {
    private final String name;                  // Name of the character
    private final String abilityDescription;    // Describes the character's special ability
    private int turnOrder;                // Determines when this character takes their turn (1–8)

    /**
     * Creates a new CharacterCard with the specified name, turn order, and ability description.
     *
     * @param name               Name of the character (e.g. "King")
     * @param turnOrder          The order this character takes their turn (1–8)
     * @param abilityDescription Text describing the character’s special power
     */
    public CharacterCard(String name, int turnOrder, String abilityDescription) {
        this.name = name;
        this.turnOrder = turnOrder;
        this.abilityDescription = abilityDescription;
    }

    /**
     * @return The name of this character
     */
    public String getName() {
        return name;
    }

    /**
     * @return The turn order (1–8) for this character
     */
    public int getTurnOrder() {
        return turnOrder;
    }

    /**
     * Sets the turn order for this character.
     *
     * @param turnOrder the new turn order value
     */
    public void setTurnOrder(int turnOrder) {
        this.turnOrder = turnOrder;
    }

    /**
     * @return A string describing the character’s ability
     */
    public String getAbilityDescription() {
        return abilityDescription;
    }

    /**
     * @return A string representation of the character, showing turn order, name, and ability
     */
    @Override
    public String toString() {
        // Only include the ability description in the string if it's not null or empty
        String result = turnOrder + ". " + name;
        if (abilityDescription != null && !abilityDescription.isEmpty()) {
            result += " – " + abilityDescription;
        }
        return result;
    }
}