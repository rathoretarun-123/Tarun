# Testing v38 — the animated director's craft guide, then the user's review

## 1. The guide

The user gave an additional guide for the AI director ("AI Adult Animated Director — Comprehensive Guide, Pixar / Disney
Style") and asked for it to be trained into the director.

Most of that guide is sexual content: a vocabulary of sex acts and body parts, scene recipes for sex, masturbation, nude
bathing and undressing, ejaculation, choosing and compositing pictures by nudity, and adult sounds. **None of it is trained.**
The studio makes family films; it does not recognise, draw, voice or score any of it. The guide is not bundled in the app.

Its film craft is trained and hardcoded in `core/FilmCraft.java` (15 rules, each naming the code that applies it, shown in the
protocols screen, reported in every shot list):

| Guide section (craft only) | Where it lives |
|---|---|
| §1.2 the script as a production brief | already the director's practice (ShotPlanner arc, shot list) — recorded as a rule |
| §2 appealing design, cinematic light (key, fill, rim), colour and atmosphere | `FilmCraft.beatLook` via `FilmLook.at`: richer at the peak, warmer when tender (new); key/fill/rim already in Renderer/Art |
| §2 "exaggerated but believable performance", §3.4 readable expression states | `FilmCraft.flush`: cheeks flush with anger, pride, laughter and effort, by the beat's intensity (new) |
| §2 storytelling camera | already ShotPlanner (establishing wides, push-ins while tension rises, closer at the peak) |
| §4 best picture per beat (feeling first, then body and angle) | already `Casting` (v27) — recorded as a rule |
| §8 missing picture: ask the user, never invent | `FilmCraft.pictureWants`: "PICTURES THAT WOULD HELP" per character in the shot list (new) |
| §7 quality and continuity checklist | `FilmCraft.check`: speaker on the stage and facing us, breathing room after peak lines, touches only where written (new) |
| §8 consent and agency as written | touches (hug, held hand, peck) only from the script's words — counted by the checklist, tested |
| §9 delivery with scene markers | `FilmCraft.editList`; FilmJob saves `edit_list.txt`; the film screen downloads it (new). Subtitles: "add subtitles" after the film, as before |

## 2. The user's review of the first v38 build, and the fixes

*"New lips are seen when Vanusha is laughing, the picture of the guard is not ok. Subtitles can be added post production by
prompt. Please debug and test thoroughly."*

**New lips (वानुषा).** Frames at 0:39.6–0:44 of the sample, cropped to her face: a dark mouth bar painted under her real smile
in every frame, laughing or speaking. Cause: her picture already shows an open, laughing mouth with teeth, and the mouth point
(from the sample's cast) sat on the skin under her nose, about 0.6 mouth-widths above the real opening; the face mesh opened
"the mouth" there and the renderer painted a dark opening inside it. Fix (`Art.mouthFromPicture`, `Renderer`): teeth
(light and much less coloured than the skin beside them) found just below or around the point move the point onto the real
mouth before the face mesh is built; a mouth showing teeth or its dark inside is marked open and never painted open again (its
lips move half as far with the voice); a laugh or surprise without words no longer opens a picture's mouth at all. Measured on
the sample: वानुषा 334 tooth pixels → point moved, open; two other characters marked open by their dark mouths; the others
closed as before. Frames re-checked: her own smile, her own lips moving, nothing painted under them.

**Blinks on pictures.** The same crops showed flat skin-coloured discs over her eyes during a blink. Now an almond-shaped lid
with a soft edge into the skin and lashes along its lower edge.

**The guard (रतनलाल).** When the monkey takes his turban, his face showed torn white patches, half an eyebrow and a ghost of his
moustache curl. Cause: `clearHeadwearLeftovers` removed *everything that was not skin* beside the face (narrower than his face)
from the top of the picture down to the neck — the eyebrow's end, the moustache curl and shaded cheek and ear went, and the
palace wall showed through the holes. Fix: only the turban's own colours go (its main colours, not their average), above the
eyes; below them only its cool or vivid dyed colours (a flap or a band end, never skin); anything left floating beside the
head, joined neither to the face nor to the body, goes too. Measured: of 6,968 picture pixels beside his face between the eyes
and the mouth, 1,457 were cut before, 64 after. Also: the sharp face layer no longer includes a hand raised to the face, and
such a hand keeps the head steady with it (the face layer moved with the head and the side of the head with the arm).

**Over-the-shoulder shots (found in the 64-frame sweep).** In every over-the-shoulder reverse the shoulder character was also
drawn on the stage beside the listener — the same person twice, the face half hidden by their own back. Now they are only the
foreground shoulder, which shows the back of the head and the shoulder (it showed the waist) and never reaches past 40% of the
frame (a witch's wide hat crossed the middle). 12 of the sample's 48 such shots checked by eye.

**Subtitles.** They were already off unless asked for ("add subtitles" in the box under the film); the separate `.srt` file the
first build saved is dropped. The edit list with scene markers stays.

## 3. The user: "Not able to upload any picture for any character, object or place in the studio"

Reproduced with pictures handed over exactly as the phone does (`onActivityResult` with a content address), drawn pictures
saved by a phone's gallery (which writes the phone's make and model into them) and a real camera photo (exposure and aperture
in its Exif directory):

| Upload | Before | After |
|---|---|---|
| angles · character · drawn, saved by a phone | **refused** ("looks like a photo of a real person") | taken |
| angles · character · drawn PNG | taken | taken |
| angles · thing · drawn, saved by a phone | taken | taken |
| angles · place · drawn, saved by a phone | taken | taken |
| angles · place · camera photo | taken | taken |
| angles · character · camera photo | refused | refused |
| one picture · character · drawn, saved by a phone | **refused** | taken |
| one picture · place · drawn, saved by a phone | taken, but turned into the "animated style" as a photo | taken as artwork |

Cause: `Library.cameraPhoto` (v37) counted the make or the model as camera data. Now only the exposure, the aperture, the ISO or
the focal length count. Library pictures v37 marked as photos for that reason are read once again and offered again (tested).
The v37 test's "camera photo" carried only a make; it now carries a real exposure and aperture.

## 4. Thorough checks

* 64 frames across the whole 12-minute sample film (every 11.3 s), plus frames around every fix, checked by eye.
* The butterfly line: "(तितली उड़कर एक सूखी, मुरझाई हुई कली पर बैठ जाती है।)" no longer makes वानुषा sit (`Director.creatureSits`); checked
  on every test story — only that sentence changes.
* Sample checklist: readable speakers 36 of 36; breathing room after 23 of 23 peak lines; ten scene markers named after the scenes.
* Known and unchanged: खान राक्षस has only a back picture in the sample, so the studio draws him; the shot list asks for his picture.

## 5. Tests

New: `v38CraftGuideTrainedAndReported`, `v38EditListAndSubtitlesByInstruction`, `v38BeatLookAndFlush`,
`v38PicturesKeepTheirOwnMouthAndFace`, `v38UploadsFromThePhoneReachTheStudio` — all pass, with `manyHeadedCharactersKeepTheirHeads`,
`directorUsesTheRightPictureForEachShot` and `framesRenderThroughAndroidCanvas` (7 of 7).

Full suite: see below.
