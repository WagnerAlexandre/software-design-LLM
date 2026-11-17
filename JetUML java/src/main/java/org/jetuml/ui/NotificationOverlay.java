package org.jetuml.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.jetuml.config.UserPreferences;

/**
 * Componente de notificacoes temporarias no canto inferior esquerdo da janela:
 * erros em vermelho, confirmacoes em verde, com duracao configuravel de 0 a 10s (RF41, RF42).
 */
public class NotificationOverlay extends StackPane {
    private final HBox banner;
    private final Label messageLabel;
    private PauseTransition activeTimer;

    public NotificationOverlay() {
        setPickOnBounds(false);
        setMouseTransparent(true);
        setAlignment(Pos.BOTTOM_LEFT);
        setPadding(new Insets(0, 0, 14, 14));

        this.messageLabel = new Label();
        this.messageLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 12.5px;");
        this.messageLabel.setWrapText(true);
        this.messageLabel.setMaxWidth(480);

        this.banner = new HBox(messageLabel);
        this.banner.setPadding(new Insets(9, 14, 9, 14));
        this.banner.setMaxWidth(510);
        this.banner.setMaxHeight(64);
        this.banner.setVisible(false);
        this.banner.setManaged(false);

        getChildren().add(banner);
    }

    public void showConfirmation(String message) {
        show(message, "#2E7D32");
    }

    public void showError(String message) {
        show(message, "#C62828");
    }

    private void show(String message, String hexColor) {
        int durationSec = UserPreferences.getInstance().getNotificationDurationSeconds();
        // RF42: O valor 0 desativa todas as notificacoes
        if (durationSec <= 0 || message == null || message.isBlank()) {
            hideBanner();
            return;
        }

        if (activeTimer != null) {
            activeTimer.stop();
        }

        messageLabel.setText(message);
        banner.setStyle(
                "-fx-background-color: " + hexColor + ";"
                        + "-fx-background-radius: 6;"
                        + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.32), 8, 0.2, 0, 2);"
        );
        banner.setManaged(true);
        banner.setVisible(true);

        activeTimer = new PauseTransition(Duration.seconds(durationSec));
        activeTimer.setOnFinished(e -> hideBanner());
        activeTimer.playFromStart();
    }

    public void hideBanner() {
        banner.setVisible(false);
        banner.setManaged(false);
    }
}
