package citadels.state;

import citadels.model.CharacterCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the deck of character cards used during the selection phase.
 * Supports drawing, shuffling, and resetting the deck.
 */
public class CharacterDeck implements Deck<CharacterCard> {
    private List<CharacterCard> characterCards;

    /**
     * Constructs a new character deck using the provided list of character cards.
     * The deck is automatically shuffled.
     *
     * @param characterCards The initial list of character cards
     */
    public CharacterDeck(List<CharacterCard> characterCards) {
        this.characterCards = new ArrayList<>(characterCards);
        shuffle();
    }

    /**
     * Shuffles the character deck randomly.
     */
    public void shuffle() {
        Collections.shuffle(characterCards);
    }

    /**
     * Draws and removes the top character card from the deck.
     *
     * @return The top character card, or null if the deck is empty
     */
    public CharacterCard draw() {
        // Only draw if there are still cards left in the deck
        if (!characterCards.isEmpty()) {
            return characterCards.remove(0);
        }
        return null;
    }

    /**
     * Returns the number of character cards left in the deck.
     *
     * @return Size of the deck
     */
    public int size() {
        return characterCards.size();
    }

    /**
     * Returns the internal list of character cards.
     * Use with care — changes affect the actual deck.
     *
     * @return List of character cards
     */
    public List<CharacterCard> getCards() {
        return characterCards;
    }

    /**
     * Replaces the current deck with a new list of character cards.
     * Useful for reloading the deck from saved game state.
     *
     * @param cards The new list of character cards
     */
    public void setCards(List<CharacterCard> cards) {
        this.characterCards = new ArrayList<>(cards);
    }
}