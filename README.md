# Mundo Olvidado (Forge 1.20.1)

Mod de terror para Minecraft Forge 1.20.1.

## Qué hace

- **Dormir te puede mandar a otra dimensión.** Al dormir en una cama del Overworld (por defecto 50 % de probabilidad, tras 3 segundos dormido) despiertas en un mundo que no es normal.
- **La dimensión** es un mundo perfectamente plano. Los robles están plantados en una cuadrícula de 7x7, todos idénticos, y unas líneas de tierra seca marcan esa cuadrícula. Cada 49 bloques, en vez de un árbol hay un pilar de cobblestone. No hay animales, flores ni agua.
- **Niebla:** de día una bruma azul pálida (70 bloques). Al caer la noche se cierra hasta unos 6 bloques, "respira" y se vuelve casi negra.
- **La linterna:** la recibes la primera vez que entras. Clic derecho para encenderla o apagarla. Encendida y en la mano, abre un claro en la niebla nocturna (34 bloques). Si hay un Asechador cerca, la luz parpadea.
  - Receta: panel de cristal, lingote de hierro y redstone, en columna (de arriba abajo).
- **El Asechador:** una silueta negra de unos 3,8 bloques con cabeza ovalada, brazos larguísimos en T y ojos blancos que brillan en la oscuridad. Nunca ataca.
  - Aparece de noche, como mucho uno por zona.
  - Se esconde detrás de los árboles a 14-30 bloques y gira la cabeza para mirarte. Si no tiene dónde esconderse, se aleja.
  - Si lo miras directamente se queda quieto. Si sigues mirándolo huye a otro escondite, y si lo mantienes a la vista unos 4 segundos o se acerca a menos de 8 bloques, se desvanece en humo. Si le pegas, también.
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
| `/pasillos noche` | Pone la noche (para ver la niebla y la linterna) |
| `/pasillos dia` | Pone el día |

También hay un huevo generador del Asechador (pestaña de huevos) y la linterna (pestaña de herramientas) en el modo creativo. El Asechador no aparece solo en dificultad Pacífico.

## Configuración

- `pasillos-common.toml`: `probabilidad_al_dormir` y `ticks_hasta_el_salto`.
- `pasillos-client.toml`: `distancia_dia`, `distancia_noche` y `distancia_linterna` de la niebla, en bloques.

## Estructura

- `worldgen/AncientChunkGenerator.java`: el terreno. `GRID` es la separación de los árboles y `PILLAR_EVERY` cada cuántas celdas hay un pilar.
- `entity/Asechador.java` y `entity/HideAndWatchGoal.java`: el mob y su comportamiento.
- `client/AsechadorModel.java` y `AsechadorRenderer.java`: el modelo 3D y los ojos brillantes. Las texturas están en `assets/pasillos/textures/entity/`.
- `item/FlashlightItem.java`: la linterna. El efecto en la niebla está en `client/FogHandler.java`.
- `DreamTravel.java` y `event/SleepHandler.java`: entrar y salir al dormir.
- `data/pasillos/...`: dimensión, tipo de dimensión, bioma y receta en JSON.

## Pendiente

- Más niveles.
- Sonidos propios (ahora usa el ambiente de cuevas).
- El id interno del mod sigue siendo `pasillos`; no afecta al juego.

## Nota

No pude compilarlo en mi entorno (no hay acceso al repositorio de Forge), así que la primera compilación puede pedir algún ajuste menor.
