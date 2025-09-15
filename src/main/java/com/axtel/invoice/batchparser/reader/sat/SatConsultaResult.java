package com.axtel.invoice.batchparser.reader.sat;

public record SatConsultaResult(SatEstado estado, String esCancelable, // p.ej. "Cancelable con aceptación"
		String estatusCancelacion, // p.ej. "En proceso"
		String rawXml // útil para auditoría/debug
) {
}
