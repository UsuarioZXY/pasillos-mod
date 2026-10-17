# Mundo Olvidado (Forge 1.20.1)

Mod de terror para Minecraft Forge 1.20.1.

## Qué hace

- **Dormir te puede mandar a otra dimensión.** Al dormir en una cama del Overworld (por defecto 50 % de probabilidad, tras 3 segundos dormido) despiertas en un mundo que no es normal.
- **La dimensión** es un mundo perfectamente plano. Los robles están plantados en una cuadrícula de 7x7, todos idénticos, y unas líneas de tierra seca marcan esa cuadrícula. Cada 49 bloques, en vez de un árbol hay un pilar de cobblestone. No hay animales, flores ni agua.
- **Barras de Cordura y Ansiedad** (arriba a la izquierda, solo dentro de la dimensión; empiezan al entrar):
  - La **cordura** baja con la oscuridad de la noche, con el Asechador cerca y, mucho más, al verlo. Se recupera de día, o de noche si estás junto a una luz de bloque (antorcha, farol, glowstone...: nivel de luz 8 o más) y sin Asechador cerca.
  - La **ansiedad** sube con la noche (menos si estás a la luz), el Asechador cerca y la poca cordura; baja despacio. Por encima de 45 oyes latidos cada vez más rápidos y aparece un borde rojo que pulsa; por encima de 40 la cámara tiembla. Con la cordura por debajo de 45 se oyen sonidos que no existen.
- **Niebla nocturna:** de noche el mundo se oscurece y la niebla se cierra a unos 30 bloques, se vuelve casi negra y avanza y retrocede. Las siluetas lejanas siguen viéndose dentro de ella. Se ajusta en `distancia_noche`.
- **El Asechador (Stalker):** una silueta negra y descarnada de unos 3,8 bloques. Cuello largo, cabeza ovalada ladeada con dos cuernos finos, ojos blancos con pupila de rendija, mandíbula que cuelga entreabierta con colmillos, columna y costillas marcadas, brazos que cuelgan hasta las rodillas y terminan en tres dedos larguísimos de hueso. Echa ceniza y tinta negra. Nunca ataca.
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
| `/pasillos volver` | Te saca de la dimensión |
| `/pasillos asechador` | Aparece un Asechador a unos 18 bloques |
| `/pasillos noche` | Pone la noche (para ver la niebla y al Asechador) |
| `/pasillos dia` | Pone el día |

También hay un huevo generador del Asechador en la pestaña de huevos del modo creativo. El Asechador no aparece solo en dificultad Pacífico ni con `doMobSpawning` desactivado.

## Configuración

- `pasillos-common.toml`: `probabilidad_al_dormir` y `ticks_hasta_el_salto`.
- `pasillos-client.toml`: `distancia_dia` y `distancia_noche` de la niebla, en bloques.

## Estructura

- `worldgen/AncientChunkGenerator.java`: el terreno. `GRID` es la separación de los árboles y `PILLAR_EVERY` cada cuántas celdas hay un pilar.
- `entity/Asechador.java`: el mob, sus estados y partículas. `entity/HideAndWatchGoal.java`: su comportamiento y los estilos de escondite. `event/AsechadorDirector.java`: cuándo y dónde aparece.
- `client/AsechadorModel.java` y `AsechadorRenderer.java`: el modelo 3D, las animaciones y los ojos brillantes.
- `client/SanityState.java` y `SanityHandler.java`: cordura, ansiedad y sus efectos. `client/HudOverlays.java`: barras y oscuridad nocturna. `client/FogHandler.java`: la niebla.
- `DreamTravel.java` y `event/SleepHandler.java`: entrar y salir al dormir.
- `data/pasillos/...`: dimensión, tipo de dimensión, bioma y receta en JSON.

## Pendiente

- Más niveles.
- Sonidos propios (ahora usa sonidos de Minecraft).
- La cordura y la ansiedad son solo del lado del cliente y se reinician al entrar; no tienen efectos en el servidor.
- El id interno del mod sigue siendo `pasillos`; no afecta al juego.

## Nota

No pude compilarlo en mi entorno (no hay acceso al repositorio de Forge), así que la primera compilación puede pedir algún ajuste menor.
