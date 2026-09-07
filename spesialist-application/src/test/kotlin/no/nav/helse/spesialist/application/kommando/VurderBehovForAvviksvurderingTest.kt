package no.nav.helse.spesialist.application.kommando

import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForAvviksvurdering
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.vedtaksperiode.Godkjenningsbehov
import no.nav.helse.modell.vedtaksperiode.Yrkesaktivitetstype
import no.nav.helse.modell.vilkårsprøving.*
import no.nav.helse.spesialist.domain.SpleisBehandlingId
import no.nav.helse.spesialist.domain.Varsel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertNotNull
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.*

class VurderBehovForAvviksvurderingTest : ApplicationTest() {
    private val vilkårsgrunnlagId = UUID.randomUUID()
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
                        organisasjonsnummer = vedtaksperiode1.organisasjonsnummer,
                        omregnetÅrsinntekt = beregningsgrunnlagTotalbeløp,
                        inntektskilde = Godkjenningsbehov.Sykepengegrunnlagsfakta.Spleis.Arbeidsgiver.Inntektskilde.Arbeidsgiver,
                    ),
                ),
            sykepengegrunnlag = BigDecimal("666666.0"),
        )
    private val expectedOmregnedeÅrsinntekter = listOf(OmregnetÅrsinntekt(vedtaksperiode1.organisasjonsnummer, beregningsgrunnlagTotalbeløp))
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
                        arbeidsgiverreferanse = vedtaksperiode1.organisasjonsnummer,
                        inntekter = listOf(Inntekt(YearMonth.of(2018, 1), sammenligningsgrunnlagTotalbeløp)),
                    ),
                ),
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
        assertEquals(vedtaksperiode1.organisasjonsnummer, behov.organisasjonsnummer)
        assertEquals(vilkårsgrunnlagId, behov.vilkårsgrunnlagId)
        assertEquals(behandling1.skjæringstidspunkt, behov.skjæringstidspunkt)
        assertEquals(vedtaksperiode1.id.value, behov.vedtaksperiodeId)
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
        val avviksvurderinger = repository.finnAvviksvurderinger(person.id.value)
        assertEquals(1, avviksvurderinger.size)
        assertEquals(
            Avviksvurdering(
                unikId = avviksvurderingId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = person.id.value,
                skjæringstidspunkt = behandling1.skjæringstidspunkt,
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
        behandling1.assertHarVarsel("RV_IV_2", Varsel.Status.AKTIV)
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
        behandling1.assertHarIkkeVarsel("RV_IV_2")
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
        val avviksvurderinger = repository.finnAvviksvurderinger(person.id.value)
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
        behandling1.assertHarIkkeVarsel("RV_IV_2")
    }

    private fun enAvviksvurdering(avviksvurderingId: UUID = this.avviksvurderingId): Avviksvurdering =
        Avviksvurdering(
            unikId = avviksvurderingId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = person.id.value,
            skjæringstidspunkt = behandling1.skjæringstidspunkt,
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
        organisasjonsnummer: String = vedtaksperiode1.organisasjonsnummer,
    ) = VurderBehovForAvviksvurdering(
        fødselsnummer = person.id.value,
        skjæringstidspunkt = behandling1.skjæringstidspunkt,
        sykepengegrunnlagsfakta = sykepengegrunnlagsfakta,
        vilkårsgrunnlagId = vilkårsgrunnlagId,
        yrkesaktivitetstype = yrkesaktivitetstype,
        organisasjonsnummer = organisasjonsnummer,
        vedtaksperiodeId = vedtaksperiode1.id,
        spleisBehandlingId = SpleisBehandlingId(godkjenningsbehovData.spleisBehandlingId),
    )
}
