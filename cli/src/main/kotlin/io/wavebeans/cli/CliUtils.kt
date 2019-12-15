package io.wavebeans.cli

import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options

fun <T> CommandLine.getRequired(option: Option, converter: (String) -> T): T {
    return this.get(option = option, converter = converter, forceToRequire = true)!!
}

fun <T> CommandLine.get(option: Option, forceToRequire: Boolean = false, converter: (String) -> T): T? {
    val v = this.options
            .firstOrNull { it == option }
            ?.let {
                val valuesList = it.valuesList
                if (option.hasArgs() && valuesList.isEmpty()) {
                    throw IllegalArgumentException("${option.argName} should have value")
                } else {
                    val s = valuesList.first().toString()
                    try {
                        converter(s)
                    } catch (e: Throwable) {
                        throw IllegalArgumentException("Can't convert `${option.argName}`: $s", e)
                    }
                }
            }
    require(!((option.isRequired || forceToRequire) && v == null)) {
        "`${option.longOpt ?: option.opt}` is not specified but required"
    }
    return v

}

fun Options.of(vararg o: Option): Options {
    o.forEach { this.addOption(it) }
    return this
}

fun Option.required(): Option {
    this.isRequired = true
    return this
}

fun CommandLine.has(o: Option): Boolean {
    return this.hasOption(o.opt ?: o.longOpt)
}
