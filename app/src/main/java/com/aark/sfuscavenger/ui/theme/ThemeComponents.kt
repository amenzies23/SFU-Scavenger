package com.aark.sfuscavenger.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object ScavengerSpacing {
    val screen = 16.dp
    val section = 24.dp
    val item = 12.dp
    val cardPadding = 16.dp
}

object ScavengerShapes {
    val CardXLarge: CornerBasedShape = RoundedCornerShape(24.dp)
    val CardLarge: CornerBasedShape = RoundedCornerShape(18.dp)
    val CardMedium: CornerBasedShape = RoundedCornerShape(12.dp)
    val Button: CornerBasedShape = RoundedCornerShape(16.dp)
}

object ScavengerButtonDefaults {
    val shape = ScavengerShapes.Button

    @Composable
    fun primaryColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )

    @Composable
    fun secondaryColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    )

    @Composable
    fun tonalColors() = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
}

fun Modifier.scavengerScreenBackground(): Modifier = background(ScavengerBackgroundBrush)

@Composable
fun ScavengerSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        action?.invoke()
    }
}

@Composable
fun ScavengerCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = ScavengerShapes.CardLarge,
    padding: PaddingValues = PaddingValues(ScavengerSpacing.cardPadding),
    tonalElevation: Dp = 0.dp,
    includeBorder: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = tonalElevation,
        border = if (includeBorder) BorderStroke(1.dp, outline) else null
    ) {
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

@Composable
fun ScavengerPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ScavengerButtonDefaults.primaryColors(),
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ScavengerButtonDefaults.shape,
        colors = colors
    ) {
        leadingIcon?.let {
            it()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
        trailingIcon?.let {
            Spacer(modifier = Modifier.width(8.dp))
            it()
        }
    }
}

@Composable
fun ScavengerSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ScavengerButtonDefaults.shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun ScavengerTonalButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ScavengerButtonDefaults.shape,
        colors = ScavengerButtonDefaults.tonalColors()
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ScavengerDangerButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    ScavengerPrimaryButton(
        text = text,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.error,
            contentColor = colors.onError
        ),
        onClick = onClick
    )
}

