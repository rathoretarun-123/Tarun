# Testing v40

What was checked for v40, with your family pictures (the same 36 sheets and photos as v39, `tools/testdata/sheets38`),
your script "चाय पर बात" (`tools/testdata/chai_par_baat.txt`), the three family stories of v39
(`tools/testdata/v39stories`) and the 25 earlier test stories.

## 1. The lips at वृंदा's neck

| Check | Before | v40 |
|---|---|---|
| वृंदा's first picture (a three-quarter view with glasses) | one "eye" read on her ear, the mouth point on her jaw and neck: the mesh opened lips there | her whole standing front picture is the rig (her three-quarter picture is kept as a source picture); the mouth point is on her lips |
| मम्मी's mouth point | on the tip of her nose | moved onto her lips (found by their colour) |
| पापा, वनुशा | on the lips | unchanged |
| 18 test faces (your four, 14 of the sample library) | — | the three points on a jaw, a nose and a moustache moved onto the lips; three moved a pixel or two; twelve stayed (two of them still wrong: a laughing pose picture whose eyes were read at its eyebrows, and a picture whose face is not found — neither is a picture a rig is made from) |

How the lips are found: pixels redder (a lower, pinker hue) than the face's own skin, or the dark red line where the
lips meet; the most compact patch of them with skin above it (between the nose and the lips) and a chin or beard
below it (a pink collar or a ribbon has the dress below it), about one eye-distance below the eyes. A point already on
lip colour is never moved; a green or blue face, or lips hidden by a moustache, keep the point as before.

## 2. Turning to each other

| Film (640 px, 24 fps, desktop) | Drawings of a character | Head turned 10° or more |
|---|---|---|
| चाय पर बात | 5,471 | 86 % |

* A listener turns to the one speaking, and keeps looking at them in the pause after the line (2.5 s).
* A speaker turns to the one spoken to: the one who spoke before (within 8 s), else the one who answers, else the
  nearest.
* The eyes lead, the head turns first, the body follows a tenth to half a second later (eased: no snap).
* In a close-up the turn is about half (the face stays to the camera, the eyeline just off it).
* The turn is the rig's mesh turned like a rounded body: up to about 30° for the body, 40° for the head; checked on
  200 points per row that no part of the picture folds over another.

## 3. Walking

| Story / moment | v39 | v40 |
|---|---|---|
| रविवार की सुबह — "वृंदा धीरे-धीरे चलकर खिड़की के पास जाती है" | 0.9 s, a third of her height (a slide) | 2.8 s to the far side, a stride every 0.3 of her height covered |
| the same walk, her picture | her three-quarter picture looking left while she went right (backwards) | her front rig turned toward where she goes |
| "वृंदा और वनुशा कमरे से बाहर जाती हैं" (test story) | only वृंदा left | both leave |
| "Asha and Raju walk up to the cave" (earlier test story) | only Asha | both |

The step: the legs swing past each other from the hips (15° walking, 24° running), the swinging leg bends and lifts
its foot, the arms swing against the legs, the body leans a little into the walk, rises as the legs pass and comes
down onto each step so the planted foot stays on the floor; the steps keep pace with the ground covered, slowing as
the walk eases in and out. A quarter of a second before the first step they turn toward where they are going.

## 4. The 25 earlier stories

Speakers and places exactly as in v39. Moves: two changed, both as asked — a spot walk in `manual_test` is now long
enough to be seen (2.7 s instead of 2.1 s), and in `soak11` Raju walks up to the cave with Asha.

## 5. Your park pictures (`tools/testdata/sheets40`)

| Sheet | Pieces | Notes |
|---|---|---|
| red swing and slide, 6 views | 6 panels | the park's views |
| wooden swing, 6 views | 6 panels | two read as views from above (library only) |
| slide, 6 renders | 6 | the slide as a thing |
| swing seat, 6 panels with a thin grey frame | 6 (was 5) | the frame lines are taken out before the background; a chain cut at an edge-on link is joined again |
| boy, two turnaround sheets | 12 | front, three-quarter, side and back read right on all |

"झूला और फिसलपट्टी": आरव runs to the swing (0.8 s) and swings (6.5 s), stands, and leaves; वनुशा walks to the slide and
slides down; both leave together at the end.

## 6. "अलाव के पास" with your five pictures (`tools/testdata/sheets40b`, `tools/testdata/v40stories/alav_ke_paas.txt`)

| Check | Before | v40 |
|---|---|---|
| The script | no character, no place, no scene found ("नाम [direction] - "line"", dashes after the headings) | 3 men from the list, the field, 7 things, the night from "समय - रात 11 बजे", 11 lines each with its speaker and what they do |
| अर्जुन's, कबीर's, समीर's sheets | fronts read as three-quarter, two profiles as backs, समीर's back as a front | front, three-quarter, side and back for each (one profile of समीर as a three-quarter) |
| अर्जुन's cream shirt | holes where the white paper's removal ran into its light folds | cut whole (a clean white sheet is cut to a smaller colour distance) |
| The fire place sheet (6 night views, no margin) | not split at all | 6 panels by the white gutters; the view from above kept in the library only |
| The things sheet | 6 (the lighter dropped as a "loose prop") | 7: log, firewood and upla, rod, kulhads and kettle, bottle and blanket, matchbox, lighter |
| The fire | a drawn fire at a free spot | your picture's own flames, cut out of it, flickering where the picture has them; the place drawn at the people's depth |
| Who sits | everyone stood the whole film | अर्जुन and समीर on logs either side of the fire from the first frame |
| कबीर | stood still, then sat at 96 s for the rest of the film | 26 stretches of walking round the fire with stops between; from behind to the front at his first line, the other way round at his second, two seconds on the far log and up again at his third, a stop for his last |
| "चुनाव" in a line | opened the sea and a boat | nothing (the boat word must start a word) |
| समीर's kulhad in both hands | made him walk over to hold अर्जुन's hand | stays seated with his kulhad |

Angles over all the test sheets: the changes on people are mostly better (a girl's profile, a profile with a braid, two
fronts of a jumping girl, a laughing front, अर्जुन's profile); two waving or crying figures moved between three-quarter
and side. The sample backs and fronts (वृंदा, राजू, कृपा) read as before.

## 7. The suite

## Said plainly

The rig is a flat picture turned and bent on a fine mesh, not a 3D model: it turns convincingly to about a
three-quarter view, never to a full profile, and it cannot show a side the picture does not have. The film is not
Pixar or Disney quality.
