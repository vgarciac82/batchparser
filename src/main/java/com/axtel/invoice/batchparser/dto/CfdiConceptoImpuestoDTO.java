package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CfdiConceptoImpuestoDTO {
	public Integer nIDConcepto; // FK a tCFDIConceptos (se asigna al insertar concepto)
	public String cImpuesto; // "001","002","003" o nombres si prefieres
	public String cTipoFactor; // "Tasa","Cuota","Exento"
	public BigDecimal nTasaOCuota; // numeric(6,4)
	public BigDecimal mBase; // money
	public BigDecimal mImporte; // money (puede ser null)
}
