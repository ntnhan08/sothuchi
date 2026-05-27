package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
      primary = MatteGold, 
      secondary = ChampagneGold, 
      tertiary = Platinum,
      background = RichBlack,
      surface = CharcoalGray,
      onPrimary = RichBlack,
      onSecondary = CharcoalGray,
      onBackground = PearlWhite,
      onSurface = PearlWhite
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldGreen,
    secondary = RoseGold,
    tertiary = MatteGold,
    background = PearlWhite,
    surface = SilkGray,
    onPrimary = PearlWhite,
    onSecondary = PearlWhite,
    onBackground = Onyx,
    onSurface = Onyx
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to prevent emulator/system monetization crashes
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        try {
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } catch (e: Throwable) {
          if (darkTheme) DarkColorScheme else LightColorScheme
        }
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
