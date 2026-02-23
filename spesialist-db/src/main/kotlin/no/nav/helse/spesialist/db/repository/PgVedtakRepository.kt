package no.nav.helse.spesialist.db.repository

import kotliquery.Session
import no.nav.helse.spesialist.application.VedtakRepository
import no.nav.helse.spesialist.db.DbQuery
import no.nav.helse.spesialist.db.SessionDbQuery
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Vedtak

class PgVedtakRepository private constructor(
    private val dbQuery: DbQuery,
) : VedtakRepository {
    internal constructor(session: Session) : this(SessionDbQuery(session))

    override fun lagre(vedtak: Vedtak) {
        when (vedtak) {
            is Vedtak.Automatisk -> lagre(vedtak, true)
            is Vedtak.ManueltMedTotrinnskontroll -> lagre(vedtak, false)
            is Vedtak.ManueltUtenTotrinnskontroll -> lagre(vedtak, false)
        }
    }

    private fun lagre(
        vedtak: Vedtak,
        fattetAutomatisk: Boolean,
    ) {
        dbQuery.update(
            """
            INSERT INTO vedtak (behandling_id, fattet_automatisk, saksbehandler_ident, beslutter_ident, tidspunkt, behandlet_av_spleis)
            VALUES (:behandlingId, :fattetAutomatisk, :saksbehandlerIdent, :beslutterIdent, :tidspunkt, :behandletAvSpleis)
            ON CONFLICT (behandling_id) DO UPDATE SET tidspunkt           = excluded.tidspunkt,
                                                      saksbehandler_ident = excluded.saksbehandler_ident,
                                                      beslutter_ident     = excluded.beslutter_ident,
                                                      fattet_automatisk   = excluded.fattet_automatisk,
                                                      behandlet_av_spleis = excluded.behandlet_av_spleis
                                                      
            """,
            "behandlingId" to vedtak.id.value,
            "fattetAutomatisk" to fattetAutomatisk,
            "saksbehandlerIdent" to vedtak.saksbehandlerIdent(),
            "beslutterIdent" to vedtak.beslutterIdent(),
            "tidspunkt" to vedtak.tidspunkt,
            "behandletAvSpleis" to vedtak.behandletAvSpleis,
        )
    }

    private fun Vedtak.saksbehandlerIdent() =
        when (this) {
            is Vedtak.Automatisk -> null
            is Vedtak.ManueltUtenTotrinnskontroll -> saksbehandlerIdent.value
            is Vedtak.ManueltMedTotrinnskontroll -> saksbehandlerIdent.value
        }

    private fun Vedtak.beslutterIdent(): String? = (this as? Vedtak.ManueltMedTotrinnskontroll)?.beslutterIdent?.value

    override fun finn(spleisBehandlingId: SpleisBehandlingId): Vedtak? =
        dbQuery.singleOrNull(
            """
            SELECT fattet_automatisk, saksbehandler_ident, beslutter_ident, tidspunkt, behandlet_av_spleis
            FROM vedtak
            WHERE behandling_id = :behandlingId
            """.trimIndent(),
            "behandlingId" to spleisBehandlingId.value,
        ) { row ->
            val tidspunkt = row.instant("tidspunkt")
            val beslutterIdent = row.stringOrNull("beslutter_ident")
            val behandletAvSpleis = row.boolean("behandlet_av_spleis")
            when {
                row.boolean("fattet_automatisk") -> {
                    Vedtak.Automatisk(spleisBehandlingId, tidspunkt, behandletAvSpleis)
                }

                beslutterIdent != null -> {
                    Vedtak.ManueltMedTotrinnskontroll(
                        spleisBehandlingId,
                        tidspunkt,
                        NAVIdent(row.string("saksbehandler_ident")),
                        NAVIdent(beslutterIdent),
                        behandletAvSpleis,
                    )
                }

                else -> {
                    Vedtak.ManueltUtenTotrinnskontroll(
                        spleisBehandlingId,
                        tidspunkt,
                        NAVIdent(row.string("saksbehandler_ident")),
                        behandletAvSpleis,
                    )
                }
            }
        }
}
