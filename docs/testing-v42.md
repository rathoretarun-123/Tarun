# Testing v42 — more stories, your three-quarter and top pictures, the picture selector, faces and bodies

Your message: write more stories to check; three-quarter views and top views will also come from you; train the
picture selector again so it picks the right picture every time; lips, facial features and body parts identified
and meshed correctly; the physical and natural effects right.

"Every time" and "perfect" cannot be promised by any reader of pictures. What follows is what was measured, what was
wrong, what was fixed, and what is still not right.

## 1. 25 more stories (`tools/testdata/v42stories`)

Twenty-five new stories in your format (cast, places and things listed first, every line in double quotes): rain and
clothes on the line, a kite, a see-saw, a bus stop, a birthday, the fire of "अलाव के पास", a scooter ride, Hinglish
lines, English lines. Each has an `expected.tsv` row: the cast, the number of places, things and scenes, and every
spoken line with its speaker.

* **Reading:** 9 misreads were found and fixed — a bare "स्थान" heading, a bare "आरंभ" (start), title lines written
  "शीर्षक | …" or "# …", numbered places and things written "1. नाम - विवरण", a thing's "रंग:" / "विवरण:" field taken
  for a new thing, two-letter place names, ages written "प्रिया - 40,", "|" inside scene headings, and an English
  line ("In your hand, Papa.") turned into Hinglish. Now **25 of 25 stories and 144 of 144 lines** read right; the 20
  stories of v41 still read right (59 of 59 lines).
* **Directing:** a see-saw rider walked while seated (fixed: the see-saw's keys stand, not sit); समीर walked in onto
  अर्जुन's spot (fixed: an entrance ends on free ground); one opening wide ran 4.3 s because its cut waited for a jump
  to land (fixed: the cut comes before the jump when waiting would pass the 4-s cap). Now 0 issues, 0 shots over 4 s.

## 2. All the test stories with your pictures

179 stories (the 100 soak stories, the 34 earlier ones, the 20 of v41, the 25 new ones), each directed with your
family's, राहुल's or the three men's pictures: **0 crashes, 0 shots over 4 s, no lip-sync shot over six words.**

* A mistake in the test bench was found on the way: the folders holding the family's and the three men's pictures for
  this test had been cleaned away, so for part of the earlier runs those stories were directed with drawn stand-ins.
  They were rebuilt from your sheets, and the test now fails loudly when a picture folder does not load.
* **अलाव के पास:** कबीर, circling the fire, made his sudden stops right on top of seated अर्जुन (20 px apart) and next
  to समीर. Now a stop on the ring never comes where someone sits — he walks on past them.
* Still flagged: two moments where two people stand on one spot in generated test stories (a scooter parked where a
  re-staged part put समीर; one scene of the old sample story) and one line whose speaker is not in the scene (the old
  "neo_mumbai" story). These were there before v42; they are not in your own stories.

## 3. The picture selector, measured on your sheets

Every figure of your character sheets — 177 pictures, cut exactly as an upload cuts them — was labelled by eye
(front, three-quarter, side, back; whole standing, walking, half figure, close-up, sitting, crouching, jumping):
`tools/testdata/labels/figures.tsv`. The selector reads each one as an upload does.

| | before v42 | v42 |
|---|---|---|
| angle read right, all 177 figures | 140 (79 %) | **148 (84 %)** |
| angle read right, the 71 whole standing figures | 61 | **67** |
| the slots (front, three-quarter, side, back) chosen for your 8 characters | — | **30 of 30** right |
| the family's slots | 8 of 14 | **14 of 14** |

Learned classifiers (trained on the labels) were tried and rejected: on figures they had not seen they were worse than
rules. The rules that were added: a standing front with a narrow, lopsided head is a three-quarter; a "back" with eyes
seen and the face off to one side is a side; a tall turned figure is never thrown out as "cut at the feet"; when no
side picture is found, the most turned whole three-quarter one stands in. The 29 figures still read wrong are mostly
close-ups and seated pictures (side read as three-quarter: 8; three-quarter read as side or front: 9; front read as
side or back: 8). You can correct any reading in the picture review (tap "Angle:").

## 4. Your three-quarter and top pictures

* **Three-quarter:** your own three-quarter picture of a character is now used in a two-shot or an over-the-shoulder
  shot while that character **listens** — standing still for the whole shot, not speaking, doing nothing else, turned
  to someone who is there. A cut brings it in and a cut takes it out (never a swap in mid-shot). It is not used while
  they speak: on your sheets the mouth of a turned face was found on the neck or the nose in five pictures of eight,
  so a three-quarter picture would move the wrong place.
* **The front that plays has free hands:** in the first render प्रिया listened in her three-quarter picture without
  the tea tray of her front picture — the tray vanished and came back at the next cut; the tray also went with her
  through the rain, and when she watered the plants a watering can floated beside it. Your sheets also have a plain
  standing front of her: now a front whose hands hold something gives way to a plain standing front of the same
  person when there is one (the tray picture stays in the library), and her back picture is the plain one that goes
  with it (a back must be about as tall for its width as the front; her back with the tray stuck out at both sides).
  A front whose hands stay held (वनुशा's toy, कबीर's hands in his pockets — no other front of them) is never swapped
  for a turned picture.
* **Top:** a picture you mark as seen from above (in the picture review, tap "Angle:" until it says "top") is that
  character's top view. A point-of-view shot that looks down — "छत से नीचे देखती है", "बालकनी से झाँकता है", "looks
  down", "from the roof" — shows them from above on the ground of the place. A top picture is never guessed, and is
  never used as a front or a turned view.

## 5. Faces and bodies on every picture of yours

Every picture the films use — 8 fronts and 20 turned pictures (three-quarter, side, back) of your 8 characters — was
drawn with what the rig reads in it: the eyes, the mouth, the chin, the shoulders, the hips, where the hands end and
the two legs. Found and fixed:

* **राहुल's mouth was on his T-shirt's collar.** His cartoon smile is a thin line, and the dark line of the collar
  under his chin scored higher. A mouth must now have the face on both sides of it (cheeks of skin or beard), never a
  shirt. Over all 177 labelled figures this changed 6 mouths: 4 moved from the collar to the lips (both of राहुल's
  sheets, a bearded पापा, a laughing toddler), 2 stayed wrong as before (a close-up with closed eyes, a back), none got
  worse. With the mouth right, राहुल's chin, shoulders and hips are right too.
* **Side, three-quarter and back pictures had their shoulders at the ears** (no face in them to measure from), so the
  arms swung from the head and the legs bent at the belt. They now start from where the front picture of the same
  person has them, and the hips are taken from the front as they are. Of the 19 turned pictures that are used, 16 now
  have the shoulders at the shoulders and the hips at the waistband; समीर's three-quarter and कबीर's three-quarter and
  side pictures still have the shoulders a little low, on the upper chest (कबीर's front, hands in pockets, cannot
  guide them). वनुशा's waving three-quarter picture is not read well, but it is never used for her.
* **Hands that hold something stay still.** वनुशा's toy and कबीर's hands in his pockets (and प्रिया's tray, in a
  picture set where it is her only front): the rig no longer swings those arms (a swing bent the tray). The hanging
  hands of the others swing as before.
* The eyes and mouths of all 8 fronts are on the eyes and lips.

## 6. Physical and natural effects

"बारिश वाला रविवार" was rendered three times with your family's pictures and looked at second by second (the room,
the drive, the rain, the evening with the rainbow), and अलाव के पास was checked for its fire. What was wrong:

* **The car vanished after the drive.** पापा parked, everyone got out — and the car faded away beside them. A car,
  a scooter or a bike ridden in now stays parked where its rider got off, until it is ridden again (behind the
  family in the rain, the cycle and the bike in the evening).
* **Stars beside the rainbow.** The evening after the rain had a full night of stars in the same sky as the rainbow.
  A rainbow needs the low sun: no stars while it shows, and an evening sky shows only a few faint ones.
* **Water on the sofa.** Standing at the end of the drawn sofa, प्रिया watered its arm. A task done on the floor in
  front of someone (watering, sweeping, a bucket, a stove, a rangoli) now turns them to the open side, or takes them a
  few steps to open floor.
* **A cut waiting for a jump** made a 4.3-s shot (section 1); the fire walker stopping on a seated man (section 2).
* Checked and right: the rain streaks and the wet darkening of everyone in the rain, the umbrella over प्रिया, the
  puddles and the splash of the jump, the thunder's flash, the rainbow after the rain, the fire's light and sparks,
  no floating feet (3519 feet positions on the ground line), no flicker or shake (the film's own frame-by-frame check:
  largest change between two frames 10.6 of 14 allowed).
* Not right yet: a drawn sofa is flat beside your painted rooms, and a person standing at its end is partly hidden by
  its arm; a held thing for a task (the watering can) is at the hand but a picture's arm does not lift with it.

## The last checks, on the final code

* All 179 test stories directed with your pictures: 0 crashes, 0 shots over 4 s, no lip-sync shot over six words;
  still flagged (from before v42, none in your stories): 2 moments of two people on one spot in generated stories,
  1 line whose speaker is not in the scene in the old "neo_mumbai" story.
* The script reader: 25 of 25 new stories (144 of 144 lines), 20 of 20 of v41 (59 of 59 lines).
* The desktop copies of the app tests: 5 of 5.
* The whole app on the phone emulator (Robolectric): **76 tests, 0 failures** — among them the new ones: the 25
  stories, the angles of the 177 labelled figures read through the phone's own picture decoding (148 of 177, whole
  standing 67 of 71), the four mouths moved off the collar, प्रिया's plain front and matching back, the parked car,
  the top view, कबीर's walk round the fire.
* Version 42.0; the APK is `release/KahaniFilm.apk`.

## 7. Said plainly

* The picture selector reads 84 % of your sheet figures right, not 100 %; it chooses around most of the misreads, and
  the review screen lets you correct the rest.
* A turned (three-quarter or side) picture does not lip-sync; it is used only for listening and walking.
* The rig is a flat picture on a fine mesh, not a 3D model. The films are not Pixar or Disney quality.
* Testing finds and fixes the kinds of mistakes it looks for; it cannot promise that no bug is left.
