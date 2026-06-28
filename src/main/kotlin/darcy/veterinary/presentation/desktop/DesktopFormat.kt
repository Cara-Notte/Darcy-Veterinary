package darcy.veterinary.presentation.desktop

import java.text.NumberFormat
import java.util.Locale

private val IndonesiaLocale: Locale = Locale("id", "ID")

internal fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(IndonesiaLocale).format(value)

internal fun formatEnumLabel(value: Enum<*>?): String =
    value?.name
        ?.lowercase()
        ?.split('_')
        ?.joinToString(" ") { word -> word.replaceFirstChar { char -> char.uppercase() } }
        ?: "Draft"
