using UnityEngine;
using TMPro;
using UnityEngine.UI;
using UnityEngine.Networking;
using System.Collections;
using System.Text;

public class GeminiChatController : MonoBehaviour
{
    // --- VARIABLES ---

    public LeapCursor cursor;

    [Header("Base de Datos Museo")]
    public TextAsset jsonArchivo; 
    private MuseumCollection inventarioMuseo;

    private string historia = "";

    [Header("Animación")]
    public Animator thotAnimator; 
    public Animator FondoAzulAnimator;
    public Animator FondoBlancoAnimator;

    [Header("Configuración Texto")]
    public float velocidadTexto = 0.01f; 
    private Coroutine corrutinaEscrituraActual; 

    [Header("Control Botones")]
    public GameObject botonApertura;

    [Header("Movimiento Thot")]
    public RectTransform thotTransform;  
    public RectTransform puntoCerrado;
    public RectTransform puntoAbierto;   

    [Header("Avatar Visual")]
    public Image thotSpriteRenderer; 
    public Sprite spriteIdle;     
    public Sprite spriteThinking; 

    [Header("Efectos Visuales")]
    public GameObject efectoExplosion; // Arrastra aquí tu objeto 'Efecto_Magia'
    public float velocidadPanel = 5f;  // Velocidad de subida/bajada
    public RectTransform panelRect;    // Arrastra aquí el 'ChatPanel' (pero el componente RectTransform)
    public Vector2 posicionPanelOculto; // Coordenadas Y donde se esconde (ej: -1080)
    public Vector2 posicionPanelVisible; // Coordenadas Y donde se ve (ej: 0)



    [Header("Configuración Contextual (NPI)")]
    public string ubicacionTotem = "Recepción";
    private string idioma = ""; 

    [Header("Configuración API")]
    public string apiKey = "AIzaSyCSMTerRtELRYwoY-qpYzMqPUtp7yUcDgg"; // Tu API Key
    private string apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    [Header("UI References")]
    public GameObject chatPanel;
    public TextMeshProUGUI thotText;
    public GameObject buttonsContainer;
    public Button[] opcionButtons; 
    public TextMeshProUGUI[] opcionTexts; 

    private bool isChatOpen = false;

    private Coroutine corrutinaPanel;
    private string ultimoMensajeThot = "";
    private string ultimaPregunta = "";

    // SYSTEM PROMPT (Personalidad)
    private string systemPrompt = 
        "ROL: Eres Thot, dios egipcio de la sabiduría y guía de un museo. " +
        "PERSONALIDAD: Solemne pero acogedora. " +
        
        "CONOCIMIENTO (Tu nuevo mapa del museo): " +
        "- Sala Inframundo: El viaje al más allá, juicio de Osiris, momificación y tumbas. " +
        "- Sala Faraones: Reyes, máscaras de oro, sarcófagos y símbolos de poder. " +
        "- Sala Pirámides: Ingeniería. Destacan: Gran Pirámide de Giza, Pirámide Escalonada (Saqqara) y Acodada (Dahshur). " +
        "- Sala Dioses: Mitología, panteón egipcio y creencias. " +
        "- Sala Río Nilo: Agricultura, fauna, inundaciones y vida cotidiana junto al río. " +
        
        "REGLAS DE RESPUESTA (JSON): " +
        "1. Estructura estricta: { \"mensaje\": \"...\", \"opciones\": [...] } " +
        "2. IMPORTANTE: NO TRADUZCAS LAS CLAVES 'mensaje' NI 'opciones'. " + 
        "3. 'mensaje': Máximo 30 palabras. " +
        
        "IMPORTANTE: Adapta tu respuesta al idioma y ubicación indicados.";
    // --- FUNCIONES ---

    void Start()
    {
        chatPanel.SetActive(true);
        if (panelRect != null) panelRect.anchoredPosition = posicionPanelOculto;
        buttonsContainer.SetActive(false);
        
        if (thotTransform != null && puntoCerrado != null)
            thotTransform.position = puntoCerrado.position;

        StartCoroutine(RutinaAnimacionAleatoria());

        // Cargar JSON
        if (jsonArchivo != null)
        {
            inventarioMuseo = JsonUtility.FromJson<MuseumCollection>(jsonArchivo.text);
            Debug.Log("Inventario cargado: " + inventarioMuseo.items.Length + " obras.");
        }
        else
        {
            Debug.LogError("¡FALTA ASIGNAR EL ARCHIVO JSON EN EL INSPECTOR!");
        }
    }

    public void ToggleChat()
    {
        isChatOpen = !isChatOpen;
        if (botonApertura != null) botonApertura.SetActive(!isChatOpen);

        // --- ANIMACIÓN DEL PANEL ---
        if (corrutinaPanel != null) StopCoroutine(corrutinaPanel);

        Vector2 destino = isChatOpen ? posicionPanelVisible : posicionPanelOculto;
        
        // Llamamos a la nueva versión con el segundo parámetro 'isChatOpen'
        corrutinaPanel = StartCoroutine(MoverPanel(destino, isChatOpen));

        if (isChatOpen)
        {
            SincronizarConIdiomaGlobal();
            
            StartCoroutine(ReproducirEfectoMagico());

            foreach (Button boton in opcionButtons)
                cursor.RegistrarBoton(boton);

            // 1. Movimiento y Animación
            if (thotTransform != null && puntoAbierto != null) 
                thotTransform.position = puntoAbierto.position;
            
            if (thotAnimator != null) { thotAnimator.enabled = true; thotAnimator.SetTrigger("AnimarAhora"); }
            if (FondoAzulAnimator != null) { FondoAzulAnimator.enabled = true; FondoAzulAnimator.SetTrigger("Desaparece"); }
            if (FondoBlancoAnimator != null) { FondoBlancoAnimator.enabled = true; FondoBlancoAnimator.SetTrigger("Desaparece"); }

            GlobalStateManager.Instance.PushAction(() => ToggleChat());

            // 2. Generar contexto basado en el JSON (Sala actual)
            string contextoSala = GenerarContextoDinamico();
            
            // 3. Crear el mensaje inicial para la IA
            string promptFinal = $"{contextoSala} [ACCIÓN: El usuario acaba de activar el tótem. Saluda en {idioma} y ofrece ayuda relevante sobre lo que tiene delante.]";

            // 4. Enviar (SOLO UNA VEZ)
            StartCoroutine(EnviarMensajeAGemini(promptFinal));
        }
        else 
        {
            cursor.ClearDinamicos();
            
            StartCoroutine(ReproducirEfectoMagico());

            // Al cerrar, volvemos al sitio
            if (thotTransform != null && puntoCerrado != null) 
                thotTransform.position = puntoCerrado.position;

            if (FondoAzulAnimator != null) { FondoAzulAnimator.enabled = true; FondoAzulAnimator.SetTrigger("Aparece"); }
            if (FondoBlancoAnimator != null) { FondoBlancoAnimator.enabled = true; FondoBlancoAnimator.SetTrigger("Aparece"); }
            
            // Limpiamos memoria al cerrar para la próxima vez
            ultimoMensajeThot = "";
            ultimaPregunta = "";
            historia = "";
        }
    }

    // --- CORRUTINA MEJORADA: GESTIONA ACTIVACIÓN/DESACTIVACIÓN ---
    IEnumerator MoverPanel(Vector2 destino, bool esApertura)
    {
        // 1. SI ESTAMOS ABRIENDO: Encendemos el panel ANTES de moverlo
        if (esApertura)
        {
            chatPanel.SetActive(true);
        }

        // 2. Animación (Bucle de movimiento)
        while (Vector2.Distance(panelRect.anchoredPosition, destino) > 1f)
        {
            panelRect.anchoredPosition = Vector2.Lerp(panelRect.anchoredPosition, destino, Time.deltaTime * velocidadPanel);
            yield return null;
        }
        
        // Aseguramos la posición final exacta
        panelRect.anchoredPosition = destino; 

        // 3. SI ESTAMOS CERRANDO: Apagamos el panel DESPUÉS de moverlo
        if (!esApertura)
        {
            chatPanel.SetActive(false);
        }
    }

    // --- CORRUTINA PARA EL EFECTO MÁGICO ---
    IEnumerator ReproducirEfectoMagico()
    {
        if (efectoExplosion != null)
        {
            efectoExplosion.SetActive(true);
            
            // Esperamos un poco (ej. 0.5s o lo que dure tu animación)
            yield return new WaitForSeconds(0.8f);
            
            efectoExplosion.SetActive(false);
        }
    }

   public void OnOpcionClick(int indiceBoton)
    {
        string textoElegido = opcionTexts[indiceBoton].text;
        string instruccionTono = "";

        // Definir tono según el botón pulsado
        switch (indiceBoton)
        {
            case 0: instruccionTono = " [TONO: Divertido y simple (Niños). NO uses emojis.]"; break;
            case 1: instruccionTono = " [TONO: Narrativo y educativo (General).]"; break;
            case 2: instruccionTono = " [TONO: Académico y técnico (Experto).]"; break;
            case 3: instruccionTono = " [TONO: Solemne y místico (Sorpresa).]"; break;
        }

        // IMPORTANTE: Le recordamos qué obras hay en la sala para que no se le olvide
        string contextoSala = GenerarContextoDinamico();
        string paqueteCompleto = $"{textoElegido} {instruccionTono} [CONTEXTO ACTUAL: {contextoSala} Idioma: {idioma}]";
        
        StartCoroutine(EnviarMensajeAGemini(paqueteCompleto));
    }

    // --- LÓGICA CONTEXTUAL (JSON) ---
    string GenerarContextoDinamico()
    {
        if (inventarioMuseo == null) return "[Error: No hay base de datos del museo]";

        string contexto = "";

        // --- CASO 1: RECEPCIÓN (Orientación General) ---
        if (ubicacionTotem == "Recepción")
        {
            contexto = 
                $"[SITUACIÓN: Estás en la RECEPCIÓN.]\n" +
                $"TU OBJETIVO: Dar la bienvenida y explicar qué se puede ver.\n" +
                $"- Sala del Inframundo (Muerte y Más Allá, al fondo del museo).\n" +
                $"- Sala de los Faraones (Reyes, delante recto después de la sala de las pirámides).\n" +
                $"- Sala de las Pirámides (Giza, Saqqara, Dahshur, delante recto).\n" +
                $"- Sala de Dioses (Mitología, delante gire a la izquierda).\n" +
                $"- Sala del Río Nilo (Vida cotidiana, delante gire a la derecha).\n\n" +

                $"INSTRUCCIONES OBLIGATORIAS PARA LOS BOTONES EN RECEPCIÓN:\n" +
                $"- Opción 1: Pregunta sobre alguna sala (Ej,: ¿Qué salas hay? , Cuéntame más.).\n" +
                $"- Opción 2: Pregunta sobre una recomendación (ej: ¿Qué es lo más famoso?,¿A dónde me recomiendas visitar?).\n" +
                $"- Opción 3: Pregunta sobre orientación (ej: ¿Dónde está la Sala X?, ¿Donde esta el baño?, ¿Donde está el ascensor? ,¿Dónde está la cafetería?, ¿Dónde está la tienda de regalos?).\n" +
                $"- Opción 4: Pregunta sobre la sala anterior solo si es coherente la pregunta (si en la respuesta se menciona una sola sala)(Ej.: ¿Qué hay en la Sala X?), si no pregunta sobre alguna curiosidad "+
                $"Elementos extras del mapa:"+
                $"Baño más cercano: izquierda pasada la sala de los Dioses, Ascensor más cercano: a la izquierda en la recepción"+
                $"Cafetería: a la derecha en la recepción, Tienda de regalos: a la derecha pasada la sala del Río Nilo"+
                $"Indica que para más información consulte el mapa";
        }
        
        // --- CASO 2: DENTRO DE UNA SALA (Inmersión Total) ---
        else
        {
            // 1. Filtramos las obras de esta sala
            string filtroSalaJSON = "";
            if (ubicacionTotem == "Sala de Pirámides") filtroSalaJSON = "Sala 5"; 
            // Añadir aquí más 'else if' cuando configures otras salas

            string obrasEnSala = "";
            foreach (var obra in inventarioMuseo.items)
            {
                if (obra.sala.Contains(filtroSalaJSON))
                {
                    string extra = obra.destacado ? "(DESTACADA)" : "";
                    obrasEnSala += $"- {obra.nombre} ({obra.material}). Temática: {obra.tematica}. {extra}\n";
                }
            }

            contexto = 
                $"[SITUACIÓN: El usuario está DENTRO de la {ubicacionTotem}.]\n" +
                $"TIENE ESTAS OBRAS DELANTE:\n{obrasEnSala}\n" +
                $"En esta sala hay exposición solamente de tres pirámides: La gran pirámide de Giza, la pirámide escalonada (Saqqra) y la piramide Acodada(Dahshur)" + 
                
                $"INSTRUCCIONES OBLIGATORIAS PARA LOS 4 BOTONES (Céntrate SOLO en esta sala):\n" +
                $"- Opción 1 (Curiosidad visual): Pregunta sobre algo llamativo que se ve en las obras presentes (ej: '¿Por qué esa forma?', '¿Es oro?').\n" +
                $"- Opción 2 (Historia/Mito): Pregunta sobre la leyenda o quién construyó esto.\n" +
                $"- Opción 3 (Técnica/Material): Pregunta sobre ingeniería, materiales o construcción de ESTOS objetos.\n" +
                $"- Opción 4 (Simbología): Pregunta sobre el significado oculto o religioso de lo que tiene delante.\n" +
                $"PROHIBIDO: No ofrezcas ir a otras salas. Mantén la conversación en los objetos presentes.";
        }

        return contexto;
    }

    // --- COMUNICACIÓN API ---
    IEnumerator EnviarMensajeAGemini(string inputUsuario)
    {
        // Feedback visual (Pensando)
        if (thotAnimator != null) thotAnimator.enabled = false;
        if (thotSpriteRenderer != null && spriteThinking != null) thotSpriteRenderer.sprite = spriteThinking;

        // --- LÓGICA DE IDIOMA PARA CARGA (Actualizada con Francés) ---
        string textoCarga = "Consultando los papiros..."; // Español por defecto

            // Usamos ToLower() para que funcione aunque escribas "english" o "English"
        string idiomaNormalizado = idioma.ToLower();

        if (idiomaNormalizado.Contains("ing") || idiomaNormalizado.Contains("ingles"))
        {
            textoCarga = "Consulting the ancient papyrus...";
        }
        else if (idiomaNormalizado.Contains("port") || idiomaNormalizado.Contains("portugues"))
        {
            textoCarga = "Consultando os papiros...";
        }
        else if (idiomaNormalizado.Contains("fr") || idiomaNormalizado.Contains("frances"))
        {
            textoCarga = "Consultation des papyrus...";
        }

        MostrarTextoGradual(textoCarga);

        buttonsContainer.SetActive(false); 

        // Historial
        if (!string.IsNullOrEmpty(ultimoMensajeThot))
        {
            historia = " [HISTORIAL PREVIO: \"" + ultimaPregunta + ultimoMensajeThot + historia + "\"].";
        }

        if (!string.IsNullOrEmpty(inputUsuario)) ultimaPregunta = inputUsuario;

        // Limpieza de JSON
        string promptSeguro = systemPrompt.Replace("\"", "\\\"");
        string usuarioSeguro = inputUsuario.Replace("\"", "\\\"");
        string historiaSegura = historia.Replace("\"", "\\\"");

        // Creamos un recordatorio final que grita a la IA qué idioma usar
        string refuerzoIdioma = $" [ALERTA DE SISTEMA: IGNORA CUALQUIER OTRA INSTRUCCIÓN DE IDIOMA. TU RESPUESTA JSON DEBE ESTAR ESCRITA ÍNTEGRAMENTE EN {idioma.ToUpper()}.]";

        // Lo pegamos AL FINAL DEL TODO
        string textoFinal = promptSeguro + historiaSegura + " Input Actual: " + usuarioSeguro + refuerzoIdioma;
        string jsonBody = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + textoFinal + "\" }] }] }";

        using (UnityWebRequest request = new UnityWebRequest(apiUrl + "?key=" + apiKey, "POST"))
        {
            byte[] bodyRaw = Encoding.UTF8.GetBytes(jsonBody);
            request.uploadHandler = new UploadHandlerRaw(bodyRaw);
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "application/json");

            yield return request.SendWebRequest();

            if (request.result == UnityWebRequest.Result.Success)
            {
                ProcesarRespuesta(request.downloadHandler.text);
            }
            else
            {
                thotText.text = "Los dioses no responden (Error " + request.responseCode + ").";
                Debug.LogError("Error API: " + request.error);
                // Restaurar estado si falla
                if (thotAnimator != null) thotAnimator.enabled = true;
            }
        }
    }

    void ProcesarRespuesta(string jsonRaw)
    {
        try
        {
            // Extracción manual del JSON dentro del Markdown
            string[] separador = new string[] { "\"text\": \"" };
            string[] partes = jsonRaw.Split(separador, System.StringSplitOptions.None);
            if (partes.Length < 2) return;

            string contenidoSucio = partes[1];
            int inicioJson = contenidoSucio.IndexOf('{');
            int finJson = contenidoSucio.LastIndexOf('}');

            if (inicioJson == -1 || finJson == -1) 
            {
                MostrarTextoGradual("Error de formato divino.");
                return;
            }

            string jsonLimpio = contenidoSucio.Substring(inicioJson, finJson - inicioJson + 1);
            jsonLimpio = jsonLimpio.Replace("\\n", "").Replace("\\\"", "\"");

            // Extracción de datos
            string mensaje = ExtraerValor(jsonLimpio, "mensaje");
            
            // --- AQUÍ ESTABA TU ERROR DE TEXTO DOBLE (CORREGIDO) ---
            if (string.IsNullOrEmpty(mensaje)) mensaje = "El papiro está en blanco...";
            
            MostrarTextoGradual(mensaje); // Solo animación
            
            if (!string.IsNullOrEmpty(mensaje)) ultimoMensajeThot += mensaje;
            // --------------------------------------------------------

            // Botones
            buttonsContainer.SetActive(true);
            int inicioOpciones = jsonLimpio.IndexOf("\"opciones\":");
            if (inicioOpciones != -1)
            {
                int inicioArray = jsonLimpio.IndexOf("[", inicioOpciones);
                int finArray = jsonLimpio.IndexOf("]", inicioArray);

                if (inicioArray != -1 && finArray != -1)
                {
                    string soloArray = jsonLimpio.Substring(inicioArray + 1, finArray - inicioArray - 1);
                    string[] opciones = soloArray.Replace("\"", "").Split(',');

                    for (int i = 0; i < 4; i++)
                    {
                        if (i < opciones.Length && !string.IsNullOrWhiteSpace(opciones[i]))
                        {
                            opcionButtons[i].gameObject.SetActive(true);
                            opcionTexts[i].text = opciones[i].Trim();
                        }
                        else
                        {
                            opcionButtons[i].gameObject.SetActive(false);
                        }
                    }
                }
            }

            // Restaurar estado visual
            if (thotAnimator != null) thotAnimator.enabled = true;
            if (thotSpriteRenderer != null && spriteIdle != null) thotSpriteRenderer.sprite = spriteIdle;

        }
        catch (System.Exception e)
        {
            if (thotAnimator != null) thotAnimator.enabled = true;
            Debug.LogError("Error Procesando: " + e.Message);
        }
    }

    // --- UTILIDADES ---
    string ExtraerValor(string json, string key)
    {
        string pattern = "\"" + key + "\":";
        int start = json.IndexOf(pattern);
        if (start == -1) return ""; 
        start += pattern.Length;
        int primeraComilla = json.IndexOf("\"", start);
        if (primeraComilla == -1) return "";
        int segundaComilla = json.IndexOf("\"", primeraComilla + 1);
        if (segundaComilla == -1) return "";
        return json.Substring(primeraComilla + 1, segundaComilla - primeraComilla - 1);
    }

    IEnumerator EfectoEscribir(string fraseCompleta)
    {
        thotText.text = ""; 
        foreach (char letra in fraseCompleta)
        {
            thotText.text += letra;
            yield return new WaitForSeconds(velocidadTexto); 
        }
        corrutinaEscrituraActual = null;
    }

    void MostrarTextoGradual(string texto)
    {
        if (corrutinaEscrituraActual != null) StopCoroutine(corrutinaEscrituraActual);
        corrutinaEscrituraActual = StartCoroutine(EfectoEscribir(texto));
    }

    IEnumerator RutinaAnimacionAleatoria()
    {
        while (true) 
        {
            float espera = Random.Range(5f, 10f); 
            yield return new WaitForSeconds(espera);
            if (!isChatOpen && thotAnimator != null) thotAnimator.SetTrigger("AnimarAhora");
        }
    }

    // --- FUNCIÓN PARA SINCRONIZAR CON EL OTRO SCRIPT ---
    void SincronizarConIdiomaGlobal()
    {
        // Leemos la variable estática de tu compañero
        // (Asegúrate de que 'GlobalLanguage' sea accesible)
        string codigo = GlobalLanguage.language; 

        switch (codigo)
        {
            case "es":
                idioma = "Español";
                break;
            case "en":
                idioma = "English";
                break;
            case "fr":
                idioma = "Français";
                break;
            case "pt":
                idioma = "Português";
                break;
            default:
                idioma = "Español"; // Por seguridad
                break;
        }
        
        Debug.Log($"Sincronizado: Global '{codigo}' -> Thot '{idioma}'");
    }
}

// --- CLASES PARA EL JSON ---
[System.Serializable]
public class MuseumCollection
{
    public ObraArte[] items;
}

[System.Serializable]
public class ObraArte
{
    public string nombre;
    public string[] categoria;
    public string epoca;
    public string material;
    public string tematica;
    public string sala;
    public bool destacado;
}