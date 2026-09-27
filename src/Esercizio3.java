import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class Esercizio3 {

    enum Livello {
        CRITICO(0, 15),
        ALTO(1, 30),
        MEDIO(2, 60),
        BASSO(3, 120);

        final int rango;
        final int minutiLavorazione;

        Livello(int rango, int minutiLavorazione) {
            this.rango = rango;
            this.minutiLavorazione = minutiLavorazione;
        }
    }

    static class Ticket implements Comparable<Ticket> {
        final String id;
        final String descrizione;
        final Livello livello;
        final long timestampArrivo;

        Ticket(String id, String descrizione, Livello livello, long timestampArrivo) {
            this.id = id;
            this.descrizione = descrizione;
            this.livello = livello;
            this.timestampArrivo = timestampArrivo;
        }

        @Override
        public int compareTo(Ticket altro) {
            int cmpLivello = Integer.compare(this.livello.rango, altro.livello.rango);
            if (cmpLivello != 0) return cmpLivello;
            return Long.compare(this.timestampArrivo, altro.timestampArrivo);
        }

        @Override
        public String toString() {
            return id + " [" + livello + "] " + descrizione + " (arrivo=" + timestampArrivo + ")";
        }
    }

   static List<Ticket> leggiTicket(Path percorso, List<String> logErrori) throws IOException {
        List<Ticket> ticket = new ArrayList<>();

        List<String> righe;
        try {
            righe = Files.readAllLines(percorso, StandardCharsets.UTF_8);
        } catch (NoSuchFileException e) {
            throw new IOException("File non trovato: " + percorso.toAbsolutePath(), e);
        }

        boolean primaRiga = true;
        int numeroRiga = 0;
        for (String riga : righe) {
            numeroRiga++;
            if (primaRiga) {
                primaRiga = false;
                continue;
            }
            if (riga.isBlank()) continue;

            String[] campi = riga.split(",", -1);
            if (campi.length != 4) {
                logErrori.add("Riga " + numeroRiga + ": numero di campi errato -> '" + riga + "'");
                continue;
            }

            String id = campi[0].trim();
            String descrizione = campi[1].trim();
            String livelloStr = campi[2].trim().toUpperCase();
            String tsStr = campi[3].trim();

            if (id.isEmpty() || descrizione.isEmpty()) {
                logErrori.add("Riga " + numeroRiga + ": campo obbligatorio vuoto -> '" + riga + "'");
                continue;
            }

            Livello livello;
            try {
                livello = Livello.valueOf(livelloStr);
            } catch (IllegalArgumentException e) {
                logErrori.add("Riga " + numeroRiga + ": livello sconosciuto '" + livelloStr + "' -> '" + riga + "'");
                continue;
            }

            long timestamp;
            try {
                timestamp = Long.parseLong(tsStr);
            } catch (NumberFormatException e) {
                logErrori.add("Riga " + numeroRiga + ": timestamp non numerico '" + tsStr + "' -> '" + riga + "'");
                continue;
            }

            ticket.add(new Ticket(id, descrizione, livello, timestamp));
        }

        return ticket;
    }

    public static void main(String[] args) {
        Path input = Path.of("ticket.csv");
        Path output = Path.of("report_lavorazione.txt");
        List<String> logErrori = new ArrayList<>();

        List<Ticket> tuttiITicket;
        try {
            tuttiITicket = leggiTicket(input, logErrori);
        } catch (IOException e) {
            System.err.println("Errore fatale nella lettura del file ticket: " + e.getMessage());
            return;
        }

        if (!logErrori.isEmpty()) {
            System.out.println("=== RIGHE SCARTATE (" + logErrori.size() + ") ===");
            logErrori.forEach(System.out::println);
            System.out.println();
        }

        PriorityQueue<Ticket> coda = new PriorityQueue<>(tuttiITicket);


        PriorityQueue<Ticket> criticiInAttesa =
                new PriorityQueue<>(Comparator.comparingLong(t -> t.timestampArrivo));
        for (Ticket t : tuttiITicket) {
            if (t.livello == Livello.CRITICO) {
                criticiInAttesa.add(t);
            }
        }

        List<String> righeReport = new ArrayList<>();
        Map<Livello, Integer> conteggioPerLivello = new EnumMap<>(Livello.class);
        for (Livello l : Livello.values()) conteggioPerLivello.put(l, 0);

        long tempoCumulativoMin = 0;
        int consecutiviCriticiAlti = 0;
        int ordineNumero = 0;

        while (!coda.isEmpty()) {
            Ticket t = coda.poll();
            ordineNumero++;

            if (t.livello == Livello.CRITICO) {
                criticiInAttesa.remove(t);
            }

            tempoCumulativoMin += t.livello.minutiLavorazione;
            conteggioPerLivello.merge(t.livello, 1, Integer::sum);
            righeReport.add(ordineNumero + ". " + t + " -> completato al minuto " + tempoCumulativoMin);

            if (t.livello == Livello.CRITICO || t.livello == Livello.ALTO) {
                consecutiviCriticiAlti++;
                if (consecutiviCriticiAlti >= 5) {
                    tempoCumulativoMin += 10;
                    righeReport.add("   -- pausa obbligatoria di 10 minuti dopo 5 ticket CRITICI/ALTI consecutivi --");
                    consecutiviCriticiAlti = 0;
                }
            } else {
                consecutiviCriticiAlti = 0;
            }

            if (criticiInAttesa.size() > 2) {
                System.out.println("[ALLARME] " + criticiInAttesa.size()
                        + " ticket CRITICI ancora in attesa mentre si sta lavorando " + t.id
                        + " (il più vecchio in attesa: " + criticiInAttesa.peek() + ")");
            }
        }

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write("=== ORDINE DI LAVORAZIONE CONSIGLIATO ===");
            writer.newLine();
            for (String riga : righeReport) {
                writer.write(riga);
                writer.newLine();
            }
            writer.newLine();
            writer.write("=== RIEPILOGO ===");
            writer.newLine();
            writer.write("Totale ticket lavorati: " + tuttiITicket.size());
            writer.newLine();
            for (Livello l : Livello.values()) {
                writer.write("  " + l + ": " + conteggioPerLivello.get(l));
                writer.newLine();
            }
            writer.write("Tempo totale stimato (incluse pause): " + tempoCumulativoMin + " minuti");
            writer.newLine();

            if (!logErrori.isEmpty()) {
                writer.newLine();
                writer.write("Righe scartate durante la lettura (" + logErrori.size() + "):");
                writer.newLine();
                for (String err : logErrori) {
                    writer.write("  " + err);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella scrittura del report: " + e.getMessage());
            return;
        }

        System.out.println("Report scritto in: " + output.toAbsolutePath());
    }
}
