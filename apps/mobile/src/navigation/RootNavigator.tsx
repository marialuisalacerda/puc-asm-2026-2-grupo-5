import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Pressable, Text } from 'react-native';

import { colors } from '../components/ui';
import { EntryScreen } from '../screens/EntryScreen';
import { CaregiverRequestsScreen } from '../screens/caregiver/CaregiverRequestsScreen';
import { CaregiversScreen } from '../screens/tutor/CaregiversScreen';
import { NewBookingScreen } from '../screens/tutor/NewBookingScreen';
import { PetsScreen } from '../screens/tutor/PetsScreen';
import { TutorBookingsScreen } from '../screens/tutor/TutorBookingsScreen';
import { useSession } from '../state/SessionContext';
import type { RootStackParamList, TutorTabParamList } from './types';

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tabs = createBottomTabNavigator<TutorTabParamList>();

function SignOutButton() {
  const { signOut } = useSession();
  return (
    <Pressable accessibilityRole="button" onPress={signOut} hitSlop={12} style={{ paddingHorizontal: 12 }}>
      <Text style={{ color: colors.primary, fontWeight: '600' }}>Sair</Text>
    </Pressable>
  );
}

const tabIcon = (glyph: string) =>
  function TabIcon({ color }: { color: string }) {
    return <Text style={{ color, fontSize: 18 }}>{glyph}</Text>;
  };

function TutorTabs() {
  const { session } = useSession();
  const firstName = session?.role === 'TUTOR' ? session.tutor.name.split(' ')[0] : '';
  return (
    <Tabs.Navigator
      screenOptions={{
        headerRight: () => <SignOutButton />,
        tabBarActiveTintColor: colors.primary,
      }}
    >
      <Tabs.Screen
        name="Pets"
        component={PetsScreen}
        options={{ title: 'Meus pets', headerTitle: `Olá, ${firstName}`, tabBarIcon: tabIcon('🐕') }}
      />
      <Tabs.Screen
        name="Caregivers"
        component={CaregiversScreen}
        options={{ title: 'Cuidadores', tabBarIcon: tabIcon('🤝') }}
      />
      <Tabs.Screen
        name="TutorBookings"
        component={TutorBookingsScreen}
        options={{ title: 'Minhas reservas', tabBarIcon: tabIcon('📅') }}
      />
    </Tabs.Navigator>
  );
}

/** A pilha exibida depende do perfil da sessão (login simulado). */
export function RootNavigator() {
  const { session } = useSession();
  return (
    <NavigationContainer>
      <Stack.Navigator>
        {session === null ? (
          <Stack.Screen name="Entry" component={EntryScreen} options={{ title: 'Entrar' }} />
        ) : session.role === 'TUTOR' ? (
          <>
            <Stack.Screen name="TutorHome" component={TutorTabs} options={{ headerShown: false }} />
            <Stack.Screen name="NewBooking" component={NewBookingScreen} options={{ title: 'Nova reserva' }} />
          </>
        ) : (
          <Stack.Screen
            name="CaregiverRequests"
            component={CaregiverRequestsScreen}
            options={{ title: `Solicitações · ${session.caregiver.name}`, headerRight: () => <SignOutButton /> }}
          />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
