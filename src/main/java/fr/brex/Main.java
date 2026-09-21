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
    private static final long TIMEOUT_NANOS =
            Long.getLong("hashbreaker.timeoutSeconds", 60L) * 1_000_000_000L;
    private static final String ASCII_BASE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    /** Lance les douze cas du dictionnaire sans arguments, ou une recherche personnalisée. */
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        try {
            if (args.length == 0) {
                runDictionary();
            } else if (args.length == 3) {
                run(args[0], args[1], 1, Integer.parseInt(args[2]), null);
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
                if (entry.length != 5) {
                    throw new IllegalArgumentException("Ligne CSV invalide : " + line);
                }
                System.out.printf("Cas %d : ", ++count);
                run(entry[1], entry[2], Integer.parseInt(entry[3]), Integer.parseInt(entry[4]), entry[0]);
            }
        }
    }

    /** Mesure une recherche et affiche son résultat, y compris si elle atteint le délai. */
    private static void run(String targetHex, String alphabet, int minLength, int maxLength, String expected)
            throws NoSuchAlgorithmException {
        alphabet = resolveAlphabet(alphabet);
        SearchCounter counter = new SearchCounter();
        String result = crack(targetHex, alphabet, minLength, maxLength, counter);
        double milliseconds = (System.nanoTime() - counter.startedAt) / 1_000_000.0;
        long alphabetSize = alphabet.codePoints().distinct().count();
        String status = counter.timedOut ? "Timeout" : result == null ? "Aucune correspondance" : "Mot trouvé : " + result;
        System.out.printf("%s (longueurs : %d-%d, alphabet : %d symbole%s, %d candidat%s, %.3f ms)%n",
                status, minLength, maxLength,
                alphabetSize, alphabetSize == 1 ? "" : "s",
                counter.candidates, counter.candidates == 1 ? "" : "s", milliseconds);
        if (!counter.timedOut && expected != null && !expected.equals(result)) {
            throw new AssertionError("Mot attendu : " + expected + ", trouvé : " + result);
        }
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
        return crack(targetHex, alphabet, 1, maxLength);
    }

    static String crack(String targetHex, String alphabet, int minLength, int maxLength)
            throws NoSuchAlgorithmException {
        return crack(targetHex, alphabet, minLength, maxLength, new SearchCounter());
    }

    private static String crack(String targetHex, String alphabet, int minLength, int maxLength, SearchCounter counter)
            throws NoSuchAlgorithmException {
        alphabet = resolveAlphabet(alphabet);
        if (alphabet.isEmpty() || minLength < 1 || maxLength < minLength) {
            throw new IllegalArgumentException("L'alphabet doit être non vide et les longueurs valides");
        }
        if (targetHex.length() != 64) {
            throw new IllegalArgumentException("Le SHA-256 doit contenir 64 caractères hexadécimaux");
        }

        byte[] target = HexFormat.of().parseHex(targetHex);
        // Un point de code représente aussi les caractères hors du plan Unicode de base.
        int[] symbols = alphabet.codePoints().distinct().toArray();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        // ponytail: parcours séquentiel sans parallélisme ; mesurer cette base avant d'optimiser.
        for (int length = minLength; length <= maxLength; length++) {
            String found = enumerateAndCheck(new int[length], 0, symbols, target, sha256, counter);
            if (found != null || counter.timedOut) {
                return found;
            }
        }
        return null;
    }

    /** Énumère les combinaisons en remplissant le candidat de gauche à droite. */
    private static String enumerateAndCheck(int[] candidate, int position, int[] symbols,
                                            byte[] target, MessageDigest sha256, SearchCounter counter) {
        if (position == candidate.length) {
            // Vérifier l'horloge tous les 1024 candidats limite le coût du timeout.
            if ((counter.candidates & 1023) == 0
                    && System.nanoTime() - counter.startedAt >= TIMEOUT_NANOS) {
                counter.timedOut = true;
                return null;
            }
            String word = new String(candidate, 0, candidate.length);
            // SHA-256 s'applique aux octets UTF-8 du mot, pas directement aux caractères Java.
            counter.candidates++;
            byte[] hash = sha256.digest(word.getBytes(StandardCharsets.UTF_8));
            return Arrays.equals(hash, target) ? word : null;
        }

        // Chaque appel fixe un symbole supplémentaire, jusqu'à former un mot complet.
        for (int symbol : symbols) {
            candidate[position] = symbol;
            String found = enumerateAndCheck(candidate, position + 1, symbols, target, sha256, counter);
            if (found != null || counter.timedOut) {
                return found;
            }
        }
        return null;
    }

    /** Étend le marqueur du CSV aux 128 caractères ASCII, sans en omettre les contrôles. */
    static String resolveAlphabet(String alphabet) {
        if (!alphabet.equals("ASCII_UTF8")) {
            return alphabet;
        }
        StringBuilder ascii = new StringBuilder(ASCII_BASE);
        for (int codePoint = 0; codePoint < 128; codePoint++) {
            if (ASCII_BASE.indexOf(codePoint) < 0) {
                ascii.append((char) codePoint);
            }
        }
        return ascii.toString();
    }

    /** Compte les candidats réellement hachés pendant une recherche. */
    private static class SearchCounter {
        final long startedAt = System.nanoTime();
        long candidates;
        boolean timedOut;
    }
}
