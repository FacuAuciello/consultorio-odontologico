package com.consultorio.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HistoriaClinicaUtilsTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 15, 9, 0);

    @Test
    void formateaElMontoConPuntoDeMiles() {
        assertThat(HistoriaClinicaUtils.formatearMonto(new BigDecimal("15000.00"))).isEqualTo("$ 15.000");
    }

    @Test
    void formateaElMontoConComaDecimalSoloSiHayDecimales() {
        assertThat(HistoriaClinicaUtils.formatearMonto(new BigDecimal("1234.5"))).isEqualTo("$ 1.234,5");
    }

    @Test
    void sinNotasNiMontoDevuelveLaHistoriaSinTocar() {
        String resultado = HistoriaClinicaUtils.conNuevaEntrada("historia previa", FECHA, null, "   ");

        assertThat(resultado).isEqualTo("historia previa");
    }

    @Test
    void conPacienteSinHistoriaCreaLaPrimeraEntrada() {
        String resultado = HistoriaClinicaUtils.conNuevaEntrada(null, FECHA, new BigDecimal("15000"), "Limpieza");

        assertThat(resultado).isEqualTo("--- 15/03/2026 09:00 | Monto: $ 15.000 ---\nLimpieza");
    }

    @Test
    void laEntradaNuevaQuedaArribaDeLaAnterior() {
        String resultado = HistoriaClinicaUtils.conNuevaEntrada("entrada vieja", FECHA, null, "Control");

        assertThat(resultado)
                .startsWith("--- 15/03/2026 09:00 | Monto: - ---\nControl")
                .endsWith("entrada vieja");
    }
}
