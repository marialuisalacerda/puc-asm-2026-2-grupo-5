import type { BottomTabScreenProps } from '@react-navigation/bottom-tabs';
import type { CompositeScreenProps, NavigatorScreenParams } from '@react-navigation/native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';

export type TutorTabParamList = {
  Pets: undefined;
  Caregivers: undefined;
  TutorBookings: undefined;
};

export type RootStackParamList = {
  Entry: undefined;
  TutorHome: NavigatorScreenParams<TutorTabParamList> | undefined;
  NewBooking: { caregiverId: string; caregiverName: string };
  CaregiverRequests: undefined;
};

export type RootScreenProps<T extends keyof RootStackParamList> = NativeStackScreenProps<RootStackParamList, T>;

export type TutorTabScreenProps<T extends keyof TutorTabParamList> = CompositeScreenProps<
  BottomTabScreenProps<TutorTabParamList, T>,
  NativeStackScreenProps<RootStackParamList>
>;
