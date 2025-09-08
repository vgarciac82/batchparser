package com.axtel.invoice.batchparser.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angelsoft.sat.cfd._40.Comprobante;
import com.angelsoft.sat.cfdi.v4.CFDv40;
import com.axtel.invoice.batchparser.dto.BatchResponse;
import com.axtel.invoice.batchparser.dto.CfdiParsedDTO;
import com.axtel.invoice.batchparser.dto.mapper.CfdiMapper;

@Service
public class CfdiBatchService {
	private static final Logger log = LoggerFactory.getLogger(CfdiBatchService.class);

	public BatchResponse processZip(byte[] zipBytes) {
		// 1) Lee ZIP y separa XML/PDF en memoria
		Map<String, byte[]> xmls = new LinkedHashMap<>();
		Set<String> pdfsBase = new HashSet<>();

		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry e;
			while ((e = zis.getNextEntry()) != null) {
				if (e.isDirectory())
					continue;
				String name = e.getName();
				String base = baseName(name).toLowerCase(Locale.ROOT);
				if (name.toLowerCase(Locale.ROOT).endsWith(".xml")) {
					xmls.put(name, toBytes(zis));
				} else if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
					pdfsBase.add(base);
				} else {
					// ignorar otros
				}
			}
		} catch (Exception ex) {
			BatchResponse br = new BatchResponse();
			var f = new BatchResponse.FailInvoice();
			f.file = "(zip)";
			f.cause = "ZIP ilegible: " + rootMessage(ex);
			br.failInvoices.add(f);
			return br;
		}

		// 2) Procesa cada XML
		BatchResponse out = new BatchResponse();
		for (Map.Entry<String, byte[]> en : xmls.entrySet()) {
			String fileXML = en.getKey();
			byte[] data = en.getValue();
			String pdfMatch = pdfsBase.contains(baseName(fileXML).toLowerCase(Locale.ROOT)) ? baseName(fileXML) + ".pdf"
					: null;

			try {
				Comprobante c = parseComprobante(data); // usa CFDv40 + TFD 1.1
				// Mapeo a CfdiParsedDTO para "preview" (sin cTipoPago/nFolioPago)
				CfdiParsedDTO dto = CfdiMapper.map("", 0, c);
				// Limpia campos que el servlet llenará al insertar:
				dto.header.cTipoPago = null;
				dto.header.nFolioPago = 0;

				BatchResponse.SuccessInvoice ok = new BatchResponse.SuccessInvoice();
				ok.fileXML = fileXML;
				ok.filePDF = pdfMatch;
				ok.comprobante = dto;

				out.successInvoices.add(ok);
			} catch (Exception ex) {
				BatchResponse.FailInvoice fail = new BatchResponse.FailInvoice();
				fail.file = fileXML;
				fail.cause = rootMessage(ex);
				out.failInvoices.add(fail);
				// log con stack y archivo responsable
				log.error("[{}] ERROR: {}", fileXML, rootMessage(ex), ex);
			}
		}

		return out;
	}

	// ===== Helpers =====

	private static Comprobante parseComprobante(byte[] data) throws Exception {
		// Preferimos pasar el contexto del TFD 1.1 explícito para que el UUID se cargue
		// siempre
		try (InputStream in = new ByteArrayInputStream(data)) {
			return (Comprobante) new CFDv40(in, "com.angelsoft.sat.common.TimbreFiscalDigital11")
					.getComprobanteDocument();
		} catch (Exception first) {
			// fallback sin contexto explícito (por si ya está auto-configurado)
			try (InputStream in2 = new ByteArrayInputStream(data)) {
				return CFDv40.newComprobante(in2);
			} catch (Exception second) {
				// re-lanza con traza de ambos intentos
				first.addSuppressed(second);
				throw first;
			}
		}
	}

	private static byte[] toBytes(InputStream in) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 1024);
		byte[] buf = new byte[8192];
		int r;
		while ((r = in.read(buf)) != -1)
			baos.write(buf, 0, r);
		return baos.toByteArray();
	}

	private static String baseName(String path) {
		String name = path.replace('\\', '/');
		int slash = name.lastIndexOf('/');
		if (slash >= 0)
			name = name.substring(slash + 1);
		int dot = name.lastIndexOf('.');
		return (dot > 0) ? name.substring(0, dot) : name;
	}

	private static String rootMessage(Throwable t) {
		Throwable x = t;
		while (x.getCause() != null)
			x = x.getCause();
		return x.getMessage() != null ? x.getMessage() : x.toString();
	}
}
