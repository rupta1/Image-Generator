package com.rupta.imagegenerator.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.rupta.imagegenerator.data.LocalLearning
import com.rupta.imagegenerator.data.GalleryStore
import com.rupta.imagegenerator.data.SavedCreation
import com.rupta.imagegenerator.data.SignalKind
import com.rupta.imagegenerator.engine.*
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

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
    fun record(kind:SignalKind, style:String?=null) { store.addSignal(prompt,kind,style); signalCount=store.signals().size }
    Scaffold(
        containerColor = Color(0xFF0B0B10),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF121219)) {
                NavigationBarItem(selected = tab==0, onClick={tab=0}, icon={Icon(Icons.Default.AutoAwesome,"Create")}, label={Text("Create")})
                NavigationBarItem(selected = tab==1, onClick={tab=1}, icon={Icon(Icons.Default.Collections,"Gallery")}, label={Text("Gallery")})
                NavigationBarItem(selected = tab==2, onClick={tab=2}, icon={Icon(Icons.Default.Tune,"Learn")}, label={Text("Personalize")})
            }
        }
    ) { pad ->
        when(tab) {
            0 -> CreateScreen(Modifier.padding(pad), prompt, {prompt=it;store.savePrompt(it)}, negative, {negative=it;store.saveNegative(it)}, steps, {steps=it;store.saveSteps(it)}, size, {size=it;store.saveSize(it)}, seed, {seed=it;store.saveSeed(it)}, onSignal={record(it)})
            1 -> GalleryScreen(Modifier.padding(pad))
            else -> PersonalizeScreen(Modifier.padding(pad), learning, {learning=it;store.setLearning(it)}, signalCount, store.profile().preferredStyles, store.profile().promptKeywords, store.profile().negativeSignals, onClear={store.clearLearning();signalCount=0})
        }
    }
}

@Composable
private fun CreateScreen(modifier: Modifier, prompt:String, onPrompt:(String)->Unit, negative:String, onNegative:(String)->Unit, steps:Float, onSteps:(Float)->Unit, size:String, onSize:(String)->Unit, seed:String, onSeed:(String)->Unit, onSignal:(SignalKind)->Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(GenerationProgress(0,0,"")) }
    var status by remember { mutableStateOf("Choose and install a compatible model pack to generate.") }
    var result by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var cancelFlag by remember { mutableStateOf<AtomicBoolean?>(null) }
    var installed by remember { mutableStateOf(ModelCatalog.installed(context)) }
    val model = installed.firstOrNull()
    var importStatus by remember { mutableStateOf("") }
    var importing by remember { mutableStateOf(false) }
    val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !importing) {
            importing = true
            importStatus = "Installing and validating model pack…"
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            ModelPackInstaller.installZip(context, "sd15-onnx-mobile", input)
                        } ?: error("Could not open selected ZIP file.")
                    }
                    installed = ModelCatalog.installed(context)
                    importStatus = "Model pack installed and preflight checks passed."
                } catch (t: Throwable) {
                    importStatus = "Import failed: ${t.message ?: t.javaClass.simpleName}"
                } finally { importing = false }
            }
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("IMAGE STUDIO  /  LOCAL AI", color=violet, letterSpacing=2.sp, fontSize=11.sp, fontWeight=FontWeight.Bold)
                Text("Dream it.\\nCreate it.", fontSize=32.sp, fontWeight=FontWeight.ExtraBold, lineHeight=36.sp)
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
                    listOf("256 × 256","256 × 384","384 × 256").forEach { value -> FilterChip(selected=size==value, onClick={onSize(value)}, label={Text(value)}) }
                }
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Steps", Modifier.weight(1f)); Text(steps.toInt().toString(), color=violet) }
                Slider(value=steps.coerceIn(5f,10f), onValueChange=onSteps, valueRange=5f..10f, steps=4)
                OutlinedTextField(seed, onSeed, modifier=Modifier.fillMaxWidth(), label={Text("Seed (optional)")}, placeholder={Text("Random seed")})
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Local model",fontWeight=FontWeight.SemiBold)
                if(model==null) Text("No model pack installed. Select the SD 1.5 ZIP pack to import it.",color=Color.Gray,fontSize=13.sp)
                else Text("Installed: ${model.descriptor.name}. Import validation passed; session compatibility is checked before inference.",fontSize=13.sp,color=Color.LightGray)
                Button(onClick={ zipPicker.launch(arrayOf("application/zip","application/octet-stream")) }, enabled=!importing, modifier=Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FolderOpen,null); Spacer(Modifier.width(8.dp)); Text(if(importing) "Importing model…" else "Import ONNX model pack (ZIP)")
                }
                if(importStatus.isNotBlank()) Text(importStatus,fontSize=12.sp,color=if(importStatus.startsWith("Import failed")) MaterialTheme.colorScheme.error else Color.Gray)
                Text("The pack is large (about 3.9 GB). Ensure sufficient internal storage is available. Import copies files into app storage.",fontSize=12.sp,color=Color.Gray)
                Text("Model licence/source: ${model?.descriptor?.licenseUrl ?: "Review the model licence before import."}",fontSize=11.sp,color=Color.Gray)
            }
        }
        if(busy) {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)),shape=RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text(progress.message.ifBlank { "Preparing model…" },fontWeight=FontWeight.Medium)
                    LinearProgressIndicator(progress={progress.fraction.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),color=violet,trackColor=Color(0xFF3C344B))
                    Text("${progress.step} / ${progress.totalSteps} steps",fontSize=12.sp,color=Color.Gray)
                    GhostLoadingPreview()
                    OutlinedButton(onClick={cancelFlag?.set(true)},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Close,null);Spacer(Modifier.width(8.dp));Text("Cancel generation") }
                }
            }
        } else {
            Button(enabled=prompt.isNotBlank() && model!=null, onClick={
                val chosen=model
                if(chosen==null) status="No compatible installed model pack was found."
                else {
                    busy=true; result=null; status="Loading model and generating…"
                    val flag=AtomicBoolean(false); cancelFlag=flag
                    val dimensions=size.split("×").map { it.trim().toIntOrNull() ?: 512 }
                    val width=dimensions.getOrElse(0){512}; val height=dimensions.getOrElse(1){512}
                    val parsedSeed=seed.trim().toLongOrNull()
                    scope.launch {
                        try {
                            val bitmap=withContext(Dispatchers.IO) {
                                OnnxDiffusionRuntime.load(context,chosen.directory).use { runtime ->
                                    runtime.generate(GenerationRequest(prompt,negative,width,height,steps.toInt().coerceIn(5,10),parsedSeed),
                                        onProgress={ p -> progress=p },isCancelled={flag.get()})
                                }
                            }
                            result=bitmap; withContext(Dispatchers.IO) { GalleryStore.save(context, bitmap) }; status="Image generated locally · saved to Gallery."
                        } catch(e:CancellationException) { status="Generation cancelled." }
                        catch(t:Throwable) { status="Generation failed: ${t.message ?: t.javaClass.simpleName}" }
                        finally { busy=false;cancelFlag=null }
                    }
                }
            }, modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=violet,contentColor=Color.Black)) {
                Icon(Icons.Default.AutoAwesome,null);Spacer(Modifier.width(10.dp));Text("Generate image",fontWeight=FontWeight.Bold)
            }
        }
        Text(status,fontSize=13.sp,color=if(status.startsWith("Generation failed")) MaterialTheme.colorScheme.error else Color.Gray)
        result?.let { bitmap ->
            Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Generated image",fontWeight=FontWeight.SemiBold)
                    Image(bitmap.asImageBitmap(),contentDescription="Generated image",modifier=Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)))
                    Text("Saved automatically to your local gallery.",fontSize=12.sp,color=Color.Gray)
                }
            }
        }
        Text("Feedback signals",color=Color.Gray,fontSize=12.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
            OutlinedButton(onClick={onSignal(SignalKind.LIKE)},modifier=Modifier.weight(1f)) { Icon(Icons.Default.Favorite,null);Spacer(Modifier.width(4.dp));Text("Like prompt") }
            OutlinedButton(onClick={onSignal(SignalKind.SAVE)},modifier=Modifier.weight(1f)) { Icon(Icons.Default.BookmarkAdd,null);Spacer(Modifier.width(4.dp));Text("Save idea") }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)),shape=RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Memory,null,tint=violet);Spacer(Modifier.width(10.dp))
                Column { Text("On-device AI engine",fontWeight=FontWeight.SemiBold);Text("Generation uses the installed ONNX pack and runs locally; pack compatibility and device performance depend on model format.",fontSize=12.sp,color=Color.LightGray) }
            }
        }
    }
}


@Composable
private fun GhostLoadingPreview() {
    val transition=rememberInfiniteTransition(label="ghost shimmer")
    val phase by transition.animateFloat(0f,1f,animationSpec=infiniteRepeatable(tween(1200,easing=LinearEasing),RepeatMode.Reverse),label="phase")
    val shimmer=Brush.linearGradient(listOf(Color(0xFF232330),Color(0xFF3C3550).copy(alpha=0.4f+phase*0.6f),Color(0xFF232330)))
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(shimmer),contentAlignment=Alignment.Center){Icon(Icons.Default.AutoAwesome,null,tint=violet.copy(alpha=0.3f+phase*0.4f),modifier=Modifier.size(42.dp))}
        Box(Modifier.fillMaxWidth(0.65f).height(12.dp).clip(CircleShape).background(shimmer))
        Box(Modifier.fillMaxWidth(0.9f).height(9.dp).clip(CircleShape).background(shimmer))
    }
}
@Composable
private fun GalleryScreen(modifier:Modifier) {
    val context=LocalContext.current
    var creations by remember { mutableStateOf(GalleryStore.list(context)) }
    var selected by remember { mutableStateOf<SavedCreation?>(null) }
    var images by remember { mutableStateOf<Map<String,android.graphics.Bitmap>>(emptyMap()) }
    LaunchedEffect(creations) { images=withContext(Dispatchers.IO){creations.mapNotNull{entry->GalleryStore.load(entry.file)?.let{entry.file.absolutePath to it}}.toMap()} }
    Column(modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("YOUR COLLECTION",color=violet,fontSize=11.sp,letterSpacing=2.sp);Text("Gallery",fontSize=30.sp,fontWeight=FontWeight.ExtraBold)};IconButton(onClick={creations=GalleryStore.list(context)}){Icon(Icons.Default.Refresh,null,tint=violet)}}
        Text("${creations.size} saved creations · stored on device",color=Color.Gray,fontSize=12.sp)
        if(creations.isEmpty()) Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Default.PhotoLibrary,null,tint=violet,modifier=Modifier.size(58.dp));Text("Your gallery is waiting",fontWeight=FontWeight.Bold);Text("Generated images save here automatically.",color=Color.Gray,fontSize=12.sp)}}
        else LazyVerticalGrid(columns=GridCells.Fixed(2),modifier=Modifier.weight(1f),contentPadding=PaddingValues(bottom=12.dp),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            items(creations,key={it.file.absolutePath}){entry->val bitmap=images[entry.file.absolutePath]
                Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=panel),modifier=Modifier.clickable{selected=entry}){Column{if(bitmap!=null)Image(bitmap.asImageBitmap(),null,modifier=Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp)))else Box(Modifier.fillMaxWidth().aspectRatio(1f).background(panel));Text(java.text.SimpleDateFormat("MMM d · HH:mm",java.util.Locale.getDefault()).format(java.util.Date(entry.createdAt)),modifier=Modifier.padding(10.dp),fontSize=11.sp,color=Color.Gray)}}
            }
        }
    }
    selected?.let { entry ->
        val bitmap = images[entry.file.absolutePath]
        if (bitmap != null) {
            AlertDialog(
                onDismissRequest = { selected = null },
                containerColor = panel,
                title = { Text("Creation") },
                text = { Image(bitmap.asImageBitmap(), contentDescription = "Saved image", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))) },
                confirmButton = { TextButton(onClick = { selected = null }) { Text("Close") } },
                dismissButton = { TextButton(onClick = { GalleryStore.delete(entry.file); selected = null; creations = GalleryStore.list(context) }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
            )
        }
    }
}
@Composable
private fun PersonalizeScreen(modifier:Modifier, learning:Boolean, onLearning:(Boolean)->Unit, count:Int, styles:Map<String,Int>, keywords:Map<String,Int>, negative:Map<String,Int>, onClear:()->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("PERSONAL AI",color=violet,fontSize=11.sp,letterSpacing=3.sp)
        Text("Learns your style.",fontSize=28.sp,fontWeight=FontWeight.Bold)
        Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Adaptive learning",fontWeight=FontWeight.Bold);Text("Collect preference signals locally.",fontSize=13.sp,color=Color.Gray) };Switch(learning,onLearning) }
                HorizontalDivider(color=Color.DarkGray)
                Text("Collected signals: $count",color=violet,fontWeight=FontWeight.Medium)
                PreferenceSection("Liked styles",styles);PreferenceSection("Prompt keywords",keywords);PreferenceSection("Avoid signals",negative)
                OutlinedButton(onClick=onClear,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteOutline,null);Spacer(Modifier.width(8.dp));Text("Clear learned signals") }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Privacy by design",fontWeight=FontWeight.Bold);Text("Preference signals are stored locally. This prototype learns from explicit feedback; actual model fine-tuning is not yet available.",color=Color.LightGray,fontSize=13.sp) }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel),shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { LearningItem("Personal adapter","Experimental fine-tuning roadmap",Icons.Default.Memory);LearningItem("Model updates","Model management is not yet implemented",Icons.Default.CloudDownload) }
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
private fun LearningItem(title:String,subtitle:String,icon:androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF292432)),contentAlignment=Alignment.Center) { Icon(icon,null,tint=violet) }
        Spacer(Modifier.width(12.dp));Column { Text(title,fontWeight=FontWeight.Medium);Text(subtitle,fontSize=12.sp,color=Color.Gray) }
    }
}
