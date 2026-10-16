# Testing v33 — twenty stories, short and long, end to end

`twentyStoriesSoak` (run with `gradle -p tools/robotest test -Dkahani.soak=1 --tests '*twentyStoriesSoak'`) makes twenty
stories the way the phone makes them (Robolectric, 240p): the user's sheets added to the library and split
(`SheetSaver.saveToLibrary`), the director placing them by name (`AutoLibrary.fill`), the film job with its animatic,
the shot check and the final QC. Ten short stories (one scene, 14–17 lines) and ten long ones (three scenes, 26–33 lines),
in Hindi, English and Hinglish, cast from the sheets of मीना / Vrinda (sheets 02 and 15), राजू (01), Asha (04), Khan (11)
and Bull (09), with the cave sheets (34, 36, 39) and the sample garden as places. Run of 9 October 2026, after the v33
changes.

**Result: 20 of 20 films finished, no problems — every story scored 100/100 with no critical defect, and the user's own
pictures were drawn in 412 of 463 shots (89 %).** The run took 95 minutes.

| # | Story | Lines | Scenes | Film (s) | Shots | Shots on the user's pictures | Score | Made in (s) |
|---|---|---|---|---|---|---|---|---|
| 1 | मीना की तितली (short, Hindi) | 16 | 1 | 41 | 14 | 11 | 100 | 172 |
| 2 | Asha at the cave (short, English) | 16 | 1 | 41 | 14 | 10 | 100 | 158 |
| 3 | Vrinda aur Bull (short, Hinglish) | 16 | 1 | 40 | 14 | 5 | 100 | 112 |
| 4 | राजू का घंटा (short, Hindi) | 17 | 1 | 48 | 21 | 21 | 100 | 129 |
| 5 | Khan's friend (short, English) | 15 | 1 | 38 | 11 | 11 | 100 | 240 |
| 6 | Raju ki chhalang (short, Hinglish) | 15 | 1 | 36 | 11 | 11 | 100 | 158 |
| 7 | मीना और खान (short, Hindi) | 15 | 1 | 39 | 14 | 10 | 100 | 101 |
| 8 | The lost sword (short, English) | 16 | 1 | 38 | 11 | 11 | 100 | 263 |
| 9 | Bull ka gussa (short, Hinglish) | 16 | 1 | 39 | 15 | 12 | 100 | 99 |
| 10 | बगीचे की सुबह (short, Hindi) | 14 | 1 | 32 | 8 | 8 | 100 | 138 |
| 11 | मीना, राजू और खान की यात्रा (long, Hindi) | 33 | 3 | 99 | 45 | 42 | 100 | 482 |
| 12 | Asha, Raju and the bell (long, English) | 32 | 3 | 95 | 37 | 37 | 100 | 426 |
| 13 | Vrinda ki dosti (long, Hinglish) | 31 | 3 | 85 | 32 | 29 | 100 | 362 |
| 14 | गुफा का रहस्य (long, Hindi) | 28 | 3 | 82 | 33 | 33 | 100 | 407 |
| 15 | The three friends (long, English) | 30 | 3 | 87 | 28 | 28 | 100 | 456 |
| 16 | Khan aur Bull (long, Hinglish) | 29 | 3 | 81 | 33 | 27 | 100 | 319 |
| 17 | राजू की चोरी (long, Hindi) | 27 | 3 | 84 | 35 | 27 | 100 | 481 |
| 18 | The brave girl (long, English) | 27 | 3 | 82 | 30 | 27 | 100 | 368 |
| 19 | Do dost aur ek ghanta (long, Hinglish) | 27 | 3 | 74 | 25 | 25 | 100 | 309 |
| 20 | तीन दोस्त और तितली (long, Hindi) | 26 | 3 | 78 | 32 | 27 | 100 | 515 |

Totals: 463 shots, 412 of them drawn from the user's own pictures.

## What the post-production check added

The three-story test (`threeSimpleStoriesEndToEnd`) ends with an instruction after the film — "make Vrinda bigger, scene 1
brighter, shorter shots, music softer" — and a remake: recognised as a remake, finished, never stopped for a proposal or the
shot check again, the cast file unchanged (the pictures intact), noted in the shot list. The instruction test
(`postProductionInstructionsAreUnderstood`) reads 28 of 28 plain instructions offline.

## Known limits that stay

* The feeling readings of ten-figure sheets are weak (a crying face often reads as neutral); the review dialog after an
  upload corrects them and the library keeps the correction.
* Some front figures of a dark-furred beast still read as "back"; the same remedy. Where no front standing picture of a
  beast is read, its front picture is drawn as it is (never bent).
* A sheet of ten figures gives faces of about 60 px at 1600 px; a phone with the memory reads sheets at up to 4000 px,
  and six to eight figures per sheet give sharper faces still.

## The full report

```
TWENTY STORIES — soak (Robolectric, 240p)

===  1. मीना की तितली (short, Hindi) — 16 lines, 1 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=41 shots=14 real-picture shots=11 score=100 critical=none (no character missing from its scen took=172 s

===  2. Asha at the cave (short, English) — 16 lines, 1 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=41 shots=14 real-picture shots=10 score=100 critical=none (no character missing from its scen took=158 s

===  3. Vrinda aur Bull (short, Hinglish) — 16 lines, 1 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=40 shots=14 real-picture shots=5 score=100 critical=none (no character missing from its scen took=112 s

===  4. राजू का घंटा (short, Hindi) — 17 lines, 1 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=48 shots=21 real-picture shots=21 score=100 critical=none (no character missing from its scen took=129 s

===  5. Khan's friend (short, English) — 15 lines, 1 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=38 shots=11 real-picture shots=11 score=100 critical=none (no character missing from its scen took=240 s

===  6. Raju ki chhalang (short, Hinglish) — 15 lines, 1 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=36 shots=11 real-picture shots=11 score=100 critical=none (no character missing from its scen took=158 s

===  7. मीना और खान (short, Hindi) — 15 lines, 1 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=39 shots=14 real-picture shots=10 score=100 critical=none (no character missing from its scen took=101 s

===  8. The lost sword (short, English) — 16 lines, 1 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=38 shots=11 real-picture shots=11 score=100 critical=none (no character missing from its scen took=263 s

===  9. Bull ka gussa (short, Hinglish) — 16 lines, 1 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=39 shots=15 real-picture shots=12 score=100 critical=none (no character missing from its scen took=99 s

=== 10. बगीचे की सुबह (short, Hindi) — 14 lines, 1 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=32 shots=8 real-picture shots=8 score=100 critical=none (no character missing from its scen took=138 s

=== 11. मीना, राजू और खान की यात्रा (long, Hindi) — 33 lines, 3 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=99 shots=45 real-picture shots=42 score=100 critical=none (no character missing from its scen took=482 s

=== 12. Asha, Raju and the bell (long, English) — 32 lines, 3 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=95 shots=37 real-picture shots=37 score=100 critical=none (no character missing from its scen took=426 s

=== 13. Vrinda ki dosti (long, Hinglish) — 31 lines, 3 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=85 shots=32 real-picture shots=29 score=100 critical=none (no character missing from its scen took=362 s

=== 14. गुफा का रहस्य (long, Hindi) — 28 lines, 3 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=82 shots=33 real-picture shots=33 score=100 critical=none (no character missing from its scen took=407 s

=== 15. The three friends (long, English) — 30 lines, 3 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=87 shots=28 real-picture shots=28 score=100 critical=none (no character missing from its scen took=456 s

=== 16. Khan aur Bull (long, Hinglish) — 29 lines, 3 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=81 shots=33 real-picture shots=27 score=100 critical=none (no character missing from its scen took=319 s

=== 17. राजू की चोरी (long, Hindi) — 27 lines, 3 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=84 shots=35 real-picture shots=27 score=100 critical=none (no character missing from its scen took=481 s

=== 18. The brave girl (long, English) — 27 lines, 3 scenes, cast 3
placed: 3 character pictures, 33 pose pictures
film: done=true error= seconds=82 shots=30 real-picture shots=27 score=100 critical=none (no character missing from its scen took=368 s

=== 19. Do dost aur ek ghanta (long, Hinglish) — 27 lines, 3 scenes, cast 3
placed: 3 character pictures, 30 pose pictures
film: done=true error= seconds=74 shots=25 real-picture shots=25 score=100 critical=none (no character missing from its scen took=309 s

=== 20. तीन दोस्त और तितली (long, Hindi) — 26 lines, 3 scenes, cast 3
placed: 3 character pictures, 31 pose pictures
film: done=true error= seconds=78 shots=32 real-picture shots=27 score=100 critical=none (no character missing from its scen took=515 s

all 20 stories in 95 min; problems: 0
```
