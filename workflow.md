# Relazione

## Esercizio 1 — Wrapper classes

**Perché wrapper e non primitivi.** Ogni campo di `LetturaSensore` può essere assente
perché il sensore non lo ha inviato. Un tipo primitivo non ha modo di rappresentare
"assente": un `double` assente andrebbe codificato con un sentinella (`-1`, `NaN`...),
che è ambiguo perché quei valori potrebbero anche essere letture legittime altrove nel
sistema, e un `boolean` non può distinguere "batteria non scarica" da "non sappiamo".
Il wrapper introduce un vero terzo stato (`null`) verificabile esplicitamente, quindi
non è un vezzo stilistico ma l'unico modo corretto di modellare un dato opzionale.

**`Integer.parseInt` vs `Integer.valueOf`.** `parseInt` restituisce un `int` primitivo;
`valueOf` restituisce un `Integer` (wrapper) e per valori tra -128 e 127 riusa un
oggetto della *Integer Cache* invece di allocarne uno nuovo. Nel parsing di `umid`
usiamo `parseInt` perché ci serve il valore primitivo subito per il controllo di range,
prima di decidere se il pacchetto è valido; se avessimo boxato subito con `valueOf` e
poi scartato il pacchetto per range invalido, avremmo fatto una boxing inutile.

**Integer Cache (trabocchetto `==`).** Due `Integer` con lo stesso valore in -128..127
sono spesso lo stesso oggetto in memoria (cache), quindi `==` restituisce `true`; fuori
da quel range la JVM crea due oggetti distinti anche per lo stesso valore numerico, e
`==` restituisce `false`. È un bug classico perché `==` confronta riferimenti, non
contenuto: la correzione è usare `.equals()` o `Objects.equals()` (quest'ultimo gestisce
anche il caso in cui uno dei due sia `null`).

**Sottrazione manuale tra `double` (bonus).** Ordinare "a mano" con `d1 - d2` è
pericoloso perché con i double la sottrazione può generare `NaN` o `Infinity` con
valori estremi, perde precisione con numeri molto vicini, e il segno del risultato non
è sempre garantito coerente per differenze piccolissime (arrotondamenti in virgola
mobile). `Double.compare` gestisce correttamente anche `NaN`, `+0.0`/`-0.0` e gli
infiniti, quindi è sempre la scelta corretta per confrontare wrapper numerici.

---

## Esercizio 2 — Collezioni

### Tabella comparativa (struttura → operazione critica → complessità)

| Struttura scelta | Operazione critica che giustifica la scelta | Complessità nella struttura scelta | Complessità nell'alternativa scartata |
|---|---|---|---|
| `ArrayList<RichiestaHttp>` (storico) | Scorrere dal fondo per le ultime N richieste con status ≥ 500 | `get(i)` O(1), scan a ritroso O(k) dove k = elementi esaminati | `LinkedList`: `get(i)` O(n) per accesso indizzato casuale, quindi scorrere a ritroso per indice sarebbe O(n²) nel caso peggiore |
| `LinkedList<RichiestaHttp>` (finestra) | `addLast()` + `removeFirst()` ad ogni nuova richiesta | O(1) per entrambe (operazioni sui nodi di testa/coda) | `ArrayList`: `add()` in coda O(1) ammortizzato, ma `remove(0)` è O(n) per lo shift di tutti gli elementi |
| `HashSet<String>` (IP sospetti) | `contains()` / `add()` per verificare/registrare un IP sospetto | O(1) medio | `ArrayList`: `contains()` O(n) e nessuna garanzia di unicità senza un controllo manuale aggiuntivo |
| `TreeSet<Long>` (tempi distinti) | Navigazione ordinata (`first`, `higher`, `tailSet`) per il percentile 90 | O(log n) per singola operazione di navigazione, insieme sempre ordinato | `ArrayList` + sort manuale: O(n log n) da rifare ogni volta che serve una nuova statistica, oltre a dover gestire i duplicati a mano |

### Note aggiuntive
- `LinkedList` è giustificata *solo* perché la finestra fa scarti continui in testa;
  se avessimo avuto solo bisogno di leggere gli ultimi 10 elementi senza rimuoverli,
  un `ArrayDeque` o persino un `ArrayList` con indice circolare sarebbero stati
  altrettanto validi (e più efficienti in cache) — ma qui l'esercizio chiedeva
  esplicitamente `addLast`/`removeFirst` su `LinkedList`.
- Il percentile 90 è "approssimato" perché naviga i **tempi distinti**, non tutte le
  occorrenze: è coerente con la richiesta di lavorare sui valori univoci del `TreeSet`,
  ma non è lo stesso risultato di un percentile calcolato sull'intero multiset di dati.

---

## Esercizio 3 — File I/O + PriorityQueue

**Lettura file: `Files.readAllLines` vs `BufferedReader`.** Per un file di dimensioni
contenute come un log giornaliero di ticket, `Files.readAllLines` (java.nio.file) è
più semplice: restituisce direttamente una `List<String>` indicizzabile, comoda per
riportare il numero di riga nei messaggi di errore, e la gestione delle eccezioni
(`NoSuchFileException`, `IOException`) è uniforme. Per file molto grandi si
preferirebbe `BufferedReader`/`Files.lines()` per lo streaming riga-per-riga, senza
caricare tutto il contenuto in memoria contemporaneamente.

**`compareTo` di `Ticket`.** La priorità reale non è l'ordine di arrivo nel file né
l'ordine "naturale" implicito dell'enum, ma: prima il livello dichiarato (più urgente
= rango più basso), poi — a parità di livello — il ticket con `timestampArrivo` più
vecchio (FIFO interno al livello). Abbiamo aggiunto un campo `rango` esplicito
all'enum invece di affidarci a `ordinal()` per non dipendere in modo fragile
dall'ordine di dichiarazione dei valori dell'enum.

**Righe malformate.** Vengono rilevate tre categorie di errore (numero di campi
sbagliato, livello sconosciuto, timestamp non numerico) e per ciascuna viene
aggiunta una riga di log con il numero di riga originale; l'elaborazione del resto
del file continua normalmente. Il livello viene normalizzato in maiuscolo prima del
parsing, così "critico"/"Critico"/"CRITICO" sono tutti accettati.

**Pausa obbligatoria e coda ausiliaria (bonus).** Il contatore di ticket CRITICI/ALTI
consecutivi si azzera non appena viene lavorato un ticket MEDIO o BASSO, coerentemente
con l'idea che la pausa serva a scaricare la tensione da ticket urgenti consecutivi.
Per l'allarme sui CRITICI in attesa usiamo una seconda `PriorityQueue` (ordinata per
timestamp) invece di un semplice contatore, perché così l'allarme può anche indicare
subito qual è il ticket critico più vecchio ancora non lavorato, informazione utile
per un intervento manuale immediato.
