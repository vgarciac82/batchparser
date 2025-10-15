package com.axtel.invoice.batchparser.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.angelsoft.sat.cfd._40.Comprobante;
import com.angelsoft.sat.cfdi.v4.CFDv40;
import com.axtel.invoice.batchparser.dto.BatchResponse;
import com.axtel.invoice.batchparser.dto.BatchResponse.FailInvoice;
import com.axtel.invoice.batchparser.dto.BatchResponse.SuccessInvoice;
import com.axtel.invoice.batchparser.dto.mapper.CfdiMapper;
import com.axtel.invoice.batchparser.reader.sat.SatCfdiStatusClient;
import com.axtel.invoice.batchparser.reader.sat.SatEstado;

@Service
public class CfdiBatchService {

	private static final Logger log = LoggerFactory.getLogger(CfdiBatchService.class);
	
	private final HistoricalInvoiceChecker historicalInvoiceChecker;
	private final SatCfdiStatusClient satCfdiStatusClient;

	@Value("${sat.consulta.enabled:true}")
	private boolean satEnabled;

	@Value("${sat.consulta.max-concurrency:8}")
	private int satMaxConcurrency;

	public CfdiBatchService(HistoricalInvoiceChecker historicalInvoiceChecker,
			SatCfdiStatusClient satCfdiStatusClient) {
		this.historicalInvoiceChecker = historicalInvoiceChecker;
		this.satCfdiStatusClient = satCfdiStatusClient;
	}

	private static final class EntryInfo {
		final String path; // ruta dentro del zip (con carpetas)
		final String base; // nombre sin extensión (normalizado)
		final byte[] bytes;

		EntryInfo(String path, String base, byte[] bytes) {
			this.path = path;
			this.base = base;
			this.bytes = bytes;
		}
	}

	private static final class PairGroup {
		final String base;
		final List<EntryInfo> xmls = new ArrayList<>();
		final List<EntryInfo> pdfs = new ArrayList<>();

		PairGroup(String base) {
			this.base = base;
		}
	}

	// ===== API principal =====
	public BatchResponse processZip(MultipartFile zipFile) throws IOException {
		try (var in = zipFile.getInputStream()) {
			return processZip(in);
		}
	}

	public BatchResponse processZip(byte[] zipBytes) throws IOException {
		return processZip(new ByteArrayInputStream(zipBytes));
	}

	public BatchResponse processZip(InputStream zipStream) throws IOException {
		BatchResponse response = new BatchResponse();

		// 1) Leer ZIP → agrupar por nombre base (sin extensión)
		var groups = readZip(zipStream);

		var success = new ArrayList<SuccessInvoice>();
		var fails = new ArrayList<FailInvoice>();

		// 2) Validaciones #1 (pares) y #2 (nombres repetidos)
		var invalidByName = new HashSet<String>();
		for (var g : groups.values()) {
			var xmlDup = g.xmls.size() > 1;
			var pdfDup = g.pdfs.size() > 1;

			if (xmlDup || pdfDup) {
				// Regla 2: repetidos por nombre
				fails.add(new FailInvoice(g.base,
						"La factura " + g.base + " esta repetida en nombre. Debe tener nombre unico"));
				invalidByName.add(g.base);
				continue;
			}

			var hasXml = g.xmls.size() == 1;
			var hasPdf = g.pdfs.size() == 1;

			if (hasXml && hasPdf) {
				continue;
			}

			// Regla 1: pares incompletos
			if (hasXml && !hasPdf) {
				var xml = g.xmls.get(0);
				fails.add(new FailInvoice(xml.path, xml.base
						+ ".xml No se encontro su archivo correspondiente. Debe registrar pares pdf y xml con el mismo nombre."));
				invalidByName.add(g.base);
			} else if (!hasXml && hasPdf) {
				var pdf = g.pdfs.get(0);
				fails.add(new FailInvoice(pdf.path, pdf.base
						+ ".pdf No se encontro su archivo correspondiente. Debe registrar pares pdf y xml con el mismo nombre."));
				invalidByName.add(g.base);
			}
		}

		// 3) Para los válidos por nombre, parsear y obtener UUID (para verificar
		// duplicados)
		// uuid → lista de bases que lo usan
		var uuidToBases = new LinkedHashMap<String, List<String>>();
		var baseToUuid = new HashMap<String, String>();
		var baseToComprobante = new HashMap<String, Comprobante>();

		for (var g : groups.values()) {
			if (invalidByName.contains(g.base))
				continue;

			var xml = g.xmls.get(0);
			try {
				var c = parseComprobante(xml.bytes);
				var uuid = CfdiMapper.extractUUID(c);

				if (uuid == null || uuid.isBlank()) {
					fails.add(new FailInvoice(xml.path, "No se encontró UUID en el CFDI (" + xml.base + ".xml)"));
					invalidByName.add(g.base);
					continue;
				}

				baseToUuid.put(g.base, uuid);
				baseToComprobante.put(g.base, c);
				uuidToBases.computeIfAbsent(uuid, k -> new ArrayList<>()).add(g.base);

			} catch (Exception ex) {
				fails.add(new FailInvoice(xml.path, "Archivo dañado, ilegible o no es un CFDI: " + ex.getMessage()));
				invalidByName.add(g.base);
			}
		}

		// 4) Validación #3: UUID duplicado entre XML diferentes (dentro del mismo ZIP)
		var invalidByUuid = new HashSet<String>();
		for (var e : uuidToBases.entrySet()) {
			var uuid = e.getKey();
			var bases = e.getValue();
			if (bases.size() > 1) {
				var listado = bases.stream().map(b -> b + ".xml").toList();
				var joined = String.join(" y ", listado);
				fails.add(new FailInvoice(uuid,
						"Factura duplicada. El UUID: " + uuid + " se encuentra en las facturas " + joined));
				invalidByUuid.addAll(bases);
			}
		}

		// 5) Validación #4: UUID ya registrado en histórico (no pagar doble)
		var candidatesForHistoryCheck = baseToUuid.entrySet().stream().filter(e -> !invalidByName.contains(e.getKey()))
				.filter(e -> !invalidByUuid.contains(e.getKey())).map(Map.Entry::getValue)
				.collect(java.util.stream.Collectors.toSet());

		if (!candidatesForHistoryCheck.isEmpty()) {
			var duplicatesInHistory = historicalInvoiceChecker.findDuplicates(candidatesForHistoryCheck);
			if (!duplicatesInHistory.isEmpty()) {
				for (var g : groups.values()) {
					if (invalidByName.contains(g.base) || invalidByUuid.contains(g.base))
						continue;

					var uuid = baseToUuid.get(g.base);
					if (uuid != null && duplicatesInHistory.contains(uuid)) {
						var xml = g.xmls.get(0);
						fails.add(new FailInvoice(xml.path, "Factura ya registrada en histórico. El UUID " + uuid
								+ " ya existe y no debe pagarse dos veces."));
						invalidByUuid.add(g.base);
					}
				}
			}
		}

		// 6) **Nueva** Validación #5: Estatus en SAT (Vigente obligatorio)
		if (satEnabled) {
			record SatCandidate(String base, String path, String rfcE, String rfcR, java.math.BigDecimal total,
					String uuid) {
			}

			var candidates = new ArrayList<SatCandidate>();
			for (var g : groups.values()) {
				if (invalidByName.contains(g.base) || invalidByUuid.contains(g.base))
					continue;

				var c = baseToComprobante.get(g.base);
				if (c == null)
					continue;

				var rfcEmisor = (c.getEmisor() != null ? c.getEmisor().getRfc() : null);
				var rfcReceptor = (c.getReceptor() != null ? c.getReceptor().getRfc() : null);
				var total = c.getTotal(); // BigDecimal
				var uuid = baseToUuid.get(g.base);

				if (rfcEmisor == null || rfcReceptor == null || total == null || uuid == null) {
					var xml = g.xmls.get(0);
					fails.add(
							new FailInvoice(xml.path, "CFDI incompleto para validar en SAT (faltan RFC/Total/UUID)."));
					invalidByUuid.add(g.base);
					continue;
				}

				candidates.add(new SatCandidate(g.base, g.xmls.get(0).path, rfcEmisor, rfcReceptor, total, uuid));
			}

			if (!candidates.isEmpty()) {
				var executor = java.util.concurrent.Executors.newFixedThreadPool(Math.max(1, satMaxConcurrency));
				try {
					var futures = new ArrayList<java.util.concurrent.CompletableFuture<Void>>();
					for (var cand : candidates) {
						var fut = java.util.concurrent.CompletableFuture.runAsync(() -> {
							var res = satCfdiStatusClient.consultar(cand.rfcE, cand.rfcR, cand.total, cand.uuid);
							var estado = res.estado();
							if (estado != SatEstado.VIGENTE) {
								synchronized (this) {
									var detalle = new StringBuilder("CFDI no vigente ante SAT. Estado=" + estado);
									if (res.esCancelable() != null)
										detalle.append(", EsCancelable=").append(res.esCancelable());
									if (res.estatusCancelacion() != null)
										detalle.append(", EstatusCancelacion=").append(res.estatusCancelacion());
									fails.add(new FailInvoice(cand.path, detalle.toString()));
									invalidByUuid.add(cand.base);
								}
							}
						}, executor);
						futures.add(fut);
					}

					for (var f : futures) {
						try {
							f.join();
						} catch (Exception ignore) {
							log.warn(ignore.toString(), ignore);
						}
					}
				} finally {
					executor.shutdown();
				}
			}
		}

		// 7) Armar successInvoices
		for (var g : groups.values()) {
			if (invalidByName.contains(g.base))
				continue;
			if (invalidByUuid.contains(g.base))
				continue;

			var xml = g.xmls.get(0);
			var pdf = g.pdfs.get(0);
			var c = baseToComprobante.get(g.base);
			var dto = CfdiMapper.map("", 0, c);

			success.add(new SuccessInvoice(xml.path, pdf.path, dto));
		}

		response.setFailInvoices(fails);
		response.setSuccessInvoices(success);
		return response;
	}

	// ===== Lectura del ZIP =====
	private Map<String, PairGroup> readZip(InputStream zipStream) throws IOException {
		var groups = new LinkedHashMap<String, PairGroup>();
		try (var zis = new ZipInputStream(zipStream)) {
			ZipEntry ze;
			var buf = new byte[8192];
			while ((ze = zis.getNextEntry()) != null) {
				if (ze.isDirectory())
					continue;
				var path = ze.getName(); // e.g. "gastos/a1.pdf"
				var fileName = fileNameOf(path); // "a1.pdf"
				var ext = extensionOf(fileName); // "xml" | "pdf" | ""
				if (!"xml".equals(ext) && !"pdf".equals(ext))
					continue;
				var base = baseNameOf(fileName); // "a1"

				var data = readAll(zis, buf);
				var key = base.toLowerCase(Locale.ROOT);
				var g = groups.computeIfAbsent(key, k -> new PairGroup(base));
				var ei = new EntryInfo(path, base, data);
				if ("xml".equals(ext))
					g.xmls.add(ei);
				else
					g.pdfs.add(ei);
			}
		}
		return groups;
	}

	private static String fileNameOf(String path) {
		if (path == null)
			return "";
		var slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
		return (slash >= 0) ? path.substring(slash + 1) : path;
	}

	private static String extensionOf(String fileName) {
		var dot = fileName.lastIndexOf('.');
		return dot >= 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
	}

	private static String baseNameOf(String fileName) {
		var dot = fileName.lastIndexOf('.');
		return (dot >= 0) ? fileName.substring(0, dot) : fileName;
	}

	private static byte[] readAll(InputStream in, byte[] buf) throws IOException {
		var baos = new ByteArrayOutputStream(8192);
		int r;
		while ((r = in.read(buf)) != -1)
			baos.write(buf, 0, r);
		return baos.toByteArray();
	}

	// ===== Parse CFDI / UUID =====
	private Comprobante parseComprobante(byte[] xmlBytes) throws Exception {
		try (var in = new ByteArrayInputStream(xmlBytes)) {
			return (Comprobante) (new CFDv40(in, "com.angelsoft.sat.common.TimbreFiscalDigital11", "com.angelsoft.sat.common.implocal10"))
					.getComprobanteDocument();
		}
	}

}
