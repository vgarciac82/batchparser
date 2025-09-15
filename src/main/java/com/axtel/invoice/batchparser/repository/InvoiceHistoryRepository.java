package com.axtel.invoice.batchparser.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.axtel.invoice.batchparser.entities.InvoiceHistory;
import com.axtel.invoice.batchparser.entities.InvoiceHistoryId;

public interface InvoiceHistoryRepository extends JpaRepository<InvoiceHistory, InvoiceHistoryId> {

    // Consulta rápida de existencia por UUID (case-insensitive por seguridad)
    @Query(value = "SELECT CASE WHEN EXISTS ( " +
                   "  SELECT 1 FROM invoice_history WHERE UPPER(invoice_uuid) = UPPER(:uuid) " +
                   ") THEN 1 ELSE 0 END",
           nativeQuery = true)
    int existsByUuid(@Param("uuid") String uuid);

    // Búsqueda masiva: regresa los UUIDs que ya existen (para batches)
    @Query(value = "SELECT invoice_uuid FROM invoice_history " +
                   "WHERE UPPER(invoice_uuid) IN (:uuidsUpper)",
           nativeQuery = true)
    List<String> findExistingUuidsUpper(@Param("uuidsUpper") Collection<String> uuidsUpper);
}
