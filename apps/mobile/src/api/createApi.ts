import type { AppConfig } from '../config/env';
import { HttpPlantaoPetApi } from './HttpPlantaoPetApi';
import { MockPlantaoPetApi } from './mock/MockPlantaoPetApi';
import type { PlantaoPetApi } from './types';

/** Fábrica: escolhe a implementação conforme EXPO_PUBLIC_USE_MOCKS. */
export function createApi(config: AppConfig): PlantaoPetApi {
  return config.useMocks ? new MockPlantaoPetApi() : new HttpPlantaoPetApi({ baseUrl: config.apiUrl });
}
