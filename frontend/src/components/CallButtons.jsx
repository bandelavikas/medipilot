import { useCall } from '../context/CallContext.jsx'

function CallButtons({ peerUserId, peerName }) {
  const { status, canCall, socketReady, startCall } = useCall()

  if (!canCall) {
    return null
  }

  const busy = status !== 'idle'

  function begin(kind) {
    const label = kind === 'video' ? 'video call' : 'voice call'
    const devices = kind === 'video' ? 'microphone and camera' : 'microphone'
    const agreed = window.confirm(
      `Start a ${label} with ${peerName}?\n\n` +
        `Calls are not recorded. Your browser will ask to use your ${devices}.\n\n` +
        'Calls are for medication questions and follow-ups. In an emergency, contact your local emergency services instead.',
    )
    if (agreed) {
      startCall(peerUserId, peerName, kind)
    }
  }

  return (
    <div className="call-buttons">
      <button
        type="button"
        className="btn btn-primary"
        disabled={busy || !socketReady}
        onClick={() => begin('audio')}
      >
        Voice call
      </button>
      <button
        type="button"
        className="btn btn-primary"
        disabled={busy || !socketReady}
        onClick={() => begin('video')}
      >
        Video call
      </button>
      {!socketReady && <span className="hint">Connecting to the call service...</span>}
    </div>
  )
}

export default CallButtons