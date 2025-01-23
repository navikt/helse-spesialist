package no.nav.helse.db

import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.spesialist.api.overstyring.OverstyringType
import java.util.UUID

interface OverstyringDao {
    fun finnOverstyringerMedTypeForVedtaksperioder(vedtaksperiodeIder: List<UUID>): List<OverstyringType>

    fun finnOverstyringerMedTypeForVedtaksperiode(vedtaksperiodeId: UUID): List<OverstyringType>

    fun finnesEksternHendelseId(eksternHendelseId: UUID): Boolean

    fun kobleOverstyringOgVedtaksperiode(
        vedtaksperiodeIder: List<UUID>,
        overstyringHendelseId: UUID,
    )

    fun harVedtaksperiodePågåendeOverstyring(vedtaksperiodeId: UUID): Boolean

    fun finnAktiveOverstyringer(vedtaksperiodeId: UUID): List<EksternHendelseId>

    fun ferdigstillOverstyringerForVedtaksperiode(vedtaksperiodeId: UUID): Int

    fun persisterOverstyringTidslinje(
        overstyrtTidslinje: OverstyrtTidslinjeForDatabase,
        saksbehandlerOid: UUID,
    )

    fun persisterOverstyringInntektOgRefusjon(
        overstyrtInntektOgRefusjon: OverstyrtInntektOgRefusjonForDatabase,
        saksbehandlerOid: UUID,
    )

    fun persisterSkjønnsfastsettingSykepengegrunnlag(
        skjønnsfastsattSykepengegrunnlag: SkjønnsfastsattSykepengegrunnlagForDatabase,
        saksbehandlerOid: UUID,
    )

    fun persisterMinimumSykdomsgrad(
        minimumSykdomsgrad: MinimumSykdomsgradForDatabase,
        saksbehandlerOid: UUID,
    )

    fun persisterOverstyringArbeidsforhold(
        overstyrtArbeidsforhold: OverstyrtArbeidsforholdForDatabase,
        saksbehandlerOid: UUID,
    )
}
