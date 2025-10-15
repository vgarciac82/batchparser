package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PagoFacturaRetencionDTO {
	private String cTipoPago;
	private int nFolioPago;
	private String uuid;
	private String cNombreRetencion;
	private BigDecimal mImporteRetencion;

	public String getcTipoPago() {
		return cTipoPago;
	}

	public void setcTipoPago(String cTipoPago) {
		this.cTipoPago = cTipoPago;
	}

	public int getnFolioPago() {
		return nFolioPago;
	}

	public void setnFolioPago(int nFolioPago) {
		this.nFolioPago = nFolioPago;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getcNombreRetencion() {
		return cNombreRetencion;
	}

	public void setcNombreRetencion(String cNombreRetencion) {
		this.cNombreRetencion = cNombreRetencion;
	}

	public BigDecimal getmImporteRetencion() {
		return mImporteRetencion;
	}

	public void setmImporteRetencion(BigDecimal mImporteRetencion) {
		this.mImporteRetencion = mImporteRetencion;
	}

	@Override
	public String toString() {
		return "PagoFacturaRetencionDTO [cTipoPago=" + cTipoPago + ", nFolioPago=" + nFolioPago + ", uuid=" + uuid
				+ ", cNombreRetencion=" + cNombreRetencion + ", mImporteRetencion=" + mImporteRetencion + "]";
	}

}
