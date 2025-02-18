package no.nav.helse.spesialist.db.dao

import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase.RAPPORTERT_ÅRSINNTEKT
import no.nav.helse.modell.vedtak.SkjønnsfastsattSykepengegrunnlagDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingstypeDto
import no.nav.helse.modell.vedtak.SkjønnsfastsettingsårsakDto
import no.nav.helse.spesialist.db.AbstractDBIntegrationTest
import no.nav.helse.spesialist.db.jan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class PgSykefraværstilfelleDaoTest : AbstractDBIntegrationTest() {

    private val sykefraværstilfelleDao = PgSykefraværstilfelleDao(session)

    @Test
    fun `Finner skjønnsfastsatt sykepengegrunnlag`() {
        nyPerson()
        opprettSaksbehandler()
        val hendelseId = UUID.randomUUID()
        testhendelse(hendelseId)
        val tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            skjønnsfastsattSykepengegrunnlag =
                skjønnsfastsattSykepengegrunnlag(
                    opprettet = tidspunkt,
                    fødselsnummer = FNR,
                ),
            saksbehandlerOid = SAKSBEHANDLER_OID,
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR)
        assertEquals(1, funnet.size)
        assertEquals(
            SkjønnsfastsattSykepengegrunnlagDto(
                SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT,
                1 jan 2018,
                "mal",
                "fritekst",
                "konklusjon",
                tidspunkt,
            ),
            funnet.single(),
        )
    }

    @Test
    fun `Finner kun data for angitt fnr`() {
        nyPerson()
        val person2 = "111111111111"
        val arbeidsgiver2 = "999999999"
        nyPerson(fødselsnummer = person2, organisasjonsnummer = arbeidsgiver2, vedtaksperiodeId = UUID.randomUUID())
        opprettSaksbehandler()
        val hendelseId1 = UUID.randomUUID()
        val hendelseId2 = UUID.randomUUID()
        testhendelse(hendelseId1)
        testhendelse(hendelseId2)
        val tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            skjønnsfastsattSykepengegrunnlag(
                opprettet = tidspunkt,
                fødselsnummer = FNR,
            ),
            saksbehandlerOid = SAKSBEHANDLER_OID,
        )
        overstyringDao.persisterSkjønnsfastsettingSykepengegrunnlag(
            skjønnsfastsattSykepengegrunnlag(
                fødselsnummer = person2,
                orgnummer = arbeidsgiver2,
                type = RAPPORTERT_ÅRSINNTEKT,
            ),
            saksbehandlerOid = SAKSBEHANDLER_OID,
        )

        val funnet = sykefraværstilfelleDao.finnSkjønnsfastsatteSykepengegrunnlag(FNR)
        assertEquals(1, funnet.size)
        assertEquals(
            SkjønnsfastsattSykepengegrunnlagDto(
                SkjønnsfastsettingstypeDto.OMREGNET_ÅRSINNTEKT,
                SkjønnsfastsettingsårsakDto.ANDRE_AVSNITT,
                1 jan 2018,
                "mal",
                "fritekst",
                "konklusjon",
                tidspunkt,
            ),
            funnet.single(),
        )
    }

    private fun skjønnsfastsattSykepengegrunnlag(
        opprettet: LocalDateTime = LocalDateTime.now(),
        fødselsnummer: String = FNR,
        orgnummer: String = ORGNUMMER,
        type: SkjønnsfastsettingstypeForDatabase = OMREGNET_ÅRSINNTEKT,
        skjæringstidspunkt: LocalDate = 1 jan 2018,
    ): SkjønnsfastsattSykepengegrunnlagForDatabase =
        SkjønnsfastsattSykepengegrunnlagForDatabase(
            eksternHendelseId = UUID.randomUUID(),
            aktørId = AKTØR,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            vedtaksperiodeId = VEDTAKSPERIODE,
            arbeidsgivere =
                listOf(
                    SkjønnsfastsattArbeidsgiverForDatabase(
                        organisasjonsnummer = orgnummer,
                        årlig = 1.0,
                        fraÅrlig = 1.0,
                        årsak = "årsak",
                        type = type,
                        begrunnelseMal = "mal",
                        begrunnelseKonklusjon = "konklusjon",
                        begrunnelseFritekst = "fritekst",
                        lovhjemmel = LovhjemmelForDatabase(paragraf = "paragraf", ledd = "ledd", bokstav = "bokstav"),
                        initierendeVedtaksperiodeId = UUID.randomUUID().toString(),
                    ),
                ),
            opprettet = opprettet,
            saksbehandlerOid = SAKSBEHANDLER_OID,
            ferdigstilt = false,
        )
}
