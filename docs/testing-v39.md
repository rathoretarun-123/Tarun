# Testing v39

What was checked for v39, with your own pictures (36 sheets and photos in `tools/testdata/sheets38`) and your
script "चाय पर बात" (`tools/testdata/chai_par_baat.txt`), plus three new stories written with your four
characters (`tools/testdata/v39stories`: रविवार की सुबह, पार्क में पिकनिक, दोनों बहनें).

## 1. The rigged picture plays every shot

| Check | Result |
|---|---|
| Character appearances in the chai film's shot list played by the rigged front picture | 123 of 125 (the 2 others: a back view behind a shoulder) |
| Full chai film (640 px, 24 fps, 4 m 17 s on the desktop): character drawings | 5,471 |
| Drawings with a living face (lips, blinks, feelings on the rig) | 100 % |
| Speaking drawings with the lips moving | 100 % (961 of 961) |
| Blinks / drawings showing a feeling | 167 / 1,186 |
| मम्मी's front picture was her back with the tray | fixed: the best whole standing front with a face plays |
| Views that turn the rig | only whole standing figures (a bending, seated or half-length picture is left out) |
| Mesh | one cell per screen pixel (up to 1024 body rows, 512 face rows) unless "faster drawing" is switched on |

Made views (Studio 3D from one front picture) were made on the desktop with the `MadeViews` tool to look at them:
the three-quarter views are usable, the side and back views made from a single front picture are rough (thin side
views, smeared hair). In the app they stay proposals you accept or reject.

## 2. Reading your pictures (silhouette framing)

141 of your pictures were labelled by hand (whole standing, seated, half-length, face and shoulders) and read by
`PoseSense.framing` at the size the app reads them (320 px): **136 right**. The 5 misses: half-length pictures
showing both thighs (read as whole), and a laughing face over a cup at the frame's edge.

Before v39 the reader called 21 of पापा's pictures "running" (busts and cross-legged ones). Pictures uploaded
before v39 are read again once (`Studio3DArt.reframe`).

## 3. Sitting, walking, getting up

| Story / moment | Before | v39 |
|---|---|---|
| चाय पर बात — everyone on the sofa | the rig stood in front of the sofa | sits in the sofa (its front over the legs); the same seat from shot to shot |
| रविवार की सुबह — "वनुशा दौड़ती हुई कमरे में आती है" | already standing there | runs in |
| "वृंदा धीरे-धीरे चलकर खिड़की के पास जाती है" | no walk | walks across |
| "तरुण सोफे से उठकर खिड़की के पास आता है" | stayed seated | gets up, then walks |
| पार्क में पिकनिक — "चारों पार्क में चलते हुए आते हैं" | found standing | the four walk in, one after another |
| "वनुशा उठकर झूले की तरफ़ दौड़ती है" | ran | gets up and runs |
| "चारों भागते हुए पार्क से बाहर जाते हैं" | no one moved | all four run off by one side |
| दोनों बहनें — "वृंदा उसके पास आकर बैठती है" | sat where she stood | walks to her sister, then sits |

The 25 earlier test stories: speakers and places exactly as in v38; postures and moves changed only where a
direction now reads right (for example "मीना दौड़ती हुई आती है", "Asha walks in", "राजा तरुण और रानी प्रिया
दौड़ते हुए आते हैं" are now entrances, "तीनों पत्थरों पर बैठे हैं" seats all three).

## 4. Uploads

Test `v39HundredPicturesPlacesThingsLibraryAndReframe`:

* 12 sheets at once for पापा (the limit was 10): 71 pictures saved; every new pose line carries the cut mark.
* 7 place pictures for the living room: 42 pictures of the place kept in the film (wide, reverse, 40 views),
  the Studio card says "42 pictures of this place in the film", and the film reads all 40 views.
* A props sheet and a tray picture as a thing: 12 pictures of the thing kept and counted.
* 11 pictures and a broken file into the library: every good one kept (sheets split into more), the broken one
  counted ("1 could not be opened") and never put in the library.
* An old seated pose line read again: it now sits, and it is read once only.

## 5. The suite

The full Robolectric suite was run after these changes; its result is in the commit that carries this file.

## Said plainly

This is a 2.5D studio on a phone: a rigged picture bends, walks and speaks, but it is not a 3D model, and the
film is not Pixar or Disney quality. Some angles of your sheets are still read wrongly (about 1 in 30); the
studio shows its reading after each upload so you can correct it.
