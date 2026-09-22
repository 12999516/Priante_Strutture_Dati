import static java.lang.IO.print;
import static java.lang.IO.println;

void main (){

//    int elementi = 100_000;
//
    //  ArrayList<Integer> arrayList = new ArrayList<>();
//    LinkedList<Integer> list = new LinkedList<>();
//
//    long start = System.nanoTime();

//    for (int i = 0; i < elementi; i++) {
//        arrayList.add(i);
//    }

//    long end = System.nanoTime();
//    println("tempo array list " + (end - start) / 1_000_000.0 + "ms");

//    long start2 = System.nanoTime();

/*    for (int i = 0; i < elementi; i++) {
        list.add(i);
    }

    long end1 = System.nanoTime();
    println("tempo list " + (end1 - start2) / 1_000_000.0 + "ms");

    start2 = System.nanoTime();

    for (int i = 0; i < 1000; i++) {
        arrayList.get(elementi/2);
    }

    end1 = System.nanoTime();
    println("tempo array accesso al centro " + (end1 - start2) / 1_000_000.0 + "ms");

    start2 = System.nanoTime();

    for (int i = 0; i < 1000; i++) {
        list.get(elementi/2);
    }

    end1 = System.nanoTime();
    println("tempo list accesso al centro " + (end1 - start2) / 1_000_000.0 + "ms");*/

    /*String chiave = "utente_barin_2008";
    int numeroBucket = 16;

    int Hashcode = chiave.hashCode();
    int indice_bucket = Math.abs(Hashcode) % numeroBucket;
    println("chiave " + chiave + " hascode " + Hashcode +" indice bucket "+ indice_bucket);*/

    System.gc();

    int elementi = 90_000_000;
    int target = elementi - 1;

  //  List<Integer> lista = new ArrayList<>();
    Set<Integer> treeset = new TreeSet<>();
    Set<Integer> hash = new HashSet<>();

    for (int i = 0; i < elementi; i++) {
        treeset.add(i);
        hash.add(i);
    }

     long start = System.nanoTime();
     boolean trovato = treeset.contains(target);
     long end = System.nanoTime();
     long tempo = (end - start) / 1_000_000;
    println("treeset O(n) Trovato" + trovato + " Tempo " + tempo);

     start = System.nanoTime();
     trovato = hash.contains(target);
     end = System.nanoTime();
     tempo = (end - start) / 1_000_000;
    println("hash O(n) Trovato" + trovato + " Tempo " + tempo);
}