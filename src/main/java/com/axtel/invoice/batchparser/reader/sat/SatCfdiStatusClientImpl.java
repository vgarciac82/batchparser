package com.axtel.invoice.batchparser.reader.sat;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Locale;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapMessage;

import com.github.benmanes.caffeine.cache.Cache;

@Component
public class SatCfdiStatusClientImpl implements SatCfdiStatusClient {

	private static final Logger log = LoggerFactory.getLogger(SatCfdiStatusClientImpl.class);
	private static final String SOAP_ACTION = "http://tempuri.org/IConsultaCFDIService/Consulta";

	private final WebServiceTemplate ws;
	private final Cache<String, SatConsultaResult> cache;

	public SatCfdiStatusClientImpl(WebServiceTemplate ws, Cache<String, SatConsultaResult> cache) {
		this.ws = ws;
		this.cache = cache;
	}

	@Override
	public SatConsultaResult consultar(String rfcEmisor, String rfcReceptor, BigDecimal total, String uuid) {
		String expr = ExpresionImpresaBuilder.build(rfcEmisor, rfcReceptor, total, uuid);
		String cacheKey = expr.toUpperCase(Locale.ROOT);

		SatConsultaResult cached = cache.getIfPresent(cacheKey);
		if (cached != null) {
			if (log.isTraceEnabled()) {
				log.trace("SAT cache hit para expr={} → estado={}", expr, cached.estado());
			}
			return cached;
		}

		String requestXml = buildSoapRequest(expr);

		if (log.isTraceEnabled()) {
			log.trace("SAT consulta expr={}", expr);
			log.trace("SAT request XML:\n{}", requestXml);
		}

		StringWriter sw = new StringWriter(1024);
		try {
			ws.sendSourceAndReceiveToResult(new StreamSource(new StringReader(requestXml)), setSoapAction(SOAP_ACTION),
					new StreamResult(sw));
			String respXml = sw.toString();

			if (log.isTraceEnabled()) {
				log.trace("SAT raw response XML:\n{}", respXml);
			}

			SatConsultaResult parsed = parseResponse(respXml);

			if (log.isTraceEnabled()) {
				log.trace("SAT parsed estado={}, esCancelable={}, estatusCancelacion={}", parsed.estado(),
						parsed.esCancelable(), parsed.estatusCancelacion());
			}

			cache.put(cacheKey, parsed);
			return parsed;
		} catch (Exception e) {
			log.warn("Error consultando SAT (expr={}): {}", expr, e.toString());
			return new SatConsultaResult(SatEstado.DESCONOCIDO, null, null, null);
		}
	}

	private static WebServiceMessageCallback setSoapAction(String action) {
		return (WebServiceMessage message) -> {
			SoapMessage soap = (SoapMessage) message;
			soap.setSoapAction(action);
			soap.getSoapHeader();
		};
	}

	private static String buildSoapRequest(String expresionImpresa) {
		String body = """
				    <Consulta xmlns="http://tempuri.org/">
				      <expresionImpresa>%s</expresionImpresa>
				    </Consulta>
				""".formatted(escapeXml(expresionImpresa));
		return body;
	}

	private static String escapeXml(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static SatConsultaResult parseResponse(String xml) throws Exception {
		if (xml == null || xml.isBlank()) {
			return new SatConsultaResult(SatEstado.DESCONOCIDO, null, null, null);
		}

		XPath xp = XPathFactory.newInstance().newXPath();
		org.xml.sax.InputSource src = new org.xml.sax.InputSource(new StringReader(xml));

		// 1) Intento original: atributos en ConsultaResult dentro de Envelope
		String estado = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@Estado",
				src, XPathConstants.STRING);

		// Reusar el InputSource: crear uno nuevo por cada evaluate
		src = new org.xml.sax.InputSource(new StringReader(xml));
		String esCancelable = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@EsCancelable",
				src, XPathConstants.STRING);

		src = new org.xml.sax.InputSource(new StringReader(xml));
		String estatusCancel = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@EstatusCancelacion",
				src, XPathConstants.STRING);

		// 2) Si no vino Envelope/atributos, intentar elementos hijos (con o sin
		// Envelope)
		if (isBlank(estado)) {
			src = new org.xml.sax.InputSource(new StringReader(xml));
			estado = (String) xp.evaluate(
					"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='Estado']/text()",
					src, XPathConstants.STRING);
			if (isBlank(estado)) {
				src = new org.xml.sax.InputSource(new StringReader(xml));
				estado = (String) xp.evaluate(
						"/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='Estado']/text()",
						src, XPathConstants.STRING);
			}
		}

		if (isBlank(esCancelable)) {
			src = new org.xml.sax.InputSource(new StringReader(xml));
			esCancelable = (String) xp.evaluate(
					"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='EsCancelable']/text()",
					src, XPathConstants.STRING);
			if (isBlank(esCancelable)) {
				src = new org.xml.sax.InputSource(new StringReader(xml));
				esCancelable = (String) xp.evaluate(
						"/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='EsCancelable']/text()",
						src, XPathConstants.STRING);
			}
		}

		if (isBlank(estatusCancel)) {
			src = new org.xml.sax.InputSource(new StringReader(xml));
			estatusCancel = (String) xp.evaluate(
					"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='EstatusCancelacion']/text()",
					src, XPathConstants.STRING);
			if (isBlank(estatusCancel)) {
				src = new org.xml.sax.InputSource(new StringReader(xml));
				estatusCancel = (String) xp.evaluate(
						"/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/*[local-name()='EstatusCancelacion']/text()",
						src, XPathConstants.STRING);
			}
		}

		// Normalización de estado
		String normEstado = (estado == null ? "" : estado.trim()).toUpperCase(Locale.ROOT);
		SatEstado st = switch (normEstado) {
		case "VIGENTE" -> SatEstado.VIGENTE;
		case "CANCELADO" -> SatEstado.CANCELADO;
		case "NO ENCONTRADO" -> SatEstado.NO_ENCONTRADO;
		default -> SatEstado.DESCONOCIDO;
		};

		return new SatConsultaResult(st, emptyToNull(esCancelable), emptyToNull(estatusCancel), xml);
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

}
