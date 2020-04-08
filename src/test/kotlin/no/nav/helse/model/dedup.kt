package no.nav.helse.model

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val initial = Files.readAllLines(Paths.get("src/main/resources/db/migration/V2__legg_til_enheter.sql"))
    val full = Files.readAllLines(Paths.get("src/main/resources/db/migration/V12__utgåtte_enheter.sql_original"))

    val result = full.filter { !initial.contains(it) }
    result.forEach(::println)
    println(result.size)
}
