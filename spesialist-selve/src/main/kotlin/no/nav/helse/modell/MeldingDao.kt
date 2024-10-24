package no.nav.helse.modell

import kotliquery.sessionOf
import no.nav.helse.db.MeldingRepository
import no.nav.helse.db.TransactionalMeldingDao
import no.nav.helse.mediator.meldinger.Personmelding
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

internal class MeldingDao(private val dataSource: DataSource) : MeldingRepository {
    override fun lagre(melding: Personmelding) {
        sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).lagre(melding)
        }
    }

    override fun finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId: UUID): Int =
        sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).finnAntallAutomatisertKorrigertSøknad(vedtaksperiodeId)
        }

    override fun erAutomatisertKorrigertSøknadHåndtert(meldingId: UUID): Boolean =
        sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).erAutomatisertKorrigertSøknadHåndtert(meldingId)
        }

    override fun opprettAutomatiseringKorrigertSøknad(
        vedtaksperiodeId: UUID,
        meldingId: UUID,
    ) {
        sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).opprettAutomatiseringKorrigertSøknad(vedtaksperiodeId, meldingId)
        }
    }

    override fun sisteOverstyringIgangsattOmKorrigertSøknad(
        fødselsnummer: String,
        vedtaksperiodeId: UUID,
    ): OverstyringIgangsattKorrigertSøknad? {
        return sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).sisteOverstyringIgangsattOmKorrigertSøknad(fødselsnummer, vedtaksperiodeId)
        }
    }

    internal data class OverstyringIgangsattKorrigertSøknad(
        val periodeForEndringFom: LocalDate,
        val meldingId: String,
        val berørtePerioder: List<BerørtPeriode>,
    )

    internal data class BerørtPeriode(
        val vedtaksperiodeId: UUID,
        val periodeFom: LocalDate,
        val orgnummer: String,
    )

    override fun finnGodkjenningsbehov(meldingId: UUID): Godkjenningsbehov {
        return sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).finnGodkjenningsbehov(meldingId)
        }
    }

    override fun finn(id: UUID): Personmelding? {
        return sessionOf(dataSource).use { session ->
            TransactionalMeldingDao(session).finn(id)
        }
    }
}
