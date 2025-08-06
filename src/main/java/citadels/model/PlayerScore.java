package citadels.model;

import java.util.List;

/**
 * Holds final score details for a player at the end of the game.
 * Tracks base district score, bonuses for city diversity and completion,
 * as well as any other bonus points (e.g. from purple districts).
 */
public class PlayerScore {
    private final int baseScore;             // Total value of built districts (sum of cost)
    private final int bonusDiverseCity;      // +3 points if player has all 5 district colors
    private final int bonusCompleteCity;     // +4 or +2 depending on whether they were first to 8 districts
    private final int extraPoints;           // Points from special district effects (e.g. Imperial Treasury)
    private final boolean bellTowerActive;   // If Bell Tower is currently built

    /**
     * Creates a score object with all breakdown values.
     *
     * @param baseScore         total district cost score
     * @param bonusDiverseCity  3 points if player has at least one of each district color
     * @param bonusCompleteCity 4 if first to finish, 2 if completed after someone else, 0 if not complete
     * @param extraPoints       bonus points from purple district effects
     */
    public PlayerScore(int baseScore, int bonusDiverseCity, int bonusCompleteCity, int extraPoints, boolean bellTowerActive) {
        this.baseScore = baseScore;
        this.bonusDiverseCity = bonusDiverseCity;
        this.bonusCompleteCity = bonusCompleteCity;
        this.extraPoints = extraPoints;
        this.bellTowerActive = bellTowerActive;
    }

    /**
     * Calculates all score components for a player at end of game.
     *
     * @param player           The player whose score is being calculated.
     * @param cityDistricts    The list of districts the player has built.
     * @param isFirstToFinish  Whether the player was the first to complete their city.
     * @param finalRoundNumber Final round of the game (used for Haunted City).
     * @return PlayerScore object representing total points and breakdown.
     */
    public static PlayerScore calculateFor(Player player, List<DistrictCard> cityDistricts, boolean isFirstToFinish, int finalRoundNumber, boolean bellTowerActive) {
        int baseScore = 0;
        int bonusCityCompletion = 0;
        int bonusCityDiversity = 0;
        int extraPoints = 0;

        // Add up the base score from the cost of each built district
        for (DistrictCard d : cityDistricts) {
            baseScore += d.getCost();
            extraPoints += d.getBonusPoints(player, cityDistricts);
        }

        // Bonus for building at least one of every district color (unless Haunted City blocks it)
        if (player.hasAllDistrictTypes(cityDistricts, finalRoundNumber)) {
            bonusCityDiversity = 3;
        }

        // First player to complete their city gets 4 bonus points
        if (isFirstToFinish) {
            bonusCityCompletion = 4;
            // Any other player who finishes their city gets 2 bonus points
        } else if (cityDistricts.size() >= 8 || (cityDistricts.size() == 7 && bellTowerActive)) {
            bonusCityCompletion = 2;
        }

        return new PlayerScore(baseScore, bonusCityDiversity, bonusCityCompletion, extraPoints, bellTowerActive);
    }

    /**
     * @return Total cost of all districts built (base score).
     */
    public int getBaseScore() {
        return baseScore;
    }

    /**
     * @return +3 bonus if player has all 5 district colors (with Haunted City rules).
     */
    public int getBonusDiverseCity() {
        return bonusDiverseCity;
    }

    /**
     * @return +4 if player finished first, +2 otherwise if they reached 8 districts.
     */
    public int getBonusCompleteCity() {
        return bonusCompleteCity;
    }

    /**
     * @return Bonus from special purple card effects like Wishing Well, Museum, etc.
     */
    public int getExtraPoints() {
        return extraPoints;
    }

    /**
     * @return Sum of base score, diversity bonus, completion bonus, and extra points.
     */
    public int getTotalScore() {
        return baseScore + bonusDiverseCity + bonusCompleteCity + extraPoints;
    }

    /**
     * Returns whether Bell Tower was active at the end of the game.
     * This can affect scoring rules like city size requirements.
     *
     * @return true if Bell Tower was active, false otherwise
     */
    public boolean isBellTowerActive() {
        return bellTowerActive;
    }
}
