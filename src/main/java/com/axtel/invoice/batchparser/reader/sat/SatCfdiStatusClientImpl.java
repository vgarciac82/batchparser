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
		if (cached != null)
			return cached;

		String requestXml = buildSoapRequest(expr);

		StringWriter sw = new StringWriter(1024);
		try {
			ws.sendSourceAndReceiveToResult(new StreamSource(new StringReader(requestXml)), setSoapAction(SOAP_ACTION),
					new StreamResult(sw));
			String respXml = sw.toString();
			SatConsultaResult parsed = parseResponse(respXml);
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
				<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
				  <s:Body>
				    <Consulta xmlns="http://tempuri.org/">
				      <expresionImpresa>%s</expresionImpresa>
				    </Consulta>
				  </s:Body>
				</s:Envelope>
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
		String estado = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@Estado",
				new org.xml.sax.InputSource(new StringReader(xml)), XPathConstants.STRING);
		String esCancelable = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@EsCancelable",
				new org.xml.sax.InputSource(new StringReader(xml)), XPathConstants.STRING);
		String estatusCancel = (String) xp.evaluate(
				"/*[local-name()='Envelope']/*[local-name()='Body']/*[local-name()='ConsultaResponse']/*[local-name()='ConsultaResult']/@EstatusCancelacion",
				new org.xml.sax.InputSource(new StringReader(xml)), XPathConstants.STRING);

		SatEstado st = switch (estado == null ? "" : estado.trim().toUpperCase(Locale.ROOT)) {
		case "VIGENTE" -> SatEstado.VIGENTE;
		case "CANCELADO" -> SatEstado.CANCELADO;
		case "NO ENCONTRADO" -> SatEstado.NO_ENCONTRADO;
		default -> SatEstado.DESCONOCIDO;
		};
		return new SatConsultaResult(st, emptyToNull(esCancelable), emptyToNull(estatusCancel), xml);
	}

	private static String emptyToNull(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
