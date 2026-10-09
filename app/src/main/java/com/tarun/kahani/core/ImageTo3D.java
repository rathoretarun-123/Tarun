package com.tarun.kahani.core;

import java.io.IOException;
import java.util.Map;

/**
 * An image-to-3D model service, the way the plain-English guide starts: the cleaned (cut-out) picture and a
 * short prompt go to the service, a textured 3D model (GLB) comes back a minute or two later, and the studio
 * renders that model from every side. The service needs the user's own key (entered in Settings, kept on the
 * phone only — the app's code is public). Without a key, the studio's own figure model does the work.
 *
 * The request and reply follow Meshy's public image-to-3D API (openapi/v1/image-to-3d): a data-URI picture,
 * a task id back, the task polled until SUCCEEDED, then model_urls.glb. Field names of other services differ.
 */
public final class ImageTo3D {
    private ImageTo3D() {}

    public interface Progress { void at(String what); }

    public static final String MESHY = "https://api.meshy.ai/openapi/v1/image-to-3d";
    /** How long to wait for a model at most (ms) and how often to ask (ms). */
    public static int WAIT_MS = 12 * 60 * 1000, POLL_MS = 6000;

    /** The service's reply: the model and where it came from. */
    public static final class Result {
        public byte[] glb;
        public String taskId = "", thumbnailUrl = "", note = "";
    }

    /** The prompt the picture is sent with: style and material from the description; the picture rules the shape (75 %). */
    public static String prompt(String description) {
        String d = description == null ? "" : description.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (d.length() > 300) d = d.substring(0, 300);
        return "A stylised 3D animated-film character, full body, standing in an A-pose, clean topology, PBR texture, " + d;
    }

    /** The request body for Meshy (public, so a test can check it without a key). */
    public static Map<String, Object> meshyBody(byte[] png, String description) {
        return Cloud.map("image_url", "data:image/png;base64," + base64(png), "enable_pbr", true, "should_remesh", true, "should_texture", true,
                "topology", "triangle", "target_polycount", 30000, "texture_prompt", prompt(description));
    }

    /** Sends the cut-out picture to Meshy and waits for the GLB. */
    public static Result meshy(Cloud cloud, String key, byte[] png, String description, Progress p) throws IOException {
        if (key == null || key.trim().length() < 8) throw new IOException("No image-to-3D key");
        String[] auth = {"Authorization", "Bearer " + key.trim()};
        if (p != null) p.at("Sending the picture to the 3D model service…");
        byte[] reply = cloud.request("POST", MESHY, "application/json", Json.write(meshyBody(png, description)).getBytes("UTF-8"), auth);
        Object r = Json.parse(new String(reply, "UTF-8"));
        String id = Json.str(r, "result", Json.str(r, "id", Json.str(r, "task_id", "")));
        if (id.length() == 0) throw new IOException("The 3D service gave no task id: " + new String(reply, "UTF-8"));
        Result out = new Result();
        out.taskId = id;
        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < WAIT_MS) {
            try { Thread.sleep(POLL_MS); } catch (InterruptedException e) { throw new IOException("Interrupted"); }
            byte[] st = cloud.request("GET", MESHY + "/" + id, null, null, auth);
            Object s = Json.parse(new String(st, "UTF-8"));
            String status = Json.str(s, "status", "");
            int progress = (int) Json.num(s, "progress", 0);
            if (p != null) p.at("The 3D model service is working… " + progress + "%");
            if (status.equalsIgnoreCase("SUCCEEDED")) {
                Map<String, Object> urls = Json.obj(s, "model_urls");
                String glbUrl = urls != null ? Json.str(urls, "glb", "") : Json.str(s, "model_url", "");
                if (glbUrl.length() == 0) throw new IOException("The 3D service finished without a GLB model");
                out.thumbnailUrl = Json.str(s, "thumbnail_url", "");
                if (p != null) p.at("Downloading the 3D model…");
                out.glb = cloud.download(glbUrl);
                out.note = "Meshy task " + id;
                return out;
            }
            if (status.equalsIgnoreCase("FAILED") || status.equalsIgnoreCase("CANCELED") || status.equalsIgnoreCase("EXPIRED")) {
                Map<String, Object> err = Json.obj(s, "task_error");
                throw new IOException("The 3D service failed: " + (err != null ? Json.str(err, "message", status) : status));
            }
        }
        throw new IOException("The 3D service took too long");
    }

    static String base64(byte[] data) {
        final String T = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuilder b = new StringBuilder((data.length + 2) / 3 * 4);
        for (int i = 0; i < data.length; i += 3) {
            int n = (data[i] & 255) << 16 | (i + 1 < data.length ? (data[i + 1] & 255) << 8 : 0) | (i + 2 < data.length ? data[i + 2] & 255 : 0);
            b.append(T.charAt(n >> 18 & 63)).append(T.charAt(n >> 12 & 63));
            b.append(i + 1 < data.length ? T.charAt(n >> 6 & 63) : '=');
            b.append(i + 2 < data.length ? T.charAt(n & 63) : '=');
        }
        return b.toString();
    }
}
