package citadels.model;

import citadels.app.App;
import citadels.core.Game;
import citadels.state.Board;
import citadels.state.DistrictDeck;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive PlayerTest setup (Java 8 compatible).
 * Sets up a HumanPlayer, fake Game, character cards, and test district cards.
 */
public class PlayerTest {

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
     * Tests that hasCompletedTurn() correctly reflects the player's turn status.
     * It should be false by default, then true after ending their turn.
     */
    @Test
    void testHasCompletedTurn_UpdatesCorrectly() {
        // Get the human player (Player 1) from the loaded game
        player = game.getPlayers().get(0);

        // By default, the player shouldn't have finished their turn yet
        assertFalse(player.hasCompletedTurn(), "Player should not have completed their turn at start");

        // End their turn
        player.setTurnCompleted(true);

        // Now it should be true
        assertTrue(player.hasCompletedTurn(), "Player should have completed their turn after setting flag");
    }

    /**
     * Tests that isKilled() correctly reflects whether the player was assassinated.
     * Uses Player 2 from the save file, who is marked as killed.
     */
    @Test
    void testIsKilled_ReturnsCorrectStatus() {
        // Player 2 is killed in the loaded test save
        Player killedPlayer = game.getPlayers().get(1);
        assertTrue(killedPlayer.isKilled(), "Player 2 should be marked as killed in the save file");

        // Player 1 is alive
        Player alivePlayer = game.getPlayers().get(0);
        assertFalse(alivePlayer.isKilled(), "Player 1 should not be killed");
    }

    /**
     * Tests that isStolen() correctly reflects whether the player was robbed.
     * Player 3 was stolen from this round, while Player 1 was not.
     */
    @Test
    void testIsStolen_ReturnsCorrectStatus() {
        // Player 3: isStolen = true
        Player robbedPlayer = game.getPlayers().get(2);
        assertTrue(robbedPlayer.isStolen(), "Player 3 should be marked as stolen from in the save");

        // Player 1: isStolen = false
        Player cleanPlayer = game.getPlayers().get(0);
        assertFalse(cleanPlayer.isStolen(), "Player 1 should not be marked as stolen from");
    }

    /**
     * Tests that getRobbedBy() returns the correct player who robbed this player.
     * Player 3 was robbed by Player 7, while Player 1 was not robbed at all.
     */
    @Test
    void testGetRobbedBy_ReturnsCorrectPlayer() {
        // Player 3: robbedBy = "Player 7"
        Player victim = game.getPlayers().get(2);
        Player expectedThief = game.getPlayers().get(6); // Player 7

        assertEquals(expectedThief, victim.getRobbedBy(), "Player 3 should have been robbed by Player 7");

        // Player 1: robbedBy = null
        Player cleanPlayer = game.getPlayers().get(0);
        assertNull(cleanPlayer.getRobbedBy(), "Player 1 should not have been robbed");
    }

    /**
     * Tests that getGold() returns the correct current gold for each player.
     * Uses gold values directly from the loaded save file.
     */
    @Test
    void testGetGold_ReturnsCorrectAmount() {
        // Player 1 has 10 gold
        Player p1 = game.getPlayers().get(0);
        assertEquals(10, p1.getGold(), "Player 1 should have 10 gold");

        // Player 2 has 1 gold
        Player p2 = game.getPlayers().get(1);
        assertEquals(1, p2.getGold(), "Player 2 should have 1 gold");

        // Player 3 has 0 gold
        Player p3 = game.getPlayers().get(2);
        assertEquals(0, p3.getGold(), "Player 3 should have 0 gold");

        // Player 6 has 3 gold
        Player p6 = game.getPlayers().get(5);
        assertEquals(3, p6.getGold(), "Player 6 should have 3 gold");
    }

    /**
     * Tests that addCardToHand() correctly adds a card to the player's hand.
     * Makes sure the hand increases in size and contains the added card.
     */
    @Test
    void testAddCardToHand_AddsCardCorrectly() {
        Player player = game.getPlayers().get(0); // Player 1
        int originalSize = player.getHand().size();

        DistrictCard newCard = new DistrictCard("Test District", "blue", 3, 1, null);

        player.addCardToHand(newCard);

        // Hand size should increase by 1
        assertEquals(originalSize + 1, player.getHand().size(), "Player's hand size should increase after adding a card");

        // The added card should be present in hand
        assertTrue(player.getHand().contains(newCard), "Player's hand should contain the newly added card");
    }

    /**
     * Tests that getHandSize() returns the correct number of cards in hand.
     * Uses hand sizes directly from the loaded save file.
     */
    @Test
    void testGetHandSize_ReturnsCorrectCount() {
        // Player 1: hand has 7 cards
        Player player1 = game.getPlayers().get(0);
        assertEquals(7, player1.getHandSize(), "Player 1 should have 7 cards in hand");

        // Player 2: hand has 3 cards
        Player player2 = game.getPlayers().get(1);
        assertEquals(3, player2.getHandSize(), "Player 2 should have 3 cards in hand");

        // Player 7: hand has 0 cards
        Player player7 = game.getPlayers().get(6);
        assertEquals(0, player7.getHandSize(), "Player 7 should have 0 cards in hand");
    }

    /**
     * Tests that getBuildsThisTurn() returns the correct number of districts built
     * during the current turn, as defined in the save file.
     */
    @Test
    void testGetBuildsThisTurn_ReturnsCorrectCount() {
        // Player 1: built 0 districts this turn
        Player player1 = game.getPlayers().get(0);
        assertEquals(0, player1.getBuildsThisTurn(), "Player 1 should have built 0 districts this turn");

        // Player 2: built 1 district this turn
        Player player2 = game.getPlayers().get(1);
        assertEquals(1, player2.getBuildsThisTurn(), "Player 2 should have built 1 district this turn");

        // Player 5: built 2 districts this turn
        Player player5 = game.getPlayers().get(4);
        assertEquals(2, player5.getBuildsThisTurn(), "Player 5 should have built 2 districts this turn");
    }

    /**
     * Tests that getMaxBuildsPerTurn() returns the correct limit on how many
     * districts a player is allowed to build in a single turn.
     */
    @Test
    void testGetMaxBuildsPerTurn_ReturnsCorrectValue() {
        // Player 1: maxBuildsPerTurn = 1
        Player player1 = game.getPlayers().get(0);
        assertEquals(1, player1.getMaxBuildsPerTurn(), "Player 1 should have a build limit of 1");

        // Player 2: maxBuildsPerTurn = 1
        Player player2 = game.getPlayers().get(1);
        assertEquals(1, player2.getMaxBuildsPerTurn(), "Player 2 should have a build limit of 1");

        // Player 5: maxBuildsPerTurn = 3 (Architect)
        Player player5 = game.getPlayers().get(4);
        assertEquals(3, player5.getMaxBuildsPerTurn(), "Player 5 should have a build limit of 3");
    }

    /**
     * Tests that hasLibrary() correctly detects if the player has built the Library.
     * Player 1 has not built Library. Player 6 has it in hand but not built either.
     */
    @Test
    void testHasLibrary_ReturnsCorrectly() {
        // Player 1 has NOT built Library
        Player player1 = game.getPlayers().get(0);
        assertFalse(player1.hasLibrary(), "Player 1 should not have Library built");

        // Player 6 has Library in hand, but not built
        Player player6 = game.getPlayers().get(5);
        assertFalse(player6.hasLibrary(), "Player 6 has Library in hand but hasn't built it, so should return false");
    }

    /**
     * Tests that each hasBuiltDistrict() wrapper method correctly detects
     * whether the player has built the relevant special district.
     * Includes all covered districts from the loaded save file.
     */
    @Test
    void testHasBuiltDistrictHelpers_ReturnCorrectly() {
        Player p1 = game.getPlayers().get(0); // Player 1
        Player p2 = game.getPlayers().get(1); // Player 2
        Player p3 = game.getPlayers().get(2); // Player 3
        Player p4 = game.getPlayers().get(3); // Player 4
        Player p5 = game.getPlayers().get(4); // Player 5
        Player p6 = game.getPlayers().get(5); // Player 6

        // Player 1
        assertTrue(p1.hasMuseum(), "Player 1 should have built Museum");
        assertTrue(p1.hasQuarry(), "Player 1 should have built Quarry");
        assertFalse(p1.hasObservatory(), "Observatory is in Player 1's hand, not built");
        assertFalse(p1.hasLaboratory(), "Laboratory is in hand only");
        assertFalse(p1.hasArmory(), "Armory is in hand only");
        assertFalse(p1.hasLibrary(), "Player 1 should not have Library built");

        // Player 2
        assertTrue(p2.hasGreatWall(), "Player 2 should have built Great Wall");

        // Player 3
        assertTrue(p3.hasGraveyard(), "Player 3 should have built Graveyard");

        // Player 4
        assertTrue(p4.hasPoorHouse(), "Player 4 should have built Poor House");
        assertFalse(p4.hasThroneRoom(), "Player 4 has Throne Room in hand but not built");

        // Player 5
        assertTrue(p5.hasSmithy(), "Player 5 should have built Smithy");

        System.out.println("Built districts for Player 6: " +
                game.getCityForPlayer(p5).stream().map(DistrictCard::getName).collect(Collectors.toList()));
        // Player 6
        assertFalse(p6.hasLibrary(), "Player 6 has Library in hand but hasn't built it");
        assertTrue(p6.hasSchoolOfMagic(), "Player 6 has School of Magic in hand only — NOT built");
        assertTrue(p6.hasHospital(), "Player 6 has Hospital in hand only — NOT built");

        // Player 3 — Park is in hand only
        assertFalse(p3.hasPark(), "Player 3 has Park in hand but not built");

        // No one built Factory in this save
        assertFalse(p1.hasFactory(), "No one built Factory");
    }

    /**
     * Tests whether hasUsedSmithy(), hasUsedLaboratory(), and hasUsedMuseum() correctly reflect usage state.
     * Only Player 7 has used Museum in this save. Everyone else has all usage flags set to false.
     */
    @Test
    void testUsedDistrictAbilitiesFlags_ReturnCorrectly() {
        // Player 1 has built Museum but not used it
        Player p1 = game.getPlayers().get(0);
        assertFalse(p1.hasUsedSmithy(), "Player 1 has not used Smithy this turn");
        assertFalse(p1.hasUsedLaboratory(), "Player 1 has not used Laboratory this turn");
        assertFalse(p1.hasUsedMuseum(), "Player 1 has not used Museum this turn");

        // Player 5 has built Smithy but hasn't used it
        Player p5 = game.getPlayers().get(4);
        assertFalse(p5.hasUsedSmithy(), "Player 5 has not used Smithy this turn");

        // Player 6 has Laboratory and Museum in hand or built, but hasn't used either
        Player p6 = game.getPlayers().get(5);
        assertFalse(p6.hasUsedLaboratory(), "Player 6 has not used Laboratory this turn");
        assertFalse(p6.hasUsedMuseum(), "Player 6 has not used Museum this turn");

        // Player 7 is the only one who has used Museum
        Player p7 = game.getPlayers().get(6);
        assertTrue(p7.hasUsedMuseum(), "Player 7 should have used Museum this turn");
    }

    /**
     * Tests that getChosenCharacter() correctly returns the character card chosen by the player.
     * Verifies the name and turn order match the save file's assignments.
     */
    @Test
    void testGetChosenCharacter_ReturnsCorrectCharacter() {
        // Player 1 → Magician (turn order 3)
        Player p1 = game.getPlayers().get(0);
        CharacterCard c1 = p1.getChosenCharacter();
        assertNotNull(c1, "Player 1 should have a character assigned");
        assertEquals("Magician", c1.getName(), "Player 1 should have chosen Magician");
        assertEquals(3, c1.getTurnOrder(), "Magician should have turn order 3");

        // Player 2 → Warlord (turn order 8)
        Player p2 = game.getPlayers().get(1);
        CharacterCard c2 = p2.getChosenCharacter();
        assertNotNull(c2, "Player 2 should have a character assigned");
        assertEquals("Warlord", c2.getName(), "Player 2 should have chosen Warlord");
        assertEquals(8, c2.getTurnOrder(), "Warlord should have turn order 8");

        // Player 6 → King (turn order 4)
        Player p6 = game.getPlayers().get(5);
        CharacterCard c6 = p6.getChosenCharacter();
        assertNotNull(c6, "Player 6 should have a character assigned");
        assertEquals("King", c6.getName(), "Player 6 should have chosen King");
        assertEquals(4, c6.getTurnOrder(), "King should have turn order 4");
    }

    /**
     * Tests that takeTurn() resets flags and prints character.
     * Includes debug lines to trace execution and input exhaustion.
     */
    @Test
    void testTakeTurn_ResetsFlagsAndPrintsCharacter() {
        System.out.println("[TEST] Starting testTakeTurn_ResetsFlagsAndPrintsCharacter");

        Player p1 = game.getCurrentPlayer();
        System.out.println("[DEBUG] Current player: " + p1.getName() + ", character: " + p1.getChosenCharacter());

        p1.setUsedLaboratory(true);
        p1.setUsedSmithy(true);
        p1.setUsedMuseum(true);

        System.out.println("[DEBUG] Flags before turn:");
        System.out.println("  usedLaboratory: " + p1.hasUsedLaboratory());
        System.out.println("  usedSmithy: " + p1.hasUsedSmithy());
        System.out.println("  usedMuseum: " + p1.hasUsedMuseum());

        String input = String.join("\n", Arrays.asList(
                "gold",                // chooseIncome
                "end"                  // end turn
        ));

        InputStream originalInput = App.input;
        App.input = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        System.out.println("[DEBUG] Mock input injected into App.input");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            System.out.println("[DEBUG] Calling takeTurn()");
            p1.takeTurn();
            System.out.println("[DEBUG] takeTurn() completed");
        } catch (Exception e) {
            System.setOut(originalOut); // Ensure output is restored before printing error
            App.input = originalInput;
            e.printStackTrace();
            fail("[ERROR] Exception occurred during takeTurn(): " + e.getMessage());
        } finally {
            System.setOut(originalOut);
            App.input = originalInput;
            System.out.println("[DEBUG] Streams restored");
        }

        System.out.println("[DEBUG] Flags after takeTurn:");
        System.out.println("  usedLaboratory: " + p1.hasUsedLaboratory());
        System.out.println("  usedSmithy: " + p1.hasUsedSmithy());
        System.out.println("  usedMuseum: " + p1.hasUsedMuseum());

        assertFalse(p1.hasUsedLaboratory(), "Expected Laboratory usage to be reset");
        assertFalse(p1.hasUsedSmithy(), "Expected Smithy usage to be reset");
        assertFalse(p1.hasUsedMuseum(), "Expected Museum usage to be reset");

        String output = outContent.toString().trim();
        System.out.println("[DEBUG] Output captured from System.out:");
        System.out.println(output);

        assertTrue(output.contains("Player 1 is the Magician"), "Expected character name in output");

        System.out.println("[TEST] testTakeTurn_ResetsFlagsAndPrintsCharacter completed successfully");
    }

    /**
     * Tests that if a player was killed but has Hospital, they can take an action (choose income) instead of losing their turn.
     */
    @Test
    void testAssassinatedPlayer_WithHospital_TakesLimitedTurn() {
        Player player = game.getPlayers().get(5); // Player 6, has Hospital
        assertTrue(player.hasHospital());
        player.setKilled(true); // Simulate assassination

        App.input = new ByteArrayInputStream("gold\n".getBytes(StandardCharsets.UTF_8)); // chooseIncome input

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            player.takeTurn();
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("was assassinated but has the Hospital"), "Hospital message should appear");
        assertTrue(output.contains("gold"), "Should process gold income choice");
        assertTrue(player.hasCompletedTurn(), "Turn should be marked completed");
    }

    /**
     * Tests that if a player was killed and does not have Hospital, they lose their turn entirely.
     */
    @Test
    void testAssassinatedPlayer_WithoutHospital_LosesTurn() {
        Player player = game.getPlayers().get(1); // Player 2, no Hospital
        assertFalse(player.hasHospital());
        player.setKilled(true);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            player.takeTurn();
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("loses their turn because they were assassinated"), "Loss message should appear");
        assertTrue(player.hasCompletedTurn(), "Turn should be marked completed");
    }

    /**
     * Tests that if a player was robbed, gold is transferred to the thief only when conditions are met:
     * - The victim is marked as stolen
     * - robbedBy is not null
     * - The victim has gold to steal
     * Then the victim receives income.
     */
    @Test
    void testRobbedPlayer_TransfersGoldToThief() {
        Player victim = game.getPlayers().get(2); // Player 3 (victim)
        Player thief = game.getPlayers().get(6);  // Player 7 (Thief)

        // Confirm initial setup
        assertTrue(victim.isStolen(), "Victim should be marked as stolen");
        assertNotNull(victim.getRobbedBy(), "Robber should be assigned");
        assertEquals(thief.getName(), victim.getRobbedBy().getName());

        // Ensure victim has gold so `stolenGold > 0` branch is activated
        victim.setGold(5);
        assertEquals(5, victim.getGold(), "Victim should start with 5 gold");

        // Capture output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            App.input = new ByteArrayInputStream("gold\n".getBytes());
            victim.takeTurn(); // Robbery logic should trigger
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("was robbed of 5 gold by Player 7"), "Expected robbery message to be printed");

        // Gold transfer check
        assertEquals(2, victim.getGold(), "Victim should end with 2 gold from income after being robbed");
        assertTrue(thief.getGold() >= 5, "Thief should receive 5 gold from victim");
    }

    /**
     * Tests that checkPoorHouseBonus() grants 1 gold if the player has 0 gold and owns Poor House.
     * Covers both branches: whether gold is granted or not.
     */
    @Test
    void testCheckPoorHouseBonus() {
        // Player 4 has Poor House built and 0 gold
        Player player = game.getPlayers().get(3); // Player 4
        assertTrue(player.hasPoorHouse(), "Should have Poor House built");
        assertEquals(0, player.getGold(), "Should start with 0 gold");

        // Capture output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            player.checkPoorHouseBonus();
        } finally {
            System.setOut(originalOut);
        }

        // Gold should be incremented
        assertEquals(1, player.getGold(), "Should gain 1 gold due to Poor House effect");

        String output = out.toString();
        assertTrue(output.contains("Poor House effect"), "Output should mention Poor House bonus");

        // Check second branch: if gold is not 0, nothing should happen
        player.setGold(2);
        out.reset();

        System.setOut(new PrintStream(out));
        try {
            player.checkPoorHouseBonus();
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(2, player.getGold(), "Gold should remain unchanged when not 0");
        assertFalse(out.toString().contains("Poor House"), "No bonus should be printed if gold > 0");
    }

    /**
     * Tests that swapHandWith correctly swaps the hands of two players.
     * Ensures all cards are moved between them and order is preserved.
     */
    @Test
    void testSwapHandWith_SwapsCardsCorrectly() {
        Player p1 = game.getPlayers().get(0); // Player 1
        Player p6 = game.getPlayers().get(5); // Player 6

        // Pre-checks: get initial hand sizes and card names
        List<String> originalP1 = p1.getHand().stream().map(DistrictCard::getName).collect(Collectors.toList());
        List<String> originalP6 = p6.getHand().stream().map(DistrictCard::getName).collect(Collectors.toList());

        assertFalse(originalP1.isEmpty(), "Player 1 hand should not be empty");
        assertFalse(originalP6.isEmpty(), "Player 6 hand should not be empty");

        // Perform the swap
        p1.swapHandWith(p6);

        // After swap, hands should match what the other originally had
        List<String> postP1 = p1.getHand().stream().map(DistrictCard::getName).collect(Collectors.toList());
        List<String> postP6 = p6.getHand().stream().map(DistrictCard::getName).collect(Collectors.toList());

        assertEquals(originalP6, postP1, "Player 1 should now have Player 6's original hand");
        assertEquals(originalP1, postP6, "Player 6 should now have Player 1's original hand");
    }

    /**
     * Tests that redrawCards() correctly discards selected cards and draws replacements from the deck.
     * Ensures proper update to hand and correct printed output.
     */
    @Test
    void testRedrawCards_ReplacesDiscardedCards() {
        Player player = game.getPlayers().get(0); // Player 1 (Magician)
        DistrictDeck districtDeck = game.getDeck();

        // Player 1's hand includes: Armory, Tavern, Castle, Laboratory, Dragon Gate, Observatory, Castle
        int originalHandSize = player.getHand().size();

        // Choose to discard the two Castles
        List<DistrictCard> toDiscard = player.getHand().stream()
                .filter(card -> card.getName().equals("Castle"))
                .collect(Collectors.toList());

        assertFalse(toDiscard.isEmpty(), "Expected at least one Castle to discard");

        // Capture output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            player.redrawCards(toDiscard);
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("discarded " + toDiscard.size()), "Expected discard count in output");
        assertTrue(output.contains("drew"), "Expected draw message");

        // The number of cards in hand should remain unchanged
        assertEquals(originalHandSize, player.getHand().size(), "Hand size should remain constant after redraw");

        // The discarded cards should not remain in hand
        for (DistrictCard discarded : toDiscard) {
            assertFalse(player.getHand().contains(discarded), "Hand should not contain discarded card: " + discarded.getName());
        }
    }

    /**
     * Tests that redrawCards() handles an empty deck gracefully.
     */
    @Test
    void testRedrawCards_DeckRunsOut() {
        Player player = game.getPlayers().get(1);
        DistrictDeck districtDeck = game.getDeck();

        // Empty the DistrictDeck
        while (districtDeck.draw() != null) {
        }

        assertEquals(0, districtDeck.size(), "DistrictDeck should be empty");

        // Get at least 1 card to discard
        List<DistrictCard> toDiscard = new ArrayList<>(player.getHand().subList(0, 1));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        game.getDeck().clear();

        try {
            System.out.println("DistrictDeck has: " + game.getDeck().size() + " cards before redraw");
            player.redrawCards(toDiscard);
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("DistrictDeck is empty. Could not draw enough cards."), "Should warn about empty DistrictDeck");
    }

    /**
     * Tests the duplicate build rules including Quarry logic and other duplicates.
     * Covers all conditional branches in canBuildDistrict.
     */
    @Test
    void testCanBuildDistrict_DuplicateRulesWithQuarry() {
        Player player = game.getPlayers().get(0); // Player 1
        assertTrue(player.hasQuarry()); // Built Quarry
        player.setGold(20); // Make sure gold isn't the limiting factor

        // --- Already has two Castles built ---
        DistrictCard castle = new DistrictCard("Castle", "yellow", 4, 1, null);

        String result = player.canBuildDistrict(castle);
        assertEquals("You already have two of that district. You cannot build more, even with Quarry.", result);

        // --- Already has one Castle built, and one other duplicate (Tavern), should block ---
        // Remove one Castle from built districts for controlled test
        game.getBoard().getCityForPlayer(player).removeIf(d -> d.getName().equals("Castle"));
        game.getBoard().getCityForPlayer(player).add(new DistrictCard("Castle", "yellow", 4, 1, null)); // 1 Castle
        game.getBoard().getCityForPlayer(player).add(new DistrictCard("Tavern", "green", 1, 1, null));
        game.getBoard().getCityForPlayer(player).add(new DistrictCard("Tavern", "green", 1, 1, null)); // Duplicate Tavern

        result = player.canBuildDistrict(castle);
        assertEquals("Quarry only allows one duplicated district in your city.", result);

        // --- Only one Castle built, no other duplicates, should allow with Quarry ---
        // Remove one Tavern to eliminate the other duplicate
        game.getBoard().getCityForPlayer(player).removeIf(d -> d.getName().equals("Tavern"));
        result = player.canBuildDistrict(castle);
        assertNull(result, "Should be allowed to build one duplicate with Quarry");

        // --- No Quarry, one Castle already built ---
        // Temporarily remove Quarry from built list
        game.getBoard().getCityForPlayer(player).removeIf(d -> d.getName().equals("Quarry"));
        result = player.canBuildDistrict(castle);
        assertEquals("You already have that district built. You need a Quarry to build a duplicate.", result);
    }

    /**
     * Tests that if a player has Factory, the cost to build other purple cards is reduced by 1.
     */
    @Test
    void testGetBuildCost_WithFactory_ReducesPurpleCardCost() {
        Player player = game.getPlayers().get(0); // Player 1
        assertFalse(player.hasFactory(), "Precondition: should not already have Factory built");

        // Manually build Factory
        DistrictCard factory = new DistrictCard("Factory", "purple", 6, 1, "Reduces cost of other purple districts");
        game.getBoard().getCityForPlayer(player).add(factory);

        // Purple card that is not the Factory
        DistrictCard dragonGate = new DistrictCard("Dragon Gate", "purple", 6, 1, "Worth 8 points");

        int reducedCost = player.getBuildCost(dragonGate);
        assertEquals(5, reducedCost, "Factory should reduce purple card cost by 1");

        // Also check that Factory does NOT reduce itself
        int factoryCost = player.getBuildCost(factory);
        assertEquals(6, factoryCost, "Factory should not reduce its own cost");
    }

    /**
     * Tests the onDistrictBuilt() method for both Bell Tower and Lighthouse.
     * Covers these branches:
     * - Bell Tower triggers game-end activation
     * - Lighthouse draws the correct high-cost card
     * - Lighthouse removes the drawn card from the deck
     */
    @Test
    void testOnDistrictBuilt_TriggersLighthouseAndBellTower() {
        AIPlayer ai = (AIPlayer) game.getPlayers().get(6); // Player 7 is an AI
        DistrictDeck districtDeck = game.getDeck();

        // Set up the DistrictDeck with predictable high-cost cards
        districtDeck.clear();
        DistrictCard purpleMax = new DistrictCard("Wishing Well", "purple", 5, 1, "Test effect");
        DistrictCard yellowMax = new DistrictCard("Palace", "yellow", 5, 1, "");
        DistrictCard notMax = new DistrictCard("Harbor", "green", 4, 1, "");
        districtDeck.addCardsToBottom(Arrays.asList(purpleMax, yellowMax, notMax));

        // Clear AI hand for clean test
        ai.getHand().clear();
        assertEquals(0, ai.getHand().size());

        // ✅ Lighthouse effect: AI should draw a max-cost card
        DistrictCard lighthouse = new DistrictCard("Lighthouse", "purple", 3, 1, "");
        ai.onDistrictBuilt(lighthouse);

        int before = ai.getHand().size();
        ai.onDistrictBuilt(lighthouse);
        int after = ai.getHand().size();

        assertEquals(before + 1, after, "AI should draw one card from Lighthouse effect");
        String drawn = ai.getHand().get(0).getName();
        assertTrue(
                drawn.equals("Wishing Well") || drawn.equals("Palace"),
                "AI should draw a 5-cost max card"
        );
        assertFalse(districtDeck.peekAll().stream().anyMatch(c -> c.getName().equals(drawn)),
                "Drawn card should be removed from the DistrictDeck");

        // ✅ Bell Tower effect: should activate game-end condition
        assertFalse(game.isBellTowerActive(), "Game should start with Bell Tower off");
        DistrictCard bellTower = new DistrictCard("Bell Tower", "purple", 5, 1, "Ends game at 7 districts");
        ai.onDistrictBuilt(bellTower);
        assertTrue(game.isBellTowerActive(), "Bell Tower should activate the end-game trigger");
    }

    /**
     * Tests that if the AI builds Lighthouse and the deck is empty, it prints a warning and doesn't crash.
     * Also confirms no cards are added to the AI's hand.
     */
    @Test
    void testOnDistrictBuilt_LighthouseWithEmptyDeck_PrintsWarning() {
        AIPlayer ai = (AIPlayer) game.getPlayers().get(6); // Player 7 is AI
        DistrictDeck districtDeck = game.getDeck();

        // Clear the DistrictDeck so it's empty
        districtDeck.clear();
        assertEquals(0, districtDeck.size(), "DistrictDeck should be empty for this test");

        // Clear AI's hand to verify it stays empty
        ai.getHand().clear();

        DistrictCard lighthouse = new DistrictCard("Lighthouse", "purple", 3, 1, "");

        // Capture output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(out));

        try {
            ai.onDistrictBuilt(lighthouse);
        } finally {
            System.setOut(originalOut);
        }

        String output = out.toString();
        assertTrue(output.contains("built Lighthouse, but the DistrictDeck is empty"), "Expected warning about empty DistrictDeck");
        assertEquals(0, ai.getHand().size(), "AI should not gain a card from an empty DistrictDeck");
    }

    /**
     * Tests that Haunted City can act as a wildcard color if not built in the final round,
     * and cannot do so if built in the final round.
     * Covers all logic branches of the Haunted City clause.
     */
    @Test
    void testHasAllColors_WithAndWithoutHauntedCity() {
        Player player = game.getPlayers().get(2); // Player 3
        List<DistrictCard> city = game.getCityForPlayer(player);
        int finalRound = game.getFinalRoundNumber();

        // Sanity check: Haunted City not built in final round
        assertEquals("Haunted City", city.get(0).getName());
        assertNotEquals(finalRound, city.get(0).getBuiltRound());

        // Missing red, yellow, blue
        boolean result = player.hasAllDistrictTypes(city, finalRound);
        assertFalse(result, "Should not qualify as full color set yet");

        // Add missing color cards to city
        city.add(new DistrictCard("Prison", "red", 2, 1, null));
        city.add(new DistrictCard("Castle", "yellow", 4, 1, null));
        city.add(new DistrictCard("Temple", "blue", 1, 1, null));

        result = player.hasAllDistrictTypes(city, finalRound);
        assertTrue(result, "Haunted City should complete the color set");

        // Now simulate Haunted City built during final round
        city.clear();
        DistrictCard haunted = new DistrictCard("Haunted City", "purple", 2, 1, null);
        haunted.setBuiltRound(finalRound);
        city.add(haunted);
        city.add(new DistrictCard("Castle", "yellow", 4, 1, null));
        city.add(new DistrictCard("Temple", "blue", 1, 1, null));
        city.add(new DistrictCard("Prison", "red", 2, 1, null));
        city.add(new DistrictCard("Market", "green", 2, 1, null));

        result = player.hasAllDistrictTypes(city, finalRound);
        assertTrue(result, "Should still have all 5 colors without needing Haunted City");
    }

    /**
     * Tests that all missing color flags are detected correctly when no matching colored districts exist.
     * Covers all five color booleans being false and incrementing the missing counter.
     */
    @Test
    void testHasAllColors_AllColorsMissing() {
        Player player = game.getPlayers().get(3); // Pick any player
        List<DistrictCard> city = game.getCityForPlayer(player);
        city.clear(); // Ensure no districts are present
        int finalRound = game.getFinalRoundNumber();

        // Sanity check: city is empty
        assertEquals(0, city.size(), "City should be empty for this test");

        boolean result = player.hasAllDistrictTypes(city, finalRound);

        // Should be false because no colors are present at all
        assertFalse(result, "Player with no colors built should not qualify for diverse city bonus");
    }

    /**
     * Tests that Haunted City acts as a wildcard color only if it completes exactly one missing color.
     * Ensures it returns true if one color is missing and Haunted City is present.
     */
    @Test
    void testHasAllColors_HauntedCityFillsOneMissingColor() {
        Player player = game.getPlayers().get(2); // Player 3
        List<DistrictCard> city = game.getCityForPlayer(player);
        city.clear();
        int finalRound = game.getFinalRoundNumber();

        // Build Haunted City (not in final round)
        DistrictCard haunted = new DistrictCard("Haunted City", "purple", 2, 1, null);
        haunted.setBuiltRound(game.getFinalRoundNumber() - 1);
        city.add(haunted);

        // Add 4 of 5 colors (missing BLUE)
        city.add(new DistrictCard("Watchtower", "red", 1, 1, null));
        city.add(new DistrictCard("Market", "green", 2, 1, null));
        city.add(new DistrictCard("Castle", "yellow", 4, 1, null));
        city.add(new DistrictCard("Graveyard", "purple", 5, 1, null)); // doesn't count as missing

        // Now it should be missing exactly 1 (blue) and have Haunted City
        boolean result = player.hasAllDistrictTypes(city, finalRound);
        assertTrue(result, "Haunted City should fill in the missing blue and complete the set");
    }

    /**
     * Checks that the Park bonus works for a human player.
     * If they have no cards and Park is in their city, they should draw 2 cards.
     * This also makes sure that the correct message is printed to System.out.
     */
    @Test
    void testParkBonus_HumanPlayerHasNoCardsAndHasParkBuilt_DisplaysDrawnCards() {
        // Setup: make player 1 (Human) have Park and no cards
        HumanPlayer human = (HumanPlayer) game.getPlayers().get(0);
        human.getHand().clear(); // empty hand
        game.getCityForPlayer(human).add(new DistrictCard("Park", "purple", 6, 1, "If you have no cards in your hand at the end of your turn, you may draw 2 cards from the District DistrictDeck."));

        // Run method
        human.checkParkBonus();

        // Assert: hand now has 2 more cards
        assertEquals(2, human.getHand().size(), "Player should have drawn 2 cards due to Park bonus.");
    }

    /**
     * Tests that an AI player with an empty hand and the Park built
     * draws two cards when checkParkBonus is triggered.
     * The Park must be in their city (Board) for the bonus to activate.
     */
    @Test
    void testParkBonus_AIPlayerHasNoCardsAndHasParkBuilt_PrintsGenericMessage() {
        // Setup: create dummy AI with Park and empty hand
        AIPlayer ai = new AIPlayer("AI Test");
        ai.setGame(game);
        ai.getHand().clear();

        // Register player in the game if necessary
        game.getPlayers().add(ai);

        // Add Park to AI's city using official method
        DistrictCard park = new DistrictCard("Park", "purple", 6, 1, "If you have no cards in your hand at the end of your turn, you may draw 2 cards from the District DistrictDeck.");
        game.getBoard().addDistrictToCity(ai, park);

        // Run method
        ai.checkParkBonus();

        // Assert: hand now has 2 cards
        assertEquals(2, ai.getHand().size(), "AI should have drawn 2 cards due to Park bonus.");
    }

    /**
     * Checks that if a player has cards in their hand, the Park effect doesn’t trigger.
     * Even if they have Park built, nothing should happen.
     */
    @Test
    void testParkBonus_PlayerWithCards_DoesNothing() {
        // Setup: player has cards in hand
        Player player = game.getPlayers().get(0);
        game.getCityForPlayer(player).add(new DistrictCard("Park", "purple", 6, 1, ""));
        assertFalse(player.getHand().isEmpty(), "Precondition: player must have cards");

        // Save current hand
        int beforeSize = player.getHand().size();

        // Run method
        player.checkParkBonus();

        // Hand should remain the same
        assertEquals(beforeSize, player.getHand().size(), "No cards should be drawn when hand is not empty.");
    }

    /**
     * Checks that if a player has no cards but doesn’t have Park built, nothing happens.
     * Covers the case where the hand is empty but Park is missing.
     */
    @Test
    void testParkBonus_PlayerWithoutPark_DoesNothing() {
        // Setup: remove Park from built list, and clear hand
        Player player = game.getPlayers().get(0);
        game.getCityForPlayer(player).removeIf(c -> c.getName().equalsIgnoreCase("Park"));
        player.getHand().clear();

        // Run method
        player.checkParkBonus();

        // No cards should be drawn
        assertTrue(player.getHand().isEmpty(), "No cards should be drawn without Park.");
    }

    /**
     * Tests the Smithy effect:
     * Player pays 2 gold and draws 3 cards from the deck.
     * This test checks that 3 cards are added, gold is reduced,
     * and the Smithy is marked as used.
     */
    @Test
    void testUseSmithy_DrawsThreeCardsAndMarksUsed() {
        // Setup: Player 1 starts with 10 gold and has a Smithy
        Player player = game.getPlayers().get(0);
        int originalGold = player.getGold();
        int originalHandSize = player.getHand().size();

        // Run method
        player.useSmithy();

        // Verify hand increased by 3
        assertEquals(originalHandSize + 3, player.getHand().size(), "Player should draw 3 cards from Smithy.");

        // Verify 2 gold was spent
        assertEquals(originalGold - 2, player.getGold(), "Player should spend 2 gold for Smithy.");

        // Verify Smithy marked as used
        assertTrue(player.hasUsedSmithy(), "Smithy should be marked as used after activation.");
    }

    /**
     * Tests the Architect ability:
     * Player draws 2 cards and their build limit increases to 3.
     * This should only trigger if the player's character is Architect (turnOrder 7).
     */
    @Test
    void testUseAbility_WhenArchitect_DrawsTwoCardsAndSetsMaxBuilds() {
        // Setup: Player 5 is the Architect in the loaded save
        Player architect = game.getPlayers().get(4); // index 4 = Player 5
        int originalHandSize = architect.getHand().size();

        // Confirm preconditions
        assertEquals(7, architect.getChosenCharacter().getTurnOrder(), "Precondition failed: character must be Architect");
        assertEquals(3, architect.getMaxBuildsPerTurn(), "Architect's build limit should already be 3 in save");

        // Run method
        architect.useAbility();

        // Check new hand size (drawn 2 cards)
        assertEquals(originalHandSize + 2, architect.getHand().size(), "Architect should draw 2 cards");

        // Check max build limit remains 3
        assertEquals(3, architect.getMaxBuildsPerTurn(), "Architect build limit should be 3");

        // Output is not asserted here, but is visible in console: "[name] drew 2 cards due to Architect ability."
    }

    /**
     * Checks that useAbility() does nothing if the player has no chosen character.
     * Specifically tests the line: if (getChosenCharacter() == null) return;
     */
    @Test
    void testUseAbility_WhenNoCharacterChosen_DoesNothing() {
        // Setup: Remove chosen character for Player 1
        Player player = game.getPlayers().get(0);
        player.setChosenCharacter(null);  // set to null explicitly
        int originalHandSize = player.getHand().size();
        int originalBuildLimit = player.getMaxBuildsPerTurn();

        // Run method
        player.useAbility();

        // Nothing should have changed
        assertEquals(originalHandSize, player.getHand().size(), "Hand should stay the same when no character is chosen.");
        assertEquals(originalBuildLimit, player.getMaxBuildsPerTurn(), "Build limit should not change when no character is chosen.");
    }

    /**
     * Tests the Laboratory ability:
     * Discards a card at the given index from the player's hand
     * and gives the player 1 gold. Marks Laboratory as used.
     */
    @Test
    void testUseLaboratory_DiscardCardGainGoldAndMarkUsed() {
        // Setup: Player 1 has Laboratory in hand at index 3
        Player player = game.getPlayers().get(0);
        int indexToDiscard = 3; // Index of the Laboratory card
        int originalGold = player.getGold();
        int originalHandSize = player.getHand().size();

        // Sanity check to avoid accidental index mismatch
        assertEquals("Laboratory", player.getHand().get(indexToDiscard).getName(), "Precondition failed: card at index must be Laboratory");

        // Run ability
        player.useLaboratory(indexToDiscard);

        // Player gains 1 gold
        assertEquals(originalGold + 1, player.getGold(), "Player should gain 1 gold after using Laboratory");

        // Hand size reduced by 1
        assertEquals(originalHandSize - 1, player.getHand().size(), "One card should be removed from hand");

        // Flag set
        assertTrue(player.hasUsedLaboratory(), "Laboratory should be marked as used");
    }

    /**
     * Tests the Museum effect:
     * Adds a card to the list of museumStoredCards and sets the flag.
     */
    @Test
    void testStoreInMuseum_AddsCardAndMarksUsed() {
        // Setup: Player 1 has Museum built and unused
        Player player = game.getPlayers().get(0);
        int originalStoredSize = player.getMuseumStoredCards().size();

        // Choose a card to store (we'll use the first card in hand)
        DistrictCard cardToStore = player.getHand().get(0);

        // Run the method
        player.storeInMuseum(cardToStore);

        // Check that the card was added to the stored list
        assertEquals(originalStoredSize + 1, player.getMuseumStoredCards().size(), "Card should be added to Museum");

        // Check that the correct card was added (by name)
        assertEquals(cardToStore.getName(), player.getMuseumStoredCards().get(originalStoredSize).getName(), "Correct card should be stored");

        // Check that Museum was marked as used
        assertTrue(player.hasUsedMuseum(), "Museum should be marked as used this turn");
    }

    /**
     * Tests that when a drawn card is not null,
     * it is added to the player's hand.
     * This covers the if (drawn != null) { addCardToHand(drawn); } line.
     */
    @Test
    void testDrawnCard_NotNull_AddsToHand() {
        // Setup: Player 1 and pre-check deck is not empty
        Player player = game.getPlayers().get(0);
        int handBefore = player.getHand().size();

        // Simulate drawing a valid card from the deck
        DistrictCard drawn = game.getDeck().draw();
        assertNotNull(drawn, "Precondition: deck must have at least one card to draw");

        // Run the line manually
        player.addCardToHand(drawn);

        // Verify hand size increased
        assertEquals(handBefore + 1, player.getHand().size(), "Hand should increase by 1 if drawn card is not null");

        // Check that card was actually added
        assertTrue(player.getHand().contains(drawn), "Drawn card should be in hand");
    }

    /**
     * Tests that useAbility() gives gold based on the color bonus from
     * GameUtils.getBonusColorForCharacter, for each valid character number.
     * Also confirms the default (null) path gives no gold.
     */
    @Test
    public void testUseAbility_BonusColorGoldAward_Java8Compatible() {
        // Format: {playerIndex, characterNumber, expectedBonusGold}
        Object[][] testCases = {
                {5, 4, 1}, // Player 6 — King — School of Magic counts as 1 yellow
                {4, 5, 0}, // Player 5 - Bishop - 0 blue
                {2, 6, 2}, // Player 3 — Merchant — 1 green district + 1 passive gold
                {1, 8, 0}, // Player 2 - Warlord - 0 red
                {0, 3, 0}  // Player 1 - Magician (no bonus color)
        };

        for (int i = 0; i < testCases.length; i++) {
            int playerIndex = (int) testCases[i][0];
            int charNum = (int) testCases[i][1];
            int expectedBonus = (int) testCases[i][2];

            Player player = game.getPlayers().get(playerIndex);

            // Set up character manually
            player.setChosenCharacter(new CharacterCard("Mock", charNum, "test"));

            int goldBefore = player.getGold();
            player.useAbility();
            int goldAfter = player.getGold();

            assertEquals(expectedBonus, goldAfter - goldBefore,
                    "Character " + charNum + " should yield " + expectedBonus + " gold");
        }
    }

    /**
     * Dummy player subclass to test default chooseIncome() from base Player.
     * The method does nothing by default and should not throw or modify state.
     */
    @Test
    public void testChooseIncome_DefaultImplementationDoesNothing() {
        // Create dummy Player that uses base class method
        Player dummy = new Player("Tester") {
            @Override
            public CharacterCard chooseCharacter(List<CharacterCard> available) {
                return null;
            }

            @Override
            public void takeTurn() {
            } // Required abstract method

            @Override
            public void buildDistrict() {

            }

            @Override
            public void destroyDistrict(Player target, int districtIndex, boolean usedArmory) {

            }
        };
        dummy.setGame(game);

        DistrictDeck districtDeck = game.getDeck();

        int goldBefore = dummy.getGold();
        int handBefore = dummy.getHand().size();

        // Should not throw, should not change state
        dummy.chooseIncome(districtDeck);

        assertEquals(goldBefore, dummy.getGold(), "Gold should remain unchanged");
        assertEquals(handBefore, dummy.getHand().size(), "Hand size should remain unchanged");
    }

    /**
     * Tests the canDestroyDistrict() method for all specified branches:
     * - bellTowerActive return path
     * - builtDistricts.size() >= limit
     * - districtIndex out of bounds
     * - attempting to destroy Keep
     */
    @Test
    public void testCanDestroyDistrict_AllSpecifiedBranches() {
        // Setup: Warlord with enough gold
        Player warlord = game.getPlayers().get(1); // Player 2
        warlord.setGold(10);

        // --- Bell Tower active blocks destruction ---
        game.activateBellTower(); // limit becomes 7
        // Player 1 has 6 districts — simulate one more to reach limit
        game.getBoard().getCityForPlayer(game.getPlayers().get(0)).add(
                new DistrictCard("Dummy", "blue", 1, 1, null)
        );
        assertFalse(warlord.canDestroyDistrict(game.getPlayers().get(0), 0),
                "Should return false when Bell Tower is active and target has 7+ districts");
        game.deactivateBellTower(); // Reset

        // --- Too many districts (limit = 8) ---
        while (game.getCityForPlayer(game.getPlayers().get(0)).size() < 8) {
            game.getBoard().addDistrictToCity(game.getPlayers().get(0),
                    new DistrictCard("Filler", "red", 1, 1, null));
        }
        assertFalse(warlord.canDestroyDistrict(game.getPlayers().get(0), 0),
                "Should return false when player has 8+ districts and Bell Tower is off");

        // --- districtIndex < 0 ---
        assertFalse(warlord.canDestroyDistrict(game.getPlayers().get(3), -1),
                "Should return false for index < 0");

        // --- districtIndex >= builtDistricts.size() ---
        int tooHigh = game.getCityForPlayer(game.getPlayers().get(3)).size();
        assertFalse(warlord.canDestroyDistrict(game.getPlayers().get(3), tooHigh),
                "Should return false for index >= builtDistricts.size()");

        // --- District is 'Keep' (case-insensitive match) ---
        AIPlayer targetWithKeep = new AIPlayer("KeepTarget");
        targetWithKeep.setGame(game);
        targetWithKeep.setGold(0);
        game.getPlayers().add(targetWithKeep);
        game.getBoard().addDistrictToCity(targetWithKeep, new DistrictCard("Keep", "purple", 3, 1, "..."));

        targetWithKeep.setGame(game);
        targetWithKeep.setGold(0); // not important
        game.getBoard().addDistrictToCity(targetWithKeep,
                new DistrictCard("Keep", "purple", 3, 1, "The Keep cannot be destroyed by the Warlord.")
        );
        assertFalse(warlord.canDestroyDistrict(targetWithKeep, 0),
                "Should return false if the district is Keep (case-insensitive)");
    }

    /**
     * Tests the cost modifier from Great Wall when calculating destroy cost.
     * The Warlord reduces cost by 1 when destroying districts,
     * but Great Wall increases cost by 1 for any district other than itself.
     */
    @Test
    public void testGreatWallDestroyCostModifier() {
        // Player 2 is the Warlord and has Great Wall built
        Player playerWithWall = game.getPlayers().get(1); // Player 2

        // Target district is NOT the Great Wall → cost should be:
        // base cost (1 for Temple) - 1 (Warlord) + 1 (Great Wall) = 1
        DistrictCard target1 = new DistrictCard("Temple", "blue", 1, 1, null);
        int cost1 = playerWithWall.getDestroyCost(target1);
        assertEquals(1, cost1, "Destroy cost should be base 1 -1 (Warlord) +1 (Great Wall) = 1");

        // Target district IS the Great Wall → cost should be:
        // base cost (6) - 1 (Warlord) + 0 (no extra cost for Great Wall itself) = 5
        DistrictCard target2 = new DistrictCard("Great Wall", "purple", 6, 1, "The cost for the Warlord to destroy any of your other districts is increased by 1 gold.");
        int cost2 = playerWithWall.getDestroyCost(target2);
        assertEquals(5, cost2, "Destroy cost of Great Wall itself should be base 6 -1 (Warlord) = 5");
    }
}