package no.nav.helse

import org.testcontainers.containers.PostgreSQLContainer

internal fun main() {
    val postgres = PostgreSQLContainer<Nothing>("postgres:13").also { it.start() }

    println("${postgres.jdbcUrl} startet")

    while (true) {
        Thread.sleep(1000)
    }
}
