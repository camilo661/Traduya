package com.traduya.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.traduya.core.designsystem.R
import com.traduya.core.designsystem.theme.TraduyaTheme

/** Emblema de Traduya: el globo con el teléfono y las letras 阿 y a. */
@Composable
fun TraduyaLogo(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Image(
        painter = painterResource(R.drawable.traduya_emblem),
        contentDescription = "Traduya",
        modifier = modifier.size(size),
    )
}

/** «TRADUYA · INTÉRPRETE UNIVERSAL DE IDIOMAS»; en blanco sobre fondos oscuros. */
@Composable
fun TraduyaName(modifier: Modifier = Modifier, height: Dp = 40.dp, onDark: Boolean = isSystemInDarkTheme()) {
    Image(
        painter = painterResource(if (onDark) R.drawable.traduya_wordmark_white else R.drawable.traduya_wordmark),
        contentDescription = null,
        modifier = modifier.height(height),
    )
}

/** Emblema junto al nombre, para las barras superiores. */
@Composable
fun TraduyaWordmark(modifier: Modifier = Modifier, logoSize: Dp = 40.dp) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
        TraduyaLogo(size = logoSize)
        Spacer(Modifier.width(10.dp))
        TraduyaName(height = logoSize * 0.72f)
    }
}

/** Logo completo (emblema encima del nombre), para la bienvenida y «Acerca de». */
@Composable
fun TraduyaLockup(modifier: Modifier = Modifier, emblemSize: Dp = 120.dp) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TraduyaLogo(size = emblemSize)
        Spacer(Modifier.height(emblemSize * 0.08f))
        TraduyaName(height = emblemSize * 0.42f)
    }
}

@Preview
@Composable
private fun TraduyaLogoPreview() {
    TraduyaTheme { TraduyaLockup() }
}
