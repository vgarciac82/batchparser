package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PagoFacturaImpuestoDTO {
	public String cTipoPago;
	public int nFolioPago;
	public String uuid;
	public String cNombreImpuesto; // "IVA", "IEPS", "ISR" (por ejemplo)
	public BigDecimal mImporteImpuesto;
	public BigDecimal nTazaImpuesto; // numeric(8,2) - redondea como necesites
	public String cTipoFactor; // Tasa / Cuota / Exento
	public BigDecimal mImporteBase;
}
