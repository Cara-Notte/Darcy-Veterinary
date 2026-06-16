package darcy.veterinary.presentation.cli

object CliListFormatter {
    fun <T> renderList(
        title: String,
        items: List<T>,
        emptyMessage: String,
        formatter: (T) -> String
    ): List<String> {
        if (items.isEmpty()) return listOf(title, emptyMessage)

        return listOf(title) + items.mapIndexed { index, item ->
            "${index + 1}. ${formatter(item)}"
        }
    }
}
