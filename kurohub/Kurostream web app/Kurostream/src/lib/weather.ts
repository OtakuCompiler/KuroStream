export interface WeatherData {
  temp: number
  condition: string
  icon: string
  location: string
  humidity: number
  wind: number
}

export async function getWeather(lat?: number, lon?: number): Promise<WeatherData | null> {
  try {
    // Using Open-Meteo (no API key required)
    const url = lat && lon
      ? `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current_weather=true`
      : `https://api.open-meteo.com/v1/forecast?latitude=40.7&longitude=-74.0&current_weather=true`

    const res = await fetch(url)
    if (!res.ok) return null
    const data = await res.json()

    const wmoCodes: Record<number, string> = {
      0: 'Clear', 1: 'Mainly Clear', 2: 'Partly Cloudy', 3: 'Overcast',
      45: 'Fog', 48: 'Fog', 51: 'Drizzle', 53: 'Drizzle', 55: 'Drizzle',
      61: 'Rain', 63: 'Rain', 65: 'Rain', 71: 'Snow', 73: 'Snow', 75: 'Snow',
      95: 'Thunderstorm', 96: 'Thunderstorm', 99: 'Thunderstorm',
    }

    return {
      temp: data.current_weather?.temperature || 0,
      condition: wmoCodes[data.current_weather?.weathercode] || 'Unknown',
      icon: getWeatherIcon(data.current_weather?.weathercode),
      location: 'Local',
      humidity: 0,
      wind: data.current_weather?.windspeed || 0,
    }
  } catch { return null }
}

function getWeatherIcon(code: number): string {
  if (code === 0) return 'sun'
  if (code <= 3) return 'cloud'
  if (code <= 48) return 'fog'
  if (code <= 67) return 'rain'
  if (code <= 77) return 'snow'
  return 'storm'
}
