package com.consultorio.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;

/*
 Arma las entradas que se agregan solas a la historia clinica cuando se guarda un turno con notas o con monto.
 Es una clase sin estado (solo funciones puras: misma entrada -> misma salida), por eso es facil de testear
 */
public final class HistoriaClinicaUtils {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    // forLanguageTag en lugar de Locale.of(...): Locale.of recien existe desde Java 19 y el proyecto apunta a Java 17
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

    private HistoriaClinicaUtils() {
    }

    /*
     Devuelve la historia con la nueva entrada ARRIBA de todo (lo mas reciente primero, como antes).
     Si no hay notas ni monto no hay nada que registrar y se devuelve la historia sin tocar.
     Ejemplo del resultado:
         --- 15/03/2026 09:00 | Monto: $ 15.000 ---
         Limpieza y control
     */
    public static String conNuevaEntrada(String historiaActual, LocalDateTime fechaHora, BigDecimal monto, String notas) {

        boolean hayNotas = notas != null && !notas.isBlank();

        if (!hayNotas && monto == null) {
            return historiaActual;
        }

        // Optional evita el if/else: si hay monto lo formatea, si no muestra un guion
        String montoTexto = Optional.ofNullable(monto)
                .map(HistoriaClinicaUtils::formatearMonto)
                .orElse("-");

        String entrada = "--- " + fechaHora.format(FORMATO_FECHA) + " | Monto: " + montoTexto + " ---\n"
                + (hayNotas ? notas.trim() : "");

        // Optional.orElse("") para no concatenar la palabra "null" cuando el paciente todavia no tenia historia
        String anterior = Optional.ofNullable(historiaActual).orElse("");

        return anterior.isBlank() ? entrada : entrada + "\n\n" + anterior;
    }

    // 15000 -> "$ 15.000" (punto de miles y coma decimal, formato argentino). Los decimales solo aparecen si existen
    public static String formatearMonto(BigDecimal monto) {
        // DecimalFormat no es thread-safe, por eso se crea uno nuevo en cada llamada en vez de guardarlo en una constante
        DecimalFormat formato = new DecimalFormat("#,##0.##", DecimalFormatSymbols.getInstance(ARGENTINA));
        return "$ " + formato.format(monto);
    }
}
