package no.nav.helse.spesialist.application.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.mediator.CommandContextObserver
import no.nav.helse.modell.kommando.CommandContext
import no.nav.helse.modell.kommando.VurderBehovForAvviksvurdering
import no.nav.helse.modell.melding.Behov
import no.nav.helse.modell.person.vedtaksperiode.Varsel.Companion.inneholderVarselOmAvvik
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.domain.legacy.LegacyBehandling
import no.nav.helse.spesialist.testhjelp.jan
import no.nav.helse.spesialist.testhjelp.lagFødselsnummer
import no.nav.helse.spesialist.testhjelp.lagOrganisasjonsnummer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class VurderBehovForAvviksvurderingTest {

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
    private val omregnedeÅrsinntekter = listOf(OmregnetÅrsinntekt(organisasjonsnummer, beregningsgrunnlagTotalbeløp))
    private val beregningsgrunnlag = Beregningsgrunnlag(
        totalbeløp = beregningsgrunnlagTotalbeløp,
        omregnedeÅrsinntekter = omregnedeÅrsinntekter
    )

    private val sammenligningsgrunnlag = Sammenligningsgrunnlag(
        totalbeløp = sammenligningsgrunnlagTotalbeløp,
        innrapporterteInntekter = listOf(
            InnrapportertInntekt(
                arbeidsgiverreferanse = organisasjonsnummer,
                inntekter = listOf(Inntekt(YearMonth.of(2018, 1), sammenligningsgrunnlagTotalbeløp))
            )
        )
    )

    private val legacyBehandling = LegacyBehandling(
        id = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        fom = 1 jan 2018,
        tom = 31 jan 2018,
        skjæringstidspunkt = 1 jan 2018,
        spleisBehandlingId = UUID.randomUUID(),
        utbetalingId = null
    )

    private val repository = object : AvviksvurderingRepository {
        var avviksvurderingSomSkalReturneres: Avviksvurdering? = null
        val avviksvurderinger = mutableListOf<Avviksvurdering>()
        val koblinger = mutableListOf<Pair<UUID, UUID>>()
        override fun lagre(avviksvurdering: Avviksvurdering) {
            avviksvurderinger.add(avviksvurdering)
        }

        override fun opprettKobling(avviksvurderingId: UUID, vilkårsgrunnlagId: UUID) {
            koblinger.add(avviksvurderingId to vilkårsgrunnlagId)
        }

        override fun hentAvviksvurdering(vilkårsgrunnlagId: UUID): Avviksvurdering = error("Ikke implementert i test")
        override fun hentAvviksvurderingFor(avviksvurderingId: UUID): Avviksvurdering? =
            avviksvurderingSomSkalReturneres

        override fun finnAvviksvurderinger(fødselsnummer: String): List<Avviksvurdering> =
            error("Ikke implementert i test")
    }

    private val observer = object : CommandContextObserver {
        val behov = mutableListOf<Behov>()
        override fun behov(behov: Behov, commandContextId: UUID) {
            this.behov.add(behov)
        }
    }

    @Test
    fun `Ikke send ut behov dersom inngangsvilkårene ikke er vurdert i Spleis`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            false,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        assertTrue(command.execute(context))
        assertEquals(0, observer.behov.size)
    }

    @Test
    fun `Send ut behov dersom inngangsvilkårene er vurdert i Spleis`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        context.nyObserver(observer)
        assertFalse(command.execute(context))
        assertEquals(1, observer.behov.size)
        val behov = observer.behov.single()
        assertInstanceOf<Behov.Avviksvurdering>(behov)
        assertEquals(omregnedeÅrsinntekter, behov.omregnedeÅrsinntekter)
        assertEquals(organisasjonsnummer, behov.organisasjonsnummer)
        assertEquals(vilkårsgrunnlagId, behov.vilkårsgrunnlagId)
        assertEquals(skjæringstidspunkt, behov.skjæringstidspunkt)
        assertEquals(legacyBehandling.vedtaksperiodeId(), behov.vedtaksperiodeId)
    }

    @Test
    fun `lagrer ned ny avviksvurdering ved løsning med ny vurdering`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = avviksprosent,
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag
            )
        )
        command.resume(context)
        assertEquals(1, repository.avviksvurderinger.size)
        assertEquals(
            Avviksvurdering(
                unikId = avviksvurderingId,
                vilkårsgrunnlagId = vilkårsgrunnlagId,
                fødselsnummer = fødselsnummer,
                skjæringstidspunkt = skjæringstidspunkt,
                opprettet = opprettet,
                avviksprosent = avviksprosent,
                sammenligningsgrunnlag = sammenligningsgrunnlag,
                beregningsgrunnlag = beregningsgrunnlag
            ),
            repository.avviksvurderinger.single()
        )
    }

    @Test
    fun `legg til varsel RV_IV_2 dersom avviket er mer enn akseptabelt avvik`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = avviksprosent,
                harAkseptabeltAvvik = harAkseptabeltAvvik,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag
            )
        )
        command.resume(context)
        assertTrue(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    @Test
    fun `ikke legg til varsel RV_IV_2 dersom avviket er innenfor akseptabelt avvik`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning(
                avviksvurderingId = avviksvurderingId,
                maksimaltTillattAvvik = maksimaltTillattAvvik,
                avviksprosent = 25.0,
                harAkseptabeltAvvik = true,
                opprettet = opprettet,
                beregningsgrunnlag = beregningsgrunnlag,
                sammenligningsgrunnlag = sammenligningsgrunnlag
            )
        )
        command.resume(context)
        assertFalse(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    @Test
    fun `lagrer kun ned kobling ved løsning med avviksvurdering som finnes fra før av`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        repository.avviksvurderingSomSkalReturneres = enAvviksvurdering(avviksvurderingId = avviksvurderingId)
        val context = CommandContext(UUID.randomUUID())
        context.add(enAvviksvurderingBehovløsning(avviksvurderingId = avviksvurderingId))
        command.resume(context)
        assertEquals(0, repository.avviksvurderinger.size)
        assertEquals(1, repository.koblinger.size)
        assertEquals(avviksvurderingId to vilkårsgrunnlagId, repository.koblinger.single())
    }

    @Test
    fun `lager ikke varsel om avvik dersom det ikke har blitt foretatt en ny vurdering`() {
        val command = VurderBehovForAvviksvurdering(
            fødselsnummer,
            skjæringstidspunkt,
            repository,
            omregnedeÅrsinntekter,
            vilkårsgrunnlagId,
            legacyBehandling,
            true,
            organisasjonsnummer
        )
        val context = CommandContext(UUID.randomUUID())
        repository.avviksvurderingSomSkalReturneres = enAvviksvurdering(avviksvurderingId = avviksvurderingId)
        context.add(enAvviksvurderingBehovløsning(avviksvurderingId = avviksvurderingId))
        command.resume(context)
        assertFalse(legacyBehandling.varsler().inneholderVarselOmAvvik())
    }

    private fun enAvviksvurdering(avviksvurderingId: UUID = this.avviksvurderingId): Avviksvurdering {
        return Avviksvurdering(
            unikId = avviksvurderingId,
            vilkårsgrunnlagId = vilkårsgrunnlagId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )
    }

    private fun enAvviksvurderingBehovløsning(avviksvurderingId: UUID = this.avviksvurderingId): AvviksvurderingBehovLøsning {
        return AvviksvurderingBehovLøsning(
            avviksvurderingId = avviksvurderingId,
            opprettet = opprettet,
            avviksprosent = avviksprosent,
            maksimaltTillattAvvik = maksimaltTillattAvvik,
            harAkseptabeltAvvik = harAkseptabeltAvvik,
            sammenligningsgrunnlag = sammenligningsgrunnlag,
            beregningsgrunnlag = beregningsgrunnlag
        )
    }
}
