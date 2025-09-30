package com.axtel.invoice.batchparser.reader.sat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angelsoft.sat.cfd._40.Comprobante;
import com.angelsoft.sat.cfdi.v4.CFDv40;
import com.axtel.invoice.batchparser.dto.CfdiParsedDTO;
import com.axtel.invoice.batchparser.dto.mapper.CfdiMapper;

public class AggregateTaxesImplocalTest {

	private static final Logger log = LoggerFactory.getLogger(AggregateTaxesImplocalTest.class);

	private Comprobante parseXml(String resource) throws Exception {
		try (InputStream is = getClass().getResourceAsStream("/" + resource)) {
			if (is == null) {
				throw new IllegalArgumentException("Recurso no encontrado: " + resource);
			}

			// Leer todos los bytes del recurso
			byte[] raw = is.readAllBytes();

			// Eliminar BOM si está presente (UTF-8 BOM = 0xEF 0xBB 0xBF)
			byte[] xmlBytes = raw;
			if (raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF) {
				xmlBytes = java.util.Arrays.copyOfRange(raw, 3, raw.length);
			}

			try (var in = new ByteArrayInputStream(xmlBytes)) {
				return (Comprobante) (new CFDv40(in, "com.angelsoft.sat.common.TimbreFiscalDigital11",
						"com.angelsoft.sat.common.implocal10")).getComprobanteDocument();
			}
		}
	}

	@Test
	void shouldParseAllXmlsInResources() throws Exception {
		var root = Path.of(getClass().getResource("/").toURI());
		log.info("Buscando XMLs en carpeta de recursos: {}", root);

		try (var files = Files.list(root)) {
			files.filter(p -> p.getFileName().toString().endsWith(".xml")).forEach(path -> {
				log.info("Procesando archivo XML: {}", path.getFileName());
				try {
					Comprobante comprobante = parseXml(path.getFileName().toString());
					log.debug("Comprobante leído correctamente para archivo {}", path.getFileName());

					CfdiParsedDTO dto = CfdiMapper.map("01", 123, comprobante);
					log.debug("DTO generado para archivo {}: {}", path.getFileName(), dto);

					log.info("Archivo={} retenciones={}", path.getFileName(), dto.getRetenciones());

					assertThat(dto).isNotNull();
					assertThat(dto.getRetenciones()).isNotEmpty();

					log.info("Retenciones: {}", dto.getRetenciones());
					log.info("✔ Archivo {} procesado exitosamente con {} retenciones", path.getFileName(),
							dto.getRetenciones().size());
				} catch (Exception e) {
					log.error("❌ Error procesando archivo {}", path.getFileName(), e);
					throw new RuntimeException("Fallo al procesar " + path, e);
				}
			});
		}
	}

}
