import { fireEvent, render, screen } from '@testing-library/react-native';
import { Text } from 'react-native';

import { ApiProvider } from '../../api/ApiContext';
import { MockPlantaoPetApi } from '../../api/mock/MockPlantaoPetApi';
import { SEED_IDS } from '../../api/mock/seeds';
import { SessionProvider, useSession } from '../../state/SessionContext';
import { EntryScreen } from '../EntryScreen';

function SessionProbe() {
  const { session } = useSession();
  return session ? <Text>sessão: {session.role}</Text> : null;
}

describe('EntryScreen', () => {
  it('lista os perfis do seed e entra como tutor', async () => {
    await render(
      <ApiProvider api={new MockPlantaoPetApi({ latencyMs: 0 })} config={{ apiUrl: 'http://localhost:8080', useMocks: true }}>
        <SessionProvider>
          <EntryScreen />
          <SessionProbe />
        </SessionProvider>
      </ApiProvider>,
    );

    expect(await screen.findByText('Ana Tutora')).toBeTruthy();
    expect(screen.getByText('Bruno Tutor')).toBeTruthy();
    expect(screen.getByText('Carla Cuidadora')).toBeTruthy();
    expect(screen.queryByText('Fábio Inativo')).toBeNull();
    expect(screen.getByTestId('api-mode')).toHaveTextContent(/Modo demonstração/);

    await fireEvent.press(screen.getByTestId(`enter-tutor-${SEED_IDS.tutors.ana}`));
    expect(await screen.findByText('sessão: TUTOR')).toBeTruthy();
  });
});
