import { useCallback, useState } from 'react';
import { RefreshControl, ScrollView, Text, TextInput, View } from 'react-native';

import { useApi } from '../../api/ApiContext';
import { toUserMessage } from '../../api/ApiError';
import { PET_SIZES, type PetSize } from '../../api/types';
import { Button, Card, Chip, ChipGroup, EmptyState, ErrorBanner, Loading, SectionTitle, styles } from '../../components/ui';
import { useAsync } from '../../hooks/useAsync';
import { useTutorSession } from '../../state/SessionContext';
import { PET_SIZE_LABELS } from '../../utils/format';

/** Tela 2: pets do tutor + cadastro (species fixa DOG, conforme escopo atual). */
export function PetsScreen() {
  const api = useApi();
  const tutor = useTutorSession();

  const loader = useCallback(() => api.listTutorPets(tutor.id), [api, tutor.id]);
  const { data: pets, loading, refreshing, error, reload } = useAsync(loader);

  const [name, setName] = useState('');
  const [size, setSize] = useState<PetSize>('MEDIUM');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const submit = async () => {
    setSuccess(null);
    if (!name.trim()) {
      setFormError('Informe o nome do pet.');
      return;
    }
    setSaving(true);
    setFormError(null);
    try {
      const pet = await api.createPet({
        tutorId: tutor.id,
        name: name.trim(),
        species: 'DOG',
        size,
        notes: notes.trim() ? notes.trim() : null,
      });
      setName('');
      setNotes('');
      setSuccess(`${pet.name} cadastrado com sucesso.`);
      await reload(true);
    } catch (e) {
      setFormError(toUserMessage(e));
    } finally {
      setSaving(false);
    }
  };

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      keyboardShouldPersistTaps="handled"
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void reload(true)} />}
    >
      <SectionTitle>Meus pets</SectionTitle>
      <ErrorBanner message={error} onRetry={() => void reload()} />
      {loading ? <Loading /> : null}
      {!loading && pets?.length === 0 ? <EmptyState message="Você ainda não cadastrou nenhum pet." /> : null}
      {pets?.map((pet) => (
        <Card key={pet.id}>
          <Text style={styles.cardTitle}>{pet.name}</Text>
          <Text style={styles.muted}>
            Cão{pet.size ? ` · porte ${PET_SIZE_LABELS[pet.size].toLowerCase()}` : ''}
          </Text>
          {pet.notes ? <Text>{pet.notes}</Text> : null}
        </Card>
      ))}

      <SectionTitle>Cadastrar pet</SectionTitle>
      <Card>
        <Text style={styles.label}>Nome</Text>
        <TextInput
          style={styles.input}
          value={name}
          onChangeText={setName}
          placeholder="Ex.: Thor"
          maxLength={80}
          accessibilityLabel="Nome do pet"
        />
        <Text style={styles.label}>Porte</Text>
        <ChipGroup>
          {PET_SIZES.map((s) => (
            <Chip key={s} label={PET_SIZE_LABELS[s]} selected={size === s} onPress={() => setSize(s)} />
          ))}
        </ChipGroup>
        <Text style={styles.label}>Observações</Text>
        <TextInput
          style={[styles.input, { minHeight: 70, textAlignVertical: 'top' }]}
          value={notes}
          onChangeText={setNotes}
          placeholder="Alergias, medicação, comportamento…"
          multiline
          maxLength={500}
          accessibilityLabel="Observações"
        />
        <ErrorBanner message={formError} />
        {success ? <Text style={{ color: '#1A7F37' }}>{success}</Text> : null}
        <View>
          <Button title="Cadastrar pet" onPress={() => void submit()} loading={saving} />
        </View>
      </Card>
    </ScrollView>
  );
}
