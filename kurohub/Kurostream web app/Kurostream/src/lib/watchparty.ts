export interface PartyMember {
  id: string
  name: string
  avatar?: string
  ready: boolean
  currentTime: number
  isPlaying: boolean
}

export interface WatchParty {
  id: string
  hostId: string
  members: PartyMember[]
  titleId: string
  episodeId?: string
  createdAt: number
}

let currentParty: WatchParty | null = null
let partyCallbacks: Set<(party: WatchParty) => void> = new Set()

export function createParty(titleId: string, episodeId?: string, hostName: string = 'Host'): WatchParty {
  const party: WatchParty = {
    id: Math.random().toString(36).substring(2, 10).toUpperCase(),
    hostId: 'host',
    members: [{ id: 'host', name: hostName, ready: true, currentTime: 0, isPlaying: false }],
    titleId,
    episodeId,
    createdAt: Date.now(),
  }
  currentParty = party
  notifyListeners()
  return party
}

export function joinParty(partyId: string, memberName: string): WatchParty | null {
  if (!currentParty || currentParty.id !== partyId) return null
  currentParty.members.push({
    id: Math.random().toString(36).substring(2),
    name: memberName,
    ready: false,
    currentTime: 0,
    isPlaying: false,
  })
  notifyListeners()
  return currentParty
}

export function updatePartyState(memberId: string, state: Partial<PartyMember>): void {
  if (!currentParty) return
  const member = currentParty.members.find(m => m.id === memberId)
  if (member) {
    Object.assign(member, state)
    notifyListeners()
  }
}

export function syncParty(time: number, isPlaying: boolean): void {
  if (!currentParty) return
  currentParty.members.forEach(m => {
    m.currentTime = time
    m.isPlaying = isPlaying
  })
  notifyListeners()
}

export function leaveParty(memberId: string): void {
  if (!currentParty) return
  currentParty.members = currentParty.members.filter(m => m.id !== memberId)
  if (currentParty.members.length === 0) currentParty = null
  notifyListeners()
}

export function getCurrentParty(): WatchParty | null {
  return currentParty
}

export function subscribeToParty(callback: (party: WatchParty) => void): () => void {
  partyCallbacks.add(callback)
  return () => partyCallbacks.delete(callback)
}

function notifyListeners() {
  if (currentParty) partyCallbacks.forEach(cb => cb(currentParty!))
}
