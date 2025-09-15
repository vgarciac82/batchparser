package com.axtel.invoice.batchparser.entities;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "invoice_history")
@Immutable // vista / solo lectura
public class InvoiceHistory {

    @EmbeddedId
    private InvoiceHistoryId id;

    protected InvoiceHistory() { }

    public InvoiceHistory(InvoiceHistoryId id) {
        this.id = id;
    }

    public InvoiceHistoryId getId() {
        return id;
    }

    // Accesores de conveniencia (opcionales)
    public Integer getFiscalYear() { return id != null ? id.getFiscalYear() : null; }
    public String getProcessName() { return id != null ? id.getProcessName() : null; }
    public Integer getProcessId() { return id != null ? id.getProcessId() : null; }
    public String getInvoiceUuid() { return id != null ? id.getInvoiceUuid() : null; }
}
