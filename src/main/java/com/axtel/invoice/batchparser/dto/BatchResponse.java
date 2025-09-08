package com.axtel.invoice.batchparser.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchResponse {
	
	public List<SuccessInvoice> successInvoices = new ArrayList<>();
	public List<FailInvoice> failInvoices = new ArrayList<>();

	public static class SuccessInvoice {
		public String fileXML;
		public String filePDF;
		public CfdiParsedDTO comprobante;
	}

	public static class FailInvoice {
		public String file;
		public String cause;
	}
}
