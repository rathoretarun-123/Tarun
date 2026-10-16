package com.tarun.kahani.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a 3D model in glTF 2.0 — binary (.glb, what image-to-3D services return) or JSON (.gltf with its .bin
 * and pictures beside it, what free model packs on GitHub ship) — into a Studio3D scene: positions, normals,
 * texture points, triangles and the base-colour picture of each material, with the node transforms applied.
 * A rigged model (a skin with joints) is posed: its "Idle" animation's first frame when it has one, else the
 * bind pose, so a free character stands naturally instead of in a T-pose; props parented to a hand slot
 * (swords, shields, books) are left out unless the character's description asks for them. The figure is then
 * centred, stood on the ground and scaled to a height of 1, so the studio can render it from any side like a
 * figure made from a picture.
 */
public final class Glb {
    private Glb() {}

    /** Decodes an embedded picture (PNG or JPEG bytes) into {w, h, pixels…}, or null. */
    public interface ImageDecoder { int[] decode(byte[] bytes); }

    /** Fetches a file named beside a .gltf (its .bin, its textures) by its uri, or returns null. */
    public interface Fetcher { byte[] fetch(String uri); }

    /** What to keep and how to pose. */
    public static final class Options {
        /** Animation names to pose from, first found wins (the first frame); empty = the bind pose. */
        public String[] pose = {"Idle", "Unarmed_Idle", "idle", "Idle_A", "Standing"};
        /** Props (meshes under a hand slot) whose names contain one of these words are kept; all others are dropped. */
        public String[] props = {};
        /** Keep every prop (nothing dropped). */
        public boolean allProps;
    }

    /** A loaded model: the scene with its meshes, and its measures. */
    public static final class Model {
        public final Studio3D.Scene scene = new Studio3D.Scene();
        public int triangles, pictures, joints;
        /** The model's width in units of its height (after scaling to height 1). */
        public float wide = 0.6f;
        public String note = "", posed = "";
        public final List<String> animations = new ArrayList<String>(), dropped = new ArrayList<String>();
    }

    /** Loads a GLB (or a .gltf whose buffers are embedded as data URIs). Throws IllegalArgumentException when it is neither. */
    public static Model load(byte[] data, ImageDecoder dec) { return load(data, dec, null, new Options()); }

    /** Loads a GLB or a .gltf; the fetcher brings the files a .gltf names beside it. */
    public static Model load(byte[] data, ImageDecoder dec, Fetcher fetch, Options opt) {
        if (opt == null) opt = new Options();
        String json;
        byte[] chunk = null;
        if (data.length >= 20 && ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt(0) == 0x46546C67) {
            ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            int total = bb.getInt(8), pos = 12;
            json = null;
            while (pos + 8 <= Math.min(total, data.length)) {
                int len = bb.getInt(pos), type = bb.getInt(pos + 4);
                pos += 8;
                if (type == 0x4E4F534A) json = new String(data, pos, len, java.nio.charset.Charset.forName("UTF-8"));
                else if (type == 0x004E4942) { chunk = new byte[len]; System.arraycopy(data, pos, chunk, 0, len); }
                pos += len;
            }
            if (json == null) throw new IllegalArgumentException("The GLB has no JSON chunk");
        } else {
            json = new String(data, java.nio.charset.Charset.forName("UTF-8")).trim();
            if (!json.startsWith("{")) throw new IllegalArgumentException("Not a glTF file");
        }
        Object g = Json.parse(json);
        Model m = new Model();
        List<Object> accessors = Json.arr(g, "accessors"), views = Json.arr(g, "bufferViews"), meshes = Json.arr(g, "meshes");
        List<Object> materials = Json.arr(g, "materials"), textures = Json.arr(g, "textures"), images = Json.arr(g, "images"), nodes = Json.arr(g, "nodes");
        List<Object> buffers = Json.arr(g, "buffers"), skins = Json.arr(g, "skins"), animations = Json.arr(g, "animations");
        // ---- the buffers: the GLB's chunk, data URIs, or files beside the .gltf
        byte[][] bins = new byte[buffers == null ? 1 : Math.max(1, buffers.size())][];
        if (buffers != null) for (int i = 0; i < buffers.size(); i++) {
            String uri = Json.str(buffers.get(i), "uri", "");
            if (uri.length() == 0) bins[i] = i == 0 ? chunk : null;
            else if (uri.startsWith("data:")) bins[i] = dataUri(uri);
            else if (fetch != null) bins[i] = fetch.fetch(uri);
        } else bins[0] = chunk;
        Buffers B = new Buffers(accessors, views, bins);
        // ---- the base-colour picture of every material, decoded once
        Studio3D.Material[] mats = new Studio3D.Material[materials == null ? 0 : materials.size()];
        Map<Integer, Studio3D.Material> decoded = new HashMap<Integer, Studio3D.Material>();
        for (int i = 0; i < mats.length; i++) {
            Object mat = materials.get(i);
            Map<String, Object> pbr = Json.obj(mat, "pbrMetallicRoughness");
            int color = 0xFFBFBFBF;
            Studio3D.Material sm = null;
            if (pbr != null) {
                List<Object> bc = Json.arr(pbr, "baseColorFactor");
                if (bc != null && bc.size() >= 3) color = 0xFF000000 | ch(bc.get(0)) << 16 | ch(bc.get(1)) << 8 | ch(bc.get(2));
                Map<String, Object> bt = Json.obj(pbr, "baseColorTexture");
                if (bt != null && textures != null && images != null && dec != null) {
                    int ti = (int) Json.num(bt, "index", -1);
                    int src = ti >= 0 && ti < textures.size() ? (int) Json.num(textures.get(ti), "source", -1) : -1;
                    if (src >= 0 && src < images.size()) {
                        if (decoded.containsKey(src)) sm = decoded.get(src);
                        else {
                            Object im = images.get(src);
                            byte[] bytes = null;
                            int bv = (int) Json.num(im, "bufferView", -1);
                            String uri = Json.str(im, "uri", "");
                            if (bv >= 0) bytes = B.view(bv);
                            else if (uri.startsWith("data:")) bytes = dataUri(uri);
                            else if (uri.length() > 0 && fetch != null) bytes = fetch.fetch(uri);
                            int[] d = bytes == null ? null : dec.decode(bytes);
                            if (d != null && d.length >= 2 + d[0] * d[1]) {
                                int[] px = new int[d[0] * d[1]];
                                System.arraycopy(d, 2, px, 0, px.length);
                                sm = Studio3D.Material.textured(px, d[0], d[1], 0.2f);
                                m.pictures++;
                            }
                            decoded.put(src, sm);
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
        int[] matIdx = new int[mats.length];
        for (int i = 0; i < mats.length; i++) matIdx[i] = m.scene.mesh.mat(mats[i]);
        // ---- the nodes: local transforms (posed by the animation's first frame), then the world transforms
        int nn = nodes == null ? 0 : nodes.size();
        float[][] local = new float[nn][];
        float[][] T = new float[nn][], R = new float[nn][], S = new float[nn][];
        boolean[] matrixNode = new boolean[nn];
        int[] parent = new int[nn];
        java.util.Arrays.fill(parent, -1);
        for (int i = 0; i < nn; i++) {
            Object n = nodes.get(i);
            List<Object> mat = Json.arr(n, "matrix");
            if (mat != null && mat.size() == 16) { local[i] = new float[16]; for (int k = 0; k < 16; k++) local[i][k] = ((Number) mat.get(k)).floatValue(); matrixNode[i] = true; }
            List<Object> t = Json.arr(n, "translation"), r = Json.arr(n, "rotation"), sc = Json.arr(n, "scale");
            T[i] = t != null && t.size() == 3 ? new float[]{f(t, 0), f(t, 1), f(t, 2)} : new float[]{0, 0, 0};
            R[i] = r != null && r.size() == 4 ? new float[]{f(r, 0), f(r, 1), f(r, 2), f(r, 3)} : new float[]{0, 0, 0, 1};
            S[i] = sc != null && sc.size() == 3 ? new float[]{f(sc, 0), f(sc, 1), f(sc, 2)} : new float[]{1, 1, 1};
            List<Object> ch = Json.arr(n, "children");
            if (ch != null) for (Object c : ch) { int ci = ((Number) c).intValue(); if (ci >= 0 && ci < nn) parent[ci] = i; }
        }
        if (animations != null) {
            for (Object a : animations) m.animations.add(Json.str(a, "name", ""));
            Object chosen = null;
            for (String want : opt.pose) {
                for (Object a : animations) if (Json.str(a, "name", "").equalsIgnoreCase(want)) { chosen = a; break; }
                if (chosen != null) break;
            }
            if (chosen != null) {
                m.posed = Json.str(chosen, "name", "");
                List<Object> channels = Json.arr(chosen, "channels"), samplers = Json.arr(chosen, "samplers");
                if (channels != null && samplers != null) for (Object c : channels) {
                    Map<String, Object> target = Json.obj(c, "target");
                    int node = target == null ? -1 : (int) Json.num(target, "node", -1);
                    int si = (int) Json.num(c, "sampler", -1);
                    if (node < 0 || node >= nn || si < 0 || si >= samplers.size()) continue;
                    String path = Json.str(target, "path", "");
                    int comps = path.equals("rotation") ? 4 : 3;
                    float[] out = B.floats((int) Json.num(samplers.get(si), "output", -1), comps);
                    if (out == null || out.length < comps) continue;
                    float[] first = new float[comps];
                    System.arraycopy(out, 0, first, 0, comps);     // the first keyframe
                    if (path.equals("rotation")) R[node] = first; else if (path.equals("translation")) T[node] = first; else if (path.equals("scale")) S[node] = first;
                    matrixNode[node] = false;
                }
            }
        }
        for (int i = 0; i < nn; i++) if (!matrixNode[i]) local[i] = trs(T[i], R[i], S[i]);
        float[][] world = new float[nn][];
        for (int i = 0; i < nn; i++) world(i, parent, local, world, 0);
        // ---- the skins: the joint matrices of the pose
        float[][][] jointMats = new float[skins == null ? 0 : skins.size()][][];
        if (skins != null) for (int s = 0; s < skins.size(); s++) {
            List<Object> joints = Json.arr(skins.get(s), "joints");
            float[] ibm = B.floats((int) Json.num(skins.get(s), "inverseBindMatrices", -1), 16);
            if (joints == null) continue;
            jointMats[s] = new float[joints.size()][];
            for (int j = 0; j < joints.size(); j++) {
                int ji = ((Number) joints.get(j)).intValue();
                float[] inv = ident();
                if (ibm != null && ibm.length >= (j + 1) * 16) System.arraycopy(ibm, j * 16, inv, 0, 16);
                jointMats[s][j] = ji >= 0 && ji < nn && world[ji] != null ? mul(world[ji], inv) : inv;
            }
            m.joints = Math.max(m.joints, joints.size());
        }
        // ---- the meshes of the scene's nodes (props under a hand slot only when asked for)
        float[] bounds = {1e9f, 1e9f, 1e9f, -1e9f, -1e9f, -1e9f};
        List<Object> scenes = Json.arr(g, "scenes");
        int sceneIdx = (int) Json.num(g, "scene", 0);
        List<Object> roots = scenes != null && sceneIdx >= 0 && sceneIdx < scenes.size() ? Json.arr(scenes.get(sceneIdx), "nodes") : null;
        boolean any = false;
        if (roots != null && nn > 0) {
            for (Object r : roots) any |= nodeMeshes(m, nodes, ((Number) r).intValue(), parent, world, meshes, B, matIdx, plain, bounds, jointMats, opt, 0);
        }
        if (!any && meshes != null) for (int i = 0; i < meshes.size(); i++) mesh(m, meshes.get(i), ident(), -1, B, matIdx, plain, bounds, null);
        if (m.scene.mesh.nv == 0) throw new IllegalArgumentException("The model has no triangles");
        // ---- centre the figure, stand it on the ground, make it 1 unit tall
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
        m.note = mesh.nv + " vertices, " + mesh.nt + " triangles, " + m.pictures + " picture(s)" + (m.joints > 0 ? ", " + m.joints + " joints" + (m.posed.length() > 0 ? " posed from " + m.posed : " in the bind pose") : "")
                + (m.dropped.isEmpty() ? "" : ", props left out: " + m.dropped);
        return m;
    }

    private static boolean nodeMeshes(Model m, List<Object> nodes, int idx, int[] parent, float[][] world, List<Object> meshes, Buffers B, int[] matIdx, Studio3D.Material plain, float[] bounds, float[][][] jointMats, Options opt, int depth) {
        if (idx < 0 || idx >= nodes.size() || depth > 48) return false;
        Object n = nodes.get(idx);
        boolean any = false;
        int mi = (int) Json.num(n, "mesh", -1);
        if (mi >= 0 && meshes != null && mi < meshes.size()) {
            int skin = (int) Json.num(n, "skin", -1);
            float[][] jm = skin >= 0 && skin < jointMats.length ? jointMats[skin] : null;
            String name = Json.str(n, "name", "");
            if (jm == null && underHandSlot(nodes, idx, parent) && !opt.allProps && !wanted(name, opt.props)) m.dropped.add(name);
            else { mesh(m, meshes.get(mi), world[idx], mi, B, matIdx, plain, bounds, jm); any = true; }
        }
        List<Object> children = Json.arr(n, "children");
        if (children != null) for (Object c : children) any |= nodeMeshes(m, nodes, ((Number) c).intValue(), parent, world, meshes, B, matIdx, plain, bounds, jointMats, opt, depth + 1);
        return any;
    }

    /** A prop: a mesh under a node whose name says it is a hand slot or a held thing (KayKit, Quaternius and Mixamo naming). */
    private static boolean underHandSlot(List<Object> nodes, int idx, int[] parent) {
        for (int p = parent[idx], guard = 0; p >= 0 && guard < 48; p = parent[p], guard++) {
            String n = Json.str(nodes.get(p), "name", "").toLowerCase();
            if (n.contains("handslot") || n.contains("hand_slot") || n.contains("weapon") || n.contains("prop") || n.contains("socket") || n.contains("attach")) return true;
        }
        return false;
    }

    private static boolean wanted(String name, String[] props) {
        if (props == null) return false;
        String n = name.toLowerCase();
        for (String p : props) if (p != null && p.length() > 0 && n.contains(p.toLowerCase())) return true;
        return false;
    }

    private static void world(int i, int[] parent, float[][] local, float[][] world, int depth) {
        if (world[i] != null || depth > 64) return;
        if (parent[i] < 0) world[i] = local[i];
        else { world(parent[i], parent, local, world, depth + 1); world[i] = mul(world[parent[i]], local[i]); }
    }

    private static int ch(Object o) { return Math.max(0, Math.min(255, Math.round((float) (((Number) o).doubleValue() * 255)))); }
    private static float f(List<Object> l, int i) { return ((Number) l.get(i)).floatValue(); }
    private static float[] ident() { return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}; }

    private static float[] trs(float[] t, float[] r, float[] s) {
        float x = r[0], y = r[1], z = r[2], w = r[3];
        float[] R = ident(), Tm = ident(), Sm = ident();
        R[0] = 1 - 2 * (y * y + z * z); R[1] = 2 * (x * y + z * w); R[2] = 2 * (x * z - y * w);
        R[4] = 2 * (x * y - z * w); R[5] = 1 - 2 * (x * x + z * z); R[6] = 2 * (y * z + x * w);
        R[8] = 2 * (x * z + y * w); R[9] = 2 * (y * z - x * w); R[10] = 1 - 2 * (x * x + y * y);
        Sm[0] = s[0]; Sm[5] = s[1]; Sm[10] = s[2];
        Tm[12] = t[0]; Tm[13] = t[1]; Tm[14] = t[2];
        return mul(Tm, mul(R, Sm));
    }

    /** Column-major 4x4 product a * b (glTF order). */
    static float[] mul(float[] a, float[] b) {
        float[] o = new float[16];
        for (int c = 0; c < 4; c++) for (int r = 0; r < 4; r++) {
            float s = 0;
            for (int k = 0; k < 4; k++) s += a[k * 4 + r] * b[c * 4 + k];
            o[c * 4 + r] = s;
        }
        return o;
    }

    private static byte[] dataUri(String uri) {
        int comma = uri.indexOf(',');
        if (comma < 0) return null;
        return uri.substring(0, comma).contains(";base64") ? Base64.decode(uri.substring(comma + 1)) : uri.substring(comma + 1).getBytes(java.nio.charset.Charset.forName("UTF-8"));
    }

    private static void mesh(Model m, Object mesh, float[] w, int meshIdx, Buffers B, int[] matIdx, Studio3D.Material plain, float[] bounds, float[][] jm) {
        List<Object> prims = Json.arr(mesh, "primitives");
        if (prims == null) return;
        for (Object p : prims) {
            int mode = (int) Json.num(p, "mode", 4);
            if (mode != 4) continue;                      // triangles only
            Map<String, Object> attr = Json.obj(p, "attributes");
            if (attr == null) continue;
            float[] pos = B.floats((int) Json.num(attr, "POSITION", -1), 3);
            if (pos == null) continue;
            float[] nrm = B.floats((int) Json.num(attr, "NORMAL", -1), 3);
            float[] uv = B.floats((int) Json.num(attr, "TEXCOORD_0", -1), 2);
            float[] jw = jm != null ? B.floats((int) Json.num(attr, "WEIGHTS_0", -1), 4) : null;
            int[] jj = jm != null ? B.ints4((int) Json.num(attr, "JOINTS_0", -1)) : null;
            int[] idx = B.ints((int) Json.num(p, "indices", -1));
            int mi = (int) Json.num(p, "material", -1);
            int mat = mi >= 0 && mi < matIdx.length ? matIdx[mi] : -1;
            if (mat < 0) mat = m.scene.mesh.mat(plain);
            Studio3D.Material material = m.scene.mesh.mats.get(mat);
            int n = pos.length / 3, base = m.scene.mesh.nv;
            boolean skinned = jm != null && jw != null && jj != null && jw.length >= n * 4 && jj.length >= n * 4;
            float[] M = new float[16];
            for (int i = 0; i < n; i++) {
                float x = pos[i * 3], y = pos[i * 3 + 1], z = pos[i * 3 + 2];
                float[] t = w;
                if (skinned) {
                    // the vertex follows its joints: the weighted sum of the joint matrices of the pose
                    java.util.Arrays.fill(M, 0);
                    float sum = 0;
                    for (int k = 0; k < 4; k++) {
                        float wk = jw[i * 4 + k];
                        int j = jj[i * 4 + k];
                        if (wk <= 0 || j < 0 || j >= jm.length) continue;
                        float[] J = jm[j];
                        for (int q = 0; q < 16; q++) M[q] += J[q] * wk;
                        sum += wk;
                    }
                    if (sum > 1e-4f) { if (Math.abs(sum - 1) > 1e-3f) for (int q = 0; q < 16; q++) M[q] /= sum; t = M; }
                }
                float wx = t[0] * x + t[4] * y + t[8] * z + t[12], wy = t[1] * x + t[5] * y + t[9] * z + t[13], wz = t[2] * x + t[6] * y + t[10] * z + t[14];
                float nx = 0, ny = 1, nz = 0;
                if (nrm != null && nrm.length >= (i + 1) * 3) {
                    float a = nrm[i * 3], b = nrm[i * 3 + 1], c = nrm[i * 3 + 2];
                    nx = t[0] * a + t[4] * b + t[8] * c; ny = t[1] * a + t[5] * b + t[9] * c; nz = t[2] * a + t[6] * b + t[10] * c;
                }
                float u = 0, v = 0;
                if (uv != null && uv.length >= (i + 1) * 2 && material.tex != null) { u = uv[i * 2] * material.texW; v = uv[i * 2 + 1] * material.texH; }
                m.scene.mesh.vertex(wx, wy, wz, nx, ny, nz, u, v);
                if (wx < bounds[0]) bounds[0] = wx; if (wy < bounds[1]) bounds[1] = wy; if (wz < bounds[2]) bounds[2] = wz;
                if (wx > bounds[3]) bounds[3] = wx; if (wy > bounds[4]) bounds[4] = wy; if (wz > bounds[5]) bounds[5] = wz;
            }
            // a mirrored transform turns the triangles inside out: keep them facing outward
            boolean flip = !skinned && det3(w) < 0;
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

    /** The accessors over the buffers. */
    private static final class Buffers {
        final List<Object> accessors, views;
        final byte[][] bins;
        Buffers(List<Object> accessors, List<Object> views, byte[][] bins) { this.accessors = accessors; this.views = views; this.bins = bins; }

        byte[] view(int bv) {
            if (views == null || bv < 0 || bv >= views.size()) return null;
            Object view = views.get(bv);
            byte[] bin = bin(view);
            int off = (int) Json.num(view, "byteOffset", 0), len = (int) Json.num(view, "byteLength", 0);
            if (bin == null || off < 0 || len <= 0 || off + len > bin.length) return null;
            byte[] out = new byte[len];
            System.arraycopy(bin, off, out, 0, len);
            return out;
        }

        byte[] bin(Object view) {
            int b = (int) Json.num(view, "buffer", 0);
            return b >= 0 && b < bins.length ? bins[b] : null;
        }

        /** The floats of an accessor (SCALAR / VEC2 / VEC3 / VEC4 / MAT4 of float or normalized ints), or null. */
        float[] floats(int ai, int comps) {
            if (ai < 0 || accessors == null || ai >= accessors.size()) return null;
            Object acc = accessors.get(ai);
            int count = (int) Json.num(acc, "count", 0), ct = (int) Json.num(acc, "componentType", 5126);
            int bv = (int) Json.num(acc, "bufferView", -1);
            if (bv < 0 || views == null || bv >= views.size() || count <= 0) return null;
            Object view = views.get(bv);
            byte[] bin = bin(view);
            if (bin == null) return null;
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
        int[] ints(int ai) { return ints(ai, 1); }

        /** The joint indices of a VEC4 accessor, 4 per vertex. */
        int[] ints4(int ai) { return ints(ai, 4); }

        private int[] ints(int ai, int comps) {
            if (ai < 0 || accessors == null || ai >= accessors.size()) return null;
            Object acc = accessors.get(ai);
            int count = (int) Json.num(acc, "count", 0), ct = (int) Json.num(acc, "componentType", 5125);
            int bv = (int) Json.num(acc, "bufferView", -1);
            if (bv < 0 || views == null || bv >= views.size() || count <= 0) return null;
            Object view = views.get(bv);
            byte[] bin = bin(view);
            if (bin == null) return null;
            int off = (int) Json.num(view, "byteOffset", 0) + (int) Json.num(acc, "byteOffset", 0);
            int size = ct == 5125 ? 4 : ct == 5123 ? 2 : 1;
            int stride = (int) Json.num(view, "byteStride", 0);
            if (stride == 0) stride = size * comps;
            int[] out = new int[count * comps];
            ByteBuffer bb = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < count; i++) for (int c = 0; c < comps; c++) {
                int at = off + i * stride + c * size;
                if (at + size > bin.length) return out;
                out[i * comps + c] = size == 4 ? bb.getInt(at) : size == 2 ? bb.getShort(at) & 0xFFFF : bin[at] & 0xFF;
            }
            return out;
        }
    }

    /** The model seen from an angle (Figure3D's views), about 'size' pixels tall. */
    public static Doll3D.Result render(Model m, int size, float angleDeg) {
        Studio3D.Scene s = m.scene;
        s.marks.clear();
        return Figure3D.renderFigure(s, m.wide, size, angleDeg, false);
    }
}
