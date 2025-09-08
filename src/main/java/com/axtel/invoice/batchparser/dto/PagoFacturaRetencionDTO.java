package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PagoFacturaRetencionDTO {
	public String cTipoPago;
	public int nFolioPago;
	public String uuid;
	public String cNombreRetencion; // "IVA", "ISR", "IEPS"
	public BigDecimal mImporteRetencion;
}
