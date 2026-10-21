package no.nav.helse.spesialist.api.saksbilde

import io.mockk.every
import io.mockk.mockk
import no.nav.helse.spesialist.api.DatabaseIntegrationTest
import no.nav.helse.spesialist.api.objectMapper
import no.nav.helse.spesialist.api.rest.personer.saksbilde.SaksbildeHenter
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettBeregnetPeriode
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSnapshotArbeidsgiver
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSnapshotGenerasjon
import no.nav.helse.spesialist.api.saksbilde.SnapshotTestdata.opprettSpleisVilkarsgrunnlag
import no.nav.helse.spesialist.application.InMemoryPersonPseudoIdProvider
import no.nav.helse.spesialist.application.InMemoryPersoninfoHenter
import no.nav.helse.spesialist.application.Snapshothenter
import no.nav.helse.spesialist.application.snapshot.SnapshotArbeidsgiver
import no.nav.helse.spesialist.application.snapshot.SnapshotPerson
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.testfixtures.jan
import tools.jackson.databind.JsonNode
import java.util.UUID

abstract class AbstractSaksbildeTest : DatabaseIntegrationTest() {
    protected val snapshothenter = mockk<Snapshothenter>(relaxed = true)
    protected val personPseudoIdProvider = InMemoryPersonPseudoIdProvider()
    protected val personinfoHenter = InMemoryPersoninfoHenter()

    private val saksbildeHenter =
        SaksbildeHenter(
            daos = daos,
            apiOppgaveService = apiOppgaveService,
            snapshothenter = snapshothenter,
            personPseudoIdProvider = personPseudoIdProvider,
            personinfoHenter = personinfoHenter,
        )

    protected fun hentSaksbilde(identitetsnummer: Identitetsnummer = Identitetsnummer.fraString(FØDSELSNUMMER)): JsonNode =
        sessionFactory.transactionalSessionScope { transaction ->
            val person = checkNotNull(transaction.personRepository.finnOrNull(identitetsnummer)) { "Fant ikke personen" }
            objectMapper.valueToTree(saksbildeHenter.hent(person, transaction))
        }

    protected fun mockSnapshot(
        fødselsnummer: String = FØDSELSNUMMER,
        arbeidsgivere: List<SnapshotArbeidsgiver> = listOf(defaultArbeidsgivere()),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
    ) {
        every { snapshothenter.hentPerson(fødselsnummer) } returns
            SnapshotPerson(
                arbeidsgivere = arbeidsgivere,
                dodsdato = null,
                fodselsnummer = fødselsnummer,
                vilkarsgrunnlag = listOf(opprettSpleisVilkarsgrunnlag(ORGANISASJONSNUMMER, vilkårsgrunnlagId)),
            )
    }

    private fun defaultArbeidsgivere(): SnapshotArbeidsgiver {
        val periodeMedOppgave = Periode(UUID.randomUUID(), 1 jan 2018, 25 jan 2018)
        val beregnetPeriode = opprettBeregnetPeriode(4 jan 2023, 5 jan 2023, periodeMedOppgave.id)
        val snapshotBehandling = opprettSnapshotGenerasjon(listOf(beregnetPeriode))
        return opprettSnapshotArbeidsgiver(ORGANISASJONSNUMMER, listOf(snapshotBehandling))
    }
}
