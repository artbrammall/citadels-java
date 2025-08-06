package citadels.model;

import citadels.core.GameUtils;
import citadels.state.DistrictDeck;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a computer-controlled player in the Citadels game.
 * Makes decisions based on basic heuristics such as gold, hand size,
 * and built district costs.
 */
public class AIPlayer extends Player {
    protected static Random random = new Random();          // Creates a new random seed

    /**
     * Constructs an AI player with the given name.
     *
     * @param name Name of the AI player
     */
    public AIPlayer(String name) {
        super(name);
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
     * Handles the AI player's full turn.
     * Also prints debug info if debug mode is active.
     */
    @Override
    public void takeTurn() {
        // Show AI hand and other info if we're in debug mode
        if (GameUtils.isDebugMode()) {
            System.out.println();
            System.out.println("Debug: " + GameUtils.getDebugStatusString(this));
        }
        super.takeTurn(); // still uses base logic
    }

    /**
     * Executes the full sequence of the AI's actions during their turn.
     */
    @Override
    public void doTurnActions() {
        super.doTurnActions();
    }

    /**
     * Executes the AI's character-specific ability and purple card abilities.
     */
    @Override
    public void useAbility() {
        if (getChosenCharacter() == null) return;

        super.useAbility();

        // Skip ability logic if no character was chosen (this shouldn't happen normally)
        if (getChosenCharacter() == null) return;

        int charNum = getChosenCharacter().getTurnOrder();

        // Run ability logic based on character number (1 = Assassin, 2 = Thief, etc.)
        switch (charNum) {
            case 1:
                handleAssassin();
                break;
            case 2:
                handleThief();
                break;
            case 3:
                handleMagician();
                break;
            case 8:
                handleWarlord();
                break;
        }

        handlePurpleDistrictAbilities();
    }

    /**
     * AI chooses a character to assassinate and tells the game to make them as dead.
     */
    private void handleAssassin() {
        int targetChar = chooseAssassinTarget();
        game.markCharacterKilled(targetChar);
    }

    /**
     * AI chooses a character to steal from and applies the effect, ignoring dead characters and the assassin.
     */
    private void handleThief() {
        int targetChar = chooseThiefTarget();
        Player robbedPlayer = game.getPlayerByCharacterNumber(targetChar);

        // Only steal if valid: can't target Assassin or dead players
        if (robbedPlayer != null && targetChar != 1 && !robbedPlayer.isKilled()) {
            robbedPlayer.setStolen(true);
            robbedPlayer.setRobbedBy(this);
        }
    }

    /**
     * AI decides whether to swap hands or discard for the Magician ability,
     * then performs the chosen option.
     */
    private void handleMagician() {
        String choice = chooseSwapOrDiscard();
        // Abort if AI couldn't decide on swap or discard
        if (choice == null) return;

        // Chose to swap hands instead of discarding
        if ("swap".equals(choice)) {
            // Choose player with biggest hand, break ties with biggest city
            List<Player> playersWithLargestHands = new ArrayList<>();
            int maxHandSize = -1;

            // Find player with biggest hand size (ignore self)
            for (Player p : game.getPlayers()) {
                if (p == this) continue;
                int size = p.getHand().size();
                // New max hand size found — reset list
                if (size > maxHandSize) {
                    maxHandSize = size;
                    playersWithLargestHands.clear();
                    playersWithLargestHands.add(p);
                    // Tie — add to the list of potential swap targets
                } else if (size == maxHandSize) {
                    playersWithLargestHands.add(p);
                }
            }

            List<Player> bestSwapTargets = new ArrayList<>();
            int maxDistricts = -1;
            // Go through all players who tied for biggest hand to decide swap target
            for (Player p : playersWithLargestHands) {
                // Check how many districts this player has to help break hand-size ties
                int districts = game.getCityForPlayer(p).size();
                if (districts > maxDistricts) {
                    maxDistricts = districts;
                    bestSwapTargets.clear();
                    bestSwapTargets.add(p);
                } else if (districts == maxDistricts) {
                    // Tie on district count — keep this player in the swap list too
                    bestSwapTargets.add(p);
                }
            }

            Player swapTarget = bestSwapTargets.get(random.nextInt(bestSwapTargets.size()));
            // Perform the actual hand swap with the chosen player
            swapHandWith(swapTarget);
        } else if ("discard".equals(choice)) {
            redrawCards(new ArrayList<>(hand));
        }
    }

    /**
     * AI logic for the Warlord: decide if to destroy. Then decide who to destroy from and what district.
     */
    private void handleWarlord() {
        if (game.getPlayers().stream().noneMatch(p -> p != this && hasDestroyableDistricts(p))) return;
        if (!shouldWarlordDestroyThisTurn()) return;

        Player warlordTargetPlayer = chooseBestPlayerToDestroy();
        if (warlordTargetPlayer == null) return;

        int districtIndexToDestroy = chooseBestDistrictToDestroyFromIndexes(warlordTargetPlayer, getDestroyableDistrictIndexes(warlordTargetPlayer));
        if (districtIndexToDestroy == -1) return;

        destroyDistrict(warlordTargetPlayer, districtIndexToDestroy, false);
    }

    /**
     * Checks and uses purple district abilities that can activate during the AI's turn.
     */
    private void handlePurpleDistrictAbilities() {
        if (hasArmory() && canUseArmoryNow()) useArmoryAbility();
        if (hasLaboratory() && !hasUsedLaboratory() && !hand.isEmpty()) useLaboratoryAbility();
        if (hasSmithy() && !hasUsedSmithy() && gold >= 2) useSmithyAbility();
        if (hasMuseum() && !hasUsedMuseum() && !hand.isEmpty()) useMuseumAbility();
    }

    /**
     * AI logic for using the Museum ability — either stores a duplicate or best scoring card to try and maximise final points.
     */
    private void useMuseumAbility() {
        if (!hasMuseum() || hasUsedMuseum() || hand.isEmpty()) return;

        List<DistrictCard> city = game.getCityForPlayer(this);
        boolean hasQuarry = hasQuarry();
        boolean quarryInHand = hand.stream().anyMatch(c -> c.getName().equalsIgnoreCase("Quarry"));

        // Group all cards in hand by name to find duplicates
        Map<String, List<DistrictCard>> cardsByName = new HashMap<>();
        for (DistrictCard card : hand) {
            cardsByName.computeIfAbsent(card.getName(), k -> new ArrayList<>()).add(card);
        }

        List<List<DistrictCard>> duplicateCardGroups = cardsByName.values().stream()
                .filter(list -> list.size() > 1)
                .collect(Collectors.toList());

        // Use Museum only if you don’t have Quarry and found duplicate cards
        if (!hasQuarry && !quarryInHand && !duplicateCardGroups.isEmpty()) {
            List<DistrictCard> selectedDuplicateGroup = duplicateCardGroups.get(random.nextInt(duplicateCardGroups.size()));
            DistrictCard duplicate = selectedDuplicateGroup.get(0);
            storeInMuseum(duplicate);
            hand.remove(duplicate);
            System.out.println(getName() + " used Museum to store a card.");
            return;
        }

        // Score all potential Museum cards to decide which to use
        Map<DistrictCard, Integer> museumCardScores = new HashMap<>();
        for (DistrictCard card : hand) {
            int score = 0;

            if (!"purple".equalsIgnoreCase(card.getColor())) score += 3;

            // Figure out how valuable this card is for Smithy draw priority
            int cost = card.getCost();
            // Don’t bother drawing if your city is basically full already
            if (city.size() < 6) {
                switch (cost) {
                    case 1:
                        score += 6;
                        break;
                    case 2:
                        score += 5;
                        break;
                    case 3:
                        score += 4;
                        break;
                    case 4:
                        score += 3;
                        break;
                    case 5:
                        score += 2;
                        break;
                    case 6:
                        score += 1;
                        break;
                }
                // Higher cost cards more favoured
            } else {
                score += cost;
            }

            museumCardScores.put(card, score);
        }

        int highestScore = Collections.max(museumCardScores.values());
        List<DistrictCard> topScoringCards = museumCardScores.entrySet().stream()
                .filter(e -> e.getValue() == highestScore)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DistrictCard chosen = topScoringCards.get(random.nextInt(topScoringCards.size()));
        storeInMuseum(chosen);
        hand.remove(chosen);
        System.out.println(getName() + " used Museum to store a card.");
    }

    /**
     * AI logic for using the Laboratory — decides what to discard for 1 gold.
     */
    private void useLaboratoryAbility() {
        if (!hasLaboratory() || hasUsedLaboratory() || hand.isEmpty()) return;

        List<DistrictCard> nonPurple = hand.stream()
                .filter(card -> !"purple".equalsIgnoreCase(card.getColor()))
                .collect(Collectors.toList());

        List<DistrictCard> purple = hand.stream()
                .filter(card -> "purple".equalsIgnoreCase(card.getColor()))
                .collect(Collectors.toList());

        List<DistrictCard> buildable = hand.stream()
                .filter(card -> canBuildDistrict(card) == null)
                .collect(Collectors.toList());

        if (buildable.size() == 1) return;

        DistrictCard toDiscard = null;

        // Apply bonus scoring only if AI has 4 or more cards
        if (hand.size() >= 4) {
            List<DistrictCard> candidates = nonPurple.isEmpty() ? purple : nonPurple;
            int minCost = candidates.stream().mapToInt(DistrictCard::getCost).min().orElse(Integer.MAX_VALUE);
            List<DistrictCard> tied = candidates.stream()
                    .filter(c -> c.getCost() == minCost)
                    .collect(Collectors.toList());
            // Pick one of the tied lowest-value cards at random
            toDiscard = tied.get(random.nextInt(tied.size()));
        } else if (buildable.isEmpty()) {
            List<DistrictCard> candidates = nonPurple.isEmpty() ? purple : nonPurple;
            int minCost = candidates.stream().mapToInt(DistrictCard::getCost).min().orElse(Integer.MAX_VALUE);
            List<DistrictCard> tied = candidates.stream()
                    .filter(c -> c.getCost() == minCost)
                    .collect(Collectors.toList());
            toDiscard = tied.get(random.nextInt(tied.size()));
        }

        // Only discard if you actually chose a card to use with Laboratory
        if (toDiscard != null) {
            int index = hand.indexOf(toDiscard);
            useLaboratory(index);
            System.out.println(getName() + " used Laboratory to discard " + toDiscard.getName() + " and gain 1 gold.");
        }
    }

    /**
     * AI logic for using the Smithy — spends 2 gold to draw 3 cards if worthwhile.
     */
    private void useSmithyAbility() {
        if (!hasSmithy() || hasUsedSmithy() || getGold() < 2) return;

        if ((2 * hand.size()) > gold) return;

        useSmithy(); // already deducts gold and draws cards
        System.out.println(getName() + " used Smithy: paid 2 gold and drew 3 cards.");
    }

    /**
     * Determines whether the player can use the Armory's ability this turn.
     * Armory can only be used if the player has less than the city size limit,
     * and there is at least one enemy district that can be destroyed.
     *
     * @return true if the Armory can be used, false otherwise
     */
    public boolean canUseArmoryNow() {
        int myCitySize = game.getCityForPlayer(this).size();
        int limit = game.isBellTowerActive() ? 6 : 7; // Bell Tower lowers city size limit
        if (myCitySize >= limit) return false;

        // Look through all players to check who has districts we can destroy with Armory
        for (Player p : game.getPlayers()) {
            if (p == this) continue;
            if (!getArmoryDestroyableDistrictIndexes(p).isEmpty()) return true;
        }

        return false;
    }

    /**
     * Activates the Armory's ability. This removes the Armory from your city
     * and destroys a destroyable district in another player's city for free.
     */
    public void useArmoryAbility() {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(this);
        DistrictCard armory = null;

        // Search our city for the Armory so we can activate it
        for (DistrictCard d : builtDistricts) {
            // Found the Armory card — now we can use its special ability
            if (d.getName().equalsIgnoreCase("Armory")) {
                armory = d;
                break;
            }
        }

        if (armory == null) return;

        builtDistricts.remove(armory);
        System.out.println(getName() + " destroyed their Armory to trigger its effect.");

        Player target = chooseBestPlayerToDestroyViaArmory();
        if (target == null) return;

        int targetIndex = chooseBestDistrictToDestroyFromIndexes(target, getArmoryDestroyableDistrictIndexes(target));
        if (targetIndex == -1) return;

        destroyDistrict(target, targetIndex, true); // Free destruction
    }

    /**
     * Gets the list of indexes of districts in the target's city that can be destroyed using the Armory.
     *
     * @param target the player whose districts to check
     * @return list of destroyable district indexes
     */
    public List<Integer> getArmoryDestroyableDistrictIndexes(Player target) {
        List<Integer> indexes = new ArrayList<>();
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);

        // Go through all of this player’s districts to see if any are destroyable
        for (int i = 0; i < builtDistricts.size(); i++) {
            if (canArmoryDestroyDistrict(target, i)) {
                indexes.add(i);
            }
        }

        return indexes;
    }

    /**
     * Destroys a district in the target player's city.
     * May be free (e.g., from Armory) or require gold (Warlord).
     *
     * @param target        the player whose district is being destroyed
     * @param districtIndex the index of the district to destroy
     * @param usedArmory    whether the destruction is free
     */
    @Override
    public void destroyDistrict(Player target, int districtIndex, boolean usedArmory) {
        // Only pay gold to destroy if this isn’t a free Armory destruction
        if (!usedArmory) {
            CharacterCard targetCharacter = target.getChosenCharacter();
            // If the Bishop is alive, we’re not allowed to destroy their district
            if (targetCharacter != null && targetCharacter.getTurnOrder() == 5 && !target.isKilled()) {
                System.out.println(getName() + " attempted to destroy a district in " + target.getName() +
                        "'s city, but they were protected by the Bishop.");
                return;
            }
        }

        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);
        DistrictCard districtToDestroy = builtDistricts.get(districtIndex);
        int destroyCost = target.getDestroyCost(districtToDestroy);

        // Pay the gold cost to destroy the district unless it's free
        if (!usedArmory) {
            this.gold -= destroyCost;
        }

        builtDistricts.remove(districtIndex);

        // If Museum is destroyed, all stored cards are lost
        if (districtToDestroy.getName().equalsIgnoreCase("Museum")) {
            target.getMuseumStoredCards().clear();
        }

        // If Bell Tower is destroyed, cancel its effect on endgame
        if (districtToDestroy.getName().equalsIgnoreCase("Bell Tower")) {
            game.deactivateBellTower();
        }

        // Skip Graveyard logic if the destruction was free (Armory)
        if (usedArmory) {
            System.out.println(getName() + " destroyed " + districtToDestroy.getName() +
                    " in " + target.getName() + "'s city for free using Armory.");

            // Otherwise, allow other players to use Graveyard to recover it
        } else {
            System.out.println(getName() + " destroyed " + districtToDestroy.getName() +
                    " in " + target.getName() + "'s city, paying " + destroyCost + " gold.");
        }

        // Let each player except the destroyer offer to use Graveyard
        if (!usedArmory) {
            for (Player player : game.getPlayers()) {
                if (player == this || !player.hasGraveyard() || player.getGold() < 1) continue;

                // Only ask human players if they want to recover the district
                if (player instanceof HumanPlayer) {
                    // Keep prompting until player gives valid input
                    System.out.println(player.getName() + ", You may pay 1 gold to recover the destroyed district (" + districtToDestroy.getName() + ") [yes/no]:");
                    while (true) {
                        System.out.print("> ");
                        // Recover the destroyed card for 1 gold if they say yes
                        String input = getScanner().nextLine().trim().toLowerCase();
                        if (input.equals("yes")) {
                            player.setGold(player.getGold() - 1);
                            player.addCardToHand(districtToDestroy);
                            System.out.println("Graveyard effect: " + player.getName() +
                                    " paid 1 gold and recovered " + districtToDestroy.getName() + ".");
                            // Stop asking anyone else once someone uses Graveyard
                            return;
                        } else if (input.equals("no")) {
                            // Exit loop if they said no
                            break;
                        } else {
                            System.out.println("Please type 'yes' or 'no'.");
                        }
                    }
                } else {
                    player.setGold(player.getGold() - 1);
                    player.addCardToHand(districtToDestroy);
                    System.out.println("Graveyard effect: " + player.getName() +
                            " recovered " + districtToDestroy.getName() + " using Graveyard.");
                    return;
                }
            }
        }
    }

    /**
     * Checks whether the target player has any destroyable districts.
     *
     * @param target the player to check
     * @return true if any of the districts can be destroyed
     */
    public boolean hasDestroyableDistricts(Player target) {
        // Check every built district to see if any can be destroyed
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);
        // Check for any destroyable districts by standard rules
        for (int i = 0; i < builtDistricts.size(); i++) {
            if (this.canDestroyDistrict(target, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the list of indexes of destroyable districts for a target player.
     *
     * @param target the player to check
     * @return list of destroyable district indexes
     */
    public List<Integer> getDestroyableDistrictIndexes(Player target) {
        List<Integer> indexes = new ArrayList<>();
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);

        for (int i = 0; i < builtDistricts.size(); i++) {
            if (this.canDestroyDistrict(target, i)) {
                indexes.add(i);
            }
        }

        return indexes;
    }

    /**
     * Checks whether a list of district indexes contains any purple-colored districts.
     *
     * @param p       the player to check
     * @param indexes list of indexes to check
     * @return true if at least one district is purple
     */
    private boolean hasDestroyablePurpleDistrictFromList(Player p, List<Integer> indexes) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(p);
        // Loop through all destroyable districts this player has
        for (int index : indexes) {
            DistrictCard card = builtDistricts.get(index);
            if ("purple".equalsIgnoreCase(card.getColor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a district at the given index can be destroyed by the Armory.
     * Cannot destroy 'Keep', and city must have less than limit (7 or 8).
     *
     * @param target        the target player
     * @param districtIndex the index of the district
     * @return true if destroyable by Armory
     */
    public boolean canArmoryDestroyDistrict(Player target, int districtIndex) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);
        if (districtIndex < 0 || districtIndex >= builtDistricts.size()) return false;

        DistrictCard districtToDestroy = builtDistricts.get(districtIndex);

        if (districtToDestroy.getName().equalsIgnoreCase("Keep")) return false;

        int limit = game.isBellTowerActive() ? 7 : 8;
        return builtDistricts.size() < limit;
    }

    /**
     * Determines whether the Warlord should attempt to destroy a district this turn.
     * Uses basic strategy depending on round and affordability.
     *
     * @return true if the Warlord should destroy a district
     */
    public boolean shouldWarlordDestroyThisTurn() {
        int round = game.getCurrentRound();
        int thresholdGold = 2;

        boolean canDestroyValuableDistrict = false;

        // Look through all players to evaluate best destruction targets
        for (Player p : game.getPlayers()) {
            if (p == this) continue;

            List<Integer> indexes = getDestroyableDistrictIndexes(p);
            List<DistrictCard> builtDistricts = game.getCityForPlayer(p);

            // Loop through each destroyable district in that player’s city
            for (int idx : indexes) {
                DistrictCard card = builtDistricts.get(idx);
                int cost = card.getCost();
                String color = card.getColor();

                int destroyCost = p.getDestroyCost(card);
                boolean wouldLeaveLowGold = (this.gold - destroyCost) < thresholdGold;

                // Early rounds: prefer cheaper targets and preserve our gold
                if (round <= 2) {
                    if (wouldLeaveLowGold) continue;
                    // Avoid expensive non-purple cards early unless we’re rich
                    if (!"purple".equalsIgnoreCase(color) && cost > 2) {
                        canDestroyValuableDistrict = true;
                    }
                    // Later in the game, be more aggressive with scoring logic
                } else {
                    canDestroyValuableDistrict = true;
                }
            }
        }

        // Avoid purple cards early in the game
        if (round <= 2) {
            if (!canDestroyValuableDistrict) return false;
            if (random.nextInt(3) == 0) return false;
        }

        // Only avoid expensive cards if we're still in the early-mid game
        if (round <= 4) {
            return random.nextInt(10) != 0;
        }

        return true;
    }

    /**
     * Chooses the best player to target for destruction based on a scoring heuristic.
     * Uses different strategies depending on whether it's the final round.
     *
     * @return the chosen target player
     */
    public Player chooseBestPlayerToDestroy() {
        // Final round: try to stop whoever has the most points
        if (isFinalRound()) {
            int mostBuilt = game.getPlayers().stream()
                    .filter(p -> p != this && hasDestroyableDistricts(p))
                    .mapToInt(p -> game.getCityForPlayer(p).size())
                    .max().orElse(-1);

            List<Player> tied = game.getPlayers().stream()
                    .filter(p -> p != this && hasDestroyableDistricts(p))
                    .filter(p -> game.getCityForPlayer(p).size() == mostBuilt)
                    .collect(Collectors.toList());

            // Break ties by picking the one with fewest cards in hand
            if (!tied.isEmpty()) {
                return tied.get(random.nextInt(tied.size()));
            }
        }

        Map<Player, Integer> playerDestructionScores = new HashMap<>();
        int highestScore = Integer.MIN_VALUE;

        // Evaluate destruction score for every player
        for (Player p : game.getPlayers()) {
            if (p == this) continue;

            List<Integer> destroyable = getDestroyableDistrictIndexes(p);
            if (destroyable.isEmpty()) continue;

            int score = 0;
            score += Math.min(destroyable.size(), 7); // Up to +7
            if (hasDestroyablePurpleDistrictFromList(p, destroyable)) score += 3;
            score += p.getGold();

            playerDestructionScores.put(p, score);
            highestScore = Math.max(highestScore, score);
        }

        // Get all players who tied for highest destruction score
        List<Player> highestScoringTargets = new ArrayList<>();
        for (Map.Entry<Player, Integer> entry : playerDestructionScores.entrySet()) {
            if (entry.getValue() == highestScore) {
                highestScoringTargets.add(entry.getKey());
            }
        }

        return highestScoringTargets.get(random.nextInt(highestScoringTargets.size()));
    }

    /**
     * Chooses the best target player for the Armory's destruction effect.
     * Prioritises players with the largest cities (if final round),
     * or scores based on number of destroyable districts and presence of purple cards.
     *
     * @return the best target player, or null if none found
     */
    public Player chooseBestPlayerToDestroyViaArmory() {
        // If it's the final round, prioritise players with the largest city
        if (isFinalRound()) {
            int mostBuilt = game.getPlayers().stream()
                    .filter(p -> p != this && !getArmoryDestroyableDistrictIndexes(p).isEmpty())
                    .mapToInt(p -> game.getCityForPlayer(p).size())
                    .max().orElse(-1);

            List<Player> tied = game.getPlayers().stream()
                    .filter(p -> p != this && !getArmoryDestroyableDistrictIndexes(p).isEmpty())
                    .filter(p -> game.getCityForPlayer(p).size() == mostBuilt)
                    .collect(Collectors.toList());

            // Pick one if there’s a tie for destruction targets
            if (!tied.isEmpty()) {
                return tied.get(random.nextInt(tied.size()));
            }
        }

        // Otherwise, score each potential target based on what can be destroyed
        Map<Player, Integer> playerDestructionScores = new HashMap<>();
        int highestScore = Integer.MIN_VALUE;

        // Evaluate destruction score normally (non-final round)
        for (Player p : game.getPlayers()) {
            if (p == this) continue;

            List<Integer> destroyable = getArmoryDestroyableDistrictIndexes(p);
            if (destroyable.isEmpty()) continue;

            int score = 0;

            // +1 to +7 based on how many destroyable districts they have
            score += Math.min(destroyable.size(), 7);

            // +3 if any of them is a purple district
            if (hasDestroyablePurpleDistrictFromList(p, destroyable)) {
                score += 3;
            }

            playerDestructionScores.put(p, score);
            highestScore = Math.max(highestScore, score);
        }

        // From all highest scoring players, randomly pick one
        List<Player> highestScoringTargets = new ArrayList<>();
        // Again, get all players tied for highest score
        for (Map.Entry<Player, Integer> entry : playerDestructionScores.entrySet()) {
            if (entry.getValue() == highestScore) {
                highestScoringTargets.add(entry.getKey());
            }
        }

        return highestScoringTargets.get(random.nextInt(highestScoringTargets.size()));
    }

    /**
     * Chooses the best district to destroy from a given list of indexes.
     * Purple cards are prioritised, then cost.
     *
     * @param target  the player whose city is being checked
     * @param indexes the indexes of districts that can be destroyed
     * @return the index of the best district to destroy, or -1 if none
     */
    public int chooseBestDistrictToDestroyFromIndexes(Player target, List<Integer> indexes) {
        if (indexes.isEmpty()) return -1;

        Map<Integer, Integer> scoreMap = new HashMap<>();
        int highestScore = Integer.MIN_VALUE;

        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);

        // Check each destroyable district in that player’s city
        for (int idx : indexes) {
            DistrictCard card = builtDistricts.get(idx);
            int score = card.getCost();

            // Purple districts are more valuable to destroy
            if ("purple".equalsIgnoreCase(card.getColor())) {
                score += 3;
            }

            scoreMap.put(idx, score);
            highestScore = Math.max(highestScore, score);
        }

        // From all equally scored districts, pick one randomly
        List<Integer> bestIndexes = new ArrayList<>();
        // Gather all the indexes that had the highest score
        for (Map.Entry<Integer, Integer> entry : scoreMap.entrySet()) {
            if (entry.getValue() == highestScore) {
                bestIndexes.add(entry.getKey());
            }
        }

        // Return a valid index, or -1 if nothing was found
        return bestIndexes.isEmpty() ? -1 :
                bestIndexes.get(random.nextInt(bestIndexes.size()));
    }

    /**
     * Checks if it's the final round of the game.
     *
     * @return true if the current round is the final round
     */
    public boolean isFinalRound() {
        return game.getCurrentRound() == game.getFinalRoundNumber();
    }

    /**
     * AI logic for choosing the best character card to play this round.
     * Evaluates based on hand size, gold, city status and known strengths of characters.
     *
     * @param available list of characters to choose from
     * @return the best character card to play
     */
    @Override
    public CharacterCard chooseCharacter(List<CharacterCard> available) {
        Map<CharacterCard, Integer> characterScores = new HashMap<>();

        // Initialise scores to zero
        for (CharacterCard card : available) {
            characterScores.put(card, 0);
        }

        // Score based on various heuristics
        applyHandSizeBonus(characterScores, available);
        applyGoldBonus(characterScores, available);
        applyDistrictBonus(characterScores, available);
        applyDistrictTypeBonus(characterScores, available);

        // Small flat bonus for generally useful characters
        for (CharacterCard card : available) {
            int score = characterScores.get(card);
            if (card.getTurnOrder() == 1) score += 1; // Assassin
            if (card.getTurnOrder() == 2) score += 1; // Thief
            if (card.getTurnOrder() == 4) score += 1; // King
            characterScores.put(card, score);
        }

        // Choose the character with the highest score, break ties by higher number
        CharacterCard bestCharacter = null;
        int bestScore = Integer.MIN_VALUE;
        int tieBreakerByCharacterNumber = Integer.MIN_VALUE;

        // Score each character to find the best one to choose
        for (CharacterCard card : available) {
            int score = characterScores.get(card);
            int number = card.getTurnOrder();

            // Prefer the higher score, or break ties with character number
            if (score > bestScore || (score == bestScore && number > tieBreakerByCharacterNumber)) {
                bestScore = score;
                tieBreakerByCharacterNumber = number;
                bestCharacter = card;
            }
        }

        System.out.println(this.name + " chose a character.");
        GameUtils.waitForTCommand(getScanner());
        return bestCharacter;
    }

    /**
     * Updates character choice scores based on how many cards the player has in hand
     * and whether those cards are bad (duplicates or low cost).
     */
    private void applyHandSizeBonus(Map<CharacterCard, Integer> scores, List<CharacterCard> available) {
        int handSize = this.hand.size();
        int topHandSize = getLargestOpponentHandSize();
        boolean hasDuplicate = hasDuplicateCardNames(this.hand);
        double avgCost = calculateAverageCardCost(this.hand);

        // Add bonuses based on how big our hand is for certain characters
        for (CharacterCard card : available) {
            int number = card.getTurnOrder();
            int bonus = 0;

            // Magician or Architect helps when your hand is small
            if (number == 3 || number == 7) {
                switch (handSize) {
                    case 0:
                        bonus += 3;
                        break;
                    case 1:
                        bonus += 2;
                        break;
                    case 2:
                        bonus += 1;
                        break;
                }
            }

            // Architect and Warlord get more value with more cards
            if (number == 7 || number == 8) {
                switch (handSize) {
                    case 4:
                        bonus += 1;
                        break;
                    case 5:
                        bonus += 2;
                        break;
                    default:
                        if (handSize >= 6) {
                            bonus += 3;
                        }
                        break;
                }
            }

            // Special bonuses just for Magician based on largest hand in game
            if (number == 3) {
                if (topHandSize >= 6) {
                    bonus += 3;
                } else if (topHandSize == 5) {
                    bonus += 2;
                } else if (topHandSize == 4) {
                    bonus += 1;
                }

                // Extra bonus if your hand has duplicates or cheap junk
                if (hasDuplicate || avgCost < 2) {
                    bonus += 2;
                }
            }

            // Only record bonuses if they're actually non-zero
            if (bonus != 0) {
                scores.put(card, scores.get(card) + bonus);
            }
        }
    }

    /**
     * Updates character scores based on how much gold the player has.
     * Encourages characters that benefit from low or high gold states.
     */
    private void applyGoldBonus(Map<CharacterCard, Integer> scores, List<CharacterCard> available) {
        int gold = this.gold;

        // Add bonuses based on how much gold we have for certain characters
        for (CharacterCard card : available) {
            int number = card.getTurnOrder();
            int bonus = 0;

            // Thief and Merchant scale differently based on gold amount
            if (number == 2 || number == 6) {
                switch (gold) {
                    case 0:
                        bonus += 3;
                        break;
                    case 1:
                        bonus += 2;
                        break;
                    case 2:
                        bonus += 1;
                        break;
                }
            }

            // Gold-rich players boost priority of aggressive or economic characters
            if (number == 1 || number == 2 || number == 7 || number == 8) {
                if (gold >= 6) {
                    bonus += 3;
                } else if (gold == 5) {
                    bonus += 2;
                } else if (gold == 4) {
                    bonus += 1;
                }
            }

            // Store only useful bonus scores
            if (bonus != 0) {
                scores.put(card, scores.get(card) + bonus);
            }
        }
    }

    /**
     * Applies bonus weights to character scores based on the player's current district count
     * and relative position to other players in terms of city size.
     *
     * @param scores    map of characters and their current scores
     * @param available list of available character cards
     */
    private void applyDistrictBonus(Map<CharacterCard, Integer> scores, List<CharacterCard> available) {
        List<Player> players = game.getPlayers();
        int myDistricts = game.getCityForPlayer(this).size();

        int minDistricts = Integer.MAX_VALUE;
        int maxDistrictsExcludingSelf = Integer.MIN_VALUE;

        // Loop through all players to find min/max districts in play
        for (Player player : players) {
            int count = game.getCityForPlayer(player).size();
            if (count < minDistricts) minDistricts = count;
            // Don’t count yourself when looking for the biggest city
            if (player != this && count > maxDistrictsExcludingSelf) {
                maxDistrictsExcludingSelf = count;
            }
        }

        int lead = maxDistrictsExcludingSelf - myDistricts;

        // Add priority bonuses based on city sizes
        for (CharacterCard card : available) {
            int number = card.getTurnOrder();
            int bonus = 0;

            // Boost Thief or Warlord if behind on city size
            if (myDistricts == minDistricts && (number == 2 || number == 8)) {
                bonus += 2;
            }

            // Penalise Architect if nearly finished
            if (number == 7) {
                if (myDistricts == 7) bonus -= 2;
                else if (myDistricts == 6) bonus -= 1;
            }

            // Boost Bishop when nearly complete to protect from Warlord
            if (number == 5) {
                if (myDistricts == 7) bonus += 3;
                else if (myDistricts == 6) bonus += 2;
                else if (myDistricts == 5) bonus += 1;
            }

            // Boost destruction-focused characters if someone else is far ahead
            if ((number == 1 || number == 2 || number == 8) && lead >= 2) {
                if (lead >= 4) bonus += 3;
                else if (lead == 3) bonus += 2;
                else bonus += 1;
            }

            // Small bonus if nearing city end and playing Assassin or King
            if (myDistricts >= 6 && (number == 1 || number == 4)) {
                bonus += 2;
            }

            // Skip adding scores if they didn’t change anything
            if (bonus != 0) {
                scores.put(card, scores.get(card) + bonus);
            }
        }
    }

    /**
     * Applies bonus weights to character scores based on the types and values
     * of districts currently built by the player, as well as special cards like Graveyard or School of Magic.
     *
     * @param scores    map of characters and their current scores
     * @param available list of available character cards
     */
    private void applyDistrictTypeBonus(Map<CharacterCard, Integer> scores, List<CharacterCard> available) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(this);

        int yellowCount = 0, blueCount = 0, redCount = 0, greenCount = 0, totalCost = 0;

        // Loop through our built districts to calculate color counts and total cost
        for (DistrictCard card : builtDistricts) {
            // Track how many districts of each color we’ve built
            totalCost += card.getCost();
            switch (card.getColor()) {
                case "Yellow":
                    yellowCount++;
                    break;
                case "Blue":
                    blueCount++;
                    break;
                case "Red":
                    redCount++;
                    break;
                case "Green":
                    greenCount++;
                    break;
            }
        }

        double avgCost = calculateAverageCardCost(builtDistricts);
        boolean ownsGraveyard = this.hasGraveyard();
        boolean ownsSchoolOfMagic = this.hasSchoolOfMagic();

        // Check if any other player has already built the Graveyard
        boolean graveyardBuiltByOthers = false;
        for (Player player : game.getPlayers()) {
            if (player != this && player.hasGraveyard()) {
                graveyardBuiltByOthers = true;
                break;
            }
        }

        // Check if we already have Graveyard in our hand
        boolean graveyardInHand = false;
        for (DistrictCard card : this.hand) {
            if (card.getName().equalsIgnoreCase("Graveyard")) {
                graveyardInHand = true;
                break;
            }
        }

        // Score each character based on color bonuses and special rules
        for (CharacterCard card : available) {
            int number = card.getTurnOrder();
            int bonus = 0;

            // Add bonus gold/district logic for character 5 (Merchant)
            if (number == 4) bonus += yellowCount;
            if (number == 5) {
                bonus += blueCount;
                if (avgCost > 4) bonus += 2;
            }
            // Apply Warlord logic, including extra Graveyard synergy check
            if (number == 6) bonus += greenCount;
            if (number == 8) {
                bonus += redCount;
                // Encourage choosing Warlord if we’re likely to build Graveyard
                if (!graveyardBuiltByOthers && (graveyardInHand || ownsGraveyard)) {
                    bonus += 2;
                }
            }

            // School of Magic acts as all colors, boost all relevant characters
            if (ownsSchoolOfMagic && (number == 4 || number == 5 || number == 6 || number == 8)) {
                bonus += 2;
            }

            // Only record bonuses that actually matter
            if (bonus != 0) {
                scores.put(card, scores.get(card) + bonus);
            }
        }
    }

    /**
     * AI decision logic to choose a target character to assassinate based on known factors of gamestate.
     * Uses weighted random selection based on likelihood and round number.
     *
     * @return the character number to assassinate (2-8)
     */
    private int chooseAssassinTarget() {
        Map<Integer, Integer> weights = new HashMap<>();

        weights.put(2, 3);
        weights.put(3, 3);
        weights.put(4, 3);
        weights.put(5, 2);
        weights.put(6, 4);
        weights.put(7, 5);
        weights.put(8, 4);

        // In early rounds, avoid using powerful abilities too early
        int round = game.getCurrentRound();
        if (round < 4) {
            weights.put(3, weights.get(3) + 1);
            weights.put(4, weights.get(4) + 1);
        }
        // In late game, go all in — use strong abilities and characters
        if (round > 6) {
            weights.put(7, weights.get(7) + 1);
            weights.put(8, weights.get(8) + 1);
        }

        // See which characters are publicly discarded — helps guide strategy
        for (CharacterCard discarded : game.getDiscardedCharacterCardsFaceUp()) {
            weights.remove(discarded.getTurnOrder());
        }

        // Build a weighted pool of character numbers based on their score
        List<Integer> weightedCharacterPool = new ArrayList<>();
        // Add the same character number multiple times for better odds
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                weightedCharacterPool.add(entry.getKey());
            }
        }

        if (weightedCharacterPool.isEmpty()) return 2;

        Collections.shuffle(weightedCharacterPool);
        return weightedCharacterPool.get(random.nextInt(weightedCharacterPool.size()));
    }

    /**
     * AI decision logic to choose a target character to steal from.
     * Uses weighted random selection based on income potential and round.
     *
     * @return the character number to steal from (3-8)
     */
    private int chooseThiefTarget() {
        Map<Integer, Integer> weights = new HashMap<>();

        weights.put(3, 2);
        weights.put(4, 5);
        weights.put(5, 3);
        weights.put(6, 6);
        weights.put(7, 4);
        weights.put(8, 5);

        // Similar early-game logic for a different character picking strategy
        int round = game.getCurrentRound();
        if (round < 4) {
            weights.put(3, weights.get(3) + 1);
            weights.put(4, weights.get(4) + 1);
        }
        // In final rounds, weight toward characters with high impact
        if (round > 6) {
            weights.put(6, weights.get(6) + 1);
            weights.put(7, weights.get(7) + 1);
        }

        // Look at public discards to avoid picking those characters
        for (CharacterCard discarded : game.getDiscardedCharacterCardsFaceUp()) {
            weights.remove(discarded.getTurnOrder());
        }

        // Build a weighted list to randomly choose from, based on scores
        List<Integer> weightedCharacterPool = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : weights.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                weightedCharacterPool.add(entry.getKey());
            }
        }

        if (weightedCharacterPool.isEmpty()) return 3;

        Collections.shuffle(weightedCharacterPool);
        return weightedCharacterPool.get(random.nextInt(weightedCharacterPool.size()));
    }

    /**
     * AI logic to decide whether to swap hands or discard cards as the Magician.
     * Makes the decision based on hand size, hand quality, opponent hand sizes, etc.
     *
     * @return "swap", "discard", or null if neither action is possible
     */
    private String chooseSwapOrDiscard() {
        int handSize = this.hand.size();
        int maxOpponentHandSize = getLargestOpponentHandSize();

        // We have the biggest hand — swap is a bad pick
        if (maxOpponentHandSize < handSize) {
            if (handSize == 0) return null;
            return "discard";
        }

        if (handSize == 0) return null;

        int swapScore = 0;
        int discardScore = 0;

        double avgCost = calculateAverageCardCost(this.hand);
        boolean hasDuplicate = hasDuplicateCardNames(this.hand);
        int expensiveCards = countCardsCostingAtLeast();

        if (handSize <= 2) swapScore += 2;
        if (maxOpponentHandSize >= 5) swapScore += 2;
        if (hasDuplicate) swapScore += 1;

        if (avgCost > 3 && this.gold < 3) discardScore += 2;
        if (expensiveCards >= 2) discardScore += 1;
        if (avgCost < 3) discardScore += 1;

        if (swapScore > discardScore) return "swap";
        if (discardScore > swapScore) return "discard";

        return random.nextBoolean() ? "swap" : "discard";
    }

    /**
     * Calculates the average cost of a list of district cards.
     *
     * @param cards list of district cards
     * @return average cost, or 0 if list is empty
     */
    private double calculateAverageCardCost(List<DistrictCard> cards) {
        if (cards.isEmpty()) return 0;
        // Sum up all the costs of the given cards to calculate their average
        int total = 0;
        for (DistrictCard card : cards) {
            total += card.getCost();
        }
        return (double) total / cards.size();
    }

    /**
     * Checks if the given list of district cards contains any duplicates by name.
     *
     * @param cards list of district cards
     * @return true if any duplicate card names exist
     */
    private boolean hasDuplicateCardNames(List<DistrictCard> cards) {
        // Track seen names to check for any duplicates in the hand
        Set<String> seen = new HashSet<>();
        for (DistrictCard card : cards) {
            if (!seen.add(card.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Counts how many cards in hand cost 5 or more gold.
     *
     * @return number of expensive cards in hand
     */
    private int countCardsCostingAtLeast() {
        // Count how many cards cost 5 or more (for strategy bonuses)
        int count = 0;
        for (DistrictCard card : hand) {
            if (card.getCost() >= 5) {
                count++;
            }
        }
        return count;
    }

    /**
     * Gets the size of the largest hand from all opponents.
     *
     * @return size of the largest opponent hand
     */
    private int getLargestOpponentHandSize() {
        // Look for the largest hand size among other players
        int max = 0;
        for (Player p : game.getPlayers()) {
            if (p != this) {
                max = Math.max(max, p.getHand().size());
            }
        }
        return max;
    }

    /**
     * Chooses and builds one or more districts from hand based on score.
     * Repeats while there's a card worth building.
     */
    @Override
    public void buildDistrict() {
        // Keep trying to build until no more districts can be built
        while (hand.stream().anyMatch(card -> canBuildDistrict(card) == null)) {
            Map<DistrictCard, Integer> buildPriorityScores = new HashMap<>();

            for (DistrictCard card : hand) {
                if (canBuildDistrict(card) == null) {
                    buildPriorityScores.put(card, scoreDistrictToBuild(card));
                }
            }

            if (buildPriorityScores.isEmpty()) break;

            int bestScore = Collections.max(buildPriorityScores.values());

            List<DistrictCard> bestBuildOptions = buildPriorityScores.entrySet().stream()
                    .filter(entry -> entry.getValue() == bestScore)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            DistrictCard selectedCardToBuild = bestBuildOptions.get(random.nextInt(bestBuildOptions.size()));

            int buildCost = getBuildCost(selectedCardToBuild);
            int originalCost = selectedCardToBuild.getCost();

            gold -= buildCost;
            hand.remove(selectedCardToBuild);
            game.addDistrictToCity(this, selectedCardToBuild);
            buildsThisTurn++;
            selectedCardToBuild.setBuiltRound(game.getCurrentRound());

            System.out.println(getName() + " built " + selectedCardToBuild.getName() + " [" + selectedCardToBuild.getColor() + originalCost + "]");

            onDistrictBuilt(selectedCardToBuild);
        }
    }

    /**
     * Scores a district card for AI decision-making based on value and city context.
     *
     * @param card the district to evaluate
     * @return priority score
     */
    private int scoreDistrictToBuild(DistrictCard card) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(this);
        int score = card.getCost();

        // Don’t score purple districts with color-specific logic
        if (card.getColor().equalsIgnoreCase("Purple")) {
            score += 1;
        }

        // Late-game push: build cheap districts to finish faster
        if (builtDistricts.size() >= 6 && card.getCost() <= gold) {
            score += 4;
        }

        Set<String> builtColors = builtDistricts.stream()
                .map(DistrictCard::getColor)
                .filter(c -> !c.equalsIgnoreCase("Purple"))
                .collect(Collectors.toSet());

        String cardColor = card.getColor();

        // Penalize reusing colors we've already built (except purple)
        if (!cardColor.equalsIgnoreCase("Purple") && builtColors.contains(cardColor)) {
            score += 3;
        }

        // Reward building districts of a new color
        if (!builtColors.contains(cardColor)) {
            // Bonus points for nearing the diverse-color bonus
            int currentColorCount = builtColors.size();
            if (currentColorCount == 3) {
                score += 2;
            } else if (currentColorCount == 4) {
                score += 5;
            }
        }

        // Special scoring bonus for unique expensive purple cards
        String name = card.getName();
        if (name.equalsIgnoreCase("Dragon Gate") || name.equalsIgnoreCase("University")) {
            score += 3;
        }

        return score;
    }

    /**
     * Triggers effects when a district is built.
     * Specifically implements Lighthouse ability for AI.
     *
     * @param built the district that was just built
     */
    @Override
    public void onDistrictBuilt(DistrictCard built) {
        super.onDistrictBuilt(built);

        // Lighthouse triggers a build bonus — draw 1 card for free
        if (built.getName().equalsIgnoreCase("Lighthouse")) {
            DistrictDeck districtDeck = game.getDeck();
            List<DistrictCard> deckCards = districtDeck.peekAll();
            if (deckCards.isEmpty()) return;

            int index = random.nextInt(deckCards.size());
            DistrictCard selected = deckCards.get(index);
            addCardToHand(selected);
            System.out.println(getName() + " activated Lighthouse and added a card to their hand after peeking at the DistrictDeck.");
            districtDeck.removeCard(selected);
            districtDeck.shuffle();
        }
    }

    /**
     * Chooses whether to take 2 gold or draw cards, based on a score system.
     * Accounts for character ability, hand size, and special buildings.
     *
     * @param districtDeck the district card DistrictDeck to draw from
     */
    @Override
    public void chooseIncome(DistrictDeck districtDeck) {
        Map<String, Integer> scores = new HashMap<>();
        CharacterCard character = this.getChosenCharacter();

        scores.put("gold", 0);
        scores.put("cards", 0);

        scores.put("cards", scores.get("cards") + this.gold);
        scores.put("gold", scores.get("gold") + this.hand.size() * 2);

        // Apply passive gold bonus for some characters based on district colors
        if (character != null) {
            int number = character.getTurnOrder();
            // Architect and Warlord — reward them if we have matching district colors
            if (number == 7 || number == 8) {
                scores.put("gold", scores.get("gold") + 3);
            }

            // Thief and Merchant — give them gold-based priority bonuses
            if (number == 2 || number == 6) {
                scores.put("cards", scores.get("cards") + 3);
            }
        }

        // Library gives us better draw options — increase draw score
        if (this.hasLibrary()) {
            scores.put("cards", scores.get("cards") + 6);
        }

        int goldScore = scores.get("gold");
        int cardScore = scores.get("cards");

        // Decide whether to take gold or draw based on current scores
        boolean chooseGold;
        if (goldScore > cardScore) {
            chooseGold = true;
            // Draw is more valuable — pick it instead
        } else if (cardScore > goldScore) {
            chooseGold = false;
            // If scores are equal, flip a coin basically
        } else {
            chooseGold = random.nextBoolean();
        }

        // Execute gold choice — gain gold normally
        if (chooseGold) {
            setGold(getGold() + 2);
            System.out.println(getName() + " chose gold.");
            // Otherwise, draw cards
        } else {
            int numToDraw = hasObservatory() ? 3 : 2;
            // Draw the correct number of cards into a temp list
            List<DistrictCard> options = new ArrayList<>();
            for (int i = 0; i < numToDraw; i++) {
                DistrictCard card = districtDeck.draw();
                if (card != null) options.add(card);
            }

            // Edge case: somehow no cards were drawn
            if (options.isEmpty()) {
                setGold(getGold() + 2);
                System.out.println(getName() + " tried to draw cards, but the DistrictDeck was empty. Took gold instead.");
                return;
            }

            // Library lets us keep all drawn cards
            if (hasLibrary()) {
                for (DistrictCard card : options) {
                    addCardToHand(card);
                }
                // Confirm draw result with Library active
                System.out.println(getName() + " chose cards (kept all due to Library).");
            } else {
                DistrictCard selectedCard;
                // 2/3 chance to keep the better of the two cards
                if (options.size() == 2 && random.nextInt(3) < 2) {
                    // Keep the more expensive card if doing the "smart" draw
                    selectedCard = (options.get(0).getCost() >= options.get(1).getCost()) ? options.get(0) : options.get(1);
                } else {
                    int pickIndex = random.nextInt(options.size());
                    selectedCard = options.get(pickIndex);
                }

                addCardToHand(selectedCard);
                options.remove(selectedCard);
                districtDeck.addCardsToBottom(options);

                System.out.println(getName() + " chose cards.");
            }
        }

        GameUtils.delay(1000);
    }
}