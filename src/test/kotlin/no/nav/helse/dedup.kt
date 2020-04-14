package no.nav.helse

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

val pattern = Pattern.compile(".+\\((\\d+),")
fun findNr(input: String) = pattern.matcher(input).let {
    it.find()
    it.group(1).toInt()
}

fun main() {
    val a = Files.readAllLines(Paths.get("src/main/resources/db/migration/V2__legg_til_enheter.sql"))
    val b = Files.readAllLines(Paths.get("src/main/resources/db/migration/V12__utgåtte_enheter.sql"))
    val enheter = (a + b).map(::findNr)
    println(enheter.sorted().distinct())

    val ny = Files.readAllLines(Paths.get("src/main/resources/db/migration/V13__tjenestesteder.sql_original"))
    println(ny.sorted().distinct())

    val result = ny.map { nyEnhet -> nyEnhet to findNr(nyEnhet) }
        .filterNot { enheter.contains(it.second) }
        .sortedBy { it.second }
        .map { it.first }
    //result.forEach(::println)
    //println(result.size)
    result.forEach(::println)
}
