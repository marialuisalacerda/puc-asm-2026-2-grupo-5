import { Alert, Platform } from 'react-native';

/** Pede confirmação ao usuário (Alert no Android/iOS, window.confirm na web). */
export function confirmAction(title: string, message: string, confirmLabel = 'Confirmar'): Promise<boolean> {
  if (Platform.OS === 'web') {
    const confirmFn = (globalThis as { confirm?: (msg: string) => boolean }).confirm;
    return Promise.resolve(confirmFn ? confirmFn(`${title}\n\n${message}`) : true);
  }
  return new Promise((resolve) => {
    Alert.alert(title, message, [
      { text: 'Voltar', style: 'cancel', onPress: () => resolve(false) },
      { text: confirmLabel, style: 'destructive', onPress: () => resolve(true) },
    ]);
  });
}
