package com.tarun.kahani.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * Reads a 3D model in the binary glTF 2.0 format (.glb) — what image-to-3D services return — into a Studio3D
 * scene: positions, normals, texture points, triangles and the base-colour picture of each material, with the
 * node transforms applied; the figure is then centred, stood on the ground and scaled to a height of 1, so the
 * studio can render it from any side like a figure made from a picture. Only what a character model needs is
 * read: triangle meshes, embedded pictures, the base colour (no animation, no skins).
 */
public final class Glb {
    private Glb() {}

    /** Decodes an embedded picture (PNG or JPEG bytes) into {w, h, pixels…}, or null. */
    public interface ImageDecoder { int[] decode(byte[] bytes); }

    /** A loaded model: the scene with its meshes, and its measures. */
    public static final class Model {
        public final Studio3D.Scene scene = new Studio3D.Scene();
        public int triangles, pictures;
        /** The model's width in units of its height (after scaling to height 1). */
        public float wide = 0.6f;
        public String note = "";
    }

    /** Loads a GLB. Throws IllegalArgumentException when it is not one. */
    public static Model load(byte[] glb, ImageDecoder dec) {
        ByteBuffer bb = ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN);
        if (glb.length < 20 || bb.getInt(0) != 0x46546C67) throw new IllegalArgumentException("Not a GLB file");
        int total = bb.getInt(8);
        int pos = 12;
        String json = null;
        byte[] bin = null;
        while (pos + 8 <= Math.min(total, glb.length)) {
            int len = bb.getInt(pos), type = bb.getInt(pos + 4);
            pos += 8;
            if (type == 0x4E4F534A) json = new String(glb, pos, len, java.nio.charset.Charset.forName("UTF-8"));
            else if (type == 0x004E4942) { bin = new byte[len]; System.arraycopy(glb, pos, bin, 0, len); }
            pos += len;
        }
        if (json == null) throw new IllegalArgumentException("The GLB has no JSON chunk");
        Object g = Json.parse(json);
        Model m = new Model();
        List<Object> accessors = Json.arr(g, "accessors"), views = Json.arr(g, "bufferViews"), meshes = Json.arr(g, "meshes");
        List<Object> materials = Json.arr(g, "materials"), textures = Json.arr(g, "textures"), images = Json.arr(g, "images"), nodes = Json.arr(g, "nodes");
        // the base-colour picture of every material, decoded once
        Studio3D.Material[] mats = new Studio3D.Material[materials == null ? 0 : materials.size()];
        for (int i = 0; i < mats.length; i++) {
            Object mat = materials.get(i);
            Map<String, Object> pbr = Json.obj(mat, "pbrMetallicRoughness");
            int color = 0xFFBFBFBF;
            Studio3D.Material sm = null;
            if (pbr != null) {
                List<Object> bc = Json.arr(pbr, "baseColorFactor");
                if (bc != null && bc.size() >= 3) color = 0xFF000000 | ch(bc.get(0)) << 16 | ch(bc.get(1)) << 8 | ch(bc.get(2));
                Map<String, Object> bt = Json.obj(pbr, "baseColorTexture");
                if (bt != null && textures != null && images != null && dec != null && bin != null) {
                    int ti = (int) Json.num(bt, "index", -1);
                    if (ti >= 0 && ti < textures.size()) {
                        int src = (int) Json.num(textures.get(ti), "source", -1);
                        if (src >= 0 && src < images.size()) {
                            Object im = images.get(src);
                            int bv = (int) Json.num(im, "bufferView", -1);
                            if (bv >= 0 && views != null && bv < views.size()) {
                                Object view = views.get(bv);
                                int off = (int) Json.num(view, "byteOffset", 0), len = (int) Json.num(view, "byteLength", 0);
                                if (off >= 0 && len > 0 && off + len <= bin.length) {
                                    byte[] bytes = new byte[len];
                                    System.arraycopy(bin, off, bytes, 0, len);
                                    int[] d = dec.decode(bytes);
                                    if (d != null && d.length >= 2 + d[0] * d[1]) {
                                        int[] px = new int[d[0] * d[1]];
                                        System.arraycopy(d, 2, px, 0, px.length);
                                        sm = Studio3D.Material.textured(px, d[0], d[1], 0.2f);
                                        m.pictures++;
                                    }
                                }
                            }
                        }
                    }
                }
                float metal = (float) Json.num(pbr, "metallicFactor", 1), rough = (float) Json.num(pbr, "roughnessFactor", 1);
                if (sm == null) sm = new Studio3D.Material(color).set(0.1f + 0.4f * (1 - rough), 8 + 40 * (1 - rough), 0.3f, 0.25f);
                if (metal > 0.5f && sm.tex == null) sm.metal = metal;
            }
            if (sm == null) sm = new Studio3D.Material(color).set(0.1f, 10f, 0.3f, 0.25f);
            mats[i] = sm;
        }
        Studio3D.Material plain = new Studio3D.Material(0xFFBFBFBF).set(0.1f, 10f, 0.3f, 0.25f);
        int plainIdx = -1;
        int[] matIdx = new int[mats.length];
        for (int i = 0; i < mats.length; i++) matIdx[i] = m.scene.mesh.mat(mats[i]);
        // the nodes of the scene with their transforms (a matrix, or translation / rotation / scale)
        float[] ident = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        List<Object> scenes = Json.arr(g, "scenes");
        int sceneIdx = (int) Json.num(g, "scene", 0);
        List<Object> roots = scenes != null && sceneIdx >= 0 && sceneIdx < scenes.size() ? Json.arr(scenes.get(sceneIdx), "nodes") : null;
        float[] bounds = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        if (roots != null && nodes != null) {
            for (Object r : roots) node(m, nodes, ((Number) r).intValue(), ident, meshes, accessors, views, bin, matIdx, plain, bounds, 0);
        } else if (meshes != null) {
            for (int i = 0; i < meshes.size(); i++) mesh(m, meshes.get(i), ident, accessors, views, bin, matIdx, plain, bounds);
        }
        if (m.scene.mesh.nv == 0) throw new IllegalArgumentException("The GLB has no triangles");
        // centre the figure, stand it on the ground, make it 1 unit tall
        float cx = (bounds[0] + bounds[3]) / 2, cz = (bounds[2] + bounds[5]) / 2, h = Math.max(1e-6f, bounds[4] - bounds[1]);
        float k = 1f / h;
        Studio3D.Mesh mesh = m.scene.mesh;
        for (int i = 0; i < mesh.nv; i++) {
            mesh.v[i * 3] = (mesh.v[i * 3] - cx) * k;
            mesh.v[i * 3 + 1] = (mesh.v[i * 3 + 1] - bounds[1]) * k;
            mesh.v[i * 3 + 2] = (mesh.v[i * 3 + 2] - cz) * k;
        }
        m.wide = Math.max(0.2f, (bounds[3] - bounds[0]) * k);
        m.triangles = mesh.nt;
        m.note = mesh.nv + " vertices, " + mesh.nt + " triangles, " + m.pictures + " picture(s)";
        return m;
    }

    private static int ch(Object o) { return Math.max(0, Math.min(255, Math.round((float) (((Number) o).doubleValue() * 255)))); }

    private static void node(Model m, List<Object> nodes, int idx, float[] parent, List<Object> meshes, List<Object> accessors, List<Object> views, byte[] bin, int[] matIdx, Studio3D.Material plain, float[] bounds, int depth) {
        if (idx < 0 || idx >= nodes.size() || depth > 32) return;
        Object n = nodes.get(idx);
        float[] local = ident();
        List<Object> mat = Json.arr(n, "matrix");
        if (mat != null && mat.size() == 16) for (int i = 0; i < 16; i++) local[i] = ((Number) mat.get(i)).floatValue();
        else {
            List<Object> t = Json.arr(n, "translation"), r = Json.arr(n, "rotation"), sc = Json.arr(n, "scale");
            float[] T = ident(), R = ident(), S = ident();
            if (sc != null && sc.size() == 3) { S[0] = f(sc, 0); S[5] = f(sc, 1); S[10] = f(sc, 2); }
            if (r != null && r.size() == 4) {
                float x = f(r, 0), y = f(r, 1), z = f(r, 2), w = f(r, 3);
                R[0] = 1 - 2 * (y * y + z * z); R[1] = 2 * (x * y + z * w); R[2] = 2 * (x * z - y * w);
                R[4] = 2 * (x * y - z * w); R[5] = 1 - 2 * (x * x + z * z); R[6] = 2 * (y * z + x * w);
                R[8] = 2 * (x * z + y * w); R[9] = 2 * (y * z - x * w); R[10] = 1 - 2 * (x * x + y * y);
            }
            if (t != null && t.size() == 3) { T[12] = f(t, 0); T[13] = f(t, 1); T[14] = f(t, 2); }
            local = mul(T, mul(R, S));
        }
        float[] world = mul(parent, local);
        int mi = (int) Json.num(n, "mesh", -1);
        if (mi >= 0 && meshes != null && mi < meshes.size()) mesh(m, meshes.get(mi), world, accessors, views, bin, matIdx, plain, bounds);
        List<Object> children = Json.arr(n, "children");
        if (children != null) for (Object c : children) node(m, nodes, ((Number) c).intValue(), world, meshes, accessors, views, bin, matIdx, plain, bounds, depth + 1);
    }

    private static float f(List<Object> l, int i) { return ((Number) l.get(i)).floatValue(); }
    private static float[] ident() { return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }

    /** Column-major 4x4 product a * b (glTF order). */
    private static float[] mul(float[] a, float[] b) {
        float[] o = new float[16];
        for (int c = 0; c < 4; c++) for (int r = 0; r < 4; r++) {
            float s = 0;
            for (int k = 0; k < 4; k++) s += a[k * 4 + r] * b[c * 4 + k];
            o[c * 4 + r] = s;
        }
        return o;
    }

    private static void mesh(Model m, Object mesh, float[] w, List<Object> accessors, List<Object> views, byte[] bin, int[] matIdx, Studio3D.Material plain, float[] bounds) {
        List<Object> prims = Json.arr(mesh, "primitives");
        if (prims == null) return;
        for (Object p : prims) {
            int mode = (int) Json.num(p, "mode", 4);
            if (mode != 4) continue;                      // triangles only
            Map<String, Object> attr = Json.obj(p, "attributes");
            if (attr == null) continue;
            float[] pos = floats(accessors, views, bin, (int) Json.num(attr, "POSITION", -1), 3);
            if (pos == null) continue;
            float[] nrm = floats(accessors, views, bin, (int) Json.num(attr, "NORMAL", -1), 3);
            float[] uv = floats(accessors, views, bin, (int) Json.num(attr, "TEXCOORD_0", -1), 2);
            int[] idx = ints(accessors, views, bin, (int) Json.num(p, "indices", -1));
            int mi = (int) Json.num(p, "material", -1);
            int mat = mi >= 0 && mi < matIdx.length ? matIdx[mi] : -1;
            if (mat < 0) mat = m.scene.mesh.mat(plain);
            Studio3D.Material material = m.scene.mesh.mats.get(mat);
            int n = pos.length / 3, base = m.scene.mesh.nv;
            float[] nw = new float[]{0, 0, 0};
            for (int i = 0; i < n; i++) {
                float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
                float wx = w[0] * x + w[4] * y + w[8] * z + w[12], wy = w[1] * x + w[5] * y + w[9] * z + w[13], wz = w[2] * x + w[6] * y + w[10] * z + w[14];
                float nx = 0, ny = 1, nz = 0;
                if (nrm != null && nrm.length >= (i + 1) * 3) {
                    float a = nrm[i * 3], b = nrm[i * 3 + 1], c = nrm[i * 3 + 2];
                    nx = w[0] * a + w[4] * b + w[8] * c; ny = w[1] * a + w[5] * b + w[9] * c; nz = w[2] * a + w[6] * b + w[10] * c;
                }
                float u = 0, v = 0;
                if (uv != null && uv.length >= (i + 1) * 2 && material.tex != null) { u = uv[i * 2] * material.texW; v = uv[i * 2 + 1] * material.texH; }
                m.scene.mesh.vertex(wx, wy, wz, nx, ny, nz, u, v);
                if (wx < bounds[0]) bounds[0] = wx; if (wy < bounds[1]) bounds[1] = wy; if (wz < bounds[2]) bounds[2] = wz;
                if (wx > bounds[3]) bounds[3] = wx; if (wy > bounds[4]) bounds[4] = wy; if (wz > bounds[5]) bounds[5] = wz;
            }
            // a mirrored transform turns the triangles inside out: keep them facing outward
            boolean flip = det3(w) < 0;
            if (idx != null) {
                for (int i = 0; i + 2 < idx.length; i += 3) {
                    if (idx[i] >= n || idx[i + 1] >= n || idx[i + 2] >= n) continue;
                    if (flip) m.scene.mesh.triangle(base + idx[i], base + idx[i + 2], base + idx[i + 1], mat);
                    else m.scene.mesh.triangle(base + idx[i], base + idx[i + 1], base + idx[i + 2], mat);
                }
            } else {
                for (int i = 0; i + 2 < n; i += 3) {
                    if (flip) m.scene.mesh.triangle(base + i, base + i + 2, base + i + 1, mat);
                    else m.scene.mesh.triangle(base + i, base + i + 1, base + i + 2, mat);
                }
            }
        }
    }

    private static float det3(float[] w) {
        return w[0] * (w[5] * w[10] - w[9] * w[6]) - w[4] * (w[1] * w[10] - w[9] * w[2]) + w[8] * (w[1] * w[6] - w[5] * w[2]);
    }

    /** The floats of an accessor (VEC2 / VEC3 / SCALAR of FLOAT), or null. */
    private static float[] floats(List<Object> accessors, List<Object> views, byte[] bin, int ai, int comps) {
        if (ai < 0 || accessors == null || ai >= accessors.size() || bin == null) return null;
        Object acc = accessors.get(ai);
        int count = (int) Json.num(acc, "count", 0), ct = (int) Json.num(acc, "componentType", 5126);
        int bv = (int) Json.num(acc, "bufferView", -1);
        if (bv < 0 || views == null || bv >= views.size() || count <= 0) return null;
        Object view = views.get(bv);
        int off = (int) Json.num(view, "byteOffset", 0) + (int) Json.num(acc, "byteOffset", 0);
        int stride = (int) Json.num(view, "byteStride", 0);
        int size = ct == 5126 ? 4 : ct == 5123 || ct == 5122 ? 2 : 1;
        if (stride == 0) stride = size * comps;
        boolean norm = Json.bool(acc, "normalized", false);
        float[] out = new float[count * comps];
        ByteBuffer bb = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) for (int c = 0; c < comps; c++) {
            int at = off + i * stride + c * size;
            if (at + size > bin.length) return out;
            float v;
            if (ct == 5126) v = bb.getFloat(at);
            else if (ct == 5123) { v = bb.getShort(at) & 0xFFFF; if (norm) v /= 65535f; }
            else if (ct == 5122) { v = bb.getShort(at); if (norm) v /= 32767f; }
            else if (ct == 5121) { v = bin[at] & 0xFF; if (norm) v /= 255f; }
            else { v = bin[at]; if (norm) v /= 127f; }
            out[i * comps + c] = v;
        }
        return out;
    }

    /** The indices of an accessor (unsigned byte / short / int), or null. */
    private static int[] ints(List<Object> accessors, List<Object> views, byte[] bin, int ai) {
        if (ai < 0 || accessors == null || ai >= accessors.size() || bin == null) return null;
        Object acc = accessors.get(ai);
        int count = (int) Json.num(acc, "count", 0), ct = (int) Json.num(acc, "componentType", 5125);
        int bv = (int) Json.num(acc, "bufferView", -1);
        if (bv < 0 || views == null || bv >= views.size() || count <= 0) return null;
        Object view = views.get(bv);
        int off = (int) Json.num(view, "byteOffset", 0) + (int) Json.num(acc, "byteOffset", 0);
        int size = ct == 5125 ? 4 : ct == 5123 ? 2 : 1;
        int stride = (int) Json.num(view, "byteStride", 0);
        if (stride == 0) stride = size;
        int[] out = new int[count];
        ByteBuffer bb = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) {
            int at = off + i * stride;
            if (at + size > bin.length) break;
            out[i] = size == 4 ? bb.getInt(at) : size == 2 ? bb.getShort(at) & 0xFFFF : bin[at] & 0xFF;
        }
        return out;
    }

    /** The model seen from an angle (Figure3D's views), about 'size' pixels tall. */
    public static Doll3D.Result render(Model m, int size, float angleDeg) {
        Studio3D.Scene s = m.scene;
        s.marks.clear();
        return Figure3D.renderFigure(s, m.wide, size, angleDeg, false);
    }
}
