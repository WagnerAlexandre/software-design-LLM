package org.jetuml.export;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.paint.Color;
import org.jetuml.geom.Rectangle2DInt;
import org.jetuml.model.Diagram;
import org.jetuml.rendering.DiagramRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Renderiza snapshots limpos do diagrama (sem grade, sem caixas de selecao e ajustados
 * aos limites do conteudo) para exportacao em arquivo (SVG, PNG, JPEG, GIF, BMP)
 * e copia para a area de transferencia do sistema (RF04, RF05).
 */
public final class DiagramImageExporter {
    private static final int PADDING = 16;

    private DiagramImageExporter() {
    }

    /**
     * Captura uma imagem do diagrama inteiro sem grade e recortada nos limites do conteudo (RF04, RF05).
     */
    public static WritableImage renderCleanCroppedImage(Diagram diagram) {
        Canvas offscreen = new Canvas(diagram.getWidth(), diagram.getHeight());
        DiagramRenderer.render(offscreen.getGraphicsContext2D(), diagram, false, false, null);

        Rectangle2DInt content = diagram.getContentBounds();
        int minX = Math.max(0, content.x() - PADDING);
        int minY = Math.max(0, content.y() - PADDING);
        int maxX = Math.min(diagram.getWidth(), content.getMaxX() + PADDING);
        int maxY = Math.min(diagram.getHeight(), content.getMaxY() + PADDING);

        int cropW = Math.max(40, maxX - minX);
        int cropH = Math.max(40, maxY - minY);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(minX, minY, cropW, cropH));

        WritableImage image = new WritableImage(cropW, cropH);
        return offscreen.snapshot(params, image);
    }

    /**
     * Copia a imagem do diagrama para a area de transferencia do sistema operacional (RF05).
     */
    public static void copyDiagramToSystemClipboard(Diagram diagram) {
        WritableImage fxImage = renderCleanCroppedImage(diagram);
        ClipboardContent content = new ClipboardContent();
        content.putImage(fxImage);
        Clipboard.getSystemClipboard().setContent(content);
    }

    /**
     * Exporta o diagrama para arquivo de imagem conforme a extensao escolhida (RF04).
     */
    public static void exportDiagramToFile(Diagram diagram, Path targetPath) throws IOException {
        String fileName = targetPath.getFileName().toString().toLowerCase(Locale.ROOT);
        if (fileName.endsWith(".svg")) {
            SvgDiagramExporter.exportToSvg(diagram, targetPath);
            return;
        }

        String format = "png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            format = "jpg";
        } else if (fileName.endsWith(".bmp")) {
            format = "bmp";
        } else if (fileName.endsWith(".gif")) {
            format = "gif";
        }

        WritableImage fxImage = renderCleanCroppedImage(diagram);
        BufferedImage rawBuffered = SwingFXUtils.fromFXImage(fxImage, null);

        BufferedImage outputImage;
        if ("jpg".equals(format) || "bmp".equals(format)) {
            // Formatos sem canal alfa exigem fundo solido branco
            outputImage = new BufferedImage(rawBuffered.getWidth(), rawBuffered.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = outputImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, outputImage.getWidth(), outputImage.getHeight());
            g2d.drawImage(rawBuffered, 0, 0, null);
            g2d.dispose();
        } else {
            outputImage = rawBuffered;
        }

        if (!ImageIO.write(outputImage, format, targetPath.toFile())) {
            throw new IOException("Formato de imagem nao suportado: " + format);
        }
    }
}
