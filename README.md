# Mundo Olvidado (Forge 1.20.1)

Mod de terror para Minecraft Forge 1.20.1.

## Qué hace

- **Dormir te puede mandar a otra dimensión.** Al dormir en una cama del Overworld (por defecto 50 % de probabilidad, tras 3 segundos dormido) despiertas en un mundo que no es normal.
- **La dimensión** es un mundo perfectamente plano. Los robles están plantados en una cuadrícula de 7x7, todos idénticos, y unas líneas de tierra seca marcan esa cuadrícula. Cada 49 bloques, en vez de un árbol hay un pilar de cobblestone. No hay animales, flores ni agua.
- **Barras de Cordura y Ansiedad** (arriba a la izquierda, solo dentro de la dimensión; empiezan al entrar):
  - La **cordura** baja con la oscuridad de la noche, con el Asechador cerca y, mucho más, al verlo. Se recupera de día, o de noche si estás junto a una luz de bloque (antorcha, farol, glowstone...: nivel de luz 8 o más) y sin Asechador cerca.
  - La **ansiedad** sube con la noche (menos si estás a la luz), el Asechador cerca y la poca cordura; baja despacio. Por encima de 45 oyes latidos cada vez más rápidos y aparece un borde rojo que pulsa; por encima de 40 la cámara tiembla. Con la cordura por debajo de 45 se oyen sonidos que no existen.
  - **Si te mira** (te ve con línea de visión a menos de 48 bloques), la cordura baja y la ansiedad sube más rápido, y el icono de cordura late en rojo.
  - Cuando hay uno merodeando sale el mensaje *"Hay una presencia extraña al alrededor..."*, con un sonido grave (cada 2 a 3 minutos como mucho).
  - Las barras muestran icono, estado (Estable / Inquieta / Frágil / Quebrada; Calma / Tensa / Alta / Pánico), porcentaje, flecha de tendencia, estela de lo perdido, marcas en 25/50/75 y destellos al curarte o herirte.
- **Sancor y Anx:** *Sancor* es una bebida (+45 cordura, −12 ansiedad, devuelve la botella vacía, espera de 5 s). *Anx* son pastillas (−38 ansiedad, +6 cordura, dan algo de lentitud 6 s, espera de 3 s). Aparecen en *Saco pequeño*, que hay muy de vez en cuando junto a algún árbol (1 de cada ~28 árboles): clic derecho para abrirlo.
- **El Ciego:** una criatura delgada, de piel gris y costillas marcadas, **sin ojos** (los tiene cosidos), con hocico largo de colmillos, patas de más y cola de rata. Anda a cuatro patas y algo parece un perro, pero no lo es. **No ve: te caza por el ruido** (te oye a 1,3 × tu nivel de ruido, y siempre a menos de 3 bloques). Va hacia el último ruido que oyó, corre si fue muy fuerte, muerde fuerte (6 de daño) si te alcanza y, si te quedas en silencio o agachado, pierde el rastro y se pone a olfatear. Aparece atraído por el ruido alto sostenido (unos 8 segundos) y, en el Nivel -1, de vez en cuando solo; uno por jugador y con pausas largas. Con él cerca sube la ansiedad y baja la cordura.
- **Vagonetas con cofre:** en los túneles del Nivel -1 hay, de vez en cuando, una vagoneta con cofre sobre un trocito de vía, con antorchas, carbón, pastillas Anx y con suerte Sancor.
- **Cuando la mente se rompe:**
  - Con la **ansiedad al 100 %** (pánico), cada segundo sube la probabilidad de un **ataque al corazón**: un golpe fuerte (nunca te mata, te deja con medio corazón como mínimo), ceguera, náuseas, lentitud y debilidad. Después la ansiedad baja a la mitad y la cordura cae 20. Se ajusta en `ataque_corazon_por_segundo` (0 lo desactiva).
  - Con **cordura baja (< 35)** ves **alucinaciones**: un Asechador falso a tu espalda que desaparece al mirarlo, pero te asusta igualmente (ansiedad +9, cordura −3). Si hay uno real cerca y tu cordura es baja (< 30), te afecta un 40 % más.
  - Con cordura muy baja (< 25) o ansiedad alta (≥ 75) a veces te mareas, se te va la vista o te tiemblan las manos y no puedes picar bien. Además te salen pensamientos sueltos ("Algo me observa.", "Quiero despertar."...).
- **Nivel -1:** si **bajas hasta el fondo del Overworld** (Y -60 por defecto) o te cavas hasta el suelo de bedrock del Nivel 0, caes a una **red de túneles de piedra**: estrechos y altos, que se cruzan, se ramifican, tienen callejones sin salida y alguna sala pequeña. No hay mobs normales y está a oscuras (sin niebla, solo oscuridad: lleva antorchas). Al llegar oyes tus pensamientos: "¿Dónde estoy?...". El Asechador sigue ahí, escondiéndose en los cruces y las paredes. La red llega a **140 bloques de radio**; cámbialo en `radio_cueva` (solo afecta a chunks nuevos). Para salir: duerme en una cama dentro (siempre es "de noche"), o `/pasillos volver`.
- **Ruido:** en los dos niveles hay un medidor de **Ruido** debajo de las barras. Caminar hace ruido medio, **correr mucho**, saltar y caer también, **romper un bloque** es lo más ruidoso y poner uno bastante; **agacharte** casi no hace ruido. El Asechador te oye a unos 0,9 × ruido bloques: se pone en alerta y se acerca (a 14-30 bloques en vez de 22-42), con ruido sostenido aparece antes y más cerca, y hacer ruido con él cerca te sube la ansiedad.
- **Niebla nocturna:** de noche el mundo se oscurece y la niebla se cierra a unos 30 bloques, se vuelve casi negra y avanza y retrocede. Las siluetas lejanas siguen viéndose dentro de ella. Se ajusta en `distancia_noche`.
- **El Asechador (Stalker):** una silueta negra y descarnada de unos 3,8 bloques. Cuello largo, cabeza ovalada ladeada, sin cuernos y con la espalda lisa (sin alas ni jirones), ojos blancos con pupila de rendija, mandíbula que cuelga entreabierta con colmillos, columna y costillas marcadas, brazos que cuelgan hasta las rodillas y terminan en tres dedos larguísimos de hueso. Echa ceniza y tinta negra. Nunca ataca.
  - Animaciones: respira, la cabeza se ladea de golpe en tics, la mandíbula se abre y se cierra, camina con zancada rígida, se agazapa y se asoma con una mano apoyada.
  - Lo hace aparecer el "director" de noche, a 36-44 bloques, como mucho uno por jugador, y siempre tras una pausa. Se disuelve al amanecer.
  - Se mantiene a **22-42 bloques** de ti, siempre con la cabeza girada hacia ti, y cambia de escondite cada 10-25 segundos con uno de tres estilos:
    1. **Detrás de cobertura** (árbol o pilar): cuando miras a otro lado se asoma por el lateral con una mano apoyada.
    2. **Agazapado**: se encoge y se mueve a cuatro patas, así cabe detrás de cosas más bajas.
    3. **Estatua**: inmóvil a campo abierto y lejos, como si fuera parte del paisaje.
  - Si **lo ves** (lo miras de frente, con línea de visión y a menos de 30 bloques de noche), se queda helado y a los 2 segundos **desaparece** en humo. También desaparece si te acercas a menos de 12 bloques o si le pegas. Pasados unos 20 a 60 segundos vuelve en otro sitio.
- **Volver:** dormir dentro de la dimensión (de noche, necesitas una cama) te devuelve al sitio donde te dormiste.

## Cómo compilarlo (GitHub, sin instalar nada)

1. Crea un repositorio en github.com y sube todo el contenido de esta carpeta, incluida la carpeta oculta `.github`. Si ya tenías una versión anterior, sube encima: los archivos con el mismo nombre se reemplazan.
2. Entra en **Actions** → "Compilar mod". Cuando salga el check verde, abre la ejecución y descarga **pasillos-jar** en *Artifacts*.
3. Copia el `.jar` a la carpeta `mods` de Minecraft con Forge 1.20.1.

Si prefieres compilar en tu PC: JDK 17, MDK de Forge 1.20.1, copia `build.gradle`, `gradle.properties`, `settings.gradle` y `src/` encima y ejecuta `gradlew build`.

## Comandos de prueba (requieren ser operador)

| Comando | Qué hace |
|---|---|
| `/pasillos entrar` | Te manda a la dimensión |
| `/pasillos profundo` | Te manda al Nivel -1 |
| `/pasillos volver` | Te saca de la dimensión |
| `/pasillos ciego` | Aparece un Ciego a 26-40 bloques |
| `/pasillos asechador` | Aparece un Asechador a unos 18 bloques |
| `/pasillos saco` | Coloca un saco delante de ti |
| `/pasillos noche` | Pone la noche (para ver la niebla y al Asechador) |
| `/pasillos dia` | Pone el día |

También hay un huevo generador del Asechador en la pestaña de huevos del modo creativo. El Asechador no aparece solo en dificultad Pacífico ni con `doMobSpawning` desactivado.

## Configuración

- `pasillos-common.toml`: `probabilidad_al_dormir`, `ticks_hasta_el_salto`, `radio_cueva`, `altura_overworld` y `altura_nivel_0`.
- `pasillos-client.toml`: `distancia_dia` y `distancia_noche` de la niebla, en bloques, y `ataque_corazon_por_segundo`.

## Estructura

- `worldgen/AncientChunkGenerator.java`: el terreno. `GRID` es la separación de los árboles y `PILLAR_EVERY` cada cuántas celdas hay un pilar.
- `entity/Asechador.java`: el mob, sus estados y partículas. `entity/HideAndWatchGoal.java`: su comportamiento y los estilos de escondite. `event/AsechadorDirector.java`: cuándo y dónde aparece.
- `client/AsechadorModel.java` y `AsechadorRenderer.java`: el modelo 3D, las animaciones y los ojos brillantes.
- `client/SanityState.java` y `SanityHandler.java`: cordura, ansiedad y sus efectos. `client/HudOverlays.java`: barras y oscuridad nocturna. `client/FogHandler.java`: la niebla.
- `item/`, `block/SackBlock.java`, `network/`: Sancor, Anx, el saco y el paquete que cambia la cordura desde el servidor. El botín del saco está en `data/pasillos/loot_tables/blocks/saco.json`.
- `worldgen/CaveShape.java` y `CaveChunkGenerator.java`: los túneles del Nivel -1. `event/NoiseSystem.java`: el ruido. `event/DepthHandler.java`: la caída. `event/PanicHandler.java` y `network/PanicMessage.java`: ataques al corazón y mareos.
- `entity/Ciego.java` y `ListenAndHuntGoal.java`: el perro ciego; `client/CiegoModel.java`: su modelo; `event/CiegoDirector.java`: cuándo aparece. `event/MinecartSpawner.java`: vagonetas con cofre.
- `DreamTravel.java` y `event/SleepHandler.java`: entrar y salir al dormir.
- `data/pasillos/...`: dimensión, tipo de dimensión, bioma y receta en JSON.

## Pendiente

- Más niveles.
- Sonidos propios (ahora usa sonidos de Minecraft).
- La cordura y la ansiedad son solo del lado del cliente y se reinician al entrar; no tienen efectos en el servidor.
- El id interno del mod sigue siendo `pasillos`; no afecta al juego.

## Nota

No pude compilarlo en mi entorno (no hay acceso al repositorio de Forge), así que la primera compilación puede pedir algún ajuste menor.
