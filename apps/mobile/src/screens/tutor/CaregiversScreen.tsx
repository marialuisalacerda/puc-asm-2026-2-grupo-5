import { useCallback } from 'react';
import { FlatList, RefreshControl, Text, View } from 'react-native';

import { useApi } from '../../api/ApiContext';
import { Button, Card, EmptyState, ErrorBanner, Loading, styles } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import type { TutorTabScreenProps } from '../../navigation/types';

/** Tela 3a: cuidadores ativos. */
export function CaregiversScreen({ navigation }: TutorTabScreenProps<'Caregivers'>) {
  const api = useApi();
  const loader = useCallback(() => api.listCaregivers({ active: true }), [api]);
  const { data, loading, refreshing, error, reload } = useAsync(loader);

  if (loading && !data) return <Loading />;

  return (
    <FlatList
      style={styles.screen}
      contentContainerStyle={styles.content}
      data={data ?? []}
      keyExtractor={(c) => c.id}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void reload(true)} />}
      ListHeaderComponent={<ErrorBanner message={error} onRetry={() => void reload()} />}
      ListEmptyComponent={error ? undefined : <EmptyState message="Nenhum cuidador disponível no momento." />}
      renderItem={({ item }) => (
        <Card>
          <View style={styles.spaceBetween}>
            <View style={{ flex: 1 }}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              <Text style={styles.muted}>{item.email}</Text>
            </View>
            <Button
              title="Reservar"
              onPress={() => navigation.navigate('NewBooking', { caregiverId: item.id, caregiverName: item.name })}
            />
          </View>
        </Card>
      )}
    />
  );
}
