import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import type { Booking } from '../api/types';
import { SERVICE_TYPE_LABELS, formatCents, formatPeriod } from '../utils/format';
import { Card, StatusBadge, styles } from './ui';

interface BookingCardProps {
  booking: Booking;
  /** Linhas descritivas (ex.: "Pet: Thor", "Cuidador: Carla"). */
  details: string[];
  actions?: ReactNode;
}

export function BookingCard({ booking, details, actions }: BookingCardProps) {
  return (
    <Card>
      <View style={styles.spaceBetween}>
        <Text style={styles.cardTitle}>{SERVICE_TYPE_LABELS[booking.serviceType]}</Text>
        <StatusBadge status={booking.status} />
      </View>
      {details.map((line) => (
        <Text key={line} style={styles.muted}>
          {line}
        </Text>
      ))}
      <Text>{formatPeriod(booking.scheduledStart, booking.scheduledEnd)}</Text>
      <Text style={styles.label}>{formatCents(booking.priceCents)}</Text>
      {actions ? <View style={styles.row}>{actions}</View> : null}
    </Card>
  );
}
