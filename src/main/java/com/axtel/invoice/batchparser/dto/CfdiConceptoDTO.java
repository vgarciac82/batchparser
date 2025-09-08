package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CfdiConceptoDTO {
	public String uuid; // cUUID en tabla
	public String cClaveProdServ;
	public Integer nCantidad; // CAST a int
	public String cClaveUnidad;
	public String cDescripcion;
	public BigDecimal mValorUnitario;
	public BigDecimal mImporte;
	// se llena nIDConcepto al insertar (identity)
	public Integer nIDConcepto;
}
