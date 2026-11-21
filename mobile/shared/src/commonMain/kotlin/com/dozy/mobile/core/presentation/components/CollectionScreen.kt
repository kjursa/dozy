package com.dozy.mobile.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dozy.mobile.core.presentation.layout.mainBottomMenuScrollSpace
import com.dozy.mobile.resources.Res
import com.dozy.mobile.resources.back
import com.dozy.mobile.resources.collection_empty_eyebrow
import com.dozy.mobile.ui.common.DozyActionButton
import com.dozy.mobile.ui.theme.DozyColors
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CollectionScreen(
    toolbarTitle: String,
    eyebrow: String,
    title: String,
    intro: String,
    addDescription: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean,
    addEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 22.dp, top = 12.dp, end = 22.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CollectionIconButton(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back), onBack)
                Text(toolbarTitle.uppercase(), style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Black,
                ))
                CollectionIconButton(Icons.Default.Add, addDescription, onAdd, addEnabled)
            }
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 8.dp)) {
                Eyebrow(eyebrow)
                Spacer(Modifier.height(12.dp))
                Text(title, style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = (-1).sp))
                Text(
                    intro,
                    modifier = Modifier.padding(top = 16.dp, bottom = 28.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                content()
            }
            Spacer(Modifier.height(mainBottomMenuScrollSpace()))
        }
    }
}

@Composable
private fun CollectionIconButton(icon: ImageVector, description: String, onClick: () -> Unit, enabled: Boolean = true) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, description, Modifier.size(21.dp)) }
    }
}

@Composable
internal fun CollectionEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    action: String,
    onAdd: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(175.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(175.dp).background(Color(0x55E2F0E7), CircleShape))
            Box(Modifier.size(145.dp).background(Color(0xFFE2F0E7), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
            }
            Icon(Icons.Default.AutoAwesome, null, Modifier.align(Alignment.TopEnd).size(32.dp), tint = DozyColors.brandCoral)
        }
        Spacer(Modifier.height(18.dp))
        Eyebrow(stringResource(Res.string.collection_empty_eyebrow))
        Text(
            title,
            Modifier.padding(vertical = 15.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 34.sp),
            textAlign = TextAlign.Center,
        )
        Text(
            description,
            Modifier.padding(bottom = 24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 26.sp),
            textAlign = TextAlign.Center,
        )
        DozyActionButton(onClick = onAdd, enabled = enabled) {
            Text(action, fontWeight = FontWeight.Black)
            Icon(Icons.Default.Add, null, Modifier.padding(start = 12.dp).size(20.dp))
        }
    }
}
