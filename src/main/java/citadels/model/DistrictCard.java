package citadels.model;

import java.util.List;

/**
 * Represents a district card in the game.
 * Districts have a name, color, cost, quantity (in the deck), and a description of any special ability.
 */
public class DistrictCard {
    private final String name;                   // District name
    private final String color;                  // District color (e.g., red, green, purple)
    private final int cost;                      // Gold cost to build this district
    private final int quantity;                  // Number of copies in the deck
    private final String abilityDescription;     // Optional ability text
    private int builtRound = -1;                 // The round this district was built in (used for effects like Graveyard)

    /**
     * Creates a new district card with all properties.
     *
     * @param name               Name of the district
     * @param color              Color of the district
     * @param cost               Build cost
     * @param quantity           Number of copies in the deck
     * @param abilityDescription Description of special ability (can be empty)
     */
    public DistrictCard(String name, String color, int cost, int quantity, String abilityDescription) {
        this.name = name;
        this.color = color;
        this.cost = cost;
        this.quantity = quantity;
        this.abilityDescription = abilityDescription;
    }

    /**
     * @return The name of the district
     */
    public String getName() {
        return name;
    }

    /**
     * @return The color of the district
     */
    public String getColor() {
        return color;
    }

    /**
     * @return The cost to build this district
     */
    public int getCost() {
        return cost;
    }

    /**
     * @return Number of times this card appears in the deck
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * @return Description of the district's ability (can be empty)
     */
    public String getAbilityDescription() {
        return abilityDescription;
    }

    /**
     * @return The round number this district was built, or -1 if not built
     */
    public int getBuiltRound() {
        return builtRound;
    }

    /**
     * Sets the round this district was built.
     * Useful for timing-based effects like Graveyard or cost modifiers.
     *
     * @param round The round number it was built in
     */
    public void setBuiltRound(int round) {
        this.builtRound = round;
    }

    /**
     * Calculates any bonus points this district provides at end of game.
     *
     * @param owner          The player who owns this district
     * @param builtDistricts The full list of districts the player has built
     * @return The number of bonus points provided by this district
     */
    public int getBonusPoints(Player owner, List<DistrictCard> builtDistricts) {
        String lowerName = name.toLowerCase();

        // Use the district's lowercase name to decide how its ability works
        switch (lowerName) {
            case "dragon gate":
            case "university":
                return 2; // Both are worth 8 points instead of 6

            case "wishing well":
                int count = 0;
                // Count how many of the same card the player has already built
                for (DistrictCard d : builtDistricts) {
                    // Wishing Well gives 1 point per other purple district
                    if (d != this && d.getColor().equalsIgnoreCase("purple")) {
                        count++;
                    }
                }
                return count;

            case "imperial treasury":
                return owner.getGold(); // 1 point per gold coin the player has

            case "map room":
                return owner.getHandSize(); // 1 point per card in hand

            case "museum":
                return owner.getMuseumStoredCards().size(); // 1 point per stored card
        }

        return 0; // Most districts give no bonus
    }

    /**
     * Returns a string summary of the district for debug/logging/display purposes.
     *
     * @return Formatted string with name, color, cost, and ability if present
     */
    @Override
    public String toString() {
        String description = name + " [" + color + cost + "]";
        boolean hasAbility = abilityDescription != null && !abilityDescription.isEmpty();
        // Only show ability text if there actually is one
        if (hasAbility) {
            description += " (Ability: " + abilityDescription + ")";
        }
        return description;
    }
}
