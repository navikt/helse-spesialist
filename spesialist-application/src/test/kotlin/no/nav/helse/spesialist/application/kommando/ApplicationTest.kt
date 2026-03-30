package no.nav.helse.spesialist.application.kommando

import no.nav.helse.spesialist.application.InMemoryRepositoriesAndDaos
import no.nav.helse.spesialist.application.Outbox

abstract class ApplicationTest {
    private val inMemoryRepositoriesAndDaos = InMemoryRepositoriesAndDaos()
    val sessionContext = inMemoryRepositoriesAndDaos.sessionFactory.sessionContext
    val outbox = Outbox("1.0.0")
}
