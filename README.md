# HashBreaker — version naïve

Le programme énumère les candidats de longueur 1 à la longueur maximale, dans l'ordre de l'alphabet fourni. Il calcule le SHA-256 des octets **UTF-8** de chaque candidat et s'arrête au premier condensat correspondant. Il utilise un seul fil d'exécution.

## Énumération et dictionnaire de test

La boucle de `crack` choisit successivement la longueur des candidats. La méthode récursive `enumerateAndCheck` essaie chaque symbole de l'alphabet à chaque position. Une fois le mot complet, elle calcule et compare son SHA-256. Il n'y a pas de type `enum` Java : l'alphabet est choisi à l'exécution.

Le fichier [dictionnaire-sha256.csv](src/main/resources/dictionnaire-sha256.csv) contient huit exemples avec le mot attendu, son condensat, l'alphabet et la longueur maximale. `Main` et `MainTest` lisent ce fichier en UTF-8 et vérifient que chaque mot est retrouvé. Pour une recherche manuelle, passer uniquement le condensat, l'alphabet et la longueur maximale à `Main`.

## Exécution

Dans IntelliJ IDEA, lancer `fr.brex.Main` avec le bouton Run, sans arguments. Le programme cherche les huit mots du dictionnaire et affiche le temps de chaque recherche.

Sous PowerShell, depuis ce dossier :

```powershell
mvn -q test-compile
java -cp 'target/classes;target/test-classes' fr.brex.MainTest
java -cp target/classes fr.brex.Main
$alphabet = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
java -cp target/classes fr.brex.Main A532CA5E11E2B06CCC911E0D962A4864CDB87DA05723F3A050A376D0F0895E63 $alphabet 3
java -cp target/classes fr.brex.Main BD7D0EA8CF7ADE4A446BA4EFC46FD99071EC3F423770991AC51F70EC5A894DC7 $alphabet 4
```

Les deux condensats correspondent respectivement à `z3D` et `Sh3n`. L'alphabet contient 62 caractères, dans l'ordre indiqué ci-dessus. La mesure commence avant la préparation du condensat et se termine dès que le mot est trouvé ; elle exclut le démarrage de la JVM.

## Référence mesurée

Windows 11, Java 25.0.3, trois exécutions indépendantes par mot :

| Mot | Essai 1 | Essai 2 | Essai 3 | Médiane |
| --- | ---: | ---: | ---: | ---: |
| `z3D` | 0,048 s | 0,046 s | 0,050 s | **0,048 s** |
| `Sh3n` | 0,846 s | 0,879 s | 0,820 s | **0,846 s** |

Ces temps sont propres à cette machine et à cet ordre d'énumération. Pour comparer une optimisation, conserver les mêmes condensats, alphabet et longueurs, puis refaire plusieurs mesures dans les mêmes conditions.
