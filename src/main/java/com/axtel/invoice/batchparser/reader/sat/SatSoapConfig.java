package com.axtel.invoice.batchparser.reader.sat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.SoapVersion;
import org.springframework.ws.soap.axiom.AxiomSoapMessageFactory;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import java.util.concurrent.TimeUnit;

@Configuration
public class SatSoapConfig {

	@Bean
	public WebServiceTemplate satWebServiceTemplate(@Value("${sat.consulta.url}") String url,
			@Value("${sat.consulta.connect-timeout-ms:3000}") int connectMs,
			@Value("${sat.consulta.read-timeout-ms:4000}") int readMs) {
		try {
			// 1) Axiom SOAP 1.1 (evita dependencias SAAJ)
			AxiomSoapMessageFactory messageFactory = new AxiomSoapMessageFactory();
			messageFactory.setSoapVersion(SoapVersion.SOAP_11);
			messageFactory.afterPropertiesSet();

			// 2) HttpClient 4.x con pool y timeouts vía RequestConfig
			PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
			cm.setMaxTotal(64);
			cm.setDefaultMaxPerRoute(16);

			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(connectMs).setSocketTimeout(readMs)
					.build();

			CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm)
					.setDefaultRequestConfig(requestConfig).disableAutomaticRetries() // los reintentos los manejamos
																						// arriba
																						// si usas Resilience4j
					.build();

			HttpComponentsMessageSender sender = new HttpComponentsMessageSender(httpClient);
			// ¡OJO! No llamar a sender.setConnectionTimeout ni setReadTimeout cuando pasas
			// un HttpClient custom

			// 3) WebServiceTemplate
			WebServiceTemplate template = new WebServiceTemplate();
			template.setMessageFactory(messageFactory);
			template.setDefaultUri(url);
			template.setMessageSender(sender);
			return template;
		} catch (Exception e) {
			throw new RuntimeException(e.toString(), e);
		}
	}

	@Bean
	public Cache<String, SatConsultaResult> satCache(@Value("${sat.consulta.cache-ttl-minutes:15}") long ttlMinutes) {
		return Caffeine.newBuilder().expireAfterWrite(ttlMinutes, TimeUnit.MINUTES).maximumSize(10_000).build();
	}
}
