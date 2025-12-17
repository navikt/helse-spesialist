package no.nav.helse.kafka

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import no.nav.helse.db.MeldingDao
import no.nav.helse.db.SessionContext
import no.nav.helse.spesialist.application.Outbox
import no.nav.helse.spesialist.application.logg.loggInfo
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Person

class SøknadSendtRiver : TransaksjonellRiver() {
    override fun preconditions(): River.PacketValidation =
        River.PacketValidation {
            it.requireAny(
                "@event_name",
                listOf("sendt_søknad_arbeidsgiver", "sendt_søknad_nav", "sendt_søknad_arbeidsledig", "sendt_søknad_selvstendig"),
            )
        }

    override fun validations() =
        River.PacketValidation {
            it.requireKey("@id", "fnr", "aktorId")
        }

    override fun transaksjonellOnPacket(
        packet: JsonMessage,
        outbox: Outbox,
        transaksjon: SessionContext,
        eventMetadata: EventMetadata,
    ) {
        transaksjon.meldingDao.lagre(
            id = eventMetadata.`@id`,
            json = packet.toJson(),
            meldingtype = MeldingDao.Meldingtype.SØKNAD_SENDT,
        )
        val identitetsnummer = Identitetsnummer.fraString(packet["fnr"].asText())
        if (transaksjon.personRepository.finn(identitetsnummer) != null) {
            loggInfo("Person finnes fra før", "identitetsnummer: ${identitetsnummer.value}")
            return
        }
        val person =
            Person.Factory.ny(
                identitetsnummer,
                packet["aktorId"].asText(),
                null,
                null,
            )
        transaksjon.personRepository.lagre(person)
    }
}
