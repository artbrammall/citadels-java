package citadels.model;

import citadels.app.App;
import citadels.behaviour.PlayerBehaviour;
import citadels.core.Game;
import citadels.core.GameUtils;
import citadels.state.DistrictDeck;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Abstract base class representing a player in the Citadels game.
 * Handles shared functionality for both human and AI players.
 */
public abstract class Player implements PlayerBehaviour {
    protected static Random random = new Random();          // Creates a new random seed
    protected String name; // Player's name
    protected Game game; // Game instance reference
    protected int gold; // Current amount of gold held by the player
    protected List<DistrictCard> hand; // Player's hand of district cards
    protected List<DistrictCard> museumStoredCards = new ArrayList<>(); // Cards stored in Museum
    protected int buildsThisTurn = 0; // Number of districts built this turn
    protected boolean usedLaboratoryThisTurn = false; // Whether Laboratory was used this turn
    protected boolean usedSmithyThisTurn = false; // Whether Smithy was used this turn
    protected boolean usedMuseumThisTurn = false; // Whether Museum was used this turn
    private CharacterCard chosenCharacter; // Character chosen this round
    private boolean turnCompleted = false; // Whether the player has completed their turn
    private boolean killed = false; // If the player was assassinated this round
    private boolean stolen = false; // If the player was robbed this round
    private Player robbedBy = null; // The player who robbed this player, if any
    private int maxBuildsPerTurn = 1; // Max districts that can be built this turn

    /**
     * Constructs a new player with the given name.
     *
     * @param name Player's name
     */
    public Player(String name) {
        this.name = name;
        this.gold = 0; // Set to 2 in game setup
        this.hand = new ArrayList<>();
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
     * Returns a new Scanner that reads from the current App.input stream.
     * This ensures test input is correctly picked up instead of System.in.
     *
     * @return a fresh Scanner bound to App.input
     */
    public Scanner getScanner() {
        return new Scanner(App.input);
    }

    /**
     * Links this player to the current Game instance.
     *
     * @param game The game object to assign
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * @return The player's name
     */
    public String getName() {
        return name;
    }

    /**
     * Abstract method: choose a character from the given options.
     *
     * @param available List of available character cards
     * @return Selected CharacterCard
     */
    public abstract CharacterCard chooseCharacter(List<CharacterCard> available);

    /**
     * @return The character card chosen by the player
     */
    public CharacterCard getChosenCharacter() {
        return chosenCharacter;
    }

    /**
     * Sets the character card chosen by this player for the current round.
     *
     * @param character The character card chosen
     */
    public void setChosenCharacter(CharacterCard character) {
        this.chosenCharacter = character;
    }

    /**
     * Marks the player's turn as completed or not.
     *
     * @param completed True if the turn is done, false otherwise
     */
    public void setTurnCompleted(boolean completed) {
        this.turnCompleted = completed;
    }

    /**
     * @return True if the player has completed their turn
     */
    public boolean hasCompletedTurn() {
        return turnCompleted;
    }

    /**
     * Starts this player's turn. Resets usage flags and shows their character.
     */
    public void takeTurn() {
        setUsedLaboratory(false);
        setUsedSmithy(false);
        setUsedMuseum(false);
        System.out.println(name + " is the " + getChosenCharacter().getName());

        doTurnActions();
    }

    /**
     * Runs the full turn sequence: assassinate check, robbery transfer,
     * ability use, income choice, and district building.
     */
    protected void doTurnActions() {
        buildsThisTurn = 0;

        // If the player was killed this round, check if Hospital lets them survive
        if (this.isKilled()) {
            // Hospital negates death effect for this round
            if (hasHospital()) {
                System.out.println(getName() + " was assassinated but has the Hospital — they may take an action only.");
                chooseIncome(game.getDeck());
                setTurnCompleted(true);
                return;
                // If there's no Hospital, skip the player's turn entirely
            } else {
                System.out.println(getName() + " loses their turn because they were assassinated.");
                GameUtils.delay(1000);
                System.out.println();
                setTurnCompleted(true);
            }
            return;
        }

        // If the player was robbed, transfer gold to the thief
        if (stolen && robbedBy != null) {
            int stolenGold = getGold();
            // Only transfer gold if there's something to steal
            if (stolenGold > 0) {
                robbedBy.setGold(robbedBy.getGold() + stolenGold);
                setGold(0);
                System.out.println(getName() + " was robbed of " + stolenGold + " gold by " + robbedBy.getName());
            }
            stolen = false;
            robbedBy = null;
        }

        useAbility();
        chooseIncome(game.getDeck());
        buildDistrict();
    }

    /**
     * @return Whether the player was killed this round.
     */
    public boolean isKilled() {
        return killed;
    }

    /**
     * Marks this player as having been killed by the Assassin.
     *
     * @param killed Whether the player was killed
     */
    public void setKilled(boolean killed) {
        this.killed = killed;
    }

    /**
     * @return True if the player was robbed this round
     */
    public boolean isStolen() {
        return stolen;
    }

    /**
     * Marks this player as having been robbed by the Thief.
     *
     * @param stolen Whether the player was robbed
     */
    public void setStolen(boolean stolen) {
        this.stolen = stolen;
    }

    /**
     * @return The player who robbed this player
     */
    public Player getRobbedBy() {
        return robbedBy;
    }

    /**
     * Sets the player who robbed this player.
     *
     * @param player The robber
     */
    public void setRobbedBy(Player player) {
        this.robbedBy = player;
    }

    /**
     * @return The amount of gold the player currently has
     */
    public int getGold() {
        return gold;
    }

    /**
     * Sets how much gold this player currently has.
     *
     * @param gold The new amount of gold
     */
    public void setGold(int gold) {
        this.gold = gold;
    }

    /**
     * Applies the Poor House effect: if player has 0 gold and owns Poor House, they gain 1 gold.
     */
    public void checkPoorHouseBonus() {
        // Poor House gives a bonus only if you have 0 gold
        if (hasPoorHouse() && getGold() == 0) {
            setGold(1);
            System.out.println(getName() + " had no gold. Poor House effect: received 1 gold from the bank.");
            GameUtils.delay(1000);
            System.out.println();
        }
    }

    /**
     * Adds a district card to the player’s hand.
     *
     * @param card The district card to add
     */
    public void addCardToHand(DistrictCard card) {
        hand.add(card);
    }

    /**
     * @return The list of district cards in the player's hand
     */
    public List<DistrictCard> getHand() {
        return hand;
    }

    /**
     * @return The number of cards in the player’s hand
     */
    public int getHandSize() {
        return hand.size();
    }

    /**
     * Swaps this player's hand with the target player.
     *
     * @param target The other player to swap with
     */
    public void swapHandWith(Player target) {
        List<DistrictCard> temp = new ArrayList<>(this.hand);
        this.hand.clear();
        this.hand.addAll(target.hand);
        target.hand.clear();
        target.hand.addAll(temp);
        System.out.println(this.name + " swapped hands with " + target.getName());
    }

    /**
     * Discards selected cards and draws the same number of new cards.
     *
     * @param toDiscard cards to discard and replace
     */
    public void redrawCards(List<DistrictCard> toDiscard) {
        DistrictDeck districtDeck = game.getDeck();
        hand.removeAll(toDiscard);

        int drawCount = toDiscard.size();
        for (int i = 0; i < drawCount; i++) {
            DistrictCard drawnCard = districtDeck.draw();
            if (drawnCard != null) {
                hand.add(drawnCard);
            } else {
                System.out.println("DistrictDeck is empty. Could not draw enough cards.");
                break;
            }
        }

        // Only recycle discarded cards after attempting to draw
        districtDeck.addCardsToBottom(toDiscard);

        System.out.println(getName() + " discarded " + toDiscard.size() + " cards and drew " + Math.min(drawCount, hand.size()) + " new cards.");
    }

    /**
     * @return how many districts this player has built this turn
     */
    public int getBuildsThisTurn() {
        return this.buildsThisTurn;
    }

    /**
     * Sets how many districts have been built this turn
     *
     * @param builds the number to set
     */
    public void setBuildsThisTurn(int builds) {
        this.buildsThisTurn = builds;
    }

    /**
     * @return the maximum number of districts this player can build per turn
     */
    public int getMaxBuildsPerTurn() {
        return maxBuildsPerTurn;
    }

    /**
     * Sets the maximum districts the player can build in one turn
     *
     * @param maxBuilds max allowed builds
     */
    public void setMaxBuildsPerTurn(int maxBuilds) {
        this.maxBuildsPerTurn = maxBuilds;
    }

    /**
     * Checks if the player is allowed to build the given district.
     * This checks build limits, gold, duplicates, and Quarry rules.
     *
     * @param card the district card player wants to build
     * @return null if buildable, otherwise a reason message why it's not
     */
    public String canBuildDistrict(DistrictCard card) {
        // Can't build more if you've already hit your limit
        if (buildsThisTurn >= maxBuildsPerTurn) {
            return "You have reached your maximum number of builds this turn (" + maxBuildsPerTurn + ").";
        }

        int costToBuild = getBuildCost(card);
        // Can't build if you can't afford the district
        if (gold < costToBuild) {
            return "Not enough gold. You need " + costToBuild + " gold.";
        }

        List<DistrictCard> built = game.getCityForPlayer(this);
        int thisCardCount = 0;
        boolean hasOtherDuplicate = false;

        // Count how many of this district name already exist in the city
        for (DistrictCard builtCard : built) {
            if (builtCard.getName().equals(card.getName())) {
                thisCardCount++;
            } else {
                int count = 0;
                // See if any other card is also duplicated (used for Haunted City rules)
                for (DistrictCard other : built) {
                    if (other.getName().equals(builtCard.getName())) {
                        count++;
                    }
                }
                if (count > 1) {
                    hasOtherDuplicate = true;
                }
            }
        }

        // You can’t build a duplicate unless you have a Quarry
        if (thisCardCount > 0 && !hasQuarry()) {
            return "You already have that district built. You need a Quarry to build a duplicate.";
        }

        // Prevents a second duplicate if one already exists
        if (thisCardCount == 1 && hasOtherDuplicate) {
            return "Quarry only allows one duplicated district in your city.";
        }

        // Can't build more than 2 of the same district, even with Quarry
        if (thisCardCount > 1) {
            return "You already have two of that district. You cannot build more, even with Quarry.";
        }

        return null;
    }

    /**
     * Calculates the cost to build a district, applying modifiers.
     *
     * @param card the district to build
     * @return cost to build after modifiers
     */
    public int getBuildCost(DistrictCard card) {
        int baseCost = card.getCost();

        // Factory reduces cost of other purple districts
        if (hasFactory()
                && card.getColor().equalsIgnoreCase("purple")
                && !card.getName().equalsIgnoreCase("Factory")) {
            return Math.max(0, baseCost - 1);
        }

        return baseCost;
    }

    /**
     * Triggers any on-build effects for certain districts.
     *
     * @param built the district just built
     */
    public void onDistrictBuilt(DistrictCard built) {
        // Prompt to activate Bell Tower when it's built
        if (built.getName().equalsIgnoreCase("Bell Tower")) {
            game.activateBellTower();
        }

        // AI automatically draws a card if it builds Lighthouse
        if (built.getName().equalsIgnoreCase("Lighthouse") && this instanceof AIPlayer) {
            DistrictDeck districtDeck = game.getDeck();
            List<DistrictCard> deckCards = districtDeck.peekAll();

            // Don’t draw if the DistrictDeck is empty
            if (deckCards.isEmpty()) {
                System.out.println(getName() + " built Lighthouse, but the DistrictDeck is empty.");
                return;
            }

            int maxCost = deckCards.stream()
                    .mapToInt(DistrictCard::getCost)
                    .max()
                    .orElse(0);

            List<DistrictCard> maxCostCards = deckCards.stream()
                    .filter(c -> c.getCost() == maxCost)
                    .collect(Collectors.toList());

            List<DistrictCard> purpleCards = maxCostCards.stream()
                    .filter(c -> "purple".equalsIgnoreCase(c.getColor()))
                    .collect(Collectors.toList());

            List<DistrictCard> candidates = purpleCards.isEmpty() ? maxCostCards : purpleCards;

            DistrictCard chosen = candidates.get(random.nextInt(candidates.size()));
            addCardToHand(chosen);
            districtDeck.removeCard(chosen);
            districtDeck.shuffle();
        }
    }

    /**
     * Abstract method to build a district. Must be implemented by subclasses.
     */
    public abstract void buildDistrict();

    /**
     * Checks if the player has already built a district by name.
     *
     * @param name the name of the district
     * @return true if built
     */
    public boolean hasBuiltDistrict(String name) {
        return game.getCityForPlayer(this).stream()
                .anyMatch(d -> d.getName().equalsIgnoreCase(name));
    }

    /**
     * Checks if a city qualifies for color bonus (all 5 district types).
     * Haunted City may be used to cover 1 missing type unless built last round.
     *
     * @param built            list of districts in city
     * @param finalRoundNumber round number of last round
     * @return true if all types present (or 4 + Haunted City)
     */
    public boolean hasAllDistrictTypes(List<DistrictCard> built, int finalRoundNumber) {
        boolean hasRed = false, hasYellow = false, hasGreen = false, hasBlue = false, hasPurple = false;
        DistrictCard hauntedCity = null;

        // Track how many unique district colors the player has
        for (DistrictCard d : built) {
            String color = d.getColor().toLowerCase();
            switch (color) {
                case "red":
                    hasRed = true;
                    break;
                case "yellow":
                    hasYellow = true;
                    break;
                case "green":
                    hasGreen = true;
                    break;
                case "blue":
                    hasBlue = true;
                    break;
                case "purple":
                    // Special logic: Haunted City can be counted as any missing color
                    if (d.getName().equalsIgnoreCase("Haunted City") &&
                            d.getBuiltRound() != finalRoundNumber) {
                        hauntedCity = d;
                    } else {
                        hasPurple = true;
                    }
                    break;
            }
        }

        int missing = 0;
        if (!hasRed) missing++;
        if (!hasYellow) missing++;
        if (!hasGreen) missing++;
        if (!hasBlue) missing++;
        if (!hasPurple) missing++;

        // Count Haunted City as the missing color if it completes the set
        if (missing == 1 && hauntedCity != null) {
            return true;
        }

        return missing == 0;
    }

    /**
     * Returns true if the player has built Library.
     */
    public boolean hasLibrary() {
        return hasBuiltDistrict("Library");
    }

    /**
     * Returns true if the player has built School Of Magic.
     */
    public boolean hasSchoolOfMagic() {
        return hasBuiltDistrict("School Of Magic");
    }

    /**
     * Returns true if the player has built Great Wall.
     */
    public boolean hasGreatWall() {
        return hasBuiltDistrict("Great Wall");
    }

    /**
     * Returns true if the player has built Observatory.
     */
    public boolean hasObservatory() {
        return hasBuiltDistrict("Observatory");
    }

    /**
     * Returns true if the player has built Poor House.
     */
    public boolean hasPoorHouse() {
        return hasBuiltDistrict("Poor House");
    }

    /**
     * Returns true if the player has built Park.
     */
    public boolean hasPark() {
        return hasBuiltDistrict("Park");
    }

    /**
     * Returns true if the player has built Factory.
     */
    public boolean hasFactory() {
        return hasBuiltDistrict("Factory");
    }

    /**
     * Returns true if the player has built Throne Room.
     */
    public boolean hasThroneRoom() {
        return hasBuiltDistrict("Throne Room");
    }

    /**
     * Returns true if the player has built Hospital.
     */
    public boolean hasHospital() {
        return hasBuiltDistrict("Hospital");
    }

    /**
     * Returns true if the player has built Quarry.
     */
    public boolean hasQuarry() {
        return hasBuiltDistrict("Quarry");
    }

    /**
     * Returns true if the player has built Laboratory.
     */
    public boolean hasLaboratory() {
        return hasBuiltDistrict("Laboratory");
    }

    /**
     * Returns true if the player has built Graveyard.
     */
    public boolean hasGraveyard() {
        return hasBuiltDistrict("Graveyard");
    }

    /**
     * Returns true if the player has built Museum.
     */
    public boolean hasMuseum() {
        return hasBuiltDistrict("Museum");
    }

    /**
     * Returns true if the player has built Armory.
     */
    public boolean hasArmory() {
        return hasBuiltDistrict("Armory");
    }

    /**
     * Returns true if the player has built Smithy.
     */
    public boolean hasSmithy() {
        return hasBuiltDistrict("Smithy");
    }

    /**
     * Triggers the Park effect if player has no cards and has Park built.
     * Draws 2 cards and shows them if player is human.
     */
    public void checkParkBonus() {
        // Park lets you draw 2 cards if you start your turn with an empty hand
        if (hasPark() && hand.isEmpty()) {
            List<DistrictCard> drawnCards = new ArrayList<>();

            // Draw two cards for the Park effect
            for (int i = 0; i < 2; i++) {
                DistrictCard drawn = game.getDeck().draw();
                // Add drawn card to a temporary list if it exists
                if (drawn != null) {
                    addCardToHand(drawn);
                    drawnCards.add(drawn);
                }
            }

            // Only show output if cards were actually drawn
            if (!drawnCards.isEmpty()) {
                // Display cards to the player only if they're human
                if (this instanceof HumanPlayer) {
                    System.out.println("You had no cards. Park effect: you draw " + drawnCards.size() + " card(s):");
                    for (DistrictCard card : drawnCards) {
                        System.out.println("- " + card.getName() + " [" + card.getColor() + card.getCost() + "]");
                    }
                } else {
                    System.out.println(getName() + " had no cards. Park effect triggered.");
                }
            }
        }
    }

    /**
     * @return true if Laboratory was used this turn
     */
    public boolean hasUsedLaboratory() {
        return usedLaboratoryThisTurn;
    }

    /**
     * Marks whether Laboratory has been used this turn
     */
    public void setUsedLaboratory(boolean used) {
        usedLaboratoryThisTurn = used;
    }

    /**
     * @return true if Smithy was used this turn
     */
    public boolean hasUsedSmithy() {
        return usedSmithyThisTurn;
    }

    /**
     * Marks whether Smithy has been used this turn
     */
    public void setUsedSmithy(boolean used) {
        usedSmithyThisTurn = used;
    }

    /**
     * Uses the Smithy effect: pay 2 gold to draw 3 cards.
     * Marks Smithy as used.
     */
    public void useSmithy() {
        setGold(getGold() - 2);
        // Draw 3 cards for Smithy effect
        for (int i = 0; i < 3; i++) {
            DistrictCard drawn = game.getDeck().draw();
            // Make sure we don’t try to add null cards
            if (drawn != null) {
                addCardToHand(drawn);
            }
        }
        usedSmithyThisTurn = true;
    }

    /**
     * Uses the Laboratory: discard a card from hand to gain 1 gold.
     *
     * @param cardIndex index of the card to discard
     */
    public void useLaboratory(int cardIndex) {
        DistrictCard discardedCard = getHand().remove(cardIndex);
        setGold(getGold() + 1);
        usedLaboratoryThisTurn = true;
    }

    /**
     * @return list of cards stored in the Museum
     */
    public List<DistrictCard> getMuseumStoredCards() {
        return museumStoredCards;
    }

    /**
     * Stores a card in the Museum and marks Museum as used this turn.
     *
     * @param card card to store
     */
    public void storeInMuseum(DistrictCard card) {
        museumStoredCards.add(card);
        usedMuseumThisTurn = true;
    }

    /**
     * @return true if Museum was used this turn
     */
    public boolean hasUsedMuseum() {
        return usedMuseumThisTurn;
    }

    /**
     * Sets Museum usage for this turn
     */
    public void setUsedMuseum(boolean used) {
        usedMuseumThisTurn = used;
    }

    /**
     * Triggers character-based abilities for gold income and card draw.
     * Handles Architect and bonuses for colored districts.
     */
    public void useAbility() {
        if (getChosenCharacter() != null && getChosenCharacter().getTurnOrder() == 7) { // Architect
            setMaxBuildsPerTurn(3);
            // Architect draws 2 cards at the start of their turn
            for (int i = 0; i < 2; i++) {
                DistrictCard drawn = game.getDeck().draw();
                // Add drawn cards if they’re valid
                if (drawn != null) {
                    addCardToHand(drawn);
                }
            }
            System.out.println(getName() + " drew 2 cards due to Architect ability.");
        } else {
            setMaxBuildsPerTurn(1);
        }

        if (getChosenCharacter() == null) return;

        int charNum = getChosenCharacter().getTurnOrder();

        // Merchant always gets +1 gold
        if (charNum == 6) {
            gold += 1;
            System.out.println(getName() + " earned 1 extra gold for Merchant ability.");
        }

        String bonusColor = null;

        // Give bonus gold based on district colors that match your character
        switch (charNum) {
            case 4:
                bonusColor = "yellow";
                break; // King
            case 5:
                bonusColor = "blue";
                break;   // Bishop
            case 6:
                bonusColor = "green";
                break;  // Merchant
            case 8:
                bonusColor = "red";
                break;    // Warlord
            default:
                return;
        }

        int count = 0;
        List<DistrictCard> built = game.getCityForPlayer(this);
        // Count how many districts match the bonus color
        for (DistrictCard district : built) {
            if (district.getColor().equalsIgnoreCase(bonusColor)) {
                count++;
            }
        }
        // School of Magic always counts as the bonus color
        if (hasSchoolOfMagic()) {
            count++; // School of Magic can count as any color
        }

        // Only apply gold bonus if there’s something to reward
        if (count > 0) {
            gold += count;
            System.out.println(getName() + " earned " + count + " extra gold for " + getChosenCharacter().getName() +
                    "'s ability (" + bonusColor + " districts).");
        }
    }

    /**
     * Default income method — overridden by subclasses if needed.
     */
    public void chooseIncome(DistrictDeck districtDeck) {
        // Default or empty implementation
    }

    /**
     * Determines whether a district can be destroyed based on city size, Keep, and gold.
     *
     * @param target        player whose district is being targeted
     * @param districtIndex index of the district to destroy
     * @return true if it can be destroyed
     */
    public boolean canDestroyDistrict(Player target, int districtIndex) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);

        int limit = game.isBellTowerActive() ? 7 : 8;
        if (builtDistricts.size() >= limit) return false;
        if (districtIndex < 0 || districtIndex >= builtDistricts.size()) return false;

        DistrictCard toDestroy = builtDistricts.get(districtIndex);
        if (toDestroy.getName().equalsIgnoreCase("Keep")) return false;

        int destroyCost = target.getDestroyCost(toDestroy);
        return this.gold >= destroyCost;
    }

    /**
     * Abstract method for destroying a district.
     */
    public abstract void destroyDistrict(Player target, int districtIndex, boolean usedArmory);

    /**
     * Calculates the cost to destroy a district, taking Great Wall into account.
     *
     * @param district the district to destroy
     * @return gold cost to destroy it
     */
    public int getDestroyCost(DistrictCard district) {
        int baseCost = district.getCost() - 1;
        // Great Wall reduces build cost by 1 for all cards except itself
        if (this.hasGreatWall() && !district.getName().equalsIgnoreCase("Great Wall")) {
            baseCost += 1;
        }
        return baseCost;
    }
}