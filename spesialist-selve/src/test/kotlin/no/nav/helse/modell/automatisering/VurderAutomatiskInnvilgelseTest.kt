package no.nav.helse.modell.automatisering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.AutomatiseringRepository
import no.nav.helse.db.CommandContextRepository
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.mediator.KommandokjedeEndretEvent
import no.nav.helse.modell.hendelse.UtgåendeHendelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.sykefraværstilfelle.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.Behandling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VurderAutomatiskInnvilgelseTest {
    private companion object {
        private val vedtaksperiodeId = UUID.randomUUID()
        private val utbetalingId = UUID.randomUUID()
        private const val fødselsnummer = "12345678910"
        private const val orgnummer = "123456789"
        private val hendelseId = UUID.randomUUID()
        private val periodetype = Periodetype.FORLENGELSE
    }

    private val automatisering = mockk<Automatisering>(relaxed = true)
    private val behandling = Behandling(UUID.randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar)
    private val automatiseringRepository = mockk<AutomatiseringRepository>(relaxed = true)
    private val command =
        VurderAutomatiskInnvilgelse(
            automatisering,
            GodkjenningMediator(
                opptegnelseRepository = mockk(relaxed = true),
            ),
            utbetaling = Utbetaling(utbetalingId, 0, 0, Utbetalingtype.UTBETALING),
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = 1.januar,
                gjeldendeBehandlinger = listOf(behandling),
            ),
            godkjenningsbehov = godkjenningsbehov(
                id = hendelseId,
                organisasjonsnummer = orgnummer,
                vedtaksperiodeId = vedtaksperiodeId,
                utbetalingId = utbetalingId,
                periodetype = periodetype,
                json = """{ "@event_name": "behov" }"""
            ),
            automatiseringRepository = automatiseringRepository,
            oppgaveService = mockk(relaxed = true),
        )

    private lateinit var context: CommandContext

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        context.nyObserver(this.observatør)
    }

    @Test
    fun `kaller automatiser utfør og returnerer true`() {
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            automatisering.utfør(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `publiserer godkjenningsmelding ved automatisert godkjenning`() {
        every {
            automatisering.utfør(any(), any(), any(), any(), any(), any())
        } returns Automatiseringsresultat.KanAutomatiseres

        assertTrue(command.execute(context))

        val løsning =
            this
                .observatør
                .hendelser
                .filterIsInstance<UtgåendeHendelse.Godkjenningsbehovløsning>()
                .singleOrNull()
        assertNotNull(løsning)
        assertEquals(true, løsning?.automatiskBehandling)
    }

    @Test
    fun `automatiserer når resultat er at perioden kan automatiseres`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        assertTrue(command.execute(context))
        verify(exactly = 1) { automatiseringRepository.automatisert(vedtaksperiodeId, hendelseId, utbetalingId) }
        verify(exactly = 0) { automatiseringRepository.manuellSaksbehandling(any(), any(), any(), any()) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden kan ikke automatiseres`() {
        val problemer = listOf("Problem 1", "Problem 2")
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanIkkeAutomatiseres(problemer)
        assertTrue(command.execute(context))
        verify(exactly = 0) { automatiseringRepository.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringRepository.manuellSaksbehandling(problemer, vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `automatiserer ikke når resultat er at perioden er stikkprøve`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.Stikkprøve("En årsak")
        assertTrue(command.execute(context))
        verify(exactly = 0) { automatiseringRepository.automatisert(any(), any(), any()) }
        verify(exactly = 1) { automatiseringRepository.stikkprøve(vedtaksperiodeId, hendelseId, utbetalingId) }
    }

    @Test
    fun `Ferdigstiller kjede når perioden kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        context.utfør(commandContextRepository, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    @Test
    fun `Ferdigstiller kjede når perioden er spesialsak som kan behandles automatisk`() {
        every { automatisering.utfør(any(), any(), any(), any(), any(), any()) } returns Automatiseringsresultat.KanAutomatiseres
        context.utfør(commandContextRepository, UUID.randomUUID(), command)
        assertEquals("Ferdig", observatør.gjeldendeTilstand)
    }

    private val commandContextRepository = object : CommandContextRepository {
        override fun nyContext(meldingId: UUID): CommandContext = TODO("Not yet implemented")
        override fun opprett(hendelseId: UUID, contextId: UUID) {}
        override fun ferdig(hendelseId: UUID, contextId: UUID) {}
        override fun suspendert(hendelseId: UUID, contextId: UUID, hash: UUID, sti: List<Int>) {}
        override fun feil(hendelseId: UUID, contextId: UUID) {}
        override fun tidsbrukForContext(contextId: UUID): Int = TODO("Not yet implemented")
        override fun avbryt(vedtaksperiodeId: UUID, contextId: UUID): List<Pair<UUID, UUID>> = TODO("Not yet implemented")
    }

    private val observatør = object : CommandContextObserver {
        val hendelser = mutableListOf<UtgåendeHendelse>()
        lateinit var gjeldendeTilstand: String
            private set

        override fun hendelse(hendelse: UtgåendeHendelse) {
            hendelser.add(hendelse)
        }

        override fun tilstandEndret(event: KommandokjedeEndretEvent) {
            gjeldendeTilstand = event::class.simpleName!!
        }
    }

    private fun godkjenningsbehov(
        id: UUID = UUID.randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        vedtaksperiodeId: UUID = UUID.randomUUID(),
        utbetalingId: UUID = UUID.randomUUID(),
        spleisBehandlingId: UUID = UUID.randomUUID(),
        avviksvurderingId: UUID = UUID.randomUUID(),
        vilkårsgrunnlagId: UUID = UUID.randomUUID(),
        fom: LocalDate = 1.januar,
        tom: LocalDate = 31.januar,
        skjæringstidspunkt: LocalDate = fom,
        tags: Set<String> = emptySet(),
        periodetype: Periodetype = Periodetype.FØRSTEGANGSBEHANDLING,
        førstegangsbehandling: Boolean = periodetype == Periodetype.FØRSTEGANGSBEHANDLING,
        utbetalingtype: Utbetalingtype = Utbetalingtype.UTBETALING,
        kanAvvises: Boolean = true,
        inntektskilde: Inntektskilde = Inntektskilde.EN_ARBEIDSGIVER,
        andreInntektskilder: List<String> = emptyList(),
        spleisSykepengegrunnlagsfakta: SpleisSykepengegrunnlagsfakta = SpleisSykepengegrunnlagsfakta(emptyList()),
        json: String = "{}"
    ) = GodkjenningsbehovData(
        id = id,
        fødselsnummer = fødselsnummer,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiodeId,
        spleisVedtaksperioder = emptyList(),
        utbetalingId = utbetalingId,
        spleisBehandlingId = spleisBehandlingId,
        avviksvurderingId = avviksvurderingId,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        tags = tags.toList(),
        periodeFom = fom,
        periodeTom = tom,
        periodetype = periodetype,
        førstegangsbehandling = førstegangsbehandling,
        utbetalingtype = utbetalingtype,
        kanAvvises = kanAvvises,
        inntektskilde = inntektskilde,
        orgnummereMedRelevanteArbeidsforhold = andreInntektskilder,
        skjæringstidspunkt = skjæringstidspunkt,
        spleisSykepengegrunnlagsfakta = spleisSykepengegrunnlagsfakta,
        json = json,
    )
}
