/**
 * DeepSeek API 服务封装层
 * ────────────────────────────────────────────
 * 功能：
 *   - 统一的 API 调用接口
 *   - 自动重试 & 指数退避
 *   - 请求超时控制
 *   - 流式/非流式双模式
 *   - 完整的错误分类处理
 *   - Token 用量统计
 *
 * DeepSeek API 文档: https://platform.deepseek.com/api-docs
 */

const https = require('https');
const http = require('http');

// ============================================================
//  配置
// ============================================================

const CONFIG = {
  apiKey: process.env.DEEPSEEK_API_KEY || '',
  baseUrl: process.env.DEEPSEEK_BASE_URL || 'https://api.deepseek.com/v1',
  model: process.env.DEEPSEEK_MODEL || 'deepseek-chat',
  timeout: parseInt(process.env.REQUEST_TIMEOUT, 10) || 60000,
  maxRetries: 3,
  retryDelayMs: 1000,
};

// ============================================================
//  自定义错误类型
// ============================================================

class DeepSeekError extends Error {
  constructor(message, statusCode, type, rawError = null) {
    super(message);
    this.name = 'DeepSeekError';
    this.statusCode = statusCode;
    this.type = type; // 'auth' | 'rate_limit' | 'server' | 'network' | 'timeout' | 'unknown'
    this.rawError = rawError;
  }
}

// ============================================================
//  内部工具函数
// ============================================================

/**
 * 等待 ms 毫秒
 */
function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/**
 * 计算指数退避时间: delay * 2^attempt + 随机抖动
 */
function calcBackoff(attempt, baseDelay = CONFIG.retryDelayMs) {
  const exponential = baseDelay * Math.pow(2, attempt);
  const jitter = Math.random() * 1000;
  return Math.min(exponential + jitter, 30000); // 上限 30 秒
}

/**
 * 判断是否为可重试的错误
 */
function isRetryable(statusCode) {
  return statusCode === 429 || statusCode === 500 || statusCode === 502 || statusCode === 503 || statusCode === 504;
}

/**
 * 解析错误类型
 */
function classifyError(statusCode, body) {
  if (statusCode === 401 || statusCode === 403) return 'auth';
  if (statusCode === 429) return 'rate_limit';
  if (statusCode >= 500) return 'server';
  return 'unknown';
}

/**
 * 从 URL 字符串解析 hostname / port / path
 */
function parseBaseUrl(urlStr) {
  const u = new URL(urlStr);
  return {
    protocol: u.protocol.replace(':', ''),
    hostname: u.hostname,
    port: u.port || (u.protocol === 'https:' ? 443 : 80),
    pathPrefix: u.pathname.replace(/\/$/, ''),
  };
}

/**
 * 发送单次 HTTP 请求（返回原始响应）
 */
function httpRequest(method, urlPath, headers, body) {
  const { protocol, hostname, port, pathPrefix } = parseBaseUrl(CONFIG.baseUrl);
  const transport = protocol === 'https' ? https : http;

  const fullPath = pathPrefix + urlPath;

  const options = {
    method,
    hostname,
    port,
    path: fullPath,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${CONFIG.apiKey}`,
      ...headers,
    },
    timeout: CONFIG.timeout,
  };

  return new Promise((resolve, reject) => {
    const req = transport.request(options, (res) => {
      const chunks = [];
      res.on('data', (chunk) => chunks.push(chunk));
      res.on('end', () => {
        const raw = Buffer.concat(chunks).toString('utf8');
        let data = null;
        try {
          data = JSON.parse(raw);
        } catch (_) {
          data = { _raw: raw };
        }

        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve({ statusCode: res.statusCode, headers: res.headers, data });
        } else {
          const errType = classifyError(res.statusCode, data);
          const errMsg =
            (data && (data.error?.message || data.message)) || `HTTP ${res.statusCode}`;
          reject(new DeepSeekError(errMsg, res.statusCode, errType, data));
        }
      });
    });

    req.on('timeout', () => {
      req.destroy();
      reject(new DeepSeekError('请求超时', 0, 'timeout'));
    });

    req.on('error', (err) => {
      reject(new DeepSeekError(`网络错误: ${err.message}`, 0, 'network', err));
    });

    if (body) {
      req.write(JSON.stringify(body));
    }
    req.end();
  });
}

// ============================================================
//  公开 API
// ============================================================

/**
 * 发送聊天补全请求（非流式）
 *
 * @param {Array<{role: string, content: string}>} messages - 消息列表
 * @param {Object} [opts] - 可选参数
 * @param {string} [opts.model] - 模型名称
 * @param {number} [opts.temperature] - 温度 0~2
 * @param {number} [opts.maxTokens] - 最大输出 token 数
 * @param {number} [opts.topP] - nucleus sampling
 * @param {boolean} [opts.jsonMode] - 是否要求 JSON 格式输出
 * @returns {Promise<{content: string, usage: {promptTokens: number, completionTokens: number, totalTokens: number}}>}
 */
async function chatCompletion(messages, opts = {}) {
  const body = {
    model: opts.model || CONFIG.model,
    messages,
    temperature: opts.temperature ?? 0.7,
    max_tokens: opts.maxTokens ?? 4096,
    top_p: opts.topP ?? 1.0,
    stream: false,
  };

  // JSON 模式：提示词必须包含 "json" 字样
  if (opts.jsonMode) {
    body.response_format = { type: 'json_object' };
  }

  let lastError = null;

  for (let attempt = 0; attempt <= CONFIG.maxRetries; attempt++) {
    try {
      const { statusCode, data } = await httpRequest('POST', '/chat/completions', {}, body);

      const choice = data.choices && data.choices[0];
      const message = choice ? choice.message : { content: '' };

      return {
        content: message.content || '',
        usage: data.usage
          ? {
              promptTokens: data.usage.prompt_tokens || 0,
              completionTokens: data.usage.completion_tokens || 0,
              totalTokens: data.usage.total_tokens || 0,
            }
          : { promptTokens: 0, completionTokens: 0, totalTokens: 0 },
      };
    } catch (err) {
      lastError = err;
      // 非可重试错误直接抛出
      if (err instanceof DeepSeekError && !isRetryable(err.statusCode)) {
        throw err;
      }
      // 最后一次尝试也失败
      if (attempt === CONFIG.maxRetries) {
        throw err;
      }
      // 等待后退避重试
      const delay = calcBackoff(attempt);
      console.warn(`[DeepSeek] 请求失败，${delay}ms 后重试 (${attempt + 1}/${CONFIG.maxRetries}): ${err.message}`);
      await sleep(delay);
    }
  }

  throw lastError;
}

/**
 * 发送聊天补全请求（流式 SSE）
 *
 * @param {Array<{role: string, content: string}>} messages - 消息列表
 * @param {Object} [opts] - 可选参数
 * @param {function(chunk: string): void} onChunk - 每收到一块文本时回调
 * @param {function(): void} onDone - 流结束时回调
 * @param {function(err: Error): void} onError - 出错时回调
 * @returns {Promise<void>}
 */
async function chatCompletionStream(messages, opts = {}, onChunk, onDone, onError) {
  const body = {
    model: opts.model || CONFIG.model,
    messages,
    temperature: opts.temperature ?? 0.7,
    max_tokens: opts.maxTokens ?? 4096,
    top_p: opts.topP ?? 1.0,
    stream: true,
  };

  try {
    const { protocol, hostname, port, pathPrefix } = parseBaseUrl(CONFIG.baseUrl);
    const transport = protocol === 'https' ? https : http;

    const options = {
      method: 'POST',
      hostname,
      port,
      path: pathPrefix + '/chat/completions',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${CONFIG.apiKey}`,
        Accept: 'text/event-stream',
      },
      timeout: CONFIG.timeout,
    };

    const req = transport.request(options, (res) => {
      if (res.statusCode >= 300) {
        let data = '';
        res.on('data', (c) => (data += c));
        res.on('end', () => {
          let parsed;
          try {
            parsed = JSON.parse(data);
          } catch (_) {
            parsed = {};
          }
          onError(new DeepSeekError(parsed.error?.message || `HTTP ${res.statusCode}`, res.statusCode, classifyError(res.statusCode, parsed)));
        });
        return;
      }

      let buffer = '';
      res.on('data', (chunk) => {
        buffer += chunk.toString('utf8');
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // 保留不完整的行

        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith('data: ')) continue;
          const payload = trimmed.slice(6);
          if (payload === '[DONE]') {
            onDone();
            return;
          }
          try {
            const parsed = JSON.parse(payload);
            const delta = parsed.choices?.[0]?.delta?.content;
            if (delta) {
              onChunk(delta);
            }
          } catch (_) {
            // 忽略解析失败的单行
          }
        }
      });

      res.on('end', () => {
        // 处理 buffer 中剩余的内容
        if (buffer.trim().startsWith('data: ')) {
          const payload = buffer.trim().slice(6);
          if (payload !== '[DONE]') {
            try {
              const parsed = JSON.parse(payload);
              const delta = parsed.choices?.[0]?.delta?.content;
              if (delta) onChunk(delta);
            } catch (_) { /* ignore */ }
          }
        }
        onDone();
      });

      res.on('error', (err) => {
        onError(new DeepSeekError(`流式响应错误: ${err.message}`, 0, 'network', err));
      });
    });

    req.on('timeout', () => {
      req.destroy();
      onError(new DeepSeekError('流式请求超时', 0, 'timeout'));
    });

    req.on('error', (err) => {
      onError(new DeepSeekError(`网络错误: ${err.message}`, 0, 'network', err));
    });

    req.write(JSON.stringify(body));
    req.end();
  } catch (err) {
    onError(err instanceof DeepSeekError ? err : new DeepSeekError(err.message, 0, 'unknown', err));
  }
}

// ============================================================
//  高级封装：面向业务场景
// ============================================================

/**
 * 生成知识点手册内容
 *
 * @param {Object} params
 * @param {string} params.subject - 学科
 * @param {string} params.grade - 年级
 * @param {string} params.topic - 章节/主题
 * @param {string} [params.focus] - 重点关注的知识点
 * @returns {Promise<Object>} 结构化知识点 JSON
 */
async function generateKnowledgePoints(params) {
  const { subject, grade, topic, focus } = params;

  const systemPrompt = `你是一位资深教研员，精通${subject}学科。请根据用户提供的章节信息，生成一份结构化的"知识点手册"。

要求：
1. 用 JSON 格式输出
2. 包含以下字段：chapterTitle（章节标题）、overview（章节概述，150字以内）、knowledgePoints（知识点数组）
3. 每个知识点包含：name（名称）、explanation（详细讲解，200-400字）、keyConcepts（核心概念列表）、commonMistakes（常见误区列表）、difficulty（难度：easy/medium/hard）、importance（重要性：required/extended/enrichment）
4. 输出 3-8 个知识点
5. 语言专业但通俗易懂`;

  const userPrompt = focus
    ? `学科：${subject}\n年级：${grade}\n章节：${topic}\n重点关注：${focus}`
    : `学科：${subject}\n年级：${grade}\n章节：${topic}`;

  const result = await chatCompletion(
    [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt },
    ],
    { temperature: 0.3, jsonMode: true }
  );

  try {
    return JSON.parse(result.content);
  } catch (_) {
    // 如果模型没严格输出 JSON，尝试提取
    const jsonMatch = result.content.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
    throw new Error('模型输出无法解析为 JSON');
  }
}

/**
 * 生成智能教案
 *
 * @param {Object} params
 * @param {string} params.subject - 学科
 * @param {string} params.grade - 年级
 * @param {string} params.topic - 课题
 * @param {string} [params.lessonType] - 课型（新授课/复习课/习题课/实验课）
 * @param {number} [params.duration] - 课时（分钟）
 * @param {string} [params.teachingStyle] - 教学风格偏好
 * @param {string} [params.extraRequirements] - 额外要求
 * @returns {Promise<Object>} 结构化教案 JSON
 */
async function generateLessonPlan(params) {
  const {
    subject,
    grade,
    topic,
    lessonType = '新授课',
    duration = 45,
    teachingStyle = '',
    extraRequirements = '',
  } = params;

  const systemPrompt = `你是一位经验丰富的${subject}教师，具有20年以上教龄。请根据提供的信息，生成一份专业、完整的教案。

要求：
1. 用 JSON 格式输出
2. 包含以下字段：
   - title（课题名称）
   - lessonType（课型）
   - duration（课时/分钟）
   - teachingObjectives（教学目标数组，每项包含 category: knowledge/skill/emotion 和 content）
   - keyPoints（教学重点数组）
   - difficultPoints（教学难点数组，每项包含 point 和 strategy/突破策略）
   - teachingResources（教学资源/教具准备）
   - teachingProcess（教学过程数组，每项包含：
       stageName/环节名称、
       duration/时长分钟、
       teacherActivity/教师活动、
       studentActivity/学生活动、
       designIntent/设计意图）
   - boardDesign（板书设计，纯文本）
   - homework（作业布置，分层：basic/基础、improve/提升、challenge/挑战）
   - teachingReflection（教学反思模板）
3. 教学内容要专业、准确、紧扣课标
4. 教学过程要体现以学生为中心的教学理念
5. 时间分配要合理，各环节时间之和应接近总时长`;

  let userPrompt = `学科：${subject}\n年级：${grade}\n课题：${topic}\n课型：${lessonType}\n课时：${duration}分钟`;
  if (teachingStyle) {
    userPrompt += `\n教学风格偏好：${teachingStyle}`;
  }
  if (extraRequirements) {
    userPrompt += `\n额外要求：${extraRequirements}`;
  }

  const result = await chatCompletion(
    [
      { role: 'system', content: systemPrompt },
      { role: 'user', content: userPrompt },
    ],
    { temperature: 0.5, jsonMode: true, maxTokens: 8192 }
  );

  try {
    return JSON.parse(result.content);
  } catch (_) {
    const jsonMatch = result.content.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      return JSON.parse(jsonMatch[0]);
    }
    throw new Error('模型输出无法解析为 JSON');
  }
}

/**
 * 流式聊天（用于前端 SSE 展示）
 *
 * @param {Array<{role: string, content: string}>} messages
 * @param {Object} opts
 * @param {function(chunk: string): void} onChunk
 * @param {function(): void} onDone
 * @param {function(err: Error): void} onError
 */
async function streamChat(messages, opts, onChunk, onDone, onError) {
  return chatCompletionStream(messages, opts, onChunk, onDone, onError);
}

// ============================================================
//  导出
// ============================================================

module.exports = {
  // 底层 API
  chatCompletion,
  chatCompletionStream,
  streamChat,

  // 业务 API
  generateKnowledgePoints,
  generateLessonPlan,

  // 工具
  DeepSeekError,
  CONFIG,
};
