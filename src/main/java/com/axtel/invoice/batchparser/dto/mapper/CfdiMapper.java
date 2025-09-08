package com.axtel.invoice.batchparser.dto.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import com.angelsoft.sat.cfd._40.Comprobante;
import com.angelsoft.sat.common.TimbreFiscalDigital11.TimbreFiscalDigital;
import com.axtel.invoice.batchparser.dto.CfdiConceptoDTO;
import com.axtel.invoice.batchparser.dto.CfdiConceptoImpuestoDTO;
import com.axtel.invoice.batchparser.dto.CfdiParsedDTO;
import com.axtel.invoice.batchparser.dto.PagoFacturaDTO;
import com.axtel.invoice.batchparser.dto.PagoFacturaImpuestoDTO;
import com.axtel.invoice.batchparser.dto.PagoFacturaRetencionDTO;
import com.axtel.invoice.batchparser.dto.PagoFacturaTipoCambioDTO;

import jakarta.xml.bind.JAXBElement;

public class CfdiMapper {

	private static final ZoneId MX_ZONE = ZoneId.of("America/Mexico_City");

	public static CfdiParsedDTO map(String cTipoPago, int nFolioPago, Comprobante c) {
		String uuid = extractUUID(c);
		CfdiParsedDTO out = new CfdiParsedDTO();

		// HEADER
		PagoFacturaDTO h = new PagoFacturaDTO();
		h.cTipoPago = cTipoPago;
		h.nFolioPago = nFolioPago;
		h.cFactura = (uuid == null || uuid.isBlank()) ? buildFallbackId(c) : uuid;
		h.mImporteBruto = nn(c.getSubTotal());
		h.mImporteConIva = nn(c.getTotal());
		h.mImporteDescuento = nn(c.getDescuento());
		h.cRFCFactura = c.getEmisor() != null ? ns(c.getEmisor().getRfc()) : "";
		h.cRazonSocial = c.getEmisor() != null ? ns(c.getEmisor().getNombre()) : "";
		h.cRegimenFiscal = c.getEmisor() != null && c.getEmisor().getRegimenFiscal() != null
				? c.getEmisor().getRegimenFiscal().value()
				: null;
		h.cEsNotaCredito = (c.getTipoDeComprobante() != null && "E".equals(c.getTipoDeComprobante().value())) ? "S"
				: "N";
		h.cMetodoPago = c.getMetodoPago() != null ? c.getMetodoPago().value() : null;
		h.dFechaFactura = toLdt(c.getFecha());
		h.dFechaTimbrado = extractFechaTimbrado(c);
		h.dFechaConsultaEstatus = null;
		h.cEstatusSAT = null;
		h.serie = ns(c.getSerie());
		h.folio = ns(c.getFolio());
		h.cCodigoPostalEmisor = ns(c.getLugarExpedicion()); // CP expedición

		// detalle de impuestos (encabezado) + otros impuestos + IVA total
		TaxAgg agg = aggregateTaxes(c);
		h.mImporteIva = agg.iva;
		h.mOtrosImpuestos = agg.otrosTraslados;

		out.header = h;
		out.impuestos = toPagoFacturaImpuestos(cTipoPago, nFolioPago, h.cFactura, agg);
		out.retenciones = toPagoFacturaRetenciones(cTipoPago, nFolioPago, h.cFactura, agg);

		// tipo de cambio
		if (c.getMoneda() != null && !"MXN".equalsIgnoreCase(c.getMoneda().value()) && c.getTipoCambio() != null) {
			PagoFacturaTipoCambioDTO tc = new PagoFacturaTipoCambioDTO();
			tc.cTipoPago = cTipoPago;
			tc.nFolioPago = nFolioPago;
			tc.cUUID = h.cFactura;
			tc.cMoneda = c.getMoneda().value();
			// mImporteConversion: Total * TipoCambio
			BigDecimal t = nn(c.getTotal());
			BigDecimal tcambio = c.getTipoCambio();
			tc.mImporteConversion = (t != null && tcambio != null) ? t.multiply(tcambio) : null;
			out.tipoCambio = tc;
		}

		// conceptos + impuestos por concepto
		if (c.getConceptos() != null && c.getConceptos().getConcepto() != null) {
			c.getConceptos().getConcepto().forEach(con -> {
				CfdiConceptoDTO cd = new CfdiConceptoDTO();
				cd.uuid = h.cFactura;
				cd.cClaveProdServ = ns(con.getClaveProdServ());
				cd.nCantidad = con.getCantidad() != null ? con.getCantidad().intValue() : 0;
				cd.cClaveUnidad = ns(con.getClaveUnidad().value());
				cd.cDescripcion = ns(con.getDescripcion());
				cd.mValorUnitario = nn(con.getValorUnitario());
				cd.mImporte = nn(con.getImporte());
				out.conceptos.add(cd);

				if (con.getImpuestos() != null) {
					if (con.getImpuestos().getTraslados() != null
							&& con.getImpuestos().getTraslados().getTraslado() != null) {
						con.getImpuestos().getTraslados().getTraslado().forEach(t -> {
							CfdiConceptoImpuestoDTO ci = new CfdiConceptoImpuestoDTO();
							ci.cImpuesto = ns(t.getImpuesto().value()); // "001","002","003"
							ci.cTipoFactor = t.getTipoFactor() != null ? t.getTipoFactor().value() : null;
							ci.nTasaOCuota = t.getTasaOCuota();
							ci.mBase = nn(t.getBase());
							ci.mImporte = nn(t.getImporte());
							out.conceptoImpuestos.add(ci);
						});
					}
					if (con.getImpuestos().getRetenciones() != null
							&& con.getImpuestos().getRetenciones().getRetencion() != null) {
						con.getImpuestos().getRetenciones().getRetencion().forEach(r -> {
							CfdiConceptoImpuestoDTO ci = new CfdiConceptoImpuestoDTO();
							ci.cImpuesto = ns(r.getImpuesto().value());
							ci.cTipoFactor = null; // en retención por concepto no aplica tipoFactor
							ci.nTasaOCuota = r.getTasaOCuota();
							ci.mBase = nn(r.getBase());
							ci.mImporte = nn(r.getImporte());
							out.conceptoImpuestos.add(ci);
						});
					}
				}
			});
		}

		return out;
	}

	// ===== Helpers de mapeo =====

	private static String extractUUID(Comprobante c) {
		if (c.getComplemento() != null && c.getComplemento().getAny() != null) {
			for (Object o : c.getComplemento().getAny()) {
				Object val = (o instanceof JAXBElement) ? ((JAXBElement<?>) o).getValue() : o;
				if (val instanceof TimbreFiscalDigital t)
					return ns(t.getUUID());
			}
		}
		return null;
	}

	private static LocalDateTime extractFechaTimbrado(Comprobante c) {
		if (c.getComplemento() != null && c.getComplemento().getAny() != null) {
			for (Object o : c.getComplemento().getAny()) {
				Object val = (o instanceof JAXBElement) ? ((JAXBElement<?>) o).getValue() : o;
				if (val instanceof TimbreFiscalDigital t) {
					return asLocalDateTime(t.getFechaTimbrado());
				}
			}
		}
		return null;
	}

	private static String buildFallbackId(Comprobante c) {
		String s = ns(c.getSerie());
		String f = ns(c.getFolio());
		if (!s.isBlank() || !f.isBlank())
			return (s + "-" + f).replaceAll("^-|-$", "");
		return UUID.randomUUID().toString();
	}

	private static LocalDateTime toLdt(Object fecha) {
		if (fecha == null)
			return null;
		if (fecha instanceof javax.xml.datatype.XMLGregorianCalendar xgc) {
			return xgc.toGregorianCalendar().toZonedDateTime().withZoneSameInstant(MX_ZONE).toLocalDateTime();
		}
		if (fecha instanceof java.time.LocalDateTime ldt)
			return ldt;
		if (fecha instanceof java.util.Date d)
			return d.toInstant().atZone(MX_ZONE).toLocalDateTime();
		return null;
	}

	private static String ns(String s) {
		return s == null ? "" : s;
	}

	private static BigDecimal nn(BigDecimal v) {
		return v == null ? null : v;
	}

	private static class TaxAgg {
		BigDecimal iva = BigDecimal.ZERO;
		BigDecimal otrosTraslados = BigDecimal.ZERO;
		// clave: impuesto|tipoFactor|tasa -> [base, importe]
		Map<String, BigDecimal[]> traslados = new LinkedHashMap<>();
		// clave: impuesto -> importe
		Map<String, BigDecimal> retenciones = new LinkedHashMap<>();
	}

	private static TaxAgg aggregateTaxes(Comprobante c) {
		TaxAgg a = new TaxAgg();
		if (c.getConceptos() != null && c.getConceptos().getConcepto() != null) {
			c.getConceptos().getConcepto().forEach(con -> {
				if (con.getImpuestos() != null && con.getImpuestos().getTraslados() != null
						&& con.getImpuestos().getTraslados().getTraslado() != null) {
					con.getImpuestos().getTraslados().getTraslado().forEach(t -> {
						String imp = ns(t.getImpuesto().value());
						String tf = t.getTipoFactor() != null ? t.getTipoFactor().value() : "";
						String tasa = t.getTasaOCuota() != null
								? t.getTasaOCuota().setScale(4, RoundingMode.HALF_UP).toPlainString()
								: "";
						String key = imp + "|" + tf + "|" + tasa;
						BigDecimal base = t.getBase() != null ? t.getBase() : BigDecimal.ZERO;
						BigDecimal impte = t.getImporte() != null ? t.getImporte() : BigDecimal.ZERO;
						a.traslados.computeIfAbsent(key, k -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
						a.traslados.get(key)[0] = a.traslados.get(key)[0].add(base);
						a.traslados.get(key)[1] = a.traslados.get(key)[1].add(impte);
						if ("002".equals(imp))
							a.iva = a.iva.add(impte);
						else
							a.otrosTraslados = a.otrosTraslados.add(impte);
					});
				}
				if (con.getImpuestos() != null && con.getImpuestos().getRetenciones() != null
						&& con.getImpuestos().getRetenciones().getRetencion() != null) {
					con.getImpuestos().getRetenciones().getRetencion().forEach(r -> {
						String imp = ns(r.getImpuesto().value());
						BigDecimal impte = r.getImporte() != null ? r.getImporte() : BigDecimal.ZERO;
						a.retenciones.merge(imp, impte, BigDecimal::add);
					});
				}
			});
		}
		return a;
	}

	private static String nombreImpuesto(String codigo) {
		return switch (codigo) {
		case "001" -> "ISR";
		case "002" -> "IVA";
		case "003" -> "IEPS";
		default -> codigo;
		};
	}

	private static List<PagoFacturaImpuestoDTO> toPagoFacturaImpuestos(String cTipoPago, int nFolioPago, String uuid,
			TaxAgg agg) {
		List<PagoFacturaImpuestoDTO> list = new ArrayList<>();
		for (Map.Entry<String, BigDecimal[]> e : agg.traslados.entrySet()) {
			String[] parts = e.getKey().split("\\|", -1);
			String imp = parts[0];
			String tipoFactor = parts[1].isBlank() ? null : parts[1];
			String tasa = parts[2].isBlank() ? null : parts[2];

			BigDecimal base = e.getValue()[0];
			BigDecimal impte = e.getValue()[1];

			PagoFacturaImpuestoDTO r = new PagoFacturaImpuestoDTO();
			r.cTipoPago = cTipoPago;
			r.nFolioPago = nFolioPago;
			r.uuid = uuid;
			r.cNombreImpuesto = nombreImpuesto(imp);
			r.mImporteImpuesto = impte;
			r.nTazaImpuesto = (tasa != null ? new BigDecimal(tasa) : null);
			r.cTipoFactor = tipoFactor;
			r.mImporteBase = base;
			list.add(r);
		}
		return list;
	}

	private static List<PagoFacturaRetencionDTO> toPagoFacturaRetenciones(String cTipoPago, int nFolioPago, String uuid,
			TaxAgg agg) {
		List<PagoFacturaRetencionDTO> list = new ArrayList<>();
		for (Map.Entry<String, BigDecimal> e : agg.retenciones.entrySet()) {
			PagoFacturaRetencionDTO r = new PagoFacturaRetencionDTO();
			r.cTipoPago = cTipoPago;
			r.nFolioPago = nFolioPago;
			r.uuid = uuid;
			r.cNombreRetencion = nombreImpuesto(e.getKey());
			r.mImporteRetencion = e.getValue();
			list.add(r);
		}
		return list;
	}

	private static LocalDateTime asLocalDateTime(Object obj) {
		if (obj == null)
			return null;
		if (obj instanceof LocalDateTime ldt)
			return ldt;
		if (obj instanceof OffsetDateTime odt)
			return odt.atZoneSameInstant(MX_ZONE).toLocalDateTime();
		if (obj instanceof ZonedDateTime zdt)
			return zdt.withZoneSameInstant(MX_ZONE).toLocalDateTime();
		if (obj instanceof XMLGregorianCalendar xgc) {
			GregorianCalendar gc = xgc.toGregorianCalendar();
			return gc.toInstant().atZone(MX_ZONE).toLocalDateTime();
		}
		if (obj instanceof Date d)
			return Instant.ofEpochMilli(d.getTime()).atZone(MX_ZONE).toLocalDateTime();
		return null;
	}
}
