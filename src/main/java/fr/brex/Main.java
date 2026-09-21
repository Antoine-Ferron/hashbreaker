package fr.brex;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

public class Main {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        if (args.length != 3) {
            System.err.println("Usage : java fr.brex.Main <sha256-hex> <alphabet> <longueur-max>");
            return;
        }

        try {
            int maxLength = Integer.parseInt(args[2]);
            long start = System.nanoTime();
            String result = crack(args[0], args[1], maxLength);
            double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
            System.out.printf("%s (%.3f s)%n",
                    result == null ? "Aucune correspondance" : "Mot trouvé : " + result,
                    seconds);
        } catch (IllegalArgumentException e) {
            System.err.println("Erreur : " + e.getMessage());
        }
    }

    static String crack(String targetHex, String alphabet, int maxLength)
            throws NoSuchAlgorithmException {
        if (alphabet.isEmpty() || maxLength < 1) {
            throw new IllegalArgumentException("L'alphabet doit être non vide et la longueur maximale positive");
        }
        if (targetHex.length() != 64) {
            throw new IllegalArgumentException("Le SHA-256 doit contenir 64 caractères hexadécimaux");
        }

        byte[] target = HexFormat.of().parseHex(targetHex);
        int[] symbols = alphabet.codePoints().distinct().toArray();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        // ponytail: parcours séquentiel sans parallélisme ; mesurer cette base avant d'optimiser.
        for (int length = 1; length <= maxLength; length++) {
            String found = search(new int[length], 0, symbols, target, sha256);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String search(int[] candidate, int position, int[] symbols,
                                 byte[] target, MessageDigest sha256) {
        if (position == candidate.length) {
            String word = new String(candidate, 0, candidate.length);
            byte[] hash = sha256.digest(word.getBytes(StandardCharsets.UTF_8));
            return Arrays.equals(hash, target) ? word : null;
        }

        for (int symbol : symbols) {
            candidate[position] = symbol;
            String found = search(candidate, position + 1, symbols, target, sha256);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
