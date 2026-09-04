import { check } from 'k6';
import exec from 'k6/execution';
import { Counter, Rate, Trend } from 'k6/metrics';
import { clearInterval, setInterval } from 'k6/timers';
import { WebSocket } from 'k6/websockets';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const profile = __ENV.K6_PROFILE || 'full';
const wsUrl = __ENV.WS_URL || 'ws://localhost:8080/chatHub';
const messageIntervalMs = Number(__ENV.MESSAGE_INTERVAL_MS || 10000);
const deliveryTimeoutMs = Number(__ENV.DELIVERY_TIMEOUT_MS || 5000);
const latencyP95Ms = Number(__ENV.LATENCY_P95_MS || 1000);
const reportDir = __ENV.REPORT_DIR || 'reports';

const connections = new Counter('chat_connections');
const connectionErrors = new Counter('chat_connection_errors');
const invalidPayloads = new Counter('chat_invalid_payloads');
const messagesSent = new Counter('chat_messages_sent');
const messagesReceived = new Counter('chat_messages_received');
const ownMessagesReceived = new Counter('chat_own_messages_received');
const handshakeLatency = new Trend('chat_handshake_latency', true);
const ownDeliveryLatency = new Trend('chat_own_delivery_latency', true);
const ownDeliverySuccess = new Rate('chat_own_delivery_success');
const server1Share = new Rate('chat_server_1_share');
const server2Share = new Rate('chat_server_2_share');
const server3Share = new Rate('chat_server_3_share');

const fullStages = [
  { duration: '1m', target: 100 },
  { duration: '2m', target: 250 },
  { duration: '2m', target: 500 },
  { duration: '2m', target: 750 },
  { duration: '3m', target: 1000 },
  { duration: '3m', target: 1000 },
  { duration: '2m', target: 0 },
];

const smokeStages = [
  { duration: '5s', target: 3 },
  { duration: '20s', target: 3 },
  { duration: '5s', target: 0 },
];

export const options = {
  scenarios: {
    websocket_chat: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: profile === 'smoke' ? smokeStages : fullStages,
      gracefulRampDown: '10s',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    chat_connection_errors: ['count==0'],
    chat_invalid_payloads: ['count==0'],
    chat_own_delivery_success: ['rate>0.99'],
    chat_own_delivery_latency: [`p(95)<${latencyP95Ms}`],
    chat_server_1_share: ['rate>0.25', 'rate<0.42'],
    chat_server_2_share: ['rate>0.25', 'rate<0.42'],
    chat_server_3_share: ['rate>0.25', 'rate<0.42'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  const connectionStartedAt = Date.now();
  const user = `k6-vu-${exec.vu.idInTest}`;
  const pending = {};
  let sequence = 0;
  let serverInfoReceived = false;
  let sendTimer;
  let expiryTimer;

  const socket = new WebSocket(wsUrl, [], {
    tags: { test: 'websocket-chat', profile },
  });

  function sendMessage() {
    if (socket.readyState !== 1 || !serverInfoReceived) return;
    const token = `${user}-${Date.now()}-${sequence++}`;
    pending[token] = Date.now();
    socket.send(JSON.stringify({
      type: 'SEND_MESSAGE',
      user,
      message: token,
    }));
    messagesSent.add(1);
  }

  function expirePendingMessages() {
    const now = Date.now();
    Object.keys(pending).forEach((token) => {
      if (now - pending[token] >= deliveryTimeoutMs) {
        ownDeliverySuccess.add(false);
        delete pending[token];
      }
    });
  }

  socket.onopen = () => {
    sendTimer = setInterval(sendMessage, messageIntervalMs);
    expiryTimer = setInterval(expirePendingMessages, 1000);
  };

  socket.onmessage = (event) => {
    let payload;
    try {
      payload = JSON.parse(event.data);
    } catch (_) {
      invalidPayloads.add(1);
      return;
    }

    if (payload.type === 'SERVER_INFO') {
      if (serverInfoReceived) return;
      serverInfoReceived = true;
      const knownServer = ['Server-1', 'Server-2', 'Server-3'].includes(payload.serverId);
      if (!knownServer) invalidPayloads.add(1, { type: 'UNKNOWN_SERVER' });
      connections.add(1, { server: payload.serverId });
      handshakeLatency.add(Date.now() - connectionStartedAt, { server: payload.serverId });
      server1Share.add(payload.serverId === 'Server-1');
      server2Share.add(payload.serverId === 'Server-2');
      server3Share.add(payload.serverId === 'Server-3');
      check(payload, {
        'server info identifies a known server': () => knownServer,
      });
      sendMessage();
      return;
    }

    if (payload.type === 'RECEIVE_MESSAGE') {
      messagesReceived.add(1, { server: payload.serverId });
      if (payload.user === user && pending[payload.text] !== undefined) {
        ownDeliveryLatency.add(Date.now() - pending[payload.text], { server: payload.serverId });
        ownDeliverySuccess.add(true);
        ownMessagesReceived.add(1);
        delete pending[payload.text];
      }
      return;
    }

    if (payload.type === 'ERROR') {
      invalidPayloads.add(1, { code: payload.code || 'UNKNOWN' });
      return;
    }

    invalidPayloads.add(1, { type: payload.type || 'UNKNOWN' });
  };

  socket.onerror = () => {
    connectionErrors.add(1);
  };

  socket.onclose = () => {
    if (sendTimer) clearInterval(sendTimer);
    if (expiryTimer) clearInterval(expiryTimer);
    Object.keys(pending).forEach((token) => {
      ownDeliverySuccess.add(false);
      delete pending[token];
    });
    check(serverInfoReceived, {
      'websocket received server info': (value) => value === true,
    });
  };
}

export function handleSummary(data) {
  const output = {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
  output[`${reportDir}/k6-websocket-report.html`] = htmlReport(data);
  output[`${reportDir}/k6-summary.json`] = JSON.stringify(data, null, 2);
  return output;
}
