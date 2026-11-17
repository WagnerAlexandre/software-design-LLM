package solitaire;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class SolitaireUI extends JFrame implements GameListener {
    private static final int CARD_W = 92;
    private static final int CARD_H = 128;
    private static final int FACE_DOWN_OFFSET = 18;
    private static final int FACE_UP_OFFSET = 28;

    private final SolitaireGame game;

    private final StockPanel stockPanel;
    private final PilePanel wastePanel;
    private final PilePanel[] foundationPanels = new PilePanel[4];
    private final TableauPanel[] tableauPanels = new TableauPanel[7];
    private final List<PilePanel> dropTargets = new ArrayList<>();

    private final JButton undoButton = new JButton("Desfazer");
    private final JButton autoStepButton = new JButton("Jogada Automática");
    private final JToggleButton autoToggle = new JToggleButton("Auto-Play: Ativo", true);
    private final JButton newGameButton = new JButton("Nova Partida");

    private final DragGlassPane dragGlassPane = new DragGlassPane();

    // Estado do arraste em andamento
    private LocationId pendingSource;
    private Card pendingCard;
    private List<Card> pendingSequence = List.of();
    private Point pressGlassPoint;
    private Point dragGrabOffset;

    private boolean isDragging = false;
    private String activeSerializedPacket;
    private BufferedImage activeDragImage;
    private Point currentDragGlassPoint;

    private boolean victoryShown = false;

    public SolitaireUI(SolitaireGame game) {
        super("Solitaire (Klondike)");
        this.game = game;
        this.game.addListener(this);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setGlassPane(dragGlassPane);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(newGameButton);
        toolbar.addSeparator();
        toolbar.add(undoButton);
        toolbar.addSeparator();
        toolbar.add(autoStepButton);
        toolbar.add(autoToggle);
        add(toolbar, BorderLayout.NORTH);

        newGameButton.addActionListener(e -> {
            victoryShown = false;
            game.newGame();
        });
        undoButton.addActionListener(e -> game.undo());
        autoStepButton.addActionListener(e -> game.playAutoStep());
        autoToggle.addActionListener(e -> {
            boolean enabled = autoToggle.isSelected();
            game.setAutoPlayEnabled(enabled);
            autoToggle.setText(enabled ? "Auto-Play: Ativo" : "Auto-Play: Desativado");
        });

        JPanel board = new JPanel(new BorderLayout(16, 16));
        board.setBackground(new Color(22, 102, 54));
        board.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel topRow = new JPanel(new GridLayout(1, 7, 14, 0));
        topRow.setOpaque(false);

        stockPanel = new StockPanel();
        wastePanel = new PilePanel(LocationId.waste(), "DESCARTE", false);

        topRow.add(stockPanel);
        topRow.add(wastePanel);
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        topRow.add(spacer);

        for (int i = 0; i < 4; i++) {
            foundationPanels[i] = new PilePanel(LocationId.foundation(i), "FUNDAÇÃO", true);
            topRow.add(foundationPanels[i]);
            dropTargets.add(foundationPanels[i]);
        }

        JPanel tableauRow = new JPanel(new GridLayout(1, 7, 14, 0));
        tableauRow.setOpaque(false);
        for (int i = 0; i < 7; i++) {
            tableauPanels[i] = new TableauPanel(i);
            tableauRow.add(tableauPanels[i]);
            dropTargets.add(tableauPanels[i]);
        }

        board.add(topRow, BorderLayout.NORTH);
        board.add(tableauRow, BorderLayout.CENTER);
        add(board, BorderLayout.CENTER);

        setSize(820, 680);
        setLocationRelativeTo(null);
        refreshUI();
    }

    @Override
    public void onGameStateChanged() {
        SwingUtilities.invokeLater(this::refreshUI);
    }

    private void refreshUI() {
        // RF03: Pontuação não aparece na interface
        undoButton.setEnabled(game.canUndo());
        autoStepButton.setEnabled(game.isAutoPlayEnabled() && !game.isWon());
        repaint();

        if (game.isWon() && !victoryShown) {
            victoryShown = true;
            JOptionPane.showMessageDialog(
                    this,
                    "Parabéns! Todas as 52 cartas chegaram às fundações.",
                    "Vitória!",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void clearAllHighlights() {
        wastePanel.setHighlight(null);
        for (PilePanel f : foundationPanels) f.setHighlight(null);
        for (TableauPanel t : tableauPanels) t.setHighlight(null);
    }

    private void tryAutoFoundation(LocationId source, Card card) {
        if (card == null) return;
        for (int f = 0; f < 4; f++) {
            LocationId target = LocationId.foundation(f);
            if (game.canMove(source, card, target)) {
                game.move(source, card, target);
                return;
            }
        }
    }

    // RNF05: Serialização e desserialização fiel de origem + sequência
    private static String serializePacket(LocationId source, List<Card> sequence) {
        return source.getType().name() + ":" + source.getIndex()
                + "|" + Card.serializeSequence(sequence);
    }

    private static ParsedPacket deserializePacket(String raw) {
        String[] parts = raw.split("\\|", 2);
        String[] locParts = parts[0].split(":");
        LocationId.Type type = LocationId.Type.valueOf(locParts[0]);
        int idx = Integer.parseInt(locParts[1]);
        LocationId loc = switch (type) {
            case STOCK -> LocationId.stock();
            case WASTE -> LocationId.waste();
            case FOUNDATION -> LocationId.foundation(idx);
            case TABLEAU -> LocationId.tableau(idx);
        };
        List<Card> cards = Card.deserializeSequence(parts.length > 1 ? parts[1] : "");
        return new ParsedPacket(loc, cards);
    }

    private record ParsedPacket(LocationId source, List<Card> cards) {}
    private record DragSelection(List<Card> sequence, Point grabOffset) {}

    private PilePanel findDropTargetAt(Point glassPoint) {
        for (PilePanel target : dropTargets) {
            Point local = SwingUtilities.convertPoint(dragGlassPane, glassPoint, target);
            if (target.contains(local)) {
                return target;
            }
        }
        return null;
    }

    private void handleMousePressed(PilePanel sourcePanel, MouseEvent e) {
        DragSelection selection = sourcePanel.getDragSelectionAt(e.getPoint());
        if (selection == null || selection.sequence().isEmpty()) {
            resetDragState();
            return;
        }
        pendingSource = sourcePanel.locationId;
        pendingSequence = selection.sequence();
        pendingCard = pendingSequence.get(0);
        dragGrabOffset = selection.grabOffset();
        pressGlassPoint = SwingUtilities.convertPoint(sourcePanel, e.getPoint(), dragGlassPane);
    }

    private void handleMouseDragged(PilePanel sourcePanel, MouseEvent e) {
        if (pendingSource == null || pendingSequence.isEmpty()) {
            return;
        }
        Point gpPoint = SwingUtilities.convertPoint(sourcePanel, e.getPoint(), dragGlassPane);

        if (!isDragging) {
            if (pressGlassPoint == null || gpPoint.distance(pressGlassPoint) <= 4) {
                return;
            }
            isDragging = true;
            activeSerializedPacket = serializePacket(pendingSource, pendingSequence);
            ParsedPacket packet = deserializePacket(activeSerializedPacket);
            activeDragImage = renderDragImage(packet.cards());
            dragGlassPane.setVisible(true);
        }

        currentDragGlassPoint = gpPoint;
        clearAllHighlights();

        PilePanel hovered = findDropTargetAt(gpPoint);
        if (hovered != null && !hovered.locationId.equals(pendingSource)) {
            // RF21 & RNF03: Interface consulta diretamente as regras do jogo durante o arraste
            boolean legal = game.canMove(pendingSource, pendingCard, hovered.locationId);
            hovered.setHighlight(legal);
        }

        dragGlassPane.repaint();
    }

    private void handleMouseReleased(PilePanel sourcePanel, MouseEvent e) {
        if (isDragging) {
            Point gpPoint = SwingUtilities.convertPoint(sourcePanel, e.getPoint(), dragGlassPane);
            PilePanel hovered = findDropTargetAt(gpPoint);
            if (hovered != null && !hovered.locationId.equals(pendingSource) && activeSerializedPacket != null) {
                ParsedPacket packet = deserializePacket(activeSerializedPacket);
                if (!packet.cards().isEmpty()) {
                    game.move(packet.source(), packet.cards().get(0), hovered.locationId);
                }
            }
        } else if (e.getClickCount() == 2 && pendingSource != null && pendingCard != null) {
            tryAutoFoundation(pendingSource, pendingCard);
        }
        resetDragState();
    }

    private void resetDragState() {
        pendingSource = null;
        pendingCard = null;
        pendingSequence = List.of();
        pressGlassPoint = null;
        dragGrabOffset = null;
        isDragging = false;
        activeSerializedPacket = null;
        activeDragImage = null;
        currentDragGlassPoint = null;
        clearAllHighlights();
        dragGlassPane.setVisible(false);
        repaint();
    }

    private static void paintCard(Graphics2D g2, Card card, int x, int y, boolean faceUp) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!faceUp) {
            g2.setColor(new Color(30, 75, 155));
            g2.fillRoundRect(x, y, CARD_W, CARD_H, 12, 12);
            g2.setColor(Color.WHITE);
            g2.drawRoundRect(x, y, CARD_W, CARD_H, 12, 12);
            g2.drawRoundRect(x + 6, y + 6, CARD_W - 12, CARD_H - 12, 8, 8);
            return;
        }

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, CARD_W, CARD_H, 12, 12);
        g2.setColor(new Color(60, 60, 60));
        g2.drawRoundRect(x, y, CARD_W, CARD_H, 12, 12);

        Color ink = card.getSuit().isRed() ? new Color(195, 25, 25) : new Color(25, 25, 25);
        g2.setColor(ink);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(card.getRank().getLabel(), x + 8, y + 20);
        g2.drawString(card.getSuit().getSymbol(), x + 8, y + 38);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 34));
        FontMetrics fm = g2.getFontMetrics();
        String sym = card.getSuit().getSymbol();
        int cx = x + (CARD_W - fm.stringWidth(sym)) / 2;
        int cy = y + (CARD_H + fm.getAscent()) / 2 - 4;
        g2.drawString(sym, cx, cy);
    }

    private static BufferedImage renderDragImage(List<Card> sequence) {
        int h = CARD_H + (sequence.size() - 1) * FACE_UP_OFFSET;
        BufferedImage img = new BufferedImage(CARD_W + 4, h + 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        for (int i = 0; i < sequence.size(); i++) {
            paintCard(g2, sequence.get(i), 2, 2 + i * FACE_UP_OFFSET, true);
        }
        g2.dispose();
        return img;
    }

    private final class DragGlassPane extends JComponent {
        DragGlassPane() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!isDragging || activeDragImage == null || currentDragGlassPoint == null || dragGrabOffset == null) {
                return;
            }
            int drawX = currentDragGlassPoint.x - dragGrabOffset.x;
            int drawY = currentDragGlassPoint.y - dragGrabOffset.y;
            g.drawImage(activeDragImage, drawX, drawY, null);
        }
    }

    private final class StockPanel extends JPanel {
        StockPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(CARD_W + 6, CARD_H + 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (game.canDraw()) {
                        game.draw(); // RF11: Compra do monte para o descarte
                    } else {
                        // RF01: Reaproveita ação de comprar quando monte está vazio
                        victoryShown = false;
                        game.newGame();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            if (game.canDraw()) {
                paintCard(g2, null, 2, 2, false);
            } else {
                g2.setColor(new Color(255, 255, 255, 130));
                g2.drawRoundRect(2, 2, CARD_W, CARD_H, 12, 12);
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.drawString("NOVA", 30, 62);
                g2.drawString("PARTIDA", 21, 78);
            }
        }
    }

    private class PilePanel extends JPanel {
        protected final LocationId locationId;
        private final String placeholder;
        private Boolean highlightValid = null;

        PilePanel(LocationId locationId, String placeholder, boolean acceptsDrops) {
            this.locationId = locationId;
            this.placeholder = placeholder;
            setOpaque(false);
            setPreferredSize(new Dimension(CARD_W + 6, CARD_H + 6));

            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        handleMousePressed(PilePanel.this, e);
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        handleMouseDragged(PilePanel.this, e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        handleMouseReleased(PilePanel.this, e);
                    }
                }
            };
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }

        void setHighlight(Boolean valid) {
            if (this.highlightValid != valid) {
                this.highlightValid = valid;
                repaint();
            }
        }

        protected void paintHighlight(Graphics2D g2, int height) {
            if (highlightValid != null) {
                Stroke old = g2.getStroke();
                g2.setStroke(new BasicStroke(3f));
                g2.setColor(highlightValid ? new Color(80, 240, 120) : new Color(240, 80, 80));
                g2.drawRoundRect(1, 1, CARD_W + 2, height + 2, 14, 14);
                g2.setStroke(old);
            }
        }

        protected DragSelection getDragSelectionAt(Point p) {
            Card top = game.peekLocation(locationId);
            if (top == null) return null;
            if (p.x < 2 || p.x > 2 + CARD_W || p.y < 2 || p.y > 2 + CARD_H) {
                return null;
            }
            return new DragSelection(List.of(top), new Point(p.x - 2, p.y - 2));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Card top = game.peekLocation(locationId);
            if (top == null) {
                g2.setColor(new Color(255, 255, 255, 90));
                g2.drawRoundRect(2, 2, CARD_W, CARD_H, 12, 12);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                g2.drawString(placeholder, 16, CARD_H / 2);
            } else {
                paintCard(g2, top, 2, 2, true);
            }
            paintHighlight(g2, CARD_H);
        }
    }

    private final class TableauPanel extends PilePanel {
        private final int columnIndex;

        TableauPanel(int columnIndex) {
            super(LocationId.tableau(columnIndex), "REI", true);
            this.columnIndex = columnIndex;
        }

        @Override
        protected DragSelection getDragSelectionAt(Point p) {
            List<Card> cards = game.getLocationCards(locationId);
            if (cards.isEmpty() || p.x < 2 || p.x > 2 + CARD_W) {
                return null;
            }

            int y = 2;
            Card chosen = null;
            int chosenTopY = 2;

            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                boolean visible = game.isCardVisibleInTableau(columnIndex, card);
                int bottom = (i == cards.size() - 1)
                        ? y + CARD_H
                        : y + (visible ? FACE_UP_OFFSET : FACE_DOWN_OFFSET);

                if (p.y >= y && p.y <= bottom && visible) {
                    chosen = card;
                    chosenTopY = y;
                }
                y += visible ? FACE_UP_OFFSET : FACE_DOWN_OFFSET;
            }

            if (chosen == null) return null;
            List<Card> seq = game.getTableauSequenceFrom(columnIndex, chosen);
            if (seq.isEmpty()) return null;
            return new DragSelection(seq, new Point(p.x - 2, p.y - chosenTopY));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            List<Card> cards = game.getLocationCards(locationId);
            if (cards.isEmpty()) {
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawRoundRect(2, 2, CARD_W, CARD_H, 12, 12);
                paintHighlight(g2, CARD_H);
                return;
            }

            int y = 2;
            for (int i = 0; i < cards.size(); i++) {
                Card card = cards.get(i);
                boolean visible = game.isCardVisibleInTableau(columnIndex, card);
                paintCard(g2, card, 2, y, visible);
                if (i < cards.size() - 1) y += visible ? FACE_UP_OFFSET : FACE_DOWN_OFFSET;
            }
            paintHighlight(g2, y + CARD_H - 2);
        }
    }
}
