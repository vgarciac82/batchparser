# Guía de Migración - Cambio en Estructura del API

## Resumen del Cambio

Se ha modificado la estructura del JSON retornado por el API para resolver un problema de correlación entre conceptos y sus impuestos/retenciones. Los impuestos y retenciones ahora están **anidados dentro de cada concepto** en lugar de estar en un array separado a nivel de comprobante.

## Problema Anterior

En la versión anterior, los impuestos de los conceptos estaban en un array separado (`conceptoImpuestos`) a nivel de comprobante, lo que causaba ambigüedad sobre qué impuesto/retención correspondía a cada concepto:

```json
{
  "successInvoices": [
    {
      "comprobante": {
        "conceptos": [
          {
            "cDescripcion": "ACERO ESTRUCTURAL PTR 2\"X2\"",
            "mImporte": 3982.86
          }
        ],
        "conceptoImpuestos": [
          {
            "cImpuesto": "001",
            "mImporte": 49.78575
          }
        ]
      }
    }
  ]
}
```

**Problema:** No había forma garantizada de saber qué impuesto pertenecía a qué concepto. Se asumía un orden 1 a 1, pero esto no era confiable.

## Nueva Estructura

Ahora los impuestos y retenciones están anidados **dentro** de cada concepto:

```json
{
  "successInvoices": [
    {
      "comprobante": {
        "conceptos": [
          {
            "cDescripcion": "ACERO ESTRUCTURAL PTR 2\"X2\"",
            "mImporte": 3982.86,
            "conceptoImpuestos": [
              {
                "cImpuesto": "002",
                "cTipoFactor": "Tasa",
                "nTasaOCuota": 0.160000,
                "mBase": 3982.860000,
                "mImporte": 637.257600
              }
            ],
            "conceptoRetenciones": [
              {
                "cImpuesto": "001",
                "nTasaOCuota": 0.012500,
                "mBase": 3982.860000,
                "mImporte": 49.785750
              }
            ]
          }
        ]
      }
    }
  ]
}
```

**Ventaja:** Ahora es completamente claro qué impuestos y retenciones pertenecen a cada concepto.

## Ejemplo Completo del JSON

```json
{
  "failInvoices": [],
  "successInvoices": [
    {
      "fileXML": "1.xml",
      "filePDF": "1.pdf",
      "comprobante": {
        "header": {
          "cFactura": "C6307117-E12E-40FA-B111-981672BF1DAE",
          "mImporteBruto": 26284.94,
          "mImporteConIva": 30161.95,
          "cRFCFactura": "TOCM630530FJ6",
          "cRazonSocial": "MARCO ANTONIO TORRES CERDA",
          "dFechaFactura": [2026, 2, 19, 14, 18, 26]
        },
        "impuestos": [
          {
            "cNombreImpuesto": "002",
            "mImporteImpuesto": 4205.590400,
            "nTazaImpuesto": 0.1600,
            "mImporteBase": 26284.940000
          }
        ],
        "retenciones": [
          {
            "cNombreRetencion": "001",
            "mImporteRetencion": 328.561750
          }
        ],
        "tipoCambio": null,
        "conceptos": [
          {
            "uuid": "C6307117-E12E-40FA-B111-981672BF1DAE",
            "cClaveProdServ": "30263600",
            "nCantidad": 42,
            "cClaveUnidad": "KGM",
            "cDescripcion": "ACERO ESTRUCTURAL PTR 2\"X2\"",
            "mValorUnitario": 94.83,
            "mImporte": 3982.860000,
            "conceptoImpuestos": [
              {
                "cImpuesto": "002",
                "cTipoFactor": "Tasa",
                "nTasaOCuota": 0.160000,
                "mBase": 3982.860000,
                "mImporte": 637.257600
              }
            ],
            "conceptoRetenciones": [
              {
                "cImpuesto": "001",
                "nTasaOCuota": 0.012500,
                "mBase": 3982.860000,
                "mImporte": 49.785750
              }
            ]
          },
          {
            "uuid": "C6307117-E12E-40FA-B111-981672BF1DAE",
            "cClaveProdServ": "30263603",
            "nCantidad": 30,
            "cClaveUnidad": "KGM",
            "cDescripcion": "ACERO ANGULO 2\"",
            "mValorUnitario": 94.83,
            "mImporte": 2844.900000,
            "conceptoImpuestos": [
              {
                "cImpuesto": "002",
                "cTipoFactor": "Tasa",
                "nTasaOCuota": 0.160000,
                "mBase": 2844.900000,
                "mImporte": 455.184000
              }
            ],
            "conceptoRetenciones": [
              {
                "cImpuesto": "001",
                "nTasaOCuota": 0.012500,
                "mBase": 2844.900000,
                "mImporte": 35.561250
              }
            ]
          }
        ]
      }
    }
  ]
}
```

## Cambios Necesarios en el Cliente

### 1. Estructura de Datos

**ANTES:**
```java
// Estructura anterior
class Comprobante {
    List<Concepto> conceptos;
    List<ConceptoImpuesto> conceptoImpuestos; // ❌ ELIMINADO
}

class Concepto {
    String descripcion;
    BigDecimal importe;
    // Sin impuestos ni retenciones
}
```

**AHORA:**
```java
// Nueva estructura
class Comprobante {
    List<Concepto> conceptos;
    // Ya no existe conceptoImpuestos a nivel de comprobante
}

class Concepto {
    String descripcion;
    BigDecimal importe;
    List<ConceptoImpuesto> conceptoImpuestos;     // ✅ NUEVO
    List<ConceptoImpuesto> conceptoRetenciones;   // ✅ NUEVO
}
```

### 2. Ejemplo de Código de Migración

#### JavaScript/TypeScript

**ANTES:**
```javascript
// ❌ Código anterior - NO FUNCIONA MÁS
const response = await fetch('/proc/batch', {
  method: 'POST',
  body: formData
});
const data = await response.json();

data.successInvoices.forEach(invoice => {
  const conceptos = invoice.comprobante.conceptos;
  const impuestos = invoice.comprobante.conceptoImpuestos; // ❌ Ya no existe aquí
  
  // ❌ Asumía correlación 1 a 1
  conceptos.forEach((concepto, index) => {
    console.log(concepto.descripcion, impuestos[index]);
  });
});
```

**AHORA:**
```javascript
// ✅ Código nuevo
const response = await fetch('/proc/batch', {
  method: 'POST',
  body: formData
});
const data = await response.json();

data.successInvoices.forEach(invoice => {
  const conceptos = invoice.comprobante.conceptos;
  
  conceptos.forEach(concepto => {
    console.log('Concepto:', concepto.cDescripcion);
    console.log('Importe:', concepto.mImporte);
    
    // ✅ Impuestos específicos de este concepto
    concepto.conceptoImpuestos?.forEach(impuesto => {
      console.log('  Impuesto:', impuesto.cImpuesto, 
                  'Importe:', impuesto.mImporte);
    });
    
    // ✅ Retenciones específicas de este concepto
    concepto.conceptoRetenciones?.forEach(retencion => {
      console.log('  Retención:', retencion.cImpuesto, 
                  'Importe:', retencion.mImporte);
    });
  });
});
```

#### C#

**ANTES:**
```csharp
// ❌ Código anterior
public class Comprobante
{
    public List<Concepto> Conceptos { get; set; }
    public List<ConceptoImpuesto> ConceptoImpuestos { get; set; } // ❌ Ya no existe
}

// ❌ Procesamiento anterior
foreach (var concepto in comprobante.Conceptos)
{
    // Asumía correlación 1 a 1
    var impuesto = comprobante.ConceptoImpuestos[index];
}
```

**AHORA:**
```csharp
// ✅ Código nuevo
public class Comprobante
{
    public List<Concepto> Conceptos { get; set; }
    // Ya no tiene ConceptoImpuestos a nivel de comprobante
}

public class Concepto
{
    public string CDescripcion { get; set; }
    public decimal MImporte { get; set; }
    public List<ConceptoImpuesto> ConceptoImpuestos { get; set; }      // ✅ NUEVO
    public List<ConceptoImpuesto> ConceptoRetenciones { get; set; }    // ✅ NUEVO
}

// ✅ Procesamiento nuevo
foreach (var concepto in comprobante.Conceptos)
{
    Console.WriteLine($"Concepto: {concepto.CDescripcion}");
    
    // ✅ Impuestos específicos de este concepto
    foreach (var impuesto in concepto.ConceptoImpuestos ?? new List<ConceptoImpuesto>())
    {
        Console.WriteLine($"  Impuesto: {impuesto.CImpuesto} - {impuesto.MImporte}");
    }
    
    // ✅ Retenciones específicas de este concepto
    foreach (var retencion in concepto.ConceptoRetenciones ?? new List<ConceptoImpuesto>())
    {
        Console.WriteLine($"  Retención: {retencion.CImpuesto} - {retencion.MImporte}");
    }
}
```

#### Python

**ANTES:**
```python
# ❌ Código anterior
response = requests.post('/proc/batch', files={'file': zip_file})
data = response.json()

for invoice in data['successInvoices']:
    conceptos = invoice['comprobante']['conceptos']
    impuestos = invoice['comprobante']['conceptoImpuestos']  # ❌ Ya no existe
    
    # ❌ Asumía correlación 1 a 1
    for i, concepto in enumerate(conceptos):
        print(concepto['cDescripcion'], impuestos[i])
```

**AHORA:**
```python
# ✅ Código nuevo
response = requests.post('/proc/batch', files={'file': zip_file})
data = response.json()

for invoice in data['successInvoices']:
    for concepto in invoice['comprobante']['conceptos']:
        print(f"Concepto: {concepto['cDescripcion']}")
        print(f"Importe: {concepto['mImporte']}")
        
        # ✅ Impuestos específicos de este concepto
        for impuesto in concepto.get('conceptoImpuestos', []):
            print(f"  Impuesto: {impuesto['cImpuesto']} - {impuesto['mImporte']}")
        
        # ✅ Retenciones específicas de este concepto
        for retencion in concepto.get('conceptoRetenciones', []):
            print(f"  Retención: {retencion['cImpuesto']} - {retencion['mImporte']}")
```

## Correspondencia con XML del SAT

La nueva estructura refleja fielmente cómo el SAT estructura los CFDIs:

```xml
<cfdi:Concepto ClaveProdServ="30263600" Descripcion="ACERO ESTRUCTURAL PTR 2&quot;X2&quot;" 
               Importe="3982.860000">
  <cfdi:Impuestos>
    <cfdi:Traslados>
      <cfdi:Traslado Base="3982.860000" Impuesto="002" Importe="637.257600"/>
    </cfdi:Traslados>
    <cfdi:Retenciones>
      <cfdi:Retencion Base="3982.860000" Impuesto="001" Importe="49.785750"/>
    </cfdi:Retenciones>
  </cfdi:Impuestos>
</cfdi:Concepto>
```

**Ahora el JSON tiene la misma jerarquía que el XML del SAT.**

## Checklist de Migración

- [ ] Actualizar modelos de datos (DTOs/clases)
- [ ] Eliminar referencias a `comprobante.conceptoImpuestos` (ya no existe)
- [ ] Acceder a impuestos desde `concepto.conceptoImpuestos`
- [ ] Acceder a retenciones desde `concepto.conceptoRetenciones`
- [ ] Actualizar validaciones y cálculos
- [ ] Actualizar tests unitarios
- [ ] Probar con facturas reales
- [ ] Actualizar documentación interna

## Notas Importantes

1. **Compatibilidad hacia atrás:** Este cambio NO es compatible hacia atrás. Los clientes deben actualizar su código.

2. **Validación:** Los arrays `conceptoImpuestos` y `conceptoRetenciones` pueden estar vacíos si el concepto no tiene impuestos o retenciones.

3. **Impuestos globales:** Los arrays `impuestos` y `retenciones` a nivel de `comprobante` siguen existiendo y contienen los totales agregados de todos los conceptos.

4. **Campos duplicados:** Notarás campos duplicados con diferentes capitalizaciones (ej: `cImpuesto` y `cimpuesto`). Esto es por compatibilidad con Lombok. Usa los campos con capitalización estándar.

## Soporte

Para dudas o problemas con la migración, contactar al equipo de desarrollo del API.

**Fecha del cambio:** 25 de febrero de 2026
