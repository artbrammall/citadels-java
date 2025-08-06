package citadels.state;

import java.util.List;

public interface Deck<T> {
    /**
     * Draws a card from the top of the deck.
     */
    T draw();

    /**
     * Shuffles the deck.
     */
    void shuffle();

    /**
     * Gets the current size of the deck.
     */
    int size();

    /**
     * Returns a copy or view of the internal card list.
     */
    List<T> getCards();

    /**
     * Replaces the current cards in the deck.
     */
    void setCards(List<T> cards);
}