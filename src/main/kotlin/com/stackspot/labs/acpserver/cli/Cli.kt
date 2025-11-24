package com.stackspot.labs.acpserver.cli

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class HelpRequestedException : RuntimeException()

class Command(
    val name: String,
    val description: String,
) {
    private val options = mutableListOf<Option<*>>()

    fun <T> option(
        vararg names: String,
        description: String,
        default: T,
        converter: (String) -> T
    ): ReadOnlyProperty<Any?, T> {
        val opt = Option(names.toList(), description, default, converter)
        options.add(opt)
        return opt
    }

    fun option(
        vararg names: String,
        description: String,
        default: String
    ) = option(*names, description = description, default = default) { it }

    fun parse(args: List<String>) {
        if (args.contains("--help") || args.contains("-h")) {
            throw HelpRequestedException()
        }

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            val opt = options.find { it.names.contains(arg) }
            if (opt != null) {
                if (i + 1 < args.size) {
                    val value = args[i + 1]
                    opt.setValue(value)
                    i += 2
                } else {
                    // Missing value for option, ignore or throw? 
                    // For now, let's ignore or maybe throw if we wanted to be strict.
                    // But given "throwaway" code, let's just skip.
                    i++
                }
            } else {
                // Unknown argument
                i++
            }
        }
    }

    fun printHelp() {
        println("Usage: $name [OPTIONS]")
        println()
        println(description)
        println()
        println("Options:")
        options.forEach { opt ->
            val names = opt.names.joinToString(", ")
            println("  ${names.padEnd(20)} ${opt.description} (default: ${opt.defaultValue})")
        }
        println("  -h, --help           Show this help message")
    }

    private class Option<T>(
        val names: List<String>,
        val description: String,
        val defaultValue: T,
        val converter: (String) -> T
    ) : ReadOnlyProperty<Any?, T> {
        private var value: T? = null

        fun setValue(s: String) {
            value = converter(s)
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            return value ?: defaultValue
        }
    }
}
