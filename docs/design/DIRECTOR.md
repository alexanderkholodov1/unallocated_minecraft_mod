# UNALLOCATED — Director por telemetría (capa bonus, 100% local)

*El motor del miedo es el mapa scripteado (GAMEPLAY.md). Esto es una capa extra que hace que la experiencia se sienta
viva y "aprenda de ti". Es prescindible: si se quita o falla, el guion fijo funciona igual. **Nada sale a internet.***

---

## 1. Qué es y qué no es

- **Es:** un director local que lee tu comportamiento y **elige el momento y la intensidad** de los sustos
  pregenerados, controlando la curva de tensión (calma → presión → pico → alivio falso). También alimenta la ilusión
  de "aprende de ti".
- **No es:** un LLM en línea, ni un servicio remoto, ni un RL que converge en una sesión de 15 min. "Parecer que
  aprende vale más que aprender de verdad" (Alien: Isolation). Como el Director de Left 4 Dead: controla el **ritmo**,
  no "le gana" al jugador.

---

## 2. Señales (telemetría del cliente, local)

Por jugador, armando un perfil para la sesión:
- Velocidad angular de cámara y picos (giros bruscos = susto).
- Sprints, saltos repetidos, agacharse, tiempo quieto, retrocesos.
- Duda ante umbrales (cuánto tardas en entrar a una cueva o lugar).
- A qué miras (raycast), distancia a cebos.
- Cambios de hotbar, daño recibido.
- Texto del chat (para el idioma que aprende; NARRATIVA.md §5).

**Calibración:** los primeros minutos tranquilos (fase presa-base) fijan tu **línea base personal**. La "señal de
miedo" f(t) es, en la trama, la señal de recompensa del Reclaimer.

---

## 3. Selección de sustos

- **Director con prioridades manuales** (priors de diseño) sobre un conjunto de ~20 primitivas de susto. Un bandit
  contextual simple (p. ej. Thompson sampling) **opcional** por encima, con restricciones: enfriamientos, no repetir
  patrón, topes de intensidad, presupuesto por fase.
- **Novedad forzada:** si el jugador se suprime (se queda quieto, no reacciona), el director explora estímulos nuevos
  (auditivos, visuales, de interfaz) en vez de insistir.
- **Selección principal por novedad y coherencia; el miedo estimado desempata.** La señal es ruidosa y hay pocos datos,
  así que no se confía en ella como verdad, solo como sesgo.

---

## 4. "Aprende de ti" de verdad (lo que sí es real)

Tres cosas aprenden de ti dentro de la sesión, y son las que hay que priorizar porque son creíbles y baratas:
1. **Tu Artifact imita tu trayectoria.** Se graban tus movimientos (posición, yaw/pitch, acciones) y tu eco los repite,
   degradándose. Es imitación real, pequeña; el error acumulado es una ventaja estética (colapso/repetición).
2. **Tu idioma entra en sus textos.** Tu nick y tus palabras del chat se incorporan a sus logs/mensajes (NARRATIVA.md
   §5).
3. **El Heap se afina contra ti entre muertes.** Lo que te asustó (según la telemetría) se intensifica o reaparece
   peor; lo que ignoraste se reemplaza (GAMEPLAY.md §3).

---

## 5. Contenido pregenerado y curado

Todo lo "generado por IA" se **pregenera offline, se cura a mano y se empaqueta** en el mod. No hay generación en vivo:
- Textos derivados (deriva g0→g5, mensajes de muerte, logs, consejos de carga, letreros que mutan).
- "Voces"/audios, si se usan (opcional; sin dependencia de chat de voz). Si hay micrófono, a lo sumo capturar el "De
  acuerdo" del contrato para devolverlo degradado; si no, todo por texto. **No es ruta crítica.**
- Estructuras y pasadas de corrupción del Heap.

Principio: **el modelo propone (offline), un filtro dispone**. Se corren muchas generaciones antes de entregar y se
quedan las mejores. Sigue siendo "generado", solo que filtrado.

---

## 6. Privacidad y seguridad

- **Todo local.** La telemetría, el perfil y cualquier captura de voz/chat **no salen del equipo del jugador**. No hay
  servidor de inferencia ni red.
- El consentimiento del contrato cubre el uso (micrófono si se activa, chat, comportamiento, ajustes temporales), con
  explicación clara. Mínima retención: solo lo necesario para el propósito del juego, borrado al cerrar.
- Lo que el jugador teclea es **dato**, nunca orden para ningún modelo (no hay modelo en vivo que inyectar); el "Shell"
  del juego es ficción scripteada (GAMEPLAY.md §6).

---

## 7. Degradación elegante

Si la capa del director falla o se desactiva: el mapa dispara sus sustos por **triggers fijos de posición/tiempo**, y
la experiencia sigue completa de punta a punta. Interruptor de depuración para el autor: forzar sustos, saltar actos,
ver f(t).
