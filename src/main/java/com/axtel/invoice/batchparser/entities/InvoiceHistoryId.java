package com.axtel.invoice.batchparser.entities;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class InvoiceHistoryId implements Serializable {

	private static final long serialVersionUID = 6863031214748079623L;
	private Integer fiscalYear;
	private String processName;
	private Integer processId;
	private String invoiceUuid;

	public InvoiceHistoryId() {
	}

	public InvoiceHistoryId(Integer fiscalYear, String processName, Integer processId, String invoiceUuid) {
		this.fiscalYear = fiscalYear;
		this.processName = processName;
		this.processId = processId;
		this.invoiceUuid = invoiceUuid;
	}

	public Integer getFiscalYear() {
		return fiscalYear;
	}

	public String getProcessName() {
		return processName;
	}

	public Integer getProcessId() {
		return processId;
	}

	public String getInvoiceUuid() {
		return invoiceUuid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof InvoiceHistoryId))
			return false;
		InvoiceHistoryId that = (InvoiceHistoryId) o;
		return Objects.equals(fiscalYear, that.fiscalYear) && Objects.equals(processName, that.processName)
				&& Objects.equals(processId, that.processId) && Objects.equals(invoiceUuid, that.invoiceUuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(fiscalYear, processName, processId, invoiceUuid);
	}
}
