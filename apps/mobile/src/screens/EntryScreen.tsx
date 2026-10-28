import { useCallback } from 'react';
import { ScrollView, Text, View } from 'react-native';

import { useApi, useApiContext } from '../api/ApiContext';
import { Button, Card, EmptyState, ErrorBanner, Loading, SectionTitle, styles } from '../components/ui';
import { useAsync } from '../hooks/useAsync';
import { useSession } from '../state/SessionContext';

/** Tela 1: escolha de perfil (login simulado, sem senha). */
export function EntryScreen() {
  const api = useApi();
  const { config } = useApiContext();
  const { signInAsTutor, signInAsCaregiver } = useSession();

  const loader = useCallback(
    () => Promise.all([api.listTutors(), api.listCaregivers({ active: true })]),
    [api],
  );
  const { data, loading, error, reload } = useAsync(loader);
  const [tutors, caregivers] = data ?? [[], []];

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Plantão de Pet</Text>
      <Text style={styles.muted}>Escolha um perfil para entrar. Este acesso é simulado e não pede senha.</Text>
      <Text style={styles.muted} testID="api-mode">
        {config.useMocks ? 'Modo demonstração: dados simulados no aparelho.' : `Conectado à API: ${config.apiUrl}`}
      </Text>

      <ErrorBanner message={error} onRetry={() => void reload()} />
      {loading ? (
        <Loading />
      ) : (
        <>
          <SectionTitle>Sou tutor</SectionTitle>
          {tutors.length === 0 ? <EmptyState message="Nenhum tutor cadastrado." /> : null}
          {tutors.map((tutor) => (
            <Card key={tutor.id}>
              <View style={styles.spaceBetween}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.cardTitle}>{tutor.name}</Text>
                  <Text style={styles.muted}>{tutor.email}</Text>
                </View>
                <Button title="Entrar" onPress={() => signInAsTutor(tutor)} testID={`enter-tutor-${tutor.id}`} />
              </View>
            </Card>
          ))}

          <SectionTitle>Sou cuidador</SectionTitle>
          {caregivers.length === 0 ? <EmptyState message="Nenhum cuidador ativo." /> : null}
          {caregivers.map((caregiver) => (
            <Card key={caregiver.id}>
              <View style={styles.spaceBetween}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.cardTitle}>{caregiver.name}</Text>
                  <Text style={styles.muted}>{caregiver.email}</Text>
                </View>
                <Button
                  title="Entrar"
                  variant="secondary"
                  onPress={() => signInAsCaregiver(caregiver)}
                  testID={`enter-caregiver-${caregiver.id}`}
                />
              </View>
            </Card>
          ))}
        </>
      )}
    </ScrollView>
  );
}
