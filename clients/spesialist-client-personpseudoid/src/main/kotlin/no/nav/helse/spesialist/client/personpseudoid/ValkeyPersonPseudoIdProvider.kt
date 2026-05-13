package no.nav.helse.spesialist.client.personpseudoid

import com.github.navikt.tbd_libs.personpseudoid.PersonPseudoIdClient
import com.github.navikt.tbd_libs.personpseudoid.ValkeyConfig
import no.nav.helse.spesialist.application.PersonPseudoId
import no.nav.helse.spesialist.application.PersonPseudoIdProvider
import no.nav.helse.spesialist.domain.Identitetsnummer

class ValkeyPersonPseudoIdProvider(
    configuration: ClientPersonPseudoIdModule.Configuration,
    private val fallbackPersonPseudoIdProvider: PersonPseudoIdProvider,
) : PersonPseudoIdProvider {
    private val client =
        PersonPseudoIdClient(
            ValkeyConfig(
                username = configuration.brukernavn,
                password = configuration.passord,
                connectionString = configuration.connectionString,
            ),
        )

    override fun nyPersonPseudoId(identitetsnummer: Identitetsnummer): PersonPseudoId {
        val id = client.nyPersonPseudoId(identitetsnummer.value)
        return PersonPseudoId.fraString(id.toString())
    }

    override fun hentIdentitetsnummer(personPseudoId: PersonPseudoId): Identitetsnummer? =
        client.finnIdentitetsnummer(personPseudoId.value)?.let { Identitetsnummer.fraString(it) }
            ?: fallbackPersonPseudoIdProvider.hentIdentitetsnummer(personPseudoId)
}
