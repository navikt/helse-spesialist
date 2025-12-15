package no.nav.helse.spesialist.db.repository

import kotliquery.Row
import kotliquery.Session
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.totrinnsvurdering.TotrinnsvurderingId
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.NAVIdent

class PgTotrinnsvurderingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    TotrinnsvurderingRepository {
    private val overstyringRepository = PgOverstyringRepository(session)

    override fun finn(id: TotrinnsvurderingId): Totrinnsvurdering? =
        asSQL(
            """
            SELECT tv.id,
                   p.fødselsnummer,
                   (SELECT ident FROM saksbehandler WHERE oid = tv.saksbehandler) as saksbehandlers_ident,
                   (SELECT ident FROM saksbehandler WHERE oid = tv.beslutter) as beslutters_ident,
                   tv.tilstand,
                   tv.vedtaksperiode_forkastet,
                   tv.opprettet,
                   tv.oppdatert
            FROM totrinnsvurdering tv
                     INNER JOIN person p on tv.person_ref = p.id
            WHERE tv.id = :id
            """.trimIndent(),
            "id" to id.value,
        ).singleOrNull { it.toTotrinnsvurdering() }

    override fun finnAktivForPerson(fødselsnummer: String): Totrinnsvurdering? =
        asSQL(
            """
            SELECT tv.id,
                   p.fødselsnummer,
                   (SELECT ident FROM saksbehandler WHERE oid = tv.saksbehandler) as saksbehandlers_ident,
                   (SELECT ident FROM saksbehandler WHERE oid = tv.beslutter) as beslutters_ident,
                   tv.tilstand,
                   tv.vedtaksperiode_forkastet,
                   tv.opprettet,
                   tv.oppdatert
            FROM totrinnsvurdering tv
                     INNER JOIN person p on tv.person_ref = p.id
            WHERE p.fødselsnummer = :fodselsnummer
              AND tv.tilstand != 'GODKJENT'
              AND tv.vedtaksperiode_forkastet = false
              order by opprettet desc limit 1
            """.trimIndent(),
            "fodselsnummer" to fødselsnummer,
        ).singleOrNull { it.toTotrinnsvurdering() }

    override fun lagre(totrinnsvurdering: Totrinnsvurdering) {
        if (totrinnsvurdering.harFåttTildeltId()) {
            update(totrinnsvurdering)
        } else {
            insert(totrinnsvurdering).let(::TotrinnsvurderingId).let(totrinnsvurdering::tildelId)
        }

        overstyringRepository.lagre(totrinnsvurdering.overstyringer, totrinnsvurdering.id())
    }

    private fun insert(totrinnsvurdering: Totrinnsvurdering): Long =
        asSQL(
            """
            INSERT INTO totrinnsvurdering (vedtaksperiode_id, saksbehandler, beslutter, person_ref, tilstand, opprettet, oppdatert)
            SELECT :vedtaksperiodeId, (SELECT oid FROM saksbehandler WHERE ident = :saksbehandler), (SELECT oid FROM saksbehandler WHERE ident = :beslutter), p.id, CAST(:tilstand AS totrinnsvurdering_tilstand), :opprettet, null
            FROM person p 
            WHERE p.fødselsnummer = :fodselsnummer
            """.trimIndent(),
            "saksbehandler" to totrinnsvurdering.saksbehandler?.value,
            "beslutter" to totrinnsvurdering.beslutter?.value,
            "fodselsnummer" to totrinnsvurdering.fødselsnummer,
            "tilstand" to totrinnsvurdering.tilstand.name,
            "opprettet" to totrinnsvurdering.opprettet,
        ).updateAndReturnGeneratedKey()

    private fun update(totrinnsvurdering: Totrinnsvurdering) {
        asSQL(
            """
            UPDATE totrinnsvurdering 
            SET saksbehandler       = (SELECT oid FROM saksbehandler WHERE ident = :saksbehandler),
                beslutter           = (SELECT oid FROM saksbehandler WHERE ident = :beslutter),
                tilstand            = CAST(:tilstand AS totrinnsvurdering_tilstand),
                vedtaksperiode_forkastet = :vedtaksperiodeForkastet,
                oppdatert           = :oppdatert
            WHERE id = :id
            """.trimIndent(),
            "id" to totrinnsvurdering.id().value,
            "saksbehandler" to totrinnsvurdering.saksbehandler?.value,
            "beslutter" to totrinnsvurdering.beslutter?.value,
            "tilstand" to totrinnsvurdering.tilstand.name,
            "vedtaksperiodeForkastet" to totrinnsvurdering.vedtaksperiodeForkastet,
            "oppdatert" to totrinnsvurdering.oppdatert,
        ).update()
    }

    private fun Row.toTotrinnsvurdering(): Totrinnsvurdering =
        Totrinnsvurdering.fraLagring(
            id = TotrinnsvurderingId(long("id")),
            fødselsnummer = string("fødselsnummer"),
            saksbehandler = stringOrNull("saksbehandlers_ident")?.let(::NAVIdent),
            beslutter = stringOrNull("beslutters_ident")?.let(::NAVIdent),
            opprettet = localDateTime("opprettet"),
            oppdatert = localDateTimeOrNull("oppdatert"),
            tilstand = enumValueOf(string("tilstand")),
            vedtaksperiodeForkastet = boolean("vedtaksperiode_forkastet"),
            overstyringer = overstyringRepository.finnAktive(TotrinnsvurderingId(long("id"))),
        )
}
