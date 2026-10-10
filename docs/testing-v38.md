# Testing v38 — the animated director's craft guide

The user gave an additional guide for the AI director ("AI Adult Animated Director — Comprehensive Guide, Pixar / Disney
Style") and asked for it to be trained into the director, then a final version after debugging and tests.

## What was trained, and what was not

Most of that guide is sexual content: a vocabulary of sex acts and body parts, scene recipes for sex, masturbation, nude
bathing and undressing, ejaculation, choosing and compositing pictures by nudity, and adult sounds. **None of it is trained.**
The studio makes family films; it does not recognise, draw, voice or score any of it (the same answer as for the earlier
requests). The guide is not bundled in the app.

Its film craft is trained and hardcoded in `core/FilmCraft.java` (15 rules, each naming the code that applies it, shown in the
protocols screen, reported in every shot list):

| Guide section (craft only) | Where it lives |
|---|---|
| §1.2 the script as a production brief | already the director's practice (ShotPlanner arc, shot list) — recorded as a rule |
| §2 appealing design, cinematic light (key, fill, rim), colour and atmosphere | `FilmCraft.beatLook` via `FilmLook.at`: richer at the peak, warmer when tender (new); key/fill/rim already in Renderer/Art |
| §2 "exaggerated but believable performance", §3.4 readable expression states | `FilmCraft.flush`: cheeks flush with anger, pride, laughter and effort, by the beat's intensity — drawn faces (Puppet) and the user's pictures (Renderer) (new) |
| §2 storytelling camera | already ShotPlanner (establishing wides, push-ins while tension rises, closer at the peak) |
| §4 best picture per beat (feeling first, then body and angle) | already `Casting` (v27) — recorded as a rule |
| §8 missing picture: ask the user, never invent | `FilmCraft.pictureWants`: "PICTURES THAT WOULD HELP" per character in the shot list (new) |
| §7 quality and continuity checklist | `FilmCraft.check`: speaker on the stage and facing us, breathing room after peak lines, touches only where written (new) |
| §8 consent and agency as written | touches (hug, held hand, peck) only from the script's words — counted by the checklist, tested |
| §5 pacing, §6 sync | already ShotPlanner.breathe / PixarLead ma, Mixer envelope — measured by the checklist |
| §9 delivery: subtitles and an edit list with scene markers | `FilmCraft.srt`, `FilmCraft.editList`; FilmJob saves `subtitles.srt` and `edit_list.txt`; the film screen downloads them (new) |

## Debugging

* **Fixed:** in the sample story, "(तितली उड़कर एक सूखी, मुरझाई हुई कली पर बैठ जाती है।)" — the butterfly settling on a bud — made
  वानुषा sit on the grass, and her laughing close-up then framed the top of her head at the bottom of the frame (the same in
  v37). `Director.creatureSits`: when a butterfly, a bird, a bee, a leaf or dust (named before the character, or with no
  character named) is what sits, the character does not. Checked on every test story: only that sentence changes. The close-up
  now shows her whole laughing face.
* Long subtitle lines (up to 10 s) are split at their sentence ends in the .srt, timed by length.
* Frames checked by eye at the sample's peak moments (0:39.6, 0:43, 2:16, 2:18.9) against v37: the laughing guard's cheeks are
  rosier (red over green in a face crop 44.3 → 48.0), nothing else changes.

## The sample story's checklist (desktop film)

Readable speakers 36 of 36; breathing room after 23 of 23 peak lines; 37 peak shots a little richer, 64 tender shots a little
warmer; 5 touches, all from the script; pictures that would help: a side view for ten characters (on the phone the studio makes
side views from each picture, so fewer are asked for there) and a picture for खान राक्षस (drawn by the studio). Ten scene markers
named after the scenes; subtitles with speakers.

## Tests

New: `v38CraftGuideTrainedAndReported`, `v38SubtitlesAndEditList`, `v38BeatLookAndFlush` — 3 of 3 passed.

Full suite: see below.
