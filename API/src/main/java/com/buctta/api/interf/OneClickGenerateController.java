package com.buctta.api.interf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

@RestController
@RequestMapping("/api/ai")
public class OneClickGenerateController {

    // 存放 id -> SseEmitter 的映射，用于前端通过 id 订阅 SSE
    private static final ConcurrentHashMap<String, SseEmitter> EMITTERS = new ConcurrentHashMap<>();
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    /**
     * 启动一次生成任务（POST），返回一个 id，前端用该 id 建立 EventSource 订阅流。
     */
    /* ===================== 1. 入口参数改成数组 ===================== */
    @PostMapping("/generate/start")
    public ResponseEntity<?> startGenerate(
            @RequestParam(value = "isCorrect",  required = false) String isCorrect,
            @RequestParam(value = "subject",    required = false) String subject,
            @RequestParam(value = "grade",      required = false) String grade,
            @RequestParam(value = "paperType",  required = false) String paperType,
            @RequestParam(value = "scores",     required = false) String scores,
            @RequestParam(value = "studentScore",required = false) String studentScore,
            @RequestParam(value = "extractedTexts") List<String> extractedTexts,
            @RequestParam(value = "fileNames") List<String> fileNames
    ){
        String id = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(0L);
        EMITTERS.put(id, emitter);

        EXECUTOR.submit(() -> doGenerate(id, emitter, extractedTexts,fileNames));

        Map<String,Object> rsp = new HashMap<>();
        rsp.put("id",id);
        rsp.put("status","started");
        return ResponseEntity.ok(rsp);
    }

    /* ===================== 2. 真正干活的线程 ===================== */
    private void doGenerate(String id, SseEmitter emitter, List<String> texts,List<String> fnames){
        try{
            int total = texts.size();
            for(int index=0; index<total; index++){
                String singleText = texts.get(index);
                /* 告诉前端“第 index 个文件开始” */
                emitter.send(SseEmitter.event()
                        .name("fileStart")
                        .data(Map.of("index",index,"total",total)));

                /* ---------- 调外部 SSE 接口（原逻辑几乎照搬） ---------- */
                HttpURLConnection conn = getHttpURLConnection();

                String payload = String.format(
                        "{\"appCode\":\"ti39Ohdy6k\",\"userNid\":\"N7I7pSTrm3\",\"sessionNid\":\"PNeg1BjP6x\",\"chatNid\":\"JJdylJaMSF\",\"testFlag\":true,\"reasoningFlag\":false,\"metadata\":{\"thinkingEnabled\":0},\"question\":%s}",
                        toJsonString(singleText)
                );
                try(OutputStream os = conn.getOutputStream()){
                    byte[] in = payload.getBytes(StandardCharsets.UTF_8);
                    os.write(in,0,in.length);os.flush();
                }

                int st = conn.getResponseCode();
                InputStream is = (st>=200&&st<400)?conn.getInputStream():conn.getErrorStream();
                try(BufferedReader br = new BufferedReader(new InputStreamReader(is,StandardCharsets.UTF_8))){
                    String line;
                    StringBuilder buf = new StringBuilder(); // 累积字符
                    final String STOP = "(AI生成)";
                    while((line=br.readLine())!=null){
                        line=line.trim();
                        /* 1. 只认 OUT_COMPLETE 事件 */
                        if (!line.startsWith("event:OUT_COMPLETE")) continue;

                        /* 2. 读下一行 data */
                        String dataLine;
                        while ((dataLine = br.readLine()) != null) {
                            dataLine = dataLine.trim();
                            if (dataLine.startsWith("data:")) {
                                /* 检测停止标记 */
                                String segment = dataLine.substring(5).trim()
                                        .replaceAll("^\\{\"text\":\"", "")
                                        .replaceAll("\"}$", "");
                                if (segment.contains(STOP)) {
                                    int cut = segment.indexOf(STOP);
                                    buf.append(segment, 0, cut);          // 只保留前面部分
                                    String raw = buf.toString().trim()
                                            .replace("\\\"", "\"")
                                            .replace("\\n", "\n")
                                            .replace("\\r", "\r")
                                            .replace("\\t", "\t")
                                            .replace("\\\\", "\\");;   // 原始带转义文本

                                    /* 1. 把转义字符串还原成 JSON 对象 */
                                    ObjectMapper mapper = new ObjectMapper();
                                    JsonNode node = mapper.readValue(raw, JsonNode.class);

                                    /* 2. 提取信息 */
                                    // 例：张三_2025-06-01_ADC实验_202107010101.pdf
                                    String name="Unidentified",date="Unidentified",exp="Unidentified",ID="Unidentified";
                                    Pattern p = Pattern.compile("([^_]+)_(\\d{8})_(.+)_([^.]+)\\.\\w+");
                                    Matcher m = p.matcher(fnames.get(index));
                                    if (m.matches()) {
                                        name   = m.group(1); // 张三
                                        date   = m.group(2); // 2025-06-01
                                        exp    = m.group(3); // ADC实验
                                        ID  = m.group(4); // 202107010101
                                    }
                                    int    score = node.get("分数").asInt();
                                    String basis = node.get("评分依据").asText();

                                    /* 3. 发给前端（只发评分依据，也可拼接） */
                                    String judgeResult=
                                            "姓名："+name+
                                            "\n学号："+ID+
                                            "\n日期："+date+
                                            "\n报告名称："+exp+
                                            "\n分数："+ Integer.toString(score)+
                                            "\n评判依据：\n"+basis;

                                    emitter.send(SseEmitter.event().name("message").data(judgeResult));
                                    buf.setLength(0);          // 清空累积区
                                    break;                     // 跳出 data 循环
                                }
                            }
                        }
                    }
                }
                /* ---------- 外部接口结束 ---------- */

                /* 告诉前端“第 index 个文件结束” */
                emitter.send(SseEmitter.event().name("fileEnd").data(Map.of("index",index,"total",total)));
            }

            /* 全部文件完成 */
            emitter.send(SseEmitter.event().name("done").data("[COMPLETED]"));
            emitter.complete();
        }catch(Exception ex){
            try{ emitter.send(SseEmitter.event().name("error").data("生成异常："+ex.getMessage()));
            }catch(Exception ig){}
            emitter.completeWithError(ex);
        }finally{
            EMITTERS.remove(id);
        }
    }

    private static HttpURLConnection getHttpURLConnection() throws IOException {
        String apiUrl = "https://cloudapi.polymas.com/bot/v2/completions/chat/stream";
        String auth   = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJON0k3cFNUcm0zIiwicm5TdHIiOiJkRllocHFhWkdkQjczY3dkM215eTNqN29XSm9HdTdyYSIsInR5cGUiOiJaSFMiLCJ1c2VyTmlkIjoiTjdJN3BTVHJtMyJ9.o99-sfD3_TmewtrmT7O-ItrLjGKAIMSaEdPGFMaoU0U";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(0);
        conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
        conn.setRequestProperty("Accept","text/event-stream");
        conn.setRequestProperty("authorization",auth);
        return conn;
    }

    /**
     * 前端通过这个 endpoint 建立 SSE 连接并接收实时生成片段
     */
    @GetMapping(path = "/generate/stream/{id}")
    public SseEmitter stream(@PathVariable("id") String id) {
        SseEmitter emitter = EMITTERS.get(id);
        if (emitter == null) {
            // 如果没有找到对应的 emitter，创建一个短时的 emitter 并返回错误信息后完成
            SseEmitter tmp = new SseEmitter(5000L);
            try {
                tmp.send(SseEmitter.event().name("error").data("no such id: " + id));
            } catch (Exception ignored) {
            }
            tmp.complete();
            return tmp;
        }
        return emitter;
    }

    // 简单工具：把字符串转为 JSON 字符串（转义内部引号等）
    private static String toJsonString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder();
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
