package no.nav.helse.spesialist.api.graphql.mutation

import graphql.execution.DataFetcherResult
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.byggFeilrespons
import no.nav.helse.spesialist.api.graphql.byggRespons
import no.nav.helse.spesialist.api.graphql.graphqlErrorException
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType
import no.nav.helse.spesialist.application.logg.MdcKey
import no.nav.helse.spesialist.application.logg.loggError
import no.nav.helse.spesialist.application.logg.medMdc
import no.nav.helse.spesialist.domain.Dialog
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Kommentar
import no.nav.helse.spesialist.domain.KommentarId
import no.nav.helse.spesialist.domain.NAVIdent
import no.nav.helse.spesialist.domain.Notat
import no.nav.helse.spesialist.domain.NotatId
import no.nav.helse.spesialist.domain.NotatType
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.VedtaksperiodeId
import java.time.LocalDateTime
import java.util.UUID

class NotatMutationHandler(
    private val sessionFactory: SessionFactory,
) : NotatMutationSchema {
    override fun leggTilNotat(
        tekst: String,
        type: ApiNotatType,
        vedtaksperiodeId: String,
        saksbehandlerOid: String,
    ): DataFetcherResult<ApiNotat?> =
        medMdc(MdcKey.VEDTAKSPERIODE_ID to vedtaksperiodeId) {
            sessionFactory.transactionalSessionScope { session ->
                val vedtaksperiode =
                    session.vedtaksperiodeRepository.finn(
                        VedtaksperiodeId(UUID.fromString(vedtaksperiodeId)),
                    )
                medMdc(MdcKey.IDENTITETSNUMMER to vedtaksperiode!!.identitetsnummer.value) {
                    try {
                        val dialog = Dialog.Factory.ny()
                        session.dialogRepository.lagre(dialog)

                        val notat =
                            Notat.Factory.ny(
                                type = type.tilNotatType(),
                                tekst = tekst,
                                dialogRef = dialog.id(),
                                vedtaksperiodeId = UUID.fromString(vedtaksperiodeId),
                                saksbehandlerOid = SaksbehandlerOid(UUID.fromString(saksbehandlerOid)),
                            )
                        session.notatRepository.lagre(notat)

                        notat.utfyllTilApiNotat(session).let(::byggRespons)
                    } catch (exception: Exception) {
                        val feilmelding = "Kunne ikke opprette notat for vedtaksperiode"
                        loggError(feilmelding, exception)
                        byggFeilrespons(graphqlErrorException(500, feilmelding))
                    }
                }
            }
        }

    override fun feilregistrerNotat(id: Int): DataFetcherResult<ApiNotat?> =
        sessionFactory.transactionalSessionScope { session ->
            try {
                val notat =
                    session.notatRepository.finn(NotatId(id))
                        ?: error("Kunne ikke finne notat med id $id")

                medMdc(MdcKey.VEDTAKSPERIODE_ID to notat.vedtaksperiodeId.toString()) {
                    val vedtaksperiode =
                        session.vedtaksperiodeRepository.finn(
                            VedtaksperiodeId(notat.vedtaksperiodeId),
                        )

                    medMdc(MdcKey.IDENTITETSNUMMER to vedtaksperiode!!.identitetsnummer.value) {
                        notat.feilregistrer()

                        session.notatRepository.lagre(notat)

                        notat.utfyllTilApiNotat(session)
                    }
                }.let(::byggRespons)
            } catch (exception: Exception) {
                val feilmelding = "Kunne ikke feilregistrere notat med id $id"
                loggError(feilmelding, exception)
                byggFeilrespons(graphqlErrorException(500, feilmelding))
            }
        }

    override fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): DataFetcherResult<ApiKommentar?> =
        sessionFactory.transactionalSessionScope { session ->
            try {
                val dialog =
                    session.dialogRepository.finn(DialogId(dialogRef.toLong()))
                        ?: error("Kunne ikke finne dialog med id $dialogRef")

                val kommentar =
                    dialog.leggTilKommentar(
                        tekst = tekst,
                        saksbehandlerident = NAVIdent(saksbehandlerident),
                    )

                session.dialogRepository.lagre(dialog)

                dialog.tilApiKommentar(kommentar.id()).let(::byggRespons)
            } catch (exception: Exception) {
                val feilmelding = "Kunne ikke legge til kommentar med dialog-ref: $dialogRef"
                loggError(feilmelding, exception)
                byggFeilrespons(graphqlErrorException(500, feilmelding))
            }
        }

    override fun feilregistrerKommentar(id: Int): DataFetcherResult<ApiKommentar?> =
        sessionFactory.transactionalSessionScope { session ->
            try {
                val kommentarId = KommentarId(id)

                val dialog =
                    session.dialogRepository.finnForKommentar(kommentarId)
                        ?: error("Kunne ikke finne dialog for kommentar med id $id")

                dialog.feilregistrerKommentar(kommentarId)

                session.dialogRepository.lagre(dialog)

                dialog.tilApiKommentar(kommentarId).let(::byggRespons)
            } catch (exception: Exception) {
                val feilmelding = "Kunne ikke feilregistrere kommentar med id $id"
                loggError(feilmelding, exception)
                byggFeilrespons(graphqlErrorException(500, feilmelding))
            }
        }

    override fun feilregistrerKommentarV2(id: Int): DataFetcherResult<ApiKommentar?> = feilregistrerKommentar(id)

    private fun Notat.utfyllTilApiNotat(session: SessionContext) =
        tilApiNotat(
            saksbehandler =
                session.saksbehandlerRepository.finn(saksbehandlerOid)
                    ?: error("Kunne ikke finne saksbehandler med oid $saksbehandlerOid"),
            dialog =
                session.dialogRepository.finn(dialogRef)
                    ?: error("Kunne ikke finne dialog med id $dialogRef"),
        )

    private fun Notat.tilApiNotat(
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

    private fun ApiNotatType.tilNotatType() =
        when (this) {
            ApiNotatType.Retur -> error("NotatType $this støttes ikke lenger")
            ApiNotatType.Generelt -> NotatType.Generelt
            ApiNotatType.PaaVent -> error("NotatType $this støttes ikke lenger")
            ApiNotatType.OpphevStans -> NotatType.OpphevStans
        }

    private fun NotatType.tilApiNotatType() =
        when (this) {
            NotatType.Generelt -> ApiNotatType.Generelt
            NotatType.OpphevStans -> ApiNotatType.OpphevStans
        }

    private fun Dialog.tilApiKommentar(kommentarId: KommentarId) =
        finnKommentar(kommentarId)?.tilApiKommentar()
            ?: error("Kunne ikke finne kommentar med id $kommentarId")

    private fun Kommentar.tilApiKommentar() =
        ApiKommentar(
            id = id().value,
            tekst = tekst,
            opprettet = opprettetTidspunkt.roundToMicroseconds(),
            saksbehandlerident = saksbehandlerident.value,
            feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
        )

    private fun LocalDateTime.roundToMicroseconds(): LocalDateTime = withNano(nano.roundHalfUp(1000))

    private fun Int.roundHalfUp(scale: Int): Int = this - this % scale + if (this % scale >= scale / 2) scale else 0
}
