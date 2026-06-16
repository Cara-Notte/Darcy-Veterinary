package darcy.veterinary.presentation.cli

class CliListSelector(private val input: InputReader) {
    fun <T> show(
        title: String,
        items: List<T>,
        emptyMessage: String,
        formatter: (T) -> String
    ) {
        CliListFormatter.renderList(title, items, emptyMessage, formatter).forEach(::println)
    }

    fun <T> choose(
        title: String,
        items: List<T>,
        emptyMessage: String,
        prompt: String,
        formatter: (T) -> String
    ): T? {
        show(title, items, emptyMessage, formatter)
        if (items.isEmpty()) return null

        val selectedIndex = input.choice(prompt, 1..items.size) - 1
        return items[selectedIndex]
    }

    fun <T> chooseOptional(
        title: String,
        items: List<T>,
        emptyMessage: String,
        prompt: String,
        formatter: (T) -> String
    ): T? {
        show(title, items, emptyMessage, formatter)
        if (items.isEmpty()) return null

        println("0. Do not link any item")
        val selectedIndex = input.choice(prompt, 0..items.size)
        return if (selectedIndex == 0) null else items[selectedIndex - 1]
    }
}
