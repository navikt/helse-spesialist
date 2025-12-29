package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsettingstypeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.Arbeidsgiver
import no.nav.helse.spesialist.domain.ArbeidsgiverIdentifikator
import no.nav.helse.spesialist.domain.Person
import no.nav.helse.spesialist.domain.Saksbehandler
import no.nav.helse.spesialist.domain.Vedtaksperiode
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgSykefraværstilfelleDaoTest : AbstractDBIntegrationTest() {
    private val arbeidsgiver = opprettArbeidsgiver()
    private val person = opprettPerson()
    private val vedtaksperiode =
        opprettVedtaksperiode(person, arbeidsgiver).also {
            opprettBehandling(it).also { behandling ->
                opprettOppgave(it, behandling)
            }
        }

    private val saksbehandler = opprettSaksbehandler()
    private val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)

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

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(person.id.value)
        assertEquals(1, funnet.size)
        val skjønnsfastsattSykepengegrunnlag = funnet.single()
        assertEquals(skjønnsfastsattSykepengegrunnlag.type, SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT)
        assertEquals(skjønnsfastsattSykepengegrunnlag.årsak, SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT)
        assertEquals(skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt, 1 jan 2018)
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal, "mal")
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst, "fritekst")
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon, "konklusjon")
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

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(person.id.value)
        assertEquals(1, funnet.size)
        val skjønnsfastsattSykepengegrunnlag = funnet.single()
        assertEquals(skjønnsfastsattSykepengegrunnlag.type, SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT)
        assertEquals(skjønnsfastsattSykepengegrunnlag.årsak, SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT)
        assertEquals(skjønnsfastsattSykepengegrunnlag.skjæringstidspunkt, 1 jan 2018)
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraMal, "mal")
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraFritekst, "fritekst")
        assertEquals(skjønnsfastsattSykepengegrunnlag.begrunnelseFraKonklusjon, "konklusjon")
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
            aktørId = person.aktørId,
            fødselsnummer = person.id.value,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = vedtaksperiode.id.value,
            arbeidsgivere =
                listOf(
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = arbeidsgiver.organisasjonsnummer,
                        årlig = 1.0,
                        fraÅrlig = 1.0,
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
                        initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                    ),
                ),
            saksbehandlerOid = saksbehandler.id,
        )

    private val Arbeidsgiver.organisasjonsnummer get() =
        when (val id = this.id) {
            is ArbeidsgiverIdentifikator.Fødselsnummer -> id.fødselsnummer
            is ArbeidsgiverIdentifikator.Organisasjonsnummer -> id.organisasjonsnummer
        }
}
