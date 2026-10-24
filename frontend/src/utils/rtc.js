const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// The call service address, for example ws://localhost:8080/ws/calls
export const WS_URL = `${API_URL.replace(/^http/, 'ws')}/ws/calls`

// STUN lets each browser discover its own public address (free, public server).
const servers = [{ urls: 'stun:stun.l.google.com:19302' }]

// Optional TURN relay for strict networks. Put these three values in a file named
// .env.local (it is ignored by Git) only if you set up a TURN server later.
if (import.meta.env.VITE_TURN_URL) {
  servers.push({
    urls: import.meta.env.VITE_TURN_URL,
    username: import.meta.env.VITE_TURN_USERNAME,
    credential: import.meta.env.VITE_TURN_CREDENTIAL,
  })
}

export const ICE_SERVERS = servers