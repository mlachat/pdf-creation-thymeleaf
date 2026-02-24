package com.example.print.benchmark;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AddressGenerator {

    private static final String[] FIRST_NAMES = {
            "Hans", "Peter", "Klaus", "Wolfgang", "Jürgen", "Günter", "Dieter", "Heinz",
            "Werner", "Gerhard", "Maria", "Elisabeth", "Monika", "Ursula", "Helga",
            "Renate", "Ingrid", "Karin", "Sabine", "Andrea", "Thomas", "Michael",
            "Andreas", "Stefan", "Christian", "Markus", "Frank", "Martin", "Bernd", "Uwe"
    };

    private static final String[] LAST_NAMES = {
            "Müller", "Schmidt", "Schneider", "Fischer", "Weber", "Meyer", "Wagner",
            "Becker", "Schulz", "Hoffmann", "Schäfer", "Koch", "Bauer", "Richter",
            "Klein", "Wolf", "Schröder", "Neumann", "Schwarz", "Zimmermann",
            "Braun", "Krüger", "Hofmann", "Hartmann", "Lange", "Schmitt", "Werner",
            "Schmitz", "Krause", "Meier"
    };

    private static final String[] STREETS = {
            "Hauptstraße", "Bahnhofstraße", "Schulstraße", "Gartenstraße", "Berliner Straße",
            "Dorfstraße", "Birkenweg", "Lindenstraße", "Kirchstraße", "Waldstraße",
            "Ringstraße", "Bergstraße", "Schillerstraße", "Goethestraße", "Mozartstraße",
            "Rosenweg", "Feldstraße", "Parkstraße", "Industriestraße", "Friedhofstraße",
            "Jahnstraße", "Lessingstraße", "Beethovenstraße", "Uhlandstraße", "Kantstraße",
            "Am Markt", "Wiesenstraße", "Mühlenweg", "Erlenweg", "Ahornweg"
    };

    private static final String[][] CITIES = {
            {"10115", "Berlin"}, {"20095", "Hamburg"}, {"80331", "München"},
            {"50667", "Köln"}, {"60311", "Frankfurt am Main"}, {"70173", "Stuttgart"},
            {"40213", "Düsseldorf"}, {"44135", "Dortmund"}, {"45127", "Essen"},
            {"04109", "Leipzig"}, {"28195", "Bremen"}, {"01067", "Dresden"},
            {"30159", "Hannover"}, {"90402", "Nürnberg"}, {"47051", "Duisburg"},
            {"44623", "Herne"}, {"42103", "Wuppertal"}, {"33602", "Bielefeld"},
            {"53111", "Bonn"}, {"48143", "Münster"}
    };

    private final Random random;

    public AddressGenerator(long seed) {
        this.random = new Random(seed);
    }

    public Map<String, Object> generateAddress() {
        String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
        String street = STREETS[random.nextInt(STREETS.length)];
        int houseNumber = random.nextInt(200) + 1;
        String[] city = CITIES[random.nextInt(CITIES.length)];

        Map<String, Object> address = new HashMap<>();
        address.put("recipientName", firstName + " " + lastName);
        address.put("recipientStreet", street + " " + houseNumber);
        address.put("recipientCity", city[0] + " " + city[1]);
        return address;
    }
}
