package no.nav.helse.spesialist.client.spleis

import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SnapshotSammenligningHenterTest {
    private fun person(versjon: Int) =
        SnapshotPerson(
            aktorId = "1234567890123",
            arbeidsgivere = emptyList(),
            dodsdato = null as LocalDate?,
            fodselsnummer = "11111111111",
            versjon = versjon,
            vilkarsgrunnlag = emptyList(),
        )

    @Test
    fun `returnerer GraphQL-resultatet uendret, og kaller ikke REST, når skalSammenligne er false`() {
        val graphQLResultat = person(1)
        var restKallt = false
        val graphQL = object : Snapshothenter { override fun hentPerson(fødselsnummer: String) = graphQLResultat }

        val henter =
            SnapshotSammenligningHenter(
                graphQL = graphQL,
                hentPersonRest = { restKallt = true; null },
                skalSammenligne = false,
            )

        val result = henter.hentPerson("11111111111")

        assertSame(graphQLResultat, result)
        assertEquals(false, restKallt)
    }

    @Test
    fun `returnerer GraphQL-resultatet uendret selv om REST kaster feil`() {
        val graphQLResultat = person(1)
        val graphQL = object : Snapshothenter { override fun hentPerson(fødselsnummer: String) = graphQLResultat }

        val henter =
            SnapshotSammenligningHenter(
                graphQL = graphQL,
                hentPersonRest = { error("REST er nede") },
                skalSammenligne = true,
            )

        val result = henter.hentPerson("11111111111")

        assertSame(graphQLResultat, result)
    }

    @Test
    fun `returnerer GraphQL-resultatet uendret selv om resultatene er ulike`() {
        val graphQLResultat = person(1)
        val graphQL = object : Snapshothenter { override fun hentPerson(fødselsnummer: String) = graphQLResultat }

        val henter =
            SnapshotSammenligningHenter(
                graphQL = graphQL,
                hentPersonRest = { null }, // ulikt resultat (null i stedet for person)
                skalSammenligne = true,
            )

        val result = henter.hentPerson("11111111111")

        assertSame(graphQLResultat, result)
    }

    @Test
    fun `kaller REST når skalSammenligne er true`() {
        val kallteller = AtomicInteger(0)
        val graphQLResultat = person(1)
        val graphQL = object : Snapshothenter { override fun hentPerson(fødselsnummer: String) = graphQLResultat }

        val henter =
            SnapshotSammenligningHenter(
                graphQL = graphQL,
                hentPersonRest = { kallteller.incrementAndGet(); null },
                skalSammenligne = true,
            )

        henter.hentPerson("11111111111")

        assertEquals(1, kallteller.get())
    }
}
