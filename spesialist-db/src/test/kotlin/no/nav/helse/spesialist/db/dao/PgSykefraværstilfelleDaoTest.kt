package no.nav.helse.spesialist.db.dao

import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.vedtak.SkjønnsfastsettingstypeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.domain.SaksbehandlerOid
import no.nav.helse.spesialist.domain.testfixtures.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class PgSykefraværstilfelleDaoTest : AbstractDBIntegrationTest() {

    private val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)

    @Test
    fun `Finner skjønnsfastsatt sykepengegrunnlag`() {
        nyPerson()
        opprettSaksbehandler()
        val totrinnsvurderingId = opprettTotrinnsvurdering()
        val hendelseId = UUID.randomUUID()
        testhendelse(hendelseId)
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    fødselsnummer = FNR,
                ),
            ),
            totrinnsvurderingId
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR)
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
        nyPerson()
        val person2 = "111111111111"
        val arbeidsgiver2 = "999999999"
        nyPerson(fødselsnummer = person2, organisasjonsnummer = arbeidsgiver2, vedtaksperiodeId = UUID.randomUUID())
        opprettSaksbehandler()
        val totrinnsvurderingId1 = opprettTotrinnsvurdering()
        val totrinnsvurderingId2 = opprettTotrinnsvurdering(fødselsnummer = person2)
        val hendelseId1 = UUID.randomUUID()
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId1)
        testhendelse(hendelseId2)
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    fødselsnummer = FNR,
                ),
            ),
            totrinnsvurderingId1
        )
        overstyringRepository.lagre(
            listOf(
                skjønnsfastsattSykepengegrunnlag(
                    fødselsnummer = person2,
                    orgnummer = arbeidsgiver2,
                    type = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT,
                ),
            ),
            totrinnsvurderingId2
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR)
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
        fødselsnummer: String = FNR,
        orgnummer: String = ORGNUMMER,
        type: SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype = SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ): SkjønnsfastsattSykepengegrunnlag =
        SkjønnsfastsattSykepengegrunnlag.ny(
            aktørId = AKTØR,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = VEDTAKSPERIODE,
            arbeidsgivere =
                listOf(
                    SkjønnsfastsattArbeidsgiver(
                        organisasjonsnummer = orgnummer,
                        årlig = 1.0,
                        fraÅrlig = 1.0,
                        årsak = "årsak",
                        type = type,
                        begrunnelseMal = "mal",
                        begrunnelseKonklusjon = "konklusjon",
                        begrunnelseFritekst = "fritekst",
                        lovhjemmel = Lovhjemmel(
                            paragraf = "paragraf",
                            ledd = "ledd",
                            bokstav = "bokstav",
                            lovverksversjon = "lovverksversjon",
                            lovverk = "lovverk"
                        ),
                        initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                    ),
                ),
            saksbehandlerOid = SaksbehandlerOid(SAKSBEHANDLER_OID),
        )
}
