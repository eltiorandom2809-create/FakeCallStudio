package com.example.fakecallstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalIndication
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class AppPage { DIALER, CONTACTS, SETTINGS, OUTGOING, ACTIVE, ENDED }
private enum class CallStyle(val title: String, val subtitle: String) {
    ANDROID("Android", "Estilo Android clásico"),
    SAMSUNG("Samsung", "Estilo One UI"),
    PIXEL("Pixel", "Estilo Google Pixel"),
    IPHONE("iPhone", "Estilo iOS")
}

private data class DemoContact(val name: String, val number: String, val relation: String, val initials: String)

private val demoContacts = listOf(
    DemoContact("Mamá", "555 010 1001", "Familia", "M"),
    DemoContact("Papá", "555 010 1002", "Familia", "P"),
    DemoContact("Hermano", "555 010 1003", "Familia", "H"),
    DemoContact("Saúl", "555 010 1004", "Amigos", "S"),
    DemoContact("Hermana Liz", "555 010 1005", "Familia", "L")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FakeCallApp() }
    }
}

@Composable
private fun FakeCallApp() {
    var page by remember { mutableStateOf(AppPage.DIALER) }
    var style by remember { mutableStateOf(CallStyle.ANDROID) }
    var number by remember { mutableStateOf("") }
    var callName by remember { mutableStateOf("Número desconocido") }
    var callNumber by remember { mutableStateOf("") }
    var seconds by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var onHold by remember { mutableStateOf(false) }
    var inCallKeypad by remember { mutableStateOf(false) }
    var tones by remember { mutableStateOf("") }
    var overflowExpanded by remember { mutableStateOf(false) }
    var numberError by remember { mutableStateOf(false) }

    val bg = when (style) {
        CallStyle.IPHONE -> Color(0xFF111114)
        CallStyle.SAMSUNG -> Color(0xFF101820)
        CallStyle.PIXEL -> Color(0xFF171A20)
        CallStyle.ANDROID -> Color(0xFF101318)
    }
    val accent = when (style) {
        CallStyle.IPHONE -> Color(0xFF34C759)
        CallStyle.SAMSUNG -> Color(0xFF78A9FF)
        CallStyle.PIXEL -> Color(0xFFB9C7FF)
        CallStyle.ANDROID -> Color(0xFF76D6A4)
    }

    // El cronómetro solo corre en una llamada simulada activa y sigue avanzando en espera.
    LaunchedEffect(page) {
        if (page == AppPage.OUTGOING) {
            delay(2200)
            if (page == AppPage.OUTGOING) page = AppPage.ACTIVE
        }
        while (page == AppPage.ACTIVE) {
            delay(1000)
            seconds += 1
        }
    }

    BackHandler(enabled = page != AppPage.DIALER) {
        page = when (page) {
            AppPage.CONTACTS, AppPage.SETTINGS -> AppPage.DIALER
            AppPage.OUTGOING -> AppPage.DIALER
            AppPage.ACTIVE -> AppPage.ENDED
            AppPage.ENDED -> AppPage.DIALER
            AppPage.DIALER -> AppPage.DIALER
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = Color(0xFF242932))) {
        Surface(modifier = Modifier.fillMaxSize(), color = bg, contentColor = Color.White) {
            when (page) {
                AppPage.DIALER -> DialerPage(
                    number = number,
                    onNumberChange = { newValue ->
                        number = newValue.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }.take(24)
                        numberError = false
                    },
                    onDigit = { digit -> number = (number + digit).take(24); numberError = false },
                    onDelete = { if (number.isNotEmpty()) number = number.dropLast(1); numberError = false },
                    accent = accent,
                    style = style,
                    error = numberError,
                    overflowExpanded = overflowExpanded,
                    onOverflowChange = { overflowExpanded = it },
                    onSettings = { overflowExpanded = false; page = AppPage.SETTINGS },
                    onContacts = { page = AppPage.CONTACTS },
                    onCall = {
                        if (number.isBlank()) {
                            numberError = true
                        } else {
                            callNumber = number.trim()
                            callName = demoContacts.firstOrNull { it.number.filter(Char::isDigit) == number.filter(Char::isDigit) }?.name ?: "Número desconocido"
                            seconds = 0
                            muted = false
                            speaker = false
                            onHold = false
                            inCallKeypad = false
                            tones = ""
                            page = AppPage.OUTGOING
                        }
                    }
                )
                AppPage.CONTACTS -> ContactsPage(
                    accent = accent,
                    onBack = { page = AppPage.DIALER },
                    onSelect = { contact -> number = contact.number; callName = contact.name; numberError = false; page = AppPage.DIALER },
                    onCall = { contact ->
                        number = contact.number
                        callName = contact.name
                        callNumber = contact.number
                        seconds = 0
                        muted = false
                        speaker = false
                        onHold = false
                        inCallKeypad = false
                        tones = ""
                        page = AppPage.OUTGOING
                    }
                )
                AppPage.SETTINGS -> SettingsPage(
                    accent = accent,
                    style = style,
                    onStyle = { style = it },
                    onBack = { page = AppPage.DIALER }
                )
                AppPage.OUTGOING -> CallStatusPage(
                    title = callName,
                    number = callNumber,
                    status = "Llamando…",
                    accent = accent,
                    style = style,
                    outgoing = true,
                    onEnd = { page = AppPage.ENDED },
                    seconds = seconds
                )
                AppPage.ACTIVE -> ActiveCallPage(
                    title = callName,
                    number = callNumber,
                    seconds = seconds,
                    accent = accent,
                    style = style,
                    muted = muted,
                    speaker = speaker,
                    isOnHold = onHold,
                    keypad = inCallKeypad,
                    tones = tones,
                    onMute = { muted = !muted },
                    onSpeaker = { speaker = !speaker },
                    onToggleHold = { onHold = !onHold },
                    onToggleKeypad = { inCallKeypad = !inCallKeypad },
                    onTone = { tones = (tones + it).takeLast(18) },
                    onEnd = { page = AppPage.ENDED }
                )
                AppPage.ENDED -> EndedPage(
                    seconds = seconds,
                    accent = accent,
                    title = callName,
                    number = callNumber,
                    onHome = { page = AppPage.DIALER; seconds = 0; callNumber = ""; tones = "" },
                    onRedial = {
                        number = callNumber
                        seconds = 0
                        muted = false
                        speaker = false
                        onHold = false
                        page = AppPage.OUTGOING
                    }
                )
            }
        }
    }
}

@Composable
private fun DialerPage(
    number: String,
    onNumberChange: (String) -> Unit,
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    accent: Color,
    style: CallStyle,
    error: Boolean,
    overflowExpanded: Boolean,
    onOverflowChange: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onContacts: () -> Unit,
    onCall: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Teléfono", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("FakeCall Studio · ${style.title}", color = Color.LightGray, fontSize = 12.sp)
            }
            Box {
                TextButton(onClick = { onOverflowChange(true) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    Text("⋮", fontSize = 28.sp, color = Color.White, lineHeight = 28.sp)
                }
                DropdownMenu(expanded = overflowExpanded, onDismissRequest = { onOverflowChange(false) }) {
                    DropdownMenuItem(text = { Text("⚙  Configuración") }, onClick = onSettings)
                    DropdownMenuItem(text = { Text("♟  Contactos") }, onClick = { onOverflowChange(false); onContacts() })
                    DropdownMenuItem(text = { Text("ℹ  Acerca de la simulación") }, onClick = { onOverflowChange(false); onSettings() })
                }
            }
        }

        OutlinedTextField(
            value = number,
            onValueChange = onNumberChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 25.sp, textAlign = TextAlign.Center, letterSpacing = 1.sp),
            label = { Text("Escribe o marca un número") },
            placeholder = { Text("Número de teléfono", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            trailingIcon = {
                TextButton(onClick = onDelete, contentPadding = PaddingValues(6.dp)) {
                    Text("⌫", fontSize = 23.sp, color = Color.White)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = Color(0xFF4D5562),
                cursorColor = accent,
                focusedLabelColor = accent
            )
        )
        if (error) Text("Marca un número antes de llamar.", color = Color(0xFFFF7378), fontSize = 13.sp)

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(0.79f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf(
                    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                    listOf("*" to "", "0" to "+", "#" to "")
                ).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        row.forEach { (digit, letters) ->
                            DialKey(digit, letters, Modifier.weight(1f).height(58.dp), accent) { onDigit(digit) }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(0.21f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF252B35))
                        .clickable(onClick = onContacts).padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("☷", fontSize = 27.sp, color = accent)
                    Text("Contactos", fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            PulsingActionButton(
                label = "Llamar",
                symbol = "☎",
                color = Color(0xFF21B45B),
                modifier = Modifier.size(width = 174.dp, height = 58.dp),
                onClick = onCall
            )
        }
        Text("Simulación únicamente · no se realizan llamadas reales", color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DialKey(digit: String, letters: String, modifier: Modifier = Modifier, accent: Color, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "dial key press")
    Column(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale).clip(CircleShape)
            .background(if (pressed) Color(0xFF465364) else Color(0xFF252B35))
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(digit, fontSize = 25.sp, fontWeight = FontWeight.Medium)
        if (letters.isNotEmpty()) Text(letters, fontSize = 8.sp, letterSpacing = 1.4.sp, color = if (pressed) accent else Color.LightGray)
    }
}

@Composable
private fun ContactsPage(
    accent: Color,
    onBack: () -> Unit,
    onSelect: (DemoContact) -> Unit,
    onCall: (DemoContact) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹  Volver", color = accent, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("Contactos", fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Text("Contactos de ejemplo", color = Color.LightGray, fontSize = 13.sp)
        Text("Selecciona uno para llevar su número al marcador.", color = Color.Gray, fontSize = 12.sp)
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(demoContacts) { contact ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFF252B35))
                        .clickable { onSelect(contact) }.padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(49.dp).clip(CircleShape).background(Color(0xFF35465A)), contentAlignment = Alignment.Center) {
                        Text(contact.initials, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text(contact.number, color = Color.LightGray, fontSize = 13.sp)
                        Text(contact.relation, color = accent, fontSize = 11.sp)
                    }
                    TextButton(onClick = { onCall(contact) }) { Text("☎", color = Color(0xFF3BD36F), fontSize = 23.sp) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Los números son ficticios y solo sirven para probar la app.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SettingsPage(accent: Color, style: CallStyle, onStyle: (CallStyle) -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("‹  Volver", color = accent, fontSize = 16.sp) }
            Spacer(Modifier.weight(1f))
            Text("Configuración", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(22.dp))
        Text("Estilo de teléfono", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Elige la apariencia de las pantallas de llamada.", color = Color.LightGray, fontSize = 13.sp)
        Spacer(Modifier.height(14.dp))
        CallStyle.values().forEach { option ->
            val chosen = style == option
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (chosen) accent.copy(alpha = 0.16f) else Color(0xFF252B35))
                    .clickable { onStyle(option) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(if (chosen) accent.copy(alpha = 0.24f) else Color(0xFF3A4350)), contentAlignment = Alignment.Center) {
                    Text(when (option) { CallStyle.ANDROID -> "A"; CallStyle.SAMSUNG -> "S"; CallStyle.PIXEL -> "P"; CallStyle.IPHONE -> "" }, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(option.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(option.subtitle, color = Color.LightGray, fontSize = 12.sp)
                }
                Text(if (chosen) "✓" else "○", color = if (chosen) accent else Color.Gray, fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Color(0xFF3A414C))
        Spacer(Modifier.height(14.dp))
        Text("Sobre FakeCall Studio", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text("Esta app representa visualmente una llamada. No llama a números reales, no usa el micrófono y no necesita permisos telefónicos.", color = Color.LightGray, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun CallStatusPage(
    title: String,
    number: String,
    status: String,
    accent: Color,
    style: CallStyle,
    outgoing: Boolean,
    onEnd: () -> Unit,
    seconds: Int
) {
    val transition = rememberInfiniteTransition(label = "calling animation")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "calling pulse"
    )
    Column(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text("${style.title} · llamada simulada", color = Color.LightGray, fontSize = 13.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(modifier = Modifier.size(130.dp).graphicsLayer(scaleX = pulse, scaleY = pulse).clip(CircleShape).background(Color(0xFF35465A)), contentAlignment = Alignment.Center) {
                Text(title.take(1).uppercase().ifBlank { "?" }, fontSize = 48.sp, fontWeight = FontWeight.Bold)
            }
            Text(title.ifBlank { "Número desconocido" }, fontSize = 28.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(number, color = Color.LightGray, fontSize = 17.sp)
            Text(status, color = accent, fontSize = 17.sp)
            Text("Conectando simulación…", color = Color.Gray, fontSize = 12.sp)
        }
        PulsingActionButton(label = "Cancelar", symbol = "☎", color = Color(0xFFE5484D), modifier = Modifier.size(width = 174.dp, height = 58.dp), onClick = onEnd)
    }
}

@Composable
private fun ActiveCallPage(
    title: String,
    number: String,
    seconds: Int,
    accent: Color,
    style: CallStyle,
    muted: Boolean,
    speaker: Boolean,
    isOnHold: Boolean,
    keypad: Boolean,
    tones: String,
    onMute: () -> Unit,
    onSpeaker: () -> Unit,
    onToggleHold: () -> Unit,
    onToggleKeypad: () -> Unit,
    onTone: (String) -> Unit,
    onEnd: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "active call avatar")
    val pulse by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(animation = tween(1400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
        label = "active avatar pulse"
    )
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text("${style.title} · llamada simulada", color = Color.LightGray, fontSize = 12.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Box(modifier = Modifier.size(98.dp).graphicsLayer(scaleX = pulse, scaleY = pulse).clip(CircleShape).background(Color(0xFF35465A)), contentAlignment = Alignment.Center) {
                Text(title.take(1).uppercase().ifBlank { "?" }, fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
            Text(title.ifBlank { "Número desconocido" }, fontSize = 28.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Text(number, color = Color.LightGray, fontSize = 15.sp)
            Text(formatTime(seconds), fontSize = 22.sp, color = accent, fontWeight = FontWeight.Medium)
            Text(when { isOnHold -> "En espera · cronómetro activo"; muted -> "Micrófono silenciado (demo)"; else -> "Conectada · simulación" }, color = if (isOnHold) Color(0xFFFFD166) else Color.Gray, fontSize = 12.sp)
        }

        if (keypad) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("*", "0", "#")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        row.forEach { digit -> DialKey(digit, "", Modifier.size(width = 67.dp, height = 48.dp), accent) { onTone(digit) } }
                    }
                }
                Text("Tonos enviados (demo): ${tones.ifBlank { "—" }}", color = Color.LightGray, fontSize = 11.sp)
                TextButton(onClick = onToggleKeypad) { Text("Volver a controles") }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CallControl("🎙", if (muted) "Activar micro" else "Silenciar", muted, Modifier.weight(1f), accent, onMute)
                    CallControl("🔊", if (speaker) "Altavoz activo" else "Altavoz", speaker, Modifier.weight(1f), accent, onSpeaker)
                    CallControl("▦", "Teclado", false, Modifier.weight(1f), accent, onToggleKeypad)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CallControl("Ⅱ", if (isOnHold) "Reanudar" else "En espera", isOnHold, Modifier.weight(1f), accent, onToggleHold)
                    CallControl("＋", "Añadir", false, Modifier.weight(1f), accent) { onTone("+") }
                    CallControl("⋯", "Más", false, Modifier.weight(1f), accent) { onTone("·") }
                }
                if (tones.isNotBlank()) Text("Interacción simulada: $tones", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        PulsingActionButton(label = "Finalizar", symbol = "☎", color = Color(0xFFE5484D), modifier = Modifier.size(width = 174.dp, height = 58.dp), onClick = onEnd)
    }
}

@Composable
private fun EndedPage(
    seconds: Int,
    accent: Color,
    title: String,
    number: String,
    onHome: () -> Unit,
    onRedial: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(84.dp).clip(CircleShape).background(Color(0xFF303744)), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 43.sp, color = accent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("Llamada finalizada", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text(title, fontSize = 18.sp)
        Text(number, color = Color.LightGray, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text("Duración simulada: ${formatTime(seconds)}", color = accent, fontSize = 17.sp)
        Spacer(Modifier.height(30.dp))
        Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Volver al marcador") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRedial, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Volver a llamar") }
        Spacer(Modifier.height(24.dp))
        Text("No se realizó ninguna llamada real.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun PulsingActionButton(label: String, symbol: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "action button press")
    Row(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale).clip(RoundedCornerShape(30.dp))
            .background(color).clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
private fun CallControl(
    symbol: String,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    accent: Color,
    onClick: () -> Unit
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "call control press")
        Box(
            modifier = Modifier.fillMaxWidth().height(62.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(RoundedCornerShape(18.dp)).background(if (selected) accent.copy(alpha = 0.34f) else Color(0xFF303744))
                .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Text(symbol, fontSize = 23.sp) }
        Text(label, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 12.sp, color = Color.White)
    }
}

private fun formatTime(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
