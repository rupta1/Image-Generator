package com.rupta.imagegenerator.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.rupta.imagegenerator.data.PreferenceStore

private val violet = Color(0xFFB7A2FF)
private val panel = Color(0xFF191922)

@Composable
fun ImageGeneratorApp() {
    var tab by remember { mutableStateOf(0) }
    var prompt by remember { mutableStateOf("") }
    var negative by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(20f) }
    var size by remember { mutableStateOf("512 × 512") }
    var seed by remember { mutableStateOf("") }
    var learning by remember { mutableStateOf(true) }
    val context = LocalContext.current

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
            0 -> CreateScreen(Modifier.padding(pad), prompt, {prompt=it}, negative, {negative=it}, steps, {steps=it}, size, {size=it}, seed, {seed=it})
            1 -> GalleryScreen(Modifier.padding(pad))
            else -> PersonalizeScreen(Modifier.padding(pad), learning, {learning=it})
        }
    }
}

@Composable
private fun CreateScreen(modifier: Modifier, prompt:String, onPrompt:(String)->Unit, negative:String, onNegative:(String)->Unit, steps:Float, onSteps:(Float)->Unit, size:String, onSize:(String)->Unit, seed:String, onSeed:(String)->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically, modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("IMAGE STUDIO", color= violet, letterSpacing=3.sp, fontSize=11.sp, fontWeight=FontWeight.Bold)
                Text("Create something\nextraordinary.", fontSize=29.sp, fontWeight=FontWeight.Bold, lineHeight=34.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(panel), contentAlignment=Alignment.Center) { Icon(Icons.Default.Bolt, "Local", tint=violet) }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Edit, null, tint=violet); Spacer(Modifier.width(8.dp)); Text("Your prompt", fontWeight=FontWeight.SemiBold) }
                OutlinedTextField(prompt, onPrompt, modifier=Modifier.fillMaxWidth(), minLines=4, placeholder={Text("Describe the image you want to create…")}, shape=RoundedCornerShape(16.dp))
                OutlinedTextField(negative, onNegative, modifier=Modifier.fillMaxWidth(), minLines=2, placeholder={Text("Negative prompt (optional)")}, shape=RoundedCornerShape(16.dp))
                Text("QUICK STYLES", fontSize=11.sp, letterSpacing=2.sp, color=Color.Gray)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Photoreal","Anime","Cinematic","Fantasy").forEach { style ->
                        AssistChip(onClick={onPrompt(if(prompt.isBlank()) style.lowercase()+" style, " else prompt)}, label={Text(style)})
                    }
                }
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Generation settings", fontWeight=FontWeight.SemiBold)
                Text("Image size", color=Color.Gray)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("512 × 512","512 × 768","768 × 512").forEach { value ->
                        FilterChip(selected=size==value, onClick={onSize(value)}, label={Text(value)})
                    }
                }
                Row(verticalAlignment=Alignment.CenterVertically) { Text("Steps", Modifier.weight(1f)); Text(steps.toInt().toString(), color=violet) }
                Slider(value=steps, onValueChange=onSteps, valueRange=10f..30f, steps=19)
                OutlinedTextField(seed, onSeed, modifier=Modifier.fillMaxWidth(), label={Text("Seed (optional)")}, placeholder={Text("Random seed")})
            }
        }
        Button(onClick={}, modifier=Modifier.fillMaxWidth().height(56.dp), shape=RoundedCornerShape(18.dp), colors=ButtonDefaults.buttonColors(containerColor=violet, contentColor=Color.Black)) {
            Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(10.dp)); Text("Generate image", fontWeight=FontWeight.Bold)
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)), shape=RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint=violet)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("On-device AI engine", fontWeight=FontWeight.SemiBold)
                    Text("Inference integration is the next milestone. This UI does not fabricate generation results.", fontSize=12.sp, color=Color.LightGray)
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
                Text("Your images will appear here", fontWeight=FontWeight.SemiBold)
                Text("Local generation history is planned for the next milestone.", color=Color.Gray, fontSize=13.sp)
            }
        }
    }
}

@Composable
private fun PersonalizeScreen(modifier:Modifier, learning:Boolean, onLearning:(Boolean)->Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("PERSONAL AI", color=violet, fontSize=11.sp, letterSpacing=3.sp)
        Text("Learns your style.", fontSize=28.sp, fontWeight=FontWeight.Bold)
        Card(colors=CardDefaults.cardColors(containerColor=panel), shape=RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Adaptive preferences", fontWeight=FontWeight.Bold)
                        Text("Learn from ratings, saves, edits and prompt patterns.", fontSize=13.sp, color=Color.Gray)
                    }
                    Switch(learning, onLearning)
                }
                HorizontalDivider(color=Color.DarkGray)
                LearningItem("Favorite styles", "Inferred from your choices", Icons.Default.Palette)
                LearningItem("Prompt habits", "Local prompt-pattern signals", Icons.Default.TextFields)
                LearningItem("Settings preference", "Aspect ratio and generation controls", Icons.Default.Tune)
                LearningItem("Personal adapter", "Experimental fine-tuning roadmap", Icons.Default.ModelTraining)
            }
        }
        Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF241D35)), shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Privacy by design", fontWeight=FontWeight.Bold)
                Text("Learning is designed to remain on-device. Explicit controls for clearing or exporting learned data will be added with persistent learning.", color=Color.LightGray, fontSize=13.sp)
            }
        }
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
