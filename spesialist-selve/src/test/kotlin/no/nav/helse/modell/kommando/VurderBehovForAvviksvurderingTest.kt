package no.nav.helse.modell.kommando

import no.nav.helse.db.AvviksvurderingRepository
import no.nav.helse.modell.vilkårsprøving.Avviksvurdering
import no.nav.helse.modell.vilkårsprøving.AvviksvurderingBehovLøsning
import no.nav.helse.modell.vilkårsprøving.Beregningsgrunnlag
import no.nav.helse.modell.vilkårsprøving.InnrapportertInntekt
import no.nav.helse.modell.vilkårsprøving.Inntekt
import no.nav.helse.modell.vilkårsprøving.OmregnetÅrsinntekt
import no.nav.helse.modell.vilkårsprøving.Sammenligningsgrunnlag
import no.nav.helse.spesialist.test.lagFødselsnummer
import no.nav.helse.spesialist.test.lagOrganisasjonsnummer
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class VurderBehovForAvviksvurderingTest {

    private val fødselsnummer = lagFødselsnummer()
    private val organisasjonsnummer = lagOrganisasjonsnummer()
    private val vilkårsgrunnlagId = UUID.randomUUID()
    private val skjæringstidspunkt = 1.januar
    private val opprettet = LocalDateTime.now()
    private val avviksvurderingId = UUID.randomUUID()
    private val maksimaltTillattAvvik = 25.0
    private val avviksprosent = 50.0
    private val harAkseptabeltAvvik = false
    private val beregningsgrunnlagTotalbeløp = 900000.0
    private val sammenligningsgrunnlagTotalbeløp = 600000.0
    private val beregningsgrunnlag = Beregningsgrunnlag(
        totalbeløp = beregningsgrunnlagTotalbeløp,
        omregnedeÅrsinntekter = listOf(OmregnetÅrsinntekt(organisasjonsnummer, beregningsgrunnlagTotalbeløp))
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

    private val repository = object : AvviksvurderingRepository {
        val avviksvurderinger = mutableListOf<Avviksvurdering>()
        val koblinger = mutableListOf<Pair<UUID, UUID>>()
        override fun lagre(avviksvurdering: Avviksvurdering) {
            avviksvurderinger.add(avviksvurdering)
        }
        override fun opprettKobling(avviksvurderingId: UUID, vilkårsgrunnlagId: UUID) {
            koblinger.add(avviksvurderingId to vilkårsgrunnlagId)
        }
        override fun finnAvviksvurderinger(fødselsnummer: String): List<Avviksvurdering> = error("Ikke implementert i test")
    }

    @Test
    fun `lagrer ned ny avviksvurdering ved løsning med ny vurdering`() {
        val command = VurderBehovForAvviksvurdering(fødselsnummer, skjæringstidspunkt, repository, beregningsgrunnlag, vilkårsgrunnlagId)
        val context = CommandContext(UUID.randomUUID())
        context.add(
            AvviksvurderingBehovLøsning.NyVurderingForetatt(
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
    fun `lagrer ned kobling ved løsning med uten vurdering`() {
        val command = VurderBehovForAvviksvurdering(fødselsnummer, skjæringstidspunkt, repository, beregningsgrunnlag, vilkårsgrunnlagId)
        val context = CommandContext(UUID.randomUUID())
        context.add(AvviksvurderingBehovLøsning.TrengerIkkeNyVurdering(avviksvurderingId = avviksvurderingId))
        command.resume(context)
        assertEquals(0, repository.avviksvurderinger.size)
        assertEquals(1, repository.koblinger.size)
        assertEquals(avviksvurderingId to vilkårsgrunnlagId, repository.koblinger.single())
    }
}
