package com.rupta.imagegenerator.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import com.rupta.imagegenerator.data.LocalLearning
import com.rupta.imagegenerator.data.SignalKind

private val violet = Color(0xFFB7A2FF)
private val panel = Color(0xFF191922)

@Composable
fun ImageGeneratorApp() {
    val context = LocalContext.current
    val store = remember { LocalLearning(context) }
    var tab by remember { mutableStateOf(0) }
    var prompt by remember { mutableStateOf(store.loadPrompt()) }
    var negative by remember { mutableStateOf(store.loadNegative()) }
    var steps by remember { mutableFloatStateOf(store.loadSteps()) }
    var size by remember { mutableStateOf(store.loadSize()) }
    var seed by remember { mutableStateOf(store.loadSeed()) }
    var learning by remember { mutableStateOf(store.learningEnabled()) }
    var signalCount by remember { mutableIntStateOf(store.signals().size) }
    var snackbar by remember { mutableStateOf("") }

    fun record(kind:SignalKind, style:String?=null) {
        store.addSignal(prompt,kind,style); signalCount=store.signals().size
    }
    Scaffold(
        containerColor = Color(0xFF0B0B10),
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121219)) {
                NavigationBarItem(selected = tab==0, onClick={tab=0}, icon={Icon(Icons.Default.AutoAwesome,"Create")}, label={Text("Create")})
                NavigationBarItem(selected = tab==1, onClick={tab=1}, icon={Icon(Icons.Default.Collections,"Gallery")}, label={Text("Gallery")})
                NavigationBarItem(selected = tab==2, onClick={tab=2}, icon={Icon(Icons.Default.Tune,"Learn")}, label={Text("Personalize")})
            }
        }
    ) { pad ->
        when(tab) {
            0 -> CreateScreen(Modifier.padding(pad), prompt, {prompt=it;store.savePrompt(it)}, negative, {negative=it;store.saveNegative(it)}, steps, {steps=it;store.saveSteps(it)}, size, {size=it;store.saveSize(it)}, seed, {seed=it;store.saveSeed(it)}, onSignal={record(it)}, onStatus={snackbar=it})
            1 -> GalleryScreen(Modifier.padding(pad))
            else -> PersonalizeScreen(Modifier.padding(pad), learning, {learning=it;store.setLearning(it)}, signalCount, store.profile().preferredStyles, store.profile().promptKeywords, store.profile().negativeSignals, onClear={store.clearLearning();signalCount=0})
        }
    }
}

@Composable
private fun CreateScreen(modifier: Modifier, prompt:String, onPrompt:(String)->Unit, negative:String, onNegative:(String)->Unit, steps:Float, onSteps:(Float)->Unit, size:String, onSize:(String)->Unit, seed:String, onSeed:(String)->Unit, onSignal:(SignalKind)->Unit, onStatus:(String)->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("IMAGE STUDIO", color=violet, letterSpacing=3.sp, fontSize=11.sp, fontWeight=FontWeight.Bold)
                Text("Create something\nextraordinary.", fontSize=29.sp, fontWeight=FontWeight.Bold, lineHeight=34.sp)
            }
            Box(Modifier.size(46.dp).clip(CircleShape).background(panel), contentAlignment=Alignment.Center) { Icon(Icons.Default.Bolt, "Local", tint=violet) }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, tint=violet); Spacer(Modifier.width(8.dp)); Text("Your prompt", fontWeight=FontWeight.SemiBold) }
                OutlinedTextField(prompt, onPrompt, modifier=Modifier.fillMaxWidth(), minLines=4, placeholder={Text("Describe the image you want to create…")}, shape=RoundedCornerShape(16.dp))
                OutlinedTextField(negative, onNegative, modifier=Modifier.fillMaxWidth(), minLines=2, placeholder={Text("Negative prompt (optional)")}, shape=RoundedCornerShape(16.dp))
                Text("QUICK STYLES", fontSize=11.sp, letterSpacing=2.sp, color=Color.Gray)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp), modifier=Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Photoreal","Anime","Cinematic","Fantasy","Watercolor").forEach { style ->
                        AssistChip(onClick={onPrompt(if(prompt.isBlank()) style.lowercase()+" style, " else prompt+", "+style.lowercase()+" style")}, label={Text(style)})
                    }
                }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Generation settings", fontWeight=FontWeight.SemiBold)
                Text("Image size", color=Color.Gray)
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    listOf("512 × 512","512 × 768","768 × 512").forEach { value ->
                        FilterChip(selected=size==value, onClick={onSize(value)}, label={Text(value)})
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Steps", Modifier.weight(1f)); Text(steps.toInt().toString(), color=violet) }
                Slider(value=steps, onValueChange=onSteps, valueRange=10f..30f, steps=19)
                OutlinedTextField(seed, onSeed, modifier=Modifier.fillMaxWidth(), label={Text("Seed (optional)")}, placeholder={Text("Random seed")})
            }
        }
        Button(onClick={onStatus("The local diffusion engine is not integrated yet. No image was fabricated.")}, modifier=Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=violet, contentColor=Color.Black)) {
            Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("Generate image", fontWeight=FontWeight.Bold)
        }
        Text("Feedback signals",color=Color.Gray,fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            OutlinedButton(onClick={onSignal(SignalKind.LIKE)},modifier=Modifier.weight(1f)) { Icon(Icons.Default.Favorite,null);Spacer(Modifier.width(4.dp));Text("Like prompt") }
            OutlinedButton(onClick={onSignal(SignalKind.SAVE)},modifier=Modifier.weight(1f)) { Icon(Icons.Default.BookmarkAdd,null);Spacer(Modifier.width(4.dp));Text("Save idea") }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)), shape=RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint=violet); Spacer(Modifier.width(10.dp))
                Column {
                    Text("On-device AI engine", fontWeight=FontWeight.SemiBold)
                    Text("Inference integration is in development. This app does not simulate generation.", fontSize=12.sp, color=Color.LightGray)
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(modifier:Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("YOUR GALLERY", color=violet, fontSize=11.sp, letterSpacing=3.sp)
        Text("Creations", fontSize=28.sp, fontWeight=FontWeight.Bold)
        Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
            Column(horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.ImageSearch, null, tint=violet, modifier=Modifier.size(56.dp))
                Text("No generated images yet", fontWeight=FontWeight.SemiBold)
                Text("Once local inference is integrated, images and metadata will be saved here.", color=Color.Gray, fontSize=13.sp)
            }
        }
    }
}

@Composable
private fun PersonalizeScreen(modifier:Modifier, learning:Boolean, onLearning:(Boolean)->Unit, count:Int, styles:Map<String,Int>, keywords:Map<String,Int>, negative:Map<String,Int>, onClear:()->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("PERSONAL AI", color=violet, fontSize=11.sp, letterSpacing=3.sp)
        Text("Learns your style.", fontSize=28.sp, fontWeight=FontWeight.Bold)
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Adaptive learning", fontWeight=FontWeight.Bold); Text("Collect preference signals locally.", fontSize=13.sp, color=Color.Gray) }
                    Switch(learning, onLearning)
                }
                HorizontalDivider(color=Color.DarkGray)
                Text("Collected signals: $count",color=violet,fontWeight=FontWeight.Medium)
                PreferenceSection("Liked styles",styles)
                PreferenceSection("Prompt keywords",keywords)
                PreferenceSection("Avoid signals",negative)
                OutlinedButton(onClick=onClear,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteOutline,null);Spacer(Modifier.width(8.dp));Text("Clear learned signals") }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)), shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Privacy by design", fontWeight=FontWeight.Bold)
                Text("Preference signals are stored locally. This prototype learns from explicit feedback; actual model fine-tuning is not yet available.", color=Color.LightGray, fontSize=13.sp)
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                LearningItem("Personal adapter", "Experimental fine-tuning roadmap", Icons.Default.Memory)
                LearningItem("Model updates", "Model management is not yet implemented", Icons.Default.CloudDownload)
            }
        }
    }
}

@Composable
private fun PreferenceSection(title:String,data:Map<String,Int>) {
    Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,color=Color.Gray,fontSize=12.sp)
        if(data.isEmpty()) Text("No data yet",fontSize=13.sp,color=Color.Gray)
        else data.entries.sortedByDescending { it.value }.take(8).forEach { (k,v) -> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text(k,Modifier.weight(1f),fontSize=13.sp);Text(v.toString(),color=violet) } }
    }
}

@Composable
private fun LearningItem(title:String, subtitle:String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF292432)), contentAlignment=Alignment.Center) { Icon(icon, null, tint=violet) }
        Spacer(Modifier.width(12.dp))
        Column { Text(title, fontWeight=FontWeight.Medium); Text(subtitle, fontSize=12.sp, color=Color.Gray) }
    }
}
