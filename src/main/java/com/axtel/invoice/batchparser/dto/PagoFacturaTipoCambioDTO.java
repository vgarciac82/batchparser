package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PagoFacturaTipoCambioDTO {
	public String cTipoPago;
	public int nFolioPago;
	public String cUUID;
	public String cMoneda;
	public BigDecimal mImporteConversion;
}
