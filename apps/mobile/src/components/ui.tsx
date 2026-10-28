import type { ReactNode } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View, type ViewStyle } from 'react-native';

import type { BookingStatus } from '../api/types';
import { BOOKING_STATUS_COLORS, BOOKING_STATUS_LABELS } from '../utils/format';

export const colors = {
  primary: '#2F6F4E',
  primaryText: '#FFFFFF',
  danger: '#CF222E',
  background: '#F6F8F7',
  card: '#FFFFFF',
  border: '#D0D7DE',
  text: '#1F2328',
  muted: '#57606A',
  errorBg: '#FFEBE9',
};

type ButtonVariant = 'primary' | 'secondary' | 'danger';

interface ButtonProps {
  title: string;
  onPress(): void;
  variant?: ButtonVariant;
  disabled?: boolean;
  loading?: boolean;
  style?: ViewStyle;
  testID?: string;
}

export function Button({ title, onPress, variant = 'primary', disabled, loading, style, testID }: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!isDisabled, busy: !!loading }}
      onPress={onPress}
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.button,
        variant === 'primary' && styles.buttonPrimary,
        variant === 'secondary' && styles.buttonSecondary,
        variant === 'danger' && styles.buttonDanger,
        (pressed || isDisabled) && styles.buttonDim,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator color={variant === 'secondary' ? colors.primary : colors.primaryText} />
      ) : (
        <Text style={[styles.buttonText, variant === 'secondary' && styles.buttonTextSecondary]}>{title}</Text>
      )}
    </Pressable>
  );
}

interface ChipProps {
  label: string;
  selected: boolean;
  onPress(): void;
  disabled?: boolean;
}

/** Opção selecionável (usada como seletor simples, sem componentes nativos). */
export function Chip({ label, selected, onPress, disabled }: ChipProps) {
  return (
    <Pressable
      accessibilityRole="radio"
      accessibilityState={{ selected, disabled: !!disabled }}
      onPress={onPress}
      disabled={disabled}
      style={[styles.chip, selected && styles.chipSelected, disabled && styles.buttonDim]}
    >
      <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{label}</Text>
    </Pressable>
  );
}

export function ChipGroup({ children }: { children: ReactNode }) {
  return <View style={styles.chipGroup}>{children}</View>;
}

/** Status com cor e texto sempre visível (não depende só da cor). */
export function StatusBadge({ status }: { status: BookingStatus }) {
  return (
    <View
      style={[styles.badge, { backgroundColor: BOOKING_STATUS_COLORS[status] }]}
      accessibilityLabel={`Status: ${BOOKING_STATUS_LABELS[status]}`}
    >
      <Text style={styles.badgeText}>{BOOKING_STATUS_LABELS[status]}</Text>
    </View>
  );
}

export function ErrorBanner({ message, onRetry }: { message: string | null; onRetry?(): void }) {
  if (!message) return null;
  return (
    <View style={styles.error} accessibilityRole="alert">
      <Text style={styles.errorText}>{message}</Text>
      {onRetry ? <Button title="Tentar novamente" variant="secondary" onPress={onRetry} style={styles.retry} /> : null}
    </View>
  );
}

export function Card({ children, style }: { children: ReactNode; style?: ViewStyle }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function SectionTitle({ children }: { children: ReactNode }) {
  return <Text style={styles.sectionTitle}>{children}</Text>;
}

export function EmptyState({ message }: { message: string }) {
  return <Text style={styles.empty}>{message}</Text>;
}

export function Loading() {
  return (
    <View style={styles.loading}>
      <ActivityIndicator size="large" color={colors.primary} />
    </View>
  );
}

export const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { padding: 16, gap: 12 },
  button: {
    minHeight: 44,
    paddingHorizontal: 16,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPrimary: { backgroundColor: colors.primary },
  buttonSecondary: { backgroundColor: colors.card, borderWidth: 1, borderColor: colors.primary },
  buttonDanger: { backgroundColor: colors.danger },
  buttonDim: { opacity: 0.6 },
  buttonText: { color: colors.primaryText, fontWeight: '600', fontSize: 15 },
  buttonTextSecondary: { color: colors.primary },
  chipGroup: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.card,
  },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { color: colors.text },
  chipTextSelected: { color: colors.primaryText, fontWeight: '600' },
  badge: { alignSelf: 'flex-start', paddingHorizontal: 8, paddingVertical: 3, borderRadius: 10 },
  badgeText: { color: '#FFFFFF', fontSize: 12, fontWeight: '700' },
  error: { backgroundColor: colors.errorBg, borderRadius: 8, padding: 12, gap: 8 },
  errorText: { color: colors.danger },
  retry: { alignSelf: 'flex-start' },
  card: {
    backgroundColor: colors.card,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: colors.border,
    padding: 14,
    gap: 6,
  },
  cardTitle: { fontSize: 16, fontWeight: '600', color: colors.text },
  muted: { color: colors.muted },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: colors.text, marginTop: 8 },
  empty: { color: colors.muted, textAlign: 'center', marginTop: 24 },
  loading: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  input: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 8,
    backgroundColor: colors.card,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 15,
    color: colors.text,
  },
  label: { fontWeight: '600', color: colors.text },
  row: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  spaceBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  title: { fontSize: 22, fontWeight: '700', color: colors.text },
});
