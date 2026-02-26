package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
	// Impuestos y retenciones anidados en cada concepto
	private List<CfdiConceptoImpuestoDTO> conceptoImpuestos = new ArrayList<>();
	private List<CfdiConceptoImpuestoDTO> conceptoRetenciones = new ArrayList<>();
}
