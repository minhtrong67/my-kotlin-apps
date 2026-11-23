package com.minhtrong67.moody.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.minhtrong67.moody.R
import com.minhtrong67.moody.ui.MainViewModel
import com.minhtrong67.moody.ui.components.ActionButton

@Composable
fun LoginScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val signingIn by vm.signingIn.collectAsStateWithLifecycle()
    val failed by vm.signInFailed.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painterResource(R.mipmap.ic_logo), null,
            Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.login_welcome), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_welcomeSub),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        if (vm.firebaseReady) {
            if (signingIn) CircularProgressIndicator(Modifier.size(32.dp))
            else ActionButton(
                stringResource(R.string.login_google), Icons.Rounded.AccountCircle,
                onClick = { vm.signInWithGoogle(ctx) }, modifier = Modifier.fillMaxWidth().height(52.dp),
            )
            if (failed) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.login_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            Text(
                stringResource(R.string.login_firebaseError),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.login_or), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        ActionButton(
            stringResource(R.string.login_guest), Icons.Rounded.PhoneAndroid,
            onClick = { vm.continueAsGuest() }, tonal = true, modifier = Modifier.fillMaxWidth().height(52.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.login_guestHint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))
        Feature(Icons.Rounded.Bolt, R.string.login_f1_title, R.string.login_f1_text)
        Feature(Icons.Rounded.BarChart, R.string.login_f2_title, R.string.login_f2_text)
        Feature(Icons.Rounded.Lock, R.string.login_f3_title, R.string.login_f3_text)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.login_footer), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Feature(icon: ImageVector, title: Int, text: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
