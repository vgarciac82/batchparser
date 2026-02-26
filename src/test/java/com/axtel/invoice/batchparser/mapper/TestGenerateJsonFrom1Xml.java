package com.axtel.invoice.batchparser.mapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.angelsoft.sat.cfd._40.Comprobante;
import com.angelsoft.sat.cfdi.v4.CFDv40;
import com.axtel.invoice.batchparser.dto.CfdiParsedDTO;
import com.axtel.invoice.batchparser.dto.mapper.CfdiMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class TestGenerateJsonFrom1Xml {

	@Test
	void testGenerate1XmlJson() throws Exception {
		// Leer el archivo 1.xml desde cfdilib
		Path xmlPath = Path.of("/home/vgarciac/WorkspaceGPP/cfdilib/src/test/resources/1.xml");
		
		byte[] xmlBytes = Files.readAllBytes(xmlPath);
		
		// Parsear el XML a Comprobante
		Comprobante comprobante;
		try (InputStream in = new ByteArrayInputStream(xmlBytes)) {
			comprobante = (Comprobante) (new CFDv40(in, 
					"com.angelsoft.sat.common.TimbreFiscalDigital11",
					"com.angelsoft.sat.common.implocal10")).getComprobanteDocument();
		}
		
		// Generar el DTO usando el mapper
		CfdiParsedDTO dto = CfdiMapper.map("01", 1, comprobante);
		
		// Convertir a JSON con formato bonito
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		String json = mapper.writeValueAsString(dto);
		
		// Imprimir el JSON
		System.out.println("=".repeat(80));
		System.out.println("JSON GENERADO PARA 1.xml:");
		System.out.println("=".repeat(80));
		System.out.println(json);
		System.out.println("=".repeat(80));
	}
}
