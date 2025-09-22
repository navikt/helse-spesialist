package no.nav.helse.db

import no.nav.helse.modell.person.vedtaksperiode.VedtaksperiodeDto
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.UUID

interface VedtakDao {
    fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    )

    fun erAutomatiskGodkjent(utbetalingId: UUID): Boolean

    fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    )

    fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde?

    fun finnOrganisasjonsnummer(vedtaksperiodeId: UUID): String?

    fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto?

    fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto,
    )

    fun lagreOpprinneligSøknadsdato(vedtaksperiodeId: UUID)
}
