package com.expensetracker.presentation.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.expensetracker.domain.model.ExpenseCategory
val CategoryColors=ExpenseCategory.values().associate{it.name to Color(it.color)}
private val Dark=darkColorScheme(primary=Color(0xFF00C853),onPrimary=Color(0xFF003822),primaryContainer=Color(0xFF003822),onPrimaryContainer=Color(0xFF9DFFB8),secondary=Color(0xFF00BFA5),background=Color(0xFF0A1410),surface=Color(0xFF0F1A14),surfaceVariant=Color(0xFF1A2E20),onBackground=Color(0xFFE0F2E8),onSurface=Color(0xFFE0F2E8),onSurfaceVariant=Color(0xFFA0BFA8),error=Color(0xFFFF6B6B),outline=Color(0xFF3A5C42))
private val Amoled=darkColorScheme(primary=Color(0xFF00E676),onPrimary=Color(0xFF003822),primaryContainer=Color(0xFF002910),onPrimaryContainer=Color(0xFF9DFFB8),secondary=Color(0xFF00BFA5),background=Color(0xFF000000),surface=Color(0xFF0A0A0A),surfaceVariant=Color(0xFF111111),onBackground=Color(0xFFE0F2E8),onSurface=Color(0xFFE0F2E8),onSurfaceVariant=Color(0xFF90A090),error=Color(0xFFFF5252),outline=Color(0xFF2A3C2A))
private val Light=lightColorScheme(primary=Color(0xFF00875A),onPrimary=Color(0xFFFFFFFF),primaryContainer=Color(0xFFB8F5D0),onPrimaryContainer=Color(0xFF002117),secondary=Color(0xFF00796B),background=Color(0xFFEDF7ED),surface=Color(0xFFF5FBF5),surfaceVariant=Color(0xFFDCEEE0),onBackground=Color(0xFF0D2015),onSurface=Color(0xFF0D2015),onSurfaceVariant=Color(0xFF3D5C46),error=Color(0xFFBA1A1A),outline=Color(0xFF6C9E76))
@Composable fun ExpenseTrackerTheme(darkTheme:Boolean=true,amoledTheme:Boolean=false,content:@Composable()->Unit){MaterialTheme(colorScheme=when{amoledTheme->Amoled;darkTheme->Dark;else->Light},typography=Typography(),content=content)}
