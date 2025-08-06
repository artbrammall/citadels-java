package citadels.behaviour;

import citadels.model.CharacterCard;
import citadels.model.DistrictCard;
import citadels.model.Player;
import citadels.state.DistrictDeck;

import java.util.List;

/**
 * Represents any player that can take actions in the game — like picking characters, building, or using abilities.
 * Used by both AI and human players.
 */
public interface PlayerBehaviour {

    /**
     * Runs this player's turn from start to end.
     * This is the main method the game calls to let the player do their actions.
     */
    void takeTurn();

    /**
     * Lets the player choose a character card from the list of available ones.
     * Called during the character selection phase.
     *
     * @param available The list of character cards left to choose from
     * @return The character card the player chose
     */
    CharacterCard chooseCharacter(List<CharacterCard> available);

    /**
     * Called when it's time for the player to pick income — either gold or cards.
     * Runs once near the start of the turn.
     *
     * @param districtDeck The DistrictDeck to draw cards from, if the player chooses cards
     */
    void chooseIncome(DistrictDeck districtDeck);

    /**
     * Lets the player use their character's ability.
     * Happens after choosing income, before building.
     */
    void useAbility();

    /**
     * Builds a district card from the player's hand.
     * Can be called once or more depending on rules.
     */
    void buildDistrict();

    /**
     * Triggered after a district was built.
     * Used for effects like Bell Tower or Wishing Well.
     *
     * @param built The district card that was just added to the city
     */
    void onDistrictBuilt(DistrictCard built);

    /**
     * Destroys one of another player's districts, if allowed.
     * Used during the Warlord's turn.
     *
     * @param target        The player whose district is being destroyed
     * @param districtIndex Index of the district to destroy
     * @param usedArmory    Whether the destruction was free via Armory
     */
    void destroyDistrict(Player target, int districtIndex, boolean usedArmory);

    /**
     * Checks if the player is allowed to destroy the given district.
     * Used before attempting to destroy with Warlord.
     *
     * @param target        The player whose district we're checking
     * @param districtIndex The index of the district being looked at
     * @return True if it's valid to destroy, false otherwise
     */
    boolean canDestroyDistrict(Player target, int districtIndex);
}