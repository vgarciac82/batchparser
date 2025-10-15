package com.axtel.invoice.batchparser.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CfdiParsedDTO {
	private PagoFacturaDTO header;
	private List<PagoFacturaImpuestoDTO> impuestos = new ArrayList<>();
	private List<PagoFacturaRetencionDTO> retenciones = new ArrayList<>();
	private PagoFacturaTipoCambioDTO tipoCambio; // opcional
	private List<CfdiConceptoDTO> conceptos = new ArrayList<>();
	private List<CfdiConceptoImpuestoDTO> conceptoImpuestos = new ArrayList<>();

	public PagoFacturaDTO getHeader() {
		return header;
	}

	public void setHeader(PagoFacturaDTO header) {
		this.header = header;
	}

	public List<PagoFacturaImpuestoDTO> getImpuestos() {
		return impuestos;
	}

	public void setImpuestos(List<PagoFacturaImpuestoDTO> impuestos) {
		this.impuestos = impuestos;
	}

	public List<PagoFacturaRetencionDTO> getRetenciones() {
		return retenciones;
	}

	public void setRetenciones(List<PagoFacturaRetencionDTO> retenciones) {
		this.retenciones = retenciones;
	}

	public PagoFacturaTipoCambioDTO getTipoCambio() {
		return tipoCambio;
	}

	public void setTipoCambio(PagoFacturaTipoCambioDTO tipoCambio) {
		this.tipoCambio = tipoCambio;
	}

	public List<CfdiConceptoDTO> getConceptos() {
		return conceptos;
	}

	public void setConceptos(List<CfdiConceptoDTO> conceptos) {
		this.conceptos = conceptos;
	}

	public List<CfdiConceptoImpuestoDTO> getConceptoImpuestos() {
		return conceptoImpuestos;
	}

	public void setConceptoImpuestos(List<CfdiConceptoImpuestoDTO> conceptoImpuestos) {
		this.conceptoImpuestos = conceptoImpuestos;
	}

	@Override
	public String toString() {
		return "CfdiParsedDTO [header=" + header + ", impuestos=" + impuestos + ", retenciones=" + retenciones
				+ ", tipoCambio=" + tipoCambio + ", conceptos=" + conceptos + ", conceptoImpuestos=" + conceptoImpuestos
				+ "]";
	}

}
