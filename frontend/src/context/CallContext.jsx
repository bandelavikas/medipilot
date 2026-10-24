import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { useAuth } from './AuthContext.jsx'
import { ICE_SERVERS, WS_URL } from '../utils/rtc.js'

const CallContext = createContext(null)

const RING_TIMEOUT_MS = 30000
const NOTICE_MS = 6000

function describeMediaError(err, kind) {
  const device = kind === 'video' ? 'camera and microphone' : 'microphone'
  if (err && err.name === 'NotAllowedError') {
    return `Permission to use your ${device} was denied. Please allow access in the browser and try again.`
  }
  if (err && err.name === 'NotFoundError') {
    return `No ${device} was found on this device.`
  }
  if (err && err.name === 'NotReadableError') {
    return `Your ${device} is being used by another application.`
  }
  return `Could not access your ${device}.`
}

export function CallProvider({ children }) {
  const { isLoggedIn, user, authRequest } = useAuth()
  const canCall = Boolean(isLoggedIn && user && (user.role === 'DOCTOR' || user.role === 'PATIENT'))

  const [status, setStatus] = useState('idle')
  const [peer, setPeer] = useState(null)
  const [media, setMedia] = useState('audio')
  const [muted, setMuted] = useState(false)
  const [cameraOff, setCameraOff] = useState(false)
  const [localStream, setLocalStream] = useState(null)
  const [remoteStream, setRemoteStream] = useState(null)
  const [connectedAt, setConnectedAt] = useState(null)
  const [notice, setNotice] = useState('')
  const [socketReady, setSocketReady] = useState(false)

  const socketRef = useRef(null)
  const pcRef = useRef(null)
  const localStreamRef = useRef(null)
  const callIdRef = useRef(null)
  const peerRef = useRef(null)
  const mediaRef = useRef('audio')
  const pendingIceRef = useRef([])
  const ringTimerRef = useRef(null)
  const noticeTimerRef = useRef(null)
  const statusRef = useRef('idle')
  const handlerRef = useRef(null)

  const updateStatus = useCallback((next) => {
    statusRef.current = next
    setStatus(next)
  }, [])

  const showNotice = useCallback((text) => {
    setNotice(text)
    clearTimeout(noticeTimerRef.current)
    noticeTimerRef.current = setTimeout(() => setNotice(''), NOTICE_MS)
  }, [])

  const dismissNotice = useCallback(() => {
    clearTimeout(noticeTimerRef.current)
    setNotice('')
  }, [])

  const send = useCallback((payload) => {
    const socket = socketRef.current
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(payload))
      return true
    }
    return false
  }, [])

  // Stops the camera and microphone, closes the connection and resets the screen.
  const cleanup = useCallback(() => {
    clearTimeout(ringTimerRef.current)

    if (pcRef.current) {
      pcRef.current.onicecandidate = null
      pcRef.current.ontrack = null
      pcRef.current.onconnectionstatechange = null
      pcRef.current.close()
      pcRef.current = null
    }
    if (localStreamRef.current) {
      localStreamRef.current.getTracks().forEach((track) => track.stop())
      localStreamRef.current = null
    }

    pendingIceRef.current = []
    callIdRef.current = null
    peerRef.current = null
    statusRef.current = 'idle'

    setStatus('idle')
    setPeer(null)
    setLocalStream(null)
    setRemoteStream(null)
    setMuted(false)
    setCameraOff(false)
    setConnectedAt(null)
  }, [])

  const finishLocally = useCallback(
    (message) => {
      cleanup()
      if (message) {
        showNotice(message)
      }
    },
    [cleanup, showNotice],
  )

  const hangUp = useCallback(
    (message) => {
      if (callIdRef.current) {
        send({ type: 'call-end', callId: callIdRef.current })
      }
      finishLocally(message)
    },
    [send, finishLocally],
  )

  const createPeerConnection = useCallback(
    (stream) => {
      const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS })
      stream.getTracks().forEach((track) => pc.addTrack(track, stream))

      pc.onicecandidate = (event) => {
        if (event.candidate && callIdRef.current) {
          send({ type: 'ice', callId: callIdRef.current, data: event.candidate.toJSON() })
        }
      }

      pc.ontrack = (event) => {
        setRemoteStream(event.streams[0])
      }

      pc.onconnectionstatechange = () => {
        const state = pc.connectionState
        if (state === 'connected') {
          updateStatus('in-call')
          setConnectedAt((previous) => previous || Date.now())
        } else if (state === 'failed') {
          hangUp('The connection failed. Please try again.')
        }
      }

      pcRef.current = pc
      return pc
    },
    [send, updateStatus, hangUp],
  )

  const flushPendingIce = useCallback(async (pc) => {
    const queued = pendingIceRef.current
    pendingIceRef.current = []
    for (const candidate of queued) {
      try {
        await pc.addIceCandidate(candidate)
      } catch {
        // A stray candidate can be ignored.
      }
    }
  }, [])

  const addRemoteIce = useCallback(async (candidate) => {
    const pc = pcRef.current
    if (!pc || !pc.remoteDescription) {
      pendingIceRef.current.push(candidate)
      return
    }
    try {
      await pc.addIceCandidate(candidate)
    } catch {
      // A stray candidate can be ignored.
    }
  }, [])

  // Caller side: the other person accepted, so create and send the offer.
  async function startOffer() {
    try {
      updateStatus('connecting')
      const pc = createPeerConnection(localStreamRef.current)
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      send({ type: 'offer', callId: callIdRef.current, data: { type: offer.type, sdp: offer.sdp } })
    } catch {
      hangUp('Could not start the call. Please try again.')
    }
  }

  // Receiver side: an offer arrived, so answer it.
  async function handleOffer(data) {
    try {
      const pc = pcRef.current
      await pc.setRemoteDescription(data)
      await flushPendingIce(pc)
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      send({ type: 'answer', callId: callIdRef.current, data: { type: answer.type, sdp: answer.sdp } })
    } catch {
      hangUp('Could not connect the call. Please try again.')
    }
  }

  // Caller side: the answer arrived.
  async function handleAnswer(data) {
    try {
      const pc = pcRef.current
      await pc.setRemoteDescription(data)
      await flushPendingIce(pc)
    } catch {
      hangUp('Could not connect the call. Please try again.')
    }
  }

  async function handleMessage(message) {
    const name = peerRef.current ? peerRef.current.name : 'The other person'

    switch (message.type) {
      case 'incoming-call':
        if (statusRef.current !== 'idle') {
          send({ type: 'call-reject', callId: message.callId })
          return
        }
        callIdRef.current = message.callId
        peerRef.current = { userId: message.from, name: message.fromName }
        mediaRef.current = message.media
        setPeer(peerRef.current)
        setMedia(message.media)
        updateStatus('incoming')
        break
      case 'call-ringing':
        callIdRef.current = message.callId
        break
      case 'call-accepted':
        clearTimeout(ringTimerRef.current)
        await startOffer()
        break
      case 'offer':
        await handleOffer(message.data)
        break
      case 'answer':
        await handleAnswer(message.data)
        break
      case 'ice':
        await addRemoteIce(message.data)
        break
      case 'call-rejected':
        finishLocally(`${name} declined the call.`)
        break
      case 'call-unavailable':
        finishLocally(`${name} is not online right now.`)
        break
      case 'call-busy':
        finishLocally(`${name} is on another call. Please try again later.`)
        break
      case 'call-ended':
        finishLocally(
          message.reason === 'cancelled'
            ? 'The call was cancelled.'
            : message.reason === 'disconnected'
              ? 'The other person lost connection.'
              : 'The call ended.',
        )
        break
      case 'error':
        if (statusRef.current === 'calling') {
          finishLocally(message.message)
        } else {
          showNotice(message.message)
        }
        break
      default:
        break
    }
  }

  // Always point the socket at the newest version of handleMessage.
  useEffect(() => {
    handlerRef.current = handleMessage
  })

  // Connect to the call service while a doctor or patient is signed in.
  useEffect(() => {
    if (!canCall) {
      return undefined
    }

    let cancelled = false
    let reconnectTimer = null

    async function connect() {
      try {
        const data = await authRequest('/api/calls/ticket', { method: 'POST' })
        if (cancelled) {
          return
        }

        const socket = new WebSocket(`${WS_URL}?ticket=${encodeURIComponent(data.ticket)}`)
        socketRef.current = socket

        socket.onopen = () => setSocketReady(true)

        socket.onmessage = (event) => {
          try {
            handlerRef.current(JSON.parse(event.data))
          } catch {
            // Ignore messages that cannot be read.
          }
        }

        socket.onclose = (event) => {
          setSocketReady(false)
          if (socketRef.current === socket) {
            socketRef.current = null
          }
          if (statusRef.current !== 'idle') {
            finishLocally('Connection to the call service was lost.')
          }
          if (event.code === 4000) {
            showNotice('MediPilot was opened in another tab. Calls will work in that tab.')
          } else if (!cancelled) {
            reconnectTimer = setTimeout(connect, 3000)
          }
        }
      } catch {
        if (!cancelled) {
          reconnectTimer = setTimeout(connect, 5000)
        }
      }
    }

    connect()

    return () => {
      cancelled = true
      clearTimeout(reconnectTimer)
      const socket = socketRef.current
      if (socket) {
        socket.onclose = null
        socket.close()
      }
      socketRef.current = null
      setSocketReady(false)
      cleanup()
    }
  }, [canCall, authRequest, cleanup, finishLocally, showNotice])

  async function getLocalMedia(kind) {
    return navigator.mediaDevices.getUserMedia({ audio: true, video: kind === 'video' })
  }

  async function startCall(peerUserId, peerName, kind) {
    if (statusRef.current !== 'idle') {
      return
    }
    try {
      const stream = await getLocalMedia(kind)
      localStreamRef.current = stream
      setLocalStream(stream)

      peerRef.current = { userId: peerUserId, name: peerName }
      mediaRef.current = kind
      setPeer(peerRef.current)
      setMedia(kind)
      updateStatus('calling')

      const sent = send({ type: 'call-request', to: peerUserId, media: kind })
      if (!sent) {
        finishLocally('You are not connected to the call service yet. Please wait a moment and try again.')
        return
      }

      ringTimerRef.current = setTimeout(() => {
        hangUp(`${peerName} did not answer.`)
      }, RING_TIMEOUT_MS)
    } catch (err) {
      finishLocally(describeMediaError(err, kind))
    }
  }

  async function acceptCall() {
    if (statusRef.current !== 'incoming') {
      return
    }
    const kind = mediaRef.current
    try {
      const stream = await getLocalMedia(kind)
      localStreamRef.current = stream
      setLocalStream(stream)
      createPeerConnection(stream)
      updateStatus('connecting')
      send({ type: 'call-accept', callId: callIdRef.current })
    } catch (err) {
      send({ type: 'call-reject', callId: callIdRef.current })
      finishLocally(describeMediaError(err, kind))
    }
  }

  function rejectCall() {
    send({ type: 'call-reject', callId: callIdRef.current })
    finishLocally('')
  }

  function endCall() {
    hangUp('')
  }

  function toggleMute() {
    const stream = localStreamRef.current
    if (!stream) {
      return
    }
    const next = !muted
    stream.getAudioTracks().forEach((track) => {
      track.enabled = !next
    })
    setMuted(next)
  }

  function toggleCamera() {
    const stream = localStreamRef.current
    if (!stream) {
      return
    }
    const next = !cameraOff
    stream.getVideoTracks().forEach((track) => {
      track.enabled = !next
    })
    setCameraOff(next)
  }

  const value = {
    status,
    peer,
    media,
    muted,
    cameraOff,
    localStream,
    remoteStream,
    connectedAt,
    notice,
    canCall,
    socketReady,
    startCall,
    acceptCall,
    rejectCall,
    endCall,
    toggleMute,
    toggleCamera,
    dismissNotice,
  }

  return <CallContext.Provider value={value}>{children}</CallContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useCall() {
  const context = useContext(CallContext)
  if (!context) {
    throw new Error('useCall must be used inside CallProvider')
  }
  return context
}