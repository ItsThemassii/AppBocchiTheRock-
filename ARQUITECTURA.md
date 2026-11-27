# Arquitectura de Bocchi Fan

> Una guía para principiantes: qué es este proyecto, cómo está organizado
> y cómo se construyó paso a paso.

---

## 1. ¿Qué es este proyecto?

**Bocchi Fan** es una app para Android hecha por fans de *Bocchi the Rock!*. Tiene cuatro partes:

| Parte | Qué hace |
|---|---|
| **Jugar** | Un juego de ritmo tipo *Guitar Hero* / *BanG Dream*: las notas caen por 5 carriles en perspectiva y hay que tocarlas a tiempo con la música del álbum. |
| **Personajes** | Fichas de 12 personajes: edad, estatura, cumpleaños, seiyū, biografía y curiosidades. |
| **Canciones** | Un reproductor de música con la portada de cada canción y la letra (que el usuario pega), sincronizada si trae tiempos. |
| **Ajustes** | Idioma (español/inglés), tema claro/oscuro, música, sonidos y sincronía del juego. |

Además tiene música de fondo (el OST del anime), sonidos de guitarra al tocar los botones y animaciones con estrellitas, púas y notas musicales.

> ⚠️ Es un proyecto **personal y no oficial**. El repositorio **no incluye** la música, los GIFs ni las imágenes del anime (tienen derechos de autor). Cada persona pone sus propios archivos en la carpeta `contenido/`.

---

## 2. Las herramientas que usamos

Si es tu primer proyecto Android, estos son los nombres que vas a ver:

| Herramienta | Para qué sirve (en simple) |
|---|---|
| **Kotlin** | El lenguaje de programación. Es el idioma "oficial" de Android hoy. |
| **Jetpack Compose** | La forma moderna de dibujar pantallas en Android. En vez de diseñar con XML, escribes funciones (`@Composable`) que describen lo que se ve. Cuando cambia un dato, Compose vuelve a dibujar solo lo necesario. |
| **Gradle** | El "constructor": descarga librerías, compila el código y genera el APK (el instalador de la app). Se usa con `./gradlew`. |
| **Android SDK** | Las herramientas de Google para crear apps Android (compilador, emulador, `adb` para instalar en el teléfono…). |
| **Robolectric + Roborazzi** | Permiten hacer *capturas de pantalla automáticas* de la app en el ordenador, sin teléfono ni emulador. Así revisamos el diseño. |
| **JUnit** | Pruebas automáticas: pequeños programas que comprueban que el código hace lo correcto. |

No usamos ninguna librería de juegos: el juego está dibujado "a mano" con el `Canvas` de Compose.

---

## 3. Mapa de carpetas

```
Proyecto Bocchi The Rock!/
├── BocchiFan/                     ← el proyecto Android (código)
│   ├── app/
│   │   ├── build.gradle.kts       ← configuración de la app (versión, librerías…)
│   │   └── src/
│   │       ├── main/
│   │       │   ├── AndroidManifest.xml
│   │       │   ├── java/com/bocchifan/app/   ← TODO el código Kotlin (ver sección 4)
│   │       │   └── res/                       ← textos (español/inglés), ícono, sonidos
│   │       └── test/                          ← pruebas y capturas automáticas
│   ├── gradle/libs.versions.toml  ← lista de librerías y sus versiones
│   └── gradlew / gradlew.bat      ← para compilar desde la terminal
│
├── contenido/                     ← lo que el usuario pone (música, imágenes, datos)
│   ├── musica/                    ← OST de fondo (mp3)
│   ├── canciones/<Álbum>/         ← canciones del juego y del reproductor (mp3)
│   ├── imagenes/                  ← retratos, portada, GIFs de carga y de fondo…
│   └── datos/personajes.json      ← fichas de personajes (sí está en el repo)
│
├── herramientas/BocchiTools.java  ← genera los sonidos y recorta los "stickers"
├── docs/capturas/                 ← imágenes para el README
├── ARQUITECTURA.md                ← este documento
└── README.md
```

**Idea clave:** el código vive en `BocchiFan/` y el contenido en `contenido/`. Gradle mete la carpeta
`contenido/` dentro de la app como *assets* (archivos que la app puede leer). Por eso, para añadir una
canción o una imagen **no hay que tocar código**: se pone el archivo en su carpeta y se vuelve a compilar.

---

## 4. El código, paquete por paquete

Todo está en `BocchiFan/app/src/main/java/com/bocchifan/app/`:

```
com.bocchifan.app
├── MainActivity.kt        ← punto de entrada: prepara música, sonidos y tema; muestra BocchiApp
├── navigation/            ← qué pantalla se ve y cómo se vuelve atrás
├── ui/                    ← pantallas y piezas visuales
│   ├── theme/             ← colores, tipografía, tema claro/oscuro
│   ├── components/        ← piezas reutilizables (stickers, sonidos, efectos, GIFs…)
│   ├── title/             ← portada
│   ├── menu/              ← menú principal
│   ├── characters/        ← lista y ficha de personajes
│   ├── songs/             ← elegir canción para jugar
│   ├── player/            ← reproductor de música y letras
│   ├── settings/          ← ajustes
│   └── placeholder/       ← la caja de mango de Bocchi
├── data/                  ← leer datos: personajes, canciones, portadas, letras, récords
├── audio/                 ← música de fondo (OST)
└── game/                  ← el juego de ritmo
```

### 4.1 Cómo arranca la app

1. Android abre `MainActivity`.
2. `MainActivity` inicializa la música de fondo (`MusicPlayer`), los ajustes del juego, los sonidos y el tema.
3. Muestra `BocchiApp()`, que decide qué pantalla dibujar.
4. Encima de todo hay una capa invisible (`TapFxOverlay`) que dibuja las estrellitas cuando tocas algo.

### 4.2 Navegación (`navigation/BocchiApp.kt`)

Usamos una **pila de pantallas**, como una pila de cartas:

- Abrir una pantalla = poner una carta encima (`push`).
- Botón atrás = quitar la de arriba (`pop`).

Cada pantalla es un tipo dentro de `Screen` (`Menu`, `Characters`, `Game(canción, dificultad)`…). La pila se
guarda (`rememberSaveable`) para que no se pierda al girar el teléfono o cambiar de idioma. Las transiciones
(deslizar a los lados) las hace `AnimatedContent`.

### 4.3 El estilo visual (`ui/theme`, `ui/components`)

- **Cuaderno con stickers**: el fondo es una hoja cuadriculada (`notebookPaper`) y casi todo es una
  `StickerCard`: tarjeta con borde de tinta, sombra dura, un trozo de cinta adhesiva y un pequeño "pop" al tocarla.
- **Colores de la banda**: rosa (Bocchi), amarillo (Nijika), azul (Ryo), rojo (Kita).
- **Tema claro/oscuro**: el objeto `Palette` guarda qué tema eligió el usuario. Los colores del "papel"
  (`Paper`, `OnPaper`, `CardShadow`…) son *propiedades que leen ese estado*, así que al cambiar el tema
  toda la app se repinta sola.
- **Idiomas**: los textos están en `res/values/strings.xml` (español) y `res/values-en/strings.xml` (inglés).

### 4.4 Sonidos y animaciones al tocar (`ui/components/TapFeedback.kt`)

- `UiSounds`: reproduce sonidos cortos con `SoundPool` (rápido, sin retraso).
  - Botones normales → una nota de guitarra al azar de la **escala pentatónica de Mi menor** (suena "rock").
  - "Jugar" → un *power chord*; volver → una nota grave apagada.
- `TapFx`: guarda "ráfagas" (dónde y cuándo tocaste) y `TapFxOverlay` las dibuja: un anillo rosa y piezas
  (estrellitas, púas, corcheas) que salen disparadas y caen con gravedad.
- `bocchiClickable`: un `clickable` que además hace sonido + ráfaga. Lo usan todos los botones.

**Los sonidos no son grabaciones**: se generaron con `herramientas/BocchiTools.java` usando el algoritmo
**Karplus-Strong**, que imita una cuerda: se llena un pequeño buffer con ruido y se va promediando una y
otra vez; el ruido se "suaviza" y queda un tono que se apaga como una cuerda pulsada.

### 4.5 Música de fondo (`audio/MusicPlayer.kt`)

- Lee los mp3 de `contenido/musica/` y elige el siguiente **al azar con pesos** (por ejemplo, "I'm Not Sure"
  tiene el doble de probabilidad), sin repetir el mismo dos veces seguidas.
- Se pausa si la app se va a segundo plano y se calla mientras juegas o usas el reproductor.

### 4.6 Personajes (`data/Characters.kt`, `ui/characters/`)

1. `contenido/datos/personajes.json` tiene los datos, con textos en español (`es`) e inglés (`en`).
2. `CharacterRepository` lee el JSON (con `org.json`, que ya viene en Android) y crea objetos `Character`.
3. `CharactersScreen` muestra la lista; `CharacterDetailScreen`, la ficha.
4. El retrato sale de `contenido/imagenes/personajes/<id>.png`; si no existe, se dibuja la inicial.

---

## 5. El juego de ritmo (la parte más interesante)

El juego **no usa notas hechas a mano**: la app *escucha* la canción y crea las notas sola.

```
mp3 ──► SongAnalysis ──► OnsetAnalyzer ──► ChartBuilder ──► GameEngine ◄── toques del dedo
        (decodifica)     (detecta golpes)  (crea notas)     (juzga)
                                                              │
                     SongClock (¿en qué ms de la canción estamos?)
                                                              │
                                     Playfield + Hud (dibuja todo, 60 veces por segundo)
```

### 5.1 Escuchar la canción (`game/SongAnalysis.kt`)

El mp3 se decodifica con `MediaCodec` (el decodificador del propio Android) a una lista de números: la
forma de onda. Se pasa a mono y a 22 050 muestras por segundo (suficiente para detectar el ritmo).
El resultado se **guarda en el teléfono**: la primera vez tarda unos segundos (Bocchi se esconde en su caja
o sale un GIF de carga), las siguientes es instantáneo.

### 5.2 Detectar los golpes (`game/OnsetAnalyzer.kt`)

Usa una técnica llamada **flujo espectral**:

1. Corta el audio en trocitos de ~46 ms.
2. A cada trocito le aplica la **FFT** (transformada rápida de Fourier), que dice "cuánto hay de cada
   frecuencia" (graves, medios, agudos).
3. Compara cada trocito con el anterior: si de repente aparece mucha energía nueva → **¡golpe!** (una
   batería, un acorde…).
4. Un umbral que se adapta al volumen de cada parte evita marcar golpes falsos.

De cada golpe guarda: el momento (ms), su fuerza, si es grave o agudo (para elegir el carril) y cuánto se
mantiene el sonido (para las notas largas). Una prueba con golpes de tiempo conocido confirma que el error
medio es de ~0 ms.

### 5.3 Crear las notas (`game/ChartBuilder.kt`)

Convierte los golpes en un *chart* para cada dificultad:

| Dificultad | Separación mínima | Golpes que usa | Extras |
|---|---|---|---|
| Fácil | 520 ms | el 45 % más fuerte | — |
| Normal | 330 ms | 65 % | notas largas |
| Difícil | 200 ms | 85 % | notas largas + acordes |
| Experto | 125 ms | todos | más acordes |

El carril sale del "tono" del golpe (graves a la izquierda, agudos a la derecha), repartido para usar los 5
carriles por igual y sin saltos imposibles.

### 5.4 Las reglas (`game/GameEngine.kt`)

No dibuja nada: solo recibe "toqué el carril 2 en el ms 15 230" y decide:

| Juicio | Diferencia con la nota | Puntos |
|---|---|---|
| PERFECT | ±45 ms | 100 % |
| GREAT | ±90 ms | 75 % |
| GOOD | ±130 ms | 40 % (rompe el combo) |
| BAD | ±170 ms | 10 % |
| MISS | no tocada | 0 |

También lleva el combo, la vida (si llega a 0: "Live fallido") y la puntuación (máximo 1 000 000).
Al estar separado del dibujo, se puede **probar automáticamente** (`GameLogicTest`).

### 5.5 El reloj (`game/GameSession.kt` → `SongClock`)

Para que las notas coincidan con la música, el reloj del juego se basa en **la posición del reproductor
de audio**, no en un cronómetro aparte. Como el reproductor avanza "a saltitos", el reloj se suaviza para
que las notas se muevan fluidas. El ajuste de **sincronía** de Ajustes compensa el retraso de los audífonos
Bluetooth.

### 5.6 Dibujar en perspectiva (`game/Playfield.kt`)

Cada nota tiene una "profundidad" (1 = al fondo, 0 = en la línea donde se toca). La escala se calcula como
`1 / (1 + profundidad × k)`: así las notas lejanas son pequeñas y se juntan hacia un **punto de fuga**,
igual que las vías del tren en una foto. Los carriles son líneas rectas que van hacia ese punto.

Encima va el `Hud`: puntuación, vida, combo, el texto PERFECT/GREAT con un "pop", los **cut-ins** de la
banda cada 50 de combo y el festejo de **FULL COMBO / ALL PERFECT**. Detrás, un GIF de concierto al azar
por canción (`contenido/imagenes/escenario/`).

### 5.7 Al terminar

`ResultsOverlay` muestra el rango (SS, S, A, B, C, D), los juicios y si es récord.
`GameRecords` guarda el mejor resultado de cada canción y dificultad en el teléfono (`SharedPreferences`).

---

## 6. El reproductor y las letras (`ui/player/`, `data/SongMedia.kt`)

- **Portada**: se lee la imagen incrustada en el mp3 con `MediaMetadataRetriever`. Si trae franjas negras
  a los lados (miniaturas de video), se recortan solas.
- **Letras**: por derechos de autor no vienen incluidas. El botón **"Buscar letra"** abre el navegador con
  la búsqueda; el usuario copia la letra, toca **"Pegar"** y la guarda. Se guarda dentro de la app.
- Si la letra trae tiempos tipo **LRC** (`[00:12.34] texto`), se ilumina la línea que suena y se puede tocar
  una línea para saltar a ese momento.
- La barra de avance tiene una **mini Bocchi gusano** que se arrastra hacia la derecha mientras suena.

---

## 7. Pruebas

Desde `BocchiFan/`:

```bash
./gradlew testDebugUnitTest        # pruebas de lógica (juicios, puntuación, detector de golpes, letras)
./gradlew recordRoborazziDebug     # capturas de todas las pantallas en app/screenshots/
```

Las capturas automáticas fueron muy útiles: permitían ver el diseño de cada pantalla (también en inglés y en
tema oscuro) sin tener que instalar la app cada vez.

---

## 8. Cómo lo hicimos (historia del proyecto)

El proyecto se planificó primero y luego se construyó en **4 fases**:

1. **Fase 1 — Inicio**: proyecto Android con Kotlin + Compose, el estilo "cuaderno con stickers", la
   portada animada, el menú y la navegación. Textos en español e inglés.
2. **Fase 2 — Personajes**: los datos se verificaron en la *Bocchi the Rock! Wiki* (Fandom) y las
   biografías se escribieron con palabras propias. Retratos opcionales por personaje.
3. **Fase 3 — El juego**: el detector de golpes, el generador de notas por dificultad, las reglas, el
   dibujo en perspectiva (horizontal, 5 carriles), resultados y récords. Se ajustó con pruebas sobre una
   canción real (densidad de notas, reparto de carriles, notas largas).
4. **Fase 4 — Pulido**: música de fondo, pantalla de carga con GIFs, Bocchi "sticker" en el menú, sonidos
   de guitarra y ráfagas al tocar, reproductor con letras, tema oscuro, fondos de concierto, cut-ins y
   festejo de FULL COMBO.

### Problemas que encontramos (y cómo se resolvieron)

| Problema | Solución |
|---|---|
| Gradle no podía descargar librerías ("PKIX path building failed"): Java no confiaba en el certificado de la red del PC. | Usar el almacén de certificados de Windows (`systemProp.javax.net.ssl.trustStoreType=Windows-ROOT` en la configuración personal de Gradle). |
| La carpeta se llama "Proyecto Bocchi The Rock**!**" y Java confunde el `!` con un separador interno de los `.jar`, lo que rompía las pruebas. | `build.gradle.kts` envía los archivos de compilación a una carpeta sin `!` (`~/.bocchifan-build`) solo cuando la ruta tiene `!`. |
| Las notas se cargaban hacia la derecha (los platillos de batería son agudos). | El "tono" se convierte en percentil entre las notas elegidas: así los 5 carriles se usan por igual. |
| Las portadas de los mp3 traían franjas negras. | Recorte automático de bordes de color liso. |
| Las imágenes "cute" tenían fondo blanco. | `BocchiTools.java` quita el fondo (relleno desde los bordes) y añade borde blanco + contorno oscuro, estilo sticker. |

---

## 9. Glosario

- **APK**: el archivo instalador de una app Android.
- **Asset**: archivo (audio, imagen, JSON) que va dentro de la app y se lee mientras se ejecuta.
- **Composable**: función de Compose que dibuja una parte de la pantalla.
- **Estado (State)**: un dato que, al cambiar, hace que Compose vuelva a dibujar lo que lo usa.
- **Chart**: la lista de notas (tiempo + carril) de una canción.
- **Onset**: el inicio de un sonido (un golpe, una nota).
- **FFT**: operación matemática que descompone un sonido en sus frecuencias.
- **LRC**: formato de letra sincronizada: cada línea empieza con `[minutos:segundos]`.
- **Robolectric**: simula Android en el ordenador para hacer pruebas sin teléfono.

---

Hecho con ♥ por **Massi**.
