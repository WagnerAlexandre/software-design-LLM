package com.solitaire;

import com.solitaire.actions.Command;
import com.solitaire.engine.GameEngine;

public class Main {
    public static void main(String[] args) {
        GameEngine e = new GameEngine();
        e.newGame(42L);                       // seed fixa ⇒ saída idêntica sempre

        System.out.println("=== Estado inicial (seed 42) ===");
        e.view().serialized().forEach(System.out::println);
        System.out.println("score=" + e.score() + " won=" + e.isWon());
        System.out.println("history=" + e.history().size());

        System.out.println("\n=== 5 jogadas automáticas ===");
        for (int i = 0; i < 5; i++) {
            Command c = e.playAuto();
            System.out.println(i + ": " + c.describe() + "  score=" + e.score());
            if (c.isNull()) break;
        }

        System.out.println("\n=== Undo total ===");
        while (e.history().canUndo()) e.undo();
        e.view().serialized().forEach(System.out::println);
        System.out.println("history=" + e.history().size());
    }
}