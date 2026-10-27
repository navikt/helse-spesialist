package no.nav.helse.spesialist.db.repository

import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelser
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserDag
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserId
import no.nav.helse.spesialist.domain.andreytelser.GraderteAndreYtelserType
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagIdentitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PgGraderteAndreYtelserRepositoryTest : AbstractDBIntegrationTest() {
    private val repository = sessionContext.graderteAndreYtelserRepository
    private val saksbehandlerIdent = lagSaksbehandler().ident

    @Test
    fun `kan lagre og hente graderte andre ytelser`() {
        val id = lagId()
        val dager =
            listOf(
                dag(1 jan 2024, mapOf(GraderteAndreYtelserType.PLEIEPENGER to 50)),
                dag(
                    2 jan 2024,
                    mapOf(GraderteAndreYtelserType.PLEIEPENGER to 40, GraderteAndreYtelserType.OMSORGSPENGER to 20),
                    sykdomsgrad = 40,
                    arbeidsgrad = 0,
                ),
            )
        val graderteAndreYtelser = ny(id, dager)

        repository.lagre(graderteAndreYtelser)

        val hentet = repository.finnOrNull(id)
        assertNotNull(hentet)
        assertEquals(id, hentet.id)
        assertEquals(dager, hentet.dager)
        assertEquals(graderteAndreYtelser.versjon, hentet.versjon)
        assertEquals(graderteAndreYtelser.events.map { it.dager }, hentet.events.map { it.dager })
        assertEquals("notat til beslutter", hentet.events.single().notatTilBeslutter)
        assertEquals(saksbehandlerIdent, hentet.events.single().utførtAvSaksbehandlerIdent)
        assertEquals(listOf(id), repository.finnAlleForIdentitetsnummer(id.identitetsnummer).map { it.id })
    }

    @Test
    fun `endringer lagres som nye events og spilles av i riktig rekkefølge`() {
        val id = lagId()
        val graderteAndreYtelser = ny(id, listOf(dag(1 jan 2024), dag(2 jan 2024)))
        repository.lagre(graderteAndreYtelser)

        val hentet = repository.finnOrNull(id)!!
        hentet.endre(listOf(dag(2 jan 2024, sykdomsgrad = 10)), saksbehandlerIdent, "endring 1")
        hentet.endre(listOf(dag(2 jan 2024, sykdomsgrad = 20), dag(3 jan 2024)), saksbehandlerIdent, "endring 2")
        repository.lagre(hentet)

        val hentetIgjen = repository.finnOrNull(id)
        assertNotNull(hentetIgjen)
        assertEquals(listOf(dag(1 jan 2024), dag(2 jan 2024, sykdomsgrad = 20), dag(3 jan 2024)), hentetIgjen.dager)
        assertEquals(hentet.events.map { it.versjon }, hentetIgjen.events.map { it.versjon })
        assertEquals(listOf(dag(2 jan 2024, sykdomsgrad = 10)), hentetIgjen.events[1].dager)
        assertEquals(3, antallRader(id.identitetsnummer))
    }

    @Test
    fun `kan lagre og hente graderte andre ytelser uten organisasjonsnummer`() {
        val id = lagId(yrkesaktivitet = Yrkesaktivitetstype.FRILANS, organisasjonsnummer = null)
        repository.lagre(ny(id, listOf(dag(1 jan 2024))))

        val hentet = repository.finnOrNull(id)

        assertNotNull(hentet)
        assertEquals(id, hentet.id)
        assertNull(hentet.id.organisasjonsnummer)
    }

    @Test
    fun `streamer holdes adskilt per yrkesaktivitet og organisasjonsnummer`() {
        val identitetsnummer = lagIdentitetsnummer()
        val arbeidsgiver1 = lagId(identitetsnummer, organisasjonsnummer = "111111111")
        val arbeidsgiver2 = lagId(identitetsnummer, organisasjonsnummer = "222222222")
        val frilans = lagId(identitetsnummer, Yrkesaktivitetstype.FRILANS, organisasjonsnummer = null)
        repository.lagre(ny(arbeidsgiver1, listOf(dag(1 jan 2024, sykdomsgrad = 10))))
        repository.lagre(ny(arbeidsgiver2, listOf(dag(1 jan 2024, sykdomsgrad = 20))))
        repository.lagre(ny(frilans, listOf(dag(1 jan 2024, sykdomsgrad = 30))))

        assertEquals(listOf(dag(1 jan 2024, sykdomsgrad = 10)), repository.finnOrNull(arbeidsgiver1)?.dager)
        assertEquals(listOf(dag(1 jan 2024, sykdomsgrad = 20)), repository.finnOrNull(arbeidsgiver2)?.dager)
        assertEquals(listOf(dag(1 jan 2024, sykdomsgrad = 30)), repository.finnOrNull(frilans)?.dager)
        assertNull(repository.finnOrNull(lagId(identitetsnummer, Yrkesaktivitetstype.SELVSTENDIG, organisasjonsnummer = null)))
        assertEquals(
            setOf(arbeidsgiver1, arbeidsgiver2, frilans),
            repository.finnAlleForIdentitetsnummer(identitetsnummer).map { it.id }.toSet(),
        )
    }

    @Test
    fun `lagre er idempotent for allerede persisterte events`() {
        val id = lagId()
        val graderteAndreYtelser = ny(id, listOf(dag(1 jan 2024)))

        repository.lagre(graderteAndreYtelser)
        repository.lagre(graderteAndreYtelser)

        assertEquals(1, antallRader(id.identitetsnummer))
    }

    private fun antallRader(identitetsnummer: Identitetsnummer) =
        dbQuery.single(
            "SELECT COUNT(*) AS antall FROM graderte_andre_ytelser_events WHERE fødselsnummer = :fodselsnummer",
            "fodselsnummer" to identitetsnummer.value,
        ) { it.int("antall") }

    private fun lagId(
        identitetsnummer: Identitetsnummer = lagIdentitetsnummer(),
        yrkesaktivitet: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        organisasjonsnummer: String? = "123456789",
    ) = GraderteAndreYtelserId(identitetsnummer, yrkesaktivitet, organisasjonsnummer)

    private fun ny(
        id: GraderteAndreYtelserId,
        dager: List<GraderteAndreYtelserDag>,
    ) = GraderteAndreYtelser.ny(
        id = id,
        dager = dager,
        saksbehandlerIdent = saksbehandlerIdent,
        notatTilBeslutter = "notat til beslutter",
    )

    private fun dag(
        dato: LocalDate,
        andreYtelserGrader: Map<GraderteAndreYtelserType, Int> = mapOf(GraderteAndreYtelserType.FORELDREPENGER to 50),
        sykdomsgrad: Int = 30,
        arbeidsgrad: Int = 20,
    ) = GraderteAndreYtelserDag(dato, andreYtelserGrader, sykdomsgrad, arbeidsgrad)
}
