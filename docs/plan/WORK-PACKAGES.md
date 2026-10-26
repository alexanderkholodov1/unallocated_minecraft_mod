# Registro de paquetes de trabajo (coordinador)

PLAN.md §5 numera los paquetes de alpha.1 en orden lógico. Tres ramas se abrieron antes de que el plan existiera, así
que la identidad de cada paquete es **su rama**; esta tabla da la correspondencia con PLAN.md. Los paquetes nuevos
toman números libres.

| Rama (identidad) | Contenido | En PLAN.md | Estado |
|---|---|---|---|
| `v1.0.0-alpha.1.1` | Toolchain, CI, smoke de servidor y cliente | alpha.1.1 | hecho, CI verde (379ffc5) |
| `v1.0.0-alpha.1.2` | Este plan (PLAN, DECISIONS, VERIFIED) | alpha.1.2 | hecho (82dc345) |
| `v1.0.0-alpha.1.3` | Contrato y advertencias | alpha.1.4 | CI verde (d044d1b); revisión cruzada en curso |
| `v1.0.0-alpha.1.4` | Interfaz como arma + diario de opciones | parte de alpha.2 (F3, muertes, logros, consejos, ajustes) | arreglando compilación; ver D-21 |
| `v1.0.0-alpha.1.5` | Cebo del catalejo | alpha.1.8 | en construcción |
| `v1.0.0-alpha.1.6` | Fabric API jar-in-jar + chequeo de JAR en CI | alpha.1.3 | por lanzar |
| `v1.0.0-alpha.1.7` | Núcleo: `Act`, `ActSequencer`, `SessionState`, `Features`, `/unallocated` | alpha.1.5 | por lanzar |
| `v1.0.0-alpha.1.8` | `MapLayout` y secciones del mapa por código | alpha.1.6 | tras 1.7 |
| `v1.0.0-alpha.1.9` | Plantilla del mundo y botón "UNALLOCATED" | alpha.1.7 | tras 1.8 |
| `v1.0.0-alpha.1.10` | Final v0 "el servidor se cerró" | alpha.1.9 | tras 1.7 |
| `v1.0.0-alpha.1.11` | Audio v0 y créditos | alpha.1.10 | por lanzar |
| `v1.0.0-alpha.1.12` | Página de itch, README, lista de la jam, errores conocidos | alpha.1.11 | al final del hito |
| `v1.0.0-alpha.1.13` | Gametest de recorrido v1 | alpha.1.12 | al final del hito |
| `v1.0.0-alpha.1` | Rama de integración del hito | — | se crea al integrar |

Los hitos alpha.2 y siguientes usan la numeración de PLAN.md tal cual; lo ya hecho en `v1.0.0-alpha.1.4` se anotará
como entregado cuando se abran esos paquetes.
