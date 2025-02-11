package no.nav.helse.spesialist.db

import no.nav.helse.db.PgOverstyringDao
import no.nav.helse.db.PgSaksbehandlerDao
import no.nav.helse.db.PgTotrinnsvurderingDao
import no.nav.helse.db.TotrinnsvurderingFraDatabase
import no.nav.helse.db.overstyring.ArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.LovhjemmelForDatabase
import no.nav.helse.db.overstyring.MinimumSykdomsgradForDatabase
import no.nav.helse.db.overstyring.OverstyringForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsforholdForDatabase
import no.nav.helse.db.overstyring.OverstyrtArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.OverstyrtInntektOgRefusjonForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjeForDatabase
import no.nav.helse.db.overstyring.OverstyrtTidslinjedagForDatabase
import no.nav.helse.db.overstyring.RefusjonselementForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattArbeidsgiverForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsattSykepengegrunnlagForDatabase
import no.nav.helse.db.overstyring.SkjønnsfastsettingstypeForDatabase
import no.nav.helse.modell.saksbehandler.handlinger.Arbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgrad
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.MinimumSykdomsgradPeriode
import no.nav.helse.modell.saksbehandler.handlinger.Overstyring
import no.nav.helse.modell.saksbehandler.handlinger.OverstyringId
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsforhold
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtInntektOgRefusjon
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinje
import no.nav.helse.modell.saksbehandler.handlinger.OverstyrtTidslinjedag
import no.nav.helse.modell.saksbehandler.handlinger.Refusjonselement
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattArbeidsgiver
import no.nav.helse.modell.saksbehandler.handlinger.SkjønnsfastsattSykepengegrunnlag
import no.nav.helse.modell.totrinnsvurdering.Totrinnsvurdering
import no.nav.helse.modell.vilkårsprøving.Lovhjemmel
import no.nav.helse.spesialist.application.TotrinnsvurderingRepository
import java.util.UUID

class PgTotrinnsvurderingRepository(
    private val overstyringDao: PgOverstyringDao,
    private val saksbehandlerDao: PgSaksbehandlerDao,
    private val totrinnsvurderingDao: PgTotrinnsvurderingDao,
) : TotrinnsvurderingRepository {
    override fun finnTotrinnsvurdering(fødselsnummer: String): Totrinnsvurdering? {
        val (id, totrinnsvurderingFraDatabase) =
            totrinnsvurderingDao.hentAktivTotrinnsvurdering(fødselsnummer)
                ?: return null

        return totrinnsvurderingFraDatabase.tilDomene(id, overstyringDao.finnOverstyringer(fødselsnummer).tilDomene())
    }

    @Deprecated("Skal fjernes, midlertidig i bruk for å tette et hull")
    override fun finnTotrinnsvurdering(vedtaksperiodeId: UUID): Totrinnsvurdering? {
        val (id, totrinnsvurderingFraDatabase) =
            totrinnsvurderingDao.hentAktivTotrinnsvurdering(vedtaksperiodeId)
                ?: return null

        return totrinnsvurderingFraDatabase.tilDomene(id, emptyList())
    }

    override fun lagre(
        totrinnsvurdering: Totrinnsvurdering,
        fødselsnummer: String,
    ) {
        val totrinnsvurderingFraDatabase = totrinnsvurdering.tilDatabase()
        if (totrinnsvurdering.harFåttTildeltId()) {
            totrinnsvurderingDao.update(totrinnsvurdering.id(), totrinnsvurderingFraDatabase)
        } else {
            totrinnsvurderingDao.insert(totrinnsvurderingFraDatabase).also { totrinnsvurdering.tildelId(it) }
        }
    }

    private fun TotrinnsvurderingFraDatabase.tilDomene(
        id: Long,
        overstyringer: List<Overstyring>,
    ): Totrinnsvurdering {
        return Totrinnsvurdering.Companion.fraLagring(
            id = id,
            vedtaksperiodeId = this.vedtaksperiodeId,
            erRetur = this.erRetur,
            saksbehandler =
                this.saksbehandler?.let {
                    saksbehandlerDao.finnSaksbehandler(
                        it,
                    )
                },
            beslutter =
                this.beslutter?.let {
                    saksbehandlerDao.finnSaksbehandler(
                        it,
                    )
                },
            utbetalingId = this.utbetalingId,
            opprettet = this.opprettet,
            oppdatert = this.oppdatert,
            ferdigstilt = false,
            overstyringer = overstyringer,
        )
    }

    private fun List<Pair<Long, OverstyringForDatabase>>.tilDomene(): List<Overstyring> =
        map { (id, overstyring) ->
            when (overstyring) {
                is OverstyrtTidslinjeForDatabase -> overstyring.tilDomene(id)
                is MinimumSykdomsgradForDatabase -> overstyring.tilDomene(id)
                is OverstyrtArbeidsforholdForDatabase -> overstyring.tilDomene(id)
                is OverstyrtInntektOgRefusjonForDatabase -> overstyring.tilDomene(id)
                is SkjønnsfastsattSykepengegrunnlagForDatabase -> overstyring.tilDomene(id)
            }
        }

    private fun OverstyrtTidslinjeForDatabase.tilDomene(id: Long): OverstyrtTidslinje =
        OverstyrtTidslinje.Companion.fraLagring(
            id = OverstyringId(id),
            eksternHendelseId = eksternHendelseId,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            organisasjonsnummer = organisasjonsnummer,
            dager = dager.map { it.tilDomene() },
            begrunnelse = begrunnelse,
            saksbehandlerOid = saksbehandlerOid,
        )

    private fun MinimumSykdomsgradForDatabase.tilDomene(id: Long): MinimumSykdomsgrad =
        MinimumSykdomsgrad.Companion.fraLagring(
            id = OverstyringId(id),
            eksternHendelseId = eksternHendelseId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            perioderVurdertOk = perioderVurdertOk.map { it.tilDomene() },
            perioderVurdertIkkeOk = perioderVurdertIkkeOk.map { it.tilDomene() },
            begrunnelse = begrunnelse,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
        )

    private fun OverstyrtArbeidsforholdForDatabase.tilDomene(id: Long) =
        OverstyrtArbeidsforhold.Companion.fraLagring(
            id = OverstyringId(id),
            eksternHendelseId = eksternHendelseId,
            vedtaksperiodeId = vedtaksperiodeId,
            fødselsnummer = fødselsnummer,
            aktørId = aktørId,
            skjæringstidspunkt = skjæringstidspunkt,
            overstyrteArbeidsforhold = overstyrteArbeidsforhold.map { it.tilDomene() },
            saksbehandlerOid = saksbehandlerOid,
        )

    private fun OverstyrtInntektOgRefusjonForDatabase.tilDomene(id: Long) =
        OverstyrtInntektOgRefusjon.Companion.fraLagring(
            id = OverstyringId(id),
            eksternHendelseId = eksternHendelseId,
            vedtaksperiodeId = vedtaksperiodeId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
            saksbehandlerOid = saksbehandlerOid,
        )

    private fun SkjønnsfastsattSykepengegrunnlagForDatabase.tilDomene(id: Long) =
        SkjønnsfastsattSykepengegrunnlag.Companion.fraLagring(
            id = OverstyringId(id),
            eksternHendelseId = eksternHendelseId,
            aktørId = aktørId,
            fødselsnummer = fødselsnummer,
            skjæringstidspunkt = skjæringstidspunkt,
            arbeidsgivere = arbeidsgivere.map { it.tilDomene() },
            vedtaksperiodeId = vedtaksperiodeId,
            saksbehandlerOid = saksbehandlerOid,
        )

    private fun SkjønnsfastsattArbeidsgiverForDatabase.tilDomene() =
        SkjønnsfastsattArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            årlig = årlig,
            fraÅrlig = fraÅrlig,
            årsak = årsak,
            type = type.tilDomene(),
            begrunnelseMal = begrunnelseMal,
            begrunnelseFritekst = begrunnelseFritekst,
            begrunnelseKonklusjon = begrunnelseKonklusjon,
            lovhjemmel = lovhjemmel?.tilDomene(),
            initierendeVedtaksperiodeId = initierendeVedtaksperiodeId,
        )

    private fun SkjønnsfastsettingstypeForDatabase.tilDomene(): SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype =
        when (this) {
            SkjønnsfastsettingstypeForDatabase.OMREGNET_ÅRSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.OMREGNET_ÅRSINNTEKT
            SkjønnsfastsettingstypeForDatabase.RAPPORTERT_ÅRSINNTEKT -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.RAPPORTERT_ÅRSINNTEKT
            SkjønnsfastsettingstypeForDatabase.ANNET -> SkjønnsfastsattArbeidsgiver.Skjønnsfastsettingstype.ANNET
        }

    private fun OverstyrtArbeidsgiverForDatabase.tilDomene() =
        OverstyrtArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            månedligInntekt = månedligInntekt,
            fraMånedligInntekt = fraMånedligInntekt,
            refusjonsopplysninger = refusjonsopplysninger?.map { it.tilDomene() },
            fraRefusjonsopplysninger = fraRefusjonsopplysninger?.map { it.tilDomene() },
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            lovhjemmel = lovhjemmel?.tilDomene(),
            fom = fom,
            tom = fom,
        )

    private fun RefusjonselementForDatabase.tilDomene() =
        Refusjonselement(
            fom = fom,
            tom = tom,
            beløp = beløp,
        )

    private fun ArbeidsforholdForDatabase.tilDomene() =
        Arbeidsforhold(
            organisasjonsnummer = organisasjonsnummer,
            deaktivert = deaktivert,
            begrunnelse = begrunnelse,
            forklaring = forklaring,
            lovhjemmel = null,
        )

    private fun MinimumSykdomsgradForDatabase.MinimumSykdomsgradPeriodeForDatabase.tilDomene(): MinimumSykdomsgradPeriode =
        MinimumSykdomsgradPeriode(
            fom = fom,
            tom = tom,
        )

    private fun MinimumSykdomsgradForDatabase.MinimumSykdomsgradArbeidsgiverForDatabase.tilDomene(): MinimumSykdomsgradArbeidsgiver =
        MinimumSykdomsgradArbeidsgiver(
            organisasjonsnummer = organisasjonsnummer,
            berørtVedtaksperiodeId = berørtVedtaksperiodeId,
        )

    private fun OverstyrtTidslinjedagForDatabase.tilDomene() =
        OverstyrtTidslinjedag(
            dato = dato,
            type = type,
            grad = grad,
            fraGrad = fraGrad,
            fraType = fraType,
            lovhjemmel = lovhjemmel?.tilDomene(),
        )

    private fun LovhjemmelForDatabase.tilDomene() =
        Lovhjemmel(
            paragraf = paragraf,
            ledd = ledd,
            bokstav = bokstav,
            lovverk = "",
            lovverksversjon = "",
        )

    private fun Totrinnsvurdering.tilDatabase() =
        TotrinnsvurderingFraDatabase(
            vedtaksperiodeId = vedtaksperiodeId,
            erRetur = erRetur,
            saksbehandler = saksbehandler?.oid,
            beslutter = beslutter?.oid,
            utbetalingId = utbetalingId,
            opprettet = opprettet,
            oppdatert = oppdatert,
        )
}
