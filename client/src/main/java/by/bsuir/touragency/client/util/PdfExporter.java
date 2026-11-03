package by.bsuir.touragency.client.util;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

/**
 * Сохранение отчёта в PDF без сторонних библиотек: снимок экрана отчёта
 * разбивается на страницы A4 и вставляется в PDF как сжатые изображения.
 * Кириллица и графики сохраняются точно так же, как выглядят в программе.
 */
public final class PdfExporter {

    private static final double PAGE_W = 595.28;   // A4, пункты
    private static final double PAGE_H = 841.89;
    private static final double MARGIN = 28;
    private static final double SCALE = 2;          // чёткость снимка

    private PdfExporter() {
    }

    public static void write(Path file, Node node) throws IOException {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.WHITE);
        params.setTransform(Transform.scale(SCALE, SCALE));
        WritableImage image = node.snapshot(params, null);
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        double contentW = PAGE_W - 2 * MARGIN;
        double pxPerPt = w / contentW;
        int pageHeightPx = (int) Math.floor((PAGE_H - 2 * MARGIN) * pxPerPt);
        PixelReader reader = image.getPixelReader();

        List<byte[]> objects = new ArrayList<>();
        List<Integer> pageIds = new ArrayList<>();
        int pages = (h + pageHeightPx - 1) / pageHeightPx;
        // номера объектов: 1 — каталог, 2 — дерево страниц, далее по 3 на страницу
        for (int p = 0; p < pages; p++) {
            int y0 = p * pageHeightPx;
            int sliceH = Math.min(pageHeightPx, h - y0);
            byte[] rgb = new byte[w * sliceH * 3];
            int i = 0;
            for (int y = y0; y < y0 + sliceH; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = reader.getArgb(x, y);
                    rgb[i++] = (byte) ((argb >> 16) & 0xFF);
                    rgb[i++] = (byte) ((argb >> 8) & 0xFF);
                    rgb[i++] = (byte) (argb & 0xFF);
                }
            }
            byte[] compressed = deflate(rgb);
            int imageId = 3 + p * 3;
            int contentId = imageId + 1;
            int pageId = imageId + 2;
            pageIds.add(pageId);

            double drawH = sliceH / pxPerPt;
            String content = String.format(java.util.Locale.ROOT, "q %.2f 0 0 %.2f %.2f %.2f cm /Im%d Do Q",
                    contentW, drawH, MARGIN, PAGE_H - MARGIN - drawH, p);
            objects.add(concat(("<< /Type /XObject /Subtype /Image /Width " + w + " /Height " + sliceH
                    + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /FlateDecode /Length "
                    + compressed.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII), compressed,
                    "\nendstream".getBytes(StandardCharsets.US_ASCII)));
            objects.add(("<< /Length " + content.length() + " >>\nstream\n" + content + "\nendstream")
                    .getBytes(StandardCharsets.US_ASCII));
            objects.add(String.format(java.util.Locale.ROOT, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 %.2f %.2f] "
                            + "/Resources << /XObject << /Im%d %d 0 R >> >> /Contents %d 0 R >>",
                    PAGE_W, PAGE_H, p, imageId, contentId).getBytes(StandardCharsets.US_ASCII));
        }

        StringBuilder kids = new StringBuilder();
        pageIds.forEach(id -> kids.append(id).append(" 0 R "));
        List<byte[]> all = new ArrayList<>();
        all.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));
        all.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>").getBytes(StandardCharsets.US_ASCII));
        all.addAll(objects);

        try (OutputStream out = Files.newOutputStream(file)) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            buf.write("%PDF-1.4\n%âãÏÓ\n".getBytes(StandardCharsets.ISO_8859_1));
            List<Integer> offsets = new ArrayList<>();
            for (int n = 0; n < all.size(); n++) {
                offsets.add(buf.size());
                buf.write(((n + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
                buf.write(all.get(n));
                buf.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            }
            int xref = buf.size();
            StringBuilder sb = new StringBuilder("xref\n0 " + (all.size() + 1) + "\n0000000000 65535 f \n");
            for (int off : offsets) {
                sb.append(String.format("%010d 00000 n \n", off));
            }
            sb.append("trailer\n<< /Size ").append(all.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                    .append(xref).append("\n%%EOF\n");
            buf.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
            buf.writeTo(out);
        }
    }

    private static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream d = new DeflaterOutputStream(out, new Deflater(Deflater.BEST_COMPRESSION))) {
            d.write(data);
        }
        return out.toByteArray();
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) len += p.length;
        byte[] r = new byte[len];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, r, pos, p.length);
            pos += p.length;
        }
        return r;
    }
}
