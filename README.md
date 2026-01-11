Partea 1
Proiectul simulează o bursă, unde mai mulți "traderi" plasează, modifică și execută tranzacții în paralel
Motorul bursei procesează tranzacțiile într-un singur thread central pentru a evita conflictele de sincronizare

Pentru compilare și rulare:
javac Main.java
java Main



Partea 2
Aplicatie distribuita java cu 2 servere(unul de exchange si unul ai) si 1 client.
Pentru rulare:
mvn spring-boot:run   (exchange server, pe port 8080)
mvn spring-boot:run   (ai-service, pe port 8081)

exemplu de input client: "distributed-stock-exchange example.txt"
