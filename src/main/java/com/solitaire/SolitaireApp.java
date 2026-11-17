package solitaire;

import javax.swing.SwingUtilities;

public final class SolitaireApp {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--simulate")) {
            int games = args.length > 1 ? Integer.parseInt(args[1]) : 500;
            runSimulations(games);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SolitaireGame game = new SolitaireGame();
            SolitaireUI ui = new SolitaireUI(game);
            ui.setVisible(true);
        });
    }

    public static void runSimulations(int totalGames) {
        int wins = 0;
        long totalScore = 0;

        for (int i = 0; i < totalGames; i++) {
            SolitaireGame game = new SolitaireGame();
            while (!game.isWon() && game.playAutoStep()) {}
            if (game.isWon()) wins++;
            totalScore += game.getScore();
        }

        double winRate = (100.0 * wins) / totalGames;
        double avgScore = (double) totalScore / totalGames;
        System.out.printf("Partidas: %d | Vitórias: %d (%.2f%%) | Pontuação média: %.2f / 52%n",
                totalGames, wins, winRate, avgScore);
    }
}
