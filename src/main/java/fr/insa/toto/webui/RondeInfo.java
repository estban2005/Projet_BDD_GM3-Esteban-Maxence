package fr.insa.toto.webui;

public class RondeInfo {
    public int id;
    public int numero;
    public boolean estTerminee;
    public int duree; // <-- AJOUTÉ

    public RondeInfo(int id, int numero, boolean estTerminee, int duree) {
        this.id = id;
        this.numero = numero;
        this.estTerminee = estTerminee;
        this.duree = duree; // <-- AJOUTÉ
    }
}