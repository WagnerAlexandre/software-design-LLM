package com.solitaire.model;

import java.util.List;

/** Fachada read-only para estratégia e interface (RF27/RNF04). */
public interface ReadOnlyGameState {
    Pile stock();
    Pile waste();
    Pile foundation(int i);
    Column column(int i);
    Pile pileAt(Slot slot);
    int score();
    boolean isWon();
    List<String> serialized();
}