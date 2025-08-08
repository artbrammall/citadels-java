package citadels.app;

import citadels.core.Game;
import citadels.model.DistrictCard;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point for the Citadels game.
 * Loads district card data and starts the game loop.
 */
public class App {
    public static InputStream input = System.in; // Allows input to be replaceable when testing

    /**
     * Default constructor. Nothing needs to be initialized manually here.
     */
    public App() {
        // No setup needed
    }

    /**
     * Main method — loads district cards and starts the game.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            App app = new App();
            Scanner scanner = new Scanner(input);
            List<DistrictCard> allDistrictCards = app.loadDistrictCards();

            Game game = new Game(allDistrictCards);
            game.runPhase();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads all district cards from the cards.tsv file in the resources folder.
     * Each line is parsed into a DistrictCard with its quantity, color, cost, and optional ability.
     *
     * @return A list of all parsed district card templates
     */
    public List<DistrictCard> loadDistrictCards() {
        List<DistrictCard> allDistrictCards = new ArrayList<>();

        try (InputStream inputStream = getClass().getResourceAsStream("/citadels/cards.tsv")) {
            // File couldn't be loaded — probably missing from resources
            if (inputStream == null) {
                System.err.println("Could not find resource /citadels/cards.tsv");
                return allDistrictCards;
            }

            try (BufferedReader tsvReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String header = tsvReader.readLine(); // Skip header row

                String tsvLine;
                // Read each line of the TSV file one at a time
                while ((tsvLine = tsvReader.readLine()) != null) {
                    if (tsvLine.trim().isEmpty()) continue; // Skip blank lines

                    String[] fields = tsvLine.split("\t");
                    // Skip malformed lines that don't have all expected fields
                    if (fields.length < 4) {
                        System.err.println("Skipping malformed line: " + tsvLine);
                        continue;
                    }

                    String name = fields[0];
                    int quantity, cost;
                    String color;

                    // Try to parse the cost field — it should be a number
                    try {
                        quantity = Integer.parseInt(fields[1]);
                        color = fields[2];
                        cost = Integer.parseInt(fields[3]);
                        // If the cost wasn’t a valid number, just skip this card
                    } catch (NumberFormatException e) {
                        System.err.println("Skipping line with invalid number: " + tsvLine);
                        continue;
                    }

                    String ability = fields.length > 4 ? fields[4] : "";

                    DistrictCard card = new DistrictCard(name, color, cost, quantity, ability);
                    allDistrictCards.add(card);
                }
            }
            // Catch anything else that goes wrong while loading the TSV
        } catch (Exception e) {
            System.err.println("Error reading cards.tsv: " + e.getMessage());
        }

        return allDistrictCards;
    }

    /**
     * Parses a district card list from a given TSV-formatted input stream.
     * <p>
     * This method reads a TSV (Tab-Separated Values) stream where each line represents
     * a district card entry. The first line (header) is skipped. Each valid line should contain:
     * - name (String)
     * - quantity (int)
     * - color (String)
     * - cost (int)
     * - optional ability (String, only for purple cards)
     * <p>
     * Malformed lines (e.g., missing fields, non-numeric cost/quantity) and blank lines
     * are skipped gracefully. If the stream is null or an exception occurs while reading,
     * an empty list is returned.
     *
     * @param inputStream the input stream pointing to the TSV source
     * @return a list of successfully parsed DistrictCard objects
     */
    List<DistrictCard> loadDistrictCardsFromStream(InputStream inputStream) {
        List<DistrictCard> allDistrictCards = new ArrayList<>();

        if (inputStream == null) {
            System.err.println("Could not find resource /citadels/cards.tsv");
            return allDistrictCards;
        }

        try (BufferedReader tsvReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            tsvReader.readLine(); // Skip header

            String tsvLine;
            while ((tsvLine = tsvReader.readLine()) != null) {
                if (tsvLine.trim().isEmpty()) continue;

                String[] fields = tsvLine.split("\t");
                if (fields.length < 4) {
                    System.err.println("Skipping malformed line: " + tsvLine);
                    continue;
                }

                try {
                    int quantity = Integer.parseInt(fields[1]);
                    String color = fields[2];
                    int cost = Integer.parseInt(fields[3]);
                    String ability = fields.length > 4 ? fields[4] : "";
                    allDistrictCards.add(new DistrictCard(fields[0], color, cost, quantity, ability));
                } catch (NumberFormatException e) {
                    System.err.println("Skipping line with invalid number: " + tsvLine);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading cards.tsv: " + e.getMessage());
        }

        return allDistrictCards;
    }
}
