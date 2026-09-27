import java.util.*;

    static class RichiestaHttp {
        String ip;
        String path;
        int statusCode;
        long tempoRispostaMs;
        long timestamp;

        RichiestaHttp(String ip, String path, int statusCode, long tempoRispostaMs, long timestamp) {
            this.ip = ip;
            this.path = path;
            this.statusCode = statusCode;
            this.tempoRispostaMs = tempoRispostaMs;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return ip + " " + path + " [" + statusCode + "] " + tempoRispostaMs + "ms @" + timestamp;
        }
    }

  static final List<RichiestaHttp> storico = new ArrayList<>();

    static final LinkedList<RichiestaHttp> finestra = new LinkedList<>();

    static final Set<String> ipSospetti = new HashSet<>();

    static final TreeSet<Long> tempiDistinti = new TreeSet<>();

    static void ricevi(RichiestaHttp r) {
        storico.add(r);

        finestra.addLast(r);
        if (finestra.size() > 10) {
            finestra.removeFirst();
        }

        if (r.statusCode >= 400) {
            ipSospetti.add(r.ip);
        }

        tempiDistinti.add(r.tempoRispostaMs);
    }

    static List<RichiestaHttp> ultimeNConErrore500(int n) {
        List<RichiestaHttp> risultato = new ArrayList<>(n);
        for (int i = storico.size() - 1; i >= 0 && risultato.size() < n; i--) {
            RichiestaHttp r = storico.get(i);
            if (r.statusCode >= 500) {
                risultato.add(r);
            }
        }
        return risultato;
    }

    static Long percentile90Approssimato() {
        if (tempiDistinti.isEmpty()) return null;

        int n = tempiDistinti.size();
        int indiceTarget = (int) Math.ceil(n * 0.9) - 1;
        indiceTarget = Math.max(0, Math.min(indiceTarget, n - 1));

        Long corrente = tempiDistinti.first();
        for (int i = 0; i < indiceTarget; i++) {
            Long successivo = tempiDistinti.higher(corrente);
            if (successivo == null) break;
            corrente = successivo;
        }
        return corrente;
    }

    static void report() {
        System.out.println("=== REPORT FINALE ===");
        System.out.println("Richieste totali storiche: " + storico.size());

        List<RichiestaHttp> ultimiErrori500 = ultimeNConErrore500(5);
        System.out.println("\nUltime (fino a) 5 richieste con status >= 500 (" + ultimiErrori500.size() + "):");
        ultimiErrori500.forEach(r -> System.out.println("  " + r));

        System.out.println("\nFinestra scorrevole attuale (ultime " + finestra.size() + "):");
        finestra.forEach(r -> System.out.println("  " + r));

        long sospettiInFinestra = finestra.stream()
                .map(r -> r.ip)
                .distinct()
                .filter(ipSospetti::contains)
                .count();

        System.out.println("\nIP sospetti totali (errori 4xx/5xx): " + ipSospetti.size() + " -> " + ipSospetti);

        Long p90 = percentile90Approssimato();
        System.out.println("Tempi di risposta distinti registrati: " + tempiDistinti.size());
        System.out.println("Il tempo di risposta al 90° percentile (approssimato) è di " + p90 + " ms.");

        if (p90 != null) {
            SortedSet<Long> sopraP90 = tempiDistinti.tailSet(p90, false);
            System.out.println("Tempi distinti sopra il 90° percentile: " + sopraP90.size());
        }

        System.out.println("\n>> " + sospettiInFinestra + " IP sospetti hanno generato richieste tra le ultime "
                + finestra.size() + " in finestra; il tempo di risposta al 90° percentile è di " + p90 + " ms.");
    }

    public static void main(String[] args) {
        String[] ipPool = {"192.168.1.10", "192.168.1.11", "10.0.0.5", "10.0.0.6", "172.16.0.3"};
        String[] pathPool = {"/home", "/login", "/api/data", "/checkout", "/static/img.png"};
        int[] statusPool = {200, 200, 200, 404, 500, 200, 503, 200, 404, 200};

        Random rnd = new Random(42);
        long ts = 1_732_000_000L;

        for (int i = 0; i < 30; i++) {
            String ip = ipPool[rnd.nextInt(ipPool.length)];
            String path = pathPool[rnd.nextInt(pathPool.length)];
            int status = statusPool[rnd.nextInt(statusPool.length)];
            long tempo = 20 + rnd.nextInt(480);
            ts += 1 + rnd.nextInt(5);
            ricevi(new RichiestaHttp(ip, path, status, tempo, ts));
        }

        report();
    }
}
