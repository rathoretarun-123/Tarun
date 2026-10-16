# Testing v35 — wind, a sharper picture, bodies, everyday actions

The user asked: *"Grass movement, leaves, trees etc. movements, wind effects — all should be realistic and smooth. Limping, some
character has fewer limbs or fingers, only one eye, blind, deaf etc. Make the picture less blurry. Sitting and getting up from a
chair or sofa, sleeping and getting up, sitting on the floor and getting up, eating food, drinking water or tea from a cup or glass or
directly from a bottle."*

## 1. Wind (test `v35WindMovesWhatGrowsOnlyOutdoors`, plus frames)

A place picture half sky, half grass and half grey wall, through the same mesh the phone draws:

| Check | Result |
|---|---|
| The grass sways between two moments | up to 4.2 units (of 1280) |
| The wall and the sky | 0.00 — never move |
| One frame to the next (24 fps) | at most 1.44 units — a small, smooth step |
| The same picture indoors | 0.00 — nothing sways |

Desktop frames of the sample's garden and forest (`WindDbg`): the tree crowns (also above the skyline), bushes and grass move in
travelling gusts, buildings and people stay still; painted sets and foreground plants sway the same way.

## 2. A sharper picture (test `v35SharperPictures`, plus a measurement)

| Check | Result |
|---|---|
| Picture sizes on a small phone (256 MB heap, 600 MB free, 720p) | characters 1440 px, places 2048 px (never below the old 1280 / 1600) |
| On a phone with memory to spare (1080p, 3 GB free) | 2160 px / 3072 px |
| The copy drawn | the full picture near its size, the half copy at a third, the quarter copy at a sixth |
| Close-up detail on the sample (Laplacian variance of a face close-up) | about 154 → 240 |
| Halo of a photo's old background | taken off its edge (ring defringe, and pockets on plain backgrounds) |

## 3. Bodies (test `v35BodiesFromTheWords`, story `bodies.txt`)

Read from the words: a limp, one arm, an artificial leg (Jaipur foot), born blind, deaf and signing, one eye; plain clothes words give no
condition. The film of `bodies.txt` reports them in its shot list; the deaf boy's line is signed (no lip-sync; the words in the subtitle),
and he does not startle at a bang he cannot hear.

## 4. Everyday actions (tests `v35EverydayActions`, `v35SeatedOnTheSeat`, frames)

Story `everyday.txt` (a grandmother, a girl and her father at home):

| Check | Result |
|---|---|
| The girl lies down and sleeps (scene 1): sits on the bed, lies back, eyes shut, slow breathing, a blanket | yes |
| Scene 2 begins with her still asleep in bed | yes |
| She wakes, sits up, stretches with a yawn, gets out of bed | yes, in that order (fixed: the getting-up used to come before the sitting-up when both were in one sentence) |
| The grandmother sits on the sofa; the father sits on a chair with tea and gets up | yes, with the sofa's thump and the chair's creak |
| The girl sits cross-legged on the floor (a woven mat), eats, drinks water from a glass | yes; eating and sipping heard, the cup set down |
| Every character's keys in time order | yes (fixed: `mobility()` read a state by adding a key out of order) |

**Seated on the seat — found and fixed.** Rendering a drawn character on each seat showed the hips near the floor and the shins
reaching about a quarter of the height *below* the floor (the oldest sitting code). Now (height 240 px):

| Seat | Head top | Lowest point |
|---|---|---|
| chair | 208 px | at the floor (−1 px) |
| sofa | 200 px | −1 px |
| bed | 203 px | −1 px |
| floor (cross-legged) | 172 px | 8 px (the lap in front of the hips, on the mat) |

## 5. Photo characters (the sample's pictures in the same everyday story)

* Sitting: the thighs (or the saree's lap) fold at the knees; on the floor the shins fold under too and the lehenga spreads on the mat.
  **Honest:** a single standing photo on a sofa or a chair reads less clearly as seated than a drawn character; the director asks
  (optionally) for a picture of them sitting or lying (test `v35AsksForSittingAndLyingPictures`: the girl lying and sitting and the
  father sitting are asked for, not the grandmother who has no picture yet; once a sitting picture is given it is not asked again; the
  film never waits for these) and frames a seated speaker from the waist up.
* Eating and drinking: the father's tea is shown in a close-up with the cup in his hand at his lips. When the picture is too small for a
  close-up (the girl's), the cup stays in her hand.

## 6. Lip-sync and motion (desktop `LipMotionQc`, the phone's own envelope)

| Story | Open while loud | Shut in pauses | Shut before/after a line | Correlation | Visible jumps |
|---|---|---|---|---|---|
| everyday (drawn) | 98.7 % | 100 % | 100 % | 0.77 | 0 |
| everyday (photos) | 98.7 % | 100 % | 100 % | 0.76 | 0 |
| bodies | 83.3 % (the signed line keeps the mouth shut by design) | 100 % | 100 % | 0.58 | 0 (5 placements while hidden, before an entrance) |

## 7. The full suite

See the end of this file.
