# Testing v36 — wind on clothes, real flames, firelight and shadows, everyday tasks

The user asked: *"Wind effect on dresses and hair, fur of animal. Flame looking like flames, and other everyday tasks. Fire should
light things and make shadows, same with sun, moon. After making these and other realistic things in the director code, debug again
and make the final APK for downloading."*

## 1. Wind on clothes, hair and fur (test `v36WindBlowsClothes`, story `firewind.txt`)

A drawn mother in a saree and a girl with a dupatta, drawn still and in a wind of 1.2, then a moment later in the same wind:

| Check | Mother (saree) | Girl (dupatta) |
|---|---|---|
| Pixels the wind moves | 2541 | 2436 |
| Pixels that change from one moment to the next (flutter) | 1257 | 1433 |

Frames of the windy courtyard: the pallu and the dupatta stream downwind from the shoulder, the hair follows the gusts, the lion's
mane blows, the fur ripples along its back and its tail sways. Indoors nothing blows.

## 2. Flames (test `v36FlamesLookLikeFlames`)

A campfire on black, still and in wind:

| Check | Still | In wind |
|---|---|---|
| Pale hot core pixels | 609 | 430 |
| Red-orange outer pixels | 2932 | 3157 |
| Top of the flame (base at 220) | 50 | 50 |
| Centre of the flame | 119.9 | 135.7 (leans downwind) |

## 3. Firelight, sun and moon (test `v36FireLightsTheNight`, frames)

| Check | Result |
|---|---|
| "(स्थान: …। रात का समय।)" makes the scene night | yes (fixed: the time was read only from the place name, so these night scenes were drawn in daylight) |
| "सब अलाव के पास बैठ जाते हैं" seats everyone | yes |
| The campfire stays in its own scene | yes (fixed: it burnt on into the next place while it faded) |
| "माँ दीये जलाती है" is framed to see the diyas | yes (fixed: the diyas were below the frame) |
| The flames are the brightest thing at night | **found and fixed:** the night's dark, the place tint and the grade fell over the flames too — the brightest flame pixel of the campfire was only (147, 72, 44); a glow pass now draws the flames again over the graded frame, and they read bright yellow-white at the core |
| Warm, firelit samples in a 1280×720 night frame (every 6th pixel) | campfire 414, diyas 76 |
| Shadows | each character casts a soft shadow away from the fire and gets a warm rim on the fire's side; under the moon a faint cool shadow |

## 4. Everyday tasks (test `v36EverydayTasks`, stories `tasks.txt` and `tasks_pics.txt`)

`tasks.txt`: a mother, a grandmother, a father, a girl, a boy and a grandfather at home — cooking, sweeping, washing dishes, reading
the newspaper, writing homework, a phone call, brushing teeth, combing hair, watering plants, lighting a diya.

| Check | Result |
|---|---|
| Every task acted with its tool | cooking (counter, gas flame, kadhai, ladle, steam), sweeping (jhadu to the floor), washing (bucket of suds), newspaper (**added** — it was drawn as a small book), notebook and pen, phone, toothbrush, comb, watering can with its stream onto a pot |
| Every task heard | sizzling, sweeping, scrubbing, pages, the pen, brushing, pouring |
| The phone call | **found and fixed:** the phone came down before the boy said "हाँ दोस्त…" into it; it now stays at the ear through the caller's line; a drawn character's phone was at the shoulder — raised to the ear |
| "नहीं" (माँ आज खाना नहीं बनाती है) | no task acted |
| Every task frame draws and is not blank | yes |

`tasks_pics.txt` with the user's photos (the king, the queen, the girl): the phone at the king's ear (**fixed:** it covered his cheek and
eye — now beside the face), the toothbrush at the girl's mouth with foam (**fixed:** it stayed at her hip when her hand was in the
frame — a phone and a toothbrush now always go to the face), the queen's broom and the king's newspaper at their hands.

## 5. Honest limits

A photo's arm does not bend: a ladle, a broom or a newspaper is at the photo's hand, which does not stir or sweep. Firelight is a warm
pool, a rim and a soft shadow on the ground — not light bouncing off every surface; a photo is tinted, not relit.

## 6. Full suite

Robolectric, all tests, on the v36 commit: **52 tests, 50 passed, 1 skipped (the soak), 1 failed — 41 minutes.** All four v36 tests
passed (on the phone's canvas: the wind moves 2 532 / 2 452 pixels of the saree and the dupatta, 1 222 / 1 394 flutter between moments;
the campfire night has 412 warm firelit samples, the diya room 69; the flame's centre leans from 120.0 to 135.7 in the wind).

**Found and fixed:** `freeGithubSourcesGuidesAndDescriptionDetails` — the 3D figure made from Vanusha's picture no longer read her
hair colour. It came from v35's cleaner cut-out: the outline is now part-transparent (the old background's halo removed), and the
hair band at the top of her head was almost all outline, too few opaque pixels to read. The outline pixels count again and the band
reaches at least 3.5 % of the figure down; her hair now reads dark brown (before v35 it read the grey of the halo). The fix is in v37.
