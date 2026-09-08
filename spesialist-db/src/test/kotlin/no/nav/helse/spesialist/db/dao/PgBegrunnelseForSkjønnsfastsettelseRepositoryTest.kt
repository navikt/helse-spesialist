package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.vedtak.Skjønnsfastsettingstype
import no.nav.helse.modell.vedtak.Skjønnsfastsettingsårsak
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattArbeidsgiver
import no.nav.helse.spesialist.domain.overstyringer.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class PgBegrunnelseForSkjønnsfastsettelseRepositoryTest : AbstractDBIntegrationTest() {
    private val arbeidsgiver = opprettArbeidsgiver()
    private val person = opprettPerson()
    private val vedtaksperiode =
        opprettVedtaksperiode(person, arbeidsgiver).also {
            opprettBehandling(it).also { behandling ->
                opprettOppgave(it, behandling)
            }
        }

    private val saksbehandler = opprettSaksbehandler()
    private val sykefraværstilfelleDao = PgBegrunnelseForSkjønnsfastsettelseRepository(session)

    @Test
    fun `Finner skjønnsfastsatt sykepengegrunnlag`() {
        val totrinnsvurderingId = opprettTotrinnsvurdering(person)
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiode = vedtaksperiode,
                    saksbehandler = saksbehandler,
                ),
            ),
            totrinnsvurderingId,
        )

        val funnet = sykefraværstilfelleDao.finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(person.id)
        assertEquals(1, funnet.size)
        val skjønnsfastsattSykepengegrunnlag = funnet.single()
        assertEquals(Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT, skjønnsfastsattSykepengegrunnlag.type)
        assertEquals(Skjønnsfastsettingsårsak.ANDRE_AVSNITT, skjønnsfastsattSykepengegrunnlag.årsak)
        assertEquals(1 jan 2018, skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt)
        assertEquals("mal", skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal)
        assertEquals("fritekst", skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst)
        assertEquals("konklusjon", skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon)
    }

    @Test
    fun `Finner kun data for angitt fnr`() {
        val arbeidsgiver2 = opprettArbeidsgiver()
        val person2 = opprettPerson()
        val vedtaksperiode2 =
            opprettVedtaksperiode(person2, arbeidsgiver2).also {
                opprettBehandling(it).also { behandling ->
                    opprettOppgave(it, behandling)
                }
            }

        val totrinnsvurderingId1 = opprettTotrinnsvurdering(person)
        val totrinnsvurderingId2 = opprettTotrinnsvurdering(person2)
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    person = person,
                    arbeidsgiver = arbeidsgiver,
                    vedtaksperiode = vedtaksperiode,
                    saksbehandler = saksbehandler,
                ),
            ),
            totrinnsvurderingId1,
        )
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    person = person2,
                    arbeidsgiver = arbeidsgiver2,
                    vedtaksperiode = vedtaksperiode2,
                    saksbehandler = saksbehandler,
                    type = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT,
                ),
            ),
            totrinnsvurderingId2,
        )

        val funnet = sykefraværstilfelleDao.finnBegrunnelseForSkjønnsfastsattSykepengegrunnlag(person.id)
        assertEquals(1, funnet.size)
        val skjønnsfastsattSykepengegrunnlag = funnet.single()
        assertEquals(Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT, skjønnsfastsattSykepengegrunnlag.type)
        assertEquals(Skjønnsfastsettingsårsak.ANDRE_AVSNITT, skjønnsfastsattSykepengegrunnlag.årsak)
        assertEquals(1 jan 2018, skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt)
        assertEquals("mal", skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal)
        assertEquals("fritekst", skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst)
        assertEquals("konklusjon", skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon)
    }

    private fun skjønnsfastsattSykepengegrunnlag(
        person: Person,
        arbeidsgiver: Arbeidsgiver,
        vedtaksperiode: Vedtaksperiode,
        saksbehandler: Saksbehandler,
        type: SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag.ny(
            saksbehandlerOid = saksbehandler.id,
            fødselsnummer = person.id.value,
            aktørId = person.aktørId,
            vedtaksperiodeId = vedtaksperiode.id.value,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere =
                listOf(
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        årlig = 1.0,
                        fraÅrlig = 1.0,
                    ),
                ),
            årsak = "årsak",
            type = type,
            begrunnelseMal = "mal",
            begrunnelseKonklusjon = "konklusjon",
            begrunnelseFritekst = "fritekst",
            lovhjemmel =
                Lovhjemmel(
                    paragraf = "paragraf",
                    ledd = "ledd",
                    bokstav = "bokstav",
                    lovverksversjon = "lovverksversjon",
                    lovverk = "lovverk",
                ),
        )

    private val Arbeidsgiver.organisasjonsnummer get() =
        when (val id = this.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
}
