# Testing v34 — pictures for everything, many-headed characters, the emotion / activity / angle guide, aids, costumes and situations

## 1. Uploads rechecked for every character, place and thing

Test `everyCharacterPlaceAndThingTakesPictures` (the sample story: 12 characters, 10 parts, 8 director-added scenes, 17 things + one named on the spot).

| What | How it was checked | Result |
|---|---|---|
| Every character takes pictures from the Studio button (its cast key) and again from the make-film popup (its display name) | `SheetSaver.save` twice per character; the Studio's `charFile`, the film's `Studio3DArt.charFile`, the count of `char` lines and the keys of the `pose`/`view` lines | 12 of 12: the newest front is shown and used, one `char` line each, all pictures under one key |
| Every place takes a new picture | `sceneFile` before and after; the earlier one in the library as "(earlier)" | 10 of 10 replaced, 10 earlier pictures kept |
| Every added scene takes a picture | `manifestLine("scene", key)` | 8 of 8 |
| Every thing takes a picture twice; the newest is the insert | the `shot||key|file|object` lines | 18 of 18: one insert line each (before v34 the lines piled up and the first — the oldest — was used) |
| One upload button per character and place in the Studio, one per thing | the "📷 Pictures (up to 10)", "📷 Angles / More angles" buttons counted | 22 and 18 |
| A row with its button for everything on the progress card | "📷 Add / More" counted | 48 of 48 |
| The make-film popup lists every character, part, added scene and thing, and lets a thing be named | the list's items | all present |
| A character whose name reads like a voice ("रेडियो की आवाज़") can be pictured | "📷 Picture it anyway" → the picker opens; `FilmJob.storyOf` then lists it in the cast | passes |

Fixed by this check:

* `SheetSaver.charKey` — the key of the character's existing line is used whatever the button passed, and on a new front every old `char` line of that character goes (`ScriptParser.resolve`), so the Studio (first line) and the film (last line) can never show different pictures.
* `SheetSaver.setObjectLine` — a thing's insert line is replaced (the manifest's own replace matches the scene field, which a thing's line leaves empty, so it never matched).
* `AutoLibrary.missingTargets` — a picture of the thing itself counts for every scene that mentions it.
* The popup's things, the QC screen's pictures card, `FilmJob.storyOf` with the "pictured" override.

## 2. Many-headed characters

Test `manyHeadedCharactersKeepTheirHeads`:

| Check | Result |
|---|---|
| "रावण — दस सिर वाला राक्षस" → 10; "दशानन" → 10; "तीन सिरों वाला अजगर" → 3; "a three-headed dragon" → 3; "a monster with 5 heads" → 5; "उसके सिर पर लाल पगड़ी" → 1; "head held high" → 1 | all as expected |
| A drawn figure with five heads in a row: heads counted in the picture, the face taken from the central head | heads = 5, mouth at x = 320 of 640 |
| The rig: no raised arm from the heads beside the face; the head is the whole row | armUp false/false, headHalf 0.50 |
| The drawn puppet with ten heads is wider at the head than with one | 67 px → 532 px |

## 3. The Emotion, Activity & Camera Angle guide (v2.0)

What is applied (see `EmotionActivityGuide.STEPS` in the app's protocols screen for the full map): the EXIF fix, a light level on every picture, a brightened reading copy for a figure so dark that no face reads in it as it is (gamma 1.8 + contrast stretch, the original drawn; a picture that reads in its own light is never brightened, since brightened fur turns skin-coloured), the NO_FACE gate (a back or faceless figure never carries a feeling, is cast only for back shots, never talks or blinks), a confidence on every reading with the unsure ones confirmed first in the review dialog, the bending activity, the structured record (angle, activity, feeling, height ratio, face points, confidence, light level) in every pose line.

What is not done, said plainly: no neural model (vision transformer, YOLO, RTMPose, MiDaS, Zero-DCE) runs on the phone and nothing is trained; the rules are fixed in code and the user's corrections re-tag the picture at hand.

Test `darkPicturesReadLikeBrightOnes` — the user's sheets 02, 15 (the kurta girl) and 01 (the monkey) darkened to 30 % as if photographed in a dark room:

| Measure | Result |
|---|---|
| Angle agreement, dark vs bright (the guide's "robustness drop") | 28 of 30 figures (93 %) |
| The two misses | sheet02 #6 (back → side) at confidence 0.43, sheet01 #7 (front → back) at 0.50 — both below 0.65, so both are flagged "please confirm" in the review dialog |
| Light level on the dark copies | "very dark" on 29, "silhouette" on 1 |
| Confidence lower on the dark copies | 30 of 30 |
| A feeling on a back or faceless figure (the guide's "false emotion on back") | 0 of 60 readings |
| The gold set of v27 (`poseSenseReadsTheUsersSheets`: five sheets, the sure cases) | still passes with the new reading path |

## 4. The 3D Still Picture Maker manual

Test `dollPassesTheStillPictureChecklist` (a man in a white kurta and a black-haired monster, made by the doll maker at the large size):

| Check | Result |
|---|---|
| Every character prompt and the place prompt carry the six blocks and the negative list after the Technical Director's cleaning | yes (the negative list's "cropped body" was being stripped by the cleaner: it now says "body cut off at the frame") |
| Dolls at least 2048 px tall | 2057 px (the man), 2070 px (the monster) |
| Eyes with catch-lights | both, on both dolls |
| Skin glows (warm shadows, little shine) | both |
| The mouth is the face's, not the shirt's | yes |
| Light | the man "harsh" (16 % blown whites) at the first light, the monster "too dark" — both reported in the proposal |
| A white kurta lit again (softer key, stronger fill) | 75/100 → 100/100 |
| The 3D maker's real path (`makeCharacter`) | proposal "Still QA 100/100 … lit again after the first check" |
| A 300-px library picture as a reference / a 900-px one | refused / allowed |

Found by the checklist and fixed: the torso of an adult or a monster ended in a half-sphere as wide as the shoulders, which rose into the lower face (a monster's head was buried entirely, so its eyes had no catch-light because they were not visible). `Doll3D.torso` now ends in low rounded shoulders.

## 5. Shorter shots, many arms, riders

Test `manyArmedRidersAndShorterShots`:

| Check | Result |
|---|---|
| "आठ भुजाओं वाली … शेर पर सवार" → 8 arms, a lion, a woman, not an animal; "a six-armed god riding a peacock" → 6 arms, a peacock; "हाथ में लाल पतंग" → 2 arms, no mount; "एक बड़ा सुनहरा शेर" → a lion, no mount | all as expected (the cast block needs the script's "दृश्य 1:" heading, as every script does) |
| "चार हाथों वाला" → 4; "चूहे की सवारी" → a mouse (oblique form); "mounted on a white bull" → a bull (an adjective between); "a girl who loves her dog" → no mount | passes |
| The drawn puppet: eight arms fan wider than two at the shoulders; the lion under her wider than her feet | passes |
| The doll: wider than tall (the lion), her face in the upper part | passes |
| The rig of a rider's picture: arms fixed, legs off the walk | passes |
| The cut: 2 s; the sample story's shots | 476 shots, 1.34 s on average, the longest 2.0 s, none over the cut |

Test `riderAndAnimalBothSpeakAndBacksAreUsed`:

| Check | Result |
|---|---|
| Durga (on a lion) and a lion character "शेर" in the cast are linked | yes |
| The lion is not staged on its own while Durga is in the part | yes |
| Durga's two lines move her lips; the lion's line is a mount line on her actor | 2 and 1 |
| The lion's line has a mouth envelope; its shot is medium-wide (the whole picture) | yes, size 2 (medium-wide) |
| The rig of a rider's picture finds the animal's jaw on the side its head points to | yes (right) |
| "रामू पीठ फेरकर धीरे-धीरे चला जाता है" turns his back; the casting wants his back picture | at 19.6 s, yes |
| Sheet 15 (the kurta girl: crying, laughing, angry, sitting…) given at once: the main picture | a standing, neutral front |

## 6. Full suite

35 tests, 34 run, 1 skipped (the twenty-story soak, run with `-Dkahani.soak=1`): see the end of this file.

## 7. Retest as films: three new stories with 3D dolls

Three stories written for this round were made as films on the desktop (`DollFilm`, a harness that gives every character a 3D doll with its three made views and every place a 3D set, as the phone's 3D maker does, then runs the same director and renderer), and their frames were looked at every two seconds:

* **Durga and Mahishasura** (Hindi, three scenes): Durga with eight arms riding her lion, the lion speaking, a giant buffalo-horned demon, Indra; the demon turns his back and runs.
* **Ravana and Hanuman** (Hindi, two scenes): ten-headed Ravana, Hanuman jumping down from a tree, Sita.
* **The Park** (English, two scenes): two children and a dog; a boy turns away and walks home.

| Found in the frames | Fixed |
|---|---|
| Heads and eyes shaded wrongly, a neck showing through the chin | Every 3D sphere was wound inside-out (its front culled, its inside drawn): `Studio3D.Mesh.sphere` winds outward |
| The lion's face hidden behind its mane | The mane sits behind the face (`Doll3D.animal`) |
| Durga standing in front of the lion, not on it | A rider is built seated: thighs forward, shins hanging, the saree over the lap |
| Durga's shadow a dark stain on the lion; the hair's shadow a hard line across faces | Soft shadow edges (a 5×5 filter two texels apart) on every doll; no measurable cost at 2900 px |
| The lion's jaw rigged at its tail | The head is found as the bulky end of the picture (`Rig.findMountJaw`), the jaw opens 22° |
| Durga smaller than Indra (her picture holds the lion too) | A rider's picture stands 1.25× a standing person (1.0× on a bird) |
| The giant's head above the top of the picture in every wide shot, his close-ups showing his chest | The camera never looks above the place's picture, so the whole cast of a part is drawn smaller together until the tallest keeps its head inside (the parts drew everyone at 55 % and 45 % of the frame instead of 60 %) |
| A reaction close-up cutting Durga's crown | Reaction shots of a picture with a known face are framed by the face (`faceCam`), headroom included |
| Durga scared by Indra's fear | A listener whose own next line reassures ("डरो मत", "don't worry", "I'm here") reacts with resolve |
| The lion's jaw moved in the film, but only as a small warp of its snout: its speech did not read | A dark mouth with a tongue and a tooth opens between the snout and the lowered jaw (`Rig.mountMouth`), as for an animal of its own |
| Ravana's doll with one head | The doll copies its head for the others in a row (`Studio3D.Mesh.copyPart`); the picture widens for the row |
| Ravana among the heroes on the end page | The epics' villains by name are never heroes |
| Hanuman hanging upside down from the branch through the whole scene, his face out of every shot | "पेड़ से कूदकर नीचे आते हैं" (jumps down) brings him to the ground beside Sita |
| Hanuman drawn at a pet monkey's size | A vanara or a mighty monkey is a man's height |
| The lion drawn twice on the title and end pages | A mount appears under its rider only; the line-up is spaced by each picture's width |
| Heaven's court a garden to the director and a hall to the 3D maker; the sample's garden scenes a gate to the 3D maker | One reading of a place for everyone (`Sets.forScene`) |
| Battlefield between mountains read as a palace courtyard | Open land (battlefield, mountains, valley, desert, fields) is the open place unless a built place is named |
| Maya ("two ponytails with yellow ribbons") made a boy with short hair | Clothing and hair cues count for a girl; two ponytails are drawn as two |

What was looked at and found right after the fixes: the battlefield at dusk with hills; the demon's back view while he runs and his front when he speaks; over-the-shoulder shots with the back views; Durga's and the lion's separate lip-sync (her face changes 3 225 px when she speaks and the lion's head 0; the lion's head 760 px when it speaks); the night court with diyas; Ravana's ten crowned heads in wides and close-ups; Hanuman walking in and speaking on the ground; the park and street at morning and evening.

The Durga story was also made with drawn puppets (no pictures at all): Durga side-saddle on her lion, the giant's head inside every wide shot, the court and the battlefield as their own places.

Test `godsDemonsAndVanarasAreStagedWell` checks each fix in the app's own code paths.


## 8. Glasses, walking aids and a change of clothes

Test `aidsCostumesAndPicturesFirst` reads a Hindi story with six characters and an English one with a change of clothes.

| Checked | Result |
|---|---|
| दादाजी "गोल चश्मा", "लाठी" | spectacles and a walking stick (not a wand) |
| दादी "व्हीलचेयर पर रहती हैं" | a wheelchair; every key seated in it, never running |
| रोहन "तैराकी के गॉगल्स" | goggles |
| मीरा "बैसाखी के सहारे" | crutches; never running |
| जादूगर "जादुई छड़ी" | a wand, no walking stick |
| सूरदास "एक अंधा बूढ़ा गायक" | dark glasses and a cane |
| The wheelchair doll | dark tyres at the bottom (4 618 pixels); the face lower than standing |
| "Maya comes back wearing a red frock with golden stars" | a costume: a frock in a new colour, worn from that line on |
| "Grandpa takes off his glasses" | a costume with no glasses |
| Maya's picture recoloured for the frock | made; asked for as "costume:Maya#1"; once uploaded, saved and no longer asked |

## 9. More situations

Test `injuriesUmbrellaWalkerAndHealing`, and the same story made as a film twice on the desktop (drawn characters, then 3D dolls) and looked at every second.

| Situation | How it is read | How it is drawn and moved |
|---|---|---|
| A leg in plaster ("पैर में प्लास्टर", "broken leg") | `Look.INJ_LEG` | White plaster from below the knee; the step onto it is shorter; a limp (the step onto the hurt leg sinks deeper and leans); never runs or jumps |
| An arm in a sling ("हाथ गले में लटका", "arm in a sling") | `Look.INJ_ARM` | The forearm in plaster across the waist in a pale blue sling; that arm never gestures |
| A bandaged head ("सिर पर पट्टी") | `Look.INJ_HEAD` | A white bandage round the forehead with a pad |
| A blindfold ("आँखों पर पट्टी", Gandhari) | glasses 4 | A dark cloth over both eyes, knotted behind; no blinks painted over it |
| An eye patch ("एक आँख पर काली पट्टी", "eye patch") | glasses 5 | A black patch on a strap |
| A walking frame ("वॉकर के सहारे") | `Look.AID_WALKER` | A frame in front with both hands on its grips; slow steps |
| An umbrella ("हाथ में लाल छाता", "छाता लेकर आती है") | `Look.umbrella` | Opens over the head while it rains outdoors (and keeps its carrier dry); drawn over pictures too |
| "डॉक्टर मोनू के सिर की पट्टी खोलते हैं" | a costume: the head bandage off | The bandage is gone from that moment |
| "रोहन का प्लास्टर कट जाता है" | a costume: the leg healed, the crutches gone | He walks freely again |

**Found and fixed while making the films:**

* The bandage taken off ("पट्टी खोलते हैं") was read only as a blindfold coming off — it now heals the part named (the head), or every bandage when no part is named.
* Rohan kept his crutches after his plaster was cut — crutches go with the plaster (an old person's stick stays).
* The sling read as a bowl held at the waist — it is now a narrow triangle under a clearly white forearm.
* A doll's change of look was only its picture recoloured, so Monu kept his bandage on his doll — the 3D maker now proposes a doll in each new look with the character's own doll (accepted or rejected together), with its own face points; a picture the user gives always wins.
* "The doctor bandages Rohan's leg" names two people — the hurt one is the owner of the leg ("रोहन के", "Rohan's").

## 10. Pictures first, and cues from the pictures already uploaded

* Started from the app, a film now waits before the 3D maker: "Waiting for you: please add pictures of Zara, the surface of the moon", with two buttons on the progress screen (add pictures, or let the 3D maker make only these). Nothing is built in 3D while it waits. A switch in Settings ("Ask me for my pictures first") turns this off.
* The 3D maker's style cue comes from the library's pictures when the story has fewer than four of its own: "Style cue from 2 reference picture(s): key light from the right, warmth +0.12, saturation 0.37, contrast 0.26, 2 skin tone(s)".
* A doll made with a reference picture takes that picture's skin and hair colour, and the style cue never overrides that skin.

## 11. Final check of lip-sync and movement

A desktop check (`LipMotionQc`, in the session's scratch tools) makes each story's voices, mouth envelopes and film exactly as the film tool does, then reads the mouth of every speaking character 30 times a second through the renderer's own `mouthAt`, and every character's position frame by frame.

What it measures for lip-sync: whether the mouth is open on loud frames, shut in pauses of 120 ms or more, and shut before and after each line; how closely the opening follows the voice's loudness; how many openings there are per second of speech; and the largest change of the opening between two frames.

**Before the fix (situations and park stories):**

| Story | Open while loud | Shut in pauses | Correlation with loudness | Largest change between two frames |
|---|---|---|---|---|
| Situations | 100 % | 100 % | 0.69 | 0.77 |
| Park | 98.4 % | 100 % | 0.72 | 0.72 |

The mouth snapped open and shut within a frame on many syllables — chatter, not speech.

**The fix (`Mixer.envelope`):** a slightly slower opening, a gap fill that keeps half the opening through a dip shorter than about 90 ms (the animators' rule of not flapping on every syllable), a glide limit of 0.08 of a full opening per 10 ms applied after the 40 ms lead, and the envelope starting and ending closed; the renderer eases the first and last 60 ms of a line.

**After:**

| Story | Open while loud | Shut in pauses | Shut before and after | Correlation | Openings per second | Largest change between two frames |
|---|---|---|---|---|---|---|
| Situations (Hindi) | 99.4 % | 100 % | 100 % | 0.79 | 3.2 | 0.40 |
| Park (English) | 100 % | 100 % | 100 % | 0.81 | 2.9 | 0.40 |
| Durga (Hindi) | 99.6 % | 100 % | 100 % | 0.78 | 3.4 | 0.40 |
| Ravana (Hindi) | 99.0 % | 100 % | 100 % | 0.72 | 4.0 | 0.40 |

The only changes near 0.4 are the first opening of a line; within a line the opening changes by at most about 0.27 a frame.

**Movement:** no character jumps while it is visible in any of the four stories. The large one-frame position changes the check first reported were characters placed off-screen just before they walk in.

**Rerun on the final code** (after the eye-lines, the moves read from stage directions and the splitting fix), with a fifth story (the change of clothes) and the movement measured only while a character is seen:

| Story | Open while loud | Shut in pauses | Shut before and after | Correlation | Openings per second | Largest change between two frames | Fastest visible move per frame (30 fps) | Jumps while seen |
|---|---|---|---|---|---|---|---|---|
| Situations | 99.4 % | 100 % | 100 % | 0.79 | 3.2 | 0.40 | 13.6 units | 0 |
| Park | 100 % | 100 % | 100 % | 0.81 | 2.9 | 0.40 | 14.4 units | 0 |
| Durga | 99.6 % | 100 % | 100 % | 0.78 | 3.4 | 0.40 | 9.2 units | 0 |
| Ravana | 99.0 % | 100 % | 100 % | 0.72 | 4.0 | 0.40 | 17.5 units | 0 |
| Costume | 100 % | 100 % | 100 % | 0.79 | 3.4 | 0.40 | 14.1 units | 0 |

The stage is 1280 units wide, so the fastest visible move is under 1.4 % of the width a frame (a run); the larger one-frame changes the first count showed are placements made while a character is off the stage.

What this does not show: these numbers say the mouth follows the voice smoothly and rests when it should. They do not make the drawing itself studio quality: a picture's mouth is still warped from the user's picture or the doll's mesh, not animated shape by shape by an animator.

## 12. More realism: light, reflections, sounds and eye-lines

| What | How | Checked |
|---|---|---|
| The sun's shadow | `Renderer.sunShadow` and `onGround`: each picture's own outline (a blurred 64-px silhouette made with the picture, `Art.castShadow`) laid on the ground away from the sun — long and to one side in the morning and evening, short at noon, fainter under cloud; none at night, indoors, in a cave mouth, or under rain, fog or snow | The park story at morning, frame by frame: the characters' shadows fall the same way as the 3D set's own shadows and bend with the body |
| Wet ground | A faint reflection under the feet (14 % at full wetness) | The rain scene of the situations story |
| Rain on an umbrella | `Director.umbrellaRain`: a drumming sound for as long as an open umbrella is in the rain | Situations story: from 15.0 s for 24.1 s |
| Steps on the floor of the place | `Director.walkSounds`, `floorSteps`: stone rings, earth thuds, running steps; a stick's tap, crutches, a wheelchair's roll | Situations story: steps at 14.9 s, crutches at 20.3 s, a stick at 25.2 s; aids story: stick, running steps, wheelchair, crutches |
| Eyes that look | `Renderer.gaze`: a listener's eyes on the speaker, a speaker's on the nearest listener, a small quick shift every 1.5–2.5 s between; in a picture the iris moves inside the face mesh while the corners stay (`Rig.faceMesh`), in a drawn character the pupils (`Puppet.drawEyes`) | The sample story's over-the-shoulder shot at 1:09: the listener's eyes turned to the speaker, the lids and the corners of the eyes in place, no tearing of the face |

**Found and fixed:** the park, the street, the rooftop and the festival ground were not counted as places under the sky (`Renderer.outdoor` listed only nature's places), so they had no sun, no shadow and no wet ground. Light now follows `Sets.outdoorSet` (`Renderer.sunlit`); `outdoor` is kept only for leaves in front of the lens and rays through trees.

## 13. The director trained for these situations

`SituationsGuide` has fourteen rules, each with the code that applies it (shown on the protocols screen); every shot list ends with a line saying what was done for that story. On the test stories:

| Story | Aids | Glasses, goggles, blindfold, patch | Hurt | Umbrella | Changes of look | Reveals | Concern / relief | Seated eye-level shots | Footsteps / aid sounds / rain on umbrella |
|---|---|---|---|---|---|---|---|---|---|
| Situations | 2 | 3 | 3 | 1 | 2 | 0 | 0 / 2 | 0 | 1 / 2 / 1 |
| Aids | 3 | 2 | 0 | 0 | 0 | 0 | 0 / 0 | 1 | 1 / 3 / 0 |
| Costume | 0 | 1 | 0 | 0 | 2 | 1 | 0 / 0 | 0 | 0 / 0 / 0 |
| Sample (Hindi) | 0 | 0 | 0 | 0 | 0 | 0 | 0 / 0 | 5 | 31 / 0 / 0 |

**Stage directions move people (`Director.travelFrom`).** A check of the park story found that nobody moved at all: "Kabir runs across the grass" and "Kabir turns away and walks slowly home" were read for the turned back only, and the director's moves were phrases of the sample story. Now, in any story, a character runs across, walks off and is gone, comes back, or goes up to someone, whatever words stand between the verb and where they go. Test `storyDirectionsMoveTheCharacters`:

* Kabir runs across the grass for 1.8 s (running steps heard), walks slowly home for 3.3 s and is gone; Maya ends an arm's length beside him after "Maya runs to Kabir" (155 stage units; the first try stopped her at 95, shoulder to shoulder with him in the doll film);
* Maya runs out of the room, is off the stage for a second while the camera stays on Grandpa, and walks back in her red frock; the reveal follows;
* "रोहन घर चला जाता है": he goes and is gone; "मीरा दूर चली जाती है" followed by her own line: she goes only to the edge and is still seen when she speaks;
* the sample story's own staging is unchanged (no move added there).

Sentences checked to move nobody, or the right people: "Maya runs to the door and Kabir follows her with grace" (no run to Kabir: he is not named after "to"; "grace" is not "race"), "Maya takes off her cap" ("off" counts only right after a verb of going), "the small pond" ("small" is not "all"); "They all walk home together" (no names: everyone on the stage goes — Maya off and gone, Kabir to the edge because he speaks again).

**Found and fixed while building it:** a run off the stage was slowed to a walk by the speed limit (the exit's running speed was above the limit for a child) — it is now under it; the turned-back rule planned a "turn round" 2.4 s ahead that blocked the walk off — leaving words keep the back turned until the character has gone; a character who walked off had to stand at the edge when she came back later in the scene — "comes back" now brings her back after a moment off the stage.

## 14. Characters with no picture: borrowed from your uploads

Test `madeCharactersFollowTheUploadedPictures`: four of the user's sheets (two girls, a monster, a monkey) saved to the library; a story with Asha (her own sheet), Neha (no picture; two plaits and round glasses) and Dadi (no picture; a wheelchair).

| Checked | Result |
|---|---|
| Asha | her own picture, placed by the director from the library |
| Neha | made from the other girl's front figure ("मीना (front, picture 7)", fit 83 %), recoloured to her yellow frock, her round glasses painted on; never Asha's picture, never the monkey's |
| Dadi in her wheelchair | the 3D doll (a standing figure cannot show the chair) |
| Neha's picture rejected | the 3D doll next time |

**Found and fixed:** (1) the 3D maker's 512-px quality rule ruled out every figure cut from a sheet (a sheet of ten in a 1600-px picture gives figures about 490 px tall), so no made character could ever borrow from the user's sheets, nor take a doll's skin and hair colours from them — a figure from a sheet now counts from 300 px (a single picture still needs 512); (2) the older "fits the description" step could make Neha out of Asha's own picture (two characters with one face) — another character's picture is now never used; (3) that step also gave a standing figure to a character in a wheelchair — it now leaves those to the doll.

## 15. Splitting rechecked on all 60 sheets

Every sheet split as the app does it (`Angles.figures(Angles.split(…))`) and looked at as a strip of its pieces; then every piece the split threw away was measured (size, how far from the nearest figure).

* All 60 sheets give the same number of figures as before: 10, 11 or 12.
* Only three pieces were thrown away in all 60 sheets: a 29×21 speck beside one figure's feet (sheet 4), and on sheet 12 the leaping girl's raised sword with the tip of her braid (134×167 px) and one of her feet (54×84 px) — both touching her outline, cut loose by the split and dropped as crumbs.
* **Fixed (`Angles.rejoin`):** a dropped part that touches a kept figure (an opaque pixel within half a percent of the sheet's smaller side) is joined back to it; crumbs under 0.05 % of the sheet stay dropped, and a place sheet's panels (whole crops) are never joined. Sheet 12's girl now has her sword (4 549 px back in place) and her foot (1 866 px); the strips of the other 59 sheets are unchanged pixel for pixel.
