package no.nav.helse.mediator.meldinger

import io.mockk.mockk
import no.nav.helse.januar
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.mediator.GodkjenningMediator
import no.nav.helse.modell.melding.UtgåendeHendelse
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.LøsGodkjenningsbehov
import no.nav.helse.modell.melding.Godkjenningsbehovløsning
import no.nav.helse.modell.person.Sykefraværstilfelle
import no.nav.helse.modell.utbetaling.Refusjonstype
import no.nav.helse.modell.utbetaling.Refusjonstype.DELVIS_REFUSJON
import no.nav.helse.modell.utbetaling.Utbetaling
import no.nav.helse.modell.utbetaling.Utbetalingtype
import no.nav.helse.modell.person.vedtaksperiode.Behandling
import no.nav.helse.modell.vedtaksperiode.GodkjenningsbehovData
import no.nav.helse.modell.vedtaksperiode.Inntektskilde
import no.nav.helse.modell.vedtaksperiode.Periodetype
import no.nav.helse.modell.vedtaksperiode.SpleisSykepengegrunnlagsfakta
import no.nav.helse.modell.vedtaksperiode.vedtak.Saksbehandlerløsning
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import java.util.UUID.randomUUID

internal class SaksbehandlerløsningTest {

    private companion object {
        private val GODKJENNINGSBEHOV_ID = randomUUID()
        private val GODKJENTTIDSPUNKT = LocalDateTime.now()
        private const val FNR = "12020052345"
        private const val IDENT = "Z999999"
        private const val GODKJENNINGSBEHOV_JSON = """{ "@event_name": "behov", "Godkjenning": {} }"""
    }

    private val saksbehandler = Saksbehandlerløsning.Saksbehandler(
        ident = "saksbehandlerident",
        epostadresse = "saksbehandler@nav.no"
    )

    private val beslutter = Saksbehandlerløsning.Saksbehandler(
        ident = "beslutterident",
        epostadresse = "beslutter@nav.no"
    )

    private fun saksbehandlerløsning(godkjent: Boolean, saksbehandlerløsning: List<UUID> = emptyList(), arbeidsgiverbeløp: Int = 0, personbeløp: Int = 0): LøsGodkjenningsbehov {
        val vedtaksperiodeId = randomUUID()
        return LøsGodkjenningsbehov(
            utbetaling = Utbetaling(randomUUID(), arbeidsgiverbeløp, personbeløp, Utbetalingtype.UTBETALING),
            sykefraværstilfelle = Sykefraværstilfelle(
                fødselsnummer = FNR,
                skjæringstidspunkt = 1.januar,
                gjeldendeBehandlinger = listOf(Behandling(randomUUID(), vedtaksperiodeId, 1.januar, 31.januar, 1.januar))
            ),
            godkjent = godkjent,
            godkjenttidspunkt = GODKJENTTIDSPUNKT,
            ident = IDENT,
            epostadresse = "saksbehandler@nav.no",
            årsak = null,
            begrunnelser = null,
            kommentar = null,
            saksbehandleroverstyringer = saksbehandlerløsning,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            godkjenningMediator = GodkjenningMediator(mockk()),
            godkjenningsbehovData = godkjenningsbehov(
                id = GODKJENNINGSBEHOV_ID,
                fødselsnummer = FNR,
                vedtaksperiodeId = vedtaksperiodeId,
                json = GODKJENNINGSBEHOV_JSON
            )
        )
    }

    @Test
    fun `løser godkjenningsbehov`() {
        val saksbehandlerløsning = saksbehandlerløsning(true, arbeidsgiverbeløp = 1000, personbeløp = 1000)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(true, DELVIS_REFUSJON)
    }

    @Test
    fun `løser godkjenningsbehov med saksbehandleroverstyringer`() {
        val  saksbehandleroverstyringer = listOf(randomUUID(), randomUUID())
        val saksbehandlerløsning = saksbehandlerløsning(true, saksbehandleroverstyringer)
        assertTrue(saksbehandlerløsning.execute(context))
        val løsning = observer.hendelser.singleOrNull { it is Godkjenningsbehovløsning }
        assertNotNull(løsning)
        check(løsning is Godkjenningsbehovløsning)

        assertEquals(saksbehandleroverstyringer, løsning.saksbehandleroverstyringer)
    }

    @Test
    fun `løser godkjenningsbehov ved avvist utbetaling`() {
        val saksbehandlerløsning = saksbehandlerløsning(false, arbeidsgiverbeløp = 1000, personbeløp = 1000)
        assertTrue(saksbehandlerløsning.execute(context))
        assertLøsning(false, DELVIS_REFUSJON)
    }

    private fun godkjenningsbehov(
        id: UUID = randomUUID(),
        fødselsnummer: String = lagFødselsnummer(),
        organisasjonsnummer: String = lagOrganisasjonsnummer(),
        vedtaksperiodeId: UUID = randomUUID(),
        utbetalingId: UUID = randomUUID(),
        spleisBehandlingId: UUID = randomUUID(),
        avviksvurderingId: UUID = randomUUID(),
        vilkårsgrunnlagId: UUID = randomUUID(),
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
    ): GodkjenningsbehovData = GodkjenningsbehovData(
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

    private val observer = object : CommandContextObserver {
        val hendelser = mutableListOf<UtgåendeHendelse>()

        override fun hendelse(hendelse: UtgåendeHendelse) {
            hendelser.add(hendelse)
        }
    }

    private val context = CommandContext(randomUUID()).also {
        it.nyObserver(observer)
    }

    private fun assertLøsning(godkjent: Boolean, refusjonstype: Refusjonstype) {
        val løsning = observer.hendelser.filterIsInstance<Godkjenningsbehovløsning>().singleOrNull()
        assertNotNull(løsning)
        assertEquals(godkjent, løsning?.godkjent)
        assertEquals(refusjonstype.name, løsning?.refusjonstype)
    }

}
