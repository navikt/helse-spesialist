package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.spesialist.application.SaksbehandlerStansRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStans
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansEvent
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansId
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansOpphevetEvent
import no.nav.helse.spesialist.domain.saksbehandlerstans.SaksbehandlerStansOpprettetEvent

class PgSaksbehandlerStansRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    SaksbehandlerStansRepository {
    override fun lagre(saksbehandlerStans: SaksbehandlerStans) {
        val sistePersisterteSekvensnummer =
            asSQL(
                """
                SELECT MAX(sekvensnummer) FROM saksbehandler_stans_events
                WHERE identitetsnummer = :identitetsnummer
                """.trimIndent(),
                "identitetsnummer" to saksbehandlerStans.identitetsnummer.value,
            ).singleOrNull { it.intOrNull(1) }
        if (sistePersisterteSekvensnummer != null) {
            saksbehandlerStans.events.filter { it.metadata.sekvensnummer > sistePersisterteSekvensnummer }
        } else {
            saksbehandlerStans.events
        }.forEach { event ->
            asSQL(
                """
                INSERT INTO saksbehandler_stans_events (
                    saksbehandlerstans_id,
                    sekvensnummer,
                    event_navn,
                    utført_av_saksbehandler_ident,
                    tidspunkt, 
                    identitetsnummer, 
                    begrunnelse
                )
                VALUES (
                    :saksbehandlerstans_id,
                    :sekvensnummer,
                    :event_navn,
                    :utfort_av_saksbehandler_ident,
                    :tidspunkt, 
                    :identitetsnummer, 
                    :begrunnelse
                )
                """.trimIndent(),
                "saksbehandlerstans_id" to saksbehandlerStans.id.value,
                "sekvensnummer" to event.metadata.sekvensnummer,
                "event_navn" to event.tilDBSaksbehandlerStansEvent().name,
                "utfort_av_saksbehandler_ident" to event.metadata.utførtAvSaksbehandlerIdent.value,
                "tidspunkt" to event.metadata.tidspunkt,
                "identitetsnummer" to event.identitetsnummer.value,
                "begrunnelse" to event.begrunnelse,
            ).update()
        }
    }

    override fun finn(identitetsnummer: Identitetsnummer): SaksbehandlerStans? {
        val events =
            asSQL(
                """
                SELECT * FROM saksbehandler_stans_events
                WHERE identitetsnummer = :identitetsnummer
                """.trimIndent(),
                "identitetsnummer" to identitetsnummer.value,
            ).list { it.tilSaksbehandlerStansEvent() }

        return events.takeUnless { it.isEmpty() }?.tilSaksbehandlerStans()
    }

    private fun Row.tilSaksbehandlerStansEvent(): SaksbehandlerStansEvent {
        val metadata =
            SaksbehandlerStansEvent.Metadata(
                saksbehandlerStansId = SaksbehandlerStansId(uuid("saksbehandlerstans_id")),
                sekvensnummer = int("sekvensnummer"),
                utførtAvSaksbehandlerIdent = NAVIdent(string("utført_av_saksbehandler_ident")),
                tidspunkt = instant("tidspunkt"),
            )
        return when (string("event_navn")) {
            "STANS_OPPRETTET" ->
                SaksbehandlerStansOpprettetEvent(
                    metadata = metadata,
                    identitetsnummer = Identitetsnummer.fraString(string("identitetsnummer")),
                    begrunnelse = string("begrunnelse"),
                )
            "STANS_OPPHEVET" ->
                SaksbehandlerStansOpphevetEvent(
                    metadata = metadata,
                    identitetsnummer = Identitetsnummer.fraString(string("identitetsnummer")),
                    begrunnelse = string("begrunnelse"),
                )
            else -> error("Ukjent event_navn: ${string("event_navn")}")
        }
    }

    private fun SaksbehandlerStansEvent.tilDBSaksbehandlerStansEvent() =
        when (this) {
            is SaksbehandlerStansOpprettetEvent -> DBSaksbehandlerStansEventType.STANS_OPPRETTET
            is SaksbehandlerStansOpphevetEvent -> DBSaksbehandlerStansEventType.STANS_OPPHEVET
        }

    private fun List<SaksbehandlerStansEvent>.tilSaksbehandlerStans(): SaksbehandlerStans = SaksbehandlerStans.fraLagring(events = sortedBy { it.metadata.sekvensnummer })
}

private enum class DBSaksbehandlerStansEventType {
    STANS_OPPRETTET,
    STANS_OPPHEVET,
}
