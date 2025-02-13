package no.nav.helse.spesialist.api.graphql.mutation

import graphql.GraphQLError
import graphql.GraphqlErrorException
import graphql.execution.DataFetcherResult
import no.nav.helse.db.SessionContext
import no.nav.helse.db.SessionFactory
import no.nav.helse.spesialist.api.graphql.schema.ApiKommentar
import no.nav.helse.spesialist.api.graphql.schema.ApiNotat
import no.nav.helse.spesialist.api.graphql.schema.ApiNotatType
import no.nav.helse.spesialist.modell.Dialog
import no.nav.helse.spesialist.modell.DialogId
import no.nav.helse.spesialist.modell.Kommentar
import no.nav.helse.spesialist.modell.KommentarId
import no.nav.helse.spesialist.modell.Notat
import no.nav.helse.spesialist.modell.NotatId
import no.nav.helse.spesialist.modell.NotatType
import no.nav.helse.spesialist.modell.Saksbehandler
import no.nav.helse.spesialist.modell.SaksbehandlerOid
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class NotatMutationHandler(
    private val sessionFactory: SessionFactory,
) : NotatMutationSchema {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun leggTilNotat(
        tekst: String,
        type: ApiNotatType,
        vedtaksperiodeId: String,
        saksbehandlerOid: String,
    ): DataFetcherResult<ApiNotat?> =
        håndterITransaksjon(
            feilmeldingSupplier = {
                "Kunne ikke opprette notat for vedtaksperiode med id ${UUID.fromString(vedtaksperiodeId)}"
            },
        ) { session ->
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

            notat.utfyllTilApiNotat(session)
        }

    override fun feilregistrerNotat(id: Int): DataFetcherResult<ApiNotat?> =
        håndterITransaksjon(
            feilmeldingSupplier = { "Kunne ikke feilregistrere notat med id $id" },
        ) { session ->
            val notat =
                session.notatRepository.finn(NotatId(id))
                    ?: error("Kunne ikke finne notat med id $id")

            notat.feilregistrer()

            session.notatRepository.lagre(notat)

            notat.utfyllTilApiNotat(session)
        }

    override fun leggTilKommentar(
        dialogRef: Int,
        tekst: String,
        saksbehandlerident: String,
    ): DataFetcherResult<ApiKommentar?> =
        håndterITransaksjon(
            feilmeldingSupplier = { "Kunne ikke legge til kommentar med dialog-ref: $dialogRef" },
        ) { session ->
            val dialog =
                session.dialogRepository.finn(DialogId(dialogRef.toLong()))
                    ?: error("Kunne ikke finne dialog med id $dialogRef")

            val kommentar =
                dialog.leggTilKommentar(
                    tekst = tekst,
                    saksbehandlerident = saksbehandlerident,
                )

            session.dialogRepository.lagre(dialog)

            dialog.tilApiKommentar(kommentar.id())
        }

    override fun feilregistrerKommentar(id: Int): DataFetcherResult<ApiKommentar?> =
        håndterITransaksjon(
            feilmeldingSupplier = { "Kunne ikke feilregistrere kommentar med id $id" },
        ) { session ->
            val kommentarId = KommentarId(id)
            val dialog =
                session.dialogRepository.finnForKommentar(kommentarId)
                    ?: error("Kunne ikke finne dialog for kommentar med id $id")

            dialog.feilregistrerKommentar(kommentarId)

            session.dialogRepository.lagre(dialog)

            dialog.tilApiKommentar(kommentarId)
        }

    override fun feilregistrerKommentarV2(id: Int): DataFetcherResult<ApiKommentar?> = feilregistrerKommentar(id)

    private fun <T> håndterITransaksjon(
        feilmeldingSupplier: () -> String,
        transactionalBlock: (SessionContext) -> T,
    ): DataFetcherResult<T> =
        try {
            sessionFactory.transactionalSessionScope { session ->
                transactionalBlock(session).tilDataFetcherResult()
            }
        } catch (exception: Exception) {
            val feilmelding = feilmeldingSupplier()
            logger.error(feilmelding, exception)
            GraphqlErrorException
                .newErrorException()
                .message(feilmelding)
                .extensions(mapOf("code" to 500))
                .build().tilDataFetcherResult()
        }

    private fun <T> T.tilDataFetcherResult(): DataFetcherResult<T> = DataFetcherResult.newResult<T>().data(this).build()

    private fun <T> GraphQLError.tilDataFetcherResult(): DataFetcherResult<T> = DataFetcherResult.newResult<T>().error(this).build()

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
        dialogRef = dialogRef.value.toInt(), // TODO: Dette vil bli et problem på et tidspunkt!
        tekst = tekst,
        opprettet = opprettetTidspunkt.roundToMicroseconds(),
        saksbehandlerOid = saksbehandlerOid.value,
        saksbehandlerNavn = saksbehandler.navn,
        saksbehandlerEpost = saksbehandler.epost,
        saksbehandlerIdent = saksbehandler.ident,
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
            saksbehandlerident = saksbehandlerident,
            feilregistrert_tidspunkt = feilregistrertTidspunkt?.roundToMicroseconds(),
        )

    private fun LocalDateTime.roundToMicroseconds(): LocalDateTime = withNano(nano.roundHalfUp(1000))

    private fun Int.roundHalfUp(scale: Int): Int = this - this % scale + if (this % scale >= scale / 2) scale else 0
}
