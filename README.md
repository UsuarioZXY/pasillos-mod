# Mundo Olvidado (Forge 1.20.1)

Mod de terror para Minecraft Forge 1.20.1.

## Qué hace

- **Dormir te puede mandar a otra dimensión.** Al dormir en una cama del Overworld (por defecto 50 % de probabilidad, tras 3 segundos dormido) despiertas en un mundo antiguo.
- **La dimensión** parece un Minecraft de 2010: colinas suaves de hierba, lagos, arena en las orillas, robles sueltos, carbón bajo tierra. Está vacía: sin animales, sin flores, sin aldeas. De vez en cuando hay un pilar de cobblestone de 3 bloques en medio de la nada.
- **Niebla:** de día una bruma azul pálida (70 bloques de visibilidad). Al caer la noche se cierra hasta unos 6 bloques, "respira" y se vuelve casi negra.
- **El Asechador:** una silueta oscura con ojos blancos que nunca ataca.
  - Aparece de noche, como mucho uno por zona.
  - Se esconde detrás de árboles y colinas a 12-26 bloques y gira la cabeza para mirarte.
  - Si lo miras directamente se queda quieto. Si sigues mirándolo huye a otro escondite, y si lo mantienes a la vista unos 4 segundos o se acerca a menos de 6 bloques, se desvanece en humo. Si le pegas, también.
- **Volver:** dormir dentro de la dimensión (de noche, necesitas una cama) te devuelve al sitio donde te dormiste.

## Cómo compilarlo (GitHub, sin instalar nada)

1. Crea un repositorio en github.com.
2. Sube todo el contenido de esta carpeta, incluida la carpeta oculta `.github`.
3. Entra en **Actions** → "Compilar mod". Cuando salga el check verde, abre la ejecución y descarga **pasillos-jar** en *Artifacts*.
4. Copia el `.jar` a la carpeta `mods` de Minecraft con Forge 1.20.1.

Si prefieres compilar en tu PC: JDK 17, MDK de Forge 1.20.1, copia `build.gradle`, `gradle.properties`, `settings.gradle` y `src/` encima y ejecuta `gradlew build`.

## Comandos de prueba (requieren ser operador)

| Comando | Qué hace |
|---|---|
| `/pasillos entrar` | Te manda a la dimensión |
| `/pasillos volver` | Te saca de la dimensión |
| `/pasillos asechador` | Aparece un Asechador a unos 18 bloques |
| `/pasillos noche` | Pone la noche (para ver la niebla cerrada) |
| `/pasillos dia` | Pone el día |

También hay un huevo generador del Asechador en la pestaña de huevos del modo creativo. El Asechador no aparece solo en dificultad Pacífico.

## Configuración

- `pasillos-common.toml`: `probabilidad_al_dormir` y `ticks_hasta_el_salto`.
- `pasillos-client.toml`: `distancia_dia` y `distancia_noche` de la niebla, en bloques.

## Estructura

- `worldgen/AncientChunkGenerator.java`: el terreno. Las constantes de arriba (altura base, nivel del mar, densidad de árboles) se ajustan ahí.
- `entity/Asechador.java` y `entity/HideAndWatchGoal.java`: el mob y su comportamiento.
- `DreamTravel.java` y `event/SleepHandler.java`: entrar y salir al dormir.
- `client/FogHandler.java`: la niebla.
- `data/pasillos/...`: dimensión, tipo de dimensión y bioma en JSON. Para un nivel nuevo, añade sus JSON y una clave en `ModDimensions`.

## Pendiente

- Más niveles.
- Sonidos propios (ahora usa el ambiente de cuevas).
- El id interno del mod sigue siendo `pasillos`; no afecta al juego.

## Nota

No pude compilarlo en mi entorno (no hay acceso al repositorio de Forge), así que la primera compilación puede pedir algún ajuste menor.
