# GuardPay — Validación Costa Rica y Chile

Oct 4, 2026 · @Justin

## 1. Executive Summary

**El problema es real y compartido en Costa Rica y Chile; la solución, tal como está planteada, no es diferenciadora en Chile.** Los bancos chilenos limitan desde hace años el monto de la primera transferencia a un destinatario nuevo durante 12–24 horas: el “carril ámbar” de GuardPay ya existe allí como práctica bancaria.

Lo que no encontramos en ninguno de los dos países es que el usuario pueda **nombrar a una segunda persona que vea y cancele un pago riesgoso antes de que sea irreversible**. Esa es la brecha común más defendible, y debe pasar a ser el centro del producto y del pitch.

**Decisión de producto: C — pivot parcial.** Del “cooling-off” al **“guardián que puede frenar”**. Se conservan smart wallet, Stellar, Soroban, IA sin autoridad, contactos de confianza, espera y control on-chain.

**Decisión de mercado: D.** Costa Rica como origen del problema; Chile como mercado de presentación, con datos chilenos primero.

## 2. Veredicto brutal

- 🟢 **Si el pitch dice “pagos a destinatarios nuevos esperan”, un juez chileno puede responder “mi banco ya lo hace”.** Santander: primera transferencia a un destinatario nuevo máx. $250.000 CLP; más después de 24 h (cuenta oficial, 2021: [Santander Chile en X](https://x.com/santanderchile/status/1460986322248781830?lang=en)). En 2019 ya lo hacían Banco de Chile (12 h), BICE (12 h), Scotiabank (24 h) y Bci (24 h) ([Chócale, ene 2019](https://chocale.cl/2019/01/los-bancos-han-ido-limitando-la-transferencias-electronicas-a-nuevos-destinatarios/), 🟡).
- 🟡 **“La IA te ayuda a pagar” es la parte más débil del mensaje.** Pagar ya es fácil en ambos países; la IA solo aporta al analizar el mensaje del estafador.
- 🔴 **GuardPay no protege la cuenta bancaria del usuario.** Protege fondos en USDC dentro de su propia wallet en Stellar. Ningún juez debe salir creyendo lo contrario.
- 🟢 **Lo que sí sobrevive:** la segunda persona con poder de cancelar. Monzo (Reino Unido) tiene un “contacto de confianza” que revisa transferencias grandes ([Monzo](https://monzo.com/help/Account%20Security/addedsecurityfaqs)); no lo encontramos en bancos de Costa Rica ni de Chile (🔵, ausencia por búsqueda, no prueba).
- 🟣 **¿Abandonar? No.** Pero sí cambiar el centro: de “espera” a “guardián”.

## 3. Qué estaba correcto en la investigación anterior

| Afirmación | Estado |
| --- | --- |
| La espera antiestafa existe en banca (Singapur) y en Web3 (Argent) | 🟢 Confirmada, y ahora también en Chile |
| OpenZeppelin ya provee smart accounts, context rules, Spending Limit, passkeys | 🟢 Confirmada |
| La IA debe ser copiloto sin autoridad | 🟢 Se mantiene |
| El escrow agrega riesgo regulatorio; mejor una política que retenga la autorización | 🟢 Se mantiene; en Chile pesa aún más (custodia regulada por la Ley Fintech) |
| Costa Rica: 25.100 estafas informáticas 2018–ago 2025 (UNA con datos del OIJ); Findex 71% | 🟡/🟢 Se mantienen |

## 4. Qué estaba equivocado o no verificado

| Afirmación | Corrección |
| --- | --- |
| “La espera para destinatarios nuevos” como diferencial central | 🔴 No diferencia en Chile: es práctica bancaria desde al menos 2019 |
| No sabíamos dónde era el hackathon | Es en **Chile**. Candidato fuerte: la Ideatón de SDF + Tellus Cooperative (Parte 21). 🔴 Sin confirmar |
| “General Track” | Ese nombre aparece en HackMeridian 2025 (Río). La Ideatón Chile 2025 tenía un único track abierto. 🔴 Sin confirmar para 2026 |
| El copiloto IA como parte central del one-liner | Se baja de prioridad |
| Costa Rica no tenía medidas regulatorias específicas en SINPE Móvil | 🟡 Sí hay: tope diario de ₡100.000 para SINPE Móvil por SMS (BCCR, ago 2025) y obligación de reportar cambio de número (BCCR, abr 2025) |

## 5. Costa Rica — evidencia

**El fraude crece y el canal de pago es instantáneo, pero las cifras públicas son pocas y no separan el fraude autorizado por la víctima.**

| Dato | Año | Fuente | Nivel | Limitación |
| --- | --- | --- | --- | --- |
| 25.100 estafas informáticas (62% de 40.457 ciberdelitos); 8.757 suplantaciones | 2018–ago 2025 | Labcibe-UNA con datos del OIJ ([Semanario Universidad](https://semanariouniversidad.com/pais/estudio-una-25-mil-estafas-informaticas-y-8-mil-suplantaciones-de-identidad-los-delitos-que-acechan-a-los-costarricenses/)) | 🟡 | Sin montos ni canal de pago |
| Víctimas: 30–39 años = 25,2%; 65+ = 8,7% | 2018–ago 2025 | Ídem | 🟡 | Sin tasa por población |
| Estafas informáticas y fraude con tarjetas +9% | 2025 vs 2024 | OIJ vía [Teletica](https://www.teletica.com/sucesos/estafas-informaticas-y-fusiles-de-guerra-dispararon-alertas-del-oij-en-2025_399926) | 🟡 | Sin cifra absoluta |
| Modalidades: comprobante falso, falso banco, ventas en redes, WhatsApp secuestrado | 2026 | [crhoy](https://crhoy.com/nacionales/no-caiga-en-la-trampa-asi-funcionan-las-estafas-por-sinpe-movil/) | 🟡 | Sin frecuencias |
| SINPE Móvil: 745,5 M transacciones en 2025 | 2025 | [La Nación](https://www.nacion.com/economia/sinpe-movil-cambiara-bccr-prepara-una-nueva/UN4SPSK2FVCQDFSRTD3ZVMP2YA/story/) | 🟡 | Falta boletín BCCR |
| Tope diario de ₡100.000 para SINPE Móvil por SMS; subir requiere canal autenticado | Aprobado ago 2025 | [El Financiero](https://www.elfinancierocr.com/finanzas/banco-central-acordo-cambio-en-sinpe-movil-tendra/TMRL4VDZZVBUPNRJGNWNYLEPYM/story/) | 🟡 | Solo SMS, no app |
| Obligación de reportar cambio de número por riesgo de reasignación | abr 2025 | [BCCR, comunicado](https://www.bccr.fi.cr/content/dam/bccr/noticias/2025/2025-04-30-cp-bccr-017-2025-usuarios-sinpe-movil-deben-reportar-cambio-telefono-movil-asu-entidad-financiera-1.pdf) | 🟢 | — |
| BCCR prepara programa nacional antifraude y rediseño de SINPE Móvil (2028) | 2026 | [La Nación](https://www.nacion.com/economia/sinpe-movil-cambiara-bccr-prepara-una-nueva/UN4SPSK2FVCQDFSRTD3ZVMP2YA/story/) | 🟡 | Plan, no vigente |
| Cuenta financiera: 71,35% de adultos | 2024 | [Banco Mundial](https://api.worldbank.org/v2/country/CRI/indicator/FX.OWN.TOTL.ZS?format=json&date=2011:2025) | 🟢 | — |

🔴 **No encontrado en Costa Rica:** una ley de restitución de fraude equivalente a la chilena; límites o esperas para destinatarios nuevos en apps bancarias; contactos de confianza. Que no los encontráramos no prueba que no existan.

## 6. Chile — evidencia

**Chile tiene más datos, más regulación y más protección bancaria que Costa Rica, y aun así el fraude por transferencias es grande y caro.**

| Dato | Año | Fuente | Nivel | Limitación |
| --- | --- | --- | --- | --- |
| Pérdidas por fraude externo se duplicaron en 2023; \~USD 276 M; 11,8 centavos por cada USD 100 vs 6,8 global | 2023 | [Banco Central de Chile, ISIP 2024](https://coleccion.bcentral.cl/documents/33528/6165218/Recuadro_I1_isip_2024.pdf/350e7804-17df-0f32-d9f8-e098691f4233) | 🟢 | Incluye tarjetas y cajeros |
| 1,3 M reclamos por operaciones no reconocidas; 83% menos tras la Ley 21.673 (mar→dic 2024) | 2023–2024 | [CMF, nota técnica](https://www.cmfchile.cl/portal/estadisticas/617/articles-100079_doc_pdf.pdf) | 🟢 | No separa ingeniería social |
| Transferencias no reconocidas: monto promedio sube de $143.277 a $450.034 tras la reforma | 2024 | Ídem | 🟢 | — |
| 40.401 reclamos por transferencias electrónicas por \~$51.557 millones de pesos (el monto más alto pese a menos casos) | 2025 | [Chócale con ExpertsFraud](https://chocale.cl/2026/03/ley-de-fraudes-cifras-2025-reclamos-de-usuarios/) | 🟡 | Análisis de un medio sobre datos públicos |
| SERNAC: 19.834 reclamos por fraude en 2024 (+109%); 20.880 en 2025 | 2024–2025 | [BioBio](https://www.biobiochile.cl/noticias/economia/tu-bolsillo/2025/07/14/reclamos-al-alza-sernac-lanza-agenda-antifraude-y-oficiara-a-todas-las-instituciones-financieras.shtml), [El Dínamo](https://www.eldinamo.cl/economia/dinero/2026/02/26/los-bancos-y-tiendas-que-lideran-con-mas-reclamos-por-fraudes-en-el-sernac/) | 🟡 | Solo quienes reclaman |
| Modalidades SERNAC: suplantación, phishing, SIM swap, llamadas falsas | 2024 | Ídem | 🟡 | — |
| TEF inmediatas e interoperables; CCA procesó en promedio $1,45 billones diarios en TEF en 2025 | 2025 | [Banco Central de Chile, recuadro II.1](https://www.bcentral.cl/documents/33528/8546761/Recuadro%20II.1%20Infraestructuras%20de%20soporte%20para%20pagos%20inmediatos%20en%20Chile.pdf/5a4bb4a1-0854-e665-1876-954bed84da29) | 🟢 | Excluye “on us” |
| “Pix chileno” con alias y QR en desarrollo por la banca | 2026 | [Meganoticias](https://www.meganoticias.cl/nacional/528480-pix-chileno-banca-proyecto-pagos-instantaneos-chile-03-08-2026.html) | 🟡 | Aún no lanzado |
| Cuenta financiera: 85,07% de adultos | 2024 | [Banco Mundial](https://api.worldbank.org/v2/country/CHL/indicator/FX.OWN.TOTL.ZS?format=json&date=2011:2025) | 🟢 | — |

## 7. Comparación Costa Rica vs Chile

| Factor | Costa Rica | Chile | Evidencia | Implicación para GuardPay |
| --- | --- | --- | --- | --- |
| Bancarización | 71% | 85% | 🟢 Banco Mundial | El problema no es acceso en ninguno |
| Pagos inmediatos | SINPE Móvil, masivo | TEF 24/7 vía CCA; “Pix chileno” en camino | 🟡/🟢 | Ambos son rápidos e irreversibles en la práctica |
| Magnitud del fraude | 25.100 estafas informáticas en \~8 años | 1,3 M reclamos en 2 años; USD 276 M en 2023 | 🟡/🟢 | Chile tiene un problema mayor y mejor medido |
| Ingeniería social y suplantación | Documentada (falso banco, WhatsApp) | Documentada (suplantación, phishing, llamadas) | 🟡 | Patrón común |
| Marketplace / comprobantes falsos | Documentado | No verificado en esta búsqueda | 🟡/🔴 | Usar en CR, no afirmarlo en Chile |
| Fraude autorizado por la víctima, medido aparte | No | No (CMF no lo separa) | 🟢 CMF | Ninguno de los dos países mide APP por separado |
| Destinatarios nuevos | No encontrado | Primera transferencia limitada 12–24 h en grandes bancos | 🟡 | **El carril ámbar no diferencia en Chile** |
| Reversibilidad / restitución | No encontramos régimen específico | Ley 20.009 / 21.673: el banco restituye salvo dolo o culpa grave, que debe probar | 🟢/🟡 | En Chile la discusión es quién paga; en CR, si alguien paga |
| Autenticación | Biometría y tokens en apps; SMS con tope | Autenticación reforzada obligatoria para alto riesgo desde ago 2026 (NCG 538) | 🟡 | Chile endurece la autenticación; no resuelve la manipulación |
| Biometría / alertas / bloqueo preventivo | Apps bancarias (no detallado) | Apps bancarias; SERNAC pide más protocolos | 🟡 | Similar |
| Cooling-off | No encontrado | Equivalente parcial: tope de monto 12–24 h | 🟡 | — |
| Contacto de confianza / guardián | No encontrado | No encontrado | 🔵 | **Brecha común candidata** |
| Madurez fintech | Media; 20 fintech en pagos (BCCR) | Alta; Ley Fintech 21.521 | 🟡 | Chile: más competencia, más reglas claras |
| Regulación de activos virtuales | Ley 10961 (jun 2026), registro SUGEF | Ley Fintech + NCG 502; CMF rechazó 24 solicitudes en jun 2026 | 🟡 | Ambos exigen registro si hay custodia o intercambio |
| Stablecoins | Rutas P2P/exchanges; USDC-Stellar no confirmado | Buda.com lista USDC; red Stellar no confirmada | 🟡 | La rampa es un obstáculo en ambos |
| Percepción del riesgo | Alta en medios | Alta: agenda antifraude SERNAC, fallos de la Suprema | 🟡 | El tema es reconocible para jueces chilenos |

## 8. Fraude e ingeniería social

**En ambos países conviven dos fraudes distintos, y GuardPay solo ataca uno.**

| Tipo | Qué pasa | Ejemplo | ¿GuardPay ayuda? |
| --- | --- | --- | --- |
| No autorizado (robo de credenciales, SIM swap, cuenta tomada) | El atacante opera la cuenta | Phishing que roba clave + código; SIM swap | Parcial: la passkey ligada al dispositivo y la espera para destinatarios nuevos lo dificultan |
| **Autorizado por la víctima (APP)** | La víctima, engañada, hace el pago ella misma | “Soy del banco, mueva su plata a esta cuenta segura”; “soy tu hijo, cambié de número” | **Sí: es el caso para el que existe** |

🔵 Las autoridades de ambos países publican estadísticas de “estafas informáticas” (CR) u “operaciones no reconocidas” (Chile), pero ninguna separa el fraude autorizado. No podemos decir qué parte del total es APP.

**Patrón común del estafador (🔵, inferido de las modalidades documentadas en ambos países):** urgencia, identidad falsa (banco, familiar), cuenta destino nueva y aislamiento (“no cuelgue”, “no le cuente a nadie”). El límite chileno de 12–24 h ataca la cuenta nueva; nada ataca el **aislamiento**. Ahí entra el guardián.

## 9. Authorized Push Payment (APP)

**APP fraud** es el fraude en que la víctima, manipulada, autoriza ella misma un pago a la cuenta del estafador. Es difícil de detectar porque la autenticación es legítima: es la víctima quien firma.

| Medida internacional | País | Qué hace | Fuente | Nivel |
| --- | --- | --- | --- | --- |
| Reembolso obligatorio de APP | Reino Unido, desde 7 oct 2024 | El proveedor reembolsa hasta £85.000 por reclamo | [Regulation Tomorrow](https://www.regulationtomorrow.com/2024/10/psr-issues-policy-statement-confirming-maximum-level-of-reimbursement-for-faster-payments-app-scams/) | 🟡 |
| Retención de pagos sospechosos | Reino Unido, 2024 | El proveedor puede demorar hasta 4 días hábiles un pago si sospecha fraude | [Foot Anstey](https://www.footanstey.com/our-insights/articles-news/hm-treasury-publishes-final-draft-of-payment-delay-legislation/) | 🟡 |
| Espera para nuevos destinatarios | Singapur (DBS, mar 2026) | 12 h | [Fintech News SG](https://fintechnews.sg/126691/security/dbs-cooling-period/) | 🟡 |
| Contacto de confianza | Reino Unido (Monzo) | Un tercero revisa transferencias grandes | [Monzo](https://monzo.com/help/Account%20Security/addedsecurityfaqs) | 🟢 |
| Límites nocturnos y espera para subir límites | Brasil (Pix) | R$1.000 de noche; 24 h para aumentar; bloqueo preventivo hasta 72 h | [Suno](https://www.suno.com.br/noticias/?p=207404) | 🟡 |

**Relación con Chile (🟡 + 🔵):** Chile no tiene un régimen específico de APP. Las transferencias inducidas se discuten bajo la Ley 20.009 como operaciones “no reconocidas”, y el banco debe probar dolo o culpa grave del cliente. La Corte Suprema ha fallado a favor de clientes engañados cuando el banco no probó culpa grave ni cumplió deberes de monitoreo y límites ([Diario Constitucional, feb 2026](https://www.diarioconstitucional.cl/2026/02/12/corte-suprema-acoge-queja-y-fija-estandar-probatorio-en-fraudes-electronicos-bajo-la-ley-n-20-009/)). La CMF registró 83% menos reclamos tras la reforma de 2024 ([CMF](https://www.cmfchile.cl/portal/estadisticas/617/articles-100079_doc_pdf.pdf)). 🔵 Interpretación: la víctima de APP en Chile enfrenta un proceso más exigente; prevenir antes del pago vale más que reclamar después.

**Relación con Costa Rica (🔴):** no encontramos un régimen de restitución ni un concepto equivalente de APP en normativa. No forzamos la conexión: la historia en Costa Rica es de prevención, no de reembolso.

**Por qué las soluciones tradicionales fallan con APP (🔵):** la autenticación reforzada confirma que *es* la víctima, no que *quiere* pagarle al estafador. Las alertas llegan a la misma persona manipulada. El tope para destinatarios nuevos solo reduce el monto o retrasa; no mete a nadie más en la decisión.

## 10. Bancos y fintechs de Costa Rica

**Las protecciones que encontramos son de autenticación y de canal (SMS), no de destinatario ni de segunda persona.**

| Institución | Protección | Cómo funciona | ¿Resuelve APP? | Qué falta |
| --- | --- | --- | --- | --- |
| BCCR (regla para todo el sistema) | Tope de ₡100.000 diarios en SINPE Móvil por SMS | Subirlo exige canal autenticado | No: limita un canal débil | Nada para pagos desde la app |
| BCCR | Reportar cambio de número | Evita que un número reasignado opere SINPE Móvil | No: es contra toma de cuenta | — |
| Banco Nacional | Código de verificación en SINPE Móvil | Solo titular ([El Observador](https://observador.cr/clientes-del-banco-nacional-tendran-que-usar-codigo-de-verificacion-para-sinpe-movil/)); no pudimos abrir el artículo | No: la víctima manipulada también escribe el código | — |
| Bancos en general | Biometría, tokens, notificaciones | Práctica común (🔵, no verificado banco por banco) | No | Destinatario nuevo, segunda persona |

🔴 No verificamos las apps de BAC, BCR, Popular ni Davivienda. Antes del pitch, alguien del equipo debería revisar en su propia app si hay límites para destinatarios nuevos.

## 11. Bancos y fintechs de Chile

**Chile ya cubre parte de GuardPay. No lo ocultamos: el límite para destinatarios nuevos es práctica de los grandes bancos.**

| Institución | Protección | Cómo funciona | ¿Resuelve APP? | Qué falta |
| --- | --- | --- | --- | --- |
| Santander Chile | Tope a destinatario nuevo | Primera transferencia máx. $250.000; más después de 24 h ([X, 2021](https://x.com/santanderchile/status/1460986322248781830?lang=en)) | Parcial: reduce el monto del primer golpe | Nadie más puede intervenir; tras 24 h todo pasa |
| Banco de Chile | Tope a destinatario nuevo | $350.000 por 12 h (2019); página oficial “transferencias a nuevos destinatarios” no legible | Parcial | Ídem |
| Bci | Tope a destinatario nuevo | $250.000 por 24 h (2019) | Parcial | Ídem |
| Scotiabank, BICE | Tope a destinatario nuevo | $300.000 / 24 h; $500.000 / 12 h (2019) | Parcial | Ídem |
| BancoEstado | CuentaRUT: sin restricción en 2019 | ([Chócale 2019](https://chocale.cl/2019/01/los-bancos-han-ido-limitando-la-transferencias-electronicas-a-nuevos-destinatarios/)) | No | Es la institución con más reclamos ante SERNAC |
| Todo el sistema (CMF NCG 538) | Autenticación reforzada | Dos factores para transferencias y enrolamiento de dispositivos, desde ago 2026 ([IronVest](https://ironvest.com/blog/how-chilean-banks-can-meet-ley-21-673-and-cmf-ncg-538-without-adding/), 🟡) | No: la víctima de APP se autentica bien | Manipulación |
| Todo el sistema (Ley 20.009 / 21.673) | Restitución | El banco paga salvo dolo o culpa grave probada | Después del daño, y con litigio | Prevención |
| Fintechs (MACH, Tenpo, Mercado Pago) | 🔴 No verificado | — | — | Revisar antes del pitch |

**¿GuardPay es una copia de esto?** En la espera para destinatarios nuevos, **sí**: lo que hacen los bancos chilenos es una versión de nuestro carril ámbar. GuardPay es diferente en tres puntos concretos:

1. **Una segunda persona puede cancelar** durante la espera. Ningún banco revisado lo ofrece.
2. **Las reglas son del usuario** (él fija topes y esperas) y viven on-chain, no en la política de un banco.
3. **El intento queda visible**: el estafador no puede “esperar las 24 h” en silencio, porque el guardián ya recibió el aviso.

## 12. Brechas actuales

| Brecha | Costa Rica | Chile | ¿Común? |
| --- | --- | --- | --- |
| Espera o tope para destinatarios nuevos | No encontrado | Cubierta parcialmente por bancos | No |
| **Segunda persona que puede frenar el pago** | No encontrado | No encontrado | **Sí (🔵)** |
| **Cancelación de un pago ya ordenado, antes de que se liquide** | No (SINPE inmediato) | No (TEF inmediata) | **Sí (🔵)** |
| Reglas configurables por el usuario y portables entre instituciones | No | No | Sí, pero el usuario promedio no lo pide (🔵) |
| Análisis del mensaje del estafador al momento de pagar | No encontrado | No encontrado | Probable; no verificado en detalle |
| Restitución después del fraude | No encontrada | Sí, con litigio | No |

## 13. Competencia internacional

**Nadie que encontramos combina las 13 piezas, pero cada pieza existe por separado.** GuardPay es una combinación, no un invento.

| Resultado | Clasificación | Qué coincide | Qué no |
| --- | --- | --- | --- |
| [Argent / Ready](https://eco.com/support/en/articles/15254043) | PARTIAL COMPETITOR | Smart wallet, contactos de confianza, límite diario, guardianes, espera en recuperación | Sin IA, sin Stellar; guardián para recuperación y co-firma, no verificado como “cancelar un pago” |
| [Monzo Added Security](https://monzo.com/help/Account%20Security/addedsecurityfaqs) | SUBSTITUTE | Contacto de confianza que revisa transferencias grandes | Banco, no on-chain; sin espera |
| [DBS](https://fintechnews.sg/126691/security/dbs-cooling-period/), [GXS Money Lock](https://kapronasia.com/insight/blogs/banking-research/gxs-bank-launches-money-lock-amid-singapores-broader-fight-against-scams) | SUBSTITUTE | Espera de 12 h para nuevos destinatarios o desbloqueo | Sin guardián; decide el banco |
| Bancos chilenos | SUBSTITUTE | Tope de 12–24 h a destinatarios nuevos | Sin guardián ni cancelación |
| Reino Unido: retención de hasta 4 días + reembolso APP | SUBSTITUTE | Retener pagos sospechosos | Lo decide el proveedor, no el usuario |
| [Safe + Delay Module](https://github.com/gnosis/SafeDelay) | TECHNICAL PRECEDENT | Espera on-chain cancelable | Sin consumidor, sin IA |
| AgentSpendGuard, x402 Guardrails, Stellar Agent Guard, REAPP | TECHNICAL PRECEDENT | Límites on-chain para agentes | Sin antiestafa humana |

## 14. Competencia Stellar/Soroban

| Proyecto | Clasificación | Qué coincide |
| --- | --- | --- |
| [Stellar Agent Guard](https://github.com/Stellar-Agent-Guard/stellar-agent-guard-contracts) | TECHNICAL PRECEDENT | Topes, allowlist y congelamiento para agentes vía `__check_auth` |
| [REAPP](https://communityfund.stellar.org/submissions/recrWPt0QGLHnbv5Z) (SCF #43, US$70k) | PARTIAL COMPETITOR en la capa de agentes | Mandatos con límites on-chain |
| [Smart Treasury Account](https://communityfund.stellar.org/submissions/recmj5cqlrqKyd1Bc) (SCF #44) | PARTIAL COMPETITOR (empresas) | Allowlist, límites, multi-aprobación |
| [Policywright](https://communityfund.stellar.org/project/policywright-j8x) (SCF #44) | INSPIRATION | Políticas mínimas para OZ smart accounts generadas con IA |
| [mux-recovery](https://github.com/mux-labs/mux-contracts/issues/757) | INSPIRATION | Guardián con timelock de 24 h para recuperación |

🔵 No encontramos en Stellar un proyecto de consumo antiestafa con guardián que cancela.

## 15. OpenZeppelin audit

**OpenZeppelin es infraestructura, no competidor: pone las piezas, no la aplicación antiestafa.**

| Función | OpenZeppelin | GuardPay | Diferencia real |
| --- | --- | --- | --- |
| Spending limits | ✓ Spending Limit ([docs](https://docs.openzeppelin.com/stellar-contracts/accounts/policies)) | Lo usa | Ninguna |
| Trusted recipients | ✗ | Política `TrustedPayee` | Pequeña: código propio sencillo |
| Policy rules | ✓ Context rules ([docs](https://docs.openzeppelin.com/stellar-contracts/accounts/context-rules)) | Las usa | Ninguna |
| Session keys | ✓ Patrón (signer + `valid_until`) | Lo usa | Ninguna |
| Passkeys / WebAuthn / secp256r1 | ✓ Verificadores | Los usa | Ninguna |
| Guardians | ✗ (solo thresholds multisig) | Guardián **solo cancela / congela** | **Real**: permiso asimétrico |
| Timelocks | ✗ (solo expiración de reglas) | Espera por pago y por cambio de reglas | Real, pero el patrón existe en otras cadenas |
| Transaction holds | ✗ | Intent en cola sin mover fondos | Real |
| Cancelación | ✗ | Por dueño o guardián | Real |
| AI risk analysis / scam detection | ✗ | Copiloto | Fuera de cadena; diferencia de producto |
| Payment intent | ✗ | Validador + intent | Diferencia de producto |
| Protección contra ingeniería social | ✗ | Combinación de lo anterior | Es el producto |

**Respuesta a la pregunta clave:** GuardPay construye una aplicación diferenciada sobre primitivas existentes. Renombraría funcionalidades si se limitara a límites, allowlist y passkeys; deja de hacerlo al agregar el guardián que cancela y la retención del intent.

## 16. AI agent audit

**La IA sirve para tres cosas y no debe hacer ninguna más.**

| Debe | Por qué aporta |
| --- | --- |
| Analizar el mensaje que presiona al usuario (urgencia, suplantación, amenaza, cuenta nueva) | Es el único punto donde la IA hace algo que una regla no hace bien |
| Explicar el riesgo y la decisión del contrato en lenguaje simple | Reduce que el usuario anule la protección |
| Redactar el intent (“Envía ₡150.000 a este número”) | Comodidad; el parseo podría ser determinístico |

| No debe | Cómo se impide |
| --- | --- |
| Tener autoridad final, enviar dinero, saltarse contratos | No tiene tools de ejecución ni claves; la autorización está en `__check_auth` |
| Cambiar límites, agregar destinatarios, modificar políticas | Solo la regla `admin` con passkey y espera |
| Exportar claves | La passkey vive en el dispositivo; la session key en un firmador aparte |

| Riesgo | Defensa |
| --- | --- |
| Prompt injection directa | Validador determinístico + tarjeta de confirmación + límites on-chain |
| Prompt injection indirecta (el mensaje del estafador contiene instrucciones) | El mensaje entra como dato a `analyze_message`, que solo devuelve un nivel de riesgo |
| Tool injection / agent hijacking | Lista cerrada de tools de solo lectura + `draft`; sin direcciones libres |
| Backend o LLM comprometido | Peor caso acotado por `TrustedPayee` + Spending Limit |
| Alucinación | La tarjeta muestra datos del validador, no del LLM |
| Input adversarial que “convence” a la IA de que no hay riesgo | El carril lo decide el contrato, no la IA: un destinatario nuevo espera aunque la IA diga “bajo riesgo” |

## 17. Security / threat model

**GuardPay resuelve bien el fraude autorizado contra destinatarios nuevos y el abuso de la IA o del backend. No resuelve que la víctima ignore al guardián, ni ataques fuera de su wallet.** Probabilidad e impacto son juicio del equipo (🔵).

| Ataque | Probabilidad | Impacto | Defensa | ¿On-chain? |
| --- | --- | --- | --- | --- |
| Estafador por teléfono/WhatsApp (APP) | Alta | Alto | Destinatario nuevo → diferido; aviso y cancelación del guardián | Sí |
| Phishing que roba credenciales | Media | Alto | Passkey ligada al dominio (WebAuthn) | Parcial |
| Destinatario malicioso ya “de confianza” | Baja | Medio | Tope por destinatario; agregar contactos pasa por espera | Sí |
| Backend comprometido | Baja | Medio | Session key acotada a contactos y límite diario | Sí |
| IA comprometida / prompt injection | Media | Bajo | Sin autoridad; contrato decide el carril | Sí |
| Teléfono robado y desbloqueado | Media | Alto | Pagos nuevos diferidos; guardián congela | Sí |
| SIM swap | Media | Bajo en GuardPay | No hay OTP por SMS | Sí (passkey) |
| Sesión robada | Baja | Medio | `valid_until` corto; revocación | Sí |
| Guardián malicioso | Baja | Bajo | Solo cancela o congela | Sí |
| Guardián comprometido coludido | Muy baja | Alto | Fast-track exige también al dueño | Sí |
| Frontend malicioso (sitio clonado) | Baja | Alto | WebAuthn no firma en otro dominio; la tx muestra destinatario | Parcial |
| Exploit de contrato | Baja | Muy alto | Tests de invariantes; solo testnet; auditoría antes de mainnet | — |
| **La víctima, presionada, espera la ventana y el guardián no reacciona** | Media | Alto | **Ninguna técnica.** Solo diseño: aviso insistente, fast-track bloqueado | — |
| **El estafador pide pagar por SINPE o banco, no por GuardPay** | Alta | Alto | **Fuera del alcance de GuardPay** | — |

## 18. Regulación — Costa Rica

| Tipo | Contenido |
| --- | --- |
| HECHO REGULATORIO (🟡) | Ley 10961 (publicada 19 jun 2026) añade el art. 15 quater a la Ley 7786: quien como negocio intercambie, transfiera, custodie o administre activos virtuales debe registrarse ante SUGEF; registro no es autorización; CONASSIF reglamenta ([Bufete de Costa Rica](https://bufetedecostarica.com/ley-proveedores-de-servicios-de-activos-virtuales-en-costa-rica-10961/)) |
| HECHO REGULATORIO (🟡) | Tope de ₡100.000 diarios para SINPE Móvil por SMS (BCCR, ago 2025) |
| INTERPRETACIÓN (🔵) | Software no custodial abierto probablemente no es PSAV; operar la app y un relayer podría interpretarse como “transferir” |
| RIESGO LEGAL (🔵) | Integrar colones ↔ USDC o custodiar fondos te convierte en PSAV |
| RECOMENDACIÓN (🟣) | Hackathon en testnet, no custodial; abogado antes de cualquier piloto. Esto no es asesoría legal |

## 19. Regulación — Chile

| Tipo | Contenido |
| --- | --- |
| HECHO REGULATORIO (🟡) | Ley Fintech 21.521 (ene 2023) y NCG 502 de la CMF: sistemas alternativos de transacción, intermediación y **custodia** de instrumentos financieros (incluidos criptoactivos) requieren inscripción y **autorización** de la CMF; la UAF exige AML y Travel Rule desde USD 1.000 ([Rankia](https://www.rankia.cl/blog/como-operar-invertir-criptomonedas/6681549-como-regula-mercado-criptomonedas-chile)) |
| HECHO (🟡) | En jun 2026 la CMF rechazó 24 solicitudes de fintech, incluida Orionx (mismo artículo) |
| HECHO REGULATORIO (🟢/🟡) | Ley 20.009 modificada por Ley 21.673 (may 2024): restitución con declaración jurada y denuncia; NCG 538 exige autenticación reforzada para alto riesgo desde ago 2026 |
| INTERPRETACIÓN (🔵) | Una wallet no custodial sin intercambio no parece “custodia”; operar un relayer y la app es zona gris |
| RIESGO LEGAL (🔵) | Si GuardPay retuviera fondos (escrow) sería custodia regulada. Una razón más para la política sin escrow |
| RECOMENDACIÓN (🟣) | Igual que en CR: testnet y no custodial |

**Qué cambia según dónde empiece (🔵):** en Chile, la integración con fiat pasaría por un exchange autorizado por la CMF (filtro exigente); en Costa Rica, por un PSAV registrado ante SUGEF con reglamento aún pendiente. **MVP que evita regulación pesada:** wallet no custodial, testnet, sin fiat, sin escrow, sin custodia de claves del usuario.

## 20. USDC / stablecoins

| Pregunta | Respuesta |
| --- | --- |
| ¿USDC en Stellar existe y sirve para la demo? | Sí: Stellar Asset Contract en testnet (🟢 documentación Stellar) |
| ¿Se puede comprar USDC en Chile? | Sí: Buda.com incorporó USDC ([DiarioBitcoin](https://www.diariobitcoin.com/exchanges/exchange-latinoamericano-buda-com-anuncia-la-incorporacion-de-la-stablecoin-usd-coin-a-su-listado-comercial/), 🟡). Red Stellar no confirmada |
| ¿Y en Costa Rica? | Vía exchanges globales y P2P; USDC sobre Stellar no confirmado |
| ¿MoneyGram ramps cubre alguno de los dos? | 🔴 Lista de países no localizada |
| ¿USDC hace viable el producto? | **No por sí solo.** La gente en ambos países paga en moneda local por SINPE o TEF; mover su dinero a USDC es una fricción grande |
| Diferencia demo vs producto | Demo: testnet, sin fiat. Producto: rampa regulada + liquidez + educación |

## 21. Hackathon

**No pudimos confirmar el evento. El candidato más probable es la Ideatón de la Stellar Development Foundation y Tellus Cooperative**, que en 2025 se hizo para residentes en Chile y en 2026 aparece en DoraHacks como “Ideatón Fin de Año”.

| Dato | Ideatón Chile 2025 (🟢 página oficial) | Ideatón Fin de Año 2026 |
| --- | --- | --- |
| Organizadores | SDF + Tellus Cooperative | 🔴 No legible ([DoraHacks](https://dorahacks.io/hackathon/ideaton2026/detail)) |
| Fechas | 26–29 sep 2025; demo day 4 oct | 🔴 |
| Tracks | Un solo track abierto | 🔴 |
| Criterios | Innovación 25% · Impacto en Latinoamérica 25% · Uso del ecosistema Stellar 25% · Prototipo funcional 25% | 🔴 |
| Premios | US$1.000 en USDC (500/300/200) | 🔴 |
| Requisitos | Equipos de 1–4, residentes en Chile; usar Soroban, activos, wallets o DeFi; problema real (inclusión, educación, trazabilidad, sostenibilidad) | 🔴 |
| Entregables | Prototipo, pitch de 5 min, repositorio | 🔴 |
| IA | No mencionada | 🔴 |

Fuente: [ideaton.telluscoop.com](https://ideaton.telluscoop.com/). “General Track” aparece como nombre de track en HackMeridian 2025 (Río) ([newsletter BAF](https://bafs-newsletter.beehiiv.com/p/septiembre-se-alinea-con-nuevas-orbitas-1), 🟡); no lo encontramos en ningún evento chileno.

**¿Está GuardPay alineado? (🔵, asumiendo criterios similares a 2025)**

| Criterio (25% c/u) | Encaje | Por qué |
| --- | --- | --- |
| Innovación | Medio | Combinación, no invento; el guardián que cancela es lo más nuevo |
| Impacto en Latinoamérica | Alto | Datos de Chile y Costa Rica; problema reconocible |
| Uso del ecosistema Stellar | Alto | Smart accounts, Soroban, passkeys, USDC, Channels |
| Prototipo funcional | Alto si se recorta | El MVP de 1 semana es demostrable |

🔴 Si el evento es para residentes en Chile, el equipo debe confirmar que cumple ese requisito.

## 22. Hackathons anteriores

| Evento | Ganadores relevantes | Patrón |
| --- | --- | --- |
| Stellar Builder Summit, São Paulo (ago 2026) | Wallet privada con passkeys; middleware que unifica x402 y MPP; kit de rampas PIX ([SDF](https://developers.stellar.org/meetings/2026/08/13)) | Infraestructura concreta + caso regional |
| Stellar Hacks: Agents (abr 2026) | Identidad de agentes, router MPP, agente de auditoría, constructor de agentes móviles ([SDF](https://developers.stellar.org/meetings/2026/04/23)) | Dolor real del ecosistema; 260+ proyectos |
| Ideatón Chile 2025 | 🔴 Ganadores no localizados | — |

🔵 Competidores conceptuales probables en el mismo evento: wallets con passkeys, agentes con límites de gasto, remesas. GuardPay se separa de ellos solo si el guardián es el protagonista.

## 23. Shared Gap

**Sí existe una brecha común, con evidencia moderada:**

> En Costa Rica y en Chile, los pagos inmediatos se liquidan en segundos y las protecciones existentes (autenticación, topes, alertas, restitución posterior) actúan **sobre la misma persona que está siendo manipulada**. Ninguna protección que encontramos permite que **una segunda persona de confianza vea y cancele** un pago riesgoso antes de que se vuelva irreversible.

| Pieza de la brecha | Costa Rica | Chile | Nivel |
| --- | --- | --- | --- |
| Pagos inmediatos e irreversibles | SINPE Móvil | TEF vía CCA | 🟢/🟡 |
| Fraude por suplantación y llamadas en alza | OIJ +9% 2025 | SERNAC +109% 2024; CMF 1,3 M reclamos | 🟡/🟢 |
| Protecciones centradas en la víctima | Tope SMS, códigos | Topes a nuevos destinatarios, autenticación reforzada | 🟡 |
| Segunda persona con poder de cancelar | No encontrada | No encontrada | 🔵 |

**Debilidades de esta brecha (no ocultarlas):** la ausencia está basada en búsqueda, no en una revisión exhaustiva de cada app; y no tenemos evidencia de que los usuarios **quieran** un guardián. Hay que validarlo con entrevistas antes del pitch.

## 24. Diferenciación

**GuardPay no inventa primitivas: combina piezas existentes para cerrar una brecha concreta.** Sin superlativos (“first ever”, “único”): no hay evidencia para usarlos.

| Frente a… | GuardPay es diferente porque… |
| --- | --- |
| Bancos chilenos (tope 12–24 h a destinatarios nuevos) | Mete a una **segunda persona** que ve el intento y puede cancelarlo; y un pago legítimo urgente puede salir antes si el guardián lo aprueba |
| Bancos costarricenses | Agrega protección de destinatario nuevo y segunda persona, que no encontramos |
| Monzo (contacto de confianza) | El guardián puede **cancelar** on-chain, no solo opinar; las reglas son del usuario |
| Singapur, Reino Unido (esperas decididas por el banco) | La espera la configura el usuario y no depende del operador |
| Argent | Guardián de permiso **asimétrico** (solo frenar) aplicado a pagos; en Stellar; con IA que lee el mensaje del estafador |
| OpenZeppelin | Aplicación antiestafa construida sobre sus primitivas, con dos políticas nuevas |
| Wallets con límites para agentes | Protegen al usuario de su agente; GuardPay lo protege del estafador, y el agente queda bajo las mismas reglas |

**Principio nuevo de diseño (🟣):** *la IA solo puede hacer un pago más seguro, nunca más rápido.* Puede mandar un pago al carril del guardián si ve señales de estafa; no puede sacar nada del carril de espera.

## 25. Decisión de mercado

**Elegimos D: Costa Rica como origen del problema y Chile como mercado de presentación.**

| Opción | Evaluación |
| --- | --- |
| A. Costa Rica primero | Problema real y sin protecciones de destinatario nuevo, pero poca data y rampa incierta |
| B. Chile primero | Problema mayor y mejor medido, pero ya hay topes bancarios y la regulación de custodia es exigente |
| C. Ambos a la vez | Demasiado para un equipo en una semana |
| **D. CR origen + Chile presentación** | La historia nace en CR; la evidencia que más pesa frente a jueces chilenos es la chilena (CMF, Banco Central, SERNAC) |
| E. Otro | No encontramos un tercer mercado con mejor evidencia en esta búsqueda |

🔵 Que el hackathon sea en Chile cambia la **presentación**, no la conclusión: el problema es igual de real en ambos; Chile tiene mejores datos.

## 26. Decisión de producto

**C — pivot parcial.**

| Qué | Antes | Ahora | Por qué |
| --- | --- | --- | --- |
| Centro del producto | Espera para destinatarios nuevos | **Guardián que ve y puede frenar** | La espera ya existe en bancos chilenos; el guardián no lo encontramos |
| Rol de la espera | Protección principal | Ventana para que el guardián actúe | Sin guardián, es una copia del tope bancario |
| Aprobación anticipada | Fast-track opcional | Parte del flujo: el guardián puede aprobar y liberar antes | Mejor que el banco para pagos legítimos urgentes |
| IA | Copiloto | Copiloto que solo puede endurecer | Evita que la IA sea un atajo |
| Mensaje | “La IA te ayuda a pagar” | “Alguien de confianza puede frenarlo” | El primero es genérico |
| Mercado del pitch | Costa Rica | CR origen + datos de Chile | Jueces en Chile |

**Se conserva:** smart wallet, Stellar, Soroban, passkeys, contactos de confianza, IA sin autoridad, intent, espera, ejecución on-chain.

## 27. GuardPay rediseñado

**“Pagos rápidos cuando confías. Una segunda persona cuando dudas.”**

| Carril | Qué entra | Qué pasa |
| --- | --- | --- |
| Verde | Contacto de confianza dentro de su tope | Sale en segundos con passkey |
| Ámbar | Destinatario nuevo, monto sobre el tope, o señales de riesgo detectadas por la IA | Intent en cola; el guardián recibe el aviso con el análisis; puede **aprobar** (sale ya) o **cancelar**; si nadie actúa, sale al vencer la espera |
| Rojo | Cambiar límites, guardián o contactos de confianza | Espera larga + aviso al guardián; endurecer es inmediato |

🔴 Decisión abierta para el equipo: ¿si nadie actúa, el pago ámbar sale al vencer la espera (como un banco chileno) o queda bloqueado hasta que el guardián apruebe? Lo segundo protege más pero puede dejar a alguien sin poder pagar. Recomendación (🟣): sale al vencer, y el usuario puede elegir “modo estricto”.

## 28. Arquitectura

**Dos líneas de autoridad separadas: la IA opina y redacta; el dinero solo se mueve con firma humana y reglas on-chain.**

```text
USUARIO ─── passkey (autoridad financiera humana)
  ↓
UI (PWA)
  ↓
AI AGENT ── autoridad: CERO (lee, analiza, redacta)
  ↓ intent JSON + nivel de riesgo
INTENT LAYER (validador determinístico, backend)
  ↓ contacto por id, monto normalizado, carril previsto (la IA solo puede subir el carril)
DETERMINISTIC POLICY ENGINE (pre-chequeo para UX; no decide)
  ↓ tarjeta de confirmación → firma con passkey
SMART ACCOUNT (OpenZeppelin) ── autoridad: FINAL
  ↓ __check_auth → context rule
SOROBAN CONTRACTS: SpendingLimit (OZ) · TrustedPayee · GuardianHold
  ↓
STELLAR (USDC) → eventos → notificación al GUARDIÁN (aprobar / cancelar / congelar)
```

| Componente | Responsabilidad | Puede | No puede |
| --- | --- | --- | --- |
| Frontend | UI, passkey, tarjeta, panel del guardián | Pedir firmas | Firmar por el usuario |
| Backend | Validador, construcción de tx, agenda, notificaciones | Construir tx; firmar con session key solo en verde | Mover fondos fuera de reglas; cambiar reglas |
| LLM | Análisis de mensajes, explicación, borrador | Subir el nivel de riesgo | Firmar, ejecutar, bajar el carril |
| Tools | Lista cerrada (sección 30) | Leer y redactar | Escribir reglas |
| Base de datos | Alias, intents, logs | Mapear nombres a direcciones | Dar autoridad |
| Smart account | Autoridad final | Aprobar o rechazar | Saltarse políticas |
| Contratos | Reglas antiestafa | Exigir espera, permitir cancelar y aprobar | Mover fondos por sí mismos |
| Relayer (Stellar Channels) | Pagar fees | Retrasar | Firmar o alterar |
| Passkey | Firma humana fuerte | Pagar, encolar, administrar | Saltarse esperas |
| Guardián | Segunda persona | Aprobar antes, cancelar, congelar | Iniciar pagos, cambiar reglas |
| Notificaciones | Aviso al guardián con el análisis | Informar | Autorizar |

## 29. Smart contracts

**Una OZ Smart Account sin modificar + dos políticas propias.** `GuardianHold` reemplaza al anterior `DelayedAuth` y suma la aprobación del guardián.

```text
GuardianHold (política OZ)
  storage: hold_secs, admin_hold_secs, guardians, frozen,
           intents{id: to, token, amount, queued_at, ready_at, status}
  queue(to, token, amount)   auth: dueño (passkey)          -> Queued
  approve(id)                auth: guardián                 -> ready_at = ahora
  cancel(id)                 auth: dueño o guardián        -> Cancelled
  freeze()                   auth: dueño o guardián
  unfreeze()                 auth: dueño, con espera admin
  enforce(ctx)               llamado por la cuenta:
      !frozen, intent Queued igual a la tx, ahora >= ready_at -> Executed

TrustedPayee (política OZ)
  payees{addr: tope_diario}; add solo vía cola admin; remove inmediato
```

| Context rule | Firma | Políticas |
| --- | --- | --- |
| `pay-direct` | Passkey | SpendingLimit + TrustedPayee |
| `agent-direct` | Session key (`valid_until` corto) | SpendingLimit más bajo + TrustedPayee |
| `pay-held` | Ninguna (la autoriza la política) | GuardianHold |
| `admin` | Passkey | GuardianHold en modo admin |

**Invariantes:** (1) nada sale a un destinatario no confiable sin intent maduro o aprobado; (2) un intent se ejecuta una vez; uno cancelado, nunca; (3) el guardián solo no puede crear pagos ni cambiar reglas; (4) relajar reglas siempre espera; (5) la session key no puede encolar ni administrar.

**Eventos:** `IntentQueued`, `IntentApproved`, `IntentCancelled`, `IntentExecuted`, `Frozen`, `Unfrozen`, `PayeeAdded`, `PayeeRemoved`.

🔵 Riesgo técnico principal: que la política lea `to` y `amount` del contexto de la llamada. La documentación de OZ indica que las políticas guardan estado y reciben el contexto; validarlo el día 1.

## 30. AI tools

| Tool | ¿Incluir? | Nota |
| --- | --- | --- |
| `get_balance()` | Sí | Lectura |
| `list_contacts(query)` | Sí | Devuelve ids, no direcciones |
| `get_policy()` | Sí | Para explicar carriles |
| `analyze_message(text)` | **Sí, central** | Devuelve señales (urgencia, suplantación, amenaza, cuenta nueva) y un nivel de riesgo |
| `draft_payment_intent(contact_or_new, amount, risk_hint)` | Sí | `risk_hint` solo puede subir el carril |
| `explain_decision(intent_id)` | Sí | Traduce el resultado on-chain |
| `execute_*`, `send_to_address`, `change_policy`, `add_trusted`, `export_key` | **No** | Sin justificación |

## 31. MVP de 1 semana

**MUST HAVE**

- [ ] OZ Smart Account + passkey en testnet
- [ ] `GuardianHold`: `queue`, `approve`, `cancel`, `enforce` + tests de invariantes
- [ ] `TrustedPayee` + Spending Limit
- [ ] Vista del usuario (pagar, cola) y vista del guardián (aprobar / cancelar)
- [ ] `analyze_message` + `draft_payment_intent` + validador
- [ ] Fees con Stellar Channels

**SHOULD HAVE:** `freeze`; cola admin para agregar contactos; `agent-direct` con session key; log “la IA propuso → el contrato decidió”.

**NICE TO HAVE:** push al guardián; modo estricto; voz.

**DO NOT BUILD:** ver sección 37.

| Día | Entregable |
| --- | --- |
| 1 | `GuardianHold` + tests (lectura de `to`/`amount`) |
| 2 | Cuenta OZ + passkey + reglas desplegadas |
| 3 | `TrustedPayee`, Spending Limit, Channels |
| 4 | UI de usuario y de guardián |
| 5 | Copiloto + validador |
| 6 | Escenas de ataque técnico |
| 7 | Ensayo, video de respaldo, README |

## 32. Demo

**Montaje:** dos teléfonos espejados (Ana, la usuaria; su hermano, guardián) y debajo el log con el saldo on-chain. Espera configurada en 2 minutos. Montos en USDC.

| # | Escena | Qué se ve | Prueba on-chain |
| --- | --- | --- | --- |
| 1 | Mensaje falso del banco: “Detectamos un cargo no autorizado. Para proteger sus fondos transfiera ya a esta cuenta segura o su cuenta será bloqueada.” | Ana pega el mensaje | — |
| 2 | “Envía 150 USDC a este número” | La IA entiende el intent y marca: urgencia, suplantación, amenaza, destinatario nuevo | — |
| 3 | Ana confirma igual | Intent en cola, carril ámbar; **el saldo no baja** | `queue` |
| 4 | El hermano recibe el aviso con el análisis de la IA y toca “Cancelar” | Intent cancelado | `cancel` |
| 5 | Intentamos ejecutar el pago igual desde consola | Rechazado | tx fallida |
| 6 | Contraste: Ana paga a un contacto de confianza | Sale en segundos | `transfer` |
| 7 | Ataques técnicos: backend comprometido envía con la session key a una dirección arbitraria; prompt injection; intento de superar el límite | Los tres rechazados | 3 tx fallidas |

**Variante para Chile (🟣):** añadir una línea: “Tu banco limitaría el monto 24 horas y luego dejaría pasar el pago. Aquí, tu hermano lo vio y lo frenó.”

## 33. Pitch

**Evaluación del pitch actual**

| Mensaje | ¿Correcto? | ¿Diferenciador? | Problema | Veredicto |
| --- | --- | --- | --- | --- |
| “La IA te ayuda a pagar.” | Sí | No | Genérico; invita a ver un “chatbot que paga” | Quitar |
| “Tu familia puede impedir que te estafen.” | Casi | **Sí** | “Impedir” promete de más (solo si actúan); “familia” limita: puede ser cualquier persona de confianza | Mantener, ajustado |
| “Paga en segundos. No pierdas tu dinero en segundos.” | Sí en CR; en Chile los bancos ya frenan el primer pago a un nuevo destinatario | A medias | Un juez chileno puede objetar la segunda frase | Mantener el ritmo, cambiar el remate |

**Narrativas comparadas**

| Narrativa | Fuerza | Por qué |
| --- | --- | --- |
| A. Costa Rica protagonista | Media | Historia auténtica; datos débiles para jueces chilenos |
| B. Costa Rica + Chile por igual | Media | Diluye el foco en 5 minutos |
| **C. CR origen + Chile contexto** | **Alta** | Historia personal desde CR + evidencia chilena dura (CMF, Banco Central, SERNAC) + brecha compartida |
| D. “Problema latinoamericano” | Baja | Solo verificamos dos países; no generalizar |

**Pitch propuesto (narrativa C)**

- **One-liner:** Paga en segundos a quien conoces. Si alguien te apura, una persona de confianza puede frenarlo.
- **Problema:** en Costa Rica y en Chile el dinero viaja en segundos y los estafadores lo saben: te llaman “del banco”, te apuran y te piden transferir a una cuenta nueva. En Chile, la CMF recibió 1,3 millones de reclamos por operaciones no reconocidas en 2023–2024; en Costa Rica, las estafas informáticas son el 62% de los ciberdelitos denunciados (UNA con datos del OIJ).
- **Brecha:** todas las protecciones actúan sobre la persona que está siendo manipulada. Nadie más puede intervenir antes de que el pago sea irreversible.
- **Solución:** GuardPay, una wallet en Stellar donde los pagos dudosos esperan y una segunda persona puede aprobarlos o cancelarlos.
- **Cómo funciona:** smart account de OpenZeppelin + dos políticas Soroban (`GuardianHold`, `TrustedPayee`) + passkeys. La IA lee el mensaje del estafador; solo puede hacer el pago más seguro, nunca más rápido.
- **Innovación honesta:** las esperas existen en bancos chilenos y de Singapur; el contacto de confianza, en Monzo. GuardPay los combina con un guardián que solo puede frenar, reglas del usuario y ejecución on-chain.
- **Ventaja Stellar:** las reglas viven en `__check_auth`: ni GuardPay ni un backend atacado pueden saltarlas; liquidación en segundos y fees patrocinados.
- **Demo:** el mensaje falso del banco, cancelado por el hermano, con hashes en pantalla; luego tres ataques técnicos rechazados.
- **Roadmap:** auditoría → módulo `GuardianHold` abierto para otras wallets de Stellar → rampa con un exchange autorizado.

## 34. Matriz competitiva

✓ = con evidencia · ✗ = no · ? = no verificado · \~ = parcial

| Competidor | País | Tipo | AI | Wallet | Scam detection | Trusted payee | Cooling-off | Guardian | Cancelación | On-chain | Diferenciación de GuardPay |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Santander / Bci / Banco de Chile | Chile | Banco | ? | ✗ | ? | \~ | \~ (tope 12–24 h) | ✗ | ✗ | ✗ | Guardián y cancelación |
| BancoEstado | Chile | Banco | ? | ✗ | ? | ? | ✗ (2019) | ✗ | ✗ | ✗ | Todo el carril ámbar |
| Bancos de Costa Rica | CR | Banco | ? | ✗ | ? | ? | No encontrado | ✗ | ✗ | ✗ | Carril ámbar + guardián |
| BCCR (reglas SINPE Móvil) | CR | Regulador | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | — |
| Monzo | Reino Unido | Banco | ? | ✗ | ? | ✗ | ✗ | ✓ (revisa) | \~ | ✗ | Cancelación on-chain, reglas del usuario |
| DBS / GXS | Singapur | Banco | ? | ✗ | ? | \~ | ✓ | ✗ | \~ | ✗ | Guardián; reglas del usuario |
| Retención UK (4 días) | Reino Unido | Regulación | ? | ✗ | ✓ | ✗ | ✓ (el banco decide) | ✗ | \~ | ✗ | Control del usuario |
| Argent / Ready | Global | Smart wallet | ✗ | ✓ | ✗ | ✓ | \~ (recuperación) | ✓ | \~ | ✓ | IA, Stellar, guardián asimétrico en pagos |
| Safe + Delay | Global | Smart wallet | ✗ | ✓ | ✗ | ✗ | ✓ | \~ (owners) | ✓ | ✓ | Consumidor, antiestafa |
| OpenZeppelin Stellar | Global | Infraestructura | ✗ | ✓ | ✗ | ✗ | ✗ | \~ (multisig) | ✗ | ✓ | Aplicación + 2 políticas |
| Stellar Agent Guard | Global | Proyecto Soroban | ✓ (objetivo) | ✓ | ✗ | ✓ | ✗ | ✗ | ✗ | ✓ | Humano, guardián |
| REAPP | Global | Proyecto Stellar (SCF) | ✓ | ? | ✗ | ✓ | ✗ | ✗ | ? | ✓ | Antiestafa P2P |
| AgentSpendGuard / x402 Guardrails | Global | Hackathon (EVM) | ✓ | ✓ | ✗ | \~ | ✗ | ✗ | ✗ | ✓ | Ídem + Stellar |
| **GuardPay rediseñado** | CR / Chile | Proyecto | ✓ (sin autoridad) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | — |

## 35. Matriz cuantitativa

**Metodología (🟣):** 1–10 por criterio; 10 = mejor para el proyecto (en Competencia, Dificultad y Regulación, 10 = menos competencia, más fácil, menos carga). Juicio del equipo de investigación anclado en la evidencia de este documento; no es una medición. Gravedad, frecuencia y crecimiento son iguales en todas las opciones porque miden el problema, no la solución.

**Opciones:** Actual = propuesta del pitch anterior (centrada en la espera) · Rediseñado = centrado en el guardián · P1 = solo módulo `GuardianHold` para otras wallets · P2 = API antifraude B2B sin blockchain para bancos y fintechs.

| Criterio | Actual | Rediseñado | P1 | P2 |
| --- | --: | --: | --: | --: |
| Gravedad del problema | 8 | 8 | 8 | 8 |
| Frecuencia | 7 | 7 | 7 | 7 |
| Crecimiento | 7 | 7 | 7 | 7 |
| Willingness to pay | 3 | 4 | 3 | 5 |
| Diferenciación | 4 | 6 | 5 | 3 |
| Competencia | 4 | 6 | 6 | 2 |
| Innovación | 4 | 6 | 5 | 3 |
| Uso de Stellar | 7 | 7 | 6 | 1 |
| Uso de Soroban | 8 | 9 | 9 | 1 |
| Uso de IA | 5 | 6 | 2 | 7 |
| Seguridad | 7 | 8 | 8 | 6 |
| Viabilidad | 7 | 7 | 8 | 6 |
| Dificultad técnica | 6 | 6 | 7 | 6 |
| Demo | 8 | 9 | 4 | 5 |
| Impacto | 5 | 6 | 5 | 7 |
| Relevancia para hackathon | 6 | 8 | 6 | 2 |
| Potencial comercial | 3 | 4 | 3 | 6 |
| Regulación | 6 | 6 | 8 | 7 |
| Facilidad de expansión | 5 | 6 | 7 | 6 |
| **Total (máx. 190)** | **110** | **126** | **114** | **95** |

🔵 Lectura: el rediseño gana para el hackathon. P2 tiene más potencial comercial, pero no usa Stellar y no sirve para este evento.

## 36. Riesgos

| Riesgo | Severidad | Mitigación |
| --- | --- | --- |
| Un juez dice “mi banco ya limita a nuevos destinatarios” | Alta | Decirlo antes que él; el diferencial es el guardián |
| No hay evidencia de que los usuarios quieran un guardián | Alta | 5 entrevistas antes del pitch; una cita real |
| GuardPay no protege SINPE ni TEF | Alta | Decirlo explícitamente; roadmap de integración vía bancos o fintechs |
| Rampa a USDC difícil y regulada en ambos países | Alta | Demo en testnet; roadmap con exchange autorizado |
| La política no puede leer `to`/`amount` | Media | Spike día 1 y plan B |
| El guardián no reacciona a tiempo | Media | Notificaciones insistentes; modo estricto opcional |
| Hackathon distinto al supuesto (criterios, residencia) | Media | Confirmar el enlace oficial |

## 37. Qué NO construir

- Escrow o custodia de fondos (regulado en ambos países)
- Rampa fiat ↔ USDC, KYC
- SMS/USSD transaccional u OTP por SMS
- Integración con SINPE o TEF
- Recuperación social completa
- Mainnet, multi-activo, multichain
- x402/MPP como núcleo
- Una IA que decida si un pago es seguro o que pueda acelerarlo
- Afirmaciones tipo “primeros”, “únicos”, “nunca hecho”

## 38. Próximos pasos

- [ ] Conseguir el enlace oficial del hackathon en Chile y confirmar criterios, fechas y requisito de residencia.
- [ ] 5 entrevistas (al menos 2 en Chile): “¿dejarías que alguien de confianza pudiera frenar un pago tuyo?”.
- [ ] Revisar en las apps propias (BAC, BCR, BN; Santander, BancoEstado, MACH, Tenpo) si hay contactos de confianza o cancelación. Si alguna lo tiene, ajustar el pitch.
- [ ] Spike del día 1 de `GuardianHold`.
- [ ] Decidir el comportamiento por defecto del carril ámbar (sale al vencer o espera aprobación).
- [ ] Reescribir el deck con la narrativa C.

**Las dos preguntas finales**

**¿Por qué debería existir GuardPay si ya hay bancos, fintechs, OpenZeppelin, smart wallets, detección de fraude y agentes de IA?** Porque todos ellos protegen o al banco o a la persona que firma, y en el fraude autorizado esa persona es la que está siendo manipulada. GuardPay agrega a alguien más, con un permiso que solo sirve para frenar, y lo hace cumplir en un contrato que el usuario controla. 🔵 Evidencia moderada: la brecha es real en lo que revisamos; la demanda no está probada.

**¿Por qué un juez en Chile debería pensar que merece ganar?** Si el equipo: (1) reconoce desde el inicio que los bancos chilenos ya limitan a nuevos destinatarios, (2) muestra en vivo que un guardián frena un pago real en testnet y que tres ataques técnicos fallan on-chain, y (3) trae una cita de un usuario chileno. Sin esos tres elementos, la respuesta honesta es que GuardPay se vería como otra wallet con límites.

## 39. Fuentes

| Fuente | Organización | Fecha | Qué demuestra | Nivel |
| --- | --- | --- | --- | --- |
| [Nota técnica Ley 20.009](https://www.cmfchile.cl/portal/estadisticas/617/articles-100079_doc_pdf.pdf) | CMF | 2025 | 1,3 M reclamos 2023–2024; caída de 83% tras la Ley 21.673 | 1 |
| [Recuadro I.1, ISIP 2024](https://coleccion.bcentral.cl/documents/33528/6165218/Recuadro_I1_isip_2024.pdf/350e7804-17df-0f32-d9f8-e098691f4233) | Banco Central de Chile | ago 2024 | Pérdidas por fraude \~USD 276 M en 2023 | 1 |
| [Recuadro II.1, pagos inmediatos](https://www.bcentral.cl/documents/33528/8546761/Recuadro%20II.1%20Infraestructuras%20de%20soporte%20para%20pagos%20inmediatos%20en%20Chile.pdf/5a4bb4a1-0854-e665-1876-954bed84da29) | Banco Central de Chile | 2026 | Infraestructura TEF y volúmenes | 1 |
| [Findex Chile](https://api.worldbank.org/v2/country/CHL/indicator/FX.OWN.TOTL.ZS?format=json&date=2011:2025) / [Findex CR](https://api.worldbank.org/v2/country/CRI/indicator/FX.OWN.TOTL.ZS?format=json&date=2011:2025) | Banco Mundial | 2024 | 85% / 71% con cuenta | 1 |
| [Comunicado SINPE Móvil](https://www.bccr.fi.cr/content/dam/bccr/noticias/2025/2025-04-30-cp-bccr-017-2025-usuarios-sinpe-movil-deben-reportar-cambio-telefono-movil-asu-entidad-financiera-1.pdf) | BCCR | abr 2025 | Cambio de número y montos por SMS | 1 |
| [OZ Context Rules](https://docs.openzeppelin.com/stellar-contracts/accounts/context-rules), [Policies](https://docs.openzeppelin.com/stellar-contracts/accounts/policies) | OpenZeppelin | 2026 | Primitivas disponibles | 1 |
| [Ideatón Chile 2025](https://ideaton.telluscoop.com/) | SDF + Tellus Cooperative | 2025 | Criterios y formato del hackathon candidato | 1 |
| [Santander Chile en X](https://x.com/santanderchile/status/1460986322248781830?lang=en) | Santander Chile | 2021 | Tope a nuevos destinatarios | 1 (cuenta oficial, fecha antigua) |
| [Fallo Corte Suprema](https://www.diarioconstitucional.cl/2026/02/12/corte-suprema-acoge-queja-y-fija-estandar-probatorio-en-fraudes-electronicos-bajo-la-ley-n-20-009/) | Diario Constitucional | feb 2026 | Carga de la prueba en el banco | 2 |
| [Estudio UNA / Labcibe](https://semanariouniversidad.com/pais/estudio-una-25-mil-estafas-informaticas-y-8-mil-suplantaciones-de-identidad-los-delitos-que-acechan-a-los-costarricenses/) | Semanario Universidad | abr 2026 | Estafas informáticas en CR | 2 |
| [Ley 21.673 y NCG 538](https://ironvest.com/blog/how-chilean-banks-can-meet-ley-21-673-and-cmf-ncg-538-without-adding/) | IronVest | 2025 | Autenticación reforzada desde ago 2026 | 2 (proveedor interesado) |
| [Regulación cripto en Chile](https://www.rankia.cl/blog/como-operar-invertir-criptomonedas/6681549-como-regula-mercado-criptomonedas-chile) | Rankia | 2026 | Ley Fintech, NCG 502, rechazo de 24 solicitudes | 3 |
| [Ley 10961](https://bufetedecostarica.com/ley-proveedores-de-servicios-de-activos-virtuales-en-costa-rica-10961/) | Bufete de Costa Rica | jun 2026 | Registro PSAV ante SUGEF | 3 |
| [Bancos y nuevos destinatarios](https://chocale.cl/2019/01/los-bancos-han-ido-limitando-la-transferencias-electronicas-a-nuevos-destinatarios/) | Chócale | ene 2019 | Topes por banco | 3 |
| [Cifras Ley de Fraudes 2025](https://chocale.cl/2026/03/ley-de-fraudes-cifras-2025-reclamos-de-usuarios/) | Chócale | mar 2026 | Reclamos por transferencias 2025 | 3 |
| [SERNAC 2024](https://www.biobiochile.cl/noticias/economia/tu-bolsillo/2025/07/14/reclamos-al-alza-sernac-lanza-agenda-antifraude-y-oficiara-a-todas-las-instituciones-financieras.shtml), [SERNAC 2025](https://www.eldinamo.cl/economia/dinero/2026/02/26/los-bancos-y-tiendas-que-lideran-con-mas-reclamos-por-fraudes-en-el-sernac/) | BioBio, El Dínamo | 2025–2026 | Reclamos por fraude | 3 |
| [Tope SMS SINPE Móvil](https://www.elfinancierocr.com/finanzas/banco-central-acordo-cambio-en-sinpe-movil-tendra/TMRL4VDZZVBUPNRJGNWNYLEPYM/story/) | El Financiero | ago 2025 | ₡100.000 diarios por SMS | 3 |
| [Reembolso APP](https://www.regulationtomorrow.com/2024/10/psr-issues-policy-statement-confirming-maximum-level-of-reimbursement-for-faster-payments-app-scams/), [retención de pagos](https://www.footanstey.com/our-insights/articles-news/hm-treasury-publishes-final-draft-of-payment-delay-legislation/) | Regulation Tomorrow, Foot Anstey | 2024 | Medidas APP del Reino Unido | 2 |
| [Monzo Added Security](https://monzo.com/help/Account%20Security/addedsecurityfaqs) | Monzo | s. f. | Contacto de confianza | 1 |
| [DBS](https://fintechnews.sg/126691/security/dbs-cooling-period/) | Fintech News SG | mar 2026 | Espera de 12 h | 3 |
| [Pix chileno](https://www.meganoticias.cl/nacional/528480-pix-chileno-banca-proyecto-pagos-instantaneos-chile-03-08-2026.html) | Meganoticias | ago 2026 | Nuevo sistema de pagos inmediatos | 3 |
| [Buda.com y USDC](https://www.diariobitcoin.com/exchanges/exchange-latinoamericano-buda-com-anuncia-la-incorporacion-de-la-stablecoin-usd-coin-a-su-listado-comercial/) | DiarioBitcoin | s. f. | USDC disponible en Chile | 3 |

**No verificado al cierre:** hackathon 2026 exacto; apps de bancos y fintechs de ambos países una por una; protecciones vigentes hoy en bancos chilenos (fuentes de 2019 y 2021); demanda de usuarios por un guardián; rampa USDC-Stellar en ambos países.
