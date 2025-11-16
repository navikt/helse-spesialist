package no.nav.helse.spesialist.application

import no.nav.helse.db.VedtakDao
import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.UUID

class UnimplementedVedtakDao : VedtakDao {
    override fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde
    ) {
        TODO("Not yet implemented")
    }

    override fun erAutomatiskGodkjent(utbetalingId: UUID): Boolean {
        TODO("Not yet implemented")
    }

    override fun opprettKobling(vedtaksperiodeId: UUID, hendelseId: UUID) {
        TODO("Not yet implemented")
    }

    override fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde? {
        TODO("Not yet implemented")
    }

    override fun finnOrganisasjonsnummer(vedtaksperiodeId: UUID): String? {
        TODO("Not yet implemented")
    }

    override fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto? {
        TODO("Not yet implemented")
    }

    override fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto
    ) {
        TODO("Not yet implemented")
    }

    override fun lagreOpprinneligSøknadsdato(vedtaksperiodeId: UUID) {
        TODO("Not yet implemented")
    }
}
