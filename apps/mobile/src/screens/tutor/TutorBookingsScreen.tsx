import { useCallback, useState } from 'react';
import { FlatList, RefreshControl } from 'react-native';

import { useApi } from '../../api/ApiContext';
import { toUserMessage } from '../../api/ApiError';
import type { Booking } from '../../api/types';
import { BookingCard } from '../../components/BookingCard';
import { Button, EmptyState, ErrorBanner, Loading, styles } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { useNameLookup } from '../../hooks/useNameLookup';
import { useReloadOnFocus } from '../../hooks/useReloadOnFocus';
import { useTutorSession } from '../../state/SessionContext';
import { confirmAction } from '../../utils/confirm';

const CANCELLABLE: Booking['status'][] = ['REQUESTED', 'ACCEPTED'];

/** Tela 4: reservas do tutor, com cancelamento e "puxar para atualizar". */
export function TutorBookingsScreen() {
  const api = useApi();
  const tutor = useTutorSession();
  const loader = useCallback(() => api.listBookings({ tutorId: tutor.id }), [api, tutor.id]);
  const { data, loading, refreshing, error, reload } = useAsync(loader);
  useReloadOnFocus(reload);
  const names = useNameLookup(data, ['pets', 'caregivers']);

  const [busyId, setBusyId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const cancel = async (booking: Booking) => {
    const ok = await confirmAction('Cancelar reserva', 'Deseja mesmo cancelar esta reserva?', 'Cancelar reserva');
    if (!ok) return;
    setBusyId(booking.id);
    setActionError(null);
    try {
      await api.cancelBooking(booking.id);
      await reload(true);
    } catch (e) {
      setActionError(toUserMessage(e));
    } finally {
      setBusyId(null);
    }
  };

  if (loading && !data) return <Loading />;

  return (
    <FlatList
      style={styles.screen}
      contentContainerStyle={styles.content}
      data={data ?? []}
      keyExtractor={(b) => b.id}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void reload(true)} />}
      ListHeaderComponent={
        <>
          <ErrorBanner message={error} onRetry={() => void reload()} />
          <ErrorBanner message={actionError} />
        </>
      }
      ListEmptyComponent={
        error ? undefined : <EmptyState message="Nenhuma reserva ainda. Escolha um cuidador na aba Cuidadores." />
      }
      renderItem={({ item }) => (
        <BookingCard
          booking={item}
          details={[`Pet: ${names.pets[item.petId] ?? '…'}`, `Cuidador: ${names.caregivers[item.caregiverId] ?? '…'}`]}
          actions={
            CANCELLABLE.includes(item.status) ? (
              <Button
                title="Cancelar"
                variant="danger"
                onPress={() => void cancel(item)}
                loading={busyId === item.id}
                disabled={busyId !== null}
              />
            ) : null
          }
        />
      )}
    />
  );
}
