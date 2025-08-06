package citadels.core;

import citadels.app.App;
import citadels.model.*;
import citadels.state.Board;
import citadels.state.CharacterDeck;
import citadels.state.DistrictDeck;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents the main game logic and state management for a session of Citadels.
 * Handles setup, character selection, and progression of rounds.
 */
public class Game {

    private static Random random = new Random();         // Creates a new random seed
    private final List<CharacterCard> discardedCharacterCardsFaceUp = new ArrayList<>(); // Face-up discards
    protected CharacterDeck characterDeck;                         // Character DistrictDeck
    protected int currentRound = 1;                                // Tracks which round we're on
    protected int crownHolderIndex;                                // Index of player who has the crown
    private List<Player> players;                        // All players in the game
    private DistrictDeck districtDeck;                                   // District DistrictDeck
    private Board board;                                 // Handles turn order and UI-like output
    private InputHandler inputHandler;                   // Handles input commands from human player
    private Scanner scanner;
    private CharacterCard discardedCharacterCardFaceDown;                    // The face-down discarded character
    private int currentPlayerIndex = 0;                  // Player whose turn is being processed
    private Player currentSelectingPlayer = null;        // Player currently choosing character
    private int activeCharacterTurnOrder = -1;           // Turn order of currently active character
    private Phase currentPhase = Phase.SELECTION;                         // Current game phase
    private int finalRoundNumber = -1;                   // Which round the game ends on
    private boolean isGameEnding = false;                // Flag for whether the game is ending
    private Player firstToCompleteCityPlayer = null;     // First player to complete their city
    private boolean bellTowerActive = false;             // If Bell Tower has been activated
    private boolean testingMode = false;                 // To turn off automatic scanner

    /**
     * Constructor that starts a new game and prompts user for number of players.
     *
     * @param districtCardTemplates Templates used to build the district card DistrictDeck
     */
    public Game(List<DistrictCard> districtCardTemplates) {
        int numPlayers = promptNumberOfPlayers();
        initializeGameState(districtCardTemplates, numPlayers);
        characterDeck = new CharacterDeck(GameUtils.createCharacterCards());
        board = new Board(players);
    }

    /**
     * Alternate constructor for testing or scripted game setup.
     *
     * @param districtCardTemplates District cards to use
     * @param numPlayers            Number of players
     */
    public Game(List<DistrictCard> districtCardTemplates, int numPlayers) {
        initializeGameState(districtCardTemplates, numPlayers);
        characterDeck = new CharacterDeck(GameUtils.createCharacterCards());
        board = new Board(players);
    }

    /**
     * Creates a new Game instance with default settings.
     * Use this when setting up a new game or loading from a save file later.
     */
    public Game() {
        // Default setup
    }

    /**
     * Overrides the random generator used by the game.
     * Useful for injecting predictable randomness during tests.
     *
     * @param r The Random instance to use for all game randomness
     */
    public static void setRandom(Random r) {
        random = r;
    }

    /**
     * Sets up decks, players, crown, and input handler.
     *
     * @param districtCardTemplates All district card definitions
     * @param numPlayers            Total number of players
     */
    private void initializeGameState(List<DistrictCard> districtCardTemplates, int numPlayers) {
        districtDeck = new DistrictDeck(districtCardTemplates);
        setupPlayers(numPlayers);
        System.out.println("\nWelcome to Citadels!");
        GameUtils.delay(1000);
        System.out.println();
        assignCrown(numPlayers);
        inputHandler = new InputHandler(this);
    }

    /**
     * Prompts user to enter number of players between 4 and 7.
     *
     * @return Validated player count
     */
    private int promptNumberOfPlayers() {
        int numPlayers = 0;
        // Keep looping until the user enters a valid number of players
        while (true) {
            System.out.print("Enter number of players [4-7]: ");
            String input = getScanner().nextLine();
            // Try converting the user input into a number
            try {
                numPlayers = Integer.parseInt(input);
                if (numPlayers >= 4 && numPlayers <= 7) break;
                System.out.println("Please enter a number between 4 and 7.");
                // Catch input that isn’t a number and ask again
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 4 and 7.");
            }
        }
        return numPlayers;
    }

    /**
     * Initializes human and AI players, sets gold, and deals initial hands.
     *
     * @param numPlayers Number of total players
     */
    private void setupPlayers(int numPlayers) {
        players = new ArrayList<>();
        HumanPlayer human = new HumanPlayer("Player 1");
        human.setGame(this);
        players.add(human);

        // Add AI players starting from player 2 onward
        for (int i = 2; i <= numPlayers; i++) {
            AIPlayer ai = new AIPlayer("Player " + i);
            ai.setGame(this);
            players.add(ai);
        }

        // Give each player their starting cards and gold
        for (Player player : players) {
            player.setGold(2);
            // Each player draws 4 cards to start with
            for (int j = 0; j < 4; j++) {
                DistrictCard card = districtDeck.draw();
                // Only add the card if the DistrictDeck wasn't empty
                if (card != null) {
                    player.addCardToHand(card);
                }
            }
        }
    }

    /**
     * Randomly assigns the crown to one player and prints who starts.
     *
     * @param numPlayers Number of players
     */
    private void assignCrown(int numPlayers) {
        crownHolderIndex = random.nextInt(numPlayers);
        System.out.println(players.get(crownHolderIndex).getName() + " is the crowned player and goes first.");
    }

    /**
     * Returns a new Scanner that reads from the current App.input stream.
     * This ensures test input is correctly picked up instead of System.in.
     *
     * @return a fresh Scanner bound to App.input
     */
    protected Scanner getScanner() {
        if (scanner == null) {
            scanner = new Scanner(App.input);
        }
        return scanner;
    }

    /**
     * Handles entire character selection phase, including discards and player picks.
     * Ensures King is not discarded face-up and triggers Throne Room if crown changes.
     */
    public void startCharacterSelectionPhase() {
        int previousCrownHolderIndex = crownHolderIndex;

        Player kingPlayer = null;
        // Search for the player who chose the King (turnOrder 4)
        for (Player player : players) {
            CharacterCard chosen = player.getChosenCharacter();
            // Found the King — this player will go first
            if (chosen != null && chosen.getTurnOrder() == 4) {
                kingPlayer = player;
                break;
            }
        }

        // If a new King is found, update crown holder and check for Throne Room bonus
        if (kingPlayer != null) {
            crownHolderIndex = players.indexOf(kingPlayer);
            // Only check Throne Room bonus if crown changed hands after round 1
            if (currentRound > 1 && crownHolderIndex != previousCrownHolderIndex) {
                for (Player player : players) {
                    // Give a coin bonus to anyone who built the Throne Room
                    if (player.hasThroneRoom()) {
                        player.setGold(player.getGold() + 1);
                        System.out.println(player instanceof HumanPlayer
                                ? "Throne Room effect: You gain 1 gold because the crown changed hands."
                                : player.getName() + " gained 1 gold from Throne Room (crown changed).");
                    }
                }
            }
            System.out.println(kingPlayer.getName() + " is the crowned player and goes first.");
            // If no King was chosen but it’s not the first round, warn and continue
        } else if (currentRound > 1) {
            System.out.println("No one chose the King. The crown remains with " +
                    players.get(crownHolderIndex).getName() + ".");
        }

        System.out.println("Press 't' to process turns");
        System.out.println("================================");
        System.out.println("SELECTION PHASE");
        System.out.println("================================");

        GameUtils.waitForTCommand(getScanner());

        discardedCharacterCardFaceDown = null;
        clearAllPlayersKilledStatus();

        characterDeck = new CharacterDeck(GameUtils.createCharacterCards());

        boolean kingDiscardedFaceUp;
        do {
            characterDeck.shuffle();

            discardedCharacterCardFaceDown = discardFaceDownCard();
            System.out.println("A mystery character was removed.");

            discardFaceUpCards();
            GameUtils.printDiscardedCharacterCardsFaceUp(discardedCharacterCardsFaceUp);

            kingDiscardedFaceUp = discardedCharacterCardsFaceUp.stream()
                    .anyMatch(c -> c.getTurnOrder() == 4);

            // Restore discarded face-up cards to the DistrictDeck if King was one of them
            if (kingDiscardedFaceUp) {
                System.out.println("King was discarded face-up. Reshuffling and discarding again...");
                characterDeck.getCards().addAll(discardedCharacterCardsFaceUp);
                // Also put the face-down card back into the DistrictDeck if it existed
                if (discardedCharacterCardFaceDown != null) {
                    characterDeck.getCards().add(discardedCharacterCardFaceDown);
                }
            }
            GameUtils.delay(1000);
            System.out.println();
        } while (kingDiscardedFaceUp);

        List<CharacterCard> availableCharacters = new ArrayList<>(characterDeck.getCards());
        int numPlayers = players.size();

        // Each player takes turns picking their character
        for (int i = 0; i < numPlayers; i++) {
            int playerIndex = (crownHolderIndex + i) % numPlayers;
            Player player = players.get(playerIndex);
            currentSelectingPlayer = player;

            List<CharacterCard> characterChoices = new ArrayList<>(availableCharacters);
            // The last player can pick the face down card
            if (i == numPlayers - 1 && discardedCharacterCardFaceDown != null) {
                characterChoices.add(discardedCharacterCardFaceDown);
            }

            CharacterCard chosen = promptPlayerCharacterChoice(player, characterChoices);
            player.setChosenCharacter(chosen);

            if (!(i == numPlayers - 1 && discardedCharacterCardFaceDown != null && chosen == discardedCharacterCardFaceDown)) {
                availableCharacters.remove(chosen);
            }
        }
        currentSelectingPlayer = null;
    }

    /**
     * Removes character cards face-up based on number of players.
     * 4 players = 2 face up; 5 players = 1 face up; 6-7 players = 0.
     */
    void discardFaceUpCards() {
        discardedCharacterCardsFaceUp.clear();

        int faceUpCount;
        // Determine how many face-up characters to discard based on player count
        switch (players.size()) {
            case 4:
                faceUpCount = 2;
                break;
            case 5:
                faceUpCount = 1;
                break;
            default:
                faceUpCount = 0;
                break;
        }

        Iterator<CharacterCard> it = characterDeck.getCards().iterator();
        // Discard the determined number of face-up characters
        while (faceUpCount > 0 && it.hasNext()) {
            discardedCharacterCardsFaceUp.add(it.next());
            it.remove();
            faceUpCount--;
        }
    }

    /**
     * Discards one character card face-down from the top of the DistrictDeck.
     *
     * @return The discarded card
     */
    CharacterCard discardFaceDownCard() {
        // Randomly remove one face-down character if DistrictDeck isn't empty
        if (!characterDeck.getCards().isEmpty()) {
            return characterDeck.getCards().remove(0);
        }
        return null;
    }

    /**
     * Prompts the given player to choose a character from a list.
     *
     * @param player    Player making the selection
     * @param available List of characters to choose from
     * @return The selected character
     */
    private CharacterCard promptPlayerCharacterChoice(Player player, List<CharacterCard> available) {
        return player.chooseCharacter(available);
    }

    /**
     * Clears the 'killed' status from all players at the start of each round.
     */
    private void clearAllPlayersKilledStatus() {
        // Clear all killed flags at the start of each round
        for (Player player : players) {
            player.setKilled(false);
        }
    }

    /**
     * Marks a character as killed based on the selected character number.
     *
     * @param killChoice The character turn number that was targeted by the Assassin.
     */
    public void markCharacterKilled(int killChoice) {
        // Mark the player with the character to be killed
        for (Player player : players) {
            CharacterCard chosen = player.getChosenCharacter();
            // Confirm the right character was chosen for assassination
            if (chosen != null && chosen.getTurnOrder() == killChoice) {
                player.setKilled(true);
                return;
            }
        }
    }

    /**
     * Returns the index of the current player whose turn it is.
     *
     * @return the current player's index in the player list
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    /**
     * Handles the full turn phase of the game, going through each character in order (1 to 8),
     * and giving the appropriate player their turn if they picked that character. If no player
     * has that character, it processes a null turn instead.
     * <p>
     * Each player will be allowed to take actions based on their character ability and then
     * end their turn. Once all character turns are done, the method prints a message and moves
     * to the next round.
     * <p>
     * This method also checks for special card effects like Poor House and Park, and triggers
     * the end-of-game condition if needed.
     */
    public void startTurnPhase() {
        if (activeCharacterTurnOrder <= 0) {
            activeCharacterTurnOrder = 1;
        }
        System.out.println("Character choosing is over, action round will now begin.");
        System.out.println("================================");
        System.out.println("TURN PHASE");
        System.out.println("================================");
        GameUtils.delay(1000);
        System.out.println();

        // Play turns in character order (1 = Assassin, ..., 8 = Warlord)
        for (int characterNum = activeCharacterTurnOrder; characterNum <= 8; characterNum++) {
            activeCharacterTurnOrder = characterNum;
            currentPlayerIndex = -1;

            // Find player for this character
            for (int i = 0; i < players.size(); i++) {
                Player player = players.get(i);
                CharacterCard chosen = player.getChosenCharacter();
                if (chosen != null && chosen.getTurnOrder() == characterNum) {
                    currentPlayerIndex = i;
                    break;
                }
            }

            System.out.println(characterNum + ": " + GameUtils.getCharacterNameByNumber(characterNum));

            if (currentPlayerIndex == -1) {
                System.out.println("No one is the " + GameUtils.getCharacterNameByNumber(characterNum));
                inputHandler.resetNullTurnFlag();
                while (!inputHandler.isNullTurnProcessed()) {
                    inputHandler.processInput(getScanner());
                }
            } else {
                if (getCurrentPlayer().hasCompletedTurn()) {
                    System.out.println(getCurrentPlayer().getName() + " already completed their turn.");
                    continue;
                }

                Player currentPlayer = getCurrentPlayer();
                currentPlayer.takeTurn();
                while (!getCurrentPlayer().hasCompletedTurn()) {
                    inputHandler.processInput(getScanner());
                }

                currentPlayer.checkPoorHouseBonus();
                currentPlayer.checkParkBonus();
                checkEndCondition();
            }
        }

        activeCharacterTurnOrder = -1;

        System.out.println("Everyone is done, new round!");
        GameUtils.delay(1000);
        System.out.println();
    }

    /**
     * Returns the player whose turn is currently active.
     *
     * @return The current player, or null if invalid index or no players exist
     */
    public Player getCurrentPlayer() {
        if (players == null || players.isEmpty()) return null;
        if (currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) return null;
        return players.get(currentPlayerIndex);
    }

    /**
     * Checks if any player has completed their city and triggers end-game if so.
     * Completion threshold is 7 if Bell Tower is active, otherwise 8.
     */
    public void checkEndCondition() {
        int requiredDistricts = isBellTowerActive() ? 7 : 8;

        // Check if any player has completed their city
        for (Player player : players) {
            if (getCityForPlayer(player).size() >= requiredDistricts) {
                // Only set endgame flag the first time a city is completed
                if (!isGameEnding) {
                    isGameEnding = true;
                    firstToCompleteCityPlayer = player;

                    System.out.println(player.getName() + " has completed their city with " +
                            getCityForPlayer(player).size() + " districts! " +
                            "The game will end after this round.");
                    GameUtils.delay(1000);
                    System.out.println();
                }
            }
        }
    }

    /**
     * Returns the final round number when the game is set to end.
     *
     * @return The round number the game ends on
     */
    public int getFinalRoundNumber() {
        return finalRoundNumber;
    }

    /**
     * Starts the end-game phase. Calculates scores, announces winner, and prints score breakdowns.
     */
    private void startEndPhase() {
        System.out.println("===== GAME END =====");
        GameUtils.delay(1000);
        System.out.println();
        Map<Player, PlayerScore> playerScores = calculateScores();
        Player winner = determineWinner(playerScores);
        GameUtils.printScoreBreakdown(playerScores, players, winner);
        GameUtils.delay(1000);
        System.out.println();
        System.out.println("Game has ended. Thanks for playing!");
    }

    /**
     * Returns the list of all players in the game.
     *
     * @return List of Player objects
     */
    public List<Player> getPlayers() {
        return players;
    }

    /**
     * Returns the district DistrictDeck used in the game.
     *
     * @return The DistrictDeck instance
     */
    public DistrictDeck getDeck() {
        return districtDeck;
    }

    /**
     * Returns the board used in the game.
     *
     * @return The Board instance
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Gets the list of face-up discarded character cards for the current round.
     *
     * @return List of discarded character cards (face-up)
     */
    public List<CharacterCard> getDiscardedCharacterCardsFaceUp() {
        return discardedCharacterCardsFaceUp;
    }

    /**
     * Gets the current DistrictDeck of character cards.
     *
     * @return The CharacterDeck instance
     */
    public CharacterDeck getCharacterDeck() {
        return characterDeck;
    }

    /**
     * Gets the number of the current round.
     *
     * @return Current round number
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * Returns the list of districts built by a specific player.
     *
     * @param player The player whose city to return
     * @return List of DistrictCard objects representing their built city
     */
    public List<DistrictCard> getCityForPlayer(Player player) {
        return board.getCityForPlayer(player);
    }

    /**
     * Adds a district to the specified player's city.
     *
     * @param player   The player to add the district to
     * @param district The district card being added
     */
    public void addDistrictToCity(Player player, DistrictCard district) {
        board.addDistrictToCity(player, district);
    }

    /**
     * Returns whether the Bell Tower is currently active.
     *
     * @return true if the Bell Tower was activated, false otherwise
     */
    public boolean isBellTowerActive() {
        return bellTowerActive;
    }

    /**
     * Activates the Bell Tower effect, which makes the game end after 7 districts instead of 8.
     */
    public void activateBellTower() {
        this.bellTowerActive = true;
        System.out.println("Bell Tower effect: The game will now end when a player builds 7 districts.");
    }

    /**
     * Deactivates the Bell Tower if it is active, restoring the default end condition.
     */
    public void deactivateBellTower() {
        // Only deactivate the Bell Tower if it’s currently active
        if (bellTowerActive) {
            System.out.println("Bell Tower was destroyed. Game end condition resets to 8 districts.");
            bellTowerActive = false;
        }
    }

    /**
     * Returns the player who selected the character with the given number.
     *
     * @param characterNumber The turn order number of the character
     * @return The player who picked that character, or null if no one did
     */
    public Player getPlayerByCharacterNumber(int characterNumber) {
        // Find the player who chose a character with the given number
        for (Player player : players) {
            CharacterCard chosen = player.getChosenCharacter();
            // Match player if their character number matches the one we're looking for
            if (chosen != null && chosen.getTurnOrder() == characterNumber) {
                return player;
            }
        }
        return null;
    }

    /**
     * Gets the player at the given player number (1-indexed).
     *
     * @param targetPlayerNum The number of the player (starting from 1)
     * @return The Player object, or null if the number is invalid
     */
    public Player getPlayerByNumber(int targetPlayerNum) {
        // Make sure the number is within range before accessing player list
        if (targetPlayerNum < 1 || targetPlayerNum > players.size()) {
            return null;
        }
        return players.get(targetPlayerNum - 1);
    }

    /**
     * Returns the chosen character's name and ability for the given player.
     *
     * @param player The player whose character info is requested
     * @return A string with the character name and ability, or a fallback if none is chosen
     */
    public String getCharacterAbilityInfo(Player player) {
        CharacterCard character = player.getChosenCharacter();
        // Skip if this player hasn't picked a character
        if (character == null) {
            return "No character chosen.";
        }
        return character.getName() + " - " + character.getAbilityDescription();
    }

    /**
     * Calculates and returns the scores for all players at the end of the game.
     *
     * @return A map of players to their corresponding PlayerScore objects
     */
    public Map<Player, PlayerScore> calculateScores() {
        Map<Player, PlayerScore> finalScores = new HashMap<>();
        // Calculate final score for each player
        for (Player player : players) {
            List<DistrictCard> builtDistricts = getCityForPlayer(player);
            boolean isFirstToFinish = (player == firstToCompleteCityPlayer);
            PlayerScore score = PlayerScore.calculateFor(player, builtDistricts, isFirstToFinish, finalRoundNumber, bellTowerActive);
            finalScores.put(player, score);
        }
        return finalScores;
    }

    /**
     * Determines the winner based on final scores. Breaks ties by character turn order.
     *
     * @param playerScores A map of players and their scores
     * @return The player who wins the game
     */
    public Player determineWinner(Map<Player, PlayerScore> playerScores) {
        // Get the highest total score
        int maxScore = playerScores.values().stream()
                .mapToInt(PlayerScore::getTotalScore)
                .max()
                .orElse(Integer.MIN_VALUE);

        // Get all players who have that score (could be a tie)
        List<Player> tiedPlayers = playerScores.entrySet().stream()
                // Filter out players who don't have the top score
                .filter(e -> e.getValue().getTotalScore() == maxScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // If only one player has it, return them
        if (tiedPlayers.size() == 1) return tiedPlayers.get(0);

        // Tie-breaker: character with higher turn order wins
        tiedPlayers.sort((p1, p2) -> Integer.compare(
                p2.getChosenCharacter().getTurnOrder(),
                p1.getChosenCharacter().getTurnOrder()
        ));

        return tiedPlayers.get(0);
    }

    /**
     * Saves the current game state to a JSON file.
     *
     * @param filename The name of the file to save to
     */
    @SuppressWarnings("unchecked")
    public void saveGame(String filename) {
        JSONObject gameData = new JSONObject();

        // Basic game state info
        gameData.put("currentRound", currentRound);
        gameData.put("currentPlayerIndex", currentPlayerIndex);
        gameData.put("activeCharacterTurnOrder", activeCharacterTurnOrder);
        gameData.put("currentSelectingPlayer", currentSelectingPlayer == null ? null : currentSelectingPlayer.getName());
        gameData.put("currentPhase", currentPhase == null ? null : currentPhase.name());
        gameData.put("crownHolderIndex", crownHolderIndex);
        gameData.put("bellTowerActive", bellTowerActive);
        gameData.put("finalRoundNumber", finalRoundNumber);
        gameData.put("isGameEnding", isGameEnding);
        gameData.put("debugMode", GameUtils.isDebugMode());
        gameData.put("firstToCompleteCityPlayer", firstToCompleteCityPlayer == null ? null : firstToCompleteCityPlayer.getName());
        gameData.put("discardedCharacterCardFaceDown", discardedCharacterCardFaceDown == null ? null : serializeCharacterCard(discardedCharacterCardFaceDown));

        JSONArray faceUpArray = new JSONArray();
        // Serialize face-up discarded character cards
        for (CharacterCard card : discardedCharacterCardsFaceUp) {
            faceUpArray.add(serializeCharacterCard(card));
        }
        gameData.put("discardedCharacterCardsFaceUp", faceUpArray);

        JSONArray playersArray = new JSONArray();
        // Serialize each player’s game state
        for (Player player : players) {
            JSONObject playerJson = new JSONObject();
            playerJson.put("name", player.getName());
            playerJson.put("gold", player.getGold());
            playerJson.put("hand", serializeDistrictList(player.getHand()));
            playerJson.put("built", serializeDistrictList(getCityForPlayer(player)));

            CharacterCard chosen = player.getChosenCharacter();
            playerJson.put("chosenCharacter", chosen == null ? null : serializeCharacterCard(chosen));
            playerJson.put("isKilled", player.isKilled());
            playerJson.put("isStolen", player.isStolen());
            playerJson.put("turnCompleted", player.hasCompletedTurn());
            playerJson.put("maxBuildsPerTurn", player.getMaxBuildsPerTurn());
            playerJson.put("buildsThisTurn", player.getBuildsThisTurn());
            playerJson.put("robbedBy", player.getRobbedBy() == null ? null : player.getRobbedBy().getName());
            playerJson.put("usedLaboratory", player.hasUsedLaboratory());
            playerJson.put("usedSmithy", player.hasUsedSmithy());
            playerJson.put("usedMuseum", player.hasUsedMuseum());
            playerJson.put("museumStored", serializeDistrictList(player.getMuseumStoredCards()));

            // Save HumanPlayer-specific info
            if (player instanceof HumanPlayer) {
                playerJson.put("hasUsedMagicianAbility", ((HumanPlayer) player).usedMagicianThisTurn());
            }

            playersArray.add(playerJson);
        }
        gameData.put("players", playersArray);

        // Save district DistrictDeck
        gameData.put("DistrictDeck", serializeDistrictList(districtDeck.getCards()));

        JSONArray characterDeckArray = new JSONArray();
        // Save the full remaining character DistrictDeck
        for (CharacterCard card : characterDeck.getCards()) {
            characterDeckArray.add(serializeCharacterCard(card));
        }
        gameData.put("characterDeck", characterDeckArray);

        // Write to file
        try (FileWriter file = new FileWriter(filename)) {
            file.write(gameData.toJSONString());
            // Handle issues writing the save file
        } catch (IOException e) {
            System.out.println("Failed to save game: " + e.getMessage());
        }
    }

    /**
     * Loads a previously saved game from the specified file.
     *
     * @param filename The name of the file to load the game from
     */
    public void loadGame(String filename) {
        JSONParser parser = new JSONParser();
        JSONObject gameData;

        // Read and parse the file into a JSONObject
        try (FileReader reader = new FileReader(filename)) {
            gameData = (JSONObject) parser.parse(reader);
            // Handle I/O problems while loading the save file
        } catch (IOException e) {
            throw new RuntimeException("Failed to load game: " + e.getMessage(), e);
            // Stop loading if JSON parsing failed
        } catch (ParseException e) {
            System.out.println("Error parsing save file: " + e.getMessage());
            return;
        }

        // Load basic game state fields
        currentRound = ((Long) gameData.get("currentRound")).intValue();
        currentPlayerIndex = ((Long) gameData.get("currentPlayerIndex")).intValue();
        activeCharacterTurnOrder = ((Long) gameData.get("activeCharacterTurnOrder")).intValue();

        String phaseName = (String) gameData.get("currentPhase");
        currentPhase = (phaseName == null) ? null : Phase.valueOf(phaseName);

        crownHolderIndex = ((Long) gameData.get("crownHolderIndex")).intValue();
        bellTowerActive = (Boolean) gameData.get("bellTowerActive");
        finalRoundNumber = ((Long) gameData.get("finalRoundNumber")).intValue();
        isGameEnding = (Boolean) gameData.get("isGameEnding");

        GameUtils.applyDebugModeOverride((Boolean) gameData.get("debugMode"));

        // Reconstruct players from JSON array
        players = new ArrayList<>();
        JSONArray playersArray = (JSONArray) gameData.get("players");

        // Load each saved player from the file
        for (Object obj : playersArray) {
            JSONObject playerData = (JSONObject) obj;
            String name = (String) playerData.get("name");

            // Choose correct player type based on name
            Player player = name.equalsIgnoreCase("Player 1")
                    ? new HumanPlayer(name)
                    : new AIPlayer(name);

            player.setGame(this);
            player.setGold(((Long) playerData.get("gold")).intValue());

            // Load hand and museum cards
            List<DistrictCard> hand = deserializeDistrictList((JSONArray) playerData.get("hand"));
            player.getHand().addAll(hand);

            List<DistrictCard> museum = deserializeDistrictList((JSONArray) playerData.get("museumStored"));
            player.getMuseumStoredCards().addAll(museum);

            JSONObject chosenObj = (JSONObject) playerData.get("chosenCharacter");
            // Restore player’s chosen character from save data
            if (chosenObj != null) {
                player.setChosenCharacter(deserializeCharacterCard(chosenObj));
            }

            // Load status fields
            player.setKilled((Boolean) playerData.get("isKilled"));
            player.setStolen((Boolean) playerData.get("isStolen"));
            player.setTurnCompleted((Boolean) playerData.get("turnCompleted"));
            player.setMaxBuildsPerTurn(((Long) playerData.get("maxBuildsPerTurn")).intValue());
            player.setBuildsThisTurn(((Long) playerData.get("buildsThisTurn")).intValue());
            player.setUsedLaboratory((Boolean) playerData.get("usedLaboratory"));
            player.setUsedSmithy((Boolean) playerData.get("usedSmithy"));
            player.setUsedMuseum((Boolean) playerData.get("usedMuseum"));

            // Restore graveyard confirmation input if the player is human
            if (player instanceof HumanPlayer) {
                ((HumanPlayer) player).setUsedMagicianThisTurn((Boolean) playerData.get("hasUsedMagicianAbility"));
            }

            players.add(player);
        }

        // Initialize board now that players exist
        board = new Board(players);

        // Add built districts to each player's city
        for (int i = 0; i < playersArray.size(); i++) {
            JSONObject playerData = (JSONObject) playersArray.get(i);
            Player player = players.get(i);

            List<DistrictCard> builtDistricts = deserializeDistrictList((JSONArray) playerData.get("built"));
            // Add each previously built district back to player’s city
            for (DistrictCard dc : builtDistricts) {
                addDistrictToCity(player, dc);
            }
        }

        // Reconnect references like robbedBy, firstToCompleteCityPlayer, etc.
        Map<String, Player> nameToPlayer = new HashMap<>();
        // Build a quick lookup for players by name (used for robbedBy reference)
        for (Player p : players) {
            nameToPlayer.put(p.getName(), p);
        }

        // Now resolve saved references like robbedBy using the lookup
        for (int i = 0; i < playersArray.size(); i++) {
            JSONObject playerData = (JSONObject) playersArray.get(i);
            String robbedByName = (String) playerData.get("robbedBy");
            // If someone robbed this player, reconnect that relationship
            if (robbedByName != null) {
                players.get(i).setRobbedBy(nameToPlayer.get(robbedByName));
            }
        }

        String firstToFinishName = (String) gameData.get("firstToCompleteCityPlayer");
        firstToCompleteCityPlayer = (firstToFinishName == null) ? null : nameToPlayer.get(firstToFinishName);

        String selectingName = (String) gameData.get("currentSelectingPlayer");
        currentSelectingPlayer = (selectingName == null) ? null : nameToPlayer.get(selectingName);

        // Restore discarded character cards
        JSONObject discardedDown = (JSONObject) gameData.get("discardedCharacterCardFaceDown");
        discardedCharacterCardFaceDown = (discardedDown == null) ? null : deserializeCharacterCard(discardedDown);

        discardedCharacterCardsFaceUp.clear();
        JSONArray discardedUpArray = (JSONArray) gameData.get("discardedCharacterCardsFaceUp");
        // Load back the face-up discarded character cards
        for (Object obj : discardedUpArray) {
            JSONObject cardData = (JSONObject) obj;
            discardedCharacterCardsFaceUp.add(deserializeCharacterCard(cardData));
        }

        // Restore DistrictDeck (district cards)
        JSONArray deckArray = (JSONArray) gameData.get("DistrictDeck");
        List<DistrictCard> restoredDeck = deserializeDistrictList(deckArray);
        if (districtDeck == null) {
            districtDeck = new DistrictDeck(new ArrayList<>());
        }
        districtDeck.setCards(restoredDeck);

        // Restore character DistrictDeck
        JSONArray charArray = (JSONArray) gameData.get("characterDeck");
        List<CharacterCard> restoredChars = new ArrayList<>();
        // Turn each saved character card back into a real object
        for (Object obj : charArray) {
            JSONObject charObj = (JSONObject) obj;
            restoredChars.add(deserializeCharacterCard(charObj));
        }
        if (characterDeck == null) {
            characterDeck = new CharacterDeck(new ArrayList<>());
        }

        characterDeck.setCards(restoredChars);

        // Create new input handler instance
        inputHandler = new InputHandler(this);

        // Final output for debug/logging
        System.out.println("Game loaded successfully from " + filename);
        System.out.println("Resumed at round: " + currentRound + ", current phase: " + currentPhase);
        System.out.println("Current player index: " + currentPlayerIndex + ", name: " + getCurrentPlayer().getName());
    }

    /**
     * Converts a list of DistrictCard objects into a JSONArray for saving.
     *
     * @param cards The list of district cards
     * @return A JSONArray representation of the list
     */
    @SuppressWarnings("unchecked")
    private JSONArray serializeDistrictList(List<DistrictCard> cards) {
        JSONArray array = new JSONArray();
        // Convert a list of district cards to JSON
        for (DistrictCard card : cards) {
            JSONObject obj = new JSONObject();
            obj.put("name", card.getName());
            obj.put("color", card.getColor());
            obj.put("cost", card.getCost());
            obj.put("ability", card.getAbilityDescription());
            obj.put("builtRound", card.getBuiltRound());
            array.add(obj);
        }
        return array;
    }

    /**
     * Converts a CharacterCard into a JSONObject for saving.
     *
     * @param card The character card to serialize
     * @return A JSONObject representing the card
     */
    @SuppressWarnings("unchecked")
    private JSONObject serializeCharacterCard(CharacterCard card) {
        if (card == null) return null;
        JSONObject obj = new JSONObject();
        obj.put("name", card.getName());
        obj.put("number", card.getTurnOrder());
        obj.put("ability", card.getAbilityDescription());
        return obj;
    }

    /**
     * Reconstructs a list of DistrictCard objects from a JSONArray.
     *
     * @param array The JSONArray to read from
     * @return A list of DistrictCard objects
     */
    private List<DistrictCard> deserializeDistrictList(JSONArray array) {
        List<DistrictCard> list = new ArrayList<>();
        // Convert each JSON object back into a DistrictCard
        for (Object obj : array) {
            JSONObject card = (JSONObject) obj;
            String name = (String) card.get("name");
            String color = (String) card.get("color");
            long cost = (Long) card.get("cost");
            String ability = (String) card.get("ability");
            int builtRound = card.get("builtRound") != null ? ((Long) card.get("builtRound")).intValue() : -1;

            DistrictCard dc = new DistrictCard(name, color, (int) cost, 1, ability);
            dc.setBuiltRound(builtRound);
            list.add(dc);
        }
        return list;
    }

    /**
     * Reconstructs a CharacterCard object from a JSONObject.
     *
     * @param obj The JSON object
     * @return A CharacterCard object
     */
    private CharacterCard deserializeCharacterCard(JSONObject obj) {
        String name = (String) obj.get("name");
        long number = (Long) obj.get("number");
        String ability = (String) obj.get("ability");
        return new CharacterCard(name, (int) number, ability);
    }

    /**
     * Returns the current phase of the game.
     *
     * @return the current game phase (SELECTION, TURN, or END)
     */
    public Phase getCurrentPhase() {
        return currentPhase;
    }

    /**
     * Runs one or more phases until the game reaches END.
     * Safe to call after loading or starting a game.
     */
    public void runPhase() {
        while (currentPhase != Phase.END) {
            switch (currentPhase) {
                case SELECTION:
                    System.out.println("=== ROUND " + currentRound + " ===");
                    GameUtils.delay(1000);
                    System.out.println();

                    for (Player player : players) {
                        player.setTurnCompleted(false);
                    }

                    startCharacterSelectionPhase();
                    currentPhase = Phase.TURN;
                    break;

                case TURN:
                    startTurnPhase();

                    if (isGameEnding) {
                        finalRoundNumber = currentRound;
                        currentPhase = Phase.END;
                    } else {
                        currentRound++;
                        currentPhase = Phase.SELECTION;
                    }
                    break;
            }
        }

        // Handle the end of the game
        startEndPhase();
    }

    /**
     * Represents the current phase of the game.
     */
    public enum Phase {
        SELECTION,
        TURN,
        END
    }
}