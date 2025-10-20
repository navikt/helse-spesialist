package no.nav.helse.mediator.oppgave

import no.nav.helse.db.EgenskapForDatabase
import no.nav.helse.mediator.oppgave.OppgaveMapper.tilEgenskaperForVisning
import no.nav.helse.spesialist.api.graphql.schema.ApiEgenskap
import no.nav.helse.spesialist.api.graphql.schema.ApiKategori
import no.nav.helse.spesialist.api.graphql.schema.ApiOppgaveegenskap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class OppgaveMapperTest {
    @Test
    fun `map EgenskapForDatabase til OppgaveEgenskap (api)`() {
        val egenskaperForDatabase = setOf(
            EgenskapForDatabase.SØKNAD,
            EgenskapForDatabase.DELVIS_REFUSJON,
            EgenskapForDatabase.FORSTEGANGSBEHANDLING,
            EgenskapForDatabase.EN_ARBEIDSGIVER
        )
        val oppgaveEgenskaper = egenskaperForDatabase.tilEgenskaperForVisning()

        assertEquals(
            setOf(
                ApiOppgaveegenskap(ApiEgenskap.SOKNAD, ApiKategori.Oppgavetype),
                ApiOppgaveegenskap(ApiEgenskap.FORSTEGANGSBEHANDLING, ApiKategori.Periodetype),
                ApiOppgaveegenskap(ApiEgenskap.EN_ARBEIDSGIVER, ApiKategori.Inntektskilde),
                ApiOppgaveegenskap(ApiEgenskap.DELVIS_REFUSJON, ApiKategori.Mottaker),
            ),
            oppgaveEgenskaper.toSet(),
        )
    }
}
