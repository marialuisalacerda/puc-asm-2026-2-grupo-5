import { useCallback, useMemo, useState } from 'react';
import { RefreshControl, SectionList } from 'react-native';

import { useApi } from '../../api/ApiContext';
import { toUserMessage } from '../../api/ApiError';
import type { Booking } from '../../api/types';
import { BookingCard } from '../../components/BookingCard';
import { Button, EmptyState, ErrorBanner, Loading, SectionTitle, styles } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { useNameLookup } from '../../hooks/useNameLookup';
import { useReloadOnFocus } from '../../hooks/useReloadOnFocus';
import { useCaregiverSession } from '../../state/SessionContext';

/** Tela 5: solicitações do cuidador (aceitar/recusar REQUESTED) e histórico. */
export function CaregiverRequestsScreen() {
  const api = useApi();
  const caregiver = useCaregiverSession();
  const loader = useCallback(() => api.listBookings({ caregiverId: caregiver.id }), [api, caregiver.id]);
  const { data, loading, refreshing, error, reload } = useAsync(loader);
  useReloadOnFocus(reload);
  const names = useNameLookup(data, ['pets', 'tutors']);

  const [busy, setBusy] = useState<{ id: string; action: 'accept' | 'reject' } | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const sections = useMemo(() => {
    const all = data ?? [];
    return [
      { title: 'Pendentes', data: all.filter((b) => b.status === 'REQUESTED'), empty: 'Nenhuma solicitação pendente.' },
      { title: 'Histórico', data: all.filter((b) => b.status !== 'REQUESTED'), empty: 'Sem reservas anteriores.' },
    ];
  }, [data]);

  const act = async (booking: Booking, action: 'accept' | 'reject') => {
    setBusy({ id: booking.id, action });
    setActionError(null);
    try {
      if (action === 'accept') await api.acceptBooking(booking.id);
      else await api.rejectBooking(booking.id);
      await reload(true);
    } catch (e) {
      setActionError(toUserMessage(e));
    } finally {
      setBusy(null);
    }
  };

  if (loading && !data) return <Loading />;

  return (
    <SectionList
      style={styles.screen}
      contentContainerStyle={styles.content}
      sections={sections}
      keyExtractor={(b) => b.id}
      stickySectionHeadersEnabled={false}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void reload(true)} />}
      ListHeaderComponent={
        <>
          <ErrorBanner message={error} onRetry={() => void reload()} />
          <ErrorBanner message={actionError} />
        </>
      }
      renderSectionHeader={({ section }) => <SectionTitle>{section.title}</SectionTitle>}
      renderSectionFooter={({ section }) => (section.data.length === 0 ? <EmptyState message={section.empty} /> : null)}
      renderItem={({ item }) => (
        <BookingCard
          booking={item}
          details={[`Pet: ${names.pets[item.petId] ?? '…'}`, `Tutor: ${names.tutors[item.tutorId] ?? '…'}`]}
          actions={
            item.status === 'REQUESTED' ? (
              <>
                <Button
                  title="Aceitar"
                  onPress={() => void act(item, 'accept')}
                  loading={busy?.id === item.id && busy.action === 'accept'}
                  disabled={busy !== null}
                />
                <Button
                  title="Recusar"
                  variant="danger"
                  onPress={() => void act(item, 'reject')}
                  loading={busy?.id === item.id && busy.action === 'reject'}
                  disabled={busy !== null}
                />
              </>
            ) : null
          }
        />
      )}
    />
  );
}
