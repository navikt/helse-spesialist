package no.nav.helse.e2e

import no.nav.helse.VedtaksperiodeInfo
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.AVBRUTT
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.NY
import no.nav.helse.e2e.AbstractE2ETest.Kommandokjedetilstand.SUSPENDERT
import no.nav.helse.objectMapper
import no.nav.helse.spesialist.domain.Periode
import no.nav.helse.spesialist.domain.testfixtures.testdata.lagFødselsnummer
import no.nav.helse.spesialist.e2etests.TestRapidHelpers.oppgaveId
import no.nav.helse.util.februar
import no.nav.helse.util.januar
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class GodkjenningE2ETest : AbstractE2ETest() {

    @Test
    fun `avbryter suspendert kommando når godkjenningsbehov kommer inn på nytt`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov()
        val godkjenningsbehovId1 = sisteGodkjenningsbehovId
        håndterGodkjenningsbehov()
        assertKommandokjedetilstander(godkjenningsbehovId1, NY, SUSPENDERT, SUSPENDERT, AVBRUTT)
    }

    @Test
    fun `tar inn arbeidsgiverinformasjon- og personinfo-behov samtidig`() {
        val arbeidsgiver2 = testperson.orgnummer2
        val arbeidsgiver3 = lagFødselsnummer()
        val andreArbeidsforhold = listOf(arbeidsgiver2, arbeidsgiver3)
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        håndterGodkjenningsbehov(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(orgnummereMedRelevanteArbeidsforhold = andreArbeidsforhold),
        )
        håndterPersoninfoløsning()
        håndterEnhetløsning()
        håndterInfotrygdutbetalingerløsning()
        håndterArbeidsgiverinformasjonløsning()
        assertKommandokjedetilstander(sisteGodkjenningsbehovId, NY, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT, SUSPENDERT)
        assertSisteEtterspurteBehov("Arbeidsforhold")
    }

    @Test
    fun `oppdaterer oppgavens peker til godkjenningsbehov ved mottak av nytt godkjenningsbehov`() {
        vedtaksløsningenMottarNySøknad()
        spleisOppretterNyBehandling()
        val gammelTag = "GAMMEL_KJEDELIG_TAG"
        spesialistBehandlerGodkjenningsbehovFremTilOppgave(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = listOf(gammelTag))
        )
        val oppgaveId = inspektør.oppgaveId()
        val godkjenningsbehovData = finnGodkjenningsbehovJson(oppgaveId).let { it["Godkjenning"] }
        check(godkjenningsbehovData["tags"].first().asText() == gammelTag)

        val nyTag = "NY_OG_BANEBRYTENDE_TAG"
        håndterGodkjenningsbehovUtenValidering(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(tags = listOf(nyTag))
        )
        val oppdaterteGodkjenningsbehovData = finnGodkjenningsbehovJson(oppgaveId).let { it["Godkjenning"] }
        assertEquals(setOf(nyTag), oppdaterteGodkjenningsbehovData["tags"].map { it.asText() }.toSet())
    }

    @Test
    fun `en test som utløser logging fordi det kommer info som ikke matcher med hva en behandling i VedtakFattet har - bør kunne ses i output fra testen`() {
        vedtaksløsningenMottarNySøknad()
        val periode1 = Periode(1.januar, 31.januar)
        val spleisBehandlingId1 = UUID.randomUUID()
        spleisOppretterNyBehandling(spleisBehandlingId = spleisBehandlingId1)
        spesialistInnvilgerAutomatisk()

        val periode2 = Periode(20.januar, 29.januar)
        val spleisBehandlingId2 = UUID.randomUUID()
        val vedtaksperiodeId2 = UUID.randomUUID()
        spleisOppretterNyBehandling(
            fom = periode2.fom,
            tom = periode2.tom,
            spleisBehandlingId = spleisBehandlingId2,
            vedtaksperiodeId = vedtaksperiodeId2
        )
        spleisOppretterNyBehandling(fom = 1.februar, tom = 10.februar, vedtaksperiodeId = testperson.vedtaksperiodeId2)
        håndterGodkjenningsbehovUtenValidering(
            godkjenningsbehovTestdata = godkjenningsbehovTestdata.copy(
                periodeFom = periode2.fom,
                periodeTom = periode2.tom,
                skjæringstidspunkt = 15.januar, // Dette skjer ikke under normale omstendigheter 🙈 Men alt kan gå galt
                vedtaksperiodeId = vedtaksperiodeId2,
                spleisBehandlingId = spleisBehandlingId2,
                perioderMedSammeSkjæringstidspunkt = listOf(
                    VedtaksperiodeInfo(periode1.fom, periode1.tom, testperson.vedtaksperiodeId1, spleisBehandlingId1),
                    VedtaksperiodeInfo(periode2.fom, periode2.tom, vedtaksperiodeId2, spleisBehandlingId2)
                )
            )
        )

        assertSkjæringstidspunkt(1.januar, spleisBehandlingId = spleisBehandlingId1)
    }

    private fun finnGodkjenningsbehovJson(oppgaveId: Long) = dbQuery.single(
        """
        select h.data from hendelse h
        join oppgave o on h.id = o.hendelse_id_godkjenningsbehov
        where o.id = :oppgaveId
        """.trimIndent(),
        "oppgaveId" to oppgaveId
    ) { it.string("data") }.let { objectMapper.readTree(it) }

    private fun assertSkjæringstidspunkt(
        forventetSkjæringstidspunkt: LocalDate,
        spleisBehandlingId: UUID,
    ) {
        val `lagretSkjæringstidspunkt` = dbQuery.single(
            "SELECT skjæringstidspunkt FROM behandling WHERE spleis_behandling_id = :spleisBehandlingId",
            "spleisBehandlingId" to spleisBehandlingId
        ) { it.localDate("skjæringstidspunkt") }

        assertEquals(forventetSkjæringstidspunkt, `lagretSkjæringstidspunkt`)
    }
}

