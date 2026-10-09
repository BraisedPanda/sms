package com.xqy.sms.knowledge.provider.service;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentChunker {
    public List<String> split(String text, int size, int overlap, String strategy) {
        validate(size, overlap, strategy);
        if (text == null || text.isBlank()) throw new IllegalArgumentException("文档内容不能为空");
        int[] codePoints = text.codePoints().toArray();
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < codePoints.length) {
            int end = Math.min(start + size, codePoints.length);
            if ("PARAGRAPH".equals(strategy) && end < codePoints.length) {
                int minimum = start + Math.max(overlap + 1, size / 2);
                for (int at = end - 1; at >= minimum; at--) {
                    if (codePoints[at] == '\n') { end = at + 1; break; }
                }
            }
            String chunk = new String(codePoints, start, end - start);
            if (!chunk.isBlank()) chunks.add(chunk);
            if (chunks.size() > 10000) throw new IllegalArgumentException("分块超过 10000 个，请增大分块大小");
            if (end == codePoints.length) break;
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    public void validate(int size, int overlap, String strategy) {
        if (size < 100 || size > 8000 || overlap < 0 || overlap >= size || overlap > size / 2)
            throw new IllegalArgumentException("分块大小须为 100–8000，重叠须为 0 到分块大小的一半");
        if (!List.of("FIXED", "PARAGRAPH").contains(strategy)) throw new IllegalArgumentException("未知切分策略");
    }
}
