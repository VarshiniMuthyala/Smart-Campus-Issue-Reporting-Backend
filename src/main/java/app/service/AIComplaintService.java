package app.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight local NLP engine for CampusFix.
 *
 * It uses weighted phrase matching rather than a single keyword hit.
 * This makes categorization and priority detection more reliable while
 * keeping the application completely local and free of external API keys.
 */
@Service
public class AIComplaintService {

    private static final String ELECTRICAL = "ELECTRICAL";
    private static final String PLUMBING = "PLUMBING";
    private static final String NETWORK = "NETWORK";
    private static final String FURNITURE = "FURNITURE";
    private static final String CIVIL = "CIVIL";
    private static final String GENERAL_MAINTENANCE = "GENERAL MAINTENANCE";
    private static final String GENERAL = "GENERAL";

    public String categorizeComplaint(String title, String description) {
        String text = normalize(title, description);

        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put(ELECTRICAL, score(text,
                "electricity", "electrical", "electric", "power cut", "power failure",
                "short circuit", "sparking", "spark", "shock", "switch", "socket",
                "plug", "wire", "wiring", "fan", "light", "bulb", "tube light", "ac",
                "air conditioner", "current"));

        scores.put(PLUMBING, score(text,
                "plumbing", "water leak", "water leakage", "leak", "leakage", "tap",
                "pipe", "washroom", "toilet", "sink", "drain", "flush", "faucet",
                "water supply", "water problem"));

        scores.put(NETWORK, score(text,
                "wifi", "wi-fi", "internet", "network", "router", "ethernet", "lan",
                "connectivity", "connection", "no internet", "internet down", "online"));

        scores.put(FURNITURE, score(text,
                "chair", "desk", "table", "bench", "furniture", "door", "window",
                "cupboard", "drawer", "seat", "broken chair", "broken desk"));

        scores.put(CIVIL, score(text,
                "wall", "floor", "ceiling", "roof", "building", "crack", "cracked",
                "paint", "construction", "road", "pothole", "civil", "tile", "tiles",
                "water seepage"));

        scores.put(GENERAL_MAINTENANCE, score(text,
                "cleaning", "clean", "garbage", "dust", "maintenance", "repair",
                "classroom", "laboratory", "lab", "corridor", "campus", "garden",
                "washroom cleaning", "waste"));

        String bestCategory = GENERAL;
        int bestScore = 0;

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (entry.getValue() > bestScore) {
                bestScore = entry.getValue();
                bestCategory = entry.getKey();
            }
        }

        return bestCategory;
    }

    public String determinePriority(String title, String description) {
        String text = normalize(title, description);

        int urgentScore = score(text,
                "fire", "flame", "smoke", "electric shock", "shock", "sparking",
                "spark", "short circuit", "explosion", "danger", "dangerous",
                "emergency", "flood", "major leakage", "gas leak", "life threatening",
                "security risk", "unsafe", "accident");

        int highScore = score(text,
                "not working", "completely broken", "power failure", "internet down",
                "water supply stopped", "major", "severe", "urgent", "critical",
                "broken", "damaged", "leaking");

        int mediumScore = score(text,
                "problem", "issue", "repair", "slow", "fault", "faulty", "needs fixing",
                "maintenance", "noise", "minor damage");

        if (urgentScore >= 3 || containsAny(text,
                "fire", "electric shock", "short circuit", "gas leak", "explosion")) {
            return "URGENT";
        }

        if (highScore >= 4 || urgentScore >= 2) {
            return "HIGH";
        }

        if (mediumScore >= 2 || highScore >= 2 || urgentScore >= 1) {
            return "MEDIUM";
        }

        return "LOW";
    }

    private int score(String text, String... phrases) {
        int total = 0;
        for (String phrase : phrases) {
            if (containsPhrase(text, phrase)) {
                total += phrase.contains(" ") ? 3 : 2;
            }
        }
        return total;
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (containsPhrase(text, phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsPhrase(String text, String phrase) {
        String normalizedPhrase = phrase.toLowerCase().trim();
        if (normalizedPhrase.isEmpty()) {
            return false;
        }
        return text.matches(".*(?s)(^|\\W)" + java.util.regex.Pattern.quote(normalizedPhrase) + "($|\\W).*");
    }

    private String normalize(String title, String description) {
        String combined = (title == null ? "" : title) + " "
                + (description == null ? "" : description);
        return combined.toLowerCase().replaceAll("[^a-z0-9\\s-]", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
