package org.jetuml.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Exibicao da Dica do Dia (RF43) e Guia de Atalhos e Ajuda (RF44).
 */
public final class TipsAndShortcutsDialog {

    private static final String[] DAILY_TIPS = {
            "Dica 1: Pressione as teclas numericas de 1 a 9 para selecionar rapidamente qualquer ferramenta da barra lateral.",
            "Dica 2: Use /texto/ entre barras para formatar membros abstratos em italico e _texto_ entre sublinhados para membros estaticos.",
            "Dica 3: Segure a tecla Shift enquanto arrasta uma Classe ou Interface sobre um Pacote para anexa-la ou remove-la.",
            "Dica 4: No Diagrama de Sequencia, arraste uma chamada ate o cabecalho de outro participante para gerar automaticamente um construtor <<create>>.",
            "Dica 5: Use Ctrl+B para copiar a imagem limpa do diagrama (sem grade) diretamente para a area de transferencia."
    };

    private static int tipCursor = 0;

    private TipsAndShortcutsDialog() {
    }

    public static void showTipOfTheDay() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Dica do Dia - JetUML");
        alert.setHeaderText("Produtividade na Modelagem UML");
        String tip = DAILY_TIPS[tipCursor % DAILY_TIPS.length];
        tipCursor++;
        alert.setContentText(tip);
        alert.showAndWait();
    }

    public static void showKeyboardShortcutsGuide() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Atalhos de Teclado e Ajuda - JetUML");
        alert.setHeaderText("Referencia Rapida de Comandos e Interacoes");

        String guide = String.join("\n",
                "ARQUIVO:",
                "  Ctrl + N         Novo Diagrama",
                "  Ctrl + O         Abrir Diagrama (.json / .jet)",
                "  Ctrl + S         Salvar Diagrama",
                "  Ctrl + Shift + S Salvar Como...",
                "  Ctrl + E         Exportar Imagem (SVG, PNG, JPG, GIF, BMP)",
                "  Ctrl + B         Copiar Imagem para Area de Transferencia",
                "  Ctrl + W         Fechar Aba Atual",
                "  Ctrl + 1..5      Abrir Arquivos Recentes",
                "",
                "EDICAO E SELECAO:",
                "  Ctrl + Z / Y     Desfazer (Undo) / Refazer (Redo)",
                "  Ctrl + X / C / V Recortar / Copiar / Colar Elementos",
                "  Ctrl + A         Selecionar Todos os Elementos",
                "  Delete / Backsp. Excluir Elementos Selecionados",
                "  Duplo Clique     Editar Propriedades do Elemento",
                "  Ctrl + Clique    Alternar Selecao Individual",
                "  Arrastar Vazio   Selecao por Laco Retangular",
                "  Setas Direcion.  Mover Elementos Selecionados na Grade",
                "",
                "FERRAMENTAS E VISUALIZACAO:",
                "  Teclas 1 .. 9    Selecionar Ferramenta da Barra Lateral",
                "  Shift + Arrastar Incluir ou Desanexar Elemento em Pacote",
                "  Ctrl + = / -     Zoom In / Zoom Out (50% a 200%)",
                "  Ctrl + 0         Restaurar Zoom para 100%",
                "  Ctrl + G         Exibir / Ocultar Grade"
        );

        TextArea area = new TextArea(guide);
        area.setEditable(false);
        area.setPrefRowCount(22);
        area.setPrefColumnCount(54);
        area.setStyle("-fx-font-family: 'Consolas', 'Monospaced'; -fx-font-size: 12px;");

        VBox box = new VBox(area);
        box.setPadding(new Insets(8));
        alert.getDialogPane().setContent(box);
        alert.showAndWait();
    }
}