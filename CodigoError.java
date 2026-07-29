package com.sma.sprinkcalc;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class calc extends AppCompatActivity {

    private int globalCantAccesorios = 0;
    private double globalLongitudEquivalenteAccesorios = 0.0;

    private TextView tvAuto, tvFondoRes, tvFrenteRes, tvGuardar, tvRes;
    private Spinner spLongFrente; // Ancho (W)
    private Spinner spLongFondo;  // Largo (D)
    private final String[] valoresMetrosFijos = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
            "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30",
            "31", "32", "33", "34", "35"
    };
    private final String[] valoresPiesFijos = {
            "0", "3", "7", "10", "13", "16", "20", "23", "26", "30", "33", "36", "40", "43", "46", "50",
            "53", "56", "60", "63", "66", "70", "73", "76", "80", "83", "86", "90", "93", "96", "100",
            "103", "106", "110", "113", "115"
    };
    private ImageView imaHandRiesgo, imaHandLargo;
    private DataBaseHelper dbHelper;
    private boolean primeraCargaRiesgo = true;
    private double area, longitud, frente, fondo, alto = 0.0;
    private int cantidad;
    private double tempTechoValor;
    private String uTemp;
    private TextView tvRegi;
    private String diamNominal, riesgoSeleccionado, diametroPrincipalCalculado, diametroSecundarioCalculado;
    private double caudalTotalSistema, presionMinimaBar, presionTotal40, presionTotal10;
    private String uFrente, uFondo, uAlto, uDim, uArea;
    private EditText etLongFrente, etLongFondo;
    private Spinner spUndFrente, spUndFondo, spClasificacionRiesgo, spAlto, spUndAlto,spTempTecho,spUndTemp;
    private TextView tvArea, tvLongitudTuberias, tvAspersoresSugeridos;
    private GridPlaneView Plano;
    private final String[] altoMetros = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12"};
    private final String[] altoPies = {"3", "7", "10", "13", "16", "20", "23", "26", "30", "33", "36", "40"};

    private final String[] tempTechoC = {"10","20","30","40","50","60","70","80","90","100"};
    private final String[] tempTechoF = {"50","60","70","80","90","100","110","120","130","140"};

    // Variables de control y c�lculo l�gico
    private double caudalPorAspersor, areaPorAspersor;
    private String resultados;
    private int numeroPisoActual;
    private double presion, caudal;
    private LinearLayout layPlanoCompleto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calc);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        configurarEdgeToEdge();

        // 2. Vincular vistas del XML
        inicializarVistas();

        // 3. Configurar adaptadores y selectores de la UI
        configurarSpinners();
        configurarListeners();

        // 4. Esperar a que la vista se dibuje para inicializar las dimensiones de la grilla
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            mainView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mainView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    actualizarGrilla();
                }
            });
        }

        // 5. Obtener datos de navegaci�n (Intent)
        numeroPisoActual = getIntent().getIntExtra("NUMERO_PISO", 1);
        setTitle("Configuraci�n Piso " + numeroPisoActual);

        tvRes.setOnClickListener(v -> {
            // 1. Validar que la vista del plano est� inicializada y contenga datos v�lidos
            if (Plano == null || Plano.getSprinklerPoints() == null || Plano.getEntryPoint() == null) {
                return;
            }

            int cantidadAspersores = Plano.getSprinklerPoints().size();
            if (frente > 0 && fondo > 0 && cantidadAspersores > 0) {
                // 2. Determinar el sistema de unidades activo
                boolean esPies = spUndFrente.getSelectedItem().toString().equals("ft");
                String unidadArea = esPies ? "ft�" : "m�";
                String unidadLong = esPies ? "ft" : "m";

                // 3. Obtener dimensiones calculadas seg�n la unidad de la interfaz y capturar Altura
                double frenteCalc = frente;
                double fondoCalc = fondo;
                double areaCalculada = frenteCalc * fondoCalc;

                // Sincronizamos la variable global 'alto' con la selecci�n actual del Spinner
                if (spAlto != null && spAlto.getSelectedItem() != null) {
                    this.alto = Double.parseDouble(spAlto.getSelectedItem().toString());
                }

                // Capturamos la unidad de la altura de forma independiente
                String unidadAlto = (spUndAlto != null && spUndAlto.getSelectedItem() != null)
                        ? spUndAlto.getSelectedItem().toString()
                        : "m";

                // Convertimos la altura a la unidad del plano para la interfaz visual
                double alturaEnUnidadPlano = this.alto;
                if (esPies && unidadAlto.equals("m")) {
                    alturaEnUnidadPlano = this.alto * 3.28084;
                } else if (!esPies && unidadAlto.equals("ft")) {
                    alturaEnUnidadPlano = this.alto * 0.3048;
                }

                // 4. Recuperar las longitudes calculadas directamente desde el objeto de instancia 'Plano'
                double longL1 = Plano.getLongitudPrincipal();
                double longL2 = Plano.getLongitudRamales();

                // Sumamos la altura f�sica del tubo de subida a la longitud de la Tuber�a Principal (L1)
                double longL1TotalConAltura = longL1 + alturaEnUnidadPlano;

                // Convertimos CADA tramo de tuber�a a metros nativos de forma INDEPENDIENTE para el motor
                double metrosL1 = longL1TotalConAltura;
                double metrosL2 = longL2;

                if (esPies) {
                    metrosL1 = longL1TotalConAltura * 0.3048;
                    metrosL2 = longL2 * 0.3048;
                }

                // 5. EJECUCI�N DEL MOTOR HIDR�ULICO DESACOPLADO (Pasando L1 y L2 por separado)
                this.riesgoSeleccionado = spClasificacionRiesgo.getSelectedItem().toString();

                int totalLineasL2 = Plano.getSprinklerPoints().size() / 2;

                // Se pasan 9 par�metros: metrosL1 y metrosL2 desacoplados para el c�lculo exacto de fricci�n por velocidad
                com.sma.sprinkcalc.HydraulicResult resultado = com.sma.sprinkcalc.HydraulicCalculator.calcularSistema(
                        areaCalculada,
                        cantidadAspersores,
                        this.riesgoSeleccionado,
                        esPies ? "ft" : "m",
                        metrosL1,
                        metrosL2,
                        totalLineasL2,
                        this.alto,
                        unidadAlto
                );

                // === ANCLAJE DE SEGURIDAD PARA EL RIESGO ===
                if (resultado.getRiesgo() == null || resultado.getRiesgo().isEmpty()) {
                    resultado.setRiesgo(this.riesgoSeleccionado);
                }

                // Asignaci�n a variables globales sincronizadas
                this.caudalPorAspersor = resultado.getCaudalPorAspersor();
                this.caudalTotalSistema = resultado.getCaudalTotalSistema();
                this.areaPorAspersor = resultado.getAreaPorAspersor();
                this.presionMinimaBar = resultado.getPresionMinimaBar();
                this.diametroPrincipalCalculado = resultado.getDiametroPrincipal();
                this.diametroSecundarioCalculado = resultado.getDiametroSecundario();
                this.presionTotal40 = resultado.getPresionTotalSch40();
                this.presionTotal10 = resultado.getPresionTotalSch10();

                // FIX: Guardamos los accesorios en las variables globales de la clase
                this.globalCantAccesorios = resultado.getTotalAccesorios();
                this.globalLongitudEquivalenteAccesorios = resultado.getLongitudEquivalenteMetros();

                // Ajuste din�mico de unidades para la longitud equivalente a mostrar en el di�logo
                double longEquiMostrar = esPies ? (this.globalLongitudEquivalenteAccesorios * 3.28084) : this.globalLongitudEquivalenteAccesorios;

                String textoDiamL1 = this.diametroPrincipalCalculado;
                String textoDiamL2 = this.diametroSecundarioCalculado;

                // 6. Desplegar el di�logo responsivo incluyendo el riesgo al inicio
                mostrar_dialogo_resultados(
                        this.riesgoSeleccionado,                                                                            // riesgoText
                        String.format(Locale.US, "%.2f %s", areaCalculada, unidadArea),                                     // areaText
                        String.format(Locale.US, "%.2f %s", longL1TotalConAltura, unidadLong),                              // LongTuberiaPrincipalText
                        textoDiamL1,                                                                                        // DiamTuberiaPrincipalText
                        String.format(Locale.US, "%.2f %s", longL2, unidadLong),                                            // LongTuberiaSecundariaText
                        textoDiamL2,                                                                                        // DiamTuberiaSecundariaText
                        String.format(Locale.US, "%.2f %s", this.alto, unidadAlto),                                         // AlturaText
                        String.valueOf(this.globalCantAccesorios),                                                          // CantidadAccesoriosText
                        String.format(Locale.US, "%.2f %s", longEquiMostrar, unidadLong),                                   // LongEquiAccesoriosText
                        String.format(Locale.US, "%.2f lpm", this.caudalTotalSistema),                                     // CaudalTotalText
                        String.valueOf(cantidadAspersores),                                                                 // CantAspText
                        String.format(Locale.US, "%.2f Bar", this.presionMinimaBar),                                        // PreAspText
                        String.format(Locale.US, "%.2f lpm", this.caudalPorAspersor),                                       // CaudalPorAspersorText
                        String.format(Locale.US, "%.2f Bar", this.presionTotal40),                                          // PresionSch40Text
                        String.format(Locale.US, "%.2f Bar", this.presionTotal10)                                           // PresionSch10Text
                );

            } else {
                // Manejo alternativo si los datos son menores o iguales a 0
            }
        });

// Inicializaci�n final segura al cargar la actividad
        if (etLongFrente != null && etLongFondo != null) {
            calcularArea(); // Reemplaza de forma segura a ejecutarCalculosBase()
        } else {
            limpiarTodoElSistema(); // Usa el m�todo global correcto de reset
        }

        dbHelper = new DataBaseHelper(this);

        tvRegi.setOnClickListener(v -> {
            Intent intent = new Intent(calc.this, regis.class);
            startActivity(intent);
        });

        mostrarGestoSugerencia();

    }

    private void inicializarVistas() {
        tvAuto = findViewById(R.id.tvAuto);
        tvRes = findViewById(R.id.tvRes);
        tvRegi = findViewById(R.id.tvRegi);
        tvFondoRes = findViewById(R.id.tvFondoRes);
        tvFrenteRes = findViewById(R.id.tvFrenteRes);


        spAlto = findViewById(R.id.spdAlto);
        spUndAlto = findViewById(R.id.spUndAlto);

        spUndFrente = findViewById(R.id.spUndFrente);
        spUndFondo = findViewById(R.id.spUndFondo);
        spClasificacionRiesgo = findViewById(R.id.spClasificacionRiesgo);
        spTempTecho = findViewById(R.id.spTempTecho);
        spUndTemp = findViewById(R.id.spUndTemp);

        // ====================================================================
        // �AQU� EST� EL FIX! Vinculamos los Spinners de dimensi�n con el XML
        // ====================================================================
        spLongFrente = findViewById(R.id.spLongFrente); // Ajusta al ID real de tu XML
        spLongFondo = findViewById(R.id.spLongFondo);   // Ajusta al ID real de tu XML
        // ====================================================================

        Plano = findViewById(R.id.Plano);

        imaHandRiesgo = findViewById(R.id.imaHandRiesgo);
        imaHandLargo = findViewById(R.id.imaHandLargo);

        if (imaHandRiesgo != null) imaHandRiesgo.setVisibility(View.GONE);
        if (imaHandLargo != null) imaHandLargo.setVisibility(View.GONE);
    }

    private void calcularYMostrarLongitudTuberias() {
        if (Plano == null || Plano.getSprinklerPoints() == null || Plano.getSprinklerPoints().isEmpty())
            return;

        Plano.setDrawPipesMode(true);

        // 1. Sincronizamos la unidad actual desde el Spinner
        if (spUndFrente != null && spUndFrente.getSelectedItem() != null) {
            this.uDim = spUndFrente.getSelectedItem().toString();
        } else {
            this.uDim = "m";
        }

        // 2. Convertimos el valor de la grilla (que viene en metros nativos) si el usuario eligi� pies
        if (this.uDim.equals("ft")) {
            this.longitud = Plano.getPipeLength() * 3.28084; // De metros a pies
        } else {
            this.longitud = Plano.getPipeLength(); // Se mantiene en metros
        }

        // 3. Mostramos en la interfaz con el formato y la unidad correcta
        if (tvLongitudTuberias != null) {
            tvLongitudTuberias.setText(String.format(Locale.US, "Longitud Tuber�as: %.2f %s", this.longitud, this.uDim));
        }
    }
    private void distribuirAspersoresAutomaticamente(int totalNoUtilizado) {
        if (Plano == null) return;
        double frente = Plano.getAnchoMetros(); // Ya est� en metros nativos
        double fondo = Plano.getAltoMetros();   // Ya est� en metros nativos
        if (frente <= 0 || fondo <= 0) return;

        // 1. Obtener el radio real seg�n el riesgo
        String riesgo = spClasificacionRiesgo.getSelectedItem().toString();
        double radio = 1.72; // Por defecto Extra
        if (riesgo.contains("Leve")) radio = 2.58;
        else if (riesgo.contains("Ordinario")) radio = 1.96;

        // 2. Calcular la distancia m�xima permitida a la pared para cubrir la esquina
        // Distancia = Radio / sqrt(2)
        double distParedMax = radio / Math.sqrt(2);

        // 3. Calcular la separaci�n m�xima entre rociadores para evitar huecos internos
        double sepMax = radio * Math.sqrt(2);

        // 4. Calcular cantidad de filas y columnas requeridas
        // Restamos el doble de la distancia a la pared y dividimos por la separaci�n m�xima
        int columnas = (int) Math.ceil((frente - (2 * distParedMax)) / sepMax) + 1;
        int filas = (int) Math.ceil((fondo - (2 * distParedMax)) / sepMax) + 1;

        // Asegurar m�nimo 1
        if (columnas < 1) columnas = 1;
        if (filas < 1) filas = 1;

        // 5. Calcular los espaciamientos reales sim�tricos
        float sRealX = (columnas > 1) ? (float) (frente - (2 * distParedMax)) / (columnas - 1) : 0f;
        float sRealY = (filas > 1) ? (float) (fondo - (2 * distParedMax)) / (filas - 1) : 0f;

        // Si solo hay uno, lo centramos
        float inicioX = (columnas > 1) ? (float) distParedMax : (float) frente / 2f;
        float inicioY = (filas > 1) ? (float) distParedMax : (float) fondo / 2f;

        // 6. Generar los puntos
        List<GridPlaneView.GridPoint> puntos = new ArrayList<>();
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                float posX = inicioX + (j * sRealX);
                float posY = inicioY + (i * sRealY);
                puntos.add(new GridPlaneView.GridPoint(posX, posY));
            }
        }

        // 7. Actualizar la variable global de cantidad para la hidr�ulica posterior
        this.cantidad = puntos.size();
        if (tvAspersoresSugeridos != null) {
            tvAspersoresSugeridos.setText(String.format(Locale.US, "Sugeridos: %d", this.cantidad));
        }

        Plano.setSprinklerPoints(puntos);
        Plano.invalidate();
    }

    private void actualizarGrilla() {
        try {
            if (Plano == null || spLongFrente == null || spLongFondo == null ||
                    spLongFrente.getSelectedItem() == null || spLongFondo.getSelectedItem() == null)
                return;

            double frenteLocal = Double.parseDouble(spLongFrente.getSelectedItem().toString());
            double fondoLocal = Double.parseDouble(spLongFondo.getSelectedItem().toString());

            double areaPrueba = frenteLocal * fondoLocal;
            double limiteMaximo = uDim.equals("ft") ? 13185.0 : 1225.0; // 1225 m� o ~13185 ft�

            if (areaPrueba > limiteMaximo) {
                return;
            }

            // Conversi�n a metros para el lienzo de dibujo
            if (spUndFrente.getSelectedItem().toString().equals("ft")) frenteLocal *= 0.3048;
            if (spUndFondo.getSelectedItem().toString().equals("ft")) fondoLocal *= 0.3048;

            Plano.setDimensiones(frenteLocal, fondoLocal);
            Plano.invalidate();
        } catch (Exception ignored) {
        }
    }
    private void ejecutarDisenoCompletoAutomatico() {
        double areaCalculada = calcularArea();
        if (areaCalculada <= 0 || Plano == null) return;

        String unidadAltoSeleccionada = "m"; // Variable local para la unidad de altura

        try {
            // 1. Sincronizar variables globales de UI (Incluyendo las nuevas de temperatura)
            actualizarTextosUnidades();

            if (spLongFrente != null && spLongFrente.getSelectedItem() != null) {
                this.frente = Double.parseDouble(spLongFrente.getSelectedItem().toString());
            }
            if (spLongFondo != null && spLongFondo.getSelectedItem() != null) {
                this.fondo = Double.parseDouble(spLongFondo.getSelectedItem().toString());
            }
            // --- Sincronizar tambi�n el alto por seguridad ---
            if (spAlto != null && spAlto.getSelectedItem() != null) {
                this.alto = Double.parseDouble(spAlto.getSelectedItem().toString());
            }
            if (spUndAlto != null && spUndAlto.getSelectedItem() != null) {
                unidadAltoSeleccionada = spUndAlto.getSelectedItem().toString();
            }

            this.riesgoSeleccionado = spClasificacionRiesgo.getSelectedItem().toString();
            this.uFrente = spUndFrente.getSelectedItem().toString();
            this.uFondo = spUndFondo.getSelectedItem().toString();
            this.uDim = this.uFrente;
            this.uArea = this.uDim.equals("ft") ? "ft�" : "m�";

            tvFrenteRes.setText(String.format(Locale.US, "%.2f %s", frente, uFrente));
            tvFondoRes.setText(String.format(Locale.US, "%.2f %s", fondo, uFondo));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 2. DELEGACI�N A LA CAPA DE LA GRILLA (Render)
        actualizarGrilla();
        Plano.setRadioCobertura(this.riesgoSeleccionado, false);

        distribuirAspersoresAutomaticamente(0); // Este m�todo llena this.cantidad y pone sprinklerPoints
        colocarValvulaEntradaMedia();
        calcularYMostrarLongitudTuberias(); // Este m�todo calcula la longitud f�sica en el lienzo

        // ====================================================================
        // --- MODIFICADO: Recuperar y Desacoplar Longitudes L1 y L2 ---
        // ====================================================================
        boolean esPies = "ft".equals(this.uDim);
        double longL1 = Plano.getLongitudPrincipal();
        double longL2 = Plano.getLongitudRamales();

        // Convertimos la altura a la unidad del plano para sum�rsela a L1
        double alturaEnUnidadPlano = this.alto;
        if (esPies && unidadAltoSeleccionada.equals("m")) {
            alturaEnUnidadPlano = this.alto * 3.28084;
        } else if (!esPies && unidadAltoSeleccionada.equals("ft")) {
            alturaEnUnidadPlano = this.alto * 0.3048;
        }

        // Sumamos la altura f�sica del tubo vertical a la tuber�a principal (L1)
        double longL1TotalConAltura = longL1 + alturaEnUnidadPlano;

        // Convertimos CADA tramo a metros nativos de forma independiente para el motor
        double metrosL1 = longL1TotalConAltura;
        double metrosL2 = longL2;

        if (esPies) {
            metrosL1 = longL1TotalConAltura * 0.3048;
            metrosL2 = longL2 * 0.3048;
        }

        // Asumiendo que sabes cu�ntos ramales calcula tu distribuci�n autom�tica
        int totalLineasL2 = this.cantidad / 2;

        // ====================================================================
        // --- NUEVO: Convertir Temperatura a Celsius para la Evaluaci�n ---
        // ====================================================================
        double tempEnCelsius = this.tempTechoValor;
        if ("�F".equals(this.uTemp)) {
            tempEnCelsius = (this.tempTechoValor - 32) / 1.8;
        }

        // --- CORREGIDO: Ahora pasamos los par�metros al motor incluyendo temperatura ---
        HydraulicResult resultado = HydraulicCalculator.calcularSistema(
                areaCalculada,
                this.cantidad,
                this.riesgoSeleccionado,
                this.uDim,
                metrosL1,                  // 5. Longitud L1 en metros (con altura)
                metrosL2,                  // 6. Longitud L2 en metros (ramales)
                totalLineasL2,             // 7. Cantidad de ramales
                this.alto,                 // 8. Valor num�rico del alto
                unidadAltoSeleccionada,    // 9. Unidad del alto ("m" o "ft")
                tempEnCelsius              // 10. NUEVO: Temperatura estandarizada en �C
        );

        // 4. SETEAR VARIABLES GLOBALES DE REGISTRO E INTERFAZ CON LOS RESULTADOS
        this.caudalPorAspersor = resultado.getCaudalPorAspersor();
        this.caudalTotalSistema = resultado.getCaudalTotalSistema();
        this.areaPorAspersor = resultado.getAreaPorAspersor();
        this.presionMinimaBar = resultado.getPresionMinimaBar();
        this.diametroPrincipalCalculado = resultado.getDiametroPrincipal();
        this.diametroSecundarioCalculado = resultado.getDiametroSecundario();
        this.presionTotal10 = resultado.getPresionTotalSch10();
        this.presionTotal40 = resultado.getPresionTotalSch40();

        // Variables globales para la info del bulbo (aseg�rate de declararlas arriba en la clase)
        this.clasificacionRociador = resultado.getClasificacionRociador();
        this.colorBulboRecomendado = resultado.getColorBulbo();
    }

    private double calcularArea() {
        double valorFrente = 0.0;
        double valorFondo = 0.0;

        try {
            if (spLongFrente != null && spLongFrente.getSelectedItem() != null) {
                valorFrente = Double.parseDouble(spLongFrente.getSelectedItem().toString());
            }
            if (spLongFondo != null && spLongFondo.getSelectedItem() != null) {
                valorFondo = Double.parseDouble(spLongFondo.getSelectedItem().toString());
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        // El c�lculo del �rea es directo: Frente x Fondo
        this.area = valorFrente * valorFondo;
        return this.area;
    }

    private void colocarValvulaEntradaMedia() {
        if (Plano == null) return;
        int xMedia = (int) (Plano.getAnchoMetros() / 2);
        int ySuperior = 0;

        Plano.setEntryPoint(new GridPlaneView.GridPoint(xMedia, ySuperior));
        Plano.invalidate();
    }






    private void dialogo_nombre() {
        Dialog dialogView = new Dialog(this, R.style.FullScreenDialog);
        dialogView.setContentView(R.layout.dialogo_nombre);

        Window window = dialogView.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.TOP);
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = (int) (200 * getResources().getDisplayMetrics().density);
            window.setAttributes(params);

            // CAMBIO 1: Obliga al di�logo a reajustarse y empujar la vista si el teclado lo tapa
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        EditText etNombreProyecto = dialogView.findViewById(R.id.etNombre);
        TextView btnConfirmarGuardar = dialogView.findViewById(R.id.tvGuardar);
        GridPlaneView planoView = findViewById(R.id.Plano);

        btnConfirmarGuardar.setOnClickListener(v -> {
            String nombreProyecto = etNombreProyecto.getText().toString().trim();

            if (nombreProyecto.isEmpty()) {
                etNombreProyecto.setError("Asigna un nombre para guardar");
                return;
            }

            // 1. Obtener la fecha actual formateada de forma segura
            String fechaActual = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

            try {
                // ** Guardamos el plano como imagen en el dispositivo ANTES de insertar en la BD **
                String rutaImagenPlano = "";
                if (planoView != null && planoView.getWidth() > 0 && planoView.getHeight() > 0) {
                    rutaImagenPlano = guardarPlanoComoImagen(planoView);
                }

                // 2. Instanciamos y poblamos tu clase modelo real: objetoPiso
                objetoPiso pisoAGuardar = new objetoPiso();

                pisoAGuardar.setNombreProyecto(nombreProyecto);
                pisoAGuardar.setFecha(fechaActual);
                pisoAGuardar.setRiesgo(this.riesgoSeleccionado);

                // Dimensiones geom�tricas principales
                pisoAGuardar.setFrente(this.frente);
                pisoAGuardar.setUndFrente(this.uDim); // "m" o "ft"
                pisoAGuardar.setFondo(this.fondo);
                pisoAGuardar.setUndFondo(this.uDim);
                pisoAGuardar.setAlto(this.alto);
                pisoAGuardar.setUndAlto(this.uDim);

                // Datos de �rea resultantes
                pisoAGuardar.setArea(this.area);
                pisoAGuardar.setUndArea(this.uArea); // "m�" o "ft�"

                // Par�metros hidr�ulicos calculados (NFPA 13)
                pisoAGuardar.setCantAspersores(this.cantidad);
                pisoAGuardar.setCaudalPorAspersor(this.caudalPorAspersor);
                pisoAGuardar.setAreaPorAspersor(this.areaPorAspersor);
                pisoAGuardar.setUndAreaPorAspersor(this.uArea);
                pisoAGuardar.setCaudalTotalSistema(this.caudalTotalSistema);
                pisoAGuardar.setPresionAspersor(this.presionMinimaBar);

                // P�rdidas por fricci�n y demandas Sch 40 y Sch 10
                pisoAGuardar.setPresionTotal40(this.presionTotal40);
                pisoAGuardar.setPresionTotal10(this.presionTotal10);

                // --- ASIGNACI�N DE TUBER�AS Y ACCESORIOS DESDE EL FLUJO ---
                if (planoView != null) {
                    // Longitudes calculadas por la vista personalizada
                    pisoAGuardar.setLongitudTuberiaPrincipal(planoView.getLongitudPrincipal());
                    pisoAGuardar.setLongitudTuberiaSecundaria(planoView.getLongitudRamales());

                    // Unidades de medida ("m" o "ft")
                    pisoAGuardar.setUndLongTuberiaPrincipal(this.uDim);
                    pisoAGuardar.setUndLongTuberiaSecundaria(this.uDim);

                    // Di�metros calculados o seleccionados en tu flujo
                    pisoAGuardar.setDiamNominalPrincipal(this.diametroPrincipalCalculado != null ? this.diametroPrincipalCalculado : "N/A");
                    pisoAGuardar.setDiamNominalSecundaria(this.diametroSecundarioCalculado != null ? this.diametroSecundarioCalculado : "N/A");

                    // ====================================================================
                    // SOLUCI�N: Asignamos los datos reales desde las variables de la clase
                    // ====================================================================
                    pisoAGuardar.setCantAccesorios(this.globalCantAccesorios);
                    pisoAGuardar.setLongitudEquivalenteAccesorios(this.globalLongitudEquivalenteAccesorios);
                    // ====================================================================
                }

                // ** Guardamos la ruta de la imagen en tu objeto modelo **
                pisoAGuardar.setRutaPlano(rutaImagenPlano);

                // 3. Enviamos el objeto al dbHelper a trav�s de guardarCalculo()
                if (dbHelper != null) {
                    boolean insertado = dbHelper.guardarCalculo(pisoAGuardar);

                    if (insertado) {
                        // === AQU� OCULTAMOS EL TECLADO ===
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(etNombreProyecto.getWindowToken(), 0);
                        }
                        dialogView.dismiss();
                        Intent intent = new Intent(calc.this, regis.class);
                        startActivity(intent);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // CAMBIO 2: Primero, obligatoriamente, mostramos el di�logo en pantalla
        dialogView.show();

        // CAMBIO 3: Esperamos a que la ventana est� lista para lanzar el teclado de forma segura
        etNombreProyecto.postDelayed(() -> {
            etNombreProyecto.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etNombreProyecto, InputMethodManager.SHOW_FORCED);
            }
        }, 100);
    }

    private void mostrar_dialogo_resultados(
            String riesgoText,
            String tempTechoText, // <-- 1. Agrega el nuevo par�metro aqu�
            String areaText,
            String LongTuberiaPrincipalText,
            String DiamTuberiaPrincipalText,
            String LongTuberiaSecundariaText,
            String DiamTuberiaSecundariaText,
            String AlturaText,
            String CantidadAccesoriosText,
            String LongEquiAccesoriosText,
            String CaudalTotalText,
            String CantAspText,
            String PreAspText,
            String CaudalPorAspersorText,
            String PresionSch40Text,
            String PresionSch10Text) {

        // 1. Instanciar el di�logo usando el layout personalizado
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialogorespiso, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();

        // 2. Establecer el fondo transparente para respetar los bordes redondeados del XML
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Mapeo de vistas
        TextView tvRiesgo = dialogView.findViewById(R.id.tvRiesgo);
        TextView tvTemp = dialogView.findViewById(R.id.tvTemp); // <-- 2. Vincula la vista del XML
        TextView tvArea = dialogView.findViewById(R.id.tvArea);
        TextView tvTuberiaPrincipal = dialogView.findViewById(R.id.tvTuberiaPrincipal);
        TextView tvTuberiaSecundaria = dialogView.findViewById(R.id.tvTuberiaSecundaria);
        TextView tvCantAcce = dialogView.findViewById(R.id.tvCantAcce);
        TextView tvLongAcce = dialogView.findViewById(R.id.tvLongEquiAcce);
        TextView tvCaudalTotal = dialogView.findViewById(R.id.tvCaudalTotal);
        TextView tvCantAsp = dialogView.findViewById(R.id.tvCantAsp);
        TextView tvPreAsp = dialogView.findViewById(R.id.tvPreAsp);
        TextView tvCaudalPorAsp = dialogView.findViewById(R.id.tvCaudalPorAsp);
        TextView tvPresionBombaSch40 = dialogView.findViewById(R.id.tvPresionBombaSch40);
        TextView tvPresionBombaSch10 = dialogView.findViewById(R.id.tvPresionBombaSch10);
        TextView tvGuardar = dialogView.findViewById(R.id.tvGuardar);
        ImageView imaCerrar = dialogView.findViewById(R.id.imaCerrar);
        TextView tvAltura = dialogView.findViewById(R.id.tvAltura);

        // Bot�n Cerrar (X)
        if (imaCerrar != null) {
            imaCerrar.setOnClickListener(v -> dialog.dismiss());
        }

        // Bot�n Guardar
        if (tvGuardar != null) {
            tvGuardar.setOnClickListener(v -> {
                dialogo_nombre();
                dialog.dismiss();
            });
        }

        // 4. Asignar los textos validando que las vistas no sean nulas
        if (tvRiesgo != null) tvRiesgo.setText(riesgoText);
        if (tvTemp != null) tvTemp.setText(tempTechoText); // <-- 3. Asigna el valor din�mico
        if (tvArea != null) tvArea.setText(areaText);
        if (tvTuberiaPrincipal != null)
            tvTuberiaPrincipal.setText(LongTuberiaPrincipalText + " ( � " + DiamTuberiaPrincipalText + " )");
        if (tvTuberiaSecundaria != null)
            tvTuberiaSecundaria.setText(LongTuberiaSecundariaText + " ( � " + DiamTuberiaSecundariaText + " )");
        if (tvAltura != null) tvAltura.setText(AlturaText);
        if (tvCantAcce != null) tvCantAcce.setText(CantidadAccesoriosText);
        if (tvLongAcce != null) tvLongAcce.setText(LongEquiAccesoriosText);
        if (tvCaudalTotal != null) tvCaudalTotal.setText(CaudalTotalText);
        if (tvCantAsp != null) tvCantAsp.setText(CantAspText);
        if (tvPreAsp != null) tvPreAsp.setText(PreAspText);
        if (tvCaudalPorAsp != null) tvCaudalPorAsp.setText(CaudalPorAspersorText);
        if (tvPresionBombaSch40 != null) tvPresionBombaSch40.setText(PresionSch40Text);
        if (tvPresionBombaSch10 != null) tvPresionBombaSch10.setText(PresionSch10Text);

        // 5. Mostrar el di�logo
        dialog.show();

        // 6. Ajustar el tama�o responsivo (90% del ancho de pantalla)
        if (dialog.getWindow() != null) {
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int widthDisplay = displayMetrics.widthPixels;
            int dialogWindowWidth = (int) (widthDisplay * 0.90);

            android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = dialogWindowWidth;
            layoutParams.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
    }
    private void configurarListenersUnidadesLimpieza() {
        // 1. CAMBIO DE UNIDAD EN EL FONDO (Largo)
        if (spUndFondo != null) {
            spUndFondo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Delegamos la sincronizaci�n compleja al m�todo seguro
                    sincronizarUnidades(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // 2. SELECCI�N NUM�RICA DEL LARGO (Fondo)
        if (spLongFondo != null) {
            spLongFondo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        fondo = Double.parseDouble(spLongFondo.getSelectedItem().toString());
                    } catch (Exception e) {
                        fondo = 0.0;
                    }

                    limpiarTodoElSistema();

                    // Si hay dimensiones v�lidas, forzamos el c�lculo del �rea e interfaz base
                    if (frente > 0 && fondo > 0) {
                        calcularArea();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // 3. SELECCI�N NUM�RICA DEL ANCHO (Frente)
        if (spLongFrente != null) {
            spLongFrente.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        frente = Double.parseDouble(spLongFrente.getSelectedItem().toString());
                    } catch (Exception e) {
                        frente = 0.0;
                    }

                    limpiarTodoElSistema();

                    if (frente > 0 && fondo > 0) {
                        calcularArea();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // ==========================================
        // NUEVO: 4. CAMBIO DE UNIDAD EN LA TEMPERATURA
        // ==========================================
        if (spUndTemp != null) {
            spUndTemp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // Sincroniza todas las unidades del sistema seg�n el �ndice seleccionado (0: M�trico, 1: Imperial)
                    sincronizarUnidades(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        // ==========================================
    }

    private void configurarListeners() {
        TextWatcher manualWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Plano != null) Plano.limpiarPlano();
                calcularArea(); // Reemplaza a ejecutarCalculosBase() para mantener el �rea al d�a
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        if (etLongFrente != null) etLongFrente.addTextChangedListener(manualWatcher);
        if (etLongFondo != null) etLongFondo.addTextChangedListener(manualWatcher);

        if (tvAuto != null) {
            tvAuto.setOnClickListener(v -> {
                ejecutarDisenoCompletoAutomatico();
            });
        }
    }

    private void sincronizarUnidades(int position) {
        // Desactivar temporalmente los listeners para evitar bucles infinitos por setSelection()
        if (spUndFrente != null) spUndFrente.setOnItemSelectedListener(null);
        if (spUndFondo != null) spUndFondo.setOnItemSelectedListener(null);
        if (spUndAlto != null) spUndAlto.setOnItemSelectedListener(null);
        if (spUndTemp != null) spUndTemp.setOnItemSelectedListener(null); // <- NUEVO: Listener de unidad de temperatura

        // Forzar la misma posici�n en todos los spinners de unidades
        if (spUndFrente != null) spUndFrente.setSelection(position);
        if (spUndFondo != null) spUndFondo.setSelection(position);
        if (spUndAlto != null) spUndAlto.setSelection(position);
        if (spUndTemp != null) spUndTemp.setSelection(position); // <- NUEVO: Sincroniza �C o �F

        // Actualizar variables de estado globales de la unidad
        if (spUndFondo != null) {
            uFondo = spUndFondo.getSelectedItem().toString();
            uDim = uFondo;
            uArea = uFondo.equals("ft") ? "ft�" : "m�";
        }

        // NUEVO: Variable global para la unidad de temperatura si la necesitas en los c�lculos
        if (spUndTemp != null) {
            uTemp = spUndTemp.getSelectedItem().toString(); // ej: "�C" o "�F"
        }

        // Intercambiar el adaptador del spinner num�rico de altura seg�n la unidad (0: m, 1: ft)
        ArrayAdapter<String> adapterMetros = new ArrayAdapter<>(this, R.layout.item_spinner, altoMetros);
        ArrayAdapter<String> adapterPies = new ArrayAdapter<>(this, R.layout.item_spinner, altoPies);

        if (position == 0) {
            if (spAlto != null) spAlto.setAdapter(adapterMetros);
        } else {
            if (spAlto != null) spAlto.setAdapter(adapterPies);
        }

        // ==========================================
        // NUEVO: INTERCAMBIAR ADAPTADOR DE TEMPERATURA DEL TECHO
        // ==========================================
        ArrayAdapter<String> adapterTempC = new ArrayAdapter<>(this, R.layout.item_spinner, tempTechoC);
        ArrayAdapter<String> adapterTempF = new ArrayAdapter<>(this, R.layout.item_spinner, tempTechoF);

        if (spTempTecho != null) {
            if (position == 0) {
                spTempTecho.setAdapter(adapterTempC);
            } else {
                spTempTecho.setAdapter(adapterTempF);
            }
            spTempTecho.setSelection(0); // Reinicia a la temperatura m�s baja por defecto (10�C o 50�F)
        }
        // ==========================================

        // Actualizar los adaptadores de los spinners de Longitud (Frente y Fondo)
        String[] opcionesDimension = "ft".equals(uDim) ? valoresPiesFijos : valoresMetrosFijos;
        ArrayAdapter<String> adapterFijo = new ArrayAdapter<>(this, R.layout.item_spinner, opcionesDimension);

        if (spLongFondo != null) {
            spLongFondo.setAdapter(adapterFijo);
            spLongFondo.setSelection(0);
        }
        if (spLongFrente != null) {
            spLongFrente.setAdapter(adapterFijo);
            spLongFrente.setSelection(0);
        }

        // Limpiar el lienzo
        if (Plano != null) Plano.limpiarPlano();

        // Recalcular �rea base en base a las nuevas unidades seleccionadas
        calcularArea();

        // Si el usuario ya ten�a texto ingresado en modo manual, dispara el redise�o autom�tico de inmediato
        if (etLongFrente != null && etLongFondo != null
                && !etLongFrente.getText().toString().isEmpty()
                && !etLongFondo.getText().toString().isEmpty()) {
            ejecutarDisenoCompletoAutomatico();
        }

        // IMPORTANTE: Volver a activar los listeners reactivos de la UI
        configurarListenersUnidadesLimpieza();
    }
    private void actualizarTextosUnidades() {
        if (spAlto != null && spAlto.getSelectedItem() != null) {
            // Guardamos el valor num�rico de la altura seleccionada (ej: "3" o "10")
            try {
                this.alto = Double.parseDouble(spAlto.getSelectedItem().toString());
            } catch (Exception e) {
                this.alto = 0.0;
            }
        }

        // ==========================================
        // NUEVO: CAPTURAR VALOR NUM�RICO DE LA TEMPERATURA DEL TECHO
        // ==========================================
        if (spTempTecho != null && spTempTecho.getSelectedItem() != null) {
            try {
                // Guarda el valor num�rico (ej: "40" o "100") en una variable global (double o int)
                this.tempTechoValor = Double.parseDouble(spTempTecho.getSelectedItem().toString());
            } catch (Exception e) {
                this.tempTechoValor = 0.0;
            }
        }
        // ==========================================

        if (spUndFrente != null && spUndFrente.getSelectedItem() != null) {
            this.uFrente = spUndFrente.getSelectedItem().toString(); // "m" o "ft"
            this.uDim = this.uFrente;
            this.uArea = this.uFrente.equals("ft") ? "ft�" : "m�";
        }

        if (spUndFondo != null && spUndFondo.getSelectedItem() != null) {
            this.uFondo = spUndFondo.getSelectedItem().toString();
        }

        if (spUndAlto != null && spUndAlto.getSelectedItem() != null) {
            this.uAlto = spUndAlto.getSelectedItem().toString();
        }

        // ==========================================
        // NUEVO: CAPTURAR LA UNIDAD DE LA TEMPERATURA
        // ==========================================
        if (spUndTemp != null && spUndTemp.getSelectedItem() != null) {
            this.uTemp = spUndTemp.getSelectedItem().toString(); // "�C" o "�F"
        }
        // ==========================================
    }

    private String guardarPlanoComoImagen(View vistaPlano) {
        // 1. Validar que la vista ya tenga dimensiones reales en pantalla
        if (vistaPlano.getWidth() == 0 || vistaPlano.getHeight() == 0) {
            return "";
        }

        // Crear el Bitmap original a partir de la View del plano
        Bitmap bitmapOriginal = Bitmap.createBitmap(vistaPlano.getWidth(), vistaPlano.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapOriginal);
        vistaPlano.draw(canvas);

        // =======================================================================
        // ** NUEVO: Recortar los m�rgenes blancos sobrantes **
        // =======================================================================
        Bitmap bitmap = recortarBordesBlancos(bitmapOriginal);

        // Si por alguna raz�n el recorte falla, usamos el original para no romper la app
        if (bitmap == null) {
            bitmap = bitmapOriginal;
        }

        // 2. Configurar los metadatos de la imagen para MediaStore
        String nombreArchivo = "Plano_Aspersores_" + System.currentTimeMillis() + ".jpg";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, nombreArchivo);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        // Organizar en una carpeta espec�fica dentro de la galer�a p�blica
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SistemaAspersores");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        // 3. Insertar el registro en el MediaStore y obtener la URI
        Uri itemUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        String rutaAbsoluta = "";

        if (itemUri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(itemUri)) {
                if (outputStream != null) {
                    // Comprimir el bitmap RECORTADO a JPEG
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                    outputStream.flush();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.clear();
                        values.put(MediaStore.Images.Media.IS_PENDING, 0);
                        getContentResolver().update(itemUri, values, null, null);
                    }

                    rutaAbsoluta = itemUri.toString();

                    // Copia al almacenamiento interno seguro
                    String rutaInternaPrivada = copiarPlanoAlAlmacenamientoInterno(bitmap, nombreArchivo);

                }
            } catch (Exception e) {
                e.printStackTrace();
                getContentResolver().delete(itemUri, null, null);
            } finally {
                // Liberar AMBOS bitmaps de la memoria
                if (bitmapOriginal != null && !bitmapOriginal.isRecycled()) {
                    bitmapOriginal.recycle();
                }
                if (bitmap != null && !bitmap.isRecycled() && bitmap != bitmapOriginal) {
                    bitmap.recycle();
                }
            }
        }

        return rutaAbsoluta;
    }

    private Bitmap recortarBordesBlancos(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();

        // Inicializar los l�mites en los extremos opuestos
        int minX = width, minY = height, maxX = -1, maxY = -1;

        // Escanear los p�xeles para encontrar el contenido (lo que no sea blanco puro)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src.getPixel(x, y);

                // Si el p�xel NO es blanco (Color.WHITE es 0xFFFFFFFF)
                if (pixel != Color.WHITE) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        // Si no se encontr� nada diferente al blanco o hay un error de l�mites
        if (maxX < minX || maxY < minY) {
            return src;
        }

        // A�adir un peque�o margen de cortes�a (ej. 15 p�xeles) para que no quede pegado al borde
        int margen = 15;
        minX = Math.max(0, minX - margen);
        minY = Math.max(0, minY - margen);
        maxX = Math.min(width - 1, maxX + margen);
        maxY = Math.min(height - 1, maxY + margen);

        int newWidth = maxX - minX + 1;
        int newHeight = maxY - minY + 1;

        // Crear y retornar el nuevo Bitmap recortado
        return Bitmap.createBitmap(src, minX, minY, newWidth, newHeight);
    }

    private String copiarPlanoAlAlmacenamientoInterno(Bitmap bitmap, String nombreArchivo) {
        // Definimos la ruta en el directorio privado de archivos de la App
        java.io.File directorioInterno = new java.io.File(getFilesDir(), "PlanosInternos");

        // Si la carpeta no existe, la creamos
        if (!directorioInterno.exists()) {
            directorioInterno.mkdirs();
        }

        java.io.File archivoDestino = new java.io.File(directorioInterno, nombreArchivo);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(archivoDestino)) {
            // Comprimimos el mismo bitmap en el almacenamiento de la app
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
            fos.flush();

            // Retornamos la ruta absoluta del archivo local
            return archivoDestino.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private void configurarEdgeToEdge() {
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
    }

    private void configurarSpinners() {
        String[] unidades = {"m", "ft"};
        // NUEVO: Unidades de temperatura alineadas al orden (0: M�trico, 1: Imperial)
        String[] unidadesTemp = {"�C", "�F"};

        ArrayAdapter<String> adapterUnd = new ArrayAdapter<>(this, R.layout.item_spinner, unidades);
        ArrayAdapter<String> adapterUndTemp = new ArrayAdapter<>(this, R.layout.item_spinner, unidadesTemp); // <- NUEVO
        ArrayAdapter<String> adapterMetros = new ArrayAdapter<>(this, R.layout.item_spinner, altoMetros);
        ArrayAdapter<String> adapterTempInicial = new ArrayAdapter<>(this, R.layout.item_spinner, tempTechoC); // Calienta motores en �C

        if (spUndFrente != null) spUndFrente.setAdapter(adapterUnd);
        if (spUndFondo != null) spUndFondo.setAdapter(adapterUnd);
        if (spUndAlto != null) spUndAlto.setAdapter(adapterUnd);
        if (spAlto != null) spAlto.setAdapter(adapterMetros);

        // ==========================================
        // NUEVO: ASIGNAR ADAPTADORES DE TEMPERATURA
        // ==========================================
        if (spUndTemp != null) spUndTemp.setAdapter(adapterUndTemp);
        if (spTempTecho != null) spTempTecho.setAdapter(adapterTempInicial);
        // ==========================================

        String[] riesgos = {
                "Leve",
                "Ordinario (Grupo 1)",
                "Ordinario (Grupo 2)",
                "Extra (Grupo 1)",
                "Extra (Grupo 2)"
        };

        if (spClasificacionRiesgo != null) {
            spClasificacionRiesgo.setAdapter(new ArrayAdapter<>(this, R.layout.item_spinner, riesgos));

            spClasificacionRiesgo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    String nuevoRiesgo = spClasificacionRiesgo.getSelectedItem().toString();

                    if (primeraCargaRiesgo) {
                        riesgoSeleccionado = nuevoRiesgo;
                        primeraCargaRiesgo = false;
                        return;
                    }

                    if (!nuevoRiesgo.equals(riesgoSeleccionado)) {
                        riesgoSeleccionado = nuevoRiesgo;
                        limpiarTodoElSistema();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> p) {
                }
            });
        }

        if (spAlto != null) {
            spAlto.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        alto = Double.parseDouble(spAlto.getSelectedItem().toString());
                    } catch (Exception e) {
                        alto = 0.0;
                    }
                    limpiarTodoElSistema();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // ==========================================
        // NUEVO: LISTENER PARA EL SPINNER NUM�RICO DE TEMPERATURA
        // ==========================================
        if (spTempTecho != null) {
            spTempTecho.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        tempTechoValor = Double.parseDouble(spTempTecho.getSelectedItem().toString());
                    } catch (Exception e) {
                        tempTechoValor = 0.0;
                    }
                    // Si cambia el calor del techo, puede cambiar la clasificaci�n del bulbo; limpiamos para forzar recalculo
                    limpiarTodoElSistema();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        // ==========================================

        configurarListenersUnidadesLimpieza();
    }

    private void limpiarTodoElSistema() {
        // 1. Reiniciar variables globales de c�lculo y dimensiones
        this.frente = 0.0;
        this.fondo = 0.0;
        this.alto = 0.0;
        this.area = 0.0;
        this.cantidad = 0;
        this.caudalPorAspersor = 0.0;
        this.caudalTotalSistema = 0.0;
        this.presionMinimaBar = 0.0;
        this.longitud = 0.0;
        this.presionTotal10 = 0.0;
        this.presionTotal40 = 0.0;

        // 2. Limpiar y refrescar el lienzo (Plano)
        if (Plano != null) {
            Plano.setDimensiones(0, 0); // Opcional: resetear dimensiones a cero
            Plano.setSprinklerPoints(null);
            Plano.setEntryPoint(null);
            Plano.invalidate(); // Fuerza a que se dibuje vac�o
        }

        // 3. Limpiar los textos de resultados en la interfaz si existen
        if (tvAspersoresSugeridos != null) {
            tvAspersoresSugeridos.setText("Sugeridos: 0");
        }
        tvFrenteRes.setText("");
        tvFondoRes.setText("");

    }

    private void asignarListenersUnidades() {
        AdapterView.OnItemSelectedListener unidadSincronizadaListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sincronizarUnidades(position);
                actualizarTextosUnidades();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        if (spUndFrente != null) spUndFrente.setOnItemSelectedListener(unidadSincronizadaListener);
        if (spUndFondo != null) spUndFondo.setOnItemSelectedListener(unidadSincronizadaListener);
        if (spUndAlto != null) spUndAlto.setOnItemSelectedListener(unidadSincronizadaListener);
    }

    private void limpiarTodo() {
        if (etLongFrente != null) etLongFrente.setText("");
        if (etLongFondo != null) etLongFondo.setText("");
        if (spUndFrente != null) spUndFrente.setSelection(0);
        if (spUndFondo != null) spUndFondo.setSelection(0);
        if (spClasificacionRiesgo != null) spClasificacionRiesgo.setSelection(0);


        if (Plano != null) {
            Plano.setPlacementMode(false);
            Plano.setEntryPlacementMode(false);
            Plano.limpiarPlano();
        }

        if (tvArea != null) tvArea.setText("0.00 m�");
        if (tvLongitudTuberias != null) tvLongitudTuberias.setText("Longitud Tuber�as: 0.00 m");
        if (tvAspersoresSugeridos != null) tvAspersoresSugeridos.setText("Sugeridos: 0");
    }

    private void mostrarGestoSugerencia() {
        // 1. Aseguramos que ambas sean visibles para poder animarlas
        imaHandRiesgo.setVisibility(View.VISIBLE);
        imaHandLargo.setVisibility(View.VISIBLE);

        // Inicializamos ambas manos con transparencia total (alpha = 0)
        imaHandRiesgo.setAlpha(0f);
        imaHandLargo.setAlpha(0f);

        // ==========================================
        // BLOQUE 1: ANIMACIONES PARA 'Riesgo' (EFECTO PULSANTE)
        // ==========================================
        ObjectAnimator fadeInRiesgo = ObjectAnimator.ofFloat(imaHandRiesgo, "alpha", 0f, 1f);
        fadeInRiesgo.setDuration(300);

        // Animaci�n de escala en X (crece de 1.0 a 1.3 y vuelve a 1.0)
        ObjectAnimator pulsarX = ObjectAnimator.ofFloat(imaHandRiesgo, "scaleX", 1f, 1.3f, 1f);
        pulsarX.setDuration(800);
        pulsarX.setInterpolator(new AccelerateDecelerateInterpolator());
        pulsarX.setRepeatCount(1); // Repite el pulso una vez m�s

        // Animaci�n de escala en Y (debe ser id�ntica a la de X para no deformar la imagen)
        ObjectAnimator pulsarY = ObjectAnimator.ofFloat(imaHandRiesgo, "scaleY", 1f, 1.3f, 1f);
        pulsarY.setDuration(800);
        pulsarY.setInterpolator(new AccelerateDecelerateInterpolator());
        pulsarY.setRepeatCount(1);

        ObjectAnimator fadeOutRiesgo = ObjectAnimator.ofFloat(imaHandRiesgo, "alpha", 1f, 0f);
        fadeOutRiesgo.setDuration(300);

        // Set para reproducir el pulso de escala X e Y al mismo tiempo
        AnimatorSet setPulso = new AnimatorSet();
        setPulso.playTogether(pulsarX, pulsarY);

        // Set secuencial exclusivo para la mano del Riesgo
        AnimatorSet setRiesgo = new AnimatorSet();
        // Primero aparece (fadeIn), luego pulsa (escala X e Y juntas), y al final desaparece (fadeOut)
        setRiesgo.playSequentially(fadeInRiesgo, setPulso, fadeOutRiesgo);

        // Al terminar el set de Riesgo, ocultamos su vista y restauramos su escala por defecto
        setRiesgo.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                imaHandRiesgo.setVisibility(View.GONE);
                // Es buena pr�ctica resetear la escala para cuando se vuelva a ejecutar la animaci�n
                imaHandRiesgo.setScaleX(1f);
                imaHandRiesgo.setScaleY(1f);
            }
        });


        // ==========================================
        // BLOQUE 2: ANIMACIONES PARA 'Largo' (MANTIENE DESPLAZAMIENTO)
        // ==========================================
        ObjectAnimator fadeInLargo = ObjectAnimator.ofFloat(imaHandLargo, "alpha", 0f, 1f);
        fadeInLargo.setDuration(300);

        ObjectAnimator desplazarLargo = ObjectAnimator.ofFloat(imaHandLargo, "translationY", 0f, -100f, 100f, 0f);
        desplazarLargo.setDuration(800);
        desplazarLargo.setInterpolator(new AccelerateDecelerateInterpolator());
        desplazarLargo.setRepeatCount(1);

        ObjectAnimator fadeOutLargo = ObjectAnimator.ofFloat(imaHandLargo, "alpha", 1f, 0f);
        fadeOutLargo.setDuration(300);

        // Set secuencial exclusivo para la mano del Largo
        AnimatorSet setLargo = new AnimatorSet();
        setLargo.playSequentially(fadeInLargo, desplazarLargo, fadeOutLargo);

        // Al terminar el set del Largo, ocultamos su vista definitivamente
        setLargo.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                imaHandLargo.setVisibility(View.GONE);
            }
        });


        // ==========================================
        // COORDENACI�N SECUENCIAL GLOBAL
        // ==========================================
        AnimatorSet setGlobal = new AnimatorSet();
        // Ejecuta primero toda la secuencia del Riesgo (incluyendo el pulso) y luego la del Largo
        setGlobal.playSequentially(setRiesgo, setLargo);

        // Iniciamos la cadena completa
        setGlobal.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.selec) {
            Intent intent = new Intent(this, selec.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.calc) {
            Intent intent = new Intent(this, calc.class);
            startActivity(intent);
        } else if (id == R.id.regis) {
            Intent intent = new Intent(this, regis.class);
            startActivity(intent);
        } else if (id == R.id.policy) {
            Intent intent = new Intent(this, poli.class);
            startActivity(intent);
        } else if (id == R.id.term) {
            Intent intent = new Intent(this, term.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
