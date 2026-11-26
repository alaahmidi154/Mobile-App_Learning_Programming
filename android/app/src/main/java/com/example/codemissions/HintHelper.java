package com.example.codemissions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns Python errors and failed tests into short explanations in French. */
public class HintHelper {

    /** "NameError: name 'x' is not defined"  ->  "NameError" */
    public static String errorType(String message) {
        if (message == null) return "";
        int i = message.indexOf(':');
        return (i < 0 ? message : message.substring(0, i)).trim();
    }

    /** What this Python error means, and how to fix it. */
    public static String explain(String message) {
        String msg = message == null ? "" : message;
        String type = errorType(msg);
        switch (type) {
            case "SyntaxError":
                return "Erreur de syntaxe : vérifie le « : » à la fin des if / for / while / def, "
                        + "les parenthèses et les guillemets bien fermés.";
            case "IndentationError":
            case "TabError":
                return "Problème d'indentation : les lignes d'un même bloc doivent commencer par le "
                        + "même nombre d'espaces (4 après un « : »).";
            case "NameError": {
                Matcher m = Pattern.compile("name '(.+?)' is not defined").matcher(msg);
                String name = m.find() ? "« " + m.group(1) + " »" : "utilisée";
                return "La variable ou fonction " + name + " n'existe pas : vérifie l'orthographe "
                        + "(Python distingue majuscules et minuscules) et qu'elle est définie avant.";
            }
            case "TypeError":
                if (msg.contains("unsupported operand") || msg.contains("can only concatenate")
                        || msg.contains("must be str") || msg.contains("not supported between"))
                    return "Tu mélanges texte et nombres : input() renvoie du texte, "
                            + "convertis avec int(...) avant de calculer.";
                return "Un type ne convient pas à cette opération ou à cette fonction. "
                        + "Vérifie ce que tu lui passes.";
            case "ValueError":
                if (msg.contains("invalid literal for int()"))
                    return "int(...) ne peut pas convertir cette valeur : ce n'est pas un nombre entier "
                            + "(texte ? ligne vide ?).";
                return "Une valeur n'est pas acceptée par la fonction utilisée.";
            case "ZeroDivisionError":
                return "Division par zéro : vérifie le diviseur avant de diviser.";
            case "IndexError":
                return "Index hors limites : tu lis une position qui n'existe pas dans la liste ou le texte.";
            case "KeyError":
                return "Cette clé n'existe pas dans le dictionnaire.";
            case "AttributeError":
                return "Cet objet n'a pas cette méthode ou cet attribut : vérifie son type et l'orthographe.";
            case "EOFError":
                return "input() est appelé plus de fois qu'il n'y a de valeurs en entrée : "
                        + "compte tes input() par rapport à l'énoncé.";
            case "RecursionError":
                return "Récursion infinie : ta fonction s'appelle sans jamais s'arrêter.";
            default:
                return type.isEmpty() ? "Une erreur s'est produite pendant l'exécution."
                        : "Python signale : " + type + ".";
        }
    }

    /**
     * 3 progressive hints.
     * 1 = what went wrong, 2 = the mission's own hint, 3 = the example to work through.
     */
    public static String hint(int level, String verdict, String message, String missionHint,
                              String firstInput, String firstOutput) {
        switch (level) {
            case 1:
                if (verdict == null)
                    return "Relis l'énoncé, puis appuie sur « Tester » pour voir ce que fait ton code sur l'exemple.";
                switch (verdict) {
                    case "erreur":
                        return explain(message);
                    case "échoué":
                        return "Ton programme tourne, mais ce qu'il affiche n'est pas ce qui est attendu. "
                                + "Compare avec l'exemple : majuscules, espaces et retours à la ligne comptent.";
                    case "temps dépassé":
                        return "Ton programme ne se termine pas assez vite : cherche une boucle (while) "
                                + "qui n'a pas de condition d'arrêt.";
                    default:
                        return "Ta solution fonctionne sur les exemples : soumets-la pour valider la mission !";
                }
            case 2:
                return missionHint != null && !missionHint.trim().isEmpty() ? missionHint.trim()
                        : "Découpe le problème : lis les données avec input(), calcule le résultat, "
                        + "puis affiche-le avec print().";
            default:
                if (firstInput == null || firstOutput == null)
                    return "Fais marcher à la main un petit exemple, puis écris les étapes en Python.";
                return "Exemple à faire marcher d'abord : avec l'entrée « "
                        + firstInput.trim().replace("\n", " ↵ ") + " », ton programme doit afficher « "
                        + firstOutput.trim() + " ».";
        }
    }
}
