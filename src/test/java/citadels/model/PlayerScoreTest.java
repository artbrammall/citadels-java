package citadels.model;

import citadels.core.Game;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit tests for the PlayerScore class.
 * PlayerScore calculates and stores final scoring details.
 */
public class PlayerScoreTest {
    private List<DistrictCard> city;
    private int baseScore;
    private int bonusDiverseCity;
    private int bonusCompleteCity;
    private int extraPoints;
    private boolean bellTowerActive;
    private PlayerScore score;

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
     * Creates a sample city with all five district colors and a few purple cards that give bonus points.
     * Also sets up the expected scoring fields and creates a PlayerScore instance to test against.
     */
    @BeforeEach
    void setUp() {
        // City with diverse district colors and extra-point purple district
        city = new ArrayList<>();
        city.add(new DistrictCard("Watchtower", "red", 1, 1, null)); // red
        city.add(new DistrictCard("Cathedral", "blue", 5, 1, null)); // blue
        city.add(new DistrictCard("Castle", "yellow", 4, 1, null)); // yellow
        city.add(new DistrictCard("Market", "green", 2, 1, null)); // green
        city.add(new DistrictCard("Graveyard", "purple", 5, 1, "When a district is destroyed, you may pay 1 gold to recover it.")); // purple
        city.add(new DistrictCard("Smithy", "purple", 5, 1, "Pay 2 gold to draw 3 cards."));
        city.add(new DistrictCard("Museum", "purple", 4, 1, "Place cards under this district for 1 point each."));
        city.add(new DistrictCard("Haunted City", "purple", 2, 1, "Counts as any color for diverse bonus, unless built last round."));

        // Set scoring fields
        baseScore = city.stream().mapToInt(DistrictCard::getCost).sum(); // 28
        bonusDiverseCity = 3; // all 5 colors including Haunted City
        bonusCompleteCity = 4; // assuming first to complete 8 districts
        extraPoints = 2; // from Museum or other purple effects
        bellTowerActive = true;

        score = new PlayerScore(baseScore, bonusDiverseCity, bonusCompleteCity, extraPoints, bellTowerActive);
    }

    /**
     * Tests that the PlayerScore constructor correctly sets all fields:
     * baseScore, bonusDiverseCity, bonusCompleteCity, and extraPoints.
     */
    @Test
    void testConstructor_SetsAllFieldsCorrectly() {
        PlayerScore score = new PlayerScore(20, 3, 4, 2, true);

        assertEquals(20, score.getBaseScore());
        assertEquals(3, score.getBonusDiverseCity());
        assertEquals(4, score.getBonusCompleteCity());
        assertEquals(2, score.getExtraPoints());
        assertTrue(score.isBellTowerActive());
    }

    /**
     * Tests that getBaseScore() returns the correct base score value
     * that was provided when constructing the PlayerScore object.
     */
    @Test
    void testGetBaseScore_ReturnsCorrectValue() {
        assertEquals(baseScore, score.getBaseScore());
    }

    /**
     * Tests that getBonusDiverseCity() returns the correct bonus value
     * for building a city with all district colors.
     */
    @Test
    void testGetBonusDiverseCity_ReturnsCorrectValue() {
        assertEquals(bonusDiverseCity, score.getBonusDiverseCity());
    }

    /**
     * Tests that getBonusCompleteCity() returns the correct bonus value
     * awarded for being the first to complete a full city.
     */
    @Test
    void testGetBonusCompleteCity_ReturnsCorrectValue() {
        assertEquals(bonusCompleteCity, score.getBonusCompleteCity());
    }

    /**
     * Tests that getExtraPoints() returns the correct value for additional
     * points gained from special districts like Museum or Haunted City.
     */
    @Test
    void testGetExtraPoints_ReturnsCorrectValue() {
        assertEquals(extraPoints, score.getExtraPoints());
    }

    /**
     * Tests that getTotalScore() correctly returns the sum of all components:
     * base score, diverse city bonus, complete city bonus, and extra points.
     */
    @Test
    void testGetTotalScore_ReturnsSumOfAllParts() {
        int expectedTotal = baseScore + bonusDiverseCity + bonusCompleteCity + extraPoints;
        assertEquals(expectedTotal, score.getTotalScore());
    }

    /**
     * Tests that getTotalScore() returns only the base score when all bonuses are zero.
     */
    @Test
    void testGetTotalScore_WithZeroBonuses_ReturnsBaseOnly() {
        PlayerScore zeroBonusScore = new PlayerScore(15, 0, 0, 0, false);
        assertEquals(15, zeroBonusScore.getTotalScore());
    }

    /**
     * Tests calculateFor() when the player is the first to complete their city
     * with all district types and some purple card bonuses.
     */
    @Test
    void testCalculateFor_FirstToFinish_WithFullDiversityAndBonuses() {
        Player player = new HumanPlayer("Tester");
        int finalRound = 5;

        // simulate 8 diverse districts (incl. Haunted City)
        PlayerScore result = PlayerScore.calculateFor(
                player, city, true, finalRound, false
        );

        assertEquals(28, result.getBaseScore());
        assertEquals(3, result.getBonusDiverseCity());
        assertEquals(4, result.getBonusCompleteCity());
        assertTrue(result.getExtraPoints() >= 0); // from Museum etc.
    }

    /**
     * Tests calculateFor() when the player completes the city with 8 districts,
     * but was not the first to finish.
     */
    @Test
    void testCalculateFor_NotFirstToFinish_WithFullCity() {
        Player player = new HumanPlayer("Tester");
        int finalRound = 5;

        PlayerScore result = PlayerScore.calculateFor(
                player, city, false, finalRound, false
        );

        assertEquals(2, result.getBonusCompleteCity()); // not first, but completed
    }

    /**
     * Tests calculateFor() when Bell Tower allows a 7-district city to count as complete.
     */
    @Test
    void testCalculateFor_BellTowerActivatedWithSevenDistricts() {
        Player player = new HumanPlayer("Tester");
        List<DistrictCard> cityOfSeven = new ArrayList<>(city.subList(0, 7)); // only 7 cards
        int finalRound = 5;

        PlayerScore result = PlayerScore.calculateFor(
                player, cityOfSeven, false, finalRound, true
        );

        assertEquals(2, result.getBonusCompleteCity()); // gets 2 bonus due to Bell Tower
    }

    /**
     * Tests calculateFor() when the city is not finished (less than 7 with no Bell Tower).
     */
    @Test
    void testCalculateFor_IncompleteCity_NoBonus() {
        Player player = new DummyPlayer("Tester");

        // Use ArrayList and add districts manually for Java 8 compatibility
        List<DistrictCard> smallCity = new ArrayList<>();
        smallCity.add(new DistrictCard("Watchtower", "red", 1, 1, null));
        smallCity.add(new DistrictCard("Prison", "red", 2, 1, null));
        smallCity.add(new DistrictCard("Cathedral", "blue", 5, 1, null));
        smallCity.add(new DistrictCard("Monastery", "blue", 3, 1, null));
        smallCity.add(new DistrictCard("Church", "blue", 2, 1, null));

        int finalRound = 5;

        PlayerScore result = PlayerScore.calculateFor(
                player, smallCity, false, finalRound, false
        );

        assertEquals(0, result.getBonusCompleteCity(), "Should not receive completion bonus with only 5 districts.");
        assertEquals(0, result.getBonusDiverseCity(), "Should not receive diversity bonus with only red and blue districts.");
    }

    /**
     * Tests that a player who has exactly 7 districts and Bell Tower is active
     * correctly receives the 2-point city completion bonus. This test ensures
     * that the condition (city size == 7 && bellTowerActive == true) is triggered
     * and that the bellTowerActive flag is recorded correctly in the PlayerScore.
     */
    @Test
    void testCalculateFor_BellTowerBranchActivated() {
        Player player = new DummyPlayer("Tester");

        List<DistrictCard> sevenDistricts = new ArrayList<>();
        sevenDistricts.add(new DistrictCard("A", "red", 1, 1, null));
        sevenDistricts.add(new DistrictCard("B", "blue", 1, 1, null));
        sevenDistricts.add(new DistrictCard("C", "yellow", 1, 1, null));
        sevenDistricts.add(new DistrictCard("D", "green", 1, 1, null));
        sevenDistricts.add(new DistrictCard("E", "purple", 1, 1, null));
        sevenDistricts.add(new DistrictCard("F", "red", 1, 1, null));
        sevenDistricts.add(new DistrictCard("G", "blue", 1, 1, null));

        // Must be false — otherwise the 4-point branch is taken
        boolean isFirstToFinish = false;
        boolean bellTowerActive = true;

        PlayerScore result = PlayerScore.calculateFor(
                player,
                sevenDistricts,
                isFirstToFinish,
                5,
                bellTowerActive
        );

        assertEquals(2, result.getBonusCompleteCity(), "Should get 2-point bonus from Bell Tower with 7 districts.");
        assertTrue(result.isBellTowerActive(), "Bell Tower flag should be true in PlayerScore");
    }

    /**
     * Minimal dummy player used for testing PlayerScore.calculateFor().
     * Overrides only the methods needed for score calculation and diverse city logic.
     */
    private static class DummyPlayer extends Player {
        public DummyPlayer(String name) {
            super(name);
        }

        @Override
        public CharacterCard chooseCharacter(List<CharacterCard> available) {
            return null; // not needed
        }

        @Override
        public void buildDistrict() {

        }

        @Override
        public boolean hasAllDistrictTypes(List<DistrictCard> cityDistricts, int finalRoundNumber) {
            // Simple logic: if all 5 colors are present, return true
            boolean hasRed = false, hasBlue = false, hasYellow = false, hasGreen = false, hasPurple = false;
            for (DistrictCard d : cityDistricts) {
                String color = d.getColor().toLowerCase();
                switch (color) {
                    case "red":
                        hasRed = true;
                        break;
                    case "blue":
                        hasBlue = true;
                        break;
                    case "yellow":
                        hasYellow = true;
                        break;
                    case "green":
                        hasGreen = true;
                        break;
                    case "purple":
                        hasPurple = true;
                        break;
                }
            }
            return hasRed && hasBlue && hasYellow && hasGreen && hasPurple;
        }

        @Override
        public void destroyDistrict(Player target, int districtIndex, boolean usedArmory) {

        }
    }
}
