package com.xqy.sms.knowledge.provider.service;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class DocumentPipelineTest {
    final DocumentTextParser parser = new DocumentTextParser();
    final DocumentChunker chunker = new DocumentChunker();
    @Test void preservesUnicodeAndOverlapWithoutLosingContent() {
        String text = "😀你好".repeat(130);
        var parts = chunker.split(text, 100, 20, "FIXED");
        StringBuilder restored = new StringBuilder(parts.getFirst());
        for (int i=1;i<parts.size();i++) {
            String previous = parts.get(i-1), next = parts.get(i);
            assertEquals(previous.substring(previous.offsetByCodePoints(0, previous.codePointCount(0, previous.length())-20)),
                    next.substring(0,next.offsetByCodePoints(0,20)));
            restored.append(next.substring(next.offsetByCodePoints(0,20)));
        }
        assertEquals(text, restored.toString());
        parts.forEach(p -> assertTrue(p.codePointCount(0,p.length()) <= 100));
    }
    @Test void prefersParagraphBoundaryAndRejectsInvalidPolicies() {
        var parts = chunker.split("a".repeat(70)+"\n"+"b".repeat(100),100,0,"PARAGRAPH");
        assertEquals("a".repeat(70)+"\n",parts.getFirst());
        assertThrows(IllegalArgumentException.class,()->chunker.split("x",100,60,"FIXED"));
        assertThrows(IllegalArgumentException.class,()->chunker.split("x",100,0,"UNKNOWN"));
        assertThrows(IllegalArgumentException.class,()->chunker.split(" ",100,0,"FIXED"));
    }
    @Test void parsesUtf8HtmlAndRejectsUnsupportedMalformedOrEmptyContent() {
        assertEquals("你好", parser.parse("a.md","\uFEFF你好".getBytes(StandardCharsets.UTF_8)));
        assertEquals("正文",parser.parse("a.html","<script>alert(1)</script><p>正文</p>".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.exe",new byte[]{1}));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.txt",new byte[]{(byte)0xff}));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.txt",new byte[0]));
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.txt",new byte[DocumentTextParser.MAX_UPLOAD_BYTES+1]));
    }
    @Test void parsesDocxAndRejectsExternalEntitiesAndCompressionBombs() throws Exception {
        String xml = "<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:p><w:r><w:t>正文</w:t></w:r></w:p></w:body></w:document>";
        assertEquals("正文",parser.parse("a.docx",zip("word/document.xml",xml.getBytes(StandardCharsets.UTF_8))));
        String evil = "<?xml version='1.0'?><!DOCTYPE x [<!ENTITY xxe SYSTEM 'file:///does-not-exist'>]><x>&xxe;</x>";
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.docx",zip("word/document.xml",evil.getBytes(StandardCharsets.UTF_8))));
        // A non-body entry is also bounded before ZipInputStream advances to the next entry.
        assertThrows(IllegalArgumentException.class,()->parser.parse("a.docx",zip("word/media/large.bin",new byte[8_000_001])));
    }
    private byte[] zip(String name, byte[] bytes) throws Exception {
        var output = new ByteArrayOutputStream();
        try(var zip = new ZipOutputStream(output)) { zip.putNextEntry(new ZipEntry(name)); zip.write(bytes); zip.closeEntry(); }
        return output.toByteArray();
    }
}
