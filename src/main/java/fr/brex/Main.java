package fr.brex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/** Recherche naïve d'un mot à partir de son condensat SHA-256. */
public class Main {
    /** Lance les huit cas du dictionnaire sans arguments, ou une recherche personnalisée. */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        try {
            if (args.length == 0) {
                runDictionary();
            } else if (args.length == 3) {
                run(args[0], args[1], Integer.parseInt(args[2]));
            } else {
                System.err.println("Usage : java fr.brex.Main [<sha256-hex> <alphabet> <longueur-max>]");
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    /** Lit les cas depuis les ressources et vérifie chaque mot retrouvé. */
    private static void runDictionary() throws IOException, NoSuchAlgorithmException {
        InputStream input = Main.class.getResourceAsStream("/dictionnaire-sha256.csv");
        if (input == null) {
            throw new IOException("Dictionnaire de test introuvable");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.readLine(); // En-tête du CSV.
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                String[] entry = line.split(";", -1);
                if (entry.length != 4) {
                    throw new IllegalArgumentException("Ligne CSV invalide : " + line);
                }
                System.out.printf("Cas %d : ", ++count);
                String found = run(entry[1], entry[2], Integer.parseInt(entry[3]));
                if (!entry[0].equals(found)) {
                    throw new AssertionError("Mot attendu : " + entry[0] + ", trouvé : " + found);
                }
            }
        }
    }

    /** Mesure le temps de recherche et affiche le mot trouvé, s'il existe. */
    private static String run(String targetHex, String alphabet, int maxLength)
            throws NoSuchAlgorithmException {
        long start = System.nanoTime();
        String result = crack(targetHex, alphabet, maxLength);
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        System.out.printf("%s (%.3f s)%n",
                result == null ? "Aucune correspondance" : "Mot trouvé : " + result,
                seconds);
        return result;
    }

    /**
     * Essaie tous les mots de longueur 1 à {@code maxLength}, dans l'ordre de l'alphabet.
     *
     * @param targetHex condensat SHA-256 à retrouver, sous forme de 64 caractères hexadécimaux
     * @param alphabet caractères autorisés dans les mots candidats
     * @param maxLength longueur maximale des mots candidats
     * @return le premier mot dont le condensat correspond, ou {@code null} si aucun ne correspond
     */
    static String crack(String targetHex, String alphabet, int maxLength)
            throws NoSuchAlgorithmException {
        if (alphabet.isEmpty() || maxLength < 1) {
            throw new IllegalArgumentException("L'alphabet doit être non vide et la longueur maximale positive");
        }
        if (targetHex.length() != 64) {
            throw new IllegalArgumentException("Le SHA-256 doit contenir 64 caractères hexadécimaux");
        }

        byte[] target = HexFormat.of().parseHex(targetHex);
        // Un point de code représente aussi les caractères hors du plan Unicode de base.
        int[] symbols = alphabet.codePoints().distinct().toArray();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        // ponytail: parcours séquentiel sans parallélisme ; mesurer cette base avant d'optimiser.
        for (int length = 1; length <= maxLength; length++) {
            String found = enumerateAndCheck(new int[length], 0, symbols, target, sha256);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /** Énumère les combinaisons en remplissant le candidat de gauche à droite. */
    private static String enumerateAndCheck(int[] candidate, int position, int[] symbols,
                                            byte[] target, MessageDigest sha256) {
        if (position == candidate.length) {
            String word = new String(candidate, 0, candidate.length);
            // SHA-256 s'applique aux octets UTF-8 du mot, pas directement aux caractères Java.
            byte[] hash = sha256.digest(word.getBytes(StandardCharsets.UTF_8));
            return Arrays.equals(hash, target) ? word : null;
        }

        // Chaque appel fixe un symbole supplémentaire, jusqu'à former un mot complet.
        for (int symbol : symbols) {
            candidate[position] = symbol;
            String found = enumerateAndCheck(candidate, position + 1, symbols, target, sha256);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
