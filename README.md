# HashBreaker — version naïve

Le programme énumère les candidats dans l'ordre de l'alphabet fourni. Il calcule le SHA-256 des octets **UTF-8** de chaque candidat et s'arrête au premier condensat correspondant, ou après **60 secondes** pour passer au cas suivant avec le statut `Timeout`. Il affiche les longueurs parcourues, la taille de l'alphabet, le nombre de candidats réellement hachés et le temps en millisecondes. Il utilise un seul fil d'exécution.

## Énumération et dictionnaire de test

La boucle de `crack` choisit successivement la longueur des candidats. La méthode récursive `enumerateAndCheck` essaie chaque symbole de l'alphabet à chaque position. Une fois le mot complet, elle calcule et compare son SHA-256. Il n'y a pas de type `enum` Java : l'alphabet est choisi à l'exécution.

Le fichier [dictionnaire-sha256.csv](src/main/resources/dictionnaire-sha256.csv) contient douze exemples avec le mot attendu, son condensat, l'alphabet et les longueurs minimale et maximale. Le marqueur `ASCII_UTF8` désigne les **128 caractères ASCII** (`0x00` à `0x7F`), y compris les caractères de contrôle. L'ordre place d'abord les lettres et chiffres, puis les autres caractères ASCII. Tous les candidats sont encodés en UTF-8 avant le hash. Les cas `café`, `é` et `😀` utilisent un alphabet Unicode distinct, car ces caractères ne sont pas ASCII.

Les quatre derniers cas ont 5 à 8 caractères et combinent lettres, chiffre et caractère spécial. Leur longueur exacte est indiquée dans le CSV : avec l'alphabet ASCII complet, parcourir aussi toutes les longueurs plus courtes rendrait notamment le cas de 8 caractères impraticable. Le mode avec arguments continue à chercher de la longueur 1 à la limite fournie.

## Exécution

Dans IntelliJ IDEA, lancer `fr.brex.Main` avec le bouton Run, sans arguments. Le programme cherche les douze mots du dictionnaire et affiche le temps de chaque recherche.

Sous PowerShell, depuis ce dossier :

```powershell
mvn -q test-compile
java -cp 'target/classes;target/test-classes' fr.brex.MainTest
java -cp target/classes fr.brex.Main
java -cp target/classes fr.brex.Main A532CA5E11E2B06CCC911E0D962A4864CDB87DA05723F3A050A376D0F0895E63 ASCII_UTF8 3
java -cp target/classes fr.brex.Main BD7D0EA8CF7ADE4A446BA4EFC46FD99071EC3F423770991AC51F70EC5A894DC7 ASCII_UTF8 4
java '-Dhashbreaker.timeoutSeconds=1' -cp target/classes fr.brex.Main
```

Les deux condensats correspondent respectivement à `z3D` et `Sh3n`. La mesure commence avant la préparation du condensat et se termine dès que le mot est trouvé ou que le délai est atteint ; elle exclut le démarrage de la JVM. La propriété Java `hashbreaker.timeoutSeconds` permet de changer le délai pour un essai ; sans elle, il est de 60 secondes.

Les temps des cas courts sont dominés par l'initialisation. Avec 128 symboles, un parcours complet des seuls mots de 8 caractères demanderait **128⁸** condensats. Le programme s'arrête dès qu'il trouve la cible ou qu'il atteint le délai.

## Référence mesurée

Windows 11, Java 25.0.3, alphabet `ASCII_UTF8`, trois exécutions indépendantes par mot :

| Mot | Candidats | Essai 1 | Essai 2 | Essai 3 | Médiane |
| --- | ---: | ---: | ---: | ---: | ---: |
| `z3D` | 433 182 | 75,394 ms | 77,923 ms | 76,679 ms | **76,679 ms** |
| `Sh3n` | 94 510 094 | 9 623,003 ms | 9 958,210 ms | 9 958,160 ms | **9 958,160 ms** |

Ces temps sont propres à cette machine et à cet ordre d'énumération. Pour comparer une optimisation, conserver les mêmes condensats, alphabet et longueurs, puis refaire plusieurs mesures dans les mêmes conditions.
