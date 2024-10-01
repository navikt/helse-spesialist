package no.nav.helse.db

import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import java.util.UUID

interface VedtakRepository {
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
}
