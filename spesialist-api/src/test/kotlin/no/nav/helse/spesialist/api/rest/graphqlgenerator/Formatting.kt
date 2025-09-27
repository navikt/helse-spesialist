package no.nav.helse.spesialist.api.rest.graphqlgenerator

fun indentation(level: Int): String = (1..level).joinToString(separator = "") { "    " }
