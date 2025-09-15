package com.axtel.invoice.batchparser.service.impl;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.axtel.invoice.batchparser.repository.InvoiceHistoryRepository;
import com.axtel.invoice.batchparser.service.HistoricalInvoiceChecker;

@Service
public class HistoricalInvoiceCheckerImpl implements HistoricalInvoiceChecker {

    private static final Logger log = LoggerFactory.getLogger(HistoricalInvoiceCheckerImpl.class);
    private final InvoiceHistoryRepository repository;

    public HistoricalInvoiceCheckerImpl(InvoiceHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return false;
        }
        int exists = repository.existsByUuid(uuid);
        boolean duplicate = exists == 1;
        if (duplicate) {
            log.info("UUID ya existe en histórico: {}", uuid);
        } else {
            log.debug("UUID no encontrado en histórico: {}", uuid);
        }
        return duplicate;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findDuplicates(Collection<String> uuids) {
        if (uuids == null || uuids.isEmpty()) {
            return Set.of();
        }
        // Normalizamos a mayúsculas para consulta IN con UPPER() en SQL
        var uuidsUpper = uuids.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        var existingUpper = repository.findExistingUuidsUpper(uuidsUpper);
        var existingSetUpper = existingUpper.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        // Regresamos en su forma original los que resultaron duplicados
        return uuids.stream()
                .filter(s -> s != null && !s.isBlank())
                .filter(s -> existingSetUpper.contains(s.toUpperCase(Locale.ROOT)))
                .collect(Collectors.toSet());
    }
}
