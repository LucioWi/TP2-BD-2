// ═══════════════════════════════════════════════════════════════════════════════
// SISTEMA DE DETECCIÓN DE FRAUDE FINANCIERO — SEED DATA COMPLETO
// Neo4j Browser | Cytoscape.js ready
//
// Patrones de fraude incluidos:
//   ① Lavado circular (layering A→B→C→A) — p01/p02/p03 — c01/c02/c03
//   ② Dispositivo compartido (account takeover) — p04/p05/p06 — d01
//   ③ Smurfing (fragmentación sub-umbral) — c04-c09 → c10 (mula)
//   ④ Identidad sintética — p09/p10/p11 — d02 (mismo dispositivo, simultáneos)
//   ⑤ Cuenta mula (money mule) — p12 — c10/c26
//   ⑥ PEP con alto movimiento — p13 — c14/c27
//   ⑦ Sancionado OFAC con cuentas activas — p14 — c15
//
// 20 Personas · 30 Cuentas · 10 Dispositivos · 50 Transacciones
// ═══════════════════════════════════════════════════════════════════════════════


// ─────────────────────────────────────────────────────────────────────────────
// 1. LIMPIAR — ejecutar primero para una carga limpia
// ─────────────────────────────────────────────────────────────────────────────
MATCH (n) DETACH DELETE n;


// ─────────────────────────────────────────────────────────────────────────────
// 2. CONSTRAINTS DE UNICIDAD
// ─────────────────────────────────────────────────────────────────────────────
CREATE CONSTRAINT persona_id      IF NOT EXISTS FOR (n:Persona)     REQUIRE n.id              IS UNIQUE;
CREATE CONSTRAINT persona_doc     IF NOT EXISTS FOR (n:Persona)     REQUIRE n.numeroDocumento  IS UNIQUE;
CREATE CONSTRAINT persona_email   IF NOT EXISTS FOR (n:Persona)     REQUIRE n.email            IS UNIQUE;
CREATE CONSTRAINT cuenta_id       IF NOT EXISTS FOR (n:Cuenta)      REQUIRE n.id               IS UNIQUE;
CREATE CONSTRAINT cuenta_numero   IF NOT EXISTS FOR (n:Cuenta)      REQUIRE n.numeroCuenta     IS UNIQUE;
CREATE CONSTRAINT cuenta_cbvu     IF NOT EXISTS FOR (n:Cuenta)      REQUIRE n.cbvu             IS UNIQUE;
CREATE CONSTRAINT disp_id         IF NOT EXISTS FOR (n:Dispositivo) REQUIRE n.id               IS UNIQUE;
CREATE CONSTRAINT disp_fp         IF NOT EXISTS FOR (n:Dispositivo) REQUIRE n.fingerprint      IS UNIQUE;
CREATE CONSTRAINT tx_id           IF NOT EXISTS FOR (n:Transaccion) REQUIRE n.id               IS UNIQUE;
CREATE CONSTRAINT tx_orden        IF NOT EXISTS FOR (n:Transaccion) REQUIRE n.numeroOrden      IS UNIQUE;


// ─────────────────────────────────────────────────────────────────────────────
// 3. ÍNDICES DE BÚSQUEDA
// ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX persona_riesgo     IF NOT EXISTS FOR (n:Persona)     ON (n.nivelRiesgo);
CREATE INDEX persona_pep        IF NOT EXISTS FOR (n:Persona)     ON (n.esPEP);
CREATE INDEX persona_sancionado IF NOT EXISTS FOR (n:Persona)     ON (n.esSancionado);
CREATE INDEX cuenta_estado      IF NOT EXISTS FOR (n:Cuenta)      ON (n.estado);
CREATE INDEX cuenta_banco       IF NOT EXISTS FOR (n:Cuenta)      ON (n.banco);
CREATE INDEX disp_ip            IF NOT EXISTS FOR (n:Dispositivo) ON (n.ipAddress);
CREATE INDEX disp_sospechoso    IF NOT EXISTS FOR (n:Dispositivo) ON (n.esSospechoso);
CREATE INDEX tx_estado          IF NOT EXISTS FOR (n:Transaccion) ON (n.estado);
CREATE INDEX tx_alertada        IF NOT EXISTS FOR (n:Transaccion) ON (n.esAlertada);
CREATE INDEX tx_riesgo          IF NOT EXISTS FOR (n:Transaccion) ON (n.nivelRiesgo);
CREATE INDEX tx_fecha           IF NOT EXISTS FOR (n:Transaccion) ON (n.fechaCreacion);


// ─────────────────────────────────────────────────────────────────────────────
// 4. PERSONAS (20)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // ① LAVADO CIRCULAR
  { id:'p01', nombre:'Carlos',    apellido:'Mendez',    tipoDocumento:'DNI',  numeroDocumento:'28456789',
    email:'c.mendez@protonmail.com',       telefono:'+5491145678901',
    fechaNacimiento:'1985-06-15', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:85, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2022-03-10T09:00:00', fechaActualizacion:'2024-09-15T11:20:00' },

  { id:'p02', nombre:'Ana',       apellido:'Garcia',    tipoDocumento:'DNI',  numeroDocumento:'31234567',
    email:'ana.garcia88@hotmail.com',      telefono:'+5491167890123',
    fechaNacimiento:'1988-11-22', genero:'FEMENINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'La Plata',
    nivelRiesgo:75, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2022-03-12T10:30:00', fechaActualizacion:'2024-09-15T11:25:00' },

  { id:'p03', nombre:'Roberto',   apellido:'Silva',     tipoDocumento:'DNI',  numeroDocumento:'25678901',
    email:'rsilva.finanzas@gmail.com',     telefono:'+5491178901234',
    fechaNacimiento:'1979-04-08', genero:'MASCULINO',
    pais:'Argentina', provincia:'Cordoba', ciudad:'Cordoba',
    nivelRiesgo:90, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2022-03-14T08:15:00', fechaActualizacion:'2024-10-02T09:00:00' },

  // ② DISPOSITIVO COMPARTIDO / ACCOUNT TAKEOVER
  { id:'p04', nombre:'Diego',     apellido:'Torres',    tipoDocumento:'DNI',  numeroDocumento:'33445566',
    email:'dtorres.2001@yahoo.com',        telefono:'+5491189012345',
    fechaNacimiento:'2001-08-30', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'Quilmes',
    nivelRiesgo:70, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2023-06-01T14:00:00', fechaActualizacion:'2024-11-01T08:00:00' },

  { id:'p05', nombre:'Lucia',     apellido:'Ramirez',   tipoDocumento:'DNI',  numeroDocumento:'29887766',
    email:'lucia.rz@outlook.com',          telefono:'+5491190123456',
    fechaNacimiento:'1992-02-14', genero:'FEMENINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'Lomas de Zamora',
    nivelRiesgo:65, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2023-06-02T15:00:00', fechaActualizacion:'2024-11-01T08:05:00' },

  { id:'p06', nombre:'Martin',    apellido:'Lopez',     tipoDocumento:'DNI',  numeroDocumento:'41223344',
    email:'mlopez.conta@gmail.com',        telefono:'+5491101234567',
    fechaNacimiento:'2000-05-20', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'Lanus',
    nivelRiesgo:60, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2023-06-03T16:00:00', fechaActualizacion:'2024-11-01T08:10:00' },

  // ③ SMURFING — victima/origen
  { id:'p07', nombre:'Sofia',     apellido:'Fernandez', tipoDocumento:'DNI',  numeroDocumento:'26543210',
    email:'sofia.fdez@gmail.com',          telefono:'+5491112345678',
    fechaNacimiento:'1990-07-03', genero:'FEMENINO',
    pais:'Argentina', provincia:'Santa Fe', ciudad:'Rosario',
    nivelRiesgo:20, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2021-07-20T10:00:00', fechaActualizacion:'2024-08-10T12:00:00' },

  { id:'p08', nombre:'Juan',      apellido:'Perez',     tipoDocumento:'CUIT', numeroDocumento:'20321098768',
    email:'jperez.inversiones@gmail.com',  telefono:'+5491123456789',
    fechaNacimiento:'1975-12-01', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:80, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2020-02-15T09:00:00', fechaActualizacion:'2024-11-05T16:00:00' },

  // ④ IDENTIDAD SINTETICA — creados casi simultáneamente, sin verificar
  { id:'p09', nombre:'Fernando',  apellido:'Quispe',    tipoDocumento:'DNI',  numeroDocumento:'99111222',
    email:'fquispe.01@tempmail.net',       telefono:'+5491134567890',
    fechaNacimiento:'1995-03-11', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:95, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2024-10-01T22:10:00', fechaActualizacion:'2024-10-01T22:10:00' },

  { id:'p10', nombre:'Valentina', apellido:'Cruz',      tipoDocumento:'DNI',  numeroDocumento:'99333444',
    email:'vcruz.temp@tempmail.net',       telefono:'+5491145679012',
    fechaNacimiento:'1997-07-19', genero:'FEMENINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:95, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2024-10-01T22:18:00', fechaActualizacion:'2024-10-01T22:18:00' },

  { id:'p11', nombre:'Matias',    apellido:'Rojas',     tipoDocumento:'DNI',  numeroDocumento:'99555666',
    email:'mrojas.id@tempmail.net',        telefono:'+5491156780123',
    fechaNacimiento:'1999-01-25', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:95, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2024-10-01T22:31:00', fechaActualizacion:'2024-10-01T22:31:00' },

  // ⑤ CUENTA MULA
  { id:'p12', nombre:'Pedro',     apellido:'Acosta',    tipoDocumento:'DNI',  numeroDocumento:'35667788',
    email:'pedromula55@gmail.com',         telefono:'+5491167891234',
    fechaNacimiento:'1983-09-17', genero:'MASCULINO',
    pais:'Argentina', provincia:'Mendoza', ciudad:'Mendoza',
    nivelRiesgo:88, esPEP:false, esSancionado:false, activa:true, verificada:false,
    fechaCreacion:'2023-04-05T11:00:00', fechaActualizacion:'2024-10-30T09:00:00' },

  // ⑥ PEP
  { id:'p13', nombre:'Elena',     apellido:'Vidal',     tipoDocumento:'DNI',  numeroDocumento:'24556677',
    email:'elena.vidal@senado.gov.ar',     telefono:'+5491178902345',
    fechaNacimiento:'1968-03-28', genero:'FEMENINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:45, esPEP:true, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2019-01-01T08:00:00', fechaActualizacion:'2024-09-01T10:00:00' },

  // ⑦ SANCIONADO OFAC
  { id:'p14', nombre:'Beatriz',   apellido:'Mora',      tipoDocumento:'DNI',  numeroDocumento:'22334455',
    email:'beatriz.mora@webmail.net',      telefono:'+5491189013456',
    fechaNacimiento:'1972-11-05', genero:'FEMENINO',
    pais:'Argentina', provincia:'Cordoba', ciudad:'Cordoba',
    nivelRiesgo:99, esPEP:false, esSancionado:true, activa:true, verificada:false,
    fechaCreacion:'2018-06-15T09:00:00', fechaActualizacion:'2024-07-20T15:00:00' },

  // HUB CONECTOR
  { id:'p15', nombre:'Victor',    apellido:'Huerta',    tipoDocumento:'CUIT', numeroDocumento:'20289001238',
    email:'victor.huerta@consultora.com.ar', telefono:'+5491190124567',
    fechaNacimiento:'1977-08-12', genero:'MASCULINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'CABA',
    nivelRiesgo:78, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2021-01-10T09:00:00', fechaActualizacion:'2024-11-10T08:00:00' },

  // NORMALES — p16 a p20
  { id:'p16', nombre:'Maria',     apellido:'Sanchez',   tipoDocumento:'DNI',  numeroDocumento:'27654321',
    email:'maria.sanchez.ok@gmail.com',    telefono:'+5491101235678',
    fechaNacimiento:'1993-05-10', genero:'FEMENINO',
    pais:'Argentina', provincia:'Buenos Aires', ciudad:'Mar del Plata',
    nivelRiesgo:10, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2021-03-15T10:00:00', fechaActualizacion:'2024-10-01T09:00:00' },

  { id:'p17', nombre:'Luis',      apellido:'Gomez',     tipoDocumento:'DNI',  numeroDocumento:'30112233',
    email:'luis.gomez.bio@gmail.com',      telefono:'+5491112346789',
    fechaNacimiento:'1987-09-25', genero:'MASCULINO',
    pais:'Argentina', provincia:'Santa Fe', ciudad:'Santa Fe',
    nivelRiesgo:15, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2020-07-20T11:00:00', fechaActualizacion:'2024-09-15T10:00:00' },

  { id:'p18', nombre:'Jorge',     apellido:'Castro',    tipoDocumento:'DNI',  numeroDocumento:'38990011',
    email:'jcastro.arq@yahoo.com',         telefono:'+5491123457890',
    fechaNacimiento:'1996-12-03', genero:'MASCULINO',
    pais:'Argentina', provincia:'Mendoza', ciudad:'Mendoza',
    nivelRiesgo:25, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2022-05-10T09:30:00', fechaActualizacion:'2024-10-05T14:00:00' },

  { id:'p19', nombre:'Pablo',     apellido:'Ruiz',      tipoDocumento:'DNI',  numeroDocumento:'44556677',
    email:'pablo.ruiz.dev@protonmail.com', telefono:'+5491134568901',
    fechaNacimiento:'2000-03-18', genero:'MASCULINO',
    pais:'Argentina', provincia:'Tucuman', ciudad:'San Miguel de Tucuman',
    nivelRiesgo:5,  esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2023-08-01T16:00:00', fechaActualizacion:'2024-11-01T08:00:00' },

  { id:'p20', nombre:'Carmen',    apellido:'Diaz',      tipoDocumento:'DNI',  numeroDocumento:'36778899',
    email:'carmen.diaz.vet@gmail.com',     telefono:'+5491145679123',
    fechaNacimiento:'1984-07-22', genero:'FEMENINO',
    pais:'Argentina', provincia:'Entre Rios', ciudad:'Parana',
    nivelRiesgo:10, esPEP:false, esSancionado:false, activa:true, verificada:true,
    fechaCreacion:'2021-11-20T12:00:00', fechaActualizacion:'2024-09-20T11:00:00' }
] AS props
CREATE (n:Persona)
SET n = props
SET n.fechaCreacion      = localdatetime(n.fechaCreacion)
SET n.fechaActualizacion = localdatetime(n.fechaActualizacion)
SET n.fechaNacimiento    = date(n.fechaNacimiento);


// ─────────────────────────────────────────────────────────────────────────────
// 5. CUENTAS (30)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // Cuentas del lavado circular
  { id:'c01', numeroCuenta:'1000000001', cbvu:'0000000000000000000001', alias:'carlos.bna', banco:'Banco Nacion',   tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'150000.00', montoAcumuladoHoy:'850000.00', cantidadTransaccionesEnviadas:5, cantidadTransaccionesRecibidas:3, fechaApertura:'2022-03-10T09:00:00', fechaActualizacion:'2024-09-15T11:20:00' },
  { id:'c02', numeroCuenta:'1000000002', cbvu:'0000000000000000000002', alias:'ana.galicia',  banco:'Banco Galicia', tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'280000.00', montoAcumuladoHoy:'620000.00', cantidadTransaccionesEnviadas:4, cantidadTransaccionesRecibidas:5, fechaApertura:'2022-03-12T10:30:00', fechaActualizacion:'2024-09-15T11:25:00' },
  { id:'c03', numeroCuenta:'1000000003', cbvu:'0000000000000000000003', alias:'roberto.bbva', banco:'BBVA',          tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'95000.00',  montoAcumuladoHoy:'575000.00', cantidadTransaccionesEnviadas:4, cantidadTransaccionesRecibidas:4, fechaApertura:'2022-03-14T08:15:00', fechaActualizacion:'2024-10-02T09:00:00' },

  // Cuentas del dispositivo compartido
  { id:'c04', numeroCuenta:'1000000004', cbvu:'0000000000000000000004', alias:'diego.san',    banco:'Santander',     tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'45000.00',  montoAcumuladoHoy:'18900.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2023-06-01T14:00:00', fechaActualizacion:'2024-11-01T08:00:00' },
  { id:'c05', numeroCuenta:'1000000005', cbvu:'0000000000000000000005', alias:'lucia.hsbc',   banco:'HSBC',          tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'62000.00',  montoAcumuladoHoy:'19600.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2023-06-02T15:00:00', fechaActualizacion:'2024-11-01T08:05:00' },
  { id:'c06', numeroCuenta:'1000000006', cbvu:'0000000000000000000006', alias:'martin.macro', banco:'Macro',         tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'38000.00',  montoAcumuladoHoy:'18800.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2023-06-03T16:00:00', fechaActualizacion:'2024-11-01T08:10:00' },

  // Cuentas smurfing origin (Juan Perez)
  { id:'c07', numeroCuenta:'1000000007', cbvu:'0000000000000000000007', alias:'juan.bna',     banco:'Banco Nacion',  tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'320000.00', montoAcumuladoHoy:'19450.00',  cantidadTransaccionesEnviadas:3, cantidadTransaccionesRecibidas:1, fechaApertura:'2020-02-15T09:00:00', fechaActualizacion:'2024-11-05T16:00:00' },
  { id:'c08', numeroCuenta:'1000000008', cbvu:'0000000000000000000008', alias:'juan.mp',      banco:'MercadoPago',   tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'85000.00',  montoAcumuladoHoy:'19000.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2021-05-10T11:00:00', fechaActualizacion:'2024-11-05T16:05:00' },
  { id:'c09', numeroCuenta:'1000000009', cbvu:'0000000000000000000009', alias:'sofia.gal',    banco:'Banco Galicia', tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'45000.00',  montoAcumuladoHoy:'18700.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2021-07-20T10:00:00', fechaActualizacion:'2024-08-10T12:00:00' },

  // Cuenta MULA — recibe de todas, envía en bloque
  { id:'c10', numeroCuenta:'1000000010', cbvu:'0000000000000000000010', alias:'pedro.lemon',  banco:'LemonCash',     tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'580000.00', montoAcumuladoHoy:'580000.00', cantidadTransaccionesEnviadas:3, cantidadTransaccionesRecibidas:14, fechaApertura:'2023-04-05T11:00:00', fechaActualizacion:'2024-10-30T09:00:00' },

  // Cuentas identidad sintética
  { id:'c11', numeroCuenta:'1000000011', cbvu:'0000000000000000000011', alias:'fq.mp',        banco:'MercadoPago',   tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'12000.00',  montoAcumuladoHoy:'12000.00',  cantidadTransaccionesEnviadas:3, cantidadTransaccionesRecibidas:2, fechaApertura:'2024-10-01T22:12:00', fechaActualizacion:'2024-10-02T10:00:00' },
  { id:'c12', numeroCuenta:'1000000012', cbvu:'0000000000000000000012', alias:'vc.uala',      banco:'Uala',          tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'8500.00',   montoAcumuladoHoy:'8500.00',   cantidadTransaccionesEnviadas:3, cantidadTransaccionesRecibidas:2, fechaApertura:'2024-10-01T22:20:00', fechaActualizacion:'2024-10-02T10:05:00' },
  { id:'c13', numeroCuenta:'1000000013', cbvu:'0000000000000000000013', alias:'mr.nrj',       banco:'NaranjaX',      tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'15000.00',  montoAcumuladoHoy:'15000.00',  cantidadTransaccionesEnviadas:3, cantidadTransaccionesRecibidas:2, fechaApertura:'2024-10-01T22:33:00', fechaActualizacion:'2024-10-02T10:10:00' },

  // PEP
  { id:'c14', numeroCuenta:'1000000014', cbvu:'0000000000000000000014', alias:'elena.bna',    banco:'Banco Nacion',  tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'1200000.00', montoAcumuladoHoy:'1050000.00', cantidadTransaccionesEnviadas:0, cantidadTransaccionesRecibidas:3, fechaApertura:'2019-01-01T08:00:00', fechaActualizacion:'2024-09-01T10:00:00' },

  // Sancionado — BLOQUEADA
  { id:'c15', numeroCuenta:'1000000015', cbvu:'0000000000000000000015', alias:'beatriz.san',  banco:'Santander',     tipoCuenta:'CORRIENTE',        estado:'BLOQUEADA', moneda:'ARS', saldo:'0.00',       montoAcumuladoHoy:'0.00',       cantidadTransaccionesEnviadas:0, cantidadTransaccionesRecibidas:0, motivoBloqueo:'Sancion OFAC Lista SDN - Resolucion UIF 2024', fechaApertura:'2018-06-15T09:00:00', fechaActualizacion:'2024-07-20T15:00:00' },

  // Hub conector (Victor Huerta tiene 3 cuentas)
  { id:'c16', numeroCuenta:'1000000016', cbvu:'0000000000000000000016', alias:'victor.bna',   banco:'Banco Nacion',  tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'850000.00',  montoAcumuladoHoy:'380000.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:5, fechaApertura:'2021-01-10T09:00:00', fechaActualizacion:'2024-11-10T08:00:00' },

  // Normales
  { id:'c17', numeroCuenta:'1000000017', cbvu:'0000000000000000000017', alias:'maria.gal',    banco:'Banco Galicia', tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'75000.00',   montoAcumuladoHoy:'25000.00',   cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:3, fechaApertura:'2021-03-15T10:00:00', fechaActualizacion:'2024-10-01T09:00:00' },
  { id:'c18', numeroCuenta:'1000000018', cbvu:'0000000000000000000018', alias:'luis.bbva',    banco:'BBVA',          tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'45000.00',   montoAcumuladoHoy:'15000.00',   cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:3, fechaApertura:'2020-07-20T11:00:00', fechaActualizacion:'2024-09-15T10:00:00' },
  { id:'c19', numeroCuenta:'1000000019', cbvu:'0000000000000000000019', alias:'jorge.mac',    banco:'Macro',         tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'25000.00',   montoAcumuladoHoy:'35000.00',   cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:2, fechaApertura:'2022-05-10T09:30:00', fechaActualizacion:'2024-10-05T14:00:00' },
  { id:'c20', numeroCuenta:'1000000020', cbvu:'0000000000000000000020', alias:'pablo.lemon',  banco:'LemonCash',     tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'18000.00',   montoAcumuladoHoy:'22000.00',   cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:2, fechaApertura:'2023-08-01T16:00:00', fechaActualizacion:'2024-11-01T08:00:00' },
  { id:'c21', numeroCuenta:'1000000021', cbvu:'0000000000000000000021', alias:'carmen.bna',   banco:'Banco Nacion',  tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'55000.00',   montoAcumuladoHoy:'12000.00',   cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:3, fechaApertura:'2021-11-20T12:00:00', fechaActualizacion:'2024-09-20T11:00:00' },

  // Segundas cuentas de actores clave
  { id:'c22', numeroCuenta:'1000000022', cbvu:'0000000000000000000022', alias:'carlos.gal2',  banco:'Banco Galicia', tipoCuenta:'AHORRO',           estado:'ACTIVA',   moneda:'ARS', saldo:'92000.00',   montoAcumuladoHoy:'430000.00',  cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:0, fechaApertura:'2022-05-20T10:00:00', fechaActualizacion:'2024-09-15T11:30:00' },
  { id:'c23', numeroCuenta:'1000000023', cbvu:'0000000000000000000023', alias:'ana.uala2',    banco:'Uala',          tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'34000.00',   montoAcumuladoHoy:'0.00',       cantidadTransaccionesEnviadas:0, cantidadTransaccionesRecibidas:0, fechaApertura:'2023-01-15T11:00:00', fechaActualizacion:'2024-09-15T11:35:00' },
  { id:'c24', numeroCuenta:'1000000024', cbvu:'0000000000000000000024', alias:'roberto.hsbc2',banco:'HSBC',          tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'180000.00',  montoAcumuladoHoy:'0.00',       cantidadTransaccionesEnviadas:0, cantidadTransaccionesRecibidas:0, fechaApertura:'2022-08-01T09:00:00', fechaActualizacion:'2024-10-02T09:05:00' },
  { id:'c25', numeroCuenta:'1000000025', cbvu:'0000000000000000000025', alias:'juan.san3',    banco:'Santander',     tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'135000.00',  montoAcumuladoHoy:'100000.00',  cantidadTransaccionesEnviadas:2, cantidadTransaccionesRecibidas:0, fechaApertura:'2022-11-10T10:00:00', fechaActualizacion:'2024-11-05T16:10:00' },
  { id:'c26', numeroCuenta:'1000000026', cbvu:'0000000000000000000026', alias:'pedro.bna2',   banco:'Banco Nacion',  tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'210000.00',  montoAcumuladoHoy:'48000.00',   cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:1, fechaApertura:'2023-05-01T11:00:00', fechaActualizacion:'2024-10-30T09:05:00' },
  { id:'c27', numeroCuenta:'1000000027', cbvu:'0000000000000000000027', alias:'elena.bbvainv', banco:'BBVA',         tipoCuenta:'INVERSION',        estado:'ACTIVA',   moneda:'ARS', saldo:'2500000.00', montoAcumuladoHoy:'800000.00',  cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:3, fechaApertura:'2019-06-01T08:00:00', fechaActualizacion:'2024-09-01T10:05:00' },
  { id:'c28', numeroCuenta:'1000000028', cbvu:'0000000000000000000028', alias:'victor.gal2',  banco:'Banco Galicia', tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'450000.00',  montoAcumuladoHoy:'180000.00',  cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:1, fechaApertura:'2021-06-01T09:00:00', fechaActualizacion:'2024-11-10T08:05:00' },
  { id:'c29', numeroCuenta:'1000000029', cbvu:'0000000000000000000029', alias:'victor.mp3',   banco:'MercadoPago',   tipoCuenta:'BILLETERA_VIRTUAL', estado:'ACTIVA',   moneda:'ARS', saldo:'88000.00',   montoAcumuladoHoy:'300000.00',  cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:0, fechaApertura:'2022-03-15T12:00:00', fechaActualizacion:'2024-11-10T08:10:00' },
  { id:'c30', numeroCuenta:'1000000030', cbvu:'0000000000000000000030', alias:'luis.macro2',  banco:'Macro',         tipoCuenta:'CORRIENTE',        estado:'ACTIVA',   moneda:'ARS', saldo:'320000.00',  montoAcumuladoHoy:'55000.00',   cantidadTransaccionesEnviadas:1, cantidadTransaccionesRecibidas:0, fechaApertura:'2021-02-10T10:00:00', fechaActualizacion:'2024-09-15T10:05:00' }
] AS props
CREATE (n:Cuenta)
SET n = props
SET n.fechaApertura      = localdatetime(n.fechaApertura)
SET n.fechaActualizacion = localdatetime(n.fechaActualizacion);


// ─────────────────────────────────────────────────────────────────────────────
// 6. DISPOSITIVOS (10)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // d01 — Tor exit node, compartido p04+p05+p06 (account takeover)
  { id:'d01', tipoDispositivo:'MOVIL',       marca:'Apple',    modelo:'iPhone 14',        sistemaOperativo:'iOS',     versionSO:'16.6',
    userAgent:'Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15',
    fingerprint:'fp-d01-tor-shared-device',
    ipAddress:'185.220.101.45', ipPais:'Netherlands', ipCiudad:'Amsterdam',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:true,
    esEmulador:false, esRooteado:false, esSospechoso:true,
    cantidadSesiones:87, cantidadPersonasAsociadas:3,
    fechaRegistro:'2023-06-01T14:00:00', fechaActualizacion:'2024-11-01T08:00:00', fechaUltimaActividad:'2024-11-01T07:58:00' },

  // d02 — VPN + emulador, compartido p09+p10+p11 (identidad sintética)
  { id:'d02', tipoDispositivo:'MOVIL',       marca:'Unknown',  modelo:'Android Emulator', sistemaOperativo:'Android', versionSO:'12.0',
    userAgent:'Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64)',
    fingerprint:'fp-d02-emulator-vpn-synthetic',
    ipAddress:'45.142.212.100', ipPais:'Romania', ipCiudad:'Bucharest',
    ipEsProxy:false, ipEsVPN:true, ipEsTor:false,
    esEmulador:true, esRooteado:false, esSospechoso:true,
    cantidadSesiones:12, cantidadPersonasAsociadas:3,
    fechaRegistro:'2024-10-01T22:08:00', fechaActualizacion:'2024-10-02T10:00:00', fechaUltimaActividad:'2024-10-02T09:55:00' },

  // d03 — rooteado, Carlos Mendez (circular)
  { id:'d03', tipoDispositivo:'MOVIL',       marca:'Motorola', modelo:'Moto G82',         sistemaOperativo:'Android', versionSO:'11.0',
    userAgent:'Mozilla/5.0 (Linux; Android 11; Moto G82)',
    fingerprint:'fp-d03-carlos-rooted',
    ipAddress:'201.216.45.23', ipPais:'Argentina', ipCiudad:'CABA',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:true, esSospechoso:true,
    cantidadSesiones:145, cantidadPersonasAsociadas:1,
    fechaRegistro:'2022-03-10T09:00:00', fechaActualizacion:'2024-09-15T11:20:00', fechaUltimaActividad:'2024-09-15T11:18:00' },

  // d04 — compartido Ana+Roberto (circular, mismos actores)
  { id:'d04', tipoDispositivo:'COMPUTADORA', marca:'Lenovo',   modelo:'ThinkPad E14',     sistemaOperativo:'Windows', versionSO:'11',
    userAgent:'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    fingerprint:'fp-d04-ana-roberto-shared',
    ipAddress:'200.122.100.55', ipPais:'Argentina', ipCiudad:'La Plata',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:true,
    cantidadSesiones:220, cantidadPersonasAsociadas:2,
    fechaRegistro:'2022-03-12T10:30:00', fechaActualizacion:'2024-10-02T09:00:00', fechaUltimaActividad:'2024-10-02T08:58:00' },

  // d05 — proxy, Juan Perez + Pedro Mula (smurfing/mula)
  { id:'d05', tipoDispositivo:'MOVIL',       marca:'Xiaomi',   modelo:'Redmi Note 10',    sistemaOperativo:'Android', versionSO:'10.0',
    userAgent:'Mozilla/5.0 (Linux; Android 10; Redmi Note 10)',
    fingerprint:'fp-d05-juan-pedro-proxy',
    ipAddress:'190.210.15.88', ipPais:'Argentina', ipCiudad:'CABA',
    ipEsProxy:true, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:true,
    cantidadSesiones:310, cantidadPersonasAsociadas:2,
    fechaRegistro:'2020-02-15T09:00:00', fechaActualizacion:'2024-11-05T16:00:00', fechaUltimaActividad:'2024-11-05T15:58:00' },

  // d06 — clean, Victor Huerta hub
  { id:'d06', tipoDispositivo:'COMPUTADORA', marca:'Apple',    modelo:'MacBook Pro',      sistemaOperativo:'macOS',   versionSO:'14.0',
    userAgent:'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
    fingerprint:'fp-d06-victor-hub-clean',
    ipAddress:'200.35.100.22', ipPais:'Argentina', ipCiudad:'CABA',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:false,
    cantidadSesiones:890, cantidadPersonasAsociadas:1,
    fechaRegistro:'2021-01-10T09:00:00', fechaActualizacion:'2024-11-10T08:00:00', fechaUltimaActividad:'2024-11-10T07:55:00' },

  // d07 — clean, Elena Vidal PEP
  { id:'d07', tipoDispositivo:'MOVIL',       marca:'Apple',    modelo:'iPhone 15 Pro',    sistemaOperativo:'iOS',     versionSO:'17.1',
    userAgent:'Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X)',
    fingerprint:'fp-d07-elena-pep-clean',
    ipAddress:'200.120.50.10', ipPais:'Argentina', ipCiudad:'CABA',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:false,
    cantidadSesiones:1240, cantidadPersonasAsociadas:1,
    fechaRegistro:'2019-01-01T08:00:00', fechaActualizacion:'2024-09-01T10:00:00', fechaUltimaActividad:'2024-09-01T09:58:00' },

  // d08 — sospechoso, Beatriz Mora sancionado
  { id:'d08', tipoDispositivo:'MOVIL',       marca:'Nokia',    modelo:'5.3',              sistemaOperativo:'Android', versionSO:'10.0',
    userAgent:'Mozilla/5.0 (Linux; Android 10; Nokia 5.3)',
    fingerprint:'fp-d08-beatriz-sancionado',
    ipAddress:'190.180.75.40', ipPais:'Argentina', ipCiudad:'Cordoba',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:true,
    cantidadSesiones:55, cantidadPersonasAsociadas:1,
    fechaRegistro:'2018-06-15T09:00:00', fechaActualizacion:'2024-07-20T15:00:00', fechaUltimaActividad:'2024-07-20T14:58:00' },

  // d09 — normales, Maria + Luis
  { id:'d09', tipoDispositivo:'COMPUTADORA', marca:'HP',       modelo:'Pavilion 15',      sistemaOperativo:'Windows', versionSO:'11',
    userAgent:'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/119.0',
    fingerprint:'fp-d09-normales-maria-luis',
    ipAddress:'200.45.200.15', ipPais:'Argentina', ipCiudad:'Mar del Plata',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:false,
    cantidadSesiones:520, cantidadPersonasAsociadas:2,
    fechaRegistro:'2021-03-15T10:00:00', fechaActualizacion:'2024-10-01T09:00:00', fechaUltimaActividad:'2024-10-01T08:58:00' },

  // d10 — normales, Jorge + Pablo + Carmen
  { id:'d10', tipoDispositivo:'MOVIL',       marca:'Samsung',  modelo:'Galaxy A52',       sistemaOperativo:'Android', versionSO:'13.0',
    userAgent:'Mozilla/5.0 (Linux; Android 13; Samsung Galaxy A52)',
    fingerprint:'fp-d10-normales-jorge-pablo-carmen',
    ipAddress:'200.60.150.33', ipPais:'Argentina', ipCiudad:'Mendoza',
    ipEsProxy:false, ipEsVPN:false, ipEsTor:false,
    esEmulador:false, esRooteado:false, esSospechoso:false,
    cantidadSesiones:380, cantidadPersonasAsociadas:3,
    fechaRegistro:'2022-05-10T09:30:00', fechaActualizacion:'2024-10-05T14:00:00', fechaUltimaActividad:'2024-10-05T13:58:00' }
] AS props
CREATE (n:Dispositivo)
SET n = props
SET n.fechaRegistro          = localdatetime(n.fechaRegistro)
SET n.fechaActualizacion     = localdatetime(n.fechaActualizacion)
SET n.fechaUltimaActividad   = localdatetime(n.fechaUltimaActividad);


// ─────────────────────────────────────────────────────────────────────────────
// 7. TRANSACCIONES (50)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // ─── ① LAVADO CIRCULAR — dos ciclos completos c01→c02→c03→c01 ────────────
  { id:'t001', numeroOrden:'TXN-2024-001', monto:'850000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Transferencia entre cuentas propias',    ipAddress:'201.216.45.23', nivelRiesgo:90, esAlertada:true,  motivoAlerta:'Monto alto sin justificacion economica', esDuplicada:false, fechaCreacion:'2024-09-10T10:15:00', fechaActualizacion:'2024-09-10T10:15:30' },
  { id:'t002', numeroOrden:'TXN-2024-002', monto:'820000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Operacion entre cuentas vinculadas',     ipAddress:'200.122.100.55', nivelRiesgo:90, esAlertada:true,  motivoAlerta:'Patron de flujo circular detectado',    esDuplicada:false, fechaCreacion:'2024-09-10T11:45:00', fechaActualizacion:'2024-09-10T11:45:30' },
  { id:'t003', numeroOrden:'TXN-2024-003', monto:'790000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Reintegro entre cuentas',                ipAddress:'200.122.100.55', nivelRiesgo:92, esAlertada:true,  motivoAlerta:'Cierre de ciclo circular A-B-C-A',      esDuplicada:false, fechaCreacion:'2024-09-10T14:20:00', fechaActualizacion:'2024-09-10T14:20:30' },
  { id:'t004', numeroOrden:'TXN-2024-004', monto:'430000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'PRESTAMO',   descripcion:'Aporte a cuenta vinculada',              ipAddress:'201.216.45.23', nivelRiesgo:75, esAlertada:true,  motivoAlerta:'Segunda cuenta del mismo titular alimenta el ciclo', esDuplicada:false, fechaCreacion:'2024-09-15T09:30:00', fechaActualizacion:'2024-09-15T09:30:30' },
  { id:'t005', numeroOrden:'TXN-2024-005', monto:'620000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Segunda vuelta del ciclo de lavado',     ipAddress:'201.216.45.23', nivelRiesgo:88, esAlertada:true,  motivoAlerta:'Patron circular repetido - segundo ciclo', esDuplicada:false, fechaCreacion:'2024-10-05T10:00:00', fechaActualizacion:'2024-10-05T10:00:30' },
  { id:'t006', numeroOrden:'TXN-2024-006', monto:'600000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Transferencia en red de lavado',         ipAddress:'200.122.100.55', nivelRiesgo:88, esAlertada:true,  motivoAlerta:'Nodo intermedio en ciclo de lavado',    esDuplicada:false, fechaCreacion:'2024-10-05T12:30:00', fechaActualizacion:'2024-10-05T12:30:30' },
  { id:'t007', numeroOrden:'TXN-2024-007', monto:'575000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Cierre segundo ciclo circular',          ipAddress:'200.122.100.55', nivelRiesgo:90, esAlertada:true,  motivoAlerta:'Reiteracion ciclo circular confirmado', esDuplicada:false, fechaCreacion:'2024-10-05T15:45:00', fechaActualizacion:'2024-10-05T15:45:30' },
  { id:'t008', numeroOrden:'TXN-2024-008', monto:'200000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'HONORARIOS', descripcion:'Extraccion hacia cuenta puente',         ipAddress:'200.122.100.55', nivelRiesgo:72, esAlertada:true,  motivoAlerta:'Extraccion de fondos del ciclo al hub', esDuplicada:false, fechaCreacion:'2024-10-06T09:00:00', fechaActualizacion:'2024-10-06T09:00:30' },

  // ─── ③ SMURFING — 12 transferencias sub-umbral hacia mula c10 ────────────
  { id:'t009', numeroOrden:'TXN-2024-009', monto:'9500.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:75, esAlertada:true,  motivoAlerta:'Transferencia sub-umbral hacia mula - patron smurfing', esDuplicada:false, fechaCreacion:'2024-11-01T08:10:00', fechaActualizacion:'2024-11-01T08:10:10' },
  { id:'t010', numeroOrden:'TXN-2024-010', monto:'9800.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:75, esAlertada:true,  motivoAlerta:'Transferencia sub-umbral - mismo destino hora seguida', esDuplicada:false, fechaCreacion:'2024-11-01T08:14:00', fechaActualizacion:'2024-11-01T08:14:10' },
  { id:'t011', numeroOrden:'TXN-2024-011', monto:'9200.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:75, esAlertada:true,  motivoAlerta:'Transferencia sub-umbral repetida - mismo dispositivo', esDuplicada:false, fechaCreacion:'2024-11-01T08:18:00', fechaActualizacion:'2024-11-01T08:18:10' },
  { id:'t012', numeroOrden:'TXN-2024-012', monto:'9700.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Transferencia personal',                 ipAddress:'190.210.15.88', nivelRiesgo:80, esAlertada:true,  motivoAlerta:'Orquestador detectado - montos similares mismo destino', esDuplicada:false, fechaCreacion:'2024-11-01T08:22:00', fechaActualizacion:'2024-11-01T08:22:10' },
  { id:'t013', numeroOrden:'TXN-2024-013', monto:'9300.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Transferencia personal',                 ipAddress:'190.210.15.88', nivelRiesgo:80, esAlertada:true,  motivoAlerta:'Segunda cuenta orquestador - mismo patron',            esDuplicada:false, fechaCreacion:'2024-11-01T08:26:00', fechaActualizacion:'2024-11-01T08:26:10' },
  { id:'t014', numeroOrden:'TXN-2024-014', monto:'9600.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre conocidos',                   ipAddress:'185.220.101.45', nivelRiesgo:70, esAlertada:true,  motivoAlerta:'Cuenta comprometida participando en smurfing',          esDuplicada:false, fechaCreacion:'2024-11-01T08:30:00', fechaActualizacion:'2024-11-01T08:30:10' },
  { id:'t015', numeroOrden:'TXN-2024-015', monto:'9400.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:77, esAlertada:true,  motivoAlerta:'Reiteracion sub-umbral misma cuenta origen',           esDuplicada:false, fechaCreacion:'2024-11-01T09:05:00', fechaActualizacion:'2024-11-01T09:05:10' },
  { id:'t016', numeroOrden:'TXN-2024-016', monto:'9800.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:77, esAlertada:true,  motivoAlerta:'Reiteracion sub-umbral misma cuenta origen',           esDuplicada:false, fechaCreacion:'2024-11-01T09:10:00', fechaActualizacion:'2024-11-01T09:10:10' },
  { id:'t017', numeroOrden:'TXN-2024-017', monto:'9750.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Transferencia personal',                 ipAddress:'190.210.15.88', nivelRiesgo:82, esAlertada:true,  motivoAlerta:'Tercera transferencia orquestador mismo dia',           esDuplicada:false, fechaCreacion:'2024-11-01T09:15:00', fechaActualizacion:'2024-11-01T09:15:10' },
  { id:'t018', numeroOrden:'TXN-2024-018', monto:'9500.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Transferencia personal',                 ipAddress:'190.210.15.88', nivelRiesgo:79, esAlertada:true,  motivoAlerta:'Cuarta transferencia billetera orquestador',            esDuplicada:false, fechaCreacion:'2024-11-01T09:20:00', fechaActualizacion:'2024-11-01T09:20:10' },
  { id:'t019', numeroOrden:'TXN-2024-019', monto:'9100.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre conocidos',                   ipAddress:'185.220.101.45', nivelRiesgo:71, esAlertada:true,  motivoAlerta:'Segunda ronda smurfing cuenta comprometida',           esDuplicada:false, fechaCreacion:'2024-11-01T09:25:00', fechaActualizacion:'2024-11-01T09:25:10' },
  { id:'t020', numeroOrden:'TXN-2024-020', monto:'9600.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago servicio',                          ipAddress:'185.220.101.45', nivelRiesgo:76, esAlertada:true,  motivoAlerta:'Patron smurfing confirmado - 12 transferencias',        esDuplicada:false, fechaCreacion:'2024-11-01T09:30:00', fechaActualizacion:'2024-11-01T09:30:10' },

  // ─── ⑤ MULA — extraccion y redistribucion ─────────────────────────────────
  { id:'t021', numeroOrden:'TXN-2024-021', monto:'85000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago honorarios profesionales',          ipAddress:'190.210.15.88', nivelRiesgo:80, esAlertada:true,  motivoAlerta:'Mula agrega y transfiere al hub - lote 1',              esDuplicada:false, fechaCreacion:'2024-11-01T11:00:00', fechaActualizacion:'2024-11-01T11:00:30' },
  { id:'t022', numeroOrden:'TXN-2024-022', monto:'95000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago honorarios profesionales',          ipAddress:'190.210.15.88', nivelRiesgo:82, esAlertada:true,  motivoAlerta:'Mula agrega y transfiere al hub - lote 2',              esDuplicada:false, fechaCreacion:'2024-11-01T11:30:00', fechaActualizacion:'2024-11-01T11:30:30' },
  { id:'t023', numeroOrden:'TXN-2024-023', monto:'48000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Transferencia a cuenta operativa',       ipAddress:'190.210.15.88', nivelRiesgo:75, esAlertada:true,  motivoAlerta:'Mula usa cuenta secundaria propia como escala',          esDuplicada:false, fechaCreacion:'2024-11-01T12:00:00', fechaActualizacion:'2024-11-01T12:00:30' },
  { id:'t024', numeroOrden:'TXN-2024-024', monto:'45000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago servicios empresariales',           ipAddress:'190.210.15.88', nivelRiesgo:70, esAlertada:true,  motivoAlerta:'Segunda cuenta mula reenvia al hub',                    esDuplicada:false, fechaCreacion:'2024-11-01T13:00:00', fechaActualizacion:'2024-11-01T13:00:30' },
  { id:'t025', numeroOrden:'TXN-2024-025', monto:'180000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago capital de trabajo',                ipAddress:'200.35.100.22', nivelRiesgo:68, esAlertada:true,  motivoAlerta:'Hub redistribuye fondos de mula a cuenta secundaria',   esDuplicada:false, fechaCreacion:'2024-11-01T14:00:00', fechaActualizacion:'2024-11-01T14:00:30' },

  // ─── ④ IDENTIDAD SINTETICA — mini ciclo + flujo hacia mula ───────────────
  { id:'t026', numeroOrden:'TXN-2024-026', monto:'5000.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre amigos',                      ipAddress:'45.142.212.100', nivelRiesgo:90, esAlertada:true,  motivoAlerta:'Identidad sintetica - ciclo entre cuentas creadas el mismo dia', esDuplicada:false, fechaCreacion:'2024-10-02T10:05:00', fechaActualizacion:'2024-10-02T10:05:10' },
  { id:'t027', numeroOrden:'TXN-2024-027', monto:'4500.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre amigos',                      ipAddress:'45.142.212.100', nivelRiesgo:90, esAlertada:true,  motivoAlerta:'Segundo nodo ciclo identidad sintetica',                esDuplicada:false, fechaCreacion:'2024-10-02T10:08:00', fechaActualizacion:'2024-10-02T10:08:10' },
  { id:'t028', numeroOrden:'TXN-2024-028', monto:'4200.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre amigos',                      ipAddress:'45.142.212.100', nivelRiesgo:92, esAlertada:true,  motivoAlerta:'Cierre ciclo sintetico - mismo dispositivo emulador',   esDuplicada:false, fechaCreacion:'2024-10-02T10:12:00', fechaActualizacion:'2024-10-02T10:12:10' },
  { id:'t029', numeroOrden:'TXN-2024-029', monto:'12000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Envio a conocido',                       ipAddress:'45.142.212.100', nivelRiesgo:88, esAlertada:true,  motivoAlerta:'Identidad sintetica transfiere a cuenta mula',          esDuplicada:false, fechaCreacion:'2024-10-02T10:20:00', fechaActualizacion:'2024-10-02T10:20:10' },
  { id:'t030', numeroOrden:'TXN-2024-030', monto:'8000.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Envio a conocido',                       ipAddress:'45.142.212.100', nivelRiesgo:85, esAlertada:true,  motivoAlerta:'Segunda identidad sintetica transfiere a mula',         esDuplicada:false, fechaCreacion:'2024-10-02T10:25:00', fechaActualizacion:'2024-10-02T10:25:10' },
  { id:'t031', numeroOrden:'TXN-2024-031', monto:'9500.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Envio a conocido',                       ipAddress:'45.142.212.100', nivelRiesgo:83, esAlertada:true,  motivoAlerta:'Tercera identidad sintetica transfiere a mula',         esDuplicada:false, fechaCreacion:'2024-10-02T10:30:00', fechaActualizacion:'2024-10-02T10:30:10' },
  { id:'t032', numeroOrden:'TXN-2024-032', monto:'6500.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre amigos',                      ipAddress:'45.142.212.100', nivelRiesgo:88, esAlertada:true,  motivoAlerta:'Segunda vuelta ciclo sintetico - patron repetido',      esDuplicada:false, fechaCreacion:'2024-10-03T09:00:00', fechaActualizacion:'2024-10-03T09:00:10' },
  { id:'t033', numeroOrden:'TXN-2024-033', monto:'6000.00',   moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre amigos',                      ipAddress:'45.142.212.100', nivelRiesgo:87, esAlertada:true,  motivoAlerta:'Reiteracion ciclo sintetico confirmada',                esDuplicada:false, fechaCreacion:'2024-10-03T09:05:00', fechaActualizacion:'2024-10-03T09:05:10' },

  // ─── ⑥ PEP — transacciones de alto valor ─────────────────────────────────
  { id:'t034', numeroOrden:'TXN-2024-034', monto:'800000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'INVERSION',  descripcion:'Movimiento entre cuentas propias',       ipAddress:'200.120.50.10', nivelRiesgo:55, esAlertada:true,  motivoAlerta:'PEP: movimiento entre cuentas propias > umbral EDD',   esDuplicada:false, fechaCreacion:'2024-09-01T10:10:00', fechaActualizacion:'2024-09-01T10:10:30' },
  { id:'t035', numeroOrden:'TXN-2024-035', monto:'500000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Honorarios consultoria',                 ipAddress:'200.35.100.22', nivelRiesgo:60, esAlertada:true,  motivoAlerta:'Hub transfiere monto alto a cuenta PEP',               esDuplicada:false, fechaCreacion:'2024-10-01T15:00:00', fechaActualizacion:'2024-10-01T15:00:30' },
  { id:'t036', numeroOrden:'TXN-2024-036', monto:'300000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Honorarios consultoria',                 ipAddress:'200.35.100.22', nivelRiesgo:40, esAlertada:false, motivoAlerta:null,                                               esDuplicada:false, fechaCreacion:'2024-10-15T11:00:00', fechaActualizacion:'2024-10-15T11:00:30' },
  { id:'t037', numeroOrden:'TXN-2024-037', monto:'250000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago servicios profesionales',           ipAddress:'200.35.100.22', nivelRiesgo:52, esAlertada:true,  motivoAlerta:'Segundo pago alto a PEP desde hub mismo mes',          esDuplicada:false, fechaCreacion:'2024-10-20T10:00:00', fechaActualizacion:'2024-10-20T10:00:30' },

  // ─── ⑦ SANCIONADO — transacciones bloqueadas/rechazadas ──────────────────
  { id:'t038', numeroOrden:'TXN-2024-038', monto:'250000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'BLOQUEADA',  concepto:'VARIOS',     descripcion:'Intento transferencia desde cuenta sancionada', ipAddress:'190.180.75.40', nivelRiesgo:99, esAlertada:true, motivoAlerta:'BLOQUEADA: cuenta origen en lista OFAC SDN',           esDuplicada:false, fechaCreacion:'2024-07-15T09:00:00', fechaActualizacion:'2024-07-15T09:00:05' },
  { id:'t039', numeroOrden:'TXN-2024-039', monto:'150000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'RECHAZADA',  concepto:'VARIOS',     descripcion:'Intento envio hacia cuenta sancionada',  ipAddress:'200.45.200.15', nivelRiesgo:85, esAlertada:true,  motivoAlerta:'RECHAZADA: destinatario en lista OFAC SDN',            esDuplicada:false, fechaCreacion:'2024-07-20T14:00:00', fechaActualizacion:'2024-07-20T14:00:05' },
  { id:'t040', numeroOrden:'TXN-2024-040', monto:'350000.00', moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'RECHAZADA',  concepto:'VARIOS',     descripcion:'Intento lavado desde cuenta sancionada', ipAddress:'190.180.75.40', nivelRiesgo:99, esAlertada:true,  motivoAlerta:'RECHAZADA: intento evasion sancion hacia hub',         esDuplicada:false, fechaCreacion:'2024-07-25T10:00:00', fechaActualizacion:'2024-07-25T10:00:05' },

  // ─── TRANSACCIONES NORMALES ────────────────────────────────────────────────
  { id:'t041', numeroOrden:'TXN-2024-041', monto:'25000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'ALQUILER',   descripcion:'Pago alquiler mensual',                  ipAddress:'200.45.200.15', nivelRiesgo:5,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-01T08:00:00', fechaActualizacion:'2024-10-01T08:00:10' },
  { id:'t042', numeroOrden:'TXN-2024-042', monto:'15000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago cuota sociedad',                    ipAddress:'200.45.200.15', nivelRiesgo:8,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-02T09:00:00', fechaActualizacion:'2024-10-02T09:00:10' },
  { id:'t043', numeroOrden:'TXN-2024-043', monto:'35000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'SUELDO',     descripcion:'Adelanto haberes',                       ipAddress:'200.60.150.33', nivelRiesgo:3,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-05T10:00:00', fechaActualizacion:'2024-10-05T10:00:10' },
  { id:'t044', numeroOrden:'TXN-2024-044', monto:'22000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago deuda personal',                    ipAddress:'200.60.150.33', nivelRiesgo:10, esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-06T11:00:00', fechaActualizacion:'2024-10-06T11:00:10' },
  { id:'t045', numeroOrden:'TXN-2024-045', monto:'18000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Reintegro gastos compartidos',           ipAddress:'200.60.150.33', nivelRiesgo:7,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-07T14:00:00', fechaActualizacion:'2024-10-07T14:00:10' },
  { id:'t046', numeroOrden:'TXN-2024-046', monto:'45000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'PRESTAMO',   descripcion:'Devolucion prestamo entre personas',     ipAddress:'190.210.15.88', nivelRiesgo:15, esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-10T09:00:00', fechaActualizacion:'2024-10-10T09:00:10' },
  { id:'t047', numeroOrden:'TXN-2024-047', monto:'30000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'SERVICIOS',  descripcion:'Pago facturas servicios',                ipAddress:'200.45.200.15', nivelRiesgo:5,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-12T10:00:00', fechaActualizacion:'2024-10-12T10:00:10' },
  { id:'t048', numeroOrden:'TXN-2024-048', monto:'28000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'ALQUILER',   descripcion:'Pago alquiler garaje',                   ipAddress:'200.45.200.15', nivelRiesgo:8,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-15T08:00:00', fechaActualizacion:'2024-10-15T08:00:10' },
  { id:'t049', numeroOrden:'TXN-2024-049', monto:'12000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'APP_MOVIL',    estado:'COMPLETADA', concepto:'VARIOS',     descripcion:'Pago entre conocidos',                   ipAddress:'200.60.150.33', nivelRiesgo:6,  esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-18T15:00:00', fechaActualizacion:'2024-10-18T15:00:10' },
  { id:'t050', numeroOrden:'TXN-2024-050', monto:'55000.00',  moneda:'ARS', tipo:'TRANSFERENCIA', canal:'HOME_BANKING', estado:'COMPLETADA', concepto:'PRESTAMO',   descripcion:'Prestamo amigos - devolucion',           ipAddress:'200.45.200.15', nivelRiesgo:12, esAlertada:false, motivoAlerta:null, esDuplicada:false, fechaCreacion:'2024-10-20T11:00:00', fechaActualizacion:'2024-10-20T11:00:10' }
] AS props
CREATE (n:Transaccion)
SET n = props
SET n.fechaCreacion      = localdatetime(n.fechaCreacion)
SET n.fechaActualizacion = localdatetime(n.fechaActualizacion);


// ─────────────────────────────────────────────────────────────────────────────
// 8. RELACIÓN Persona → Cuenta (POSEE_CUENTA)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // Cuentas principales
  {p:'p01',c:'c01',fecha:'2022-03-10T09:00:00'}, {p:'p02',c:'c02',fecha:'2022-03-12T10:30:00'},
  {p:'p03',c:'c03',fecha:'2022-03-14T08:15:00'}, {p:'p04',c:'c04',fecha:'2023-06-01T14:00:00'},
  {p:'p05',c:'c05',fecha:'2023-06-02T15:00:00'}, {p:'p06',c:'c06',fecha:'2023-06-03T16:00:00'},
  {p:'p08',c:'c07',fecha:'2020-02-15T09:00:00'}, {p:'p08',c:'c08',fecha:'2021-05-10T11:00:00'},
  {p:'p07',c:'c09',fecha:'2021-07-20T10:00:00'}, {p:'p12',c:'c10',fecha:'2023-04-05T11:00:00'},
  {p:'p09',c:'c11',fecha:'2024-10-01T22:12:00'}, {p:'p10',c:'c12',fecha:'2024-10-01T22:20:00'},
  {p:'p11',c:'c13',fecha:'2024-10-01T22:33:00'}, {p:'p13',c:'c14',fecha:'2019-01-01T08:00:00'},
  {p:'p14',c:'c15',fecha:'2018-06-15T09:00:00'}, {p:'p15',c:'c16',fecha:'2021-01-10T09:00:00'},
  {p:'p16',c:'c17',fecha:'2021-03-15T10:00:00'}, {p:'p17',c:'c18',fecha:'2020-07-20T11:00:00'},
  {p:'p18',c:'c19',fecha:'2022-05-10T09:30:00'}, {p:'p19',c:'c20',fecha:'2023-08-01T16:00:00'},
  {p:'p20',c:'c21',fecha:'2021-11-20T12:00:00'},
  // Segundas y terceras cuentas (exceso = indicador mula)
  {p:'p01',c:'c22',fecha:'2022-05-20T10:00:00'}, {p:'p02',c:'c23',fecha:'2023-01-15T11:00:00'},
  {p:'p03',c:'c24',fecha:'2022-08-01T09:00:00'}, {p:'p08',c:'c25',fecha:'2022-11-10T10:00:00'},
  {p:'p12',c:'c26',fecha:'2023-05-01T11:00:00'}, {p:'p13',c:'c27',fecha:'2019-06-01T08:00:00'},
  {p:'p15',c:'c28',fecha:'2021-06-01T09:00:00'}, {p:'p15',c:'c29',fecha:'2022-03-15T12:00:00'},
  {p:'p17',c:'c30',fecha:'2021-02-10T10:00:00'}
] AS r
MATCH (p:Persona {id:r.p}), (c:Cuenta {id:r.c})
CREATE (p)-[:POSEE_CUENTA {fechaAsignacion: localdatetime(r.fecha), activa: true}]->(c);


// ─────────────────────────────────────────────────────────────────────────────
// 9. RELACIÓN Persona → Dispositivo (USA_DISPOSITIVO)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // ② Dispositivo compartido — d01 con p04+p05+p06
  {p:'p04',d:'d01',ultimo:'2024-11-01T07:55:00',freq:87},
  {p:'p05',d:'d01',ultimo:'2024-11-01T07:57:00',freq:62},
  {p:'p06',d:'d01',ultimo:'2024-11-01T07:58:00',freq:45},
  // ④ Identidad sintética — d02 con p09+p10+p11
  {p:'p09',d:'d02',ultimo:'2024-10-02T09:55:00',freq:8},
  {p:'p10',d:'d02',ultimo:'2024-10-02T09:57:00',freq:7},
  {p:'p11',d:'d02',ultimo:'2024-10-02T09:58:00',freq:6},
  // ① Circular — d03 solo Carlos, d04 Ana+Roberto
  {p:'p01',d:'d03',ultimo:'2024-09-15T11:18:00',freq:145},
  {p:'p02',d:'d04',ultimo:'2024-10-02T08:58:00',freq:110},
  {p:'p03',d:'d04',ultimo:'2024-10-02T08:55:00',freq:98},
  // ③⑤ Smurfing/Mula — d05 Juan+Pedro
  {p:'p08',d:'d05',ultimo:'2024-11-05T15:58:00',freq:210},
  {p:'p12',d:'d05',ultimo:'2024-10-30T08:58:00',freq:85},
  // Hub, PEP, Sancionado
  {p:'p15',d:'d06',ultimo:'2024-11-10T07:55:00',freq:890},
  {p:'p13',d:'d07',ultimo:'2024-09-01T09:58:00',freq:1240},
  {p:'p14',d:'d08',ultimo:'2024-07-20T14:58:00',freq:55},
  // Normales
  {p:'p16',d:'d09',ultimo:'2024-10-01T08:58:00',freq:320},
  {p:'p17',d:'d09',ultimo:'2024-09-15T09:58:00',freq:200},
  {p:'p18',d:'d10',ultimo:'2024-10-05T13:55:00',freq:180},
  {p:'p19',d:'d10',ultimo:'2024-11-01T07:58:00',freq:95},
  {p:'p20',d:'d10',ultimo:'2024-09-20T10:58:00',freq:150}
] AS r
MATCH (p:Persona {id:r.p}), (d:Dispositivo {id:r.d})
CREATE (p)-[:USA_DISPOSITIVO {ultimoUso: localdatetime(r.ultimo), frecuenciaUso: r.freq}]->(d);


// ─────────────────────────────────────────────────────────────────────────────
// 10. RELACIÓN Cuenta → Dispositivo (USADA_EN)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // Cuentas del account takeover — todas con d01
  {c:'c04',d:'d01',primera:'2023-06-01T14:10:00',ultima:'2024-11-01T07:55:00',ops:87},
  {c:'c05',d:'d01',primera:'2023-06-02T15:05:00',ultima:'2024-11-01T07:57:00',ops:62},
  {c:'c06',d:'d01',primera:'2023-06-03T16:05:00',ultima:'2024-11-01T07:58:00',ops:45},
  // Cuentas sintéticas — todas con d02
  {c:'c11',d:'d02',primera:'2024-10-01T22:12:00',ultima:'2024-10-02T09:55:00',ops:8},
  {c:'c12',d:'d02',primera:'2024-10-01T22:20:00',ultima:'2024-10-02T09:57:00',ops:7},
  {c:'c13',d:'d02',primera:'2024-10-01T22:33:00',ultima:'2024-10-02T09:58:00',ops:6},
  // Circular con d03, d04
  {c:'c01',d:'d03',primera:'2022-03-10T09:05:00',ultima:'2024-09-15T11:18:00',ops:145},
  {c:'c22',d:'d03',primera:'2022-05-20T10:05:00',ultima:'2024-09-15T11:18:00',ops:55},
  {c:'c02',d:'d04',primera:'2022-03-12T10:35:00',ultima:'2024-10-02T08:58:00',ops:110},
  {c:'c03',d:'d04',primera:'2022-03-14T08:20:00',ultima:'2024-10-02T08:55:00',ops:98},
  {c:'c23',d:'d04',primera:'2023-01-15T11:05:00',ultima:'2024-10-02T08:50:00',ops:30},
  {c:'c24',d:'d04',primera:'2022-08-01T09:05:00',ultima:'2024-10-02T08:45:00',ops:45},
  // Juan+Pedro con d05
  {c:'c07',d:'d05',primera:'2020-02-15T09:05:00',ultima:'2024-11-05T15:58:00',ops:210},
  {c:'c08',d:'d05',primera:'2021-05-10T11:05:00',ultima:'2024-11-05T15:55:00',ops:95},
  {c:'c25',d:'d05',primera:'2022-11-10T10:05:00',ultima:'2024-11-05T15:50:00',ops:80},
  {c:'c10',d:'d05',primera:'2023-04-05T11:05:00',ultima:'2024-10-30T08:58:00',ops:85},
  {c:'c26',d:'d05',primera:'2023-05-01T11:05:00',ultima:'2024-10-30T08:55:00',ops:40},
  // Hub
  {c:'c16',d:'d06',primera:'2021-01-10T09:05:00',ultima:'2024-11-10T07:55:00',ops:890},
  {c:'c28',d:'d06',primera:'2021-06-01T09:05:00',ultima:'2024-11-10T07:53:00',ops:450},
  {c:'c29',d:'d06',primera:'2022-03-15T12:05:00',ultima:'2024-11-10T07:50:00',ops:200},
  // PEP
  {c:'c14',d:'d07',primera:'2019-01-01T08:05:00',ultima:'2024-09-01T09:58:00',ops:1240},
  {c:'c27',d:'d07',primera:'2019-06-01T08:05:00',ultima:'2024-09-01T09:55:00',ops:500},
  // Sancionado
  {c:'c15',d:'d08',primera:'2018-06-15T09:05:00',ultima:'2024-07-20T14:58:00',ops:55},
  // Normales
  {c:'c17',d:'d09',primera:'2021-03-15T10:05:00',ultima:'2024-10-01T08:58:00',ops:320},
  {c:'c18',d:'d09',primera:'2020-07-20T11:05:00',ultima:'2024-09-15T09:58:00',ops:200},
  {c:'c30',d:'d09',primera:'2021-02-10T10:05:00',ultima:'2024-09-15T09:55:00',ops:150},
  {c:'c19',d:'d10',primera:'2022-05-10T09:35:00',ultima:'2024-10-05T13:55:00',ops:180},
  {c:'c20',d:'d10',primera:'2023-08-01T16:05:00',ultima:'2024-11-01T07:58:00',ops:95},
  {c:'c21',d:'d10',primera:'2021-11-20T12:05:00',ultima:'2024-09-20T10:58:00',ops:150},
  {c:'c09',d:'d09',primera:'2021-07-20T10:05:00',ultima:'2024-08-10T11:58:00',ops:120}
] AS r
MATCH (c:Cuenta {id:r.c}), (d:Dispositivo {id:r.d})
CREATE (c)-[:USADA_EN {
  fechaPrimeraOperacion: localdatetime(r.primera),
  fechaUltimaOperacion:  localdatetime(r.ultima),
  cantidadOperaciones:   r.ops
}]->(d);


// ─────────────────────────────────────────────────────────────────────────────
// 11. RELACIÓN Cuenta → Cuenta (TRANSFIERE_A)
//     Arista directa para algoritmos de camino más corto y detección circular
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // ① CICLO CIRCULAR c01→c02→c03→c01
  {o:'c01',d:'c02',tx:'t001',monto:'850000.00',canal:'APP_MOVIL',   fecha:'2024-09-10T10:15:00',estado:'COMPLETADA'},
  {o:'c02',d:'c03',tx:'t002',monto:'820000.00',canal:'HOME_BANKING',fecha:'2024-09-10T11:45:00',estado:'COMPLETADA'},
  {o:'c03',d:'c01',tx:'t003',monto:'790000.00',canal:'HOME_BANKING',fecha:'2024-09-10T14:20:00',estado:'COMPLETADA'},
  {o:'c22',d:'c02',tx:'t004',monto:'430000.00',canal:'APP_MOVIL',   fecha:'2024-09-15T09:30:00',estado:'COMPLETADA'},
  {o:'c03',d:'c16',tx:'t008',monto:'200000.00',canal:'HOME_BANKING',fecha:'2024-10-06T09:00:00',estado:'COMPLETADA'},
  // ③ SMURFING hacia mula c10
  {o:'c04',d:'c10',tx:'t009',monto:'9500.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:10:00',estado:'COMPLETADA'},
  {o:'c05',d:'c10',tx:'t010',monto:'9800.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:14:00',estado:'COMPLETADA'},
  {o:'c06',d:'c10',tx:'t011',monto:'9200.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:18:00',estado:'COMPLETADA'},
  {o:'c07',d:'c10',tx:'t012',monto:'9700.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:22:00',estado:'COMPLETADA'},
  {o:'c08',d:'c10',tx:'t013',monto:'9300.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:26:00',estado:'COMPLETADA'},
  {o:'c09',d:'c10',tx:'t014',monto:'9600.00',  canal:'APP_MOVIL',   fecha:'2024-11-01T08:30:00',estado:'COMPLETADA'},
  // ⑤ MULA agrega y reenvía
  {o:'c10',d:'c16',tx:'t021',monto:'85000.00', canal:'HOME_BANKING',fecha:'2024-11-01T11:00:00',estado:'COMPLETADA'},
  {o:'c10',d:'c26',tx:'t023',monto:'48000.00', canal:'HOME_BANKING',fecha:'2024-11-01T12:00:00',estado:'COMPLETADA'},
  {o:'c26',d:'c16',tx:'t024',monto:'45000.00', canal:'HOME_BANKING',fecha:'2024-11-01T13:00:00',estado:'COMPLETADA'},
  {o:'c16',d:'c28',tx:'t025',monto:'180000.00',canal:'HOME_BANKING',fecha:'2024-11-01T14:00:00',estado:'COMPLETADA'},
  // ④ CICLO SINTETICO c11→c12→c13→c11
  {o:'c11',d:'c12',tx:'t026',monto:'5000.00',  canal:'APP_MOVIL',   fecha:'2024-10-02T10:05:00',estado:'COMPLETADA'},
  {o:'c12',d:'c13',tx:'t027',monto:'4500.00',  canal:'APP_MOVIL',   fecha:'2024-10-02T10:08:00',estado:'COMPLETADA'},
  {o:'c13',d:'c11',tx:'t028',monto:'4200.00',  canal:'APP_MOVIL',   fecha:'2024-10-02T10:12:00',estado:'COMPLETADA'},
  {o:'c11',d:'c10',tx:'t029',monto:'12000.00', canal:'APP_MOVIL',   fecha:'2024-10-02T10:20:00',estado:'COMPLETADA'},
  {o:'c12',d:'c10',tx:'t030',monto:'8000.00',  canal:'APP_MOVIL',   fecha:'2024-10-02T10:25:00',estado:'COMPLETADA'},
  {o:'c13',d:'c10',tx:'t031',monto:'9500.00',  canal:'APP_MOVIL',   fecha:'2024-10-02T10:30:00',estado:'COMPLETADA'},
  // ⑥ PEP
  {o:'c27',d:'c14',tx:'t034',monto:'800000.00',canal:'HOME_BANKING',fecha:'2024-09-01T10:10:00',estado:'COMPLETADA'},
  {o:'c16',d:'c27',tx:'t035',monto:'500000.00',canal:'HOME_BANKING',fecha:'2024-10-01T15:00:00',estado:'COMPLETADA'},
  {o:'c29',d:'c27',tx:'t036',monto:'300000.00',canal:'APP_MOVIL',   fecha:'2024-10-15T11:00:00',estado:'COMPLETADA'},
  {o:'c28',d:'c14',tx:'t037',monto:'250000.00',canal:'HOME_BANKING',fecha:'2024-10-20T10:00:00',estado:'COMPLETADA'},
  // ⑦ SANCIONADO
  {o:'c15',d:'c17',tx:'t038',monto:'250000.00',canal:'HOME_BANKING',fecha:'2024-07-15T09:00:00',estado:'BLOQUEADA'},
  {o:'c18',d:'c15',tx:'t039',monto:'150000.00',canal:'HOME_BANKING',fecha:'2024-07-20T14:00:00',estado:'RECHAZADA'},
  {o:'c15',d:'c16',tx:'t040',monto:'350000.00',canal:'HOME_BANKING',fecha:'2024-07-25T10:00:00',estado:'RECHAZADA'},
  // NORMALES
  {o:'c17',d:'c18',tx:'t041',monto:'25000.00', canal:'APP_MOVIL',   fecha:'2024-10-01T08:00:00',estado:'COMPLETADA'},
  {o:'c18',d:'c19',tx:'t042',monto:'15000.00', canal:'APP_MOVIL',   fecha:'2024-10-02T09:00:00',estado:'COMPLETADA'},
  {o:'c19',d:'c20',tx:'t043',monto:'35000.00', canal:'HOME_BANKING',fecha:'2024-10-05T10:00:00',estado:'COMPLETADA'},
  {o:'c20',d:'c21',tx:'t044',monto:'22000.00', canal:'APP_MOVIL',   fecha:'2024-10-06T11:00:00',estado:'COMPLETADA'},
  {o:'c21',d:'c17',tx:'t045',monto:'18000.00', canal:'APP_MOVIL',   fecha:'2024-10-07T14:00:00',estado:'COMPLETADA'},
  {o:'c25',d:'c17',tx:'t046',monto:'45000.00', canal:'HOME_BANKING',fecha:'2024-10-10T09:00:00',estado:'COMPLETADA'},
  {o:'c17',d:'c19',tx:'t047',monto:'30000.00', canal:'APP_MOVIL',   fecha:'2024-10-12T10:00:00',estado:'COMPLETADA'},
  {o:'c18',d:'c20',tx:'t048',monto:'28000.00', canal:'APP_MOVIL',   fecha:'2024-10-15T08:00:00',estado:'COMPLETADA'},
  {o:'c21',d:'c18',tx:'t049',monto:'12000.00', canal:'APP_MOVIL',   fecha:'2024-10-18T15:00:00',estado:'COMPLETADA'},
  {o:'c30',d:'c21',tx:'t050',monto:'55000.00', canal:'HOME_BANKING',fecha:'2024-10-20T11:00:00',estado:'COMPLETADA'}
] AS r
MATCH (co:Cuenta {id:r.o}), (cd:Cuenta {id:r.d})
CREATE (co)-[:TRANSFIERE_A {
  transaccionId: r.tx,
  monto:         r.monto,
  moneda:        'ARS',
  canal:         r.canal,
  fecha:         localdatetime(r.fecha),
  estado:        r.estado
}]->(cd);


// ─────────────────────────────────────────────────────────────────────────────
// 12. RELACIONES Transaccion → Cuenta (ORIGINADA_EN y DIRIGIDA_A)
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  {tx:'t001',origen:'c01',destino:'c02'}, {tx:'t002',origen:'c02',destino:'c03'},
  {tx:'t003',origen:'c03',destino:'c01'}, {tx:'t004',origen:'c22',destino:'c02'},
  {tx:'t005',origen:'c01',destino:'c02'}, {tx:'t006',origen:'c02',destino:'c03'},
  {tx:'t007',origen:'c03',destino:'c01'}, {tx:'t008',origen:'c03',destino:'c16'},
  {tx:'t009',origen:'c04',destino:'c10'}, {tx:'t010',origen:'c05',destino:'c10'},
  {tx:'t011',origen:'c06',destino:'c10'}, {tx:'t012',origen:'c07',destino:'c10'},
  {tx:'t013',origen:'c08',destino:'c10'}, {tx:'t014',origen:'c09',destino:'c10'},
  {tx:'t015',origen:'c04',destino:'c10'}, {tx:'t016',origen:'c05',destino:'c10'},
  {tx:'t017',origen:'c07',destino:'c10'}, {tx:'t018',origen:'c08',destino:'c10'},
  {tx:'t019',origen:'c06',destino:'c10'}, {tx:'t020',origen:'c09',destino:'c10'},
  {tx:'t021',origen:'c10',destino:'c16'}, {tx:'t022',origen:'c10',destino:'c16'},
  {tx:'t023',origen:'c10',destino:'c26'}, {tx:'t024',origen:'c26',destino:'c16'},
  {tx:'t025',origen:'c16',destino:'c28'}, {tx:'t026',origen:'c11',destino:'c12'},
  {tx:'t027',origen:'c12',destino:'c13'}, {tx:'t028',origen:'c13',destino:'c11'},
  {tx:'t029',origen:'c11',destino:'c10'}, {tx:'t030',origen:'c12',destino:'c10'},
  {tx:'t031',origen:'c13',destino:'c10'}, {tx:'t032',origen:'c11',destino:'c12'},
  {tx:'t033',origen:'c12',destino:'c13'}, {tx:'t034',origen:'c27',destino:'c14'},
  {tx:'t035',origen:'c16',destino:'c27'}, {tx:'t036',origen:'c29',destino:'c27'},
  {tx:'t037',origen:'c28',destino:'c14'}, {tx:'t038',origen:'c15',destino:'c17'},
  {tx:'t039',origen:'c18',destino:'c15'}, {tx:'t040',origen:'c15',destino:'c16'},
  {tx:'t041',origen:'c17',destino:'c18'}, {tx:'t042',origen:'c18',destino:'c19'},
  {tx:'t043',origen:'c19',destino:'c20'}, {tx:'t044',origen:'c20',destino:'c21'},
  {tx:'t045',origen:'c21',destino:'c17'}, {tx:'t046',origen:'c25',destino:'c17'},
  {tx:'t047',origen:'c17',destino:'c19'}, {tx:'t048',origen:'c18',destino:'c20'},
  {tx:'t049',origen:'c21',destino:'c18'}, {tx:'t050',origen:'c30',destino:'c21'}
] AS r
MATCH (t:Transaccion {id:r.tx}), (co:Cuenta {id:r.origen}), (cd:Cuenta {id:r.destino})
CREATE (t)-[:ORIGINADA_EN]->(co)
CREATE (t)-[:DIRIGIDA_A]->(cd);


// ─────────────────────────────────────────────────────────────────────────────
// 13. RELACIÓN Transaccion → Transaccion (RELACIONADA_CON)
//     Red de fraude: vincula transacciones sospechosas relacionadas
// ─────────────────────────────────────────────────────────────────────────────
UNWIND [
  // ① Anillo de lavado circular — ciclo 1
  {t1:'t001',t2:'t002',tipo:'ANILLO_FRAUDE',    score:0.98, desc:'Leg 1→2 del ciclo circular c01-c02-c03',           fecha:'2024-09-10T15:00:00'},
  {t1:'t002',t2:'t003',tipo:'ANILLO_FRAUDE',    score:0.98, desc:'Leg 2→3 cierre del ciclo circular A-B-C-A',         fecha:'2024-09-10T15:00:05'},
  {t1:'t001',t2:'t005',tipo:'PATRON_TEMPORAL',  score:0.85, desc:'Mismo origen destino — segunda vuelta del ciclo',   fecha:'2024-10-05T16:00:00'},
  {t1:'t005',t2:'t006',tipo:'ANILLO_FRAUDE',    score:0.95, desc:'Leg 1→2 segundo ciclo',                             fecha:'2024-10-05T16:00:05'},
  {t1:'t006',t2:'t007',tipo:'ANILLO_FRAUDE',    score:0.95, desc:'Leg 2→3 cierre segundo ciclo',                      fecha:'2024-10-05T16:00:10'},
  // ③ Anillo smurfing — mismos montos, mismo destino, misma ventana
  {t1:'t009',t2:'t010',tipo:'MONTO_SIMILAR',    score:0.94, desc:'Mismo destino c10 — montos sub-umbral consecutivos',fecha:'2024-11-01T10:00:00'},
  {t1:'t009',t2:'t012',tipo:'MONTO_SIMILAR',    score:0.92, desc:'Montos similares <10K hacia mula',                  fecha:'2024-11-01T10:00:05'},
  {t1:'t009',t2:'t015',tipo:'PATRON_TEMPORAL',  score:0.90, desc:'Misma cuenta origen repite patron hora despues',    fecha:'2024-11-01T10:00:10'},
  {t1:'t012',t2:'t017',tipo:'PATRON_TEMPORAL',  score:0.93, desc:'Orquestador repite patron — tercera transferencia', fecha:'2024-11-01T10:00:15'},
  {t1:'t010',t2:'t016',tipo:'PATRON_TEMPORAL',  score:0.91, desc:'Segunda cuenta device sharing repite patron',       fecha:'2024-11-01T10:00:20'},
  // ④ Anillo identidad sintética
  {t1:'t026',t2:'t027',tipo:'ANILLO_FRAUDE',    score:0.97, desc:'Ciclo entre cuentas creadas el mismo dia',          fecha:'2024-10-02T11:00:00'},
  {t1:'t027',t2:'t028',tipo:'ANILLO_FRAUDE',    score:0.97, desc:'Cierre ciclo sintetico A11-A12-A13-A11',            fecha:'2024-10-02T11:00:05'},
  {t1:'t026',t2:'t032',tipo:'PATRON_TEMPORAL',  score:0.88, desc:'Mismo ciclo repetido dia siguiente',                fecha:'2024-10-03T10:00:00'},
  // ⑤ Cuenta puente — flujo de fondos hacia hub
  {t1:'t029',t2:'t021',tipo:'CUENTA_PUENTE',    score:0.88, desc:'Dinero sintetico llega a mula y sale al hub',       fecha:'2024-11-01T15:00:00'},
  {t1:'t003',t2:'t008',tipo:'CUENTA_PUENTE',    score:0.75, desc:'Dinero circular extrae porcentaje al hub',          fecha:'2024-10-06T10:00:00'},
  {t1:'t021',t2:'t025',tipo:'CUENTA_PUENTE',    score:0.72, desc:'Hub re-distribuye fondos de mula',                  fecha:'2024-11-01T15:00:05'},
  // ⑥ PEP conectado al hub
  {t1:'t035',t2:'t025',tipo:'CUENTA_PUENTE',    score:0.65, desc:'Hub envia a PEP y a hub-secundario mismo periodo',  fecha:'2024-10-20T12:00:00'},
  // MISMO_DISPOSITIVO — transacciones desde d01 vinculadas
  {t1:'t009',t2:'t011',tipo:'MISMO_DISPOSITIVO',score:0.96, desc:'Tres transferencias desde mismo dispositivo Tor',   fecha:'2024-11-01T10:00:25'},
  {t1:'t010',t2:'t014',tipo:'MISMO_DISPOSITIVO',score:0.95, desc:'Dispositivo compartido vincula smurfs',             fecha:'2024-11-01T10:00:30'}
] AS r
MATCH (t1:Transaccion {id:r.t1}), (t2:Transaccion {id:r.t2})
CREATE (t1)-[:RELACIONADA_CON {
  tipoRelacion:     r.tipo,
  puntajeSimilitud: r.score,
  descripcion:      r.desc,
  fechaDeteccion:   localdatetime(r.fecha)
}]->(t2);


// ═══════════════════════════════════════════════════════════════════════════════
// 14. VERIFICACIÓN — consultas de validación post-carga
// ═══════════════════════════════════════════════════════════════════════════════

// Resumen de nodos y relaciones
MATCH (n) RETURN labels(n)[0] AS tipo, COUNT(n) AS cantidad ORDER BY cantidad DESC;

// Ciclo circular detectado
MATCH ciclo = (c:Cuenta {id:'c01'})-[:TRANSFIERE_A*3]->(c)
RETURN [n IN nodes(ciclo) | n.numeroCuenta] AS cuentasEnCiclo, length(ciclo) AS saltos;

// Dispositivo compartido por múltiples personas
MATCH (d:Dispositivo)<-[:USA_DISPOSITIVO]-(p:Persona)
WITH d, COLLECT(p.nombre + ' ' + p.apellido) AS personas, COUNT(p) AS total
WHERE total > 1
RETURN d.fingerprint, total, personas ORDER BY total DESC;

// Top smurfing — cuentas que más recibieron transferencias sub-umbral
MATCH (t:Transaccion)-[:DIRIGIDA_A]->(c:Cuenta)
WHERE toFloat(t.monto) < 10000 AND t.esAlertada = true
WITH c, COUNT(t) AS totalSmurf, SUM(toFloat(t.monto)) AS totalMonto
RETURN c.id, c.banco, totalSmurf, totalMonto ORDER BY totalSmurf DESC LIMIT 5;

// Red de fraude completa — anillo desde t001
MATCH (t:Transaccion {id:'t001'})-[:RELACIONADA_CON*1..4]-(tRel:Transaccion)
RETURN DISTINCT tRel.id, tRel.nivelRiesgo, tRel.motivoAlerta ORDER BY tRel.nivelRiesgo DESC;
