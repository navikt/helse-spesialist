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
import no.nav.helse.spesialist.modell.SaksbehandlerOid
import org.slf4j.LoggerFactory
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
                "Kunne ikke opprette notat for vedtaksperiode med id ${
                    UUID.fromString(
                        vedtaksperiodeId,
                    )
                }"
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

            hentApiNotat(notat.id(), session)
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

            hentApiNotat(notat.id(), session)
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

            hentKommentarApi(kommentar.id(), DialogId(dialogRef.toLong()), session)
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

            hentKommentarApi(kommentarId, dialog.id(), session)
        }

    override fun feilregistrerKommentarV2(id: Int): DataFetcherResult<ApiKommentar?> = feilregistrerKommentar(id)

    private fun hentApiNotat(
        notatId: NotatId,
        session: SessionContext,
    ): ApiNotat {
        val notat =
            session.notatRepository.finn(notatId)
                ?: error("Kunne ikke finne notat med id $notatId")

        val dialog =
            session.dialogRepository.finn(notat.dialogRef)
                ?: error("Kunne ikke finne dialog med id ${notat.dialogRef}")

        val saksbehandler =
            session.saksbehandlerRepository.finn(notat.saksbehandlerOid)
                ?: error("Kunne ikke finne saksbehandler med oid ${notat.saksbehandlerOid}")

        return ApiNotat(
            id = notat.id().value,
            dialogRef = notat.dialogRef.value.toInt(), // TODO: Dette vil bli et problem på et tidspunkt!
            tekst = notat.tekst,
            opprettet = notat.opprettetTidspunkt,
            saksbehandlerOid = notat.saksbehandlerOid.value,
            saksbehandlerNavn = saksbehandler.navn,
            saksbehandlerEpost = saksbehandler.epost,
            saksbehandlerIdent = saksbehandler.ident,
            vedtaksperiodeId = notat.vedtaksperiodeId,
            feilregistrert = notat.feilregistrert,
            feilregistrert_tidspunkt = notat.feilregistrertTidspunkt,
            type = notat.type.tilApiNotatType(),
            kommentarer = dialog.kommentarer.map { it.tilApiKommentar() },
        )
    }

    private fun NotatType.tilApiNotatType() =
        when (this) {
            NotatType.Generelt -> ApiNotatType.Generelt
            NotatType.OpphevStans -> ApiNotatType.OpphevStans
        }

    private fun ApiNotatType.tilNotatType() =
        when (this) {
            ApiNotatType.Retur -> error("NotatType $this støttes ikke lenger")
            ApiNotatType.Generelt -> NotatType.Generelt
            ApiNotatType.PaaVent -> error("NotatType $this støttes ikke lenger")
            ApiNotatType.OpphevStans -> NotatType.OpphevStans
        }

    private fun hentKommentarApi(
        kommentarId: KommentarId,
        dialogId: DialogId,
        session: SessionContext,
    ): ApiKommentar {
        val dialog =
            session.dialogRepository.finn(dialogId)
                ?: error("Kunne ikke finne dialog med id $dialogId")

        val kommentar =
            dialog.finnKommentar(kommentarId)
                ?: error("Kunne ikke finne kommentar med id $kommentarId")

        return kommentar.tilApiKommentar()
    }

    private fun Kommentar.tilApiKommentar() =
        ApiKommentar(
            id = id().value,
            tekst = tekst,
            opprettet = opprettetTidspunkt,
            saksbehandlerident = saksbehandlerident,
            feilregistrert_tidspunkt = feilregistrertTidspunkt,
        )

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
}
