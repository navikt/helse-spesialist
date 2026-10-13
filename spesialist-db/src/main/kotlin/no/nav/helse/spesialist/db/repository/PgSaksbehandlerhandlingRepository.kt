package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.spesialist.application.SaksbehandlerhandlingRepository
import no.nav.helse.spesialist.db.HelseDao.Companion.asSQL
import no.nav.helse.spesialist.db.MedSession
import no.nav.helse.spesialist.db.QueryRunner
import no.nav.helse.spesialist.domain.saksbehandlerhandling.Saksbehandlerhandling

class PgSaksbehandlerhandlingRepository(
    session: Session,
) : QueryRunner by MedSession(session),
    SaksbehandlerhandlingRepository {

    override fun lagre(saksbehandlerhandling: Saksbehandlerhandling) {
        asSQL(
            """
            INSERT INTO saksbehandlerhandling (
                id,
                identitetsnummer,
                handling_id,
                handling_type,
                saksbehandler_ident,
                tidspunkt,
                totrinnsvurdering_id,
                skjaeringstidspunkt,
                behandling_id
            )
            VALUES (
                :id,
                :identitetsnummer,
                :handling_id,
                :handling_type,
                :saksbehandler_ident,
                :tidspunkt,
                :totrinnsvurdering_id,
                :skjaeringstidspunkt,
                :behandling_id
            )
            ON CONFLICT (handling_id) DO NOTHING
            """.trimIndent(),
            "id" to saksbehandlerhandling.id.value,
            "identitetsnummer" to saksbehandlerhandling.identitetsnummer.value,
            "handling_id" to saksbehandlerhandling.handlingId,
            "handling_type" to saksbehandlerhandling.handlingType.name,
            "saksbehandler_ident" to saksbehandlerhandling.saksbehandlerIdent,
            "tidspunkt" to saksbehandlerhandling.tidspunkt,
            "totrinnsvurdering_id" to saksbehandlerhandling.totrinnsvurderingId?.value,
            "skjaeringstidspunkt" to saksbehandlerhandling.skjæringstidspunkt,
            "behandling_id" to saksbehandlerhandling.behandlingId?.value,
        ).update()
    }
}