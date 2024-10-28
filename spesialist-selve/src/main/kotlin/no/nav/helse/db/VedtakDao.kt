package no.nav.helse.db

import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.VedtaksperiodeDto
import java.util.UUID

interface VedtakDao {
    fun leggTilVedtaksperiodetype(
        vedtaksperiodeId: UUID,
        type: Periodetype,
        inntektskilde: Inntektskilde,
    )

    fun erSpesialsak(vedtaksperiodeId: UUID): Boolean

    fun erAutomatiskGodkjent(utbetalingId: UUID): Boolean

    fun opprettKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    )

    fun fjernKobling(
        vedtaksperiodeId: UUID,
        hendelseId: UUID,
    )

    fun finnInntektskilde(vedtaksperiodeId: UUID): Inntektskilde?

    fun finnOrgnummer(vedtaksperiodeId: UUID): String?

    fun finnVedtaksperiode(vedtaksperiodeId: UUID): VedtaksperiodeDto?

    fun lagreVedtaksperiode(
        fødselsnummer: String,
        vedtaksperiodeDto: VedtaksperiodeDto,
    )

    fun lagreOpprinneligSøknadsdato(vedtaksperiodeId: UUID)

    fun spesialsakFerdigbehandlet(vedtaksperiodeId: UUID): Int

    fun finnVedtaksperiodetype(vedtaksperiodeId: UUID): Periodetype
}
