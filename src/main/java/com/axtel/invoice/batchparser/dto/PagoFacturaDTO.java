package com.axtel.invoice.batchparser.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PagoFacturaDTO {
	public String cTipoPago;
	public int nFolioPago;
	public String cFactura; // UUID o fallback
	public BigDecimal mImporteBruto; // SubTotal
	public BigDecimal mImporteConIva; // Total
	public BigDecimal mImporteIva; // suma IVA traslado
	public String cRFCFactura; // RFC Emisor
	public BigDecimal mOtrosImpuestos; // traslados no IVA (IEPS, etc.)
	public String cEsNotaCredito; // 'S' si Egreso, 'N' de lo contrario
	public BigDecimal mImporteDescuento; // Comprobante.descuento
	public String cMetodoPago; // c_MetodoPago
	public String cRazonSocial; // Emisor.nombre
	public String cRegimenFiscal; // Emisor.regimenFiscal
	public LocalDateTime dFechaFactura; // Comprobante.fecha
	public LocalDateTime dFechaTimbrado; // TFD.fechaTimbrado
	public LocalDateTime dFechaConsultaEstatus; // null
	public String cEstatusSAT; // null
	public String serie; // Comprobante.serie
	public String folio; // Comprobante.folio
	public String cCodigoPostalEmisor; // Comprobante.lugarExpedicion (CP)
}
