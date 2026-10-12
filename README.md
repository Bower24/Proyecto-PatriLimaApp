# Patrimonio Lima - Monitoreo Ciudadano y Gestión del Patrimonio Cultural

Aplicación móvil nativa en **Android (Jetpack Compose)** orientada a la identificación, reporte y priorización de zonas críticas de deterioro patrimonial en el Centro Histórico de Lima (Cercado de Lima). El sistema implementa una arquitectura desacoplada y un modelo **100% Offline-First** para la cartografía vectorial, eliminando costos recurrentes de API.

---

## 🏛️ Características Principales

- **RF01 – Autenticación Ciudadana:** Inicio de sesión y registro mediante Firebase Authentication.
- **RF02 – Reporte Ciudadano con Evidencia:** Formulario interactivo con selección de nivel de severidad (*Leve*, *Moderado*, *Severo*), dirección descriptiva y captura de fotografía desde la galería del dispositivo convertida a Base64 optimizada.
- **RF03 / RF08 – Mapa del Patrimonio Interactivo:** Visualización cartográfica offline mediante el motor vectorial **Mapsforge**, centrado en el Centro Histórico de Lima, con filtrado dinámico por categorías.
- **RF04 – Historial y Seguimiento:** Consulta del estado cronológico de los reportes enviados por el usuario (*Enviado*, *En revisión*, *Atendido*).
- **Algoritmo A1 – Identificación y Priorización de Zonas Críticas:**
  - Agrupación espacial por proximidad de Haversine dentro de un radio de $r = 100\text{ m}$.
  - Cálculo de índice de criticidad acumulado según severidad:
    $$\text{Criticidad} = \sum_{i=1}^{n} w_i \quad \text{donde } w \in \{1: \text{Leve},\, 2: \text{Moderado},\, 3: \text{Severo}\}$$
  - Delimitación visual de polígonos/círculos de afectación sobre el mapa.

---

## 🛠️ Stack Tecnológico

- **Lenguaje:** Kotlin 2.x
- **Framework UI:** Jetpack Compose (Material Design 3)
- **Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Persistencia y Backend:**
  - Firebase Authentication (Gestión de identidades)
  - Cloud Firestore (Base de datos NoSQL para reportes y catálogo patrimonial)
- **Motor Cartográfico:** Mapsforge 0.20+ (Renderizado vectorial offline a partir de extracto binario `.map`)
- **Procesamiento Gráfico:** Android Jetpack Activity Result Contracts (`PickVisualMedia`) + Compresión de Bitmap a Base64.

---

## 📂 Estructura del Proyecto

```text
app/src/main/
├── assets/
│   └── lima.map                     # Cartografía vectorial recortada de Lima Centro (~10MB)
├── java/com/patrimonialima/app/
│   ├── data/                        # Repositorios e integraciones con Firebase
│   ├── domain/
│   │   ├── model/                   # Modelos de dominio (Reporte, BienPatrimonial, Severidad)
│   │   └── AlgoritmoA1.kt           # Lógica de clustering espacial y criticidad
│   ├── ui/
│   │   ├── screens/                 # Pantallas Jetpack Compose (Auth, Reportes, Mapa, Historial)
│   │   └── theme/                   # Paleta de colores institucionales y tipografías
│   └── MainActivity.kt
└── res/
```

---

## 🚀 Requisitos e Instalación

### Requisitos previos
- Android Studio Ladybug / Koala o superior.
- JDK 17 o superior.
- Dispositivo físico o emulador Android con API 26 (Android 8.0) en adelante.

### Pasos para ejecutar
1. Clonar este repositorio:
   ```bash
   git clone https://github.com/tu-usuario/patrimonio-lima.git
   ```
2. Abrir el proyecto en Android Studio.
3. Asegurarse de que el archivo `lima.map` se encuentre en `app/src/main/assets/lima.map`.
4. Incluir el archivo `google-services.json` correspondiente en la carpeta `app/`.
5. Sincronizar Gradle y ejecutar la aplicación en el dispositivo seleccionado.