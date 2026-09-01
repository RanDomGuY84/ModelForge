#!/usr/bin/env node
/**
 * Minimal Agent Runtime
 * ----------------------
 * Talks to the Local AI Gateway (Spring Boot) over HTTP. Its job is:
 *   1. Take a user prompt (from CLI args, or stdin for interactive mode).
 *   2. Send it to the gateway, which routes it to the right Ollama model.
 *   3. If the model responds with a tool call, check permission with the
 *      gateway's Permission Engine, execute the tool locally (file ops etc.),
 *      report the tool event back to the gateway, and feed the result back
 *      into the conversation.
 *   4. Repeat until the model gives a plain-text final answer.
 *
 * This is intentionally simple - a real VS Code extension would replace the
 * CLI I/O with editor context (open files, selections, workspace root) and
 * richer tools, but the loop and the gateway contract stay the same.
 *
 * Usage:
 *   GATEWAY_URL=http://localhost:8080 node agent.js "list the files in src/"
 *   node agent.js --project <projectId> --workspace /path/to/repo "explain this repo"
 */

import fs from 'node:fs/promises';
import path from 'node:path';
import readline from 'node:readline';

const GATEWAY_URL = process.env.GATEWAY_URL || 'http://localhost:8080';

const TOOL_SYSTEM_PROMPT = `You are a coding agent with access to tools. When you need to use a tool, reply with ONLY a JSON object of this exact shape and nothing else:
{"type":"tool_call","tool":"<tool_name>","args":{...}}

Available tools:
- read_file: args {"path": "relative/path"} - returns file contents
- write_file: args {"path": "relative/path", "content": "..."} - overwrites a file
- list_dir: args {"path": "relative/path"} - lists directory contents

When you are ready to give your final answer to the user, reply with plain text (not JSON).`;

function parseArgs(argv) {
  const args = { _: [] };
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--project') args.project = argv[++i];
    else if (a === '--workspace') args.workspace = argv[++i];
    else if (a === '--model') args.model = argv[++i];
    else args._.push(a);
  }
  return args;
}

async function callGateway(payload) {
  const res = await fetch(`${GATEWAY_URL}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    throw new Error(`Gateway error ${res.status}: ${await res.text()}`);
  }
  return res.json();
}

async function checkPermission(toolName, projectId) {
  const url = new URL(`${GATEWAY_URL}/api/permissions/check`);
  url.searchParams.set('toolName', toolName);
  if (projectId) url.searchParams.set('projectId', projectId);
  const res = await fetch(url);
  if (!res.ok) return 'ASK';
  const data = await res.json();
  return data.decision;
}

async function reportToolEvent(conversationId, toolName, args, status, resultSummary) {
  try {
    await fetch(`${GATEWAY_URL}/api/tool-events`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        conversationId,
        toolName,
        argsJson: JSON.stringify(args),
        status,
        resultSummary,
      }),
    });
  } catch (err) {
    console.error('Warning: failed to report tool event:', err.message);
  }
}

function resolveInWorkspace(workspace, relativePath) {
  const resolved = path.resolve(workspace, relativePath);
  if (!resolved.startsWith(path.resolve(workspace))) {
    throw new Error('Refusing to access a path outside the workspace');
  }
  return resolved;
}

async function executeTool(tool, args, workspace) {
  if (tool === 'read_file') {
    const p = resolveInWorkspace(workspace, args.path);
    return await fs.readFile(p, 'utf-8');
  }
  if (tool === 'write_file') {
    const p = resolveInWorkspace(workspace, args.path);
    await fs.writeFile(p, args.content, 'utf-8');
    return `Wrote ${args.content.length} bytes to ${args.path}`;
  }
  if (tool === 'list_dir') {
    const p = resolveInWorkspace(workspace, args.path || '.');
    const entries = await fs.readdir(p, { withFileTypes: true });
    return entries.map((e) => (e.isDirectory() ? `${e.name}/` : e.name)).join('\n');
  }
  throw new Error(`Unknown tool: ${tool}`);
}

function tryParseToolCall(text) {
  const trimmed = text.trim();
  if (!trimmed.startsWith('{')) return null;
  try {
    const parsed = JSON.parse(trimmed);
    if (parsed.type === 'tool_call' && parsed.tool) return parsed;
  } catch {
    // not JSON - treat as a normal final answer
  }
  return null;
}

async function runTurn({ conversationId, projectId, model, workspace, message, role }) {
  const result = await callGateway({ conversationId, projectId, model, message, role });
  conversationId = result.conversationId;

  const toolCall = tryParseToolCall(result.reply);
  if (!toolCall) {
    return { conversationId, finalAnswer: result.reply };
  }

  console.log(`\n[agent] wants to call tool "${toolCall.tool}" with`, toolCall.args);

  const decision = await checkPermission(toolCall.tool, projectId);
  if (decision === 'DENY') {
    console.log(`[agent] permission DENIED for "${toolCall.tool}" - skipping.`);
    await reportToolEvent(conversationId, toolCall.tool, toolCall.args, 'DENIED', null);
    return runTurn({
      conversationId, projectId, model, workspace,
      role: 'tool',
      message: `Tool call denied by permission policy: ${toolCall.tool}. Please continue without it or ask the user for another approach.`,
    });
  }

  if (decision === 'ASK') {
    const approved = await askUserToApprove(toolCall);
    if (!approved) {
      await reportToolEvent(conversationId, toolCall.tool, toolCall.args, 'DENIED', 'user declined');
      return runTurn({
        conversationId, projectId, model, workspace,
        role: 'tool',
        message: `User declined the tool call: ${toolCall.tool}. Please continue without it.`,
      });
    }
  }

  try {
    const output = await executeTool(toolCall.tool, toolCall.args, workspace);
    await reportToolEvent(conversationId, toolCall.tool, toolCall.args, 'SUCCEEDED', output.slice(0, 200));
    return runTurn({
      conversationId, projectId, model, workspace,
      role: 'tool',
      message: `Tool "${toolCall.tool}" result:\n${output}`,
    });
  } catch (err) {
    await reportToolEvent(conversationId, toolCall.tool, toolCall.args, 'FAILED', err.message);
    return runTurn({
      conversationId, projectId, model, workspace,
      role: 'tool',
      message: `Tool "${toolCall.tool}" failed: ${err.message}`,
    });
  }
}

function askUserToApprove(toolCall) {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise((resolve) => {
    rl.question(`Allow "${toolCall.tool}" with args ${JSON.stringify(toolCall.args)}? [y/N] `, (answer) => {
      rl.close();
      resolve(answer.trim().toLowerCase() === 'y');
    });
  });
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const workspace = path.resolve(args.workspace || process.cwd());
  const projectId = args.project || null;
  const model = args.model || null;
  const promptFromArgs = args._.join(' ').trim();

  console.log(`Local AI Agent Runtime`);
  console.log(`  gateway:   ${GATEWAY_URL}`);
  console.log(`  workspace: ${workspace}`);
  console.log(`  project:   ${projectId || '(none)'}\n`);

  let conversationId = null;

  const send = async (message) => {
    // Seed the tool-use system prompt only on the very first turn of a fresh conversation.
    const firstMessage = conversationId === null
      ? `${TOOL_SYSTEM_PROMPT}\n\nUser request: ${message}`
      : message;

    const { conversationId: newId, finalAnswer } = await runTurn({
      conversationId, projectId, model, workspace, message: firstMessage, role: 'user',
    });
    conversationId = newId;
    console.log(`\n[assistant] ${finalAnswer}\n`);
  };

  if (promptFromArgs) {
    await send(promptFromArgs);
    return;
  }

  // Interactive mode
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout, prompt: '> ' });
  rl.prompt();
  rl.on('line', async (line) => {
    const trimmed = line.trim();
    if (!trimmed) return rl.prompt();
    if (trimmed === '/exit') return rl.close();
    try {
      await send(trimmed);
    } catch (err) {
      console.error('Error:', err.message);
    }
    rl.prompt();
  });
}

main().catch((err) => {
  console.error('Fatal error:', err.message);
  process.exit(1);
});
