package no.nav.helse.spleis

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ReverseClientTest {

    @Test
    fun `smoke test på at spleis får tak i skjemafilen som spesialist bruker`() {
        /*
            Denne testen er tenkt å være et hjelpemiddel for å oppdage informasjonsbehov tidlig, i stedet for at ting
            feiler på spleiselagets side og de må grave i hvorfor.

            Den biten med '../spesialist-client-spleis' i filstien er fordi spleis-koden henter fra der filen vår havner
            *etter* at vi har pushet, mens vi ønsker å oppdage breaking changes før vi pusher.
         */
        assertTrue(File("../spesialist-client-spleis/src/main/resources/graphql/hentSnapshot.graphql").isFile) {
            """
                Hei! Hvis denne testen feiler, kan det hende du har gjort en endring som må informeres om til spleiselaget.
                Spleis henter nemlig graphql-skjemafilen som spesialist bruker mot spleis her:
                https://github.com/navikt/helse-spleis/blob/0558a4aedf0615cb01ce8af4269b2b29af4badb9/sykepenger-api/src/test/kotlin/no/nav/helse/spleis/graphql/GraphQLApiTest.kt#L98
            """.trimIndent()
        }
    }
}
