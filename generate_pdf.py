#!/usr/bin/env python3
"""
Generador de PDF para la documentación de Black Hole Glow
"""

from fpdf import FPDF
import re

class ArchitecturePDF(FPDF):
    def __init__(self):
        super().__init__()
        self.set_auto_page_break(auto=True, margin=15)

    def header(self):
        self.set_font('Helvetica', 'B', 10)
        self.set_text_color(100, 100, 100)
        self.cell(0, 10, 'Black Hole Glow - Arquitectura del Proyecto', align='C')
        self.ln(5)
        self.set_draw_color(200, 200, 200)
        self.line(10, 18, 200, 18)
        self.ln(10)

    def footer(self):
        self.set_y(-15)
        self.set_font('Helvetica', 'I', 8)
        self.set_text_color(128, 128, 128)
        self.cell(0, 10, f'Pagina {self.page_no()}', align='C')

    def chapter_title(self, title):
        self.set_font('Helvetica', 'B', 16)
        self.set_text_color(30, 60, 120)
        self.cell(0, 10, title, ln=True)
        self.ln(4)

    def section_title(self, title):
        self.set_font('Helvetica', 'B', 13)
        self.set_text_color(50, 80, 140)
        self.cell(0, 8, title, ln=True)
        self.ln(2)

    def subsection_title(self, title):
        self.set_font('Helvetica', 'B', 11)
        self.set_text_color(70, 100, 160)
        self.cell(0, 7, title, ln=True)
        self.ln(1)

    def body_text(self, text):
        self.set_font('Helvetica', '', 10)
        self.set_text_color(0, 0, 0)
        self.multi_cell(0, 5, text)
        self.ln(2)

    def code_block(self, code):
        self.set_font('Courier', '', 8)
        self.set_fill_color(245, 245, 245)
        self.set_text_color(50, 50, 50)
        # Dividir en lineas y mostrar
        lines = code.strip().split('\n')
        for line in lines:
            # Truncar lineas muy largas
            if len(line) > 95:
                line = line[:92] + '...'
            self.cell(0, 4, line, ln=True, fill=True)
        self.ln(3)

    def table_row(self, cols, is_header=False):
        if is_header:
            self.set_font('Helvetica', 'B', 9)
            self.set_fill_color(220, 230, 240)
        else:
            self.set_font('Helvetica', '', 9)
            self.set_fill_color(255, 255, 255)

        self.set_text_color(0, 0, 0)

        # Calcular anchos basado en numero de columnas
        page_width = 190
        col_width = page_width / len(cols)

        for col in cols:
            # Truncar texto muy largo
            text = str(col)[:40] if len(str(col)) > 40 else str(col)
            self.cell(col_width, 6, text, border=1, fill=True)
        self.ln()

    def bullet_point(self, text):
        self.set_font('Helvetica', '', 10)
        self.set_text_color(0, 0, 0)
        self.cell(5, 5, chr(149))  # Bullet character
        self.multi_cell(0, 5, text)

def create_pdf():
    pdf = ArchitecturePDF()
    pdf.add_page()

    # ===========================================
    # PORTADA
    # ===========================================
    pdf.set_font('Helvetica', 'B', 28)
    pdf.set_text_color(30, 60, 120)
    pdf.ln(40)
    pdf.cell(0, 15, 'BLACK HOLE GLOW', align='C', ln=True)

    pdf.set_font('Helvetica', 'B', 18)
    pdf.set_text_color(80, 80, 80)
    pdf.cell(0, 10, 'Arquitectura del Proyecto', align='C', ln=True)

    pdf.ln(20)
    pdf.set_font('Helvetica', '', 12)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(0, 8, 'Version 4.0.1', align='C', ln=True)
    pdf.cell(0, 8, 'Diciembre 2024', align='C', ln=True)
    pdf.cell(0, 8, 'Paquete: com.secret.blackholeglow', align='C', ln=True)
    pdf.cell(0, 8, 'Plataforma: Android (Java)', align='C', ln=True)
    pdf.cell(0, 8, 'OpenGL: ES 2.0 / 3.0', align='C', ln=True)

    pdf.ln(30)
    pdf.set_font('Helvetica', 'I', 10)
    pdf.cell(0, 8, '176 clases | 16 paquetes | 3 escenas activas', align='C', ln=True)

    # ===========================================
    # TABLA DE CONTENIDOS
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('Tabla de Contenidos')

    toc = [
        '1. Resumen Ejecutivo',
        '2. Estructura de Carpetas',
        '3. Arquitectura General',
        '4. Diagramas UML',
        '5. Catalogo de Clases',
        '   5.1 Activities',
        '   5.2 Core - Pipeline de Renderizado',
        '   5.3 Scenes - Escenas',
        '   5.4 OpenGL Base',
        '   5.5 Objetos 3D - Espacio',
        '   5.6 Objetos 3D - Navidad',
        '   5.7 Sistema de Combate',
        '   5.8 Sistema de Musica',
        '   5.9 UI y HUD',
        '   5.10 Systems - Gestores Globales',
        '6. Clases Obsoletas',
        '7. Flujo de Datos',
        '8. Guia para IAs',
    ]

    for item in toc:
        pdf.body_text(item)

    # ===========================================
    # 1. RESUMEN EJECUTIVO
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('1. Resumen Ejecutivo')

    pdf.section_title('Que es Black Hole Glow?')
    pdf.body_text('Black Hole Glow es un Live Wallpaper para Android que renderiza escenas 3D animadas usando OpenGL ES. El usuario puede seleccionar diferentes escenas tematicas que se muestran como fondo de pantalla animado.')

    pdf.section_title('Caracteristicas Principales')
    pdf.table_row(['Feature', 'Descripcion'], is_header=True)
    pdf.table_row(['Escenas 3D', 'Renderizado OpenGL ES 2.0/3.0'])
    pdf.table_row(['Musica Reactiva', 'Visualizacion de audio en tiempo real'])
    pdf.table_row(['Sistema de Combate', 'Meteoritos, naves, escudos'])
    pdf.table_row(['Nieve Interactiva', 'Particulas GPU (escena Navidena)'])
    pdf.table_row(['IA Gemini', 'Saludos personalizados'])
    pdf.table_row(['Firebase', 'Auth, stats, remote config'])

    pdf.ln(5)
    pdf.section_title('Escenas Disponibles (Diciembre 2024)')
    pdf.table_row(['Escena', 'Estado', 'Clase'], is_header=True)
    pdf.table_row(['Batalla Cosmica', 'Activa', 'BatallaCosmicaScene'])
    pdf.table_row(['Bosque Navideno', 'Activa', 'ChristmasScene'])
    pdf.table_row(['Ocean Pearl', 'Coming Soon', 'OceanPearlScene'])
    pdf.table_row(['La Mansion', 'Coming Soon', 'No implementada'])

    # ===========================================
    # 2. ESTRUCTURA DE CARPETAS
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('2. Estructura de Carpetas')

    folder_structure = """blackholeglow/
  app/src/main/
    java/com/secret/blackholeglow/
      activities/     - Activities de Android
      adapters/       - Adaptadores RecyclerView
      christmas/      - Componentes escena navidena
      core/           - Pipeline de renderizado
      effects/        - Efectos post-proceso
      fragments/      - Fragments UI
      gl3/            - Utilidades OpenGL 3.0
      models/         - Modelos de datos
      opengl/         - Componentes OpenGL
      scenes/         - Escenas de wallpaper
      sharing/        - Sistema compartir musica
      systems/        - Sistemas globales
      ui/             - Componentes UI custom
      util/           - Utilidades (OBJ loader)
      wallpaper/      - Gestion de wallpapers
      *.java          - Clases paquete raiz
    assets/
      shaders/        - Shaders GLSL
      *.obj           - Modelos 3D"""

    pdf.code_block(folder_structure)

    # ===========================================
    # 3. ARQUITECTURA GENERAL
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('3. Arquitectura General')

    pdf.section_title('Diagrama de Capas')

    arch_diagram = """
+----------------------------------------------------------+
|                    CAPA ANDROID                          |
|  Activities | Fragments | LiveWallpaperService           |
+----------------------------------------------------------+
                           |
+----------------------------------------------------------+
|                    CAPA CORE                             |
|  WallpaperDirector (GLSurfaceView.Renderer)              |
|  - RenderModeController                                  |
|  - PanelModeRenderer                                     |
|  - SceneFactory                                          |
|  - SongSharingController                                 |
|  - TouchRouter                                           |
+----------------------------------------------------------+
                           |
+----------------------------------------------------------+
|                    CAPA SCENES                           |
|  WallpaperScene (Abstracta)                              |
|    |-- BatallaCosmicaScene                               |
|    |-- ChristmasScene                                    |
|    |-- OceanPearlScene                                   |
+----------------------------------------------------------+
                           |
+----------------------------------------------------------+
|                    CAPA OPENGL                           |
|  BaseShaderProgram | CameraController | TextureManager   |
+----------------------------------------------------------+
                           |
+----------------------------------------------------------+
|                 CAPA SCENE OBJECTS                       |
|  Planeta, Meteorito, Spaceship, ChristmasTree, etc.      |
+----------------------------------------------------------+
"""
    pdf.code_block(arch_diagram)

    pdf.section_title('Patron Actor Model')
    pdf.body_text('El proyecto usa el patron Actor donde cada componente tiene una responsabilidad unica:')

    pdf.table_row(['Actor', 'Responsabilidad'], is_header=True)
    pdf.table_row(['WallpaperDirector', 'Orquesta todo, implementa Renderer'])
    pdf.table_row(['RenderModeController', 'Maquina de estados'])
    pdf.table_row(['PanelModeRenderer', 'UI del panel de control'])
    pdf.table_row(['SceneFactory', 'Crea y destruye escenas'])
    pdf.table_row(['SongSharingController', 'Musica y Gemini AI'])
    pdf.table_row(['TouchRouter', 'Distribuye eventos tactiles'])

    # ===========================================
    # 4. DIAGRAMAS UML
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('4. Diagramas UML')

    pdf.section_title('4.1 Diagrama de Estados - RenderModeController')

    state_diagram = """
              +-------------+
              |    START    |
              +------+------+
                     |
                     v
         +---------------------+
         |     PANEL_MODE      |
         | (Boton Play visible)|
         +----------+----------+
                    | startLoading()
                    v
         +---------------------+
         |    LOADING_MODE     |
         | (Barra de progreso) |
         +----------+----------+
                    | activateWallpaper()
                    v
         +---------------------+
    +--->|   WALLPAPER_MODE    |<---+
    |    |  (Escena 3D activa) |    |
    |    +----------+----------+    |
    |               |               |
    | stopWallpaper()        goDirectToWallpaper()
    |               |          (Preview Mode)
    |               v               |
    |    +---------------------+    |
    +----+     PANEL_MODE      +----+
         +---------------------+
"""
    pdf.code_block(state_diagram)

    pdf.section_title('4.2 Diagrama de Clases - Core')

    class_diagram = """
+--------------------------------------------------+
|          <<interface>>                           |
|       GLSurfaceView.Renderer                     |
| + onSurfaceCreated(GL10, EGLConfig)              |
| + onSurfaceChanged(GL10, int, int)               |
| + onDrawFrame(GL10)                              |
+--------------------------------------------------+
                        ^
                        | implements
+--------------------------------------------------+
|              WallpaperDirector                   |
|--------------------------------------------------|
| - modeController: RenderModeController           |
| - panelRenderer: PanelModeRenderer               |
| - sceneFactory: SceneFactory                     |
| - camera: CameraController                       |
| - textureManager: TextureManager                 |
|--------------------------------------------------|
| + onSurfaceCreated()                             |
| + onDrawFrame()                                  |
| + onTouchEvent(MotionEvent): boolean             |
| + changeScene(String)                            |
+--------------------------------------------------+
"""
    pdf.code_block(class_diagram)

    pdf.add_page()
    pdf.section_title('4.3 Diagrama de Clases - Scenes')

    scene_diagram = """
+--------------------------------------------------+
|              <<abstract>>                        |
|             WallpaperScene                       |
|--------------------------------------------------|
| # context: Context                               |
| # textureManager: TextureManager                 |
| # camera: CameraController                       |
| # sceneObjects: List<SceneObject>                |
|--------------------------------------------------|
| + {abstract} getName(): String                   |
| + {abstract} getDescription(): String            |
| # {abstract} setupScene()                        |
| + onCreate(Context, TextureManager, Camera)      |
| + update(float deltaTime)                        |
| + draw()                                         |
+--------------------------------------------------+
          ^               ^               ^
          |               |               |
+-----------------+ +-----------------+ +-----------------+
|BatallaCosmicaS. | | ChristmasScene  | | OceanPearlScene |
|-----------------| |-----------------| |-----------------|
|- tierra         | |- christmasTree  | |- ocean          |
|- meteorShower   | |- snowParticles  | |- pearl          |
|- spaceship3D    | |- snowGround     | |- fish           |
+-----------------+ +-----------------+ +-----------------+
"""
    pdf.code_block(scene_diagram)

    # ===========================================
    # 5. CATALOGO DE CLASES
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('5. Catalogo de Clases')

    pdf.section_title('5.1 Activities (5 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['SplashActivity', 'Pantalla inicio con logo animado'])
    pdf.table_row(['MainActivity', 'Pantalla principal con NavigationDrawer'])
    pdf.table_row(['WallpaperPreviewActivity', 'Preview antes de aplicar wallpaper'])
    pdf.table_row(['WallpaperLoadingActivity', 'Pantalla de carga'])
    pdf.table_row(['GeminiChatActivity', 'Chat con Gemini AI'])

    pdf.ln(5)
    pdf.section_title('5.2 Core - Pipeline de Renderizado (7 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['WallpaperDirector', 'Director principal - GLSurfaceView.Renderer'])
    pdf.table_row(['RenderModeController', 'Maquina de estados (PANEL/LOADING/WALLPAPER)'])
    pdf.table_row(['PanelModeRenderer', 'Renderiza UI del panel'])
    pdf.table_row(['SceneFactory', 'Factory para crear/destruir escenas'])
    pdf.table_row(['SongSharingController', 'Gestion musica y Gemini AI'])
    pdf.table_row(['TouchRouter', 'Distribuye eventos tactiles'])
    pdf.table_row(['ResourcePreloader', 'Precarga recursos en background'])

    pdf.add_page()
    pdf.section_title('5.3 Scenes - Escenas (8 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['WallpaperScene', 'Clase base abstracta para escenas'])
    pdf.table_row(['BatallaCosmicaScene', 'Batalla espacial con meteoritos'])
    pdf.table_row(['ChristmasScene', 'Bosque navideno con nieve'])
    pdf.table_row(['OceanPearlScene', 'Escena submarina (Coming Soon)'])
    pdf.table_row(['SceneConstants', 'Constantes configurables por escena'])
    pdf.table_row(['SceneManager', 'Gestiona cambios entre escenas'])
    pdf.table_row(['SceneCallbacks', 'Interface de callbacks'])
    pdf.table_row(['Disposable', 'Interface para liberar recursos'])

    pdf.ln(5)
    pdf.section_title('5.4 OpenGL Base')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['BaseShaderProgram', 'Clase base para shaders GLSL'])
    pdf.table_row(['CameraController', 'Sistema camara multi-modo'])
    pdf.table_row(['TextureManager', 'Cache lazy-loading de texturas'])
    pdf.table_row(['ShaderUtils', 'Utilidades compilar shaders'])
    pdf.table_row(['SceneObject', 'Interface - update() y draw()'])
    pdf.table_row(['CameraAware', 'Interface - objetos que necesitan camara'])

    pdf.add_page()
    pdf.section_title('5.5 Objetos 3D - Espacio (~30 clases)')

    pdf.subsection_title('Planetas y Cuerpos Celestes')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['Planeta', 'Planeta generico con orbita y textura'])
    pdf.table_row(['TierraLiveHD', 'Tierra con texturas HD'])
    pdf.table_row(['TierraMeshy', 'Tierra modelo Meshy AI'])
    pdf.table_row(['SolMeshy', 'Sol con modelo 3D'])
    pdf.table_row(['SolRealista', 'Sol procedural con corona'])
    pdf.table_row(['SaturnoMeshy', 'Saturno con anillos'])

    pdf.ln(3)
    pdf.subsection_title('Meteoritos y Particulas')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['Meteorito', 'Meteorito individual'])
    pdf.table_row(['MeteorShower', 'Sistema lluvia de meteoros'])
    pdf.table_row(['MeteorTrail', 'Estela de fuego'])
    pdf.table_row(['MeteorExplosion', 'Explosion al impactar'])
    pdf.table_row(['SpaceComets', 'Cometas espaciales'])
    pdf.table_row(['SpaceDust', 'Polvo cosmico'])

    pdf.ln(3)
    pdf.subsection_title('Naves Espaciales')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['Spaceship', 'Nave espacial base'])
    pdf.table_row(['Spaceship3D', 'Nave 3D con modelo OBJ'])
    pdf.table_row(['UfoAttacker', 'OVNI atacante con IA'])
    pdf.table_row(['UfoScout', 'OVNI explorador'])
    pdf.table_row(['DefenderShip', 'Nave defensora humana'])
    pdf.table_row(['SpaceStation', 'Estacion espacial'])

    pdf.add_page()
    pdf.section_title('5.6 Objetos 3D - Navidad (4 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['ChristmasTree', 'Arbol 3D con modelo OBJ, animacion viento'])
    pdf.table_row(['ChristmasBackground', 'Fondo bosque nevado'])
    pdf.table_row(['SnowGround', 'Suelo con textura nieve'])
    pdf.table_row(['SnowParticles', 'Sistema particulas nieve GPU'])

    pdf.ln(5)
    pdf.section_title('5.7 Sistema de Combate (~15 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['CollisionSystem', 'Deteccion de colisiones'])
    pdf.table_row(['EarthShield', 'Escudo protector Tierra'])
    pdf.table_row(['Laser', 'Proyectil laser'])
    pdf.table_row(['PlasmaBeamWeapon', 'Arma rayo plasma'])
    pdf.table_row(['Projectile', 'Proyectil generico'])
    pdf.table_row(['ProjectilePool', 'Object pool (optimizacion)'])
    pdf.table_row(['TargetingSystem', 'Sistema apuntado automatico'])
    pdf.table_row(['EnemyAI', 'IA de enemigos'])
    pdf.table_row(['BattleHUD', 'HUD de batalla'])

    pdf.add_page()
    pdf.section_title('5.8 Sistema de Musica (~10 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['MusicVisualizer', 'Analisis audio FFT tiempo real'])
    pdf.table_row(['MusicReactive', 'Interface objetos reactivos'])
    pdf.table_row(['MusicStars', 'Estrellas pulsan con beat'])
    pdf.table_row(['MusicIndicator', 'Ecualizador visual barras'])
    pdf.table_row(['EqualizerBarsDJ', 'Barras ecualizador estilo DJ'])
    pdf.table_row(['MusicSystem', 'Sistema central musica'])

    pdf.ln(5)
    pdf.section_title('5.9 UI y HUD (~15 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['HealthBar / HPBar', 'Barra de vida'])
    pdf.table_row(['ComboBar', 'Barra de combo'])
    pdf.table_row(['LoadingBar', 'Barra carga animada'])
    pdf.table_row(['HolographicTitle', 'Titulo efecto holografico'])
    pdf.table_row(['PlayPauseButton', 'Boton play/pause animado'])
    pdf.table_row(['MiniStopButton', 'Boton detener wallpaper'])
    pdf.table_row(['SimpleTextRenderer', 'Renderizador texto OpenGL'])

    pdf.add_page()
    pdf.section_title('5.10 Systems - Gestores Globales (16 clases)')
    pdf.table_row(['Clase', 'Proposito'], is_header=True)
    pdf.table_row(['WallpaperCatalog', 'Catalogo wallpapers disponibles'])
    pdf.table_row(['WallpaperPreferences', 'Preferencias SharedPreferences'])
    pdf.table_row(['EventBus', 'Bus eventos comunicacion desacoplada'])
    pdf.table_row(['GLStateManager', 'Estado global OpenGL'])
    pdf.table_row(['ScreenManager', 'Dimensiones y orientacion'])
    pdf.table_row(['ResourceManager', 'Gestion recursos'])
    pdf.table_row(['AdsManager', 'Gestion anuncios'])
    pdf.table_row(['SubscriptionManager', 'Suscripciones premium'])
    pdf.table_row(['RemoteConfigManager', 'Firebase Remote Config'])
    pdf.table_row(['FirebaseQueueManager', 'Batching operaciones Firebase'])

    # ===========================================
    # 6. CLASES OBSOLETAS
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('6. Clases Obsoletas')

    pdf.section_title('6.1 Completamente Huerfanas (Sin referencias)')
    pdf.body_text('Estas clases NO estan siendo usadas en ningun lugar del codigo:')

    pdf.table_row(['Clase', 'Razon'], is_header=True)
    pdf.table_row(['ArcadePreview.java', 'Comentada como REMOVIDO'])
    pdf.table_row(['EstrellaBailarina.java', 'Feature removida'])
    pdf.table_row(['CloudLayer.java', 'Sin imports ni instanciaciones'])
    pdf.table_row(['SunHeatEffect.java', 'Comentada como REMOVIDO'])
    pdf.table_row(['CircularLoadingRing.java', 'Sin referencias'])
    pdf.table_row(['DiscoBallShaderProgram.java', 'Sin referencias'])

    pdf.ln(5)
    pdf.section_title('6.2 Features Removidas (Codigo comentado)')
    pdf.table_row(['Clase', 'Razon'], is_header=True)
    pdf.table_row(['EarthShield.java', 'Comentada en BatallaCosmicaScene'])
    pdf.table_row(['ForceField.java', 'Nunca instanciada'])
    pdf.table_row(['BirthdayManager.java', 'Feature removida'])
    pdf.table_row(['BirthdayMarquee.java', 'Feature removida'])
    pdf.table_row(['LeaderboardManager.java', 'Feature removida'])
    pdf.table_row(['MagicLeaderboard.java', 'Feature removida'])
    pdf.table_row(['HoroscopeManager.java', 'Inicializacion comentada'])
    pdf.table_row(['HoroscopeDisplay.java', 'Inicializacion comentada'])

    pdf.ln(5)
    pdf.set_font('Helvetica', 'B', 11)
    pdf.set_text_color(200, 50, 50)
    pdf.cell(0, 8, 'Total: ~14 clases que podrian eliminarse para limpiar el proyecto', ln=True)

    # ===========================================
    # 7. FLUJO DE DATOS
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('7. Flujo de Datos')

    pdf.section_title('7.1 Flujo de Inicio de Wallpaper')

    flow1 = """
Usuario selecciona wallpaper en MainActivity
        |
        v
WallpaperAdapter.onClick()
        |
        | Guarda preferencia
        v
WallpaperPreferences.setSelectedWallpaper("Batalla Cosmica")
        |
        v
Intent -> WallpaperPreviewActivity
        |
        | Usuario presiona "Definir fondo"
        v
ACTION_CHANGE_LIVE_WALLPAPER
        |
        v
Android crea LiveWallpaperService
        |
        | onCreateEngine()
        v
new GLWallpaperEngine()
        |
        v
WallpaperDirector.onSurfaceCreated()
        |
        | Lee preferencia
        | SceneFactory.createScene("Batalla Cosmica")
        v
BatallaCosmicaScene.onCreate()
        |
        v
Loop de renderizado activo (60 FPS)
"""
    pdf.code_block(flow1)

    pdf.add_page()
    pdf.section_title('7.2 Flujo de Render Loop')

    flow2 = """
onDrawFrame() [60 FPS]
    |
    |  1. GLStateManager.beginFrame()
    |       - Calcula deltaTime, limpia buffers
    |
    |  2. switch(currentMode):
    |
    |     PANEL_MODE:
    |       - panelRenderer.updatePanelMode(dt)
    |       - panelRenderer.drawPanelMode()
    |
    |     LOADING_MODE:
    |       - panelRenderer.updateLoadingMode(dt)
    |       - panelRenderer.drawLoadingMode()
    |       - checkLoadingComplete()
    |
    |     WALLPAPER_MODE:
    |       - musicVisualizer.getFrequencyBands()
    |       - scene.updateMusicBands(bands)
    |       - sceneFactory.updateCurrentScene(dt)
    |           -> [cada objeto] obj.update(dt)
    |       - bloomEffect.beginCapture()
    |       - sceneFactory.drawCurrentScene()
    |           -> [cada objeto] obj.draw()
    |       - screenEffects.draw()
    |       - bloomEffect.endCaptureAndApply()
    |       - panelRenderer.drawWallpaperOverlay()
    |       - songSharing.draw()
"""
    pdf.code_block(flow2)

    # ===========================================
    # 8. GUIA PARA IAs
    # ===========================================
    pdf.add_page()
    pdf.chapter_title('8. Guia para IAs')

    pdf.section_title('8.1 Contexto Rapido')

    context = """
PROYECTO: Black Hole Glow
TIPO: Android Live Wallpaper
LENGUAJE: Java (NO Kotlin)
RENDERING: OpenGL ES 2.0/3.0
ARQUITECTURA: Actor Model + Scene Graph

ENTRY POINT: LiveWallpaperService.java
DIRECTOR: WallpaperDirector.java
ESCENAS: Extienden WallpaperScene.java
OBJETOS 3D: Implementan SceneObject interface
"""
    pdf.code_block(context)

    pdf.section_title('8.2 Patrones Usados')
    pdf.table_row(['Patron', 'Implementacion'], is_header=True)
    pdf.table_row(['Singleton', 'WallpaperCatalog, EventBus, GLStateManager'])
    pdf.table_row(['Factory', 'SceneFactory crea escenas por nombre'])
    pdf.table_row(['Observer', 'EventBus comunicacion desacoplada'])
    pdf.table_row(['State Machine', 'RenderModeController'])
    pdf.table_row(['Scene Graph', 'WallpaperScene contiene SceneObjects'])
    pdf.table_row(['Object Pool', 'ProjectilePool reutiliza proyectiles'])

    pdf.ln(5)
    pdf.section_title('8.3 Archivos Importantes')
    pdf.table_row(['Archivo', 'Importancia'], is_header=True)
    pdf.table_row(['WallpaperDirector.java', '***** Core del renderizado'])
    pdf.table_row(['WallpaperScene.java', '***** Base de todas las escenas'])
    pdf.table_row(['SceneFactory.java', '**** Creacion de escenas'])
    pdf.table_row(['BatallaCosmicaScene.java', '**** Escena principal'])
    pdf.table_row(['WallpaperCatalog.java', '*** Catalogo de wallpapers'])
    pdf.table_row(['LiveWallpaperService.java', '*** Entry point Android'])

    pdf.add_page()
    pdf.section_title('8.4 Como Agregar una Nueva Escena')

    new_scene = """
// 1. Crear clase que extienda WallpaperScene
public class MiNuevaScene extends WallpaperScene {

    @Override
    public String getName() { return "Mi Nueva Escena"; }

    @Override
    public String getDescription() { return "Descripcion..."; }

    @Override
    public int getPreviewResourceId() {
        return R.drawable.mi_preview;
    }

    @Override
    protected void setupScene() {
        // Crear objetos 3D aqui
        MiObjeto obj = new MiObjeto(context, textureManager);
        addSceneObject(obj);
    }

    @Override
    protected void releaseSceneResources() {
        // Liberar recursos especificos
    }
}

// 2. Registrar en SceneFactory.registerDefaultScenes()
registerScene("Mi Nueva Escena", MiNuevaScene.class);

// 3. Agregar al catalogo en WallpaperCatalog
catalog.add(new WallpaperItem.Builder("Mi Nueva Escena")
    .descripcion("...")
    .preview(R.drawable.mi_preview)
    .sceneName("Mi Nueva Escena")
    .tier(WallpaperTier.FREE)
    .build());
"""
    pdf.code_block(new_scene)

    pdf.section_title('8.5 Preguntas Frecuentes')
    pdf.body_text('P: Donde esta el main loop de renderizado?')
    pdf.body_text('R: WallpaperDirector.onDrawFrame()')
    pdf.ln(2)
    pdf.body_text('P: Como se cambia de escena?')
    pdf.body_text('R: WallpaperDirector.changeScene("nombre") -> SceneFactory.createScene()')
    pdf.ln(2)
    pdf.body_text('P: Donde se guardan las preferencias?')
    pdf.body_text('R: WallpaperPreferences usa SharedPreferences')
    pdf.ln(2)
    pdf.body_text('P: Como funciona la musica reactiva?')
    pdf.body_text('R: MusicVisualizer analiza audio -> pasa bandas de frecuencia a la escena -> objetos reaccionan')
    pdf.ln(2)
    pdf.body_text('P: Donde estan los shaders?')
    pdf.body_text('R: app/src/main/assets/shaders/*.glsl')

    # ===========================================
    # FINAL
    # ===========================================
    pdf.add_page()
    pdf.ln(40)
    pdf.set_font('Helvetica', 'B', 20)
    pdf.set_text_color(30, 60, 120)
    pdf.cell(0, 15, 'Fin del Documento', align='C', ln=True)

    pdf.ln(20)
    pdf.set_font('Helvetica', '', 12)
    pdf.set_text_color(100, 100, 100)
    pdf.cell(0, 8, 'Generado: Diciembre 2024', align='C', ln=True)
    pdf.cell(0, 8, 'Proyecto: Black Hole Glow v4.0.1', align='C', ln=True)
    pdf.cell(0, 8, 'Clases totales: 176', align='C', ln=True)
    pdf.cell(0, 8, 'Clases obsoletas identificadas: ~14', align='C', ln=True)

    # Guardar PDF
    output_path = 'D:/Orbix/blackholeglow/BLACKHOLEGLOW_ARCHITECTURE.pdf'
    pdf.output(output_path)
    print(f'PDF generado exitosamente: {output_path}')
    return output_path

if __name__ == '__main__':
    create_pdf()
