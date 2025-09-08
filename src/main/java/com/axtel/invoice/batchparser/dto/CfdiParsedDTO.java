package com.axtel.invoice.batchparser.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CfdiParsedDTO {
	public PagoFacturaDTO header;
	public List<PagoFacturaImpuestoDTO> impuestos = new ArrayList<>();
	public List<PagoFacturaRetencionDTO> retenciones = new ArrayList<>();
	public PagoFacturaTipoCambioDTO tipoCambio; // opcional
	public List<CfdiConceptoDTO> conceptos = new ArrayList<>();
	public List<CfdiConceptoImpuestoDTO> conceptoImpuestos = new ArrayList<>();
}
