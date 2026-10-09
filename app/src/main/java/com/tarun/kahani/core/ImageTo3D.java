package com.tarun.kahani.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Image-to-3D model services, the way the plain-English guide starts: the cleaned (cut-out) picture and a short
 * prompt (the description) go to a service, a textured 3D model (GLB) comes back a minute or more later, and the
 * studio renders that model from every side.
 *
 * Two kinds of service:
 * <ul>
 * <li>a paid one with the user's own key (Meshy's public image-to-3D API: a data-URI picture, a task id, the task
 *     polled until SUCCEEDED, then model_urls.glb);</li>
 * <li>the free demos of open-source models on Hugging Face Spaces (TripoSR — MIT, InstantMesh — Apache-2.0,
 *     Hunyuan3D-2 — community licence), reached through Gradio's HTTP API without any key: the picture is
 *     uploaded, each step of the demo is called by name, the result is read from the event stream, the GLB is
 *     downloaded. Free demos queue, sleep and change; a failure or a long wait falls back to the studio's own
 *     figure model, and the service is not tried again in the same film. The steps are the ones in each
 *     project's own demo app on GitHub.</li>
 * </ul>
 */
public final class ImageTo3D {
    private ImageTo3D() {}

    public interface Progress { void at(String what); }

    public static final String MESHY = "https://api.meshy.ai/openapi/v1/image-to-3d";
    /** How long to wait for a paid model at most (ms) and how often to ask (ms). */
    public static int WAIT_MS = 12 * 60 * 1000, POLL_MS = 6000;
    /** How long one free Space may take for one picture (ms), all its steps together. */
    public static int SPACE_WAIT_MS = 5 * 60 * 1000;

    /** The service's reply: the model and where it came from. */
    public static final class Result {
        public byte[] glb;
        public String taskId = "", thumbnailUrl = "", note = "", credit = "";
    }

    /** The prompt the picture is sent with: style and material from the description; the picture rules the shape (75 %). */
    public static String prompt(String description) {
        String d = description == null ? "" : description.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (d.length() > 300) d = d.substring(0, 300);
        return "A stylised 3D animated-film character, full body, standing in an A-pose, clean topology, PBR texture, " + d;
    }

    // ------------------------------------------------------------------ Meshy (the user's key)

    /** The request body for Meshy (public, so a test can check it without a key). */
    public static Map<String, Object> meshyBody(byte[] png, String description) {
        return Cloud.map("image_url", "data:image/png;base64," + Base64.encode(png), "enable_pbr", true, "should_remesh", true, "should_texture", true,
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
                out.credit = "Meshy image-to-3D (the user's own key)";
                return out;
            }
            if (status.equalsIgnoreCase("FAILED") || status.equalsIgnoreCase("CANCELED") || status.equalsIgnoreCase("EXPIRED")) {
                Map<String, Object> err = Json.obj(s, "task_error");
                throw new IOException("The 3D service failed: " + (err != null ? Json.str(err, "message", status) : status));
            }
        }
        throw new IOException("The 3D service took too long");
    }

    // ------------------------------------------------------------------ free Spaces (no key)

    /** A free demo of an open-source image-to-3D model, as a Gradio app on Hugging Face Spaces. */
    public static final class Space {
        public final String name, base, licence, source;
        /** The api names of the demo's steps, in order (from the project's own demo app). */
        public final String[] steps;
        Space(String name, String base, String licence, String source, String... steps) { this.name = name; this.base = base; this.licence = licence; this.source = source; this.steps = steps; }
    }

    /** The free demos, in the order they are tried. */
    public static final Space[] SPACES = {
        new Space("TripoSR", "https://stabilityai-triposr.hf.space", "MIT (code and weights: Tripo AI & Stability AI)", "github.com/VAST-AI-Research/TripoSR", "preprocess", "generate"),
        new Space("InstantMesh", "https://tencentarc-instantmesh.hf.space", "Apache-2.0 (Tencent ARC)", "github.com/TencentARC/InstantMesh", "preprocess", "generate_mvs", "make3d"),
        new Space("Hunyuan3D-2", "https://tencent-hunyuan3d-2.hf.space", "Tencent Hunyuan 3D 2.0 Community License", "github.com/Tencent-Hunyuan/Hunyuan3D-2", "shape_generation"),
    };

    /** Spaces that failed in this run are not asked again (a demo asleep or over its free quota wastes minutes). */
    private static final java.util.Set<String> down = new java.util.HashSet<String>();
    public static void forgetFailures() { synchronized (down) { down.clear(); } }

    /** Tries the free Spaces in turn and returns the first model; throws when none worked. */
    public static Result freeSpaces(Cloud cloud, byte[] png, String description, Progress p) throws IOException {
        IOException last = null;
        for (Space s : SPACES) {
            synchronized (down) { if (down.contains(s.base)) continue; }
            try {
                return space(cloud, s, png, description, p);
            } catch (IOException e) {
                last = e;
                synchronized (down) { down.add(s.base); }
                if (p != null) p.at(s.name + " did not answer (" + shortMsg(e) + ")");
            }
        }
        throw last != null ? last : new IOException("No free 3D service is available");
    }

    private static String shortMsg(Exception e) { String m = e.getMessage() == null ? e.toString() : e.getMessage(); return m.length() > 80 ? m.substring(0, 80) : m; }

    /** Runs one Space's steps on the picture and downloads its GLB. */
    public static Result space(Cloud cloud, Space s, byte[] png, String description, Progress p) throws IOException {
        long t0 = System.currentTimeMillis();
        int savedTimeout = cloud.timeoutMs;
        cloud.timeoutMs = SPACE_WAIT_MS;
        try {
            if (p != null) p.at("Free 3D service " + s.name + ": uploading the picture…");
            Object info = null;
            try { info = Json.parseLoose(new String(cloud.request("GET", s.base + "/gradio_api/info", null, null), "UTF-8")); } catch (IOException ignored) {}
            Object file = Cloud.map("path", upload(cloud, s.base, png), "orig_name", "picture.png", "meta", Cloud.map("_type", "gradio.FileData"));
            Object current = file;
            List<Object> outputs = null;
            for (String step : s.steps) {
                if (System.currentTimeMillis() - t0 > SPACE_WAIT_MS) throw new IOException("too slow");
                if (p != null) p.at("Free 3D service " + s.name + ": " + step + "…");
                List<Object> data = stepInputs(info, step, current, description);
                outputs = call(cloud, s.base, step, data, t0);
                Object next = firstFile(outputs);
                if (next != null) current = next;
            }
            if (outputs == null) throw new IOException("no steps");
            String url = glbUrl(outputs, s.base);
            if (url == null) throw new IOException("no GLB in the reply");
            if (p != null) p.at("Free 3D service " + s.name + ": downloading the model…");
            Result out = new Result();
            out.glb = cloud.download(url);
            out.note = s.name + " (free demo on Hugging Face Spaces)";
            out.credit = s.name + " — " + s.licence + " — " + s.source;
            return out;
        } finally {
            cloud.timeoutMs = savedTimeout;
        }
    }

    /** Uploads the picture (multipart) and returns the server path Gradio gives it. */
    static String upload(Cloud cloud, String base, byte[] png) throws IOException {
        String boundary = "----KahaniFilm" + Long.toHexString(System.nanoTime());
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\"picture.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes("UTF-8"));
        body.write(png);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
        byte[] reply = cloud.request("POST", base + "/gradio_api/upload", "multipart/form-data; boundary=" + boundary, body.toByteArray());
        Object r = Json.parseLoose(new String(reply, "UTF-8"));
        if (r instanceof List && !((List<?>) r).isEmpty() && ((List<?>) r).get(0) instanceof String) return (String) ((List<?>) r).get(0);
        throw new IOException("upload refused: " + new String(reply, "UTF-8"));
    }

    /**
     * The inputs of a step, from the Space's own description of it (/gradio_api/info): the picture goes to the
     * first picture or file parameter, the description to a caption or prompt box, every other parameter keeps
     * the demo's default. Without the description, the picture alone is sent (TripoSR-style steps).
     */
    public static List<Object> stepInputs(Object info, String step, Object file, String description) {
        List<Object> data = new ArrayList<Object>();
        Map<String, Object> named = info == null ? null : Json.obj(info, "named_endpoints");
        Object ep = named == null ? null : named.get("/" + step);
        List<Object> params = ep == null ? null : Json.arr(ep, "parameters");
        if (params == null) { data.add(file); return data; }
        boolean fileGiven = false;
        for (Object prm : params) {
            String comp = Json.str(prm, "component", ""), name = Json.str(prm, "parameter_name", Json.str(prm, "label", "")).toLowerCase();
            Map<String, Object> pt = Json.obj(prm, "python_type");
            String type = pt == null ? "" : Json.str(pt, "type", "");
            boolean fileLike = comp.equals("Image") || comp.equals("File") || comp.equals("Model3D") || type.contains("filepath") || type.contains("Image");
            if (fileLike) { data.add(fileGiven ? null : file); fileGiven = true; continue; }
            boolean hasDefault = prm instanceof Map && ((Map<?, ?>) prm).containsKey("parameter_default");
            Object def = hasDefault ? ((Map<?, ?>) prm).get("parameter_default") : null;
            if (comp.equals("Textbox") && (name.contains("caption") || name.contains("prompt") || name.contains("text") || name.contains("description")))
                data.add(prompt(description));
            else if (hasDefault) data.add(def);
            else if (comp.equals("Checkbox")) data.add(Boolean.FALSE);
            else if (comp.equals("Slider") || comp.equals("Number")) data.add(0);
            else data.add(null);
        }
        if (!fileGiven) data.add(0, file);
        return data;
    }

    /** Calls one step and reads its result from the event stream. */
    static List<Object> call(Cloud cloud, String base, String step, List<Object> data, long t0) throws IOException {
        byte[] reply = cloud.request("POST", base + "/gradio_api/call/" + step, "application/json", Json.write(Cloud.map("data", data)).getBytes("UTF-8"));
        String eventId = Json.str(Json.parseLoose(new String(reply, "UTF-8")), "event_id", "");
        if (eventId.length() == 0) throw new IOException(step + ": no event id (" + new String(reply, "UTF-8") + ")");
        String stream = new String(cloud.request("GET", base + "/gradio_api/call/" + step + "/" + eventId, null, null), "UTF-8");
        return complete(stream, step);
    }

    /** The data of the "complete" event of a Gradio event stream; an "error" event is an exception. */
    public static List<Object> complete(String stream, String step) throws IOException {
        String event = "";
        List<Object> result = null;
        for (String line : stream.split("\n")) {
            line = line.trim();
            if (line.startsWith("event:")) event = line.substring(6).trim();
            else if (line.startsWith("data:")) {
                String d = line.substring(5).trim();
                if (event.equals("error")) throw new IOException(step + " failed: " + (d.length() > 160 ? d.substring(0, 160) : d));
                if (event.equals("complete")) {
                    Object o = Json.parseLoose(d);
                    if (o instanceof List) result = (List<Object>) o;
                }
            }
        }
        if (result == null) throw new IOException(step + ": the service gave no result");
        return result;
    }

    private static Object firstFile(List<Object> outputs) {
        for (Object o : outputs) if (o instanceof Map && (Json.str(o, "path", "").length() > 0 || Json.str(o, "url", "").length() > 0)) return o;
        return null;
    }

    /** The GLB among a step's outputs, as a URL to download. */
    public static String glbUrl(List<Object> outputs, String base) {
        String any = null;
        for (Object o : outputs) {
            if (!(o instanceof Map)) continue;
            String url = Json.str(o, "url", ""), path = Json.str(o, "path", "");
            String u = url.length() > 0 ? url : path.length() > 0 ? base + "/gradio_api/file=" + path : null;
            if (u == null) continue;
            String low = (path.length() > 0 ? path : url).toLowerCase();
            if (low.endsWith(".glb")) return u;
            if (any == null && !low.endsWith(".mp4") && !low.endsWith(".png") && !low.endsWith(".jpg") && !low.endsWith(".obj")) any = u;
        }
        return any;
    }
}
