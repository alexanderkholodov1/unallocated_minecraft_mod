# Registro de paquetes de trabajo (coordinador)

PLAN.md §5 numera los paquetes de v0.1 en orden lógico. Tres ramas se abrieron antes de que el plan existiera, así
que la identidad de cada paquete es **su rama**; esta tabla da la correspondencia con PLAN.md. Los paquetes nuevos
toman números libres.

| Rama (identidad) | Contenido | En PLAN.md | Estado |
|---|---|---|---|
| `v0.1.1` | Toolchain, CI, smoke de servidor y cliente | v0.1.1 | hecho, CI verde (379ffc5) |
| `v0.1.2` | Este plan (PLAN, DECISIONS, VERIFIED) | v0.1.2 | hecho (82dc345) |
| `v0.1.3` | Contrato y advertencias | v0.1.4 | CI verde (d044d1b); revisión cruzada en curso |
| `v0.1.4` | Interfaz como arma + diario de opciones | parte de v0.2 (F3, muertes, logros, consejos, ajustes) | arreglando compilación; ver D-21 |
| `v0.1.5` | Cebo del catalejo | v0.1.8 | en construcción |
| `v0.1.6` | Fabric API jar-in-jar + chequeo de JAR en CI | v0.1.3 | por lanzar |
| `v0.1.7` | Núcleo: `Act`, `ActSequencer`, `SessionState`, `Features`, `/unallocated` | v0.1.5 | por lanzar |
| `v0.1.8` | `MapLayout` y secciones del mapa por código | v0.1.6 | tras 1.7 |
| `v0.1.9` | Plantilla del mundo y botón "UNALLOCATED" | v0.1.7 | tras 1.8 |
| `v0.1.10` | Final v0 "el servidor se cerró" | v0.1.9 | tras 1.7 |
| `v0.1.11` | Audio v0 y créditos | v0.1.10 | por lanzar |
| `v0.1.12` | Página de itch, README, lista de la jam, errores conocidos | v0.1.11 | al final del hito |
| `v0.1.13` | Gametest de recorrido v1 | v0.1.12 | al final del hito |
| `v0.1.0` | Rama de integración del hito | — | se crea al integrar |

Los hitos v0.2 y siguientes usan la numeración de PLAN.md tal cual; lo ya hecho en `v0.1.4` se anotará
como entregado cuando se abran esos paquetes.
