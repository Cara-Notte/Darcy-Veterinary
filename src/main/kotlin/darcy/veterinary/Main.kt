package darcy.veterinary

import darcy.veterinary.application.CliRuntimeFactory

fun main() {
    CliRuntimeFactory.sqlite().consoleUI.start()
}
