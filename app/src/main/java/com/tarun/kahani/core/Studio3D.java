package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Studio 3D: the studio's own three-dimensional picture maker, in plain Java, so the director can create a
 * missing picture on the phone without any service — a character (Doll3D) or a place (Set3D) built from
 * rounded solids and rendered like a small animated-feature frame:
 *
 *  - a perspective camera with a long lens for characters (no facial distortion, handbook ch. 5) and a wide one
 *    for places;
 *  - the two-light model of the Pixar-Lead protocol: one key light with a soft shadow (a shadow map with
 *    filtered edges) and one bounce from the ground; plus a rim light on the edges and sky/ground ambient;
 *  - materials as the handbook asks (ch. 7): skin with controlled specular and a little subsurface warmth, hair
 *    with coherent highlights, cloth with a soft rough response, metal with tinted reflections, stone matte,
 *    glass-like eyes with a catch-light; glowing things (lamps, screens, eyes of a monster) emit;
 *  - ambient occlusion in the creases, atmospheric fog for far things, a gentle depth of field for places,
 *    2x supersampling for clean edges, and a filmic tone curve.
 *
 * The output is ARGB pixels: transparent round a character (ready to be cut out and rigged), a full frame for a
 * place with its floor line known exactly.
 */
public final class Studio3D {
    private Studio3D() {}

    // ================================================================== materials

    public static final class Material {
        public int color;
        public float spec = 0.25f, gloss = 24f, wrap = 0.2f, rim = 0.3f, metal = 0f, sss = 0f;
        public int emissive = 0;
        public Material(int color) { this.color = color; }
        Material set(float spec, float gloss, float wrap, float rim) { this.spec = spec; this.gloss = gloss; this.wrap = wrap; this.rim = rim; return this; }
    }

    public static Material skin(int c) { Material m = new Material(c).set(0.16f, 16f, 0.45f, 0.3f); m.sss = 0.35f; return m; }
    public static Material hair(int c) { return new Material(c).set(0.35f, 36f, 0.1f, 0.55f); }
    public static Material cloth(int c) { return new Material(c).set(0.06f, 8f, 0.3f, 0.25f); }
    public static Material silk(int c) { return new Material(c).set(0.3f, 28f, 0.25f, 0.45f); }
    public static Material metal(int c) { Material m = new Material(c).set(0.9f, 56f, 0f, 0.35f); m.metal = 1f; return m; }
    public static Material stone(int c) { return new Material(c).set(0.05f, 6f, 0.15f, 0.12f); }
    public static Material leaf(int c) { return new Material(c).set(0.12f, 12f, 0.4f, 0.3f); }
    public static Material wood(int c) { return new Material(c).set(0.1f, 10f, 0.2f, 0.15f); }
    public static Material eye(int c) { return new Material(c).set(0.9f, 90f, 0f, 0f); }
    public static Material plastic(int c) { return new Material(c).set(0.45f, 30f, 0.1f, 0.3f); }
    public static Material fur(int c) { return new Material(c).set(0.07f, 10f, 0.55f, 0.5f); }
    public static Material glow(int c) { Material m = new Material(c).set(0.1f, 10f, 0.3f, 0f); m.emissive = c; return m; }
    public static Material glowing(int c, int light) { Material m = new Material(c).set(0.1f, 10f, 0.3f, 0f); m.emissive = light; return m; }

    // ================================================================== the mesh and its solids

    public static final class Mesh {
        float[] v = new float[3 * 8192], n = new float[3 * 8192];
        int nv;
        int[] t = new int[3 * 16384], tm = new int[16384];
        int nt;
        final List<Material> mats = new ArrayList<Material>();

        public int mat(Material m) { mats.add(m); return mats.size() - 1; }

        int vertex(float x, float y, float z, float nx, float ny, float nz) {
            if (nv * 3 + 3 > v.length) { v = java.util.Arrays.copyOf(v, v.length * 2); n = java.util.Arrays.copyOf(n, n.length * 2); }
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-6f) { nx = 0; ny = 1; nz = 0; l = 1; }
            v[nv * 3] = x; v[nv * 3 + 1] = y; v[nv * 3 + 2] = z;
            n[nv * 3] = nx / l; n[nv * 3 + 1] = ny / l; n[nv * 3 + 2] = nz / l;
            return nv++;
        }

        void tri(int a, int b, int c, int m) {
            if (nt * 3 + 3 > t.length) { t = java.util.Arrays.copyOf(t, t.length * 2); tm = java.util.Arrays.copyOf(tm, tm.length * 2); }
            t[nt * 3] = a; t[nt * 3 + 1] = b; t[nt * 3 + 2] = c;
            tm[nt] = m;
            nt++;
        }

        public int triangles() { return nt; }

        /** An ellipsoid (a sphere with three radii). seg = rings; twice as many around. */
        public void sphere(float cx, float cy, float cz, float rx, float ry, float rz, int seg, int m) {
            int rings = Math.max(3, seg), around = Math.max(6, seg * 2);
            int base = nv;
            for (int i = 0; i <= rings; i++) {
                double ph = Math.PI * i / rings - Math.PI / 2;
                float cy1 = (float) Math.sin(ph), r1 = (float) Math.cos(ph);
                for (int j = 0; j <= around; j++) {
                    double th = 2 * Math.PI * j / around;
                    float ux = r1 * (float) Math.cos(th), uz = r1 * (float) Math.sin(th);
                    vertex(cx + ux * rx, cy + cy1 * ry, cz + uz * rz, ux / rx, cy1 / ry, uz / rz);
                }
            }
            grid(base, rings, around, m, false);
        }

        /** A tube between two points with a radius at each end (r1 = 0 is a cone), round caps when capped. */
        public void capsule(float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1, int seg, int m) { tube(x0, y0, z0, x1, y1, z1, r0, r1, seg, m, true); }
        public void cylinder(float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1, int seg, int m) { tube(x0, y0, z0, x1, y1, z1, r0, r1, seg, m, false); }

        private void tube(float x0, float y0, float z0, float x1, float y1, float z1, float r0, float r1, int seg, int m, boolean caps) {
            float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-5f) { dx = 0; dy = 1; dz = 0; len = 1e-5f; }
            dx /= len; dy /= len; dz /= len;
            // a basis across the axis
            float ax = Math.abs(dx) < 0.9f ? 1 : 0, ay = Math.abs(dx) < 0.9f ? 0 : 1, az = 0;
            float ux = dy * az - dz * ay, uy = dz * ax - dx * az, uz = dx * ay - dy * ax;
            float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz); ux /= ul; uy /= ul; uz /= ul;
            float wx = dy * uz - dz * uy, wy = dz * ux - dx * uz, wz = dx * uy - dy * ux;
            int around = Math.max(6, seg * 2), capRings = caps ? Math.max(2, seg / 2) : 0;
            float slope = (r0 - r1) / len;      // the taper tilts the side normals along the axis
            int base = nv, rings = 0;
            // the cap at the start
            for (int i = 0; i < capRings; i++) {
                double ph = -Math.PI / 2 + Math.PI / 2 * i / capRings;
                float along = (float) Math.sin(ph) * r0, rr = (float) Math.cos(ph) * r0;
                ring(x0 + dx * along, y0 + dy * along, z0 + dz * along, rr, ux, uy, uz, wx, wy, wz, dx, dy, dz, (float) Math.sin(ph), around, (float) Math.cos(ph));
                rings++;
            }
            // the two ends of the side
            ring(x0, y0, z0, r0, ux, uy, uz, wx, wy, wz, dx, dy, dz, slope, around, 1f); rings++;
            ring(x1, y1, z1, r1, ux, uy, uz, wx, wy, wz, dx, dy, dz, slope, around, 1f); rings++;
            // the cap at the end
            for (int i = 1; i <= capRings; i++) {
                double ph = Math.PI / 2 * i / capRings;
                float along = (float) Math.sin(ph) * r1, rr = (float) Math.cos(ph) * r1;
                ring(x1 + dx * along, y1 + dy * along, z1 + dz * along, rr, ux, uy, uz, wx, wy, wz, dx, dy, dz, (float) Math.sin(ph), around, (float) Math.cos(ph));
                rings++;
            }
            grid(base, rings - 1, around, m, false);
            if (!caps) {
                // flat ends
                int c0 = vertex(x0, y0, z0, -dx, -dy, -dz), c1 = vertex(x1, y1, z1, dx, dy, dz);
                for (int j = 0; j < around; j++) {
                    tri(c0, base + (capRings) * (around + 1) + j, base + (capRings) * (around + 1) + j + 1, m);
                    int e = base + (capRings + 1) * (around + 1);
                    tri(c1, e + j + 1, e + j, m);
                }
            }
        }

        /** One ring of vertices round an axis; the normal leans along the axis by 'lean' (a taper or a cap). */
        private void ring(float cx, float cy, float cz, float r, float ux, float uy, float uz, float wx, float wy, float wz, float dx, float dy, float dz, float lean, int around, float radial) {
            for (int j = 0; j <= around; j++) {
                double th = 2 * Math.PI * j / around;
                float c = (float) Math.cos(th), s = (float) Math.sin(th);
                float rx = ux * c + wx * s, ry = uy * c + wy * s, rz = uz * c + wz * s;
                vertex(cx + rx * r, cy + ry * r, cz + rz * r, rx * radial + dx * lean, ry * radial + dy * lean, rz * radial + dz * lean);
            }
        }

        private void grid(int base, int rows, int around, int m, boolean flip) {
            for (int i = 0; i < rows; i++) for (int j = 0; j < around; j++) {
                int a = base + i * (around + 1) + j, b = a + 1, c = a + around + 1, d = c + 1;
                if (flip) { tri(a, c, b, m); tri(b, c, d, m); } else { tri(a, b, c, m); tri(b, d, c, m); }
            }
        }

        /** An axis-aligned box centred at (cx, cy, cz) with full sizes sx, sy, sz. */
        public void box(float cx, float cy, float cz, float sx, float sy, float sz, int m) {
            float hx = sx / 2, hy = sy / 2, hz = sz / 2;
            float[][] f = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (float[] nn : f) {
                float nx = nn[0], ny = nn[1], nz = nn[2];
                // two axes in the face
                float ax = ny != 0 || nz != 0 ? 1 : 0, ay = nx != 0 ? 1 : 0, az = 0;
                float bx = ny * az - nz * ay, by = nz * ax - nx * az, bz = nx * ay - ny * ax;
                int i0 = vertex(cx + nx * hx - ax * hx - bx * hx, cy + ny * hy - ay * hy - by * hy, cz + nz * hz - az * hz - bz * hz, nx, ny, nz);
                int i1 = vertex(cx + nx * hx + ax * hx - bx * hx, cy + ny * hy + ay * hy - by * hy, cz + nz * hz + az * hz - bz * hz, nx, ny, nz);
                int i2 = vertex(cx + nx * hx + ax * hx + bx * hx, cy + ny * hy + ay * hy + by * hy, cz + nz * hz + az * hz + bz * hz, nx, ny, nz);
                int i3 = vertex(cx + nx * hx - ax * hx + bx * hx, cy + ny * hy - ay * hy + by * hy, cz + nz * hz - az * hz + bz * hz, nx, ny, nz);
                tri(i0, i1, i2, m); tri(i0, i2, i3, m);
            }
        }

        /** A ring (a bangle, a turban's roll): big radius R round the axis (0 x, 1 y, 2 z), tube radius r. */
        public void torus(float cx, float cy, float cz, float R, float r, int axis, int seg, int m) {
            int big = Math.max(8, seg * 2), small = Math.max(5, seg);
            int base = nv;
            for (int i = 0; i <= big; i++) {
                double th = 2 * Math.PI * i / big;
                float c = (float) Math.cos(th), s = (float) Math.sin(th);
                for (int j = 0; j <= small; j++) {
                    double ph = 2 * Math.PI * j / small;
                    float cp = (float) Math.cos(ph), sp = (float) Math.sin(ph);
                    float px, py, pz, nx, ny, nz;
                    if (axis == 1) { px = (R + r * cp) * c; pz = (R + r * cp) * s; py = r * sp; nx = cp * c; nz = cp * s; ny = sp; }
                    else if (axis == 0) { py = (R + r * cp) * c; pz = (R + r * cp) * s; px = r * sp; ny = cp * c; nz = cp * s; nx = sp; }
                    else { px = (R + r * cp) * c; py = (R + r * cp) * s; pz = r * sp; nx = cp * c; ny = cp * s; nz = sp; }
                    vertex(cx + px, cy + py, cz + pz, nx, ny, nz);
                }
            }
            grid(base, big, small, m, false);
        }

        /** A flat disc round the axis (0 x, 1 y, 2 z); normal along the positive axis. */
        public void disc(float cx, float cy, float cz, float r, int axis, int seg, int m) {
            int around = Math.max(8, seg * 2);
            float nx = axis == 0 ? 1 : 0, ny = axis == 1 ? 1 : 0, nz = axis == 2 ? 1 : 0;
            int c = vertex(cx, cy, cz, nx, ny, nz);
            int base = nv;
            for (int j = 0; j <= around; j++) {
                double th = 2 * Math.PI * j / around;
                float a = (float) Math.cos(th) * r, b = (float) Math.sin(th) * r;
                if (axis == 1) vertex(cx + a, cy, cz + b, nx, ny, nz);
                else if (axis == 0) vertex(cx, cy + a, cz + b, nx, ny, nz);
                else vertex(cx + a, cy + b, cz, nx, ny, nz);
            }
            for (int j = 0; j < around; j++) tri(c, base + j + 1, base + j, m);
        }

        /** A flattened ellipsoid lying on its side: a leaf, a wing, an ear. Thickness along z. */
        public void blob(float cx, float cy, float cz, float rx, float ry, float rz, int seg, int m) { sphere(cx, cy, cz, rx, ry, rz, seg, m); }
    }

    // ================================================================== the scene

    public static final class Scene {
        public final Mesh mesh = new Mesh();
        public float camX, camY = 1f, camZ = 10f, lookX, lookY = 1f, lookZ, fovDeg = 30f;
        /** Direction toward the key light (from the subject) and toward the rim light. */
        public float keyX = -0.45f, keyY = 1f, keyZ = 0.75f, rimX = 0.7f, rimY = 0.45f, rimZ = -0.6f;
        public int keyColor = 0xFFFFF1DA, fillColor = 0xFF9FB4CC, rimColor = 0xFFFFE9C9, skyColor = 0xFFC4DCF4, groundColor = 0xFF8C7C5E;
        public float keyStrength = 1.15f, fillStrength = 0.42f, rimStrength = 0.55f, ambient = 0.34f;
        /** 0 = transparent background (a character); else the sky's top and bottom colours (a place). */
        public int skyTop = 0, skyBottom = 0;
        public float fogStart = 40f, fogEnd = 160f;
        public int fogColor = 0;
        public float dof = 0f;
        public boolean shadows = true, ao = true;
        public float exposure = 1f;
        /** The reach of the ambient occlusion in world units (a fraction of the subject's size). */
        public float aoRadius = 0.05f;
        /** The shadow map covers only this far around (shadowX, shadowY, shadowZ) when shadowRadius > 0 (a big place keeps its shadows sharp near the stage). */
        public float shadowRadius = 0, shadowX, shadowY, shadowZ;
        /** Named points to project after rendering (eyes, mouth, the floor): {x, y, z} each. */
        public final List<float[]> marks = new ArrayList<float[]>();
        public int mark(float x, float y, float z) { marks.add(new float[]{x, y, z}); return marks.size() - 1; }
    }

    /** A rendered picture: ARGB pixels and where the marks landed (fractions of the picture). */
    public static final class Picture {
        public int[] px;
        public int w, h;
        public float[][] marks;
    }

    // ================================================================== rendering

    private static final int SHADOW = 1024;

    /** Renders the scene at w x h with ss x ss supersampling (2 is plenty). */
    public static Picture render(Scene s, int w, int h, int ss) {
        ss = Math.max(1, Math.min(3, ss));
        int W = w * ss, H = h * ss;
        Mesh m = s.mesh;
        // ---- the camera
        float fx = s.lookX - s.camX, fy = s.lookY - s.camY, fz = s.lookZ - s.camZ;
        float fl = len(fx, fy, fz); fx /= fl; fy /= fl; fz /= fl;
        float rx = fy * 0 - fz * 1, ry = fz * 0 - fx * 0, rz = fx * 1 - fy * 0;     // f x up(0,1,0)
        float rl = len(rx, ry, rz); rx /= rl; ry /= rl; rz /= rl;
        float ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;  // r x f
        float focal = (H / 2f) / (float) Math.tan(Math.toRadians(s.fovDeg) / 2);
        // view-space vertices: (vx right, vy up, vz forward distance)
        int nv = m.nv;
        float[] sx = new float[nv], sy = new float[nv], sw = new float[nv];
        for (int i = 0; i < nv; i++) {
            float x = m.v[i * 3] - s.camX, y = m.v[i * 3 + 1] - s.camY, z = m.v[i * 3 + 2] - s.camZ;
            float vx = x * rx + y * ry + z * rz, vy = x * ux + y * uy + z * uz, vz = x * fx + y * fy + z * fz;
            if (vz < 0.05f) { sw[i] = 0; continue; }
            sw[i] = 1f / vz;
            sx[i] = W / 2f + vx * focal / vz;
            sy[i] = H / 2f - vy * focal / vz;
        }
        // ---- the shadow map from the key light (orthographic)
        float kl = len(s.keyX, s.keyY, s.keyZ);
        float lx = s.keyX / kl, ly = s.keyY / kl, lz = s.keyZ / kl;    // toward the light
        float[] shadow = null;
        float lax = 0, lay = 0, laz = 0, lbx = 0, lby = 0, lbz = 0, lminA = 0, lminB = 0, lscale = 1;
        if (s.shadows) {
            // a basis across the light
            float tx = Math.abs(ly) < 0.9f ? 0 : 1, ty = Math.abs(ly) < 0.9f ? 1 : 0;
            lax = ly * 0 - lz * ty; lay = lz * tx - lx * 0; laz = lx * ty - ly * tx;
            float al = len(lax, lay, laz); lax /= al; lay /= al; laz /= al;
            lbx = ly * laz - lz * lay; lby = lz * lax - lx * laz; lbz = lx * lay - ly * lax;
            float minA = 1e9f, maxA = -1e9f, minB = 1e9f, maxB = -1e9f;
            float[] la = new float[nv], lb = new float[nv], ld = new float[nv];
            for (int i = 0; i < nv; i++) {
                float x = m.v[i * 3], y = m.v[i * 3 + 1], z = m.v[i * 3 + 2];
                la[i] = x * lax + y * lay + z * laz; lb[i] = x * lbx + y * lby + z * lbz; ld[i] = -(x * lx + y * ly + z * lz);
                if (la[i] < minA) minA = la[i]; if (la[i] > maxA) maxA = la[i];
                if (lb[i] < minB) minB = lb[i]; if (lb[i] > maxB) maxB = lb[i];
            }
            float margin = 0.02f * Math.max(maxA - minA, maxB - minB) + 0.01f;
            if (s.shadowRadius > 0) {
                float ca = s.shadowX * lax + s.shadowY * lay + s.shadowZ * laz, cb = s.shadowX * lbx + s.shadowY * lby + s.shadowZ * lbz;
                minA = Math.max(minA, ca - s.shadowRadius); maxA = Math.min(maxA, ca + s.shadowRadius);
                minB = Math.max(minB, cb - s.shadowRadius); maxB = Math.min(maxB, cb + s.shadowRadius);
                margin = 0.01f;
            }
            lminA = minA - margin; lminB = minB - margin;
            lscale = (SHADOW - 2) / Math.max(1e-3f, Math.max(maxA - minA + 2 * margin, maxB - minB + 2 * margin));
            shadow = new float[SHADOW * SHADOW];
            java.util.Arrays.fill(shadow, 1e9f);
            for (int ti = 0; ti < m.nt; ti++) {
                int a = m.t[ti * 3], b = m.t[ti * 3 + 1], c = m.t[ti * 3 + 2];
                depthTri(shadow, SHADOW, SHADOW, (la[a] - lminA) * lscale, (lb[a] - lminB) * lscale, ld[a], (la[b] - lminA) * lscale, (lb[b] - lminB) * lscale, ld[b],
                        (la[c] - lminA) * lscale, (lb[c] - lminB) * lscale, ld[c]);
            }
        }
        // ---- the main pass: forward shading with a depth buffer
        float[] depth = new float[W * H];
        java.util.Arrays.fill(depth, 1e9f);
        int[] col = new int[W * H];
        int[] nrm = new int[W * H];
        Shader sh = new Shader(s, lx, ly, lz, shadow, lax, lay, laz, lbx, lby, lbz, lminA, lminB, lscale);
        for (int ti = 0; ti < m.nt; ti++) {
            int a = m.t[ti * 3], b = m.t[ti * 3 + 1], c = m.t[ti * 3 + 2];
            if (sw[a] == 0 || sw[b] == 0 || sw[c] == 0) continue;
            // back faces away from the camera are not drawn (closed solids)
            float ex = sx[b] - sx[a], ey = sy[b] - sy[a], gx = sx[c] - sx[a], gy = sy[c] - sy[a];
            float area = ex * gy - ey * gx;
            if (area >= 0) continue;
            shadeTri(m, ti, a, b, c, sx, sy, sw, W, H, depth, col, nrm, sh);
        }
        // ---- ambient occlusion from the depth buffer
        if (s.ao) ambientOcclusion(depth, nrm, col, W, H, focal, s.aoRadius);
        // ---- the sky, the fog, the depth of field
        if (s.skyTop != 0) {
            for (int y = 0; y < H; y++) {
                int sky = mix(s.skyTop, s.skyBottom, y / (float) (H - 1));
                for (int x = 0; x < W; x++) {
                    int i = y * W + x;
                    if (depth[i] >= 1e8f) { col[i] = sky; continue; }
                    if (s.fogColor != 0) {
                        float f = clamp((depth[i] - s.fogStart) / Math.max(1f, s.fogEnd - s.fogStart));
                        if (f > 0) col[i] = mix(col[i], s.fogColor, f * f * 0.85f);
                    }
                }
            }
            if (s.dof > 0) depthOfField(col, depth, W, H, s.dof * ss);
        }
        // ---- down to the picture (the alpha is the coverage)
        Picture p = new Picture();
        p.w = w; p.h = h;
        p.px = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int j = 0; j < ss; j++) for (int i = 0; i < ss; i++) {
                int c = col[(y * ss + j) * W + x * ss + i];
                int a = c >>> 24;
                sa += a; sr += (c >> 16 & 255) * a; sg += (c >> 8 & 255) * a; sb += (c & 255) * a;
            }
            int n = ss * ss;
            if (sa == 0) { p.px[y * w + x] = 0; continue; }
            p.px[y * w + x] = (sa / n) << 24 | (sr / sa) << 16 | (sg / sa) << 8 | (sb / sa);
        }
        // ---- the marks
        p.marks = new float[s.marks.size()][];
        for (int i = 0; i < s.marks.size(); i++) {
            float[] k = s.marks.get(i);
            float x = k[0] - s.camX, y = k[1] - s.camY, z = k[2] - s.camZ;
            float vx = x * rx + y * ry + z * rz, vy = x * ux + y * uy + z * uz, vz = x * fx + y * fy + z * fz;
            p.marks[i] = new float[]{(w / 2f + vx * (focal / ss) / vz) / w, (h / 2f - vy * (focal / ss) / vz) / h};
        }
        return p;
    }

    /** Crops a transparent picture to what is drawn (plus a margin), moving the marks with it. */
    public static Picture crop(Picture p, int margin) {
        int x0 = p.w, y0 = p.h, x1 = -1, y1 = -1;
        for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) if ((p.px[y * p.w + x] >>> 24) > 8) {
            if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y;
        }
        if (x1 < 0) return p;
        x0 = Math.max(0, x0 - margin); y0 = Math.max(0, y0 - margin); x1 = Math.min(p.w - 1, x1 + margin); y1 = Math.min(p.h - 1, y1 + margin);
        Picture o = new Picture();
        o.w = x1 - x0 + 1; o.h = y1 - y0 + 1;
        o.px = new int[o.w * o.h];
        for (int y = 0; y < o.h; y++) System.arraycopy(p.px, (y + y0) * p.w + x0, o.px, y * o.w, o.w);
        o.marks = new float[p.marks.length][];
        for (int i = 0; i < p.marks.length; i++) o.marks[i] = new float[]{(p.marks[i][0] * p.w - x0) / o.w, (p.marks[i][1] * p.h - y0) / o.h};
        return o;
    }

    // ------------------------------------------------------------------ rasterizing

    private static void depthTri(float[] buf, int W, int H, float x0, float y0, float d0, float x1, float y1, float d1, float x2, float y2, float d2) {
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)))), maxX = Math.min(W - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)))), maxY = Math.min(H - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        float area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (Math.abs(area) < 1e-6f) return;
        float inv = 1f / area;
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = ((x1 - px) * (y2 - py) - (y1 - py) * (x2 - px)) * inv;
                float w1 = ((x2 - px) * (y0 - py) - (y2 - py) * (x0 - px)) * inv;
                float w2 = 1 - w0 - w1;
                if (w0 < -1e-4f || w1 < -1e-4f || w2 < -1e-4f) continue;
                float d = w0 * d0 + w1 * d1 + w2 * d2;
                int i = y * W + x;
                if (d < buf[i]) buf[i] = d;
            }
        }
    }

    private static void shadeTri(Mesh m, int ti, int a, int b, int c, float[] sx, float[] sy, float[] sw, int W, int H, float[] depth, int[] col, int[] nrm, Shader sh) {
        float x0 = sx[a], y0 = sy[a], x1 = sx[b], y1 = sy[b], x2 = sx[c], y2 = sy[c];
        int minX = Math.max(0, (int) Math.floor(Math.min(x0, Math.min(x1, x2)))), maxX = Math.min(W - 1, (int) Math.ceil(Math.max(x0, Math.max(x1, x2))));
        int minY = Math.max(0, (int) Math.floor(Math.min(y0, Math.min(y1, y2)))), maxY = Math.min(H - 1, (int) Math.ceil(Math.max(y0, Math.max(y1, y2))));
        if (minX > maxX || minY > maxY) return;
        float area = (x1 - x0) * (y2 - y0) - (y1 - y0) * (x2 - x0);
        if (Math.abs(area) < 1e-7f) return;
        float inv = 1f / area;
        Material mat = m.mats.get(m.tm[ti]);
        float wa = sw[a], wb = sw[b], wc = sw[c];
        for (int y = minY; y <= maxY; y++) {
            float py = y + 0.5f;
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float w0 = ((x1 - px) * (y2 - py) - (y1 - py) * (x2 - px)) * inv;
                float w1 = ((x2 - px) * (y0 - py) - (y2 - py) * (x0 - px)) * inv;
                float w2 = 1 - w0 - w1;
                if (w0 < -2e-4f || w1 < -2e-4f || w2 < -2e-4f) continue;
                // perspective-correct weights
                float pa = w0 * wa, pb = w1 * wb, pc = w2 * wc, ps = pa + pb + pc;
                if (ps <= 0) continue;
                float z = 1f / ps;
                int i = y * W + x;
                if (z >= depth[i]) continue;
                pa /= ps; pb /= ps; pc /= ps;
                float wx = pa * m.v[a * 3] + pb * m.v[b * 3] + pc * m.v[c * 3];
                float wy = pa * m.v[a * 3 + 1] + pb * m.v[b * 3 + 1] + pc * m.v[c * 3 + 1];
                float wz = pa * m.v[a * 3 + 2] + pb * m.v[b * 3 + 2] + pc * m.v[c * 3 + 2];
                float nx = pa * m.n[a * 3] + pb * m.n[b * 3] + pc * m.n[c * 3];
                float ny = pa * m.n[a * 3 + 1] + pb * m.n[b * 3 + 1] + pc * m.n[c * 3 + 1];
                float nz = pa * m.n[a * 3 + 2] + pb * m.n[b * 3 + 2] + pc * m.n[c * 3 + 2];
                float nl = len(nx, ny, nz); nx /= nl; ny /= nl; nz /= nl;
                depth[i] = z;
                col[i] = sh.shade(mat, wx, wy, wz, nx, ny, nz);
                nrm[i] = ((int) ((nx * 0.5f + 0.5f) * 255) << 16) | ((int) ((ny * 0.5f + 0.5f) * 255) << 8) | (int) ((nz * 0.5f + 0.5f) * 255);
            }
        }
    }

    // ------------------------------------------------------------------ shading

    static final class Shader {
        final Scene s;
        final float lx, ly, lz, fx, fy, fz, rmx, rmy, rmz;
        final float[] shadow;
        final float lax, lay, laz, lbx, lby, lbz, lminA, lminB, lscale;
        final float kr, kg, kb, fr, fg, fb, rr, rg, rb, skr, skg, skb, gr, gg, gb;

        Shader(Scene s, float lx, float ly, float lz, float[] shadow, float lax, float lay, float laz, float lbx, float lby, float lbz, float lminA, float lminB, float lscale) {
            this.s = s; this.lx = lx; this.ly = ly; this.lz = lz; this.shadow = shadow;
            this.lax = lax; this.lay = lay; this.laz = laz; this.lbx = lbx; this.lby = lby; this.lbz = lbz; this.lminA = lminA; this.lminB = lminB; this.lscale = lscale;
            // the bounce comes up from the ground on the far side of the key
            float bl = len(-lx, 0.6f, -lz * 0.5f + 0.3f);
            fx = -lx / bl; fy = 0.6f / bl; fz = (-lz * 0.5f + 0.3f) / bl;
            float rl = len(s.rimX, s.rimY, s.rimZ);
            rmx = s.rimX / rl; rmy = s.rimY / rl; rmz = s.rimZ / rl;
            kr = (s.keyColor >> 16 & 255) / 255f * s.keyStrength; kg = (s.keyColor >> 8 & 255) / 255f * s.keyStrength; kb = (s.keyColor & 255) / 255f * s.keyStrength;
            fr = (s.fillColor >> 16 & 255) / 255f * s.fillStrength; fg = (s.fillColor >> 8 & 255) / 255f * s.fillStrength; fb = (s.fillColor & 255) / 255f * s.fillStrength;
            rr = (s.rimColor >> 16 & 255) / 255f * s.rimStrength; rg = (s.rimColor >> 8 & 255) / 255f * s.rimStrength; rb = (s.rimColor & 255) / 255f * s.rimStrength;
            skr = (s.skyColor >> 16 & 255) / 255f * s.ambient; skg = (s.skyColor >> 8 & 255) / 255f * s.ambient; skb = (s.skyColor & 255) / 255f * s.ambient;
            gr = (s.groundColor >> 16 & 255) / 255f * s.ambient; gg = (s.groundColor >> 8 & 255) / 255f * s.ambient; gb = (s.groundColor & 255) / 255f * s.ambient;
        }

        /** How much of the key light reaches a point (0 shadow .. 1 lit), filtered over 3x3 texels. */
        float lit(float x, float y, float z, float nx, float ny, float nz) {
            if (shadow == null) return 1f;
            float ndl = nx * lx + ny * ly + nz * lz;
            if (ndl <= 0.02f) return 0.25f;                 // facing away from the light: in its own shadow
            // the map's texel in world units: the surface is moved out along its normal by a texel and a half, and the
            // depth test allows a texel more per unit of slope, so a surface never shadows itself (no acne)
            float texel = 1f / lscale;
            float slope = Math.min(8f, (float) Math.sqrt(Math.max(0, 1 - ndl * ndl)) / Math.max(0.08f, ndl));
            float off = texel * 1.5f;
            x += nx * off; y += ny * off; z += nz * off;
            float bias = texel * (1.5f + 2.5f * slope);
            float a = (x * lax + y * lay + z * laz - lminA) * lscale, b = (x * lbx + y * lby + z * lbz - lminB) * lscale;
            float d = -(x * lx + y * ly + z * lz);
            int ia = (int) a, ib = (int) b;
            if (ia < 1 || ib < 1 || ia >= SHADOW - 1 || ib >= SHADOW - 1) return 1f;
            float sum = 0;
            for (int j = -1; j <= 1; j++) for (int i = -1; i <= 1; i++) {
                float sd = shadow[(ib + j) * SHADOW + ia + i];
                sum += d - bias <= sd ? 1 : 0;
            }
            return sum / 9f;
        }

        int shade(Material m, float x, float y, float z, float nx, float ny, float nz) {
            float br = (m.color >> 16 & 255) / 255f, bg = (m.color >> 8 & 255) / 255f, bb = (m.color & 255) / 255f;
            // the view direction
            float vx = s.camX - x, vy = s.camY - y, vz = s.camZ - z;
            float vl = len(vx, vy, vz); vx /= vl; vy /= vl; vz /= vl;
            float ndl = nx * lx + ny * ly + nz * lz;
            float wrap = m.wrap;
            float diff = clamp((ndl + wrap) / (1 + wrap));
            float shade = lit(x, y, z, nx, ny, nz);
            diff *= 0.12f + 0.88f * shade;
            // the bounce from the ground, the sky and the ground ambient
            float fill = clamp(nx * fx + ny * fy + nz * fz) * 0.9f + 0.1f;
            float up = ny * 0.5f + 0.5f;
            float ar = skr * up + gr * (1 - up), ag = skg * up + gg * (1 - up), ab = skb * up + gb * (1 - up);
            // specular (Blinn-Phong), tinted by the colour on metal
            float hx = lx + vx, hy = ly + vy, hz = lz + vz;
            float hl = len(hx, hy, hz); hx /= hl; hy /= hl; hz /= hl;
            float ndh = Math.max(0, nx * hx + ny * hy + nz * hz);
            float spec = (float) Math.pow(ndh, m.gloss) * m.spec * shade * (0.5f + 0.5f * clamp(ndl * 2 + 0.5f));
            float sr = spec, sg = spec, sb = spec;
            if (m.metal > 0) { sr *= br * m.metal + (1 - m.metal); sg *= bg * m.metal + (1 - m.metal); sb *= bb * m.metal + (1 - m.metal); }
            float dk = 1 - 0.7f * m.metal;
            // the rim: the edge against the view, from the rim light's side
            float ndv = Math.max(0, nx * vx + ny * vy + nz * vz);
            float fres = (float) Math.pow(1 - ndv, 3);
            float rimSide = clamp(nx * rmx + ny * rmy + nz * rmz) * 0.7f + 0.3f;
            float rim = fres * rimSide * m.rim;
            // subsurface warmth where the light grazes or leaves the skin
            float sss = m.sss * clamp(0.55f - ndl * 0.5f) * (0.4f + 0.6f * shade);
            float r = br * (kr * diff * dk + fr * fill * dk + ar) + sr * kr + rim * rr + sss * br * 1.1f;
            float g = bg * (kg * diff * dk + fg * fill * dk + ag) + sg * kg + rim * rg + sss * bg * 0.55f;
            float b = bb * (kb * diff * dk + fb * fill * dk + ab) + sb * kb + rim * rb + sss * bb * 0.4f;
            if (m.emissive != 0) {
                float ea = (m.emissive >>> 24) / 255f;
                r += (m.emissive >> 16 & 255) / 255f * ea; g += (m.emissive >> 8 & 255) / 255f * ea; b += (m.emissive & 255) / 255f * ea;
            }
            r *= s.exposure; g *= s.exposure; b *= s.exposure;
            return 0xFF000000 | tone(r) << 16 | tone(g) << 8 | tone(b);
        }
    }

    /** A filmic shoulder: the brights roll off instead of clipping. */
    static int tone(float c) {
        if (c < 0) c = 0;
        float t = c < 0.8f ? c : 0.8f + (c - 0.8f) / (1 + (c - 0.8f) * 2.2f);
        return Math.min(255, (int) (t * 255 + 0.5f));
    }

    // ------------------------------------------------------------------ ambient occlusion and depth of field

    private static final float[] AO_KERNEL = {
            0.2f, 0.1f, 0.4f, -0.3f, 0.25f, 0.2f, 0.1f, -0.4f, 0.35f, -0.15f, 0.5f, 0.1f, 0.45f, 0.3f, -0.2f, -0.5f, 0.2f, 0.3f,
            0.05f, 0.6f, 0.2f, 0.3f, -0.1f, 0.55f, -0.4f, 0.4f, 0.1f, 0.25f, 0.45f, 0.45f, -0.25f, 0.05f, 0.6f, 0.15f, 0.35f, -0.5f};

    private static void ambientOcclusion(float[] depth, int[] nrm, int[] col, int W, int H, float focal, float aoRadius) {
        float[] occ = new float[W * H];
        int n = AO_KERNEL.length / 3;
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x;
            float d = depth[i];
            if (d >= 1e8f) continue;
            float nx = (nrm[i] >> 16 & 255) / 127.5f - 1, ny = (nrm[i] >> 8 & 255) / 127.5f - 1, nz = (nrm[i] & 255) / 127.5f - 1;
            // the point in view space
            float px = (x + 0.5f - W / 2f) * d / focal, py = -(y + 0.5f - H / 2f) * d / focal;
            float radius = aoRadius * (0.7f + 0.3f * Math.min(4f, d / 10f));
            int hits = 0, tried = 0;
            for (int k = 0; k < n; k++) {
                float kx = AO_KERNEL[k * 3], ky = AO_KERNEL[k * 3 + 1], kz = AO_KERNEL[k * 3 + 2];
                // flip into the normal's hemisphere and lean the sample along the normal: a sample skimming a smooth
                // convex surface must not count it as a crease (the normal is in world space; near enough for a front view)
                float dot = kx * nx + ky * ny + kz * nz;
                if (dot < 0) { kx = -kx; ky = -ky; kz = -kz; }
                kx += nx * 0.8f; ky += ny * 0.8f; kz += nz * 0.8f;
                float kl = len(kx, ky, kz), scale = (0.35f + 0.65f * ((k * 7) % n) / (float) n) * radius / kl;
                float qx = px + kx * scale, qy = py + ky * scale, qz = d - kz * scale;    // kz toward the camera shortens the depth
                if (qz < 0.05f) continue;
                int qsx = (int) (W / 2f + qx * focal / qz), qsy = (int) (H / 2f - qy * focal / qz);
                if (qsx < 0 || qsy < 0 || qsx >= W || qsy >= H) continue;
                tried++;
                float sd = depth[qsy * W + qsx];
                if (sd < qz - radius * 0.25f && qz - sd < radius * 2.5f) hits++;
            }
            occ[i] = tried == 0 ? 0 : hits / (float) tried;
        }
        // a small blur, then darken
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x;
            if (depth[i] >= 1e8f) continue;
            float sum = 0; int cnt = 0;
            for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
                int xx = x + k, yy = y + j;
                if (xx < 0 || yy < 0 || xx >= W || yy >= H || depth[yy * W + xx] >= 1e8f) continue;
                sum += occ[yy * W + xx]; cnt++;
            }
            float o = cnt == 0 ? 0 : sum / cnt;
            float k = 1 - 0.55f * o;
            int c = col[i];
            col[i] = 0xFF000000 | ((int) ((c >> 16 & 255) * k) << 16) | ((int) ((c >> 8 & 255) * k) << 8) | (int) ((c & 255) * k);
        }
    }

    private static void depthOfField(int[] col, float[] depth, int W, int H, float strength) {
        int[] out = col.clone();
        for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) {
            int i = y * W + x;
            float d = depth[i];
            int r = d >= 1e8f ? (int) (strength * 2) : (int) Math.min(strength * 2, Math.max(0, (d - 25f) / 20f * strength));
            if (r <= 0) continue;
            int sr = 0, sg = 0, sb = 0, n = 0;
            for (int j = -r; j <= r; j += Math.max(1, r / 2)) for (int k = -r; k <= r; k += Math.max(1, r / 2)) {
                int xx = x + k, yy = y + j;
                if (xx < 0 || yy < 0 || xx >= W || yy >= H) continue;
                int c = col[yy * W + xx];
                sr += c >> 16 & 255; sg += c >> 8 & 255; sb += c & 255; n++;
            }
            if (n > 0) out[i] = 0xFF000000 | (sr / n) << 16 | (sg / n) << 8 | (sb / n);
        }
        System.arraycopy(out, 0, col, 0, col.length);
    }

    // ------------------------------------------------------------------ small helpers

    static float len(float x, float y, float z) { return (float) Math.sqrt(x * x + y * y + z * z); }
    static float clamp(float v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    public static int mix(int a, int b, float k) {
        k = clamp(k);
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return 0xFF000000 | (int) (ar + (br - ar) * k) << 16 | (int) (ag + (bg - ag) * k) << 8 | (int) (ab + (bb - ab) * k);
    }

    public static int shade(int c, float k) {
        return 0xFF000000 | Math.min(255, (int) ((c >> 16 & 255) * k)) << 16 | Math.min(255, (int) ((c >> 8 & 255) * k)) << 8 | Math.min(255, (int) ((c & 255) * k));
    }
}
