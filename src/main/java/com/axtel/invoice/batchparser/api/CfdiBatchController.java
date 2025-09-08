package com.axtel.invoice.batchparser.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.angelsoft.sai.service.CfdiBatchService;
import com.axtel.invoice.batchparser.dto.BatchResponse;

@RestController
@RequestMapping("/api/cfdi")
public class CfdiBatchController {

	private static final Logger log = LoggerFactory.getLogger(CfdiBatchController.class);
	private final CfdiBatchService service;

	public CfdiBatchController(CfdiBatchService service) {
		this.service = service;
	}

	@PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<BatchResponse> uploadZip(@RequestPart("file") MultipartFile zip) throws Exception {
		if (zip == null || zip.isEmpty()) {
			return ResponseEntity.badRequest().build();
		}
		log.info("Recibido ZIP: name={}, size={} bytes, contentType={}", zip.getOriginalFilename(), zip.getSize(),
				zip.getContentType());
		BatchResponse resp = service.processZip(zip.getBytes());
		return ResponseEntity.ok(resp);
	}
}
