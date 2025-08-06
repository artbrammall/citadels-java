package citadels.state;

import citadels.model.DistrictCard;
import citadels.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks what districts each player has built in their city.
 * Internally uses a map to associate each player with a list of their built districts.
 */
public class Board {
    private final Map<Player, List<DistrictCard>> playerCityMap; // Maps each player to their city (built districts)

    /**
     * Creates a board and initializes an empty city for each player.
     *
     * @param players The list of all players in the game
     */
    public Board(List<Player> players) {
        // Initialize each player's city entry in the map with an empty list
        playerCityMap = new HashMap<>();
        for (Player player : players) {
            playerCityMap.put(player, new ArrayList<>()); // Start with empty cities
        }
    }

    /**
     * Returns the internal mapping of players to their built districts.
     *
     * @return A map from each player to their list of built districts.
     */
    public Map<Player, List<DistrictCard>> getPlayerCityMap() {
        return playerCityMap;
    }

    /**
     * Adds a district to the given player's city.
     *
     * @param player   The player whose city the district is added to
     * @param district The district to add
     */
    public void addDistrictToCity(Player player, DistrictCard district) {
        // If the player isn't already in the map (shouldn't happen), initialize their city list
        playerCityMap
                .computeIfAbsent(player, p -> new ArrayList<>())
                .add(district);
    }

    /**
     * Returns the list of districts built by the given player.
     *
     * @param player The player whose city you want to access
     * @return A list of built districts, or empty list if player has none
     */
    public List<DistrictCard> getCityForPlayer(Player player) {
        return playerCityMap.getOrDefault(player, new ArrayList<>());
    }
}
