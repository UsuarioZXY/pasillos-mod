# Pasillos Liminales (Forge 1.20.1)

Mod de terror para Minecraft Forge 1.20.1. Esta primera versión incluye **solo la dimensión**:

- **Dormir te puede mandar al Nivel 0.** Al dormir en una cama del Overworld (por defecto 50 % de probabilidad, tras 3 segundos dormido) despiertas en otra dimensión.
- **Nivel 0:** laberinto infinito de habitaciones amarillas, moqueta húmeda, paneles fluorescentes y salones enormes con pilares. Sin mobs, sin cielo.
- **Detalles que no cuadran:** habitaciones con la luz apagada, paredes manchadas, un bloque suelto de dos de alto en mitad de una sala, agujeros en el techo que dejan ver la bedrock.
- **Niebla:** de día una bruma amarillenta (40 bloques de visibilidad). Al caer la noche se cierra hasta unos 6 bloques, "respira" y se vuelve casi negra.
- **Volver:** dormir dentro de los Pasillos (de noche, necesitas una cama) te devuelve al sitio donde te dormiste.

## Cómo compilarlo

Necesitas JDK 17.

1. Descarga el **MDK de Forge 1.20.1** (47.3.0 o superior) desde https://files.minecraftforge.net/ y descomprímelo.
2. Copia dentro del MDK (sobrescribiendo): `build.gradle`, `gradle.properties`, `settings.gradle` y la carpeta `src/`. Borra la carpeta `src/main/java/com/example` si existe.
3. Ejecuta `./gradlew build` (Windows: `gradlew.bat build`).
4. El mod queda en `build/libs/pasillos-0.1.0.jar`. Ponlo en la carpeta `mods` de Forge 1.20.1.

Para probarlo desde el IDE: `./gradlew runClient`.

## Comandos de prueba (requieren ser operador)

| Comando | Qué hace |
|---|---|
| `/pasillos entrar` | Te manda al Nivel 0 |
| `/pasillos volver` | Te saca del Nivel 0 |
| `/pasillos noche` | Pone la noche en el Overworld (para ver la niebla cerrada) |
| `/pasillos dia` | Pone el día |

## Configuración

- `pasillos-common.toml`: `probabilidad_al_dormir` (0.0 a 1.0) y `ticks_hasta_el_salto`.
- `pasillos-client.toml`: `distancia_dia` y `distancia_noche` de la niebla, en bloques.

## Estructura (pensada para ampliarse)

- `worldgen/LiminalChunkGenerator.java`: genera el laberinto. El parámetro `level` del JSON de la dimensión cambia la semilla del diseño; así cada nivel nuevo puede reutilizar o extender el generador.
- `data/pasillos/dimension/nivel_0.json`, `dimension_type/`, `worldgen/biome/`: la dimensión. Para un Nivel 1 añade sus propios JSON y una clave en `ModDimensions`.
- `DreamTravel.java`: entrar y salir. `event/SleepHandler.java`: el disparador al dormir.
- `client/FogHandler.java`: la niebla.

## Pendiente

- El mob **Asechador** (te observa escondido, solo mira).
- Más niveles.
- Sonido ambiente propio (ahora usa el sonido de cuevas del bioma).

## Notas

- El mapa es el mismo en todos los mundos (no depende de la semilla).
- Las paredes se rompen a mano en pocos golpes, por si una habitación queda cerrada (pasa en menos del 1 % de las celdas).
- No pude compilarlo en mi entorno (no hay acceso al repositorio de Forge), así que la primera compilación puede pedir algún ajuste menor de nombres de clase.
