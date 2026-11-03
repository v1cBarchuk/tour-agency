package by.bsuir.touragency.client.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Запись отчёта в файл Excel (.xlsx) без сторонних библиотек:
 * .xlsx — это ZIP-архив с XML-файлами формата Office Open XML.
 */
public final class XlsxExporter {

    /** Лист: название, заголовки столбцов и строки (String или Number). */
    public record Sheet(String name, List<String> headers, List<List<Object>> rows) {
    }

    private XlsxExporter() {
    }

    public static void write(Path file, List<Sheet> sheets) throws IOException {
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            entry(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            entry(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                    + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/"
                    + "relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            entry(zip, "xl/workbook.xml", workbook(sheets));
            entry(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
            entry(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < sheets.size(); i++) {
                entry(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(sheets.get(i)));
            }
        }
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes(int sheets) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-"
                + "officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-"
                + "officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= sheets; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i).append(".xml\" ContentType=\"application/"
                    + "vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private static String workbook(List<Sheet> sheets) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < sheets.size(); i++) {
            String name = sheets.get(i).name().replaceAll("[\\\\/?*\\[\\]:]", " ");
            sb.append("<sheet name=\"").append(esc(name.length() > 31 ? name.substring(0, 31) : name))
                    .append("\" sheetId=\"").append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int sheets) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= sheets; i++) {
            sb.append("<Relationship Id=\"rId").append(i).append("\" Type=\"http://schemas.openxmlformats.org/"
                    + "officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(sheets + 1).append("\" Type=\"http://schemas.openxmlformats.org/"
                + "officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<numFmts count=\"1\"><numFmt numFmtId=\"164\" formatCode=\"#,##0.00\"/></numFmts>"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0B5C73\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border/></borders><cellStyleXfs count=\"1\"><xf/></cellStyleXfs>"
            + "<cellXfs count=\"3\"><xf/><xf fontId=\"1\" fillId=\"2\" applyFont=\"1\" applyFill=\"1\"/>"
            + "<xf numFmtId=\"164\" applyNumberFormat=\"1\"/></cellXfs></styleSheet>";

    private static String sheet(Sheet s) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><cols>");
        for (int c = 0; c < s.headers().size(); c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(c == 0 ? 34 : 18).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\">");
        for (int c = 0; c < s.headers().size(); c++) {
            sb.append(cell(c, 1, s.headers().get(c), 1));
        }
        sb.append("</row>");
        int r = 2;
        for (List<Object> row : s.rows()) {
            sb.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < row.size(); c++) {
                sb.append(cell(c, r, row.get(c), 0));
            }
            sb.append("</row>");
            r++;
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static String cell(int col, int row, Object value, int style) {
        String ref = column(col) + row;
        if (value instanceof Number n) {
            int st = style != 0 ? style : (n.doubleValue() == Math.rint(n.doubleValue()) ? 0 : 2);
            return "<c r=\"" + ref + "\"" + (st != 0 ? " s=\"" + st + "\"" : "") + "><v>" + n + "</v></c>";
        }
        return "<c r=\"" + ref + "\" t=\"inlineStr\"" + (style != 0 ? " s=\"" + style + "\"" : "")
                + "><is><t>" + esc(String.valueOf(value)) + "</t></is></c>";
    }

    private static String column(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return sb.toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
