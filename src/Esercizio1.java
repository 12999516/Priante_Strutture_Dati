import java.util.*;
public class Esercizio1 {

    static class LetturaInvalidaException extends IllegalArgumentException {
        public LetturaInvalidaException(String message) {
            super(message);
        }
    }

    static class LetturaSensore {
        Double temperatura;
        Integer umiditaPercentuale;
        Long timestampUnix;
        Boolean batteriaScarica;
        @Override
        public String toString() {
            return "LetturaSensore{temp=" + temperatura + ", umid=" + umiditaPercentuale
                    + ", ts=" + timestampUnix + ", battLow=" + batteriaScarica + "}";
        }
    }

    static Optional<LetturaSensore> parsePacchetto(String raw) {
        if (raw == null || raw.isBlank()) {
            System.out.println("[ERRORE] Pacchetto vuoto o nullo, scartato.");
            return Optional.empty();
        }

        LetturaSensore l = new LetturaSensore();

        for (String campo : raw.split(";")) {
            String[] kv = campo.split("=", 2);
            if (kv.length != 2) {
                System.out.println("[ERRORE] Campo malformato ignorato: '" + campo + "'");
                continue;
            }
            String chiave = kv[0].trim();
            String valore = kv[1].trim();

            try {
                switch (chiave) {
                    case "temp":
                        l.temperatura = Double.valueOf(valore);
                        break;
                    case "umid":
                        int umid = Integer.parseInt(valore);
                        if (umid < 0 || umid > 100) {
                            throw new LetturaInvalidaException("umid fuori range (0-100): " + umid);
                        }
                        l.umiditaPercentuale = umid;
                        break;
                    case "ts":
                        l.timestampUnix = Long.valueOf(valore);
                        break;
                    case "batt_low":
                        l.batteriaScarica = Boolean.valueOf(valore);
                        break;
                    default:
                        System.out.println("[WARN] Campo sconosciuto ignorato: '" + chiave + "'");
                }
            } catch (NumberFormatException e) {
                System.out.println("[ERRORE] Valore non numerico per '" + chiave + "'='" + valore
                        + "': " + e.getMessage());
            }
        }

        return Optional.of(l);
    }

    static boolean confrontaBatteriaConDoppioUguale(Integer a, Integer b) {
        return a == b;
    }

    static boolean confrontaBatteriaCorretto(Integer a, Integer b) {
        return Objects.equals(a, b);
    }

    public static void main(String[] args) {
        String[] pacchetti = {
                "temp=23.5;umid=61;ts=1732000000;batt_low=false",
                "temp=xx.xx;umid=45;ts=1732000100;batt_low=true",
                "umid=150;ts=1732000200;batt_low=false",
                "temp=19.2;ts=1732000300;batt_low=true",
                "temp=21.0;umid=70;batt_low=false",
                "temp=;umid=55;ts=1732000500;batt_low=true",
                "temp=18.4;umid=40;ts=1732000600",
                "",
        };

        List<LetturaSensore> valide = new ArrayList<>();
        int erroriParsing = 0;

        for (String raw : pacchetti) {
            try {
                Optional<LetturaSensore> opt = parsePacchetto(raw);
                if (opt.isPresent()) {
                    valide.add(opt.get());
                } else {
                    erroriParsing++;
                }
            } catch (LetturaInvalidaException e) {
                System.out.println("[SCARTATO] " + e.getMessage());
                erroriParsing++;
            }
        }

        double sommaTemp = 0;
        int countTemp = 0;
        for (LetturaSensore l : valide) {
            if (l.temperatura != null) {
                sommaTemp += l.temperatura;
                countTemp++;
            }
        }
        double media = countTemp > 0 ? sommaTemp / countTemp : 0.0;

        System.out.println("\n=== REPORT ===");
        System.out.println("Pacchetti totali: " + pacchetti.length);
        System.out.println("Letture create con successo: " + valide.size());
        System.out.println("Errori di parsing/scarti: " + erroriParsing);
        System.out.println("Media temperature valide: " + media + " (" + countTemp + " letture con temperatura non nulla)");

        System.out.println("\n=== TEST AUTOBOXING / INTEGER CACHE ===");
        Integer b1a = 100, b1b = 100;
        Integer b2a = 200, b2b = 200;
        System.out.println("100 == 100 (in cache)      -> " + confrontaBatteriaConDoppioUguale(b1a, b1b) + " (atteso: true)");
        System.out.println("200 == 200 (fuori cache)   -> " + confrontaBatteriaConDoppioUguale(b2a, b2b) + " (atteso: false, è il bug!)");
        System.out.println("equals -> 100 vs 100       -> " + confrontaBatteriaCorretto(b1a, b1b));
        System.out.println("equals -> 200 vs 200       -> " + confrontaBatteriaCorretto(b2a, b2b));

        valide.sort((l1, l2) -> {
            if (l1.temperatura == null && l2.temperatura == null) return 0;
            if (l1.temperatura == null) return 1;
            if (l2.temperatura == null) return -1;
            return Double.compare(l2.temperatura, l1.temperatura);
        });

        System.out.println("\n=== LETTURE ORDINATE PER TEMPERATURA DECRESCENTE (bonus) ===");
        valide.forEach(System.out::println);
    }
}
