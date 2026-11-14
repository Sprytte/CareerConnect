package com.example.careercontactapi.ResumeFeedbackManagement.businesslayer;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads the visible text of a DOCX file using only the JDK. A DOCX file is a
 * zip archive whose main text lives in word/document.xml.
 */
final class DocxTextExtractor {

    // Uploaded files are untrusted: cap how much is unpacked and returned.
    static final int MAX_DOCUMENT_XML_BYTES = 10 * 1024 * 1024;
    static final int MAX_TEXT_CHARS = 40_000;

    private static final String MAIN_DOCUMENT = "word/document.xml";

    private DocxTextExtractor() {
    }

    static String extract(byte[] docx) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (MAIN_DOCUMENT.equals(entry.getName())) {
                    return readText(readBounded(zip));
                }
            }
        }
        throw new IOException("Not a DOCX file: " + MAIN_DOCUMENT + " is missing");
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            if (output.size() + read > MAX_DOCUMENT_XML_BYTES) {
                throw new IOException("The DOCX document is too large to read");
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static String readText(byte[] documentXml) throws IOException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // No DTDs or external entities from an uploaded file.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

        StringBuilder text = new StringBuilder();
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(new ByteArrayInputStream(documentXml));
            boolean insideText = false;
            while (reader.hasNext() && text.length() < MAX_TEXT_CHARS) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "t" -> insideText = true;
                        case "tab" -> text.append('\t');
                        case "br", "cr" -> text.append('\n');
                        default -> { }
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    switch (reader.getLocalName()) {
                        case "t" -> insideText = false;
                        case "p" -> text.append('\n');
                        default -> { }
                    }
                } else if (insideText
                        && (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA)) {
                    text.append(reader.getText());
                }
            }
            reader.close();
        } catch (XMLStreamException e) {
            throw new IOException("The DOCX document could not be read", e);
        }

        if (text.length() > MAX_TEXT_CHARS) {
            text.setLength(MAX_TEXT_CHARS);
        }
        return text.toString().strip();
    }
}
