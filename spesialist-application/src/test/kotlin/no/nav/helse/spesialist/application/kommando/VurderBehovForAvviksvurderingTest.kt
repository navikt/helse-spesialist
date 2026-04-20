package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForAvviksvurdering
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.LegacyVarsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.domain.testfixtures.jan
import no.nav.helse.spesialist.domain.testfixtures.lagOrganisasjonsnummer
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class VurderBehovForAvviksvurderingTest : ApplicationTest() {
    private val fødselsnummer = lagFødselsnummer()
    private val organisasjonsnummer = lagOrganisasjonsnummer()
    private val vilkårsgrunnlagId = UUID.randomUUID()
    private val skjæringstidspunkt = 1 jan 2018
    private val opprettet = LocalDateTime.now()
    private val avviksvurderingId = UUID.randomUUID()
    private val maksimaltTillattAvvik = 25.0
    private val avviksprosent = 50.0
    private val harAkseptabeltAvvik = false
    private val beregningsgrunnlagTotalbeløp = 900000.0
    private val sammenligningsgrunnlagTotalbeløp = 600000.0
    private val spleisSykepengegrunnlagsfakta =
        Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidstaker.EtterHovedregel(
            seksG = 666666.00,
            arbeidsgivere =
                listOf(
                    Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.EtterHovedregel(
                        organisasjonsnummer = organisasjonsnummer,
                        omregnetÅrsinntekt = beregningsgrunnlagTotalbeløp,
                        inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                    ),
                ),
            sykepengegrunnlag = BigDecimal("666666.0"),
        )
    private val expectedOmregnedeÅrsinntekter = listOf(OmregnetÅrsinntekt(organisasjonsnummer, beregningsgrunnlagTotalbeløp))
    private val beregningsgrunnlag =
        Beregningsgrunnlag(
            totalbeløp = beregningsgrunnlagTotalbeløp,
            omregnedeÅrsinntekter = expectedOmregnedeÅrsinntekter,
        )

    private val sammenligningsgrunnlag =
        Sammenligningsgrunnlag(
            totalbeløp = sammenligningsgrunnlagTotalbeløp,
            innrapporterteInntekter =
                listOf(
                    InnrapportertInntekt(
                        arbeidsgiverreferanse = organisasjonsnummer,
                        inntekter = listOf(Inntekt(YearMonth.of(2018, 1), sammenligningsgrunnlagTotalbeløp)),
                    ),
                ),
        )

    private val legacyBehandling =
        LegacyBehandling(
            id = UUID.randomUUID(),
            vedtaksperiodeId = UUID.randomUUID(),
            fom = 1 jan 2018,
            tom = 31 jan 2018,
            skjæringstidspunkt = 1 jan 2018,
            spleisBehandlingId = UUID.randomUUID(),
            utbetalingId = null,
            yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        )

    private val repository = sessionContext.avviksvurderingRepository

    private val observer =
        object : CommandContextObserver {
            val behov = mutableListOf<Behov>()

            override fun behov(
                behov: Behov,
                commandContextId: UUID,
                sti: List<Int>,
            ) {
                this.behov.add(behov)
            }
        }

    @Test
    fun `Ikke send ut behov dersom inngangsvilkårene ikke er vurdert i Spleis`() {
        val command =
            vurderBehovForAvviksvurderingCommand(
                sykepengegrunnlagsfakta =
                    Godkjenningsbehov.Sykepengegrunnlagsfakta.Infotrygd(
                        sykepengegrunnlag = BigDecimal("600000.0"),
                    ),
            )
        val context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertEquals(0, observer.behov.size)
    }

    @Test
    fun `Ikke send ut behov dersom person er selvstendig næringsdrivende`() {
        val command = vurderBehovForAvviksvurderingCommand(yrkesaktivitetstype = Yrkesaktivitetstype.SELVSTENDIG)
        val context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        assertTrue(command.execute(context, sessionContext, outbox))
        assertEquals(0, observer.behov.size)
    }

    @Test
    fun `Send ut behov dersom inngangsvilkårene er vurdert i Spleis`() {
        val command = vurderBehovForAvviksvurderingCommand()
        val context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        assertFalse(command.execute(context, sessionContext, outbox))
        assertEquals(1, observer.behov.size)
        val behov = observer.behov.single()
        assertInstanceOf<Behov.Avviksvurdering>(behov)
        assertEquals(expectedOmregnedeÅrsinntekter, behov.omregnedeÅrsinntekter)
        assertEquals(organisasjonsnummer, behov.organisasjonsnummer)
        assertEquals(vilkårsgrunnlagId, behov.vilkårsgrunnlagId)
        assertEquals(skjæringstidspunkt, behov.skjæringstidspunkt)
        assertEquals(legacyBehandling.vedtaksperiodeId(), behov.vedtaksperiodeId)
    }

    @Test
    fun `lagrer ned ny avviksvurdering ved løsning med ny vurdering`() {
        val command = vurderBehovForAvviksvurderingCommand()
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = avviksprosent,
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
            ),
        )
        command.resume(context, sessionContext, outbox)
        val avviksvurderinger = repository.finnAvviksvurderinger(fødselsnummer)
        assertEquals(1, avviksvurderinger.size)
        assertEquals(
            Avviksvurdering(
                unikId = avviksvurderingId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                beregningsgrunnlag = beregningsgrunnlag,
            ),
            avviksvurderinger.single(),
        )
    }

    @Test
    fun `legg til varsel RV_IV_2 dersom avviket er mer enn akseptabelt avvik`() {
        val command = vurderBehovForAvviksvurderingCommand()
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = avviksprosent,
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
            ),
        )
        command.resume(context, sessionContext, outbox)
        assertTrue(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    @Test
    fun `ikke legg til varsel RV_IV_2 dersom avviket er innenfor akseptabelt avvik`() {
        val command = vurderBehovForAvviksvurderingCommand()
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = 25.0,
                harAkseptabeltAvvik = true,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
            ),
        )
        command.resume(context, sessionContext, outbox)
        assertFalse(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    @Test
    fun `lagrer kun ned kobling ved løsning med avviksvurdering som finnes fra før av`() {
        // given
        val command = vurderBehovForAvviksvurderingCommand()
        repository.lagre(enAvviksvurdering(avviksvurderingId = avviksvurderingId))
        val context = CommandContext(UUID.randomUUID())
        context.add(enAvviksvurderingBehovløsning(avviksvurderingId = avviksvurderingId))

        // when
        command.resume(context, sessionContext, outbox)

        // then
        val avviksvurderinger = repository.finnAvviksvurderinger(fødselsnummer)
        assertEquals(1, avviksvurderinger.size)
        assertNotNull(repository.hentAvviksvurdering(vilkårsgrunnlagId))
    }

    @Test
    fun `lager ikke varsel om avvik dersom det ikke har blitt foretatt en ny vurdering`() {
        // given
        val command = vurderBehovForAvviksvurderingCommand()
        val context = CommandContext(UUID.randomUUID())
        repository.lagre(enAvviksvurdering(avviksvurderingId = avviksvurderingId))
        context.add(enAvviksvurderingBehovløsning(avviksvurderingId = avviksvurderingId))

        // when
        command.resume(context, sessionContext, outbox)

        // then
        assertFalse(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    private fun enAvviksvurdering(avviksvurderingId: UUID = this.avviksvurderingId): Avviksvurdering =
        Avviksvurdering(
            unikId = avviksvurderingId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
        )

    private fun enAvviksvurderingBehovløsning(avviksvurderingId: UUID = this.avviksvurderingId): AvviksvurderingBehovLøsning =
        AvviksvurderingBehovLøsning(
            avviksvurderingId = avviksvurderingId,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            maksimaltTillattAvvik = maksimaltTillattAvvik,
            harAkseptabeltAvvik = harAkseptabeltAvvik,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag,
        )

    private fun vurderBehovForAvviksvurderingCommand(
        sykepengegrunnlagsfakta: Godkjenningsbehov.Sykepengegrunnlagsfakta = spleisSykepengegrunnlagsfakta,
        yrkesaktivitetstype: Yrkesaktivitetstype = Yrkesaktivitetstype.ARBEIDSTAKER,
        organisasjonsnummer: String = this.organisasjonsnummer,
    ) = VurderBehovForAvviksvurdering(
        fødselsnummer = fødselsnummer,
        skjæringstidspunkt = skjæringstidspunkt,
        sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        legacyBehandling = legacyBehandling,
        yrkesaktivitetstype = yrkesaktivitetstype,
        organisasjonsnummer = organisasjonsnummer,
    )
}
