# UNALLOCATED — Orden de construcción (para el capitán de Beacon)

*Por hitos, no por fechas (preferencia del dueño): los hitos pueden avanzarse de golpe o uno a uno según los recursos.
Cada hito tiene un criterio de aceptación claro, para hacer imposible el desastre de producción. Minecraft Java 26.3,
Fabric, un jugador primero. El motor del miedo es el mapa scripteado; el director es bonus.*

---

## 1. Piezas de código existentes (reutilizar o descartar, sin obligación)

Del código base ya disponible:
- **Pantalla de "contrato"** antes del menú → usar para el consentimiento + advertencias + número de sujeto + spec en
  letra chica (LORE.md §4.1, NARRATIVA.md §6).
- **Núcleo de sesión con actos y estado guardado** → usar para fases, Trial actual, perfil por jugador, flags
  (proceso suspendido, punto de no retorno).
- **Mapa por código (lobby, colina, camino, base, mina)** → esqueleto del Overworld (MAPA.md §3).
- **Cebo de catalejo con susto** → la colina [3] (GAMEPLAY.md §9).
- **Efectos sobre la interfaz (cámara, chat, HUD, avisos, pantalla de muerte)** → interfaz como arma, chat reescrito,
  pantalla de muerte escrita por él, ventana del watchdog.
- **Final provisional** → base para la ventana del watchdog y los tres finales.
- **Sistema de audio** → lag/atenuación, screamers puntuales, ambiente.

Si el diseño necesita algo que no está, pedirlo.

---

## 2. Hitos (con criterio de aceptación)

> Dependencias entre corchetes. El orden recomendado es H0→H6; H7 y H8 son opcionales/posteriores.

### H0 — Cimientos
Arranca el mod en 26.3; contrato → menú → carga del mapa; núcleo de sesión con fases; gestor de opciones que **guarda y
restaura siempre** (incluso tras crash); telemetría básica local; esqueleto del Overworld por código.
**Aceptación:** build verde; una sesión entra, carga el lobby y sale sin crash; cualquier ajuste tocado se restaura.

### H1 — Presa jugable (línea base + estímulo) [H0]
Lobby/hub/colina/camino/base/mina; catalejo-cebo; lag como presencia; interfaz como arma (sutil); chat reescrito;
tiempo perdido; loop de la mina seamless; suelo podrido → transición al Heap.
**Aceptación:** un jugador recorre del contrato a la caída al Heap en ~7 min, sin crash, entendiendo a dónde ir sin
que le expliquen.

### H2 — El Heap y la primera muerte [H1]
Dimensión Heap (copia corrupta por datapack) con etapas de pudrición; primera muerte inevitable; flip a vista de
Puppet; la red de Threads y los Artifacts visibles como Puppet.
**Aceptación:** caer al Heap, morir, verse como Puppet con la red, y reaparecer ("De acuerdo") de forma estable.

### H3 — Mecánicas de Puppet [H2]
Cortar hilos (tijeras) → leaks + aceleración de la vuelta (rechazo de muestra corrupta); tirón de los hilos; Trials que
suben y escalada del Heap entre muertes; cegado de sensores en el camino al Kernel.
**Aceptación:** el jugador puede acelerar su vuelta cortando hilos; dos o tres muertes a propósito abren el camino al
Kernel; el Heap se nota más hostil con cada muerte.

### H4 — Entrega de la historia [H1, H3]
Rastro del predecesor (foro/MOTD, letreros, libro, postmortem); ecos de comportamiento; logs/errores por varias vías;
idioma que incorpora nick y chat del jugador. Mapear cada hecho del lore a su pieza (NARRATIVA.md §6).
**Aceptación:** un jugador nuevo, sin explicaciones, puede reconstruir la historia de principio a fin al terminar
(verificar con 2–3 testers y un cuestionario corto).

### H5 — Kernel, Shell y finales [H2, H3]
Punto de no retorno señalizado; Shell diegético (entrada del Puppet → intérprete) con secretos por mundo; suspender el
bucle; ventana del watchdog guiada; los tres finales (Completar / Envenenar / Consumido) con sus textos (LORE.md §4).
**Aceptación:** los tres finales son alcanzables y legibles; el final malo (Consumido) solo ocurre tras cruzar el punto
de no retorno avisado; Completar y Envenenar cierran con su texto.

### H6 — Director y pulido [H1–H5]
Director por telemetría afinando momento/intensidad (con degradación a triggers fijos); balance de ritmo; advertencias
de contenido; empaquetado; página de itch.io; capturas y video corto.
**Aceptación:** 0 crashes en varias sesiones seguidas; instalación en frío exitosa desde la página; ritmo que no cansa.

### H7 — (Opcional) Voz [H4]
Captura del "De acuerdo" y devolución degradada, solo si alcanza. Sin dependencia de chat de voz. **No ruta crítica.**

### H8 — (Posterior) Multijugador
Puppets de unos ayudando/estorbando a vivos de otros (GAMEPLAY.md §12). Después de la entrega de un jugador.

---

## 3. Qué recortar primero si aprieta el tiempo (técnico, NUNCA la historia)

El dueño fue explícito: la historia y los finales no se recortan ("hacemos bien o no hacemos"). Si falta tiempo, se
recorta **alcance técnico**, en este orden:
1. **H8 multijugador** (ya es posterior).
2. **H7 voz** (opcional).
3. **H6 director** → degradar a **triggers fijos** (el mapa scripteado ya es el motor; el director es bonus).
4. **Fidelidad del Heap** → menos etapas de pudrición / radio de copia menor, sin perder la primera muerte ni el Kernel.
5. **Interfaz como arma** → quedarse con los golpes más fuertes (pantalla de muerte, F3, chat reescrito) y soltar los
   menores.

Lo que **no** se recorta: contrato, presa jugable (H1), primera muerte y Puppet (H2–H3), entrega de la historia (H4),
Kernel y los tres finales (H5).

---

## 4. Restricciones de entrega (itch.io)

Objetivo de entrega: **2026-10-28 23:59 (UTC−5)** (confirmar estado actual con el dueño). La página debe incluir:
archivo del mod y dependencias; loader y versión exactos (Fabric, Minecraft 26.3, Java 25); instrucciones de
instalación e inicio; mapa/config si hacen falta; descripción breve; equipo y créditos de recursos externos; errores
conocidos y **advertencias de contenido** (luces intermitentes, sonidos fuertes). Recomendado: capturas y video corto.

Criterio de dependencias: **cada dependencia obligatoria baja la facilidad de instalación**; incluir solo lo realmente
necesario y con instalación sencilla. Verificar en 26.3: Fabric Loader/API, y mods de optimización si de verdad hacen
falta (ModernFix, Lithium, C2ME, Noisium, Starlight). Simple Voice Chat solo si se hace H7.

---

## 5. Pruebas mínimas

- **E0 Humo:** carga, no se cae, ajustes se restauran.
- **E1 Un jugador completo:** sesión de punta a punta sin crash; un tester nuevo, sin explicaciones, llega a un final y
  entiende la historia (cuestionario corto: momento más aterrador, dónde se confundió, si entendió qué hacer y qué era
  el Reclaimer, si el audio molestó, si volvería a jugar).
- **E5 Jurado:** instalación en frío desde la página de itch por un desconocido, 10–15 min cronometrados.
