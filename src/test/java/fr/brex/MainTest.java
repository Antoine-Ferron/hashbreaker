package fr.brex;

public class MainTest {
    public static void main(String[] args) throws Exception {
        String abc = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        if (!"abc".equals(Main.crack(abc, "abc", 3))) {
            throw new AssertionError("Le mot abc doit être trouvé");
        }
        if (Main.crack(abc, "ab", 3) != null) {
            throw new AssertionError("Aucun mot ne doit être trouvé sans c");
        }
        String accent = "4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c";
        if (!"é".equals(Main.crack(accent, "é", 1))) {
            throw new AssertionError("Le condensat doit utiliser UTF-8");
        }
        String emoji = "f0443a342c5ef54783a111b51ba56c938e474c32324d90c3a60c9c8e3a37e2d9";
        if (!"😀".equals(Main.crack(emoji, "😀", 1))) {
            throw new AssertionError("L'alphabet doit accepter les caractères Unicode");
        }
    }
}
