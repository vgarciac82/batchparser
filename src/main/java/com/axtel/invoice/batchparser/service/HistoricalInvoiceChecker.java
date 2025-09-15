package com.axtel.invoice.batchparser.service;

import java.util.Collection;
import java.util.Set;

public interface HistoricalInvoiceChecker {

    /** @return true si el UUID ya está en histórico (no pagar de nuevo). */
    boolean isDuplicateUuid(String uuid);

    /**
     * Para batches: devuelve el subconjunto de UUIDs que ya existen en histórico.
     * Útil para marcar fallas antes de continuar con SAT u otras validaciones.
     */
    Set<String> findDuplicates(Collection<String> uuids);
}
