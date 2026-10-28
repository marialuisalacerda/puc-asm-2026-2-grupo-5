import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { ApiProvider } from './src/api/ApiContext';
import { RootNavigator } from './src/navigation/RootNavigator';
import { SessionProvider } from './src/state/SessionContext';

export default function App() {
  return (
    <SafeAreaProvider>
      <ApiProvider>
        <SessionProvider>
          <RootNavigator />
          <StatusBar style="auto" />
        </SessionProvider>
      </ApiProvider>
    </SafeAreaProvider>
  );
}
