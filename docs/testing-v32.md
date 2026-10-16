# Testing v32 — three simple stories, end to end

The test `threeSimpleStoriesEndToEnd` makes three short stories the way the phone makes them (Robolectric, 360p):
the user's sheets are added to the library and split (`SheetSaver.saveToLibrary`), the director places them by name
(`AutoLibrary.fill`), the film job runs with its animatic, the shot check and the final QC, and the result is measured —
shots, the pictures used per shot, the QC score, missing characters — and frames are rendered for a look
(`tools/robotest/build/frames/story*_shot*.png`). The numbers below are the run of 9 October 2026.

## What the run found, and what was fixed for v32

| Finding | Fix |
|---|---|
| The director placed a child picture of a split sheet ("gufa (angle 10)", "Asha (front, picture 4)") instead of the main one | a figure of a split sheet scores 0.9× its main picture (`AutoLibrary.pictures`): the main is placed, the figures follow it |
| Cave scenes came out very dark: the user's dark cave picture plus the mood grade, the hard-light gradient and the vignette | a picture backdrop carries its own light: tint ×0.6, hard-light gradient ×0.5, vignette 0.30 instead of 0.44 (`Renderer.grade`, `Backdrop.picture`) |
| The horned beast's figures were read as "back" (dark fur = no skin = a back, the person rule) | a monster is read as a beast (`SheetSaver.save`); a library sheet whose figures show little skin is re-read as a beast (`saveToLibrary`): Khan's sitting picture is now cast for "Khan sits down, surprised" |
| A sitting girl (sheet 15, figure 10) was read as running since v28 | the v28 "spread feet" condition removed from the seat rule (`PoseSense`) |

## Still true after the fixes (known limits)

* The feeling readings stay weak on ten-figure sheets: the crying, laughing and angry figures of sheet 15 read as neutral, so a crying line is played by the rigged front with its own sad expression rather than the user's crying picture. The review dialog after an upload ("What each picture shows") is the remedy; the library keeps the corrections.
* Several front figures of the dark-furred beast still read as "back" or "running"; the same remedy.
* An animal that "sits" may be drawn with its lying picture (the bull), which the rubric allows for a beast.
* The rigged front of a furred monster bends badly in a roar (a known weakness of the humanoid rig on fur); real pictures are used where the readings allow.

## The report

```
THREE SIMPLE STORIES — v31 end to end (Robolectric, 360p)

library: मीना      ← sheet15.jpg split into 10 pictures
library: राजू      ← sheet01.jpg split into 10 pictures
library: Asha      ← sheet04.jpg split into 12 pictures
library: Khan      ← sheet11.jpg split into 11 pictures
library: Vrinda    ← sheet02.jpg split into 10 pictures
library: Bull      ← sheet09.jpg split into 10 pictures
library: गुफा      ← sheet39.jpg split into 10 pictures
library: the cave  ← sheet34.jpg split into 10 pictures
library: gufa      ← sheet36.jpg split into 10 pictures

=== Story 1: मीना और राजू की तितली (Hindi)
cast read: मीना (kind 0 girl) राजू (kind 6 monkey) ; scenes: 2
director's placement: महल का बगीचा ← picture "महल का बगीचा", गुफा ← picture "gufa", राजू ← picture "राजू", मीना ← picture "मीना", मीना ← side view "मीना (side view)", मीना ← three-quarter view "मीना (three-quarter, picture 9)", मीना ← back view "मीना (back view)", मीना ← 10 pose pictures (angles, poses, feelings) from the library, राजू ← side view "राजू (side, picture 10)", राजू ← back view "राजू (back view)", राजू ← three-quarter view "राजू (three-quarter view)", राजू ← 10 pose pictures (angles, poses, feelings) from the library
cast.txt: 2 character pictures, 20 pose pictures, 2 place pictures
film job: done=true error= seconds=60.2 stills checked=22 proposals asked=false took=211 s
  3× PICTURES USED: मीना ← the front picture (rigged)
  16× PICTURES USED: मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  2× PICTURES USED: मीना ← the front picture (rigged); राजू ← your picture 2 (side, walking, neutral) stepping through pictures 2, 3, 4, 7
  1× PICTURES USED: मीना ← your picture 7 (back, waving, neutral); राजू ← the front picture (rigged)
shots: 22; shots with a PICTURES USED line: 22, of them drawn from the user's own pictures: 3, walking through step pictures: 2
QC SCORE: 100/100 — review for approval; override: none (no character missing from its scene, feet on the floor)
  frame story1_shot01.png at 7.7 s: Extreme wide — मीना ← the front picture (rigged)
  frame story1_shot04.png at 14.3 s: Medium wide — मीना ← the front picture (rigged); राजू ← your picture 2 (side, walking, neutral) stepping through pictures 2, 3, 4, 7
  frame story1_shot07.png at 16.4 s: Close-up — मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  frame story1_shot10.png at 23.5 s: Close-up — मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  frame story1_shot13.png at 32.0 s: Wide — मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  frame story1_shot16.png at 38.3 s: Close-up — मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  frame story1_shot19.png at 43.2 s: Medium wide — मीना ← the front picture (rigged); राजू ← the front picture (rigged)
  frame story1_shot22.png at 49.4 s: Medium wide — मीना ← the front picture (rigged); राजू ← the front picture (rigged)

=== Story 2: Asha and the Beast (English)
cast read: Asha (kind 1) Khan (kind 5 monster) ; scenes: 1
director's placement: the cave ← picture "the cave", Khan ← picture "Khan", Asha ← picture "Asha", Asha ← side view "Asha (side view)", Asha ← three-quarter view "Asha (three-quarter view)", Asha ← back view "Asha (back view)", Asha ← 12 pose pictures (angles, poses, feelings) from the library, Khan ← three-quarter view "Khan (three-quarter, picture 11)", Khan ← back view "Khan (back, picture 10)", Khan ← side view "Khan (side view)", Khan ← 11 pose pictures (angles, poses, feelings) from the library
cast.txt: 2 character pictures, 23 pose pictures, 1 place pictures
film job: done=true error= seconds=46.3 stills checked=18 proposals asked=false took=135 s
  12× PICTURES USED: Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  5× PICTURES USED: Asha ← the front picture (rigged); Khan ← your picture 11 (front, sitting, surprised)
  1× PICTURES USED: Asha ← the front picture (rigged); Khan ← your picture 7 (back, standing, neutral)
shots: 18; shots with a PICTURES USED line: 18, of them drawn from the user's own pictures: 6, walking through step pictures: 0
QC SCORE: 100/100 — review for approval; override: none (no character missing from its scene, feet on the floor)
  frame story2_shot01.png at 7.7 s: Wide — Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  frame story2_shot04.png at 13.9 s: Wide — Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  frame story2_shot07.png at 17.9 s: Medium close-up — Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  frame story2_shot10.png at 23.2 s: Wide — Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  frame story2_shot13.png at 29.1 s: Wide — Asha ← the front picture (rigged); Khan ← the front picture (rigged)
  frame story2_shot16.png at 33.5 s: Extreme close-up — Asha ← the front picture (rigged); Khan ← your picture 11 (front, sitting, surprised)

=== Story 3: Vrinda aur Bull (Hinglish)
cast read: Vrinda (kind 0 girl) Bull (kind 8 animal) ; scenes: 1
director's placement: gufa ← picture "gufa", Bull ← picture "Bull", Vrinda ← picture "Vrinda", Vrinda ← three-quarter view "Vrinda (three-quarter view)", Vrinda ← back view "Vrinda (back, picture 8)", Vrinda ← side view "Vrinda (side, picture 5)", Vrinda ← 10 pose pictures (angles, poses, feelings) from the library, Bull ← three-quarter view "Bull (three-quarter view)", Bull ← back view "Bull (back, picture 9)", Bull ← side view "Bull (side view)", Bull ← 10 pose pictures (angles, poses, feelings) from the library
cast.txt: 2 character pictures, 20 pose pictures, 1 place pictures
film job: done=true error= seconds=43.4 stills checked=15 proposals asked=false took=111 s
  3× PICTURES USED: Vrinda ← the front picture (rigged)
  9× PICTURES USED: Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)
  1× PICTURES USED: Vrinda ← the front picture (rigged); Bull ← your picture 3 (back, standing, neutral)
  2× PICTURES USED: Vrinda ← the front picture (rigged); Bull ← your picture 7 (side, lying, neutral)
shots: 15; shots with a PICTURES USED line: 15, of them drawn from the user's own pictures: 3, walking through step pictures: 0
QC SCORE: 100/100 — review for approval; override: none (no character missing from its scene, feet on the floor)
  frame story3_shot01.png at 7.7 s: Wide — Vrinda ← the front picture (rigged)
  frame story3_shot03.png at 12.1 s: Wide — Vrinda ← the front picture (rigged)
  frame story3_shot05.png at 14.7 s: Medium wide — Vrinda ← the front picture (rigged); Bull ← your picture 7 (side, lying, neutral)
  frame story3_shot07.png at 16.4 s: Close-up — Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)
  frame story3_shot09.png at 20.1 s: Close-up — Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)
  frame story3_shot11.png at 23.3 s: Medium wide — Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)
  frame story3_shot13.png at 28.2 s: Close-up — Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)
  frame story3_shot15.png at 33.3 s: Medium wide — Vrinda ← the front picture (rigged); Bull ← the front picture (rigged)

stories finished: 3 of 3
```
