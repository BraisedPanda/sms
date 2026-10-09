package com.xqy.sms.knowledge.provider.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipInputStream;

@Component
public class DocumentTextParser {
    public static final int MAX_UPLOAD_BYTES = 4 * 1024 * 1024;
    private static final int MAX_TEXT_CHARACTERS = 2_000_000;

    public String parse(String filename, byte[] bytes) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("文件大小须为 1 字节到 4MB");
        String extension = extension(filename);
        try {
            String text = switch (extension) {
                case "txt", "md", "csv", "json" -> utf8(bytes);
                case "html", "htm" -> utf8(bytes).replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1>", "")
                        .replaceAll("(?i)<(?:br|/p|/div|/h[1-6])\\s*/?>", "\n").replaceAll("(?s)<[^>]*>", " ")
                        .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
                case "docx" -> docx(bytes);
                case "pdf" -> pdf(bytes);
                default -> throw new IllegalArgumentException("支持 TXT、Markdown、CSV、JSON、HTML、DOCX 和文字型 PDF");
            };
            text = text.replace("\u0000", "").replace("\r\n", "\n").replace('\r', '\n').strip();
            if (text.isBlank()) throw new IllegalArgumentException("文件没有可提取的文本；扫描 PDF 需要先进行 OCR");
            if (text.length() > MAX_TEXT_CHARACTERS) throw new IllegalArgumentException("提取文本超过 200 万字符");
            return text;
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalArgumentException("文件解析失败，请检查格式或编码", error); }
    }

    public String extension(String filename) {
        if (filename == null || filename.isBlank() || filename.length() > 255) throw new IllegalArgumentException("无效文件名");
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("文件必须有扩展名");
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String utf8(byte[] bytes) throws Exception {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().replace("\uFEFF", "");
    }
    private String pdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() > 1000) throw new IllegalArgumentException("PDF 页数超过 1000");
            var output = new java.io.Writer() {
                final StringBuilder text = new StringBuilder();
                public void write(char[] chars, int start, int length) {
                    if (text.length() + length > MAX_TEXT_CHARACTERS) throw new IllegalArgumentException("提取文本超过 200 万字符");
                    text.append(chars, start, length);
                }
                public void flush() { }
                public void close() { }
                public String toString() { return text.toString(); }
            };
            new PDFTextStripper().writeText(document, output);
            return output.toString();
        }
    }
    private String docx(byte[] bytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            int entries = 0, expanded = 0;
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (++entries > 1000) throw new IllegalArgumentException("DOCX 文件条目过多");
                byte[] xml = zip.readNBytes(MAX_TEXT_CHARACTERS * 4 + 1);
                expanded += xml.length;
                if (xml.length > MAX_TEXT_CHARACTERS * 4 || expanded > 32 * 1024 * 1024) throw new IllegalArgumentException("DOCX 解压内容过大");
                if (!"word/document.xml".equals(entry.getName())) continue;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
                NodeList paragraphs = document.getElementsByTagNameNS("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "p");
                StringBuilder text = new StringBuilder();
                for (int i = 0; i < paragraphs.getLength(); i++) {
                    text.append(paragraphs.item(i).getTextContent()).append('\n');
                    if (text.length() > MAX_TEXT_CHARACTERS) throw new IllegalArgumentException("提取文本超过 200 万字符");
                }
                return text.toString();
            }
        }
        throw new IllegalArgumentException("DOCX 缺少正文");
    }
}
