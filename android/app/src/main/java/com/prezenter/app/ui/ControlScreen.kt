package com.prezenter.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.prezenter.app.network.CommandAction
import com.prezenter.app.network.PresenterSession
import com.prezenter.app.service.PresenterService
import kotlin.math.max
import kotlin.math.min

@Composable
fun ControlScreen() {
    val context = LocalContext.current
    var presentationModeOn by remember { mutableStateOf(false) }
    var laserOn by remember { mutableStateOf(false) }
    var trackpadSize by remember { mutableStateOf(IntSize.Zero) }

    fun send(action: String) = PresenterSession.client.sendCommand(action)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Boshqaruv", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Taqdimot rejimi (ekran o'chiq holatda tovush tugmalari)", modifier = Modifier.weight(1f))
            Switch(
                checked = presentationModeOn,
                onCheckedChange = { checked ->
                    presentationModeOn = checked
                    if (checked) PresenterService.start(context) else PresenterService.stop(context)
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(modifier = Modifier.weight(1f), onClick = { send(CommandAction.START_FROM_BEGINNING) }) {
                Text("Boshidan boshlash")
            }
            Button(modifier = Modifier.weight(1f), onClick = { send(CommandAction.START_FROM_CURRENT) }) {
                Text("Joriy slayddan")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { send(CommandAction.PREV) }) {
                Text("← Orqaga")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { send(CommandAction.NEXT) }) {
                Text("Oldinga →")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            onClick = { send(CommandAction.STOP) }
        ) {
            Text("Taqdimotni to'xtatish (Esc)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Ovoz", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { send(CommandAction.VOLUME_DOWN) }) {
                Text("Ovoz −")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = { send(CommandAction.VOLUME_UP) }) {
                Text("Ovoz +")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB00020)),
            onClick = { send(CommandAction.MUTE_TOGGLE) }
        ) {
            Text("Mute (to'liq o'chirish)")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lazer ko'rsatkich", modifier = Modifier.weight(1f))
            Switch(
                checked = laserOn,
                onCheckedChange = { checked ->
                    laserOn = checked
                    PresenterSession.client.sendLaserToggle(checked)
                }
            )
        }

        if (laserOn) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ekranda nuqtani surish uchun quyidagi maydonda barmog'ingizni yuriting",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                    .onSizeChanged { trackpadSize = it }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val size = trackpadSize
                            if (size.width > 0 && size.height > 0) {
                                val relativeX = clamp01(change.position.x / size.width)
                                val relativeY = clamp01(change.position.y / size.height)
                                PresenterSession.client.sendLaserMove(relativeX.toDouble(), relativeY.toDouble())
                            }
                        }
                    }
            ) {}
        }
    }
}

private fun clamp01(value: Float): Float = max(0f, min(1f, value))
