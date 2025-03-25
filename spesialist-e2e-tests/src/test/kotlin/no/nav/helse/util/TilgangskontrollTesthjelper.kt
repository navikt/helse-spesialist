package no.nav.helse.util

import no.nav.helse.spesialist.api.bootstrap.Gruppe
import java.util.UUID

val testEnv = Gruppe.__indreInnhold_kunForTest().values.associateWith { _ -> UUID.randomUUID().toString() }
