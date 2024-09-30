package no.nav.helse.modell.automatisering

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.db.EgenAnsattRepository
import no.nav.helse.db.PersonRepository
import no.nav.helse.db.VergemålRepository
import no.nav.helse.januar
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.spesialist.test.lagAktørId
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class VurderAutomatiskAvvisningTest {
    private lateinit var context: CommandContext

    private val vergemålRepository = mockk<VergemålRepository>(relaxed = true)
    private val personRepository = mockk<PersonRepository>(relaxed = true)
    private val egenAnsattRepository = mockk<EgenAnsattRepository>(relaxed = true)
    private val godkjenningMediator = mockk<GodkjenningMediator>(relaxed = true)

    @BeforeEach
    fun setup() {
        context = CommandContext(UUID.randomUUID())
        clearMocks(vergemålRepository, personRepository, egenAnsattRepository, godkjenningMediator)
    }

    @Test
    fun `skal avvise ved vergemål dersom perioden kan avvises`() {
        every { vergemålRepository.harVergemål(fødselsnummer) } returns true
        assertAvvisning(lagCommand(kanAvvises = true), "Vergemål")
    }

    @Test
    fun `skal ikke avvise ved vergemål dersom perioden ikke kan avvises`() {
        every { vergemålRepository.harVergemål(fødselsnummer) } returns true
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    @Test
    fun `skal avvise ved utland dersom perioden kan avvises`() {
        every { personRepository.finnEnhetId(fødselsnummer) } returns "0393"
        assertAvvisning(lagCommand(kanAvvises = true), "Utland")
    }

    @Test
    fun `skal ikke avvise ved utland dersom perioden ikke kan avvises`() {
        every { personRepository.finnEnhetId(fødselsnummer) } returns "0393"
        assertIkkeAvvisning(lagCommand(kanAvvises = false))
    }

    private fun assertAvvisning(
        command: VurderAutomatiskAvvisning,
        forventetÅrsak: String,
    ) {
        assertTrue(command.execute(context))
        verify(exactly = 1) {
            godkjenningMediator.automatiskAvvisning(
                publiserer = any(),
                begrunnelser = listOf(forventetÅrsak),
                utbetaling = any(),
                godkjenningsbehov = any()
            )
        }
    }

    private fun assertIkkeAvvisning(command: VurderAutomatiskAvvisning) {
        assertTrue(command.execute(context))
        verify(exactly = 0) { godkjenningMediator.automatiskAvvisning(any(), any(), any(), any()) }
    }

    private fun lagCommand(
        kanAvvises: Boolean = true,
        fødselsnummer: String = "12345678910",
    ) = VurderAutomatiskAvvisning(
        personRepository = personRepository,
        vergemålRepository = vergemålRepository,
        godkjenningMediator = godkjenningMediator,
        utbetaling = Utbetaling(utbetalingId, 1000, 1000, Utbetalingtype.UTBETALING),
        godkjenningsbehov = godkjenningsbehov(
            kanAvvises = kanAvvises,
            fødselsnummer = fødselsnummer
        )
    )

    private fun godkjenningsbehov(
        id: UUID = UUID.randomUUID(),
        aktørId: String = lagAktørId(),
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
        json: String = "{}"
    ) = GodkjenningsbehovData(
        id = id,
        fødselsnummer = fødselsnummer,
        aktørId = aktørId,
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
        json = json,
    )

    private companion object {
        private const val fødselsnummer = "12345678910"
        private val utbetalingId = UUID.randomUUID()
    }
}
