# The plain-English image-to-3D guide — as recorded for the 3D maker

This is the app's record of the plain-English guide given with the 3D maker's training ("how an image-to-3D app
works": a few photos and a description in, a 3D model out, bones and an animation, show and export). The
studio keeps every rule of it; next to each rule is where the app does it.

1. **Input: one to five pictures and a description.** The character's picture is the master; a back and a side
   picture may be added (📷 Back, 📷 Side on the character card); the script's description is the text input.
   → `Studio3DArt.makeViews`, `AutoLibrary.views`, the sample's eleven real back views.
2. **Make 3D: remove the background, clean the edge, send the picture to a 3D AI.** The cut-out (`Cutout`) and
   the de-fringed edge (`Figure3D.defringe`) go either to the studio's own figure model, to a free image-to-3D
   demo on Hugging Face Spaces (TripoSR, InstantMesh, Hunyuan3D-2 — no key), or to Meshy with the user's key;
   the model that comes back (GLB) is read by `Glb` and rendered by `Studio3D`.
3. **The reference rules the shape (about 75 %), the description rules style, pose, material and props (25 %).**
   → `SceneMaker.REFERENCE_STRENGTH`, `ImageTo3D.prompt` (the description goes with every picture),
   `Figure3D.details` (what the picture cannot show — a braid down the back, a tail, wings — is drawn from the
   description in the picture's own colours), `FreeModels.propsFor` (a free model keeps the sword, shield or
   staff the description names).
4. **Prompt enhancement: a T-pose or A-pose, both hands visible, nothing in front of the body, a plain
   background, and a list of defects to avoid.** → `Bible.characterPrompt` for the free AI picture service;
   `ImageTo3D.prompt` for the model services.
5. **Ask for a back photo too; a front view does not show the back.** → the character card's 📷 Back; a given
   back view is never replaced by a made one.
6. **Bones and an animation: rig the model, play an idle or a walk.** → every view gets its own rig (`Rig`);
   a free model's own skeleton is posed from its "Idle" animation (`Glb.Options.pose`); the film animates the
   views (walk, breathing, gestures, lip-sync).
7. **Show and export.** → the views on the character card (📐 Views ✓), the film as MP4; the app makes films,
   not model files.
8. **Queue status while the 3D takes minutes.** → the film job's progress line shows each service's step; a
   demo that sleeps or runs out of free quota is skipped for the rest of the film and the figure model steps in.
9. **Save every generation with its prompt and reference.** → the library item's source names the service or
   the model; the manifest keeps the file and the score; the production file carries the credits.
10. **Payment, credits, a web stack, watermarks** — not applicable: the app is free of keys by default; a paid
    service only ever runs with the user's own key.
