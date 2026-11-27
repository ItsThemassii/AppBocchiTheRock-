import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Random;

/**
 * Herramientas para preparar contenido de la app (se ejecuta con: java BocchiTools.java ...).
 *
 *   java BocchiTools.java sticker <carpeta-con-png> <carpeta-salida>
 *       Quita el fondo claro de cada PNG (relleno desde los bordes) y añade un borde
 *       blanco + uno oscuro, estilo sticker recortado.
 *
 *   java BocchiTools.java sticker-main <carpeta-con-png> <carpeta-salida>
 *       Igual, pero conserva solo la pieza más grande (quita gotitas, adornos sueltos...).
 *
 *   java BocchiTools.java icon <imagen> <x> <y> <lado> <carpeta-res>
 *       Recorta un cuadrado (x, y, lado) en círculo con borde blanco + rosa + tinta y
 *       escribe ic_launcher_foreground.png en todas las densidades (mipmap-*dpi).
 *
 *   java BocchiTools.java sounds <carpeta-salida>
 *       Genera sonidos de púa de guitarra (síntesis Karplus-Strong) en WAV.
 */
public class BocchiTools {
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "sticker" -> stickerFolder(new File(args[1]), new File(args[2]));
            case "sticker-main" -> { mainOnly = true; stickerFolder(new File(args[1]), new File(args[2])); }
            case "sounds" -> sounds(new File(args[1]));
            case "icon" -> icon(new File(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), new File(args[5]));
            default -> throw new IllegalArgumentException(args[0]);
        }
    }

    // ------------------------------------------------------------------ stickers

    static final int WHITE_BORDER = 7;
    static final int INK_BORDER = 3;

    static void stickerFolder(File in, File out) throws IOException {
        out.mkdirs();
        File[] files = in.listFiles((d, n) -> n.endsWith(".png"));
        java.util.Arrays.sort(files);
        for (File f : files) ImageIO.write(sticker(ImageIO.read(f)), "png", new File(out, f.getName()));
        System.out.println("stickers: " + files.length);
    }

    static BufferedImage sticker(BufferedImage src) {
        int pad = WHITE_BORDER + INK_BORDER + 2;
        int w = src.getWidth(), h = src.getHeight();
        int W = w + pad * 2, H = h + pad * 2;
        boolean[] bg = new boolean[w * h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < w; x++) { q.add(new int[]{x, 0}); q.add(new int[]{x, h - 1}); }
        for (int y = 0; y < h; y++) { q.add(new int[]{0, y}); q.add(new int[]{w - 1, y}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || bg[y * w + x]) continue;
            if (!isBackground(src.getRGB(x, y))) continue;
            bg[y * w + x] = true;
            q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1});
        }
        if (mainOnly) keepLargestPiece(bg, w, h);
        // Máscara del personaje en el lienzo con margen.
        boolean[] fg = new boolean[W * H];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (!bg[y * w + x]) fg[(y + pad) * W + x + pad] = true;
        int[] dist = distance(fg, W, H, WHITE_BORDER + INK_BORDER + 1);

        BufferedImage out = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        int ink = 0xFF1C1A2E;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x;
            if (fg[i]) {
                out.setRGB(x, y, 0xFF000000 | src.getRGB(x - pad, y - pad));
            } else {
                int d = dist[i];
                if (d <= WHITE_BORDER) out.setRGB(x, y, 0xFFFFFFFF);
                else if (d <= WHITE_BORDER + INK_BORDER) out.setRGB(x, y, ink);
            }
        }
        return trimTransparent(out);
    }

    /** Recorta los bordes totalmente transparentes. */
    static BufferedImage trimTransparent(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
            if ((img.getRGB(x, y) >>> 24) == 0) continue;
            minX = Math.min(minX, x); minY = Math.min(minY, y); maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
        }
        if (maxX < 0) return img;
        return img.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    static boolean mainOnly = false;

    /** Marca como fondo todo lo que no pertenezca a la pieza (zona conectada) más grande. */
    static void keepLargestPiece(boolean[] bg, int w, int h) {
        int[] label = new int[w * h];
        int best = -1, bestSize = 0, next = 1;
        for (int start = 0; start < w * h; start++) {
            if (bg[start] || label[start] != 0) continue;
            int size = 0;
            ArrayDeque<Integer> q = new ArrayDeque<>();
            q.add(start);
            label[start] = next;
            while (!q.isEmpty()) {
                int i = q.poll();
                size++;
                int x = i % w, y = i / w;
                int[] nb = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
                for (int n : nb) if (n >= 0 && !bg[n] && label[n] == 0) { label[n] = next; q.add(n); }
            }
            if (size > bestSize) { bestSize = size; best = next; }
            next++;
        }
        for (int i = 0; i < w * h; i++) if (!bg[i] && label[i] != best) bg[i] = true;
    }

    /** Fondo: casi blanco o gris muy claro y poco saturado (también borra marcas de agua tenues). */
    static boolean isBackground(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        return min > 215 && max - min < 22;
    }

    /** Distancia (aprox. euclídea, en px) desde cada píxel al personaje, hasta [limit]. */
    static int[] distance(boolean[] fg, int W, int H, int limit) {
        int[] d = new int[W * H];
        java.util.Arrays.fill(d, Integer.MAX_VALUE);
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            if (!fg[y * W + x]) continue;
            // Solo los píxeles del contorno propagan.
            boolean edge = false;
            for (int k = 0; k < 4 && !edge; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || ny < 0 || nx >= W || ny >= H || !fg[ny * W + nx]) edge = true;
            }
            if (!edge) continue;
            for (int dy = -limit; dy <= limit; dy++) for (int dx = -limit; dx <= limit; dx++) {
                int nx = x + dx, ny = y + dy;
                if (nx < 0 || ny < 0 || nx >= W || ny >= H) continue;
                int dd = (int) Math.round(Math.sqrt(dx * dx + dy * dy));
                if (dd <= limit && dd < d[ny * W + nx]) d[ny * W + nx] = dd;
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ ícono

    /** Capa frontal del ícono adaptativo (108 dp; lo visible es el círculo central de ~72 dp). */
    static void icon(File src, int x, int y, int side, File res) throws IOException {
        BufferedImage face = ImageIO.read(src).getSubimage(x, y, side, side);
        String[] dirs = {"mipmap-mdpi", "mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi", "mipmap-xxxhdpi"};
        int[] sizes = {108, 162, 216, 324, 432};
        for (int i = 0; i < dirs.length; i++) {
            int s = sizes[i];
            BufferedImage out = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);
            double c = s / 2.0;
            // Anillos de fuera hacia dentro: tinta, rosa, blanco y la foto.
            double rInk = s * 0.318, rPink = s * 0.308, rWhite = s * 0.285, rPhoto = s * 0.268;
            g.setColor(new java.awt.Color(0x1C1A2E));
            g.fill(new java.awt.geom.Ellipse2D.Double(c - rInk, c - rInk, rInk * 2, rInk * 2));
            g.setColor(new java.awt.Color(0xE5578C));
            g.fill(new java.awt.geom.Ellipse2D.Double(c - rPink, c - rPink, rPink * 2, rPink * 2));
            g.setColor(java.awt.Color.WHITE);
            g.fill(new java.awt.geom.Ellipse2D.Double(c - rWhite, c - rWhite, rWhite * 2, rWhite * 2));
            java.awt.Shape photo = new java.awt.geom.Ellipse2D.Double(c - rPhoto, c - rPhoto, rPhoto * 2, rPhoto * 2);
            g.setClip(photo);
            g.drawImage(face, (int) Math.round(c - rPhoto), (int) Math.round(c - rPhoto), (int) Math.round(rPhoto * 2), (int) Math.round(rPhoto * 2), null);
            g.dispose();
            File dir = new File(res, dirs[i]);
            dir.mkdirs();
            ImageIO.write(out, "png", new File(dir, "ic_launcher_foreground.png"));
        }
        System.out.println("ícono listo");
    }

    // ------------------------------------------------------------------ sonidos

    static final int RATE = 44100;

    static void sounds(File out) throws IOException {
        out.mkdirs();
        // Escala pentatónica de Mi menor: suena "rock" y combina siempre.
        double[] notes = {329.63, 392.00, 440.00, 493.88, 587.33};
        for (int i = 0; i < notes.length; i++) {
            float[] s = pluck(notes[i], 0.55, 0.9955, 7 + i);
            writeWav(new File(out, "ui_tap" + (i + 1) + ".wav"), normalize(s, 0.8f));
        }
        // Volver: nota grave apagada (palm mute).
        writeWav(new File(out, "ui_back.wav"), normalize(pluck(164.81, 0.25, 0.975, 3), 0.75f));
        // Acción grande (jugar): power chord rasgueado Mi5 (Mi-Si-Mi).
        float[] chord = new float[(int) (RATE * 0.9)];
        double[] chordNotes = {82.41, 123.47, 164.81, 246.94};
        for (int i = 0; i < chordNotes.length; i++) {
            float[] s = pluck(chordNotes[i], 0.9, 0.9965, 20 + i);
            int offset = (int) (RATE * 0.012 * i);
            for (int j = 0; j < s.length && j + offset < chord.length; j++) chord[j + offset] += s[j];
        }
        writeWav(new File(out, "ui_chord.wav"), normalize(overdrive(chord, 2.2f), 0.8f));
        System.out.println("sonidos listos");
    }

    /** Cuerda pulsada (Karplus-Strong) con un poco de "cuerpo" para que suene a guitarra eléctrica limpia. */
    static float[] pluck(double freq, double seconds, double decay, long seed) {
        int n = (int) (RATE * seconds);
        int period = (int) Math.round(RATE / freq);
        float[] buf = new float[period];
        Random r = new Random(seed);
        for (int i = 0; i < period; i++) buf[i] = r.nextFloat() * 2 - 1;
        // Suaviza el ruido inicial (púa algo redonda, menos chillona).
        for (int k = 0; k < 2; k++) for (int i = 1; i < period; i++) buf[i] = 0.5f * (buf[i] + buf[i - 1]);
        float[] out = new float[n];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            int next = (idx + 1) % period;
            float v = (float) (decay * 0.5 * (buf[idx] + buf[next]));
            out[i] = buf[idx];
            buf[idx] = v;
            idx = next;
        }
        // Ataque rápido y caída suave al final para que no haga clic.
        int fade = Math.min(n / 4, RATE / 20);
        for (int i = 0; i < fade; i++) out[n - 1 - i] *= i / (float) fade;
        for (int i = 0; i < 40; i++) out[i] *= i / 40f;
        return out;
    }

    static float[] overdrive(float[] s, float gain) {
        float[] o = new float[s.length];
        for (int i = 0; i < s.length; i++) o[i] = (float) Math.tanh(s[i] * gain);
        return o;
    }

    static float[] normalize(float[] s, float peak) {
        float max = 1e-6f;
        for (float v : s) max = Math.max(max, Math.abs(v));
        float[] o = new float[s.length];
        for (int i = 0; i < s.length; i++) o[i] = s[i] / max * peak;
        return o;
    }

    static void writeWav(File f, float[] s) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(44 + s.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(36 + s.length * 2).put("WAVE".getBytes());
        b.put("fmt ".getBytes()).putInt(16).putShort((short) 1).putShort((short) 1).putInt(RATE).putInt(RATE * 2).putShort((short) 2).putShort((short) 16);
        b.put("data".getBytes()).putInt(s.length * 2);
        for (float v : s) b.putShort((short) Math.max(-32768, Math.min(32767, v * 32767)));
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(b.array()); }
    }
}
