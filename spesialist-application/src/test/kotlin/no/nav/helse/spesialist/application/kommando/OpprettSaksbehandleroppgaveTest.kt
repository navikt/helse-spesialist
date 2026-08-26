package no.nav.helse.spesialist.application.kommando

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.mediator.oppgave.OppgaveService
import no.nav.helse.modell.automatisering.Automatisering
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.OpprettSaksbehandleroppgave
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Arbeidssituasjon
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.Periodetype.FORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.FØRSTEGANGSBEHANDLING
import no.nav.helse.modell.vedtaksperiode.Periodetype.INFOTRYGDFORLENGELSE
import no.nav.helse.modell.vedtaksperiode.Periodetype.OVERGANG_FRA_IT
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.spesialist.application.MockForsikringsvurderingHenter
import no.nav.helse.spesialist.application.Testdata.godkjenningsbehovData
import no.nav.helse.spesialist.application.testfixtures.lagForsikringsvurdering
import no.nav.helse.spesialist.application.testfixtures.lagKollektivForsikring
import no.nav.helse.spesialist.domain.DialogId
import no.nav.helse.spesialist.domain.Identitetsnummer
import no.nav.helse.spesialist.domain.Personinfo
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.oppgave.Egenskap
import no.nav.helse.spesialist.domain.oppgave.Egenskap.ARBEIDSTAKER
import no.nav.helse.spesialist.domain.oppgave.Egenskap.DELVIS_REFUSJON
import no.nav.helse.spesialist.domain.oppgave.Egenskap.EGEN_ANSATT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.EN_ARBEIDSGIVER
import no.nav.helse.spesialist.domain.oppgave.Egenskap.FLERE_ARBEIDSGIVERE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.FORSIKRING
import no.nav.helse.spesialist.domain.oppgave.Egenskap.FORSTEGANGSBEHANDLING
import no.nav.helse.spesialist.domain.oppgave.Egenskap.FORTROLIG_ADRESSE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.GRUNNBELØPSREGULERING
import no.nav.helse.spesialist.domain.oppgave.Egenskap.HASTER
import no.nav.helse.spesialist.domain.oppgave.Egenskap.INGEN_UTBETALING
import no.nav.helse.spesialist.domain.oppgave.Egenskap.JORDBRUKER_REINDRIFT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.PÅ_VENT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.REVURDERING
import no.nav.helse.spesialist.domain.oppgave.Egenskap.RISK_QA
import no.nav.helse.spesialist.domain.oppgave.Egenskap.SELVSTENDIG_NÆRINGSDRIVENDE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.SKJØNNSFASTSETTELSE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.STIKKPRØVE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.STRENGT_FORTROLIG_ADRESSE
import no.nav.helse.spesialist.domain.oppgave.Egenskap.SØKNAD
import no.nav.helse.spesialist.domain.oppgave.Egenskap.TILBAKEDATERT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.UTBETALING_TIL_ARBEIDSGIVER
import no.nav.helse.spesialist.domain.oppgave.Egenskap.UTBETALING_TIL_SYKMELDT
import no.nav.helse.spesialist.domain.oppgave.Egenskap.UTLAND
import no.nav.helse.spesialist.domain.testfixtures.lagPåVent
import no.nav.helse.spesialist.domain.testfixtures.lagSpleisBehandlingId
import no.nav.helse.spesialist.domain.testfixtures.lagVedtaksperiodeId
import no.nav.helse.spesialist.domain.testfixtures.testdata.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.JsonNodeFactory
import tools.jackson.databind.node.ObjectNode
import java.time.LocalDateTime
import java.util.*
import kotlin.test.assertEquals

internal class OpprettSaksbehandleroppgaveTest : ApplicationTest() {
    private val FNR = lagFødselsnummer()
    private val VEDTAKSPERIODE_ID = lagVedtaksperiodeId()
    private val BEHANDLING_ID = lagSpleisBehandlingId()
    private val UTBETALING_ID = UUID.randomUUID()
    private val HENDELSE_ID = UUID.randomUUID()
    private val contextId = UUID.randomUUID()
    private val context = CommandContext(contextId)

    private val oppgaveService = mockk<OppgaveService>(relaxed = true)
    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val sykefraværstilfelle = mockk<Sykefraværstilfelle>(relaxed = true)
    private val forsikringsvurderingHenter = MockForsikringsvurderingHenter()

    private val command get() = opprettSaksbehandlerOppgaveCommand()
    private val utbetaling = mockk<Utbetaling>(relaxed = true)

    init {
        lagrePerson()
    }

    @Test
    fun `oppretter oppgave`() {
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
        assertEquals(1, sessionContext.opptegnelseRepository.alle().size)
    }

    @Test
    fun `oppretter stikkprøve`() {
        every { automatisering.erStikkprøve(VEDTAKSPERIODE_ID.value, any()) } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            STIKKPRØVE,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter risk QA`() {
        lagreRisikovurdering(kanGodkjennesAutomatisk = false)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            RISK_QA,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter revurdering`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(utbetalingtype = Utbetalingtype.REVURDERING).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for fortrolig adresse`() {
        lagrePerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.Fortrolig)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            FORTROLIG_ADRESSE,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for strengt fortrolig adresse`() {
        lagrePerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortrolig)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            STRENGT_FORTROLIG_ADRESSE,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for strengt fortrolig adresse utland`() {
        lagrePerson(adressebeskyttelse = Personinfo.Adressebeskyttelse.StrengtFortroligUtland)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            STRENGT_FORTROLIG_ADRESSE,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for utbetaling til sykmeldt`() {
        every { utbetaling.kunUtbetalingTilSykmeldt() } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            UTBETALING_TIL_SYKMELDT,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egen oppgavetype for delvis refusjon`() {
        every { utbetaling.delvisRefusjon() } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, DELVIS_REFUSJON, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egenskap utbetaling til arbeidsgiver`() {
        every { utbetaling.kunUtbetalingTilArbeidsgiver() } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            UTBETALING_TIL_ARBEIDSGIVER,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap ingen utbetaling`() {
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egenskap forsikring dersom vurderingen har forsikring`() {
        val forsikringsvurderingId = UUID.randomUUID()
        forsikringsvurderingHenter.forsikringsvurdering =
            lagForsikringsvurdering(kollektivForsikring = lagKollektivForsikring())
        assertTrue(
            opprettSaksbehandlerOppgaveCommand(forsikringsvurderingId = forsikringsvurderingId)
                .execute(context, sessionContext, outbox),
        )
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER, FORSIKRING)
    }

    @Test
    fun `oppretter ikke oppgave med egenskap forsikring dersom vurderingen ikke har forsikring`() {
        val forsikringsvurderingId = UUID.randomUUID()
        forsikringsvurderingHenter.forsikringsvurdering = lagForsikringsvurdering()
        assertTrue(
            opprettSaksbehandlerOppgaveCommand(forsikringsvurderingId = forsikringsvurderingId)
                .execute(context, sessionContext, outbox),
        )
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter ikke oppgave med egenskap forsikring dersom det ikke finnes forsikringsvurderingId`() {
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter ikke oppgave med egenskap haster dersom det er utbetaling til arbeidsgiver`() {
        every { utbetaling.kunUtbetalingTilArbeidsgiver() } returns true
        every { sykefraværstilfelle.haster(VEDTAKSPERIODE_ID.value) } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            UTBETALING_TIL_ARBEIDSGIVER,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap haster dersom det er endring i utbetaling til sykmeldte`() {
        every { utbetaling.harEndringIUtbetalingTilSykmeldt() } returns true
        every { sykefraværstilfelle.haster(VEDTAKSPERIODE_ID.value) } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            HASTER,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap skjønnsfastsettelse dersom det finnes varsel om avvik`() {
        every { sykefraværstilfelle.kreverSkjønnsfastsettelse(VEDTAKSPERIODE_ID.value) } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            SKJØNNSFASTSETTELSE,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap tilbakedatert dersom det finnes varsel om tilbakedatering`() {
        every { sykefraværstilfelle.erTilbakedatert(VEDTAKSPERIODE_ID.value) } returns true
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            TILBAKEDATERT,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egen ansatt`() {
        lagrePerson(erEgenAnsatt = true)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            EGEN_ANSATT,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap UTLAND`() {
        lagrePerson(enhet = 393)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            UTLAND,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap FLERE_ARBEIDSGIVERE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(inntektskilde = Inntektskilde.FLERE_ARBEIDSGIVERE).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, FLERE_ARBEIDSGIVERE, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egenskap FORLENGELSE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = FORLENGELSE).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, Egenskap.FORLENGELSE, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egenskap INFOTRYGDFORLENGELSE`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = INFOTRYGDFORLENGELSE).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            Egenskap.INFOTRYGDFORLENGELSE,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap OVERGANG_FRA_IT`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(periodetype = OVERGANG_FRA_IT).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(SØKNAD, INGEN_UTBETALING, EN_ARBEIDSGIVER, Egenskap.OVERGANG_FRA_IT, ARBEIDSTAKER)
    }

    @Test
    fun `oppretter oppgave med egenskap PÅ_VENT`() {
        sessionContext.påVentRepository.lagre(
            lagPåVent(
                vedtaksperiodeId = VEDTAKSPERIODE_ID,
                saksbehandlerOid = SaksbehandlerOid(UUID.randomUUID()),
                dialogId = DialogId(1L),
            ),
        )
        assertTrue(command.execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            PÅ_VENT,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `oppretter oppgave med egenskap JORDBRUKER_REINDRIFT`() {
        assertTrue(
            opprettSaksbehandlerOppgaveCommand(
                arbeidssituasjon = Arbeidssituasjon.JORDBRUKER,
                yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG,
            ).execute(context, sessionContext, outbox),
        )
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            JORDBRUKER_REINDRIFT,
            SELVSTENDIG_NÆRINGSDRIVENDE,
        )
    }

    @Test
    fun `legger ikke til egenskap RISK_QA hvis oppgaven har egenskap REVURDERING`() {
        lagreRisikovurdering(kanGodkjennesAutomatisk = false)
        assertTrue(opprettSaksbehandlerOppgaveCommand(utbetalingtype = Utbetalingtype.REVURDERING).execute(context, sessionContext, outbox))

        assertForventedeEgenskaper(REVURDERING, INGEN_UTBETALING, EN_ARBEIDSGIVER, FORSTEGANGSBEHANDLING, ARBEIDSTAKER)
    }

    @Test
    fun `legger til oppgave med kanAvvises lik false`() {
        assertTrue(opprettSaksbehandlerOppgaveCommand(kanAvvises = false).execute(context, sessionContext, outbox))
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
            kanAvvises = false,
        )
    }

    @Test
    fun `oppretter oppgave med egenskapen grunnbeløpsregulering`() {
        assertTrue(
            opprettSaksbehandlerOppgaveCommand(
                utbetalingtype = Utbetalingtype.REVURDERING,
                tags = listOf("Grunnbeløpsregulering"),
            ).execute(context, sessionContext, outbox),
        )
        assertForventedeEgenskaper(
            REVURDERING,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            GRUNNBELØPSREGULERING,
            ARBEIDSTAKER,
        )
    }

    @Test
    fun `legger ikke til egenskapen grunnbeløpsregulering ved oppgavetype søknad`() {
        assertTrue(
            opprettSaksbehandlerOppgaveCommand(
                utbetalingtype = Utbetalingtype.UTBETALING,
                tags = listOf("Grunnbeløpsregulering"),
            ).execute(context, sessionContext, outbox),
        )
        assertForventedeEgenskaper(
            SØKNAD,
            INGEN_UTBETALING,
            EN_ARBEIDSGIVER,
            FORSTEGANGSBEHANDLING,
            ARBEIDSTAKER,
        )
    }

    private fun lagrePerson(
        adressebeskyttelse: Personinfo.Adressebeskyttelse = Personinfo.Adressebeskyttelse.Ugradert,
        erEgenAnsatt: Boolean = false,
        enhet: Int? = null,
    ) {
        sessionContext.personRepository.lagre(
            lagPerson(
                id = Identitetsnummer.fraString(FNR),
                adressebeskyttelse = adressebeskyttelse,
                erEgenAnsatt = erEgenAnsatt,
                enhet = enhet ?: 100,
            ),
        )
    }

    private fun lagreRisikovurdering(kanGodkjennesAutomatisk: Boolean) {
        sessionContext.risikovurderingDao.lagre(
            vedtaksperiodeId = VEDTAKSPERIODE_ID.value,
            kanGodkjennesAutomatisk = kanGodkjennesAutomatisk,
            data = ObjectNode(JsonNodeFactory.instance),
            opprettet = LocalDateTime.now(),
        )
    }

    private fun assertForventedeEgenskaper(
        vararg egenskaper: Egenskap,
        kanAvvises: Boolean = true,
    ) {
        verify(exactly = 1) {
            oppgaveService.nyOppgave(
                FNR,
                VEDTAKSPERIODE_ID,
                BEHANDLING_ID,
                UTBETALING_ID,
                HENDELSE_ID,
                kanAvvises,
                egenskaper.toSet(),
                mottaker = egenskaper.finnMottaker(),
                type = egenskaper.finnOppgavetype(),
                inntektskilde = egenskaper.finnInntektskilde(),
                inntektsforhold = egenskaper.finnInntektsforhold(),
                periodetype = egenskaper.finnPeriodetype(),
            )
        }
    }

    private fun opprettSaksbehandlerOppgaveCommand(
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        periodetype: Periodetype = FØRSTEGANGSBEHANDLING,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        tags: List<String> = emptyList(),
        arbeidssituasjon: Arbeidssituasjon? = null,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        forsikringsvurderingId: UUID? = null,
    ) = OpprettSaksbehandleroppgave(
        behovData =
            godkjenningsbehovData(
                id = HENDELSE_ID,
                fødselsnummer = FNR,
                vedtaksperiodeId = VEDTAKSPERIODE_ID.value,
                spleisBehandlingId = BEHANDLING_ID.value,
                utbetalingId = UTBETALING_ID,
                inntektskilde = inntektskilde,
                periodetype = periodetype,
                utbetalingtype = utbetalingtype,
                kanAvvises = kanAvvises,
                tags = tags,
                arbeidssituasjon = arbeidssituasjon,
                yrkesaktivitetstype = yrkesaktivitetstype,
                forsikringsvurderingId = forsikringsvurderingId,
            ),
        oppgaveService = oppgaveService,
        automatisering = automatisering,
        utbetalingtype = utbetalingtype,
        sykefraværstilfelle = sykefraværstilfelle,
        utbetaling = utbetaling,
        forsikringsvurderingHenter = forsikringsvurderingHenter,
    )
}
