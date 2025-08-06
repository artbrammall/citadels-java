package citadels.model;

import citadels.core.GameUtils;
import citadels.state.DistrictDeck;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a human-controlled player in the game.
 * Reads input from the console to decide what to do,
 * like choosing cards, using abilities, and building districts.
 */
public class HumanPlayer extends Player {
    private boolean hasUsedMagicianAbility = false; // tracks if magician ability was used this turn

    /**
     * Constructs a new HumanPlayer with the given name.
     *
     * @param name the player's name
     */
    public HumanPlayer(String name) {
        super(name);
    }

    /**
     * Prompts the human player to choose a character from the list.
     *
     * @param available list of available CharacterCards
     * @return the chosen CharacterCard
     */
    @Override
    public CharacterCard chooseCharacter(List<CharacterCard> available) {
        CharacterCard chosenCharacter = null;

        // Sort character cards by turn order
        List<CharacterCard> sorted = new ArrayList<>(available);
        sorted.sort((c1, c2) -> Integer.compare(c1.getTurnOrder(), c2.getTurnOrder()));

        // Display debug info and choices
        System.out.print(GameUtils.getDebugStatusString(this));
        GameUtils.delay(1000);
        System.out.println();

        System.out.println("Choose your character. Available characters:");
        // Display each available character with their info so the player can pick
        for (CharacterCard card : sorted) {
            System.out.println(card);
        }

        // Loop until a valid choice is made
        while (chosenCharacter == null) {
            System.out.print("Enter the number of your choice: ");
            String input = getScanner().nextLine();
            // Display each available character with their info so the player can pick
            try {
                int choice = Integer.parseInt(input);

                for (CharacterCard card : available) {
                    // Match the chosen number with one of the available character cards
                    if (card.getTurnOrder() == choice) {
                        chosenCharacter = card;
                        System.out.println("You chose the " + chosenCharacter.getName());
                        GameUtils.delay(1000);
                        System.out.println();
                        break;
                    }
                }

                // If no character was matched with the input, show an error
                if (chosenCharacter == null) {
                    System.out.println("Invalid choice number. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Enter a number.");
            }
        }

        return chosenCharacter;
    }

    /**
     * Runs the full turn for this player.
     * Resets ability flags, shows debug info, and executes shared turn actions.
     */
    @Override
    public void takeTurn() {
        setUsedMagicianThisTurn(false);
        setUsedLaboratory(false);
        setUsedSmithy(false);
        setUsedMuseum(false);

        System.out.println(name + " is the " + getChosenCharacter().getName());

        System.out.println("Your turn.");
        GameUtils.delay(1000);
        System.out.println();

        System.out.print(GameUtils.getDebugStatusString(this));
        GameUtils.delay(1000);
        System.out.println();

        printAbilityReminders();
        doTurnActions();
    }

    /**
     * Prompts the human player to use their character's ability (e.g. Assassin or Thief).
     * Handles targeting logic and applies effects directly.
     */
    @Override
    public void useAbility() {
        if (getChosenCharacter() == null) return;

        super.useAbility();

        int charNum = getChosenCharacter().getTurnOrder();

        // Assassin: player chooses a character to kill
        if (charNum == 1) {
            // Assassin ability
            System.out.println("Who do you want to kill? Choose a character from 2-8:");
            int killChoice = -1;
            // Keep asking until the kill choice is a valid character number (can't kill self)
            while (killChoice < 2 || killChoice > 8) {
                System.out.print("> ");
                String input = getScanner().nextLine();
                // Try reading and parsing the player's input for assassination
                try {
                    killChoice = Integer.parseInt(input);
                    // Reject invalid character numbers for killing
                    if (killChoice < 2 || killChoice > 8) {
                        System.out.println("Invalid choice. Choose a character number from 2 to 8.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a number from 2 to 8.");
                }
            }

            System.out.println("You chose to kill the " + GameUtils.getCharacterNameByNumber(killChoice));
            System.out.println();
            game.markCharacterKilled(killChoice);

            // Thief: player chooses a character to rob
        } else if (charNum == 2) {
            // Thief ability
            System.out.println("Who do you want to rob? Choose a character from 3-8:");
            int robChoice = -1;
            // Repeat until a valid rob target is chosen (can't rob Assassin or yourself)
            while (robChoice < 3 || robChoice > 8) {
                System.out.print("> ");
                String input = getScanner().nextLine();
                try {
                    robChoice = Integer.parseInt(input);
                    if (robChoice < 3 || robChoice > 8) {
                        System.out.println("Invalid choice. Choose a character number from 3 to 8.");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Please enter a number from 3 to 8.");
                }
            }

            System.out.println("You chose to rob the " + GameUtils.getCharacterNameByNumber(robChoice));
            System.out.println();

            Player robbedPlayer = game.getPlayerByCharacterNumber(robChoice);
            // Set who the thief is stealing from, if it's a valid player
            if (robbedPlayer != null &&
                    robbedPlayer.getChosenCharacter().getTurnOrder() != 1 &&
                    !robbedPlayer.isKilled()) {
                robbedPlayer.setStolen(true);
                robbedPlayer.setRobbedBy(this);
            }
        }
    }

    /**
     * Prompts the player to choose income for the turn: either 2 gold or drawing cards.
     * Handles logic for Observatory and Library effects.
     *
     * @param districtDeck the district card DistrictDeck
     */
    @Override
    public void chooseIncome(DistrictDeck districtDeck) {
        System.out.println("Collect 2 gold or draw cards [gold/cards]:");

        // Loop until the player enters a valid choice for income method
        while (true) {
            System.out.print("> ");
            String input = getScanner().nextLine().trim().toLowerCase();

            // Player chooses to gain 2 gold
            if (input.equals("gold")) {
                setGold(getGold() + 2);
                System.out.println(getName() + " chose gold.");
                break;

                // Player chooses to draw district cards
            } else if (input.equals("cards")) {
                int numToDraw = hasObservatory() ? 3 : 2;

                List<DistrictCard> options = new ArrayList<>();
                // Draw the correct number of cards and store them in a temporary list
                for (int i = 0; i < numToDraw; i++) {
                    DistrictCard card = districtDeck.draw();
                    if (card != null) options.add(card);
                }

                // If no cards were drawn, end this phase
                if (options.isEmpty()) {
                    System.out.println("No cards left to draw. " + getName() + " chose gold instead.");
                    setGold(getGold() + 2);
                    break;
                }

                // Library allows you to keep both drawn cards instead of choosing one
                if (hasLibrary()) {
                    for (DistrictCard card : options) {
                        addCardToHand(card);
                        System.out.println("Library effect: you kept " + card.getName() + " [" + card.getColor() + card.getCost() + "]");
                    }
                    break;
                }

                System.out.println(getName() + " drew " + options.size() + " cards.");
                System.out.println("Pick one of the following cards to keep: 'collect card <number>'");

                // Show the player the list of drawn cards
                for (int i = 0; i < options.size(); i++) {
                    DistrictCard card = options.get(i);
                    System.out.printf("%d. %s [color: %s], cost: %d%n", i + 1, card.getName(), card.getColor(), card.getCost());
                }

                // Keep asking until a valid card selection is made
                while (true) {
                    System.out.print("> ");
                    String choice = getScanner().nextLine().trim().toLowerCase();

                    // Player types a command to pick a card by number
                    if (choice.startsWith("collect card")) {
                        String[] parts = choice.split(" ");
                        // Make sure the collect card input was formatted correctly
                        if (parts.length == 3) {
                            try {
                                int selected = Integer.parseInt(parts[2]);
                                // Only allow collecting a card that was actually offered
                                if (selected >= 1 && selected <= options.size()) {
                                    DistrictCard chosenCard = options.get(selected - 1);
                                    addCardToHand(chosenCard);
                                    System.out.println("You chose card " + chosenCard.getName() + " [" + chosenCard.getColor() + chosenCard.getCost() + "]");
                                    options.remove(chosenCard);
                                    districtDeck.addCardsToBottom(options);
                                    break;
                                } else {
                                    System.out.println("Invalid card option. Try again.");
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("Invalid input. Enter a valid card option number.");
                            }
                        } else {
                            System.out.println("Invalid command format. Use 'collect card <number>'.");
                        }
                    } else {
                        System.out.println("Invalid command. Use 'collect card <number>'.");
                    }
                }

                break;

            } else {
                System.out.println("Invalid choice. Please type 'gold' or 'cards'.");
            }
        }
    }

    /**
     * Empty override required for compatibility. HumanPlayer does not use this.
     */
    @Override
    public void buildDistrict() {
        // intentionally left empty
    }

    /**
     * Builds the specified district card into the player's city.
     * Applies cost, removes the card from hand, and handles special effects like Factory.
     *
     * @param card the district card to build
     */
    public void buildDistrict(DistrictCard card) {
        int buildCost = getBuildCost(card);
        int originalCost = card.getCost();

        gold -= buildCost;
        hand.remove(card);

        game.addDistrictToCity(this, card);
        buildsThisTurn++;
        card.setBuiltRound(game.getCurrentRound());

        System.out.println(getName() + " built " + card.getName() + " [" + card.getColor() + originalCost + "]");

        // Factory reduces the cost of other purple districts
        if (hasFactory()
                && card.getColor().equalsIgnoreCase("purple")
                && !card.getName().equalsIgnoreCase("Factory")
                && buildCost < originalCost) {
            System.out.println("Factory effect: cost reduced from " + originalCost + " to " + buildCost + ".");
        }

        onDistrictBuilt(card);
    }

    /**
     * Called when this player finishes building a district.
     * Applies special effects based on the district's name (e.g. Bell Tower or Lighthouse).
     *
     * @param built the district that was just built
     */
    @Override
    public void onDistrictBuilt(DistrictCard built) {
        String name = built.getName();

        // Bell Tower: optional rule to end game at 7 districts
        if (name.equalsIgnoreCase("Bell Tower")) {
            System.out.println("Bell Tower effect: You may set the game to end when a player builds 7 districts [yes/no]");
            // Keep asking if the player wants to activate the Bell Tower
            while (true) {
                System.out.print("> ");
                String input = getScanner().nextLine().trim().toLowerCase();
                // Player agrees to end the game at 7 districts instead of 8
                if (input.equals("yes")) {
                    game.activateBellTower();
                    break;
                } else if (input.equals("no")) {
                    System.out.println("You chose not to activate Bell Tower.");
                    break;
                } else {
                    System.out.println("Please type 'yes' or 'no'.");
                }
            }
            return; // skip Lighthouse logic
        }

        // Lighthouse: lets you pick any one card from the visible DistrictDeck
        if (!name.equalsIgnoreCase("Lighthouse")) return;

        DistrictDeck districtDeck = game.getDeck();
        List<DistrictCard> deckCards = districtDeck.peekAll();

        // If no cards were drawn, exit early
        if (deckCards.isEmpty()) {
            System.out.println("The DistrictDeck is empty.");
            return;
        }

        System.out.println("Lighthouse effect: Choose one card from the DistrictDeck to add to your hand.");
        System.out.println("Use the command: collect card <number>");
        // Show the drawn cards so the player can choose one
        for (int i = 0; i < deckCards.size(); i++) {
            DistrictCard card = deckCards.get(i);
            System.out.printf("%d. %s [color: %s], cost: %d%n", i + 1, card.getName(), card.getColor(), card.getCost());
        }

        // Repeat until the player picks a valid card
        while (true) {
            System.out.print("> ");
            String input = getScanner().nextLine().trim().toLowerCase();
            // Interpret the command to collect a card by number
            if (input.startsWith("collect card")) {
                String[] parts = input.split(" ");
                if (parts.length == 3) {
                    try {
                        int selected = Integer.parseInt(parts[2]);
                        // Make sure the selected card index is valid
                        if (selected >= 1 && selected <= deckCards.size()) {
                            DistrictCard chosenCard = deckCards.get(selected - 1);
                            addCardToHand(chosenCard);
                            System.out.println("You chose card " + chosenCard.getName() + " [" + chosenCard.getColor() + chosenCard.getCost() + "]");
                            districtDeck.removeCard(chosenCard);
                            districtDeck.shuffle(); // shuffle remaining cards after selection
                            break;
                        } else {
                            System.out.println("Invalid card number. Choose between 1 and " + deckCards.size() + ".");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Enter a valid number after 'collect card'.");
                    }
                } else {
                    System.out.println("Invalid command format. Use: collect card <number>");
                }
            } else {
                System.out.println("Invalid command. Use: collect card <number>");
            }
        }
    }

    /**
     * Destroys a district in another player's city.
     * Applies cost (unless it's a free destruction), handles special logic for Museum, Bell Tower, and Graveyard.
     *
     * @param target        the player whose district is being destroyed
     * @param districtIndex index of the district in their city list
     * @param usedArmory    true if the destruction is free (e.g. from Armory)
     */
    @Override
    public void destroyDistrict(Player target, int districtIndex, boolean usedArmory) {
        List<DistrictCard> builtDistricts = game.getCityForPlayer(target);
        DistrictCard districtToDestroy = builtDistricts.get(districtIndex);
        int destroyCost = target.getDestroyCost(districtToDestroy);

        // Only deduct gold if destroy wasn't free due to armory
        if (!usedArmory) {
            this.gold -= destroyCost;
        }

        builtDistricts.remove(districtIndex);

        // Remove museum stored cards if Museum is destroyed
        if (districtToDestroy.getName().equalsIgnoreCase("Museum")) {
            target.getMuseumStoredCards().clear();
        }

        // Turn off Bell Tower mode if it's destroyed
        if (districtToDestroy.getName().equalsIgnoreCase("Bell Tower")) {
            game.deactivateBellTower();
        }

        // Graveyard effect (Warlord only and paid destruction)
        if (!usedArmory && this.getChosenCharacter() != null && this.getChosenCharacter().getTurnOrder() == 8) {
            for (Player player : game.getPlayers()) {
                if (player == this || !player.hasGraveyard() || player.getGold() < 1) continue;

                // Only human players can choose to activate Graveyard
                if (player instanceof HumanPlayer) {
                    System.out.println(player.getName() + ", You may pay 1 gold to recover the destroyed district (" + districtToDestroy.getName() + ") [yes/no]:");

                    // Keep asking until the user types yes or no
                    while (true) {
                        System.out.print("> ");
                        String input = getScanner().nextLine().trim().toLowerCase();
                        if (input.equals("yes")) {
                            player.setGold(player.getGold() - 1);
                            player.addCardToHand(districtToDestroy);
                            System.out.println("Graveyard effect: " + player.getName() + " paid 1 gold and recovered " + districtToDestroy.getName() + ".");
                            return;
                        } else if (input.equals("no")) {
                            break;
                        } else {
                            System.out.println("Please type 'yes' or 'no'.");
                        }
                    }
                } else {
                    // AI automatically uses Graveyard if eligible
                    player.setGold(player.getGold() - 1);
                    player.addCardToHand(districtToDestroy);
                    System.out.println("Graveyard effect: " + player.getName() + " recovered " + districtToDestroy.getName() + " using Graveyard.");
                    return;
                }
            }
        }

        System.out.println(this.getName() + " destroyed " + districtToDestroy.getName() + " in " + target.getName() + "'s city" + (usedArmory ? " for free (Armory)." : ", paying " + destroyCost + " gold."));
    }

    /**
     * @return true if the Magician ability has been used this turn
     */
    public boolean usedMagicianThisTurn() {
        return hasUsedMagicianAbility;
    }

    /**
     * Sets whether the Magician ability was used this turn.
     *
     * @param used true if used, false otherwise
     */
    public void setUsedMagicianThisTurn(boolean used) {
        hasUsedMagicianAbility = used;
    }

    /**
     * Prints ability reminders based on districts the player has built.
     * These reminders help the player remember special actions they can perform this turn.
     */
    public void printAbilityReminders() {
        List<DistrictCard> built = game.getCityForPlayer(this);
        boolean printedReminder = false;
        // Check all districts built by the player for ability reminders
        for (DistrictCard d : built) {
            String name = d.getName().toLowerCase();

            switch (name) {
                case "laboratory":
                    if (!usedLaboratoryThisTurn) {
                        System.out.println("Laboratory: Once during your turn, you may discard a district card from your hand and receive one gold from the bank. Use: discard <number>");
                    }
                    printedReminder = true;
                    break;

                case "smithy":
                    if (!usedSmithyThisTurn) {
                        System.out.println("Smithy: Once during your turn, you may pay two gold to draw 3 district cards. Use: draw");
                    }
                    printedReminder = true;
                    break;

                case "museum":
                    if (!usedMuseumThisTurn) {
                        System.out.println("Museum: Once during your turn, you may place a district card from your hand face down under the Museum. At the end of the game, you score one extra point for every card under the Museum. Use: place <number>");
                    }
                    printedReminder = true;
                    break;

                case "armory":
                    System.out.println("Armory: During your turn, you may destroy the Armory in order to destroy any other district card of your choice in another player's city. Use: armory");
                    printedReminder = true;
                    break;
            }
        }
        if (printedReminder) {
            GameUtils.delay(1000);
            System.out.println();
        }
    }
}