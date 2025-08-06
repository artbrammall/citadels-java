package citadels.state;

import citadels.model.DistrictCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the deck of district cards used in the game.
 * Handles drawing, shuffling, and manipulating the district card pile.
 */
public class DistrictDeck implements Deck<DistrictCard> {
    private List<DistrictCard> districtCards;

    /**
     * Creates a full deck by duplicating each district card type according to its quantity.
     * Automatically shuffles the deck after building it.
     *
     * @param cardTypes List of card templates (each with a quantity)
     */
    public DistrictDeck(List<DistrictCard> cardTypes) {
        this.districtCards = new ArrayList<>();
        // For each card type in the starting list, add the correct number of copies to the deck
        for (DistrictCard cardType : cardTypes) {
            // Repeat the card based on how many of that type should exist in the deck
            for (int i = 0; i < cardType.getQuantity(); i++) {
                districtCards.add(cardType);
            }
        }
        shuffle();
    }

    /**
     * Randomly shuffles the deck.
     */
    public void shuffle() {
        Collections.shuffle(districtCards);
    }

    /**
     * Draws the top card from the deck. Returns null if the deck is empty.
     *
     * @return The drawn district card, or null if no cards left
     */
    public DistrictCard draw() {
        // Only draw if the deck isn’t empty
        if (!districtCards.isEmpty()) {
            return districtCards.remove(0);
        }
        return null;
    }

    /**
     * Removes the first matching instance of the given card from the deck.
     *
     * @param card The district card to remove
     */
    public void removeCard(DistrictCard card) {
        districtCards.remove(card);
    }

    /**
     * Adds a list of cards to the bottom of the deck.
     *
     * @param cardsToAdd Cards to be added to the end
     */
    public void addCardsToBottom(List<DistrictCard> cardsToAdd) {
        districtCards.addAll(cardsToAdd);
    }

    /**
     * Returns a copy of the current deck without modifying it.
     *
     * @return A new list containing all remaining cards
     */
    public List<DistrictCard> peekAll() {
        return new ArrayList<>(districtCards);
    }

    /**
     * Returns the number of cards left in the deck.
     *
     * @return The size of the deck
     */
    public int size() {
        return districtCards.size();
    }

    /**
     * Returns the internal list representing the current deck.
     * Should be used carefully to avoid exposing internal state.
     *
     * @return The list of remaining district cards
     */
    public List<DistrictCard> getCards() {
        return districtCards;
    }

    /**
     * Replaces the current deck with a new list of cards.
     * Used for restoring game state from a save.
     *
     * @param cards The list of cards to set as the deck
     */
    public void setCards(List<DistrictCard> cards) {
        this.districtCards = cards;
    }

    /**
     * Empties the deck of all remaining district cards.
     * Useful for testing or resetting the deck.
     */
    public void clear() {
        districtCards.clear();
    }
}