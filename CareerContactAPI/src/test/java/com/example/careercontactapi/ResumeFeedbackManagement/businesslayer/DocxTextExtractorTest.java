package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxTextExtractorTest {

    private static final String NAMESPACE =
            "xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"";

    static byte[] docx(String entryName, String xml) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write("<Types/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(xml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }

    static byte[] docxWithBody(String body) throws IOException {
        return docx("word/document.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document " + NAMESPACE + "><w:body>"
                        + body + "</w:body></w:document>");
    }

    @Test
    void readsParagraphsRunsTabsAndLineBreaks() throws IOException {
        byte[] file = docxWithBody(
                "<w:p><w:r><w:t>Arley Ndaribike</w:t></w:r></w:p>"
                        + "<w:p><w:r><w:t xml:space=\"preserve\">Skills: </w:t></w:r><w:r><w:t>React &amp; Java</w:t></w:r></w:p>"
                        + "<w:p><w:r><w:t>2024</w:t><w:tab/><w:t>Intern</w:t><w:br/><w:t>Montreal</w:t></w:r></w:p>");

        assertEquals("Arley Ndaribike\nSkills: React & Java\n2024\tIntern\nMontreal",
                DocxTextExtractor.extract(file));
    }

    @Test
    void ignoresMarkupThatIsNotVisibleText() throws IOException {
        byte[] file = docxWithBody(
                "<w:p><w:pPr><w:pStyle w:val=\"Heading1\"/></w:pPr><w:r><w:rPr><w:b/></w:rPr><w:t>Experience</w:t></w:r></w:p>");

        assertEquals("Experience", DocxTextExtractor.extract(file));
    }

    @Test
    void returnsEmptyTextForADocumentWithoutText() throws IOException {
        assertEquals("", DocxTextExtractor.extract(docxWithBody("<w:p/>")));
    }

    @Test
    void rejectsFilesThatAreNotDocx() throws IOException {
        assertThrows(IOException.class,
                () -> DocxTextExtractor.extract("just some text".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IOException.class,
                () -> DocxTextExtractor.extract(docx("xl/workbook.xml", "<workbook/>")));
        assertThrows(IOException.class,
                () -> DocxTextExtractor.extract(docx("word/document.xml", "<w:document><w:body>")));
    }

    @Test
    void doesNotResolveEntitiesDeclaredInTheFile() throws IOException {
        Path secret = Files.createTempFile("resume-feedback-entity", ".txt");
        try {
            Files.writeString(secret, "TOP-SECRET-MARKER");
            byte[] file = docx("word/document.xml",
                    "<?xml version=\"1.0\"?><!DOCTYPE d [<!ENTITY leak SYSTEM \"" + secret.toUri() + "\">]>"
                            + "<w:document " + NAMESPACE + "><w:body><w:p><w:r><w:t>Before &leak; after</w:t></w:r></w:p>"
                            + "</w:body></w:document>");

            String text;
            try {
                text = DocxTextExtractor.extract(file);
            } catch (IOException rejected) {
                return; // Refusing the document is the expected outcome.
            }
            assertFalse(text.contains("TOP-SECRET-MARKER"));
        } finally {
            Files.deleteIfExists(secret);
        }
    }

    @Test
    void capsTheAmountOfTextReturned() throws IOException {
        String run = "<w:p><w:r><w:t>" + "a".repeat(1000) + "</w:t></w:r></w:p>";
        byte[] file = docxWithBody(run.repeat(60));

        assertTrue(DocxTextExtractor.extract(file).length() <= DocxTextExtractor.MAX_TEXT_CHARS);
    }
}
