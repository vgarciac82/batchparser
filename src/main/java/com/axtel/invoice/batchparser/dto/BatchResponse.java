package com.axtel.invoice.batchparser.dto;

import java.util.ArrayList;
import java.util.List;

public class BatchResponse {

	public static class FailInvoice {

		public String cause;

		public String file;

		public FailInvoice(String file, String cause) {
			super();
			this.file = file;
			this.cause = cause;
		}

	}

	public static class SuccessInvoice {

		public CfdiParsedDTO comprobante;

		public String filePDF;
		public String fileXML;

		public SuccessInvoice(String fileXML, String filePDF, CfdiParsedDTO comprobante) {
			super();
			this.fileXML = fileXML;
			this.filePDF = filePDF;
			this.comprobante = comprobante;
		}
	}

	public List<FailInvoice> failInvoices = new ArrayList<>();

	public List<SuccessInvoice> successInvoices = new ArrayList<>();

	public List<FailInvoice> getFailInvoices() {
		return failInvoices;
	}

	public List<SuccessInvoice> getSuccessInvoices() {
		return successInvoices;
	}

	public void setFailInvoices(List<FailInvoice> failInvoices) {
		this.failInvoices = failInvoices;
	}

	public void setSuccessInvoices(List<SuccessInvoice> successInvoices) {
		this.successInvoices = successInvoices;
	}
}
