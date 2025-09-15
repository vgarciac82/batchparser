package com.axtel.invoice.batchparser.reader.sat;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class ExpresionImpresaBuilder {

	private ExpresionImpresaBuilder() {
	}

	public static String build(String rfcEmisor, String rfcReceptor, BigDecimal total, String uuid) {
		// Formato total: hasta 18 enteros y 6 decimales, sin ceros no significativos
		// (Anexo 20)
		String tt = formatTotalForExpression(total);
		// Claves estándar en QR/WS: re, rr, tt, id
		return String.format("?re=%s&rr=%s&tt=%s&id=%s", rfcEmisor.trim().toUpperCase(Locale.ROOT),
				rfcReceptor.trim().toUpperCase(Locale.ROOT), tt, uuid.trim().toUpperCase(Locale.ROOT));
	}

	static String formatTotalForExpression(BigDecimal total) {
		if (total == null)
			total = BigDecimal.ZERO;
		// Escalar a 6 decimales, quitar ceros no significativos
		BigDecimal scaled = total.stripTrailingZeros();
		if (scaled.scale() < 0)
			scaled = scaled.setScale(0);
		if (scaled.scale() > 6)
			scaled = scaled.setScale(6, BigDecimal.ROUND_HALF_UP);

		DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
		DecimalFormat df = new DecimalFormat("0.######", sym);
		String s = df.format(scaled);
		// Validación simple de longitud (máx. ~25): 18 enteros + '.' + 6 decimales
		if (s.length() > 25) {
			throw new IllegalArgumentException("Total fuera de especificación para expresión impresa: " + s);
		}
		return s;
	}
}
