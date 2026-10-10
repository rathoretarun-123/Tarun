package com.tarun.kahani.core;

/** Procedural music and sound effects (no downloads, works offline). Mono float samples. */
public final class Synth {
    public static final int SR = 32000;

    private long seed = 12345;

    private float rnd() {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return ((seed >>> 40) & 0xFFFFFF) / (float) 0x1000000 * 2f - 1f;
    }

    private static float midi(float n) { return (float) (440.0 * Math.pow(2, (n - 69) / 12.0)); }

    static float clamp(float v) { return v > 1 ? 1 : v < -1 ? -1 : v; }

    // ================================================================== effects

    public float[] sfx(int type, float dur) {
        seed = 777 + type * 31;
        int n = Math.max(1, (int) (dur * SR));
        float[] o = new float[n];
        switch (type) {
            case Film.SFX_STREAM: noiseBed(o, 0.06f, 0.35f, 2.5f, 0.5f); break;
            case Film.SFX_WIND: noiseBed(o, 0.015f, 0.4f, 0.25f, 0.6f); leaves(o, 0.05f); break;     // v35: with the leaves rustling
            case Film.SFX_HISS: { noiseBed(o, 0.5f, 0.25f, 6f, 0.7f); fadeEnds(o, 0.3f); break; }
            case Film.SFX_RUSTLE: { for (int i = 0; i < 6; i++) burst(o, i * 0.18f + rnd() * 0.05f, 0.18f, 0.35f, 0.45f); break; }
            case Film.SFX_BIRDS: { float t = 0.1f; while (t < dur - 0.3f) { chirp(o, t, 2200 + rnd() * 1500, 0.08f + Math.abs(rnd()) * 0.08f, 0.25f); t += 0.15f + Math.abs(rnd()) * 0.6f; } break; }
            case Film.SFX_CLACK: { float t = 0.05f; int k = 0; while (t < dur - 0.1f) { knock(o, t, 900 + (k % 2) * 250, 0.5f); t += (k % 3 == 2) ? 0.42f : 0.24f; k++; } break; }
            case Film.SFX_POP: { tone(o, 0, 0.12f, 700, 1400, 0.5f, 0); break; }
            case Film.SFX_CHIME: { int[] notes = {84, 88, 91, 96}; for (int i = 0; i < 4; i++) bellTone(o, i * 0.09f, midi(notes[i]), 1.2f, 0.18f); break; }
            case Film.SFX_MAGIC: { for (int i = 0; i < 12; i++) bellTone(o, i * 0.07f, midi(79 + i * 2 + (i % 3)), 0.7f, 0.12f); noiseBedRange(o, 0, dur, 0.6f, 0.05f); break; }
            case Film.SFX_MONKEY: { float t = 0.02f; while (t < dur - 0.12f) { tone(o, t, 0.07f + Math.abs(rnd()) * 0.05f, 1200 + rnd() * 300, 2400 + rnd() * 600, 0.35f, 1); t += 0.11f + Math.abs(rnd()) * 0.1f; } break; }
            case Film.SFX_THUD: { float t = 0; while (t < dur - 0.2f) { thump(o, t, 55, 0.9f); t += 0.55f; } break; }
            case Film.SFX_STEPS: { float t = 0; while (t < dur) { thump(o, t, 120, 0.25f); burst(o, t, 0.05f, 0.4f, 0.12f); t += 0.32f; } break; }
            // hard ground (stone, marble, a cave floor): a sharper click with a short ring, less thump
            case Film.SFX_STEPS_HARD: { float t = 0; while (t < dur) { thump(o, t, 220, 0.12f); burst(o, t, 0.012f, 0.9f, 0.22f); burst(o, t + 0.02f, 0.03f, 0.5f, 0.08f); t += 0.3f; } break; }
            case Film.SFX_STEPS_RUN: { float t = 0; while (t < dur) { thump(o, t, 110, 0.3f); burst(o, t, 0.04f, 0.5f, 0.14f); t += 0.21f; } break; }
            // ---- machines and the city
            case Film.SFX_HUM: { tone(o, 0, dur, 100, 100, 0.08f, 1); tone(o, 0, dur, 200, 200, 0.03f, 0); noiseBed(o, 0.004f, 0.1f, 0.5f, 0.3f); fadeEnds(o, 0.4f); break; }
            case Film.SFX_TRAFFIC: { noiseBed(o, 0.03f, 0.18f, 1.2f, 0.4f); lowpass(o, 0.08f); for (float t = 1.5f; t < dur - 0.5f; t += 3.5f + Math.abs(rnd()) * 4) tone(o, t, 0.25f, 420 + rnd() * 80, 400, 0.05f, 1); fadeEnds(o, 0.5f); break; }
            case Film.SFX_DRONE: { for (float t = 0; t < dur; t += 0.02f) tone(o, t, 0.025f, 210 + (float) Math.sin(t * 3) * 25 + rnd() * 6, 210, 0.07f * (float) Math.sin(Math.PI * Math.min(1, t / dur)), 1); break; }
            case Film.SFX_BEEP: { float t = 0.02f; int k = 0; while (t < dur - 0.08f) { tone(o, t, 0.09f, k % 2 == 0 ? 880 : 1175, k % 2 == 0 ? 880 : 1175, 0.28f, 1); t += 0.16f; k++; } break; }
            case Film.SFX_CLICK: { knock(o, 0.01f, 2600, 0.5f); burst(o, 0.01f, 0.02f, 0.95f, 0.3f); if (dur > 0.3f) knock(o, 0.18f, 1900, 0.3f); break; }
            case Film.SFX_TYPING: { float t = 0.02f; while (t < dur - 0.05f) { knock(o, t, 1800 + rnd() * 600, 0.14f); burst(o, t, 0.012f, 0.9f, 0.08f); t += 0.07f + Math.abs(rnd()) * 0.08f; } break; }
            case Film.SFX_BUZZ: { for (float t = 0; t < dur; t += 0.02f) tone(o, t, 0.025f, 150 + rnd() * 15, 150, 0.12f, 1); fadeEnds(o, 0.15f); break; }
            case Film.SFX_GLITCH: { for (float t = 0; t < dur; t += 0.06f + Math.abs(rnd()) * 0.1f) { burst(o, t, 0.03f + Math.abs(rnd()) * 0.05f, 0.3f + Math.abs(rnd()) * 0.6f, 0.3f); tone(o, t, 0.04f, 300 + Math.abs(rnd()) * 2500, 200, 0.15f, 1); } break; }
            case Film.SFX_POWER_DOWN: { tone(o, 0, Math.min(dur, 1.4f), 520, 60, 0.3f, 1); noiseBed(o, 0.02f, 0.3f, 2f, 0.4f); fadeEnds(o, 0.3f); break; }
            case Film.SFX_SPARK: { for (float t = 0; t < Math.min(dur, 0.6f); t += 0.03f + Math.abs(rnd()) * 0.05f) burst(o, t, 0.01f, 0.98f, 0.5f); tone(o, 0, 0.3f, 3000, 2200, 0.08f, 1); break; }
            case Film.SFX_HEARTBEAT: { float t = 0.1f; while (t < dur - 0.3f) { thump(o, t, 55, 0.7f); thump(o, t + 0.22f, 50, 0.45f); t += 0.95f; } break; }
            case Film.SFX_TWINKLE: { for (int i = 0; i < Math.max(3, (int) (dur * 4)); i++) bellTone(o, i * 0.11f + Math.abs(rnd()) * 0.08f, midi(88 + (int) (Math.abs(rnd()) * 12)), 0.5f, 0.12f); break; }
            case Film.SFX_BLIP: { tone(o, 0, 0.09f, 660, 990, 0.3f, 0); tone(o, 0.1f, 0.14f, 990, 1320, 0.3f, 0); break; }
            case Film.SFX_CRACKLE: { for (float t = 0; t < dur; t += 0.04f + Math.abs(rnd()) * 0.12f) burst(o, t, 0.015f, 0.8f, 0.25f); noiseBed(o, 0.006f, 0.2f, 0.8f, 0.5f); fadeEnds(o, 0.1f); break; }
            case Film.SFX_HISS_SHORT: { noiseBed(o, 0.3f, 0.3f, 3f, 0.6f); fadeEnds(o, Math.min(0.25f, dur / 2)); break; }
            case Film.SFX_TAP: { float t = 0; while (t < dur) { knock(o, t, 1400, 0.3f); t += 0.26f; } break; }
            // v34: a walking stick — a slow step, and on every other step the stick's dry wooden tap a moment before the foot
            case Film.SFX_STICK: { float t = 0.05f; int k = 0; while (t < dur) { if (k % 2 == 0) { knock(o, t - 0.04f, 720, 0.32f); burst(o, t - 0.04f, 0.01f, 0.95f, 0.12f); } thump(o, t, 120, 0.2f); burst(o, t, 0.05f, 0.35f, 0.08f); t += 0.44f; k++; } break; }
            // crutches — the two rubber tips land together (a dull knock), the body swings, then the one foot
            case Film.SFX_CRUTCH: { float t = 0.05f; while (t < dur) { knock(o, t, 380, 0.3f); knock(o, t + 0.015f, 340, 0.25f); thump(o, t + 0.3f, 115, 0.22f); burst(o, t + 0.3f, 0.05f, 0.35f, 0.08f); t += 0.62f; } break; }
            // a wheelchair rolling — low tyre rumble and a soft tick of the hand-rims each push
            case Film.SFX_WHEELCHAIR: { noiseBed(o, 0.02f, 0.14f, 1.6f, 0.4f); lowpass(o, 0.12f); float t = 0.1f; while (t < dur - 0.1f) { knock(o, t, 1500, 0.06f); burst(o, t + 0.02f, 0.03f, 0.3f, 0.04f); t += 0.7f; } fadeEnds(o, 0.25f); break; }
            // rain on an umbrella — many soft, dull drops on taut fabric over a light hiss
            case Film.SFX_UMBRELLA_RAIN: { float t = 0.02f; while (t < dur - 0.05f) { knock(o, t, 520 + Math.abs(rnd()) * 380, 0.05f + Math.abs(rnd()) * 0.05f); t += 0.012f + Math.abs(rnd()) * 0.045f; } noiseBedRange(o, 0, dur, 0.25f, 0.04f); fadeEnds(o, 0.4f); break; }
            // v34: a door — the latch clicks, the hinge creaks as it swings, then the door shuts with a soft wooden thump
            case Film.SFX_DOOR: {
                knock(o, 0.02f, 2400, 0.22f); burst(o, 0.02f, 0.015f, 0.95f, 0.15f);
                float cd = Math.min(0.55f, Math.max(0.2f, dur * 0.45f));
                for (float t = 0.09f; t < 0.09f + cd; t += 0.03f) {
                    float u = (t - 0.09f) / cd;
                    tone(o, t, 0.035f, 250 + 150 * (float) Math.sin(u * Math.PI) + rnd() * 25, 262 + 150 * (float) Math.sin(Math.min(1f, u + 0.05f) * Math.PI), 0.05f, 1);
                }
                float ts = Math.max(0.35f, dur - 0.4f);
                thump(o, ts, 90, 0.42f); knock(o, ts, 260, 0.22f); burst(o, ts, 0.04f, 0.4f, 0.1f);
                break;
            }
            // v35: a limp — a firm step, then a lighter one that drags a little, the rhythm uneven
            case Film.SFX_STEPS_LIMP: { float t = 0; while (t < dur) { thump(o, t, 120, 0.25f); burst(o, t, 0.05f, 0.4f, 0.12f); thump(o, t + 0.3f, 100, 0.12f); burst(o, t + 0.3f, 0.2f, 0.25f, 0.05f); t += 0.78f; } break; }
            // v35: a chair — the legs scrape on the floor, the wood creaks, the weight settles
            case Film.SFX_CHAIR: {
                burst(o, 0.02f, 0.22f, 0.55f, 0.07f);
                for (float t = 0.12f; t < Math.min(dur, 0.5f); t += 0.04f) tone(o, t, 0.04f, 190 + rnd() * 30, 240 + rnd() * 30, 0.035f, 1);
                thump(o, Math.min(dur - 0.1f, 0.45f), 110, 0.14f);
                break;
            }
            // v35: a sofa — the cushion gives with a soft, dull thump and a spring sighs
            case Film.SFX_SOFA: { thump(o, 0.02f, 70, 0.32f); burst(o, 0.02f, 0.22f, 0.12f, 0.1f); tone(o, 0.08f, 0.14f, 620, 520, 0.025f, 0); break; }
            // v35: a bed — the frame creaks low, the covers rustle
            case Film.SFX_BED: {
                for (float t = 0.05f; t < Math.min(dur, 0.6f); t += 0.05f) tone(o, t, 0.05f, 130 + rnd() * 20, 170 + rnd() * 20, 0.03f, 1);
                noiseBedRange(o, 0.1f, Math.min(dur, 1.1f), 0.35f, 0.05f);
                break;
            }
            // v35: eating — the spoon on the plate, then quiet, muffled chewing
            case Film.SFX_EAT: { knock(o, 0.03f, 2600, 0.12f); knock(o, 0.035f, 3400, 0.05f); for (float t = 0.4f; t < dur - 0.1f; t += 0.3f + Math.abs(rnd()) * 0.08f) burst(o, t, 0.07f, 0.15f, 0.045f); fadeEnds(o, 0.1f); break; }
            // v35: a sip from a cup or a glass — a soft slurp, a swallow, the cup set down
            case Film.SFX_SIP: {
                sweepNoise(o, 0.05f, Math.min(0.45f, dur * 0.4f), 0.3f, 0.7f, 0.05f);
                float sw = Math.min(dur - 0.3f, 0.65f);
                thump(o, sw, 160, 0.1f); tone(o, sw, 0.06f, 300, 200, 0.04f, 0);
                if (dur > 1.2f) { knock(o, dur - 0.18f, 2300, 0.12f); knock(o, dur - 0.178f, 3100, 0.05f); }
                break;
            }
            // v35: gulps from a bottle
            case Film.SFX_GULP: { for (float t = 0.15f; t < dur - 0.15f; t += 0.42f) { thump(o, t, 150, 0.13f); tone(o, t, 0.06f, 320, 220, 0.05f, 0); } break; }
            // v35: a cup or a glass set down on the table
            case Film.SFX_CUP: { knock(o, 0.01f, 2300, 0.2f); knock(o, 0.012f, 3100, 0.08f); break; }
            // v35: a sleeper's slow breathing — a soft breath in, a longer breath out
            case Film.SFX_SLEEP: { for (float t = 0.1f; t < dur - 1f; t += 4f) { noiseBedRange(o, t, Math.min(dur, t + 1.5f), 0.3f, 0.022f); noiseBedRange(o, t + 1.7f, Math.min(dur, t + 3.7f), 0.18f, 0.028f); } break; }
            // v36: cooking — a steady sizzle of oil (bright crackling noise) with the ladle scraping and tapping the pan
            case Film.SFX_SIZZLE: {
                noiseBedRange(o, 0.02f, dur, 0.85f, 0.05f);
                for (float t = 0.05f; t < dur - 0.05f; t += 0.05f + Math.abs(rnd()) * 0.08f) burst(o, t, 0.012f, 0.9f, 0.035f);
                for (float t = 0.6f; t < dur - 0.2f; t += 1.1f) { sweepNoise(o, t, 0.25f, 0.5f, 0.3f, 0.05f); knock(o, t + 0.3f, 1700, 0.06f); }
                fadeEnds(o, 0.15f);
                break;
            }
            // v36: a broom (a jhadu) — soft swishes, one each way
            case Film.SFX_SWEEP: { for (float t = 0.05f; t < dur - 0.2f; t += 0.55f) sweepNoise(o, t, 0.32f, 0.35f, 0.65f, 0.09f); fadeEnds(o, 0.05f); break; }
            // v36: washing — water sloshing in a bucket, a scrub, now and then a wring and drips
            case Film.SFX_SCRUB: {
                for (float t = 0.05f; t < dur - 0.3f; t += 0.7f) { sweepNoise(o, t, 0.4f, 0.15f, 0.35f, 0.07f); burst(o, t + 0.42f, 0.18f, 0.6f, 0.05f); }
                for (float t = 0.3f; t < dur - 0.1f; t += 0.23f + Math.abs(rnd()) * 0.2f) knock(o, t, 900 + rnd() * 300, 0.025f);
                fadeEnds(o, 0.1f);
                break;
            }
            // v36: a page turned — a quick papery whisper
            case Film.SFX_PAGE: { sweepNoise(o, 0.02f, Math.min(dur - 0.03f, 0.33f), 0.55f, 0.85f, 0.08f); break; }
            // v36: a pen or pencil on paper — short scratchy strokes
            case Film.SFX_SCRIBBLE: { for (float t = 0.05f; t < dur - 0.1f; t += 0.16f + Math.abs(rnd()) * 0.12f) burst(o, t, 0.06f + Math.abs(rnd()) * 0.05f, 0.95f, 0.03f); fadeEnds(o, 0.05f); break; }
            // v36: brushing teeth — quick back-and-forth bristle strokes
            case Film.SFX_BRUSH: { for (float t = 0.05f; t < dur - 0.1f; t += 0.13f) burst(o, t, 0.09f, 0.8f, 0.04f); fadeEnds(o, 0.08f); break; }
            // v36: water poured from a watering can — a steady trickle with splashes below
            case Film.SFX_POUR: {
                noiseBedRange(o, 0.05f, dur - 0.1f, 0.55f, 0.06f);
                for (float t = 0.1f; t < dur - 0.1f; t += 0.09f + Math.abs(rnd()) * 0.08f) knock(o, t, 700 + rnd() * 400, 0.02f);
                fadeEnds(o, 0.2f);
                break;
            }
            // v37: a car's or a bus's engine — a low, rough hum with its harmonics, a little uneven
            case Film.SFX_ENGINE: {
                for (int i = 0; i < n; i++) {
                    float t = i / (float) SR, f = 46 + 6 * (float) Math.sin(t * 0.8f) + 3 * (float) Math.sin(t * 5.3f);
                    float ph = t * f;
                    float v = (float) (Math.sin(ph * 6.283) * 0.5 + Math.sin(ph * 2 * 6.283) * 0.3 + Math.sin(ph * 3 * 6.283) * 0.15) + rnd() * 0.12f;
                    o[i] += v * 0.32f;
                }
                lowpass(o, 0.12f);
                fadeEnds(o, 0.4f);
                break;
            }
            // v37: a motorbike, a scooter or an auto — the two-stroke putter, quick pops on a hum
            case Film.SFX_MOTOR: {
                for (float t = 0.02f; t < dur - 0.05f; t += 0.045f + 0.008f * (float) Math.sin(t * 3)) knock(o, t, 140 + rnd() * 30, 0.16f);
                for (int i = 0; i < n; i++) { float t = i / (float) SR; o[i] += (float) Math.sin(t * 92 * 6.283) * 0.05f; }
                fadeEnds(o, 0.3f);
                break;
            }
            // v37: a horn, twice ("पॉं-पॉं")
            case Film.SFX_HORN: { tone(o, 0, 0.28f, 420, 420, 0.35f, 1); tone(o, 0, 0.28f, 530, 530, 0.25f, 1); tone(o, 0.38f, 0.32f, 420, 420, 0.35f, 1); tone(o, 0.38f, 0.32f, 530, 530, 0.25f, 1); break; }
            // v37: a bicycle bell — two quick rings
            case Film.SFX_CYCLE_BELL: { for (int r = 0; r < 2; r++) for (int k = 0; k < 6; k++) bellTone(o, r * 0.32f + k * 0.035f, 2900 + (k % 2) * 260, 0.35f, 0.09f); break; }
            // v37: a bicycle's chain and freewheel — a soft fast ticking
            case Film.SFX_CHAIN: { for (float t = 0.03f; t < dur - 0.05f; t += 0.07f) knock(o, t, 3800 + rnd() * 400, 0.018f); fadeEnds(o, 0.2f); break; }
            // v37: a swing's chains — a creak at each end of the swing (every 1.3 s)
            case Film.SFX_CREAK: { for (float t = 0.1f; t < dur - 0.4f; t += 1.3f) { tone(o, t, 0.32f, 520, 610, 0.05f, 1); tone(o, t + 0.04f, 0.25f, 760, 700, 0.03f, 1); } break; }
            // v37: a skipping rope — the whirr past the ear, the tap on the ground
            case Film.SFX_ROPE: { for (float t = 0.05f; t < dur - 0.1f; t += 0.55f) { sweepNoise(o, t, 0.18f, 0.4f, 0.7f, 0.05f); burst(o, t + 0.22f, 0.025f, 0.5f, 0.12f); } fadeEnds(o, 0.05f); break; }
            // v37: a soft peck
            case Film.SFX_KISS: { burst(o, 0.02f, 0.035f, 0.85f, 0.12f); tone(o, 0.03f, 0.06f, 900, 1300, 0.04f, 0); break; }
            case Film.SFX_BOOM: { thump(o, 0, 40, 1.0f); burst(o, 0, 0.6f, 0.2f, 0.7f); noiseBed(o, 0.05f, 0.4f, 1.5f, 0.3f); fadeEnds(o, 0.4f); break; }
            case Film.SFX_WHOOSH: case Film.SFX_WHOOSH_CARD: { sweepNoise(o, 0, dur, 0.02f, 0.4f, type == Film.SFX_WHOOSH ? 0.6f : 0.35f); break; }
            case Film.SFX_BELL: { bellTone(o, 0, 196, Math.min(dur, 4f), 0.7f); bellTone(o, 0, 196 * 2.76f, Math.min(dur, 2.5f), 0.2f); break; }
            case Film.SFX_DRUMS: dhol(o, 0, dur, 0.6f); break;
            case Film.SFX_NIGHT: {
                for (float t = 0; t < dur; t += 0.5f + Math.abs(rnd()) * 0.4f)
                    for (int k = 0; k < 3; k++) tone(o, t + k * 0.05f, 0.03f, 4400, 4400, 0.06f, 0);
                for (float t = 1.2f; t < dur - 1; t += 3.5f) { tone(o, t, 0.35f, 420, 380, 0.12f, 0); tone(o, t + 0.45f, 0.5f, 400, 360, 0.12f, 0); }
                break;
            }
            case Film.SFX_ROAR: {
                for (int i = 0; i < n; i++) {
                    float t = i / (float) SR, env = (float) Math.sin(Math.PI * Math.min(1, t / dur)) ;
                    float f = 70 + 25 * (float) Math.sin(t * 9);
                    float saw = (float) (((t * f) % 1.0) * 2 - 1);
                    o[i] += (saw * 0.5f + rnd() * 0.35f) * env * 0.8f;
                }
                lowpass(o, 0.08f);
                break;
            }
            case Film.SFX_CLAP: { float t = 0.02f; while (t < dur - 0.1f) { burst(o, t, 0.04f, 0.9f, 0.6f); t += 0.3f; } break; }
            case Film.SFX_ANKLET: { float t = 0; while (t < dur) { for (int k = 0; k < 3; k++) bellTone(o, t + k * 0.02f, 3500 + k * 600 + rnd() * 200, 0.25f, 0.06f); t += 0.22f; } break; }
            case Film.SFX_DRIP: { for (float t = 0.3f; t < dur; t += 1.4f + Math.abs(rnd()) * 1.5f) tone(o, t, 0.08f, 1600, 900, 0.18f, 0); break; }
            case Film.SFX_SPLASH: { burst(o, 0, 0.5f, 0.5f, 0.6f); break; }
            case Film.SFX_NET: { sweepNoise(o, 0, 0.5f, 0.05f, 0.3f, 0.4f); thump(o, 0.45f, 90, 0.5f); break; }
            case Film.SFX_FANFARE: fanfare(o); break;
            case Film.SFX_END_CHORD: endChord(o); break;
            case Film.SFX_CROWD: { noiseBed(o, 0.03f, 0.25f, 3f, 0.6f); break; }
            case Film.SFX_GLASS: { bellTone(o, 0, 2637, 1.2f, 0.3f); bellTone(o, 0.05f, 3951, 1.0f, 0.2f); break; }
            case Film.SFX_SWORD: { sweepNoise(o, 0, 0.3f, 0.3f, 0.9f, 0.3f); bellTone(o, 0.05f, 2200, 0.6f, 0.2f); break; }
            default:
        }
        return o;
    }

    private void noiseBed(float[] o, float lp, float amp, float lfoHz, float depth) {
        float y = 0;
        for (int i = 0; i < o.length; i++) {
            float t = i / (float) SR;
            y += lp * (rnd() - y);
            float m = 1 - depth + depth * (0.5f + 0.5f * (float) Math.sin(t * lfoHz * 6.283f + Math.sin(t * 0.7f)));
            o[i] += y * amp * m * 4;
        }
        fadeEnds(o, 0.4f);
    }

    /**
     * v35: leaves rustling in the wind: the bright part of a noise, in quick uneven flurries (a new level forty times a
     * second, glided into). The Mixer swells the whole wind with the gusts the picture shows.
     */
    private void leaves(float[] o, float amp) {
        float lo = 0, m = 0, target = 0;
        int step = SR / 40;
        for (int i = 0; i < o.length; i++) {
            float n = rnd();
            lo += 0.25f * (n - lo);
            if (i % step == 0) target = (float) Math.pow(Math.abs(rnd()), 2.5);
            m += 0.004f * (target - m);
            o[i] += (n - lo) * amp * (0.25f + 1.2f * m);
        }
        fadeEnds(o, 0.4f);
    }

    private void noiseBedRange(float[] o, float t0, float t1, float lp, float amp) {
        int a = (int) (t0 * SR), b = Math.min(o.length, (int) (t1 * SR));
        float y = 0;
        for (int i = a; i < b; i++) {
            y += lp * (rnd() - y);
            float e = (float) Math.sin(Math.PI * (i - a) / (float) Math.max(1, b - a));
            o[i] += y * amp * e;
        }
    }

    private void fadeEnds(float[] o, float sec) {
        int f = Math.min(o.length / 2, (int) (sec * SR));
        for (int i = 0; i < f; i++) { float g = i / (float) f; o[i] *= g; o[o.length - 1 - i] *= g; }
    }

    private void burst(float[] o, float t, float d, float bright, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        float y = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            if (a + i < 0) continue;
            y += bright * (rnd() - y);
            float e = (float) Math.exp(-i / (n * 0.25f));
            o[a + i] += y * amp * e;
        }
    }

    private void chirp(float[] o, float t, float f, float d, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        double ph = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            float u = i / (float) n;
            float fr = f * (1 + 0.6f * (float) Math.sin(u * Math.PI * 3));
            ph += 2 * Math.PI * fr / SR;
            o[a + i] += (float) Math.sin(ph) * amp * (float) Math.sin(Math.PI * u);
        }
    }

    private void knock(float[] o, float t, float f, float amp) {
        int a = (int) (t * SR), n = (int) (0.09f * SR);
        for (int i = 0; i < n && a + i < o.length; i++) {
            float e = (float) Math.exp(-i / (SR * 0.012f));
            o[a + i] += ((float) Math.sin(2 * Math.PI * f * i / SR) * 0.7f + rnd() * 0.3f) * amp * e;
        }
    }

    /** f0 -> f1 glide; shape 0 sine, 1 square-ish. */
    private void tone(float[] o, float t, float d, float f0, float f1, float amp, int shape) {
        int a = (int) (t * SR), n = (int) (d * SR);
        double ph = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            if (a + i < 0) continue;
            float u = i / (float) n;
            ph += 2 * Math.PI * (f0 + (f1 - f0) * u) / SR;
            float s = (float) Math.sin(ph);
            if (shape == 1) s = Math.signum(s) * 0.6f + s * 0.4f;
            float e = Math.min(1, u * 20) * (1 - u);
            o[a + i] += s * amp * e;
        }
    }

    private void thump(float[] o, float t, float f, float amp) {
        int a = (int) (t * SR), n = (int) (0.35f * SR);
        double ph = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            float fr = f * (1 + 1.5f * (float) Math.exp(-i / (SR * 0.02f)));
            ph += 2 * Math.PI * fr / SR;
            o[a + i] += (float) Math.sin(ph) * amp * (float) Math.exp(-i / (SR * 0.08f));
        }
    }

    private void bellTone(float[] o, float t, float f, float d, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        float[] ratio = {1f, 2.01f, 2.76f, 4.07f, 5.4f};
        float[] g = {1f, 0.5f, 0.35f, 0.2f, 0.1f};
        for (int i = 0; i < n && a + i < o.length; i++) {
            if (a + i < 0) continue;
            float s = 0;
            float tt = i / (float) SR;
            for (int k = 0; k < ratio.length; k++) s += (float) Math.sin(2 * Math.PI * f * ratio[k] * tt) * g[k] * (float) Math.exp(-tt * (2.2f + k * 1.4f) / d);
            o[a + i] += s * amp * Math.min(1, i / 60f) * 0.6f;
        }
    }

    private void sweepNoise(float[] o, float t, float d, float lp0, float lp1, float amp) {
        int a = (int) (t * SR), n = Math.min(o.length - a, (int) (d * SR));
        float y = 0;
        for (int i = 0; i < n; i++) {
            float u = i / (float) n;
            y += (lp0 + (lp1 - lp0) * (float) Math.sin(u * Math.PI)) * (rnd() - y);
            o[a + i] += y * amp * (float) Math.sin(Math.PI * u) * 2;
        }
    }

    private static void lowpass(float[] o, float k) {
        float y = 0;
        for (int i = 0; i < o.length; i++) { y += k * (o[i] - y); o[i] = y * 2.5f; }
    }

    private void dhol(float[] o, float t0, float d, float amp) {
        float beat = 0.25f;
        int[] pat = {2, 0, 1, 1, 2, 0, 1, 0, 2, 1, 1, 0, 2, 0, 1, 1};
        int k = 0;
        for (float t = t0; t < t0 + d - 0.3f; t += beat, k++) {
            int p = pat[k % pat.length];
            if (p == 2) thump(o, t, 70, amp);
            else if (p == 1) { knock(o, t, 420, amp * 0.45f); burst(o, t, 0.05f, 0.5f, amp * 0.15f); }
        }
    }

    private void fanfare(float[] o) {
        // shehnai-like bright reed melody over a drum roll
        int[] mel = {67, 72, 74, 76, 74, 72, 79, 76, 77, 79};
        float[] len = {0.25f, 0.25f, 0.25f, 0.5f, 0.25f, 0.25f, 0.75f, 0.25f, 0.25f, 1.2f};
        float t = 0.1f;
        for (int i = 0; i < mel.length; i++) { reed(o, t, len[i] * 0.95f, midi(mel[i]), 0.22f); t += len[i]; }
        dhol(o, 0, 4.2f, 0.4f);
        bellTone(o, t - 1.2f, midi(79), 2f, 0.2f);
    }

    private void reed(float[] o, float t, float d, float f, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        double ph = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            float tt = i / (float) SR;
            float vib = 1 + 0.008f * (float) Math.sin(tt * 2 * Math.PI * 5.5f) * Math.min(1, tt * 4);
            ph += 2 * Math.PI * f * vib / SR;
            float s = (float) (Math.sin(ph) + 0.5 * Math.sin(2 * ph) + 0.33 * Math.sin(3 * ph) + 0.2 * Math.sin(4 * ph) + 0.12 * Math.sin(5 * ph));
            float e = Math.min(1, tt * 25) * Math.min(1, (d - tt) * 12);
            o[a + i] += s * amp * 0.45f * e;
        }
    }

    private void endChord(float[] o) {
        int[] arp = {60, 64, 67, 72, 76, 79, 84};
        for (int i = 0; i < arp.length; i++) pluck(o, 0.1f + i * 0.14f, midi(arp[i]), 2.5f, 0.3f);
        int[] ch = {48, 55, 60, 64, 67};
        for (int c : ch) pad(o, 0.9f, 4f, midi(c), 0.07f);
        bellTone(o, 1.0f, midi(84), 3f, 0.15f);
    }

    // ================================================================== music

    /**
     * Background music for a mood. Built from a small Indian-flavoured palette:
     * tanpura drone, santoor-like plucks, bansuri-like flute and tabla/dhol patterns.
     */
    public float[] music(int mood, float dur) {
        seed = 4242 + mood * 97;
        int n = Math.max(1, (int) (dur * SR));
        float[] o = new float[n];
        int root;
        int[] scale;
        float bpm;
        switch (mood) {
            case Film.M_TITLE: root = 62; scale = new int[]{0, 2, 4, 7, 9, 12}; bpm = 96; break;
            case Film.M_HAPPY: root = 64; scale = new int[]{0, 2, 4, 7, 9, 12}; bpm = 100; break;
            case Film.M_PLAYFUL: root = 67; scale = new int[]{0, 2, 4, 7, 9, 12}; bpm = 126; break;
            case Film.M_TENSE: root = 57; scale = new int[]{0, 1, 4, 5, 7, 8, 11, 12}; bpm = 84; break;
            case Film.M_VILLAIN: root = 50; scale = new int[]{0, 1, 3, 6, 7, 8, 12}; bpm = 70; break;
            case Film.M_SAD: root = 57; scale = new int[]{0, 2, 3, 7, 8, 12}; bpm = 66; break;
            case Film.M_ACTION: root = 55; scale = new int[]{0, 1, 4, 5, 7, 8, 10, 12}; bpm = 138; break;
            case Film.M_CELEBRATE: root = 65; scale = new int[]{0, 2, 4, 5, 7, 9, 11, 12}; bpm = 128; break;
            case Film.M_NIGHT: root = 55; scale = new int[]{0, 2, 3, 7, 8, 12}; bpm = 72; break;
            case Film.M_END: root = 60; scale = new int[]{0, 2, 4, 7, 9, 12}; bpm = 90; break;
            default: root = 62; scale = new int[]{0, 2, 4, 7, 9, 12}; bpm = 96;
        }
        float beat = 60f / bpm;
        // drone
        float droneAmp = (mood == Film.M_VILLAIN || mood == Film.M_TENSE || mood == Film.M_NIGHT) ? 0.11f : 0.07f;
        for (int i = 0; i < n; i++) {
            float t = i / (float) SR;
            float f1 = midi(root - 24), f2 = midi(root - 17);
            float s = (float) (Math.sin(2 * Math.PI * f1 * t) * 0.6 + Math.sin(2 * Math.PI * f1 * 2 * t) * 0.25 * (0.5 + 0.5 * Math.sin(t * 0.9))
                    + Math.sin(2 * Math.PI * f2 * t) * 0.35 + Math.sin(2 * Math.PI * f1 * 3 * t) * 0.12 * (0.5 + 0.5 * Math.sin(t * 1.3 + 1)));
            o[i] += s * droneAmp;
        }
        // melody phrases
        boolean flute = mood == Film.M_TITLE || mood == Film.M_SAD || mood == Film.M_NIGHT || mood == Film.M_END || mood == Film.M_HAPPY;
        float t = 0.2f;
        int deg = 2;
        int phrase = 0;
        while (t < dur - 0.5f) {
            int notes = 6 + (phrase % 3) * 2;
            for (int k = 0; k < notes && t < dur - 0.5f; k++) {
                int step = (int) Math.round(rnd() * 2);
                deg = Math.max(0, Math.min(scale.length - 1, deg + (step == 0 ? 1 : step)));
                if (k == notes - 1) deg = 0;
                float f = midi(root + scale[deg]);
                float len = beat * ((k % 3 == 2) ? 2 : 1) * (mood == Film.M_ACTION || mood == Film.M_PLAYFUL ? 0.5f : 1f);
                if (mood == Film.M_VILLAIN || mood == Film.M_TENSE) {
                    if (k % 2 == 0) pluck(o, t, f * 0.5f, len * 1.8f, 0.16f);
                } else if (flute && phrase % 2 == 0) flute(o, t, len * 0.95f, f, 0.13f);
                else pluck(o, t, f, len * 2.2f, 0.17f);
                t += len;
            }
            t += beat * 2;
            phrase++;
        }
        // rhythm
        switch (mood) {
            case Film.M_CELEBRATE: dhol(o, 0, dur, 0.38f); break;
            case Film.M_ACTION: for (float x = 0; x < dur - 0.2f; x += beat / 2) { thump(o, x, 60, (((int) (x / beat * 2)) % 4 == 0) ? 0.45f : 0.2f); } break;
            case Film.M_PLAYFUL: case Film.M_HAPPY: case Film.M_TITLE: case Film.M_END: tabla(o, dur, beat, 0.22f); break;
            case Film.M_TENSE: case Film.M_VILLAIN: for (float x = 0; x < dur - 0.5f; x += beat * 2) thump(o, x, 45, 0.3f); break;
            default:
        }
        fadeEnds(o, 0.8f);
        return o;
    }

    private void tabla(float[] o, float dur, float beat, float amp) {
        int[] pat = {2, 0, 1, 1, 2, 1, 0, 1}; // dha . ti ti dha ti . ti (teentaal-ish feel)
        int k = 0;
        for (float t = 0; t < dur - 0.3f; t += beat / 2, k++) {
            int p = pat[k % pat.length];
            if (p == 2) { tone(o, t, 0.25f, 180, 140, amp, 0); knock(o, t, 600, amp * 0.3f); }
            else if (p == 1) knock(o, t, 1100 + (k % 3) * 120, amp * 0.35f);
        }
    }

    private void pluck(float[] o, float t, float f, float d, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        int period = Math.max(2, (int) (SR / f));
        float[] buf = new float[period];
        for (int i = 0; i < period; i++) buf[i] = rnd();
        int idx = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            if (a + i < 0) continue;
            float v = buf[idx];
            int nx = (idx + 1) % period;
            buf[idx] = 0.5f * (v + buf[nx]) * 0.996f;
            idx = nx;
            o[a + i] += v * amp;
        }
    }

    private void flute(float[] o, float t, float d, float f, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        double ph = 0;
        float y = 0;
        for (int i = 0; i < n && a + i < o.length; i++) {
            float tt = i / (float) SR;
            float vib = 1 + 0.006f * (float) Math.sin(tt * 2 * Math.PI * 5) * Math.min(1, tt * 3);
            ph += 2 * Math.PI * f * vib / SR;
            y += 0.1f * (rnd() - y);
            float s = (float) (Math.sin(ph) + 0.12 * Math.sin(2 * ph)) + y * 0.15f;
            float e = Math.min(1, tt * 10) * Math.min(1, (d - tt) * 6);
            o[a + i] += s * amp * e;
        }
    }

    private void pad(float[] o, float t, float d, float f, float amp) {
        int a = (int) (t * SR), n = (int) (d * SR);
        for (int i = 0; i < n && a + i < o.length; i++) {
            float tt = i / (float) SR;
            float e = Math.min(1, tt / 0.8f) * Math.min(1, (d - tt) / 1.2f);
            float s = (float) (Math.sin(2 * Math.PI * f * tt) + 0.5 * Math.sin(2 * Math.PI * f * 1.003 * tt) + 0.3 * Math.sin(2 * Math.PI * f * 2 * tt));
            o[a + i] += s * amp * e;
        }
    }
}
