package com.notas.model;

import java.util.List;

// Registros validos y errores de formato, separados: lo invalido no se pierde en silencio.
public record ResultadoCarga<T>(List<T> registros, List<String> errores) {
}
