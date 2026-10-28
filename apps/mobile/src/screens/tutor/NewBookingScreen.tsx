import { useCallback, useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, ScrollView, Text } from 'react-native';

import { useApi } from '../../api/ApiContext';
import { toUserMessage } from '../../api/ApiError';
import { SERVICE_TYPES, type PriceQuote, type ServiceType } from '../../api/types';
import { Button, Card, Chip, ChipGroup, EmptyState, ErrorBanner, Loading, SectionTitle, colors, styles } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import type { RootScreenProps } from '../../navigation/types';
import { useTutorSession } from '../../state/SessionContext';
import { SERVICE_TYPE_LABELS, formatCents, formatDuration, formatPeriod } from '../../utils/format';
import { DAY_OPTIONS, DURATION_OPTIONS, TIME_OPTIONS, buildPeriod, isInFuture } from '../../utils/schedule';

/** Tela 3b: nova reserva com orçamento (GET /bookings/quote) e confirmação (POST /bookings). */
export function NewBookingScreen({ route, navigation }: RootScreenProps<'NewBooking'>) {
  const { caregiverId, caregiverName } = route.params;
  const api = useApi();
  const tutor = useTutorSession();

  const petsLoader = useCallback(() => api.listTutorPets(tutor.id), [api, tutor.id]);
  const { data: pets, loading: loadingPets, error: petsError, reload: reloadPets } = useAsync(petsLoader);

  const [chosenPetId, setPetId] = useState<string | null>(null);
  const [serviceType, setServiceType] = useState<ServiceType>('WALK');
  const [dayOffset, setDayOffset] = useState<number>(1);
  const [time, setTime] = useState<string>('09:00');
  const [duration, setDuration] = useState<number>(DURATION_OPTIONS.WALK[1]);

  const [quoteResult, setQuoteResult] = useState<{ key: string; quote: PriceQuote | null; error: string | null } | null>(
    null,
  );
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Sem escolha explícita, usa o primeiro pet.
  const petId = chosenPetId ?? pets?.[0]?.id ?? null;

  const period = useMemo(() => buildPeriod(new Date(), dayOffset, time, duration), [dayOffset, time, duration]);
  const startsInFuture = isInFuture(period.start);
  const scheduledStart = period.start.toISOString();
  const scheduledEnd = period.end.toISOString();

  // Atualiza o orçamento sempre que serviço ou período mudam.
  const quoteKey = `${serviceType}|${scheduledStart}|${scheduledEnd}`;
  useEffect(() => {
    let active = true;
    api.quoteBooking({ serviceType, scheduledStart, scheduledEnd }).then(
      (quote) => active && setQuoteResult({ key: quoteKey, quote, error: null }),
      (e: unknown) => active && setQuoteResult({ key: quoteKey, quote: null, error: toUserMessage(e) }),
    );
    return () => {
      active = false;
    };
  }, [api, quoteKey, serviceType, scheduledStart, scheduledEnd]);
  const quoting = quoteResult?.key !== quoteKey;
  const quote = quoting ? null : (quoteResult?.quote ?? null);
  const quoteError = quoting ? null : (quoteResult?.error ?? null);

  const selectService = (type: ServiceType) => {
    setServiceType(type);
    setDuration(DURATION_OPTIONS[type][0]);
  };

  const confirm = async () => {
    if (!petId) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      await api.createBooking({ tutorId: tutor.id, petId, caregiverId, serviceType, scheduledStart, scheduledEnd });
      navigation.navigate('TutorHome', { screen: 'TutorBookings' });
    } catch (e) {
      setSubmitError(toUserMessage(e));
    } finally {
      setSubmitting(false);
    }
  };

  if (loadingPets) return <Loading />;

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Reservar com {caregiverName}</Text>
      <ErrorBanner message={petsError} onRetry={() => void reloadPets()} />

      <SectionTitle>Pet</SectionTitle>
      {pets?.length === 0 ? <EmptyState message="Cadastre um pet na aba Meus pets antes de reservar." /> : null}
      <ChipGroup>
        {pets?.map((pet) => (
          <Chip key={pet.id} label={pet.name} selected={petId === pet.id} onPress={() => setPetId(pet.id)} />
        ))}
      </ChipGroup>

      <SectionTitle>Serviço</SectionTitle>
      <ChipGroup>
        {SERVICE_TYPES.map((type) => (
          <Chip key={type} label={SERVICE_TYPE_LABELS[type]} selected={serviceType === type} onPress={() => selectService(type)} />
        ))}
      </ChipGroup>

      <SectionTitle>Dia</SectionTitle>
      <ChipGroup>
        {DAY_OPTIONS.map((d) => (
          <Chip key={d.offset} label={d.label} selected={dayOffset === d.offset} onPress={() => setDayOffset(d.offset)} />
        ))}
      </ChipGroup>

      <SectionTitle>Horário de início</SectionTitle>
      <ChipGroup>
        {TIME_OPTIONS.map((t) => (
          <Chip key={t} label={t} selected={time === t} onPress={() => setTime(t)} />
        ))}
      </ChipGroup>

      <SectionTitle>Duração</SectionTitle>
      <ChipGroup>
        {DURATION_OPTIONS[serviceType].map((m) => (
          <Chip key={m} label={formatDuration(m)} selected={duration === m} onPress={() => setDuration(m)} />
        ))}
      </ChipGroup>

      <Card>
        <Text style={styles.label}>Resumo</Text>
        <Text>
          {SERVICE_TYPE_LABELS[serviceType]} · {formatPeriod(period.start, period.end)}
        </Text>
        {!startsInFuture ? (
          <Text style={{ color: colors.danger }}>Esse horário já passou. Escolha outro dia ou horário.</Text>
        ) : null}
        {quoting ? (
          <ActivityIndicator color={colors.primary} />
        ) : quote ? (
          <Text style={styles.cardTitle} testID="quote-price">
            Orçamento: {formatCents(quote.priceCents, quote.currency)}
          </Text>
        ) : null}
        <ErrorBanner message={quoteError} />
      </Card>

      <ErrorBanner message={submitError} />
      <Button
        title="Confirmar reserva"
        onPress={() => void confirm()}
        loading={submitting}
        disabled={!petId || !startsInFuture || !quote}
      />
    </ScrollView>
  );
}
