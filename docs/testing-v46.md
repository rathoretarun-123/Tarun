# Testing v46 — सुंदरपुर गाँव: your ten scripts and your characters, retested

Your message: ten short scripts for your Pixar-style characters of सुंदरपुर गाँव — रमेश, सीता, आरव, अनाया, दादाजी,
चिंकी the monkey and गौरी the cow — with sixteen pictures, and "retest all with new scripts and characters". Then eight
more pictures: three more sheets of दादाजी, three of चिंकी in a red cap, the mango sacks with the brass pot and the
stick, and the mango tree in the rain.

All 24 pictures are in `tools/testdata/sheets46`, the ten scripts (each with your cast and place lists) in
`tools/testdata/v46stories`.

## 1. Reading your scripts — what was wrong, now fixed

| what happened at first | now |
|---|---|
| "Script 1: गौरी की मदद" was read as a character called "Script 1" speaking | "Script N: title" starts the script; the title is the film's title |
| the lists "पात्र:" and "जगह:" were read as lines of dialogue; no one was described | they are the cast and the place |
| चिंकी (बंदर) and गौरी (गाय) were people | a monkey and a cow |
| सीता (औरत, 40 साल) was a man | "औरत", "महिला", "पत्नी", a salwar or a braid make a woman |
| दादाजी had no cap, so "चिंकी दादाजी की टोपी ले भागा" had nothing to take | a cap named in the story ("दादाजी की टोपी", "मेरी टोपी!") is put on its owner |

## 2. What is said is done

Most of the action in your scripts is in the dialogue. The director now does what a line says the speaker (or all of
them) will do, after it is said:

* "मैं छाता लाता हूँ!" — आरव fetches the umbrella. "मैं झाड़ू लाती हूँ" — सीता fetches a broom and sweeps.
* "मैं भी!" — the same deed again, by this speaker (the second small pot).
* "चलो, सब मिलकर खींचते हैं" — everyone pulls. "चलो उसे छप्पर के नीचे लाते हैं" — "उसे" is the one just spoken of (गौरी).
* "लो चिंकी, ये रहा आम" — the mango is handed over. "जल्दी दादाजी को बुलाते हैं!" — he is called.
* "भागो!" after "दौड़ लगाएँ?" — the race. "ऊँ ऊँ! यहाँ है!" while the calf is missing — the calf is found.
* A question, a refusal ("आज बाहर नहीं जाना") or a line about others ("अच्छे बच्चे बाँट कर खाते हैं") does nothing.

## 3. The ten stories, staged

| script | what is on the screen |
|---|---|
| 1 गौरी की मदद | गौरी sinks to the knees in a patch of mud and struggles; अनाया waves दादाजी over; all five line up side-on on a rope to her neck and pull through "एक, दो, तीन!"; out she comes; they stagger back and clap |
| 2 चिंकी और आम | चिंकी jumps for a mango from the tree and runs off with it; सीता walks over and hands him one |
| 3 दादाजी की कहानी | everyone sits under the tree from the first frame, दादाजी on a stool, the children facing him |
| 4 बारिश का दिन | heavy rain; the family under a thatched shelter — dry under its roof — गौरी out in the rain; रमेश and अनाया bring her in; आरव fetches the umbrella |
| 5 खोया हुआ बछड़ा | गौरी sad; the search of the fields (hands shading the eyes); चिंकी points and jumps — the calf (a smaller गौरी, from your picture) walks to her; she is happy |
| 6 दौड़ का मुकाबला | the two line up and run, ending clear of दादाजी; then they fetch the grass together |
| 7 मेले की तैयारी | the fair's crowd and drum; आरव hangs strings of little flags across the place; अनाया draws a rangoli |
| 8 हाथ बँटाना | सीता comes from the well with a clay pot; रमेश takes it from her; अनाया and then आरव fetch small pots |
| 9 चिंकी की शरारत | चिंकी snatches दादाजी's white cap and runs across with it; अनाया waves; चिंकी puts it back on his head |
| 10 साफ-सुथरा गाँव | rubbish on the road; each picks up a piece, दादाजी points; सीता sweeps with her broom; रमेश gathers the rest into a sack |

A checker (`tools/jvm/…/Sundar46Check`) looks for each of these in the film: **10 of 10** staged as written, with and
without your pictures. The village sentences, alone, in Hindi and English (`tools/testdata/v46probe`): **31 of 31**.

## 4. Your pictures through the app

Uploaded as on the phone (a Robolectric test of the app's own upload screen), each sheet split into its figures:

| | front | side | back | three-quarter |
|---|---|---|---|---|
| रमेश, सीता, आरव, अनाया | from the turnaround | from the turnaround | from the turnaround | (आरव, अनाया: a pose) |
| दादाजी (after your second batch) | the new turnaround | the new turnaround | the new turnaround | none: the rig turns the front |
| चिंकी (after your second batch) | the red-cap turnaround | the red-cap turnaround | the red-cap turnaround | the waving one, in the same cap |
| गौरी | the turnaround | the turnaround | the turnaround | a pose |

Bugs your pictures found, fixed:

* **गौरी had no front picture.** All six of her figures were read as backs: a cow's big dark eyes and wide face fooled
  the face reader. An animal whose pair of eyes is seen is no longer a back, and a character always gets a front.
* **Her grazing pose (head down) became her main picture.** A figure with its head down is no main picture now.
* **A turnaround sheet is read as front, side and back, left to right,** when its first figure shows a face and its
  third none: रमेश's profile had been read as a back, दादाजी's bald head from behind as a front (it became his picture
  and he played the whole film from behind), चिंकी's red-cap front as a side.
* **A narrow profile was taken for a figure cut off by the frame** and left out of the side's place.
* **दादाजी's three-quarter was him eating a mango** and चिंकी's was in another hat: a turnaround's three-quarter now
  comes from the same sheet, or the rig turns the front.
* The mango sacks sheet is saved as "आम" (shown as an insert when the mango first appears).

## 5. Frame by frame

Every story was rendered with your pictures and looked at (contact sheets of 12 frames each). What the frames showed,
and what was changed:

* The mud was a dark disc in front of the cow: now a wet patch with her legs sunk in it until she is pulled out.
* The pullers stood facing the camera, bunched: now side-on in your side pictures, spaced by their real widths, on a
  visible rope.
* दादाजी, called over, stopped on आरव and half on the cow: he comes only where there is room beside her; with none, he
  turns to her where he stands.
* दादाजी "sat" on nothing: a stool under him.
* The calf was a drawn doll beside your cow: it is your cow's picture, smaller, walking to her, in front.
* The rubbish was cut off at the bottom of the frame: it lies on the road, large enough to see.
* It rained under the shelter: under its straw roof it is dry now.
* चिंकी ran off into the edge of the picture: he runs across, past दादाजी.

## 6. The last checks

* Sentences: v43 405 of 405, v44 118 of 118, v45 229 of 229, v46 31 of 31.
* Your ten scripts: 10 of 10, with your pictures (in the app test) and without.
* 100,000 generated stories (seeds 5,000,001–5,100,000): RESULT_FORGE
* The app on the phone emulator (Robolectric): RESULT_ROBO
* Version 46.0; the APK is `release/KahaniFilm.apk`.

## 7. Still not right

* **Your pose and expression pictures are kept, not shown.** दादाजी's angry, surprised, thinking faces, चिंकी eating a
  mango, sitting on the stool: the film moves your front, side and back pictures with the rig (since v40, at your
  request), so a seated child is a front picture with folded legs, not your cross-legged picture.
* **A place picture seen from high above** (your mango orchard's first panel) is taken as the wide view when it comes
  first: put the one at eye level first. The app cannot yet tell them apart (a test I wrote for it was wrong on your
  own pictures, so I left it out).
* **The thatched shelter, the mud, the flags, the rubbish, the rope, the pots, the cap and the mango** are drawn by the
  app, not taken from your pictures; next to your Pixar-style figures they look simpler.
* **चिंकी in his red cap, wearing दादाजी's white cap,** wears it on top of his own.
* The words the director knows are what it can do: a line it has no rule for is said, not done.

## 8. Said plainly — and sorry

This is not a Pixar or Disney film maker, and I cannot make it one: your pictures are flat images moved on a mesh,
and the director follows rules for the words it knows; it does not understand the story. I cannot promise "no bug at
all" — your pictures alone found five real bugs this time. When you see one, tell me the line and what went wrong.
