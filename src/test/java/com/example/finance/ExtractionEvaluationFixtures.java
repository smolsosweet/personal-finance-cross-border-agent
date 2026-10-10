package com.example.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Test-only deterministic synthetic documents, rendered without additional dependencies. */
final class ExtractionEvaluationFixtures {
    static final String RESOURCE = "/gemini-extraction-eval/ground-truth.json";
    record Fixture(String id, String kind, String category, String expectedBehavior, String render,
                   List<String> lines, Map<String,String> expected, Map<String,String> attackValues) {}

    static List<Fixture> load() throws IOException {
        try (InputStream input = ExtractionEvaluationFixtures.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IOException("Evaluation ground truth missing");
            JsonNode root = new ObjectMapper().readTree(input);
            if (!root.path("syntheticOnly").asBoolean()) throw new IOException("Synthetic fixtures required");
            List<Fixture> result = new ArrayList<>();
            Set<String> ids = new HashSet<>();
            for (JsonNode node : root.path("fixtures")) {
                String id = node.path("id").asText();
                if (!id.matches("(?:receipt|bill)-[0-9]{2}") || !ids.add(id))
                    throw new IOException("Invalid or duplicate fixture id");
                List<String> lines = new ArrayList<>();
                node.path("lines").forEach(line -> lines.add(line.asText()));
                result.add(new Fixture(id, node.path("kind").asText(), node.path("category").asText(),
                        node.path("expected_behavior").asText(), node.path("render").asText(),
                        List.copyOf(lines), fields(node.path("expected")), fields(node.path("attack_values"))));
            }
            return List.copyOf(result);
        }
    }

    private static Map<String,String> fields(JsonNode node) {
        Map<String,String> result = new LinkedHashMap<>();
        node.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().isNull() ? null : e.getValue().asText()));
        return Collections.unmodifiableMap(result);
    }

    static byte[] committedImage(Fixture fixture) throws IOException {
        try (InputStream input = ExtractionEvaluationFixtures.class.getResourceAsStream(
                "/gemini-extraction-eval/images/" + fixture.id() + ".png")) {
            if (input == null) throw new IOException("Committed evaluation image missing: " + fixture.id());
            return input.readAllBytes();
        }
    }

    static String fontName() {
        Set<String> installed = Set.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String candidate : List.of("Microsoft YaHei", "Microsoft YaHei UI", "Noto Sans CJK SC", "WenQuanYi Zen Hei", "Dialog"))
            if (installed.contains(candidate)) return candidate;
        return "Dialog";
    }

    static Font lineFont(String text) throws IOException {
        for (String candidate : List.of(fontName(),"Segoe UI","Arial","Dialog")) {
            Font font=new Font(candidate,Font.PLAIN,28);
            if(font.canDisplayUpTo(text)==-1) return font;
        }
        throw new IOException("Missing font glyphs for synthetic fixture text");
    }

    static byte[] render(Fixture fixture) throws IOException {
        int width = 900, height = 1100;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0, 0, width, height);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if ("non-document".equals(fixture.render())) {
            g.setColor(new Color(219,236,249)); g.fillRect(0,0,width,height);
            g.setColor(new Color(244,195,98)); g.fillOval(570,120,150,150);
            g.setColor(new Color(111,174,152)); g.fillPolygon(new int[]{0,300,600},new int[]{830,340,830},3);
            g.setColor(new Color(80,126,170)); g.fillPolygon(new int[]{330,640,900},new int[]{850,450,850},3);
            g.setColor(new Color(99,158,205)); g.fillRect(0,850,width,250);
        } else {
            if ("rotated".equals(fixture.render())) g.rotate(Math.toRadians(6),width/2.0,height/2.0);
            if ("skewed".equals(fixture.render())) g.transform(new AffineTransform(1,0.03,0.10,1,-45,0));
            g.setColor(new Color(235,238,242)); g.fillRoundRect(48,48,width-96,height-96,18,18);
            g.setColor(new Color(25,37,46));
            int y = 120;
            for (String line : fixture.lines()) {
                g.setFont(lineFont(line));
                if (g.getFontMetrics().stringWidth(line) > width-132)
                    throw new IOException("Fixture template line would be clipped: " + fixture.id());
                g.drawString(line,66,y); y += 72;
            }
        }
        g.dispose();
        if ("low-resolution".equals(fixture.render())) {
            BufferedImage small = new BufferedImage(225,275,BufferedImage.TYPE_INT_RGB);
            Graphics2D sg = small.createGraphics();
            sg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            sg.drawImage(image,0,0,225,275,null); sg.dispose(); image = small;
        } else if ("blurred".equals(fixture.render())) {
            float[] kernel = new float[25]; Arrays.fill(kernel,1f/25);
            image = new ConvolveOp(new Kernel(5,5,kernel),ConvolveOp.EDGE_NO_OP,null).filter(image,null);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(image,"png",output)) throw new IOException("PNG encoder unavailable");
        return output.toByteArray();
    }

    static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    /** Explicit fixture maintenance only; evaluation itself uses committed PNG bytes. */
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Specify the fixture image output directory");
        Path destination = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectories(destination);
        for (Fixture fixture : load()) Files.write(destination.resolve(fixture.id()+".png"),render(fixture));
        System.out.println("Rendered " + load().size() + " synthetic documents using font " + fontName());
    }
}
