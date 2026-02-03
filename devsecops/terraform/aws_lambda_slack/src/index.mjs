import crypto from 'crypto';

const GITHUB_TOKEN = process.env.GITHUB_TOKEN;
const GITHUB_REPO = process.env.GITHUB_REPO;
const SLACK_SIGNING_SECRET = process.env.SLACK_SIGNING_SECRET;

function getBody(event) {
  // API Gateway HTTP API may base64 encode the body
  if (event.isBase64Encoded) {
    return Buffer.from(event.body, 'base64').toString('utf-8');
  }
  return event.body;
}

function verifySlackRequest(event, rawBody) {
  const timestamp = event.headers['x-slack-request-timestamp'];
  const signature = event.headers['x-slack-signature'];

  console.log('Verifying signature:', {
    hasTimestamp: !!timestamp,
    hasSignature: !!signature,
    signaturePrefix: signature?.substring(0, 10),
    secretLength: SLACK_SIGNING_SECRET?.length,
    isBase64Encoded: event.isBase64Encoded,
    bodyPreview: rawBody?.substring(0, 50),
  });

  // Check timestamp to prevent replay attacks (5 min window)
  const now = Math.floor(Date.now() / 1000);
  if (Math.abs(now - parseInt(timestamp)) > 300) {
    console.log('Timestamp too old:', { now, timestamp, diff: Math.abs(now - parseInt(timestamp)) });
    return false;
  }

  const sigBaseString = `v0:${timestamp}:${rawBody}`;
  const hmac = crypto.createHmac('sha256', SLACK_SIGNING_SECRET);
  hmac.update(sigBaseString);
  const expectedSignature = `v0=${hmac.digest('hex')}`;

  console.log('Signature comparison:', {
    received: signature,
    expected: expectedSignature,
    match: signature === expectedSignature,
  });

  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(expectedSignature)
  );
}

async function triggerGitHubWorkflow(action, module) {
  const url = `https://api.github.com/repos/${GITHUB_REPO}/actions/workflows/terraform.yml/dispatches`;

  console.log('Triggering GitHub workflow:', { action, module, repo: GITHUB_REPO });

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Authorization': `token ${GITHUB_TOKEN}`,
      'Accept': 'application/vnd.github.v3+json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      ref: 'main',
      inputs: {
        action: action,
        module: module,
      },
    }),
  });

  console.log('GitHub API response:', response.status);
  if (response.status !== 204) {
    const body = await response.text();
    console.log('GitHub API error body:', body);
  }

  return response.status === 204;
}

export async function handler(event) {
  console.log('Incoming request:', JSON.stringify({
    path: event.rawPath,
    headers: Object.keys(event.headers),
    hasBody: !!event.body,
    isBase64Encoded: event.isBase64Encoded,
  }));

  // Decode body if base64 encoded
  const rawBody = getBody(event);

  // Verify Slack signature
  if (!verifySlackRequest(event, rawBody)) {
    console.log('Signature verification FAILED');
    return {
      statusCode: 401,
      body: 'Invalid signature',
    };
  }
  console.log('Signature verification OK');

  // Parse Slack command
  const params = new URLSearchParams(rawBody);
  const text = params.get('text') || '';
  const userId = params.get('user_id');
  console.log('Command received:', { text, userId });

  // Parse command: /infra apply ec2 or /infra destroy ec2
  const parts = text.trim().split(/\s+/);
  const action = parts[0]?.toLowerCase();
  const module = parts[1]?.toLowerCase();

  // Validate action
  const validActions = ['plan', 'apply', 'destroy'];
  if (!validActions.includes(action)) {
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        response_type: 'ephemeral',
        text: `Usage: \`/infra <action> <module>\`\nActions: ${validActions.join(', ')}\nModules: aws_ec2`,
      }),
    };
  }

  // Validate module
  const validModules = ['aws_ec2'];
  const targetModule = module === 'ec2' ? 'aws_ec2' : module;
  if (!validModules.includes(targetModule)) {
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        response_type: 'ephemeral',
        text: `Module invalide: \`${module}\`\nModules disponibles: ec2`,
      }),
    };
  }

  // Trigger GitHub Actions
  const success = await triggerGitHubWorkflow(action, targetModule);

  if (success) {
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        response_type: 'in_channel',
        text: `🚀 <@${userId}> a lancé \`terraform ${action}\` sur \`${targetModule}\`\n<https://github.com/${GITHUB_REPO}/actions|Voir les logs>`,
      }),
    };
  } else {
    return {
      statusCode: 200,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        response_type: 'ephemeral',
        text: '❌ Erreur lors du déclenchement du workflow GitHub',
      }),
    };
  }
}
