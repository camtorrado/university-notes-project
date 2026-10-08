package com.notas;

import com.notas.gui.VentanaPrincipal;
import com.notas.service.GestionService;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

// Punto de entrada de la version grafica (la de consola sigue siendo App).
public class AppGUI {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Notas Universitarias");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignorado) {
                // Si no esta disponible se usa el aspecto por defecto de Swing.
            }
            new VentanaPrincipal(new GestionService(Path.of("data"))).setVisible(true);
        });
    }
}
