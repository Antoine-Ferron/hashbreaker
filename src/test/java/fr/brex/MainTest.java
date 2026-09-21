package fr.brex;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MainTest {
    public static void main(String[] args) throws Exception {
        String ascii = Main.resolveAlphabet("ASCII_UTF8");
        if (ascii.length() != 128) {
            throw new AssertionError("L'alphabet doit contenir les 128 caractères ASCII");
        }
        for (int codePoint = 0; codePoint < 128; codePoint++) {
            if (ascii.indexOf(codePoint) < 0) {
                throw new AssertionError("Caractère ASCII manquant : " + codePoint);
            }
        }

        InputStream input = MainTest.class.getResourceAsStream("/dictionnaire-sha256.csv");
        if (input == null) {
            throw new AssertionError("Dictionnaire de test introuvable");
        }

        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            reader.readLine(); // En-tête du fichier CSV.
            String line;
            while ((line = reader.readLine()) != null) {
                String[] entry = line.split(";", -1);
                if (entry.length != 5) {
                    throw new AssertionError("Ligne CSV invalide : " + line);
                }
                String found = Main.crack(entry[1], entry[2],
                        Integer.parseInt(entry[3]), Integer.parseInt(entry[4]));
                if (!entry[0].equals(found)) {
                    throw new AssertionError("Mot attendu : " + entry[0] + ", trouvé : " + found);
                }
                count++;
            }
        }

        String abcHash = "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD";
        if (Main.crack(abcHash, "ab", 3) != null) {
            throw new AssertionError("Aucun mot ne doit être trouvé sans c");
        }
        if (count != 12) {
            throw new AssertionError("Le dictionnaire doit contenir douze cas, trouvé : " + count);
        }
        System.out.println(count + " cas valides");
    }
}
