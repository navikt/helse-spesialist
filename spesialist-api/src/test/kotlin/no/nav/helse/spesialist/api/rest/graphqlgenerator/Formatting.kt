package no.nav.helse.spesialist.api.rest.graphqlgenerator

fun indentation(level: Int = 1): String = (1..level).joinToString(separator = "") { "    " }

fun String.indentNewlines() = replace("\n", "\n" + indentation())
