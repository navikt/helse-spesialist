package no.nav.helse.spesialist.api.rest.mapping

import no.nav.helse.spesialist.api.rest.ApiKommentar
import no.nav.helse.spesialist.api.rest.ApiNotat
import no.nav.helse.spesialist.api.rest.ApiNotatType
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import java.time.LocalDateTime

fun Notat.tilApiNotat(
    saksbehandler: Saksbehandler,
    dialog: Dialog,
) = ApiNotat(
    id = id().value,
    // TODO: Dette vil bli et problem på et tidspunkt!
    dialogRef = dialogRef.value.toInt(),
    tekst = tekst,
    opprettet = opprettetTidspunkt.roundToMicroseconds(),
    saksbehandlerOid = saksbehandlerOid.value,
    saksbehandlerNavn = saksbehandler.navn,
    saksbehandlerEpost = saksbehandler.epost,
    saksbehandlerIdent = saksbehandler.ident.value,
    vedtaksperiodeId = vedtaksperiodeId,
    feilregistrert = feilregistrert,
    feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
    type = type.tilApiNotatType(),
    kommentarer = dialog.kommentarer.map { it.tilApiKommentar() },
)

fun NotatType.tilApiNotatType() =
    when (this) {
        NotatType.Generelt -> ApiNotatType.Generelt
        NotatType.OpphevStans -> ApiNotatType.OpphevStans
    }

fun Kommentar.tilApiKommentar() =
    ApiKommentar(
        id = id().value,
        tekst = tekst,
        opprettet = opprettetTidspunkt.roundToMicroseconds(),
        saksbehandlerident = saksbehandlerident.value,
        feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
    )

private fun LocalDateTime.roundToMicroseconds(): LocalDateTime = withNano(nano.roundHalfUp(1000))

private fun Int.roundHalfUp(scale: Int): Int = this - this % scale + if (this % scale >= scale / 2) scale else 0
