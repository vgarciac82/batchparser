package com.axtel.invoice.batchparser.reader.sat;

import java.math.BigDecimal;

public interface SatCfdiStatusClient {
    SatConsultaResult consultar(String rfcEmisor, String rfcReceptor, BigDecimal total, String uuid);
}
