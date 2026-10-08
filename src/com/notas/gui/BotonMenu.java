package com.notas.gui;

import javax.swing.JPopupMenu;

// Boton secundario que despliega un menu con acciones menos frecuentes ("Más ▾").
class BotonMenu extends BotonPlano {

    BotonMenu(String texto, JPopupMenu menu) {
        super(texto + "  ▾", Tipo.SECUNDARIO);
        addActionListener(e -> menu.show(this, 0, getHeight() + 4));
    }
}
