import { useEffect, useRef, useState } from 'react'
import { useCall } from '../context/CallContext.jsx'

function formatElapsed(totalSeconds) {
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

function CallOverlay() {
  const {
    status,
    peer,
    media,
    muted,
    cameraOff,
    localStream,
    remoteStream,
    connectedAt,
    notice,
    acceptCall,
    rejectCall,
    endCall,
    toggleMute,
    toggleCamera,
    dismissNotice,
  } = useCall()

  const remoteRef = useRef(null)
  const localRef = useRef(null)
  const [now, setNow] = useState(() => Date.now())

  const inCall = status === 'connecting' || status === 'in-call'

  useEffect(() => {
    if (remoteRef.current) {
      remoteRef.current.srcObject = remoteStream
    }
  }, [remoteStream, status])

  useEffect(() => {
    if (localRef.current) {
      localRef.current.srcObject = localStream
    }
  }, [localStream, status])

  useEffect(() => {
    if (!connectedAt) {
      return undefined
    }
    const timer = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(timer)
  }, [connectedAt])

  const elapsed = connectedAt ? Math.max(0, Math.floor((now - connectedAt) / 1000)) : 0
  const kindLabel = media === 'video' ? 'video' : 'voice'

  return (
    <>
      {notice && (
        <div className="call-notice" role="status">
          <span>{notice}</span>
          <button type="button" aria-label="Dismiss" onClick={dismissNotice}>
            ×
          </button>
        </div>
      )}

      {status === 'calling' && peer && (
        <div className="call-backdrop">
          <div className="call-dialog">
            <div className="call-avatar">{peer.name.charAt(0)}</div>
            <h2>Calling {peer.name}...</h2>
            <p className="hint">Waiting for them to answer ({kindLabel} call).</p>
            <button type="button" className="btn btn-danger" onClick={endCall}>
              Cancel
            </button>
          </div>
        </div>
      )}

      {status === 'incoming' && peer && (
        <div className="call-backdrop">
          <div className="call-dialog">
            <div className="call-avatar">{peer.name.charAt(0)}</div>
            <h2>Incoming {kindLabel} call</h2>
            <p>
              <strong>{peer.name}</strong> is calling you.
            </p>
            <p className="hint">
              Calls are not recorded. By accepting, you agree to take this call. In an emergency,
              contact your local emergency services instead.
            </p>
            <div className="call-actions">
              <button type="button" className="btn btn-primary" onClick={acceptCall}>
                Accept
              </button>
              <button type="button" className="btn btn-danger" onClick={rejectCall}>
                Decline
              </button>
            </div>
          </div>
        </div>
      )}

      {inCall && peer && (
        <div className="call-screen">
          <video
            ref={remoteRef}
            className={media === 'video' ? 'remote-video' : 'remote-audio'}
            autoPlay
            playsInline
          />

          {media !== 'video' && (
            <div className="call-center">
              <div className="call-avatar large">{peer.name.charAt(0)}</div>
            </div>
          )}

          {media === 'video' && (
            <video ref={localRef} className="local-video" autoPlay muted playsInline />
          )}

          <div className="call-info">
            <div className="call-peer">{peer.name}</div>
            <div className="call-state">
              {status === 'connecting' ? 'Connecting...' : `Connected · ${formatElapsed(elapsed)}`}
            </div>
          </div>

          <div className="call-controls">
            <button type="button" className="btn btn-secondary" aria-pressed={muted} onClick={toggleMute}>
              {muted ? 'Unmute' : 'Mute'}
            </button>
            {media === 'video' && (
              <button
                type="button"
                className="btn btn-secondary"
                aria-pressed={cameraOff}
                onClick={toggleCamera}
              >
                {cameraOff ? 'Camera on' : 'Camera off'}
              </button>
            )}
            <button type="button" className="btn btn-danger" onClick={endCall}>
              End call
            </button>
          </div>
        </div>
      )}
    </>
  )
}

export default CallOverlay