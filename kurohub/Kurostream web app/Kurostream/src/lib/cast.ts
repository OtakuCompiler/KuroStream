declare global {
  interface Window {
    __arctic_cast_api?: any
    chrome?: {
      cast?: {
        isAvailable?: boolean
      }
    }
  }
}

export interface CastDevice {
  id: string
  name: string
  type: 'chromecast' | 'airplay' | 'dlna'
  connected: boolean
}

let castDevices: CastDevice[] = []
let activeCastDevice: CastDevice | null = null

export function discoverCastDevices(): CastDevice[] {
  // In a real implementation, this would use the Cast SDK
  // For now, return mock devices if Chromecast API is available
  if (window.chrome?.cast?.isAvailable) {
    return [
      { id: 'cc1', name: 'Living Room TV', type: 'chromecast', connected: false },
    ]
  }
  return []
}

export async function connectCastDevice(device: CastDevice): Promise<boolean> {
  activeCastDevice = device
  return true
}

export async function castMedia(url: string, title: string, position: number = 0): Promise<void> {
  if (!activeCastDevice) throw new Error('No cast device connected')
  // Would use Chrome Cast SDK here
  console.log('[Arctic] Casting to', activeCastDevice.name, ':', title)
}

export function disconnectCast(): void {
  activeCastDevice = null
}

export function getActiveCastDevice(): CastDevice | null {
  return activeCastDevice
}
