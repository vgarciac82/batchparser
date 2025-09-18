package com.axtel.invoice.batchparser.reader.sat;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Integration test REAL que llama al Web Service del SAT.
 *
 * Requisitos: - Salida a Internet disponible para el host que ejecuta el test.
 * - Dependencias: spring-ws-core, httpclient 4.5.x, axiom-impl,
 * javax.activation-api (para Axiom). - Este test NO usa contexto de Spring;
 * construye el cliente a mano para aislar problemas.
 *
 * Consideraciones: - El SAT puede estar lento/caído. Ajusta timeouts abajo si
 * es necesario. - Si necesitas desactivar este test en CI, puedes anotarlo
 * con @Disabled o @EnabledIfEnvironmentVariable.
 */
public class SatCfdiStatusClientLiveIT {

	private static final Logger log = LoggerFactory.getLogger(SatCfdiStatusClientLiveIT.class);

	// Endpoint real del SAT (SOAP 1.1)
	private static final String SAT_URL = "https://consultaqr.facturaelectronica.sat.gob.mx/ConsultaCFDIService.svc";

	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	void consultar_en_SAT_con_datos_reales() {
		try {
			// Datos de prueba
			final String rfcEmisor = "AXT940727FP8";
			final String rfcReceptor = "CNF010405EG1";
			final BigDecimal total = new BigDecimal("191109.44");
			final String uuid = "e206f0a2-4153-41b8-ace1-fd157c205ee5";

			// 1) Message factory Axiom SOAP 1.1
			var messageFactory = new AxiomSoapMessageFactory();
			messageFactory.setSoapVersion(SoapVersion.SOAP_11);
			messageFactory.afterPropertiesSet();

			// 2) WebServiceTemplate apuntando al SAT
			var wst = new WebServiceTemplate(messageFactory);
			wst.setDefaultUri(SAT_URL);

			// 3) HttpClient 4.x con pool y timeouts
			var cm = new PoolingHttpClientConnectionManager();
			cm.setMaxTotal(16);
			cm.setDefaultMaxPerRoute(8);

			var rc = RequestConfig.custom().setConnectTimeout(10_000).setSocketTimeout(20_000)
					.setConnectionRequestTimeout(5_000).setExpectContinueEnabled(true).build();

			CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(rc)
					.disableAutomaticRetries().build();

			var sender = new org.springframework.ws.transport.http.HttpComponentsMessageSender(httpClient);
			wst.setMessageSender(sender);

			// 4) Cache para el cliente
			Cache<String, SatConsultaResult> cache = Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES)
					.maximumSize(10_000).build();

			// 5) Instanciar cliente real
			var client = new SatCfdiStatusClientImpl(wst, cache);

			// 6) Ejecutar consulta
			SatConsultaResult result = client.consultar(rfcEmisor, rfcReceptor, total, uuid);

			// 7) Validaciones mínimas
			assertNotNull(result, "La respuesta no debe ser null");
			assertNotNull(result.estado(), "El estado devuelto no debe ser null");

			// Logs detallados
			String expr = ExpresionImpresaBuilder.build(rfcEmisor, rfcReceptor, total, uuid);
			log.info("Expresión impresa enviada: {}", expr);
			log.info("Estado devuelto por SAT: {}", result.estado());
			log.info("EsCancelable: {}", result.esCancelable());
			log.info("EstatusCancelacion: {}", result.estatusCancelacion());
			log.debug("Respuesta completa (raw XML): {}", result.rawXml());

		} catch (Exception e) {
			log.error("Fallo al consultar el SAT", e);
			fail(e.toString());
		}
	}
}
