**PHONE-LOCAL AI 3D ANIMATED FILM CREATOR**

*A comprehensive, step-by-step implementation guide using existing
photos and pictures*

Product blueprint • AI director workflow • 3D character pipeline •
Offline-first mobile architecture • Testing and delivery

Version 1.0 \| 9 October 2026

# How to use this guide

This document is written for a builder or AI coding agent that must
create a mobile application which turns a user's existing pictures into
a coherent 3D animated film. It gives product requirements, a build
sequence, processing rules, quality checks, and practical constraints.

Important feasibility note: "runs locally on the phone" means the app's
project files, photos, editing, scene assembly, rendering and saved
output remain on the device. A full high-end text-to-3D/video diffusion
pipeline is generally too large or slow for an ordinary phone. The
dependable design is a hybrid of on-device processing and
prebuilt/reusable 3D assets, with optional model downloads performed
once. The core editor and playback must work offline after setup. Do not
promise studio-quality automatic 3D reconstruction from one photo; treat
it as an approximation with user review.

# Contents

-   1\. Define the product and success criteria

-   2\. Decide what "local" means

-   3\. Mobile platform strategy

-   4\. End-to-end user workflow

-   5\. Photo intake and reference library

-   6\. Character identity and facial-feature preservation

-   7\. Convert images into usable 3D assets

-   8\. Story analysis and scene expansion

-   9\. Animation, camera and cinematic direction

-   10\. Hindi dialogue, voice and lip-sync

-   11\. Rendering and export

-   12\. Recommended technical architecture

-   13\. Step-by-step build plan

-   14\. Quality assurance and failure handling

-   15\. Privacy, storage and performance

-   16\. Acceptance checklist

-   Appendix A. Prompts for the AI director

-   Appendix B. Example project structure

-   Appendix C. Practical scope boundaries

# 1. Define the product and success criteria

Product goal: a user selects existing pictures (people, characters,
objects, buildings, landscapes, costumes or locations), supplies a story
or script, and receives an editable 3D animated short film. The app must
preserve recognizable identities and locations across shots, expand a
short script into a sensible shot list, provide voice and lip-sync
options, and render a video file.

## 1.1 Core capabilities

-   Import from the device photo library, Files app, camera, or a
    project folder; support JPEG, PNG and WebP first.

-   Create a reference library with named people, places, props,
    costumes and visual styles.

-   Let the user confirm which reference corresponds to each character
    or place; never silently guess when two matches are plausible.

-   Generate a story breakdown: scenes, shots, action, emotion,
    dialogue, duration, location, props and continuity requirements.

-   Create a repeatable 3D look from the references using a reusable
    rigged character, photo-textured 3D asset, 2.5D card, or generated
    proxy as appropriate.

-   Support timeline editing, shot regeneration, camera presets,
    subtitles, audio, preview and final export.

-   Keep projects and media on-device by default. Make any cloud
    operation explicit and optional.

-   Save intermediate work so a crash or app closure does not destroy
    the project.

## 1.2 Quality priorities

  -----------------------------------------------------------------------
  **Priority**            **What "good" means**   **How to test it**
  ----------------------- ----------------------- -----------------------
  Identity                Same character remains  Compare front, profile,
                          recognisable in every   close-up and wide shots
                          scene.                  against approved
                                                  reference.

  Continuity              Clothing, hair,         Automated continuity
                          accessories, age, scale report plus human
                          and location layout do  review.
                          not drift.              

  Story                   Each shot has a clear   Storyboard review and
                          purpose; added shots    scene-level checklist.
                          improve pacing rather   
                          than pad runtime.       

  Motion                  No sliding feet, broken Play every shot at full
                          joints, jitter,         speed and inspect key
                          teleporting props or    frames.
                          impossible turns.       

  Audio                   Dialogue timing and     Listen with headphones;
                          mouth movement are      inspect phoneme/viseme
                          understandable and      timing.
                          aligned.                

  Offline reliability     Core project editing    Airplane-mode test
                          and rendering do not    after assets/models are
                          require network access. installed.
  -----------------------------------------------------------------------

# 2. Decide what "local" means

Use three clearly labelled operating modes so the app does not mislead
users.

  ----------------------------------------------------------------------------
  **Mode**                **Available functions**      **Constraints**
  ----------------------- ---------------------------- -----------------------
  Offline core            Import references, edit      Uses installed assets
                          story, build shot lists,     and models only.
                          animate existing rigs,       
                          assemble scenes, render      
                          supported projects,          
                          save/export.                 

  Offline AI assist       Local image                  Model size, memory,
                          classification/embeddings,   thermal throttling and
                          face/landmark analysis where platform APIs vary.
                          supported, lightweight text  
                          assistance, segmentation or  
                          depth estimation if device   
                          permits.                     

  Optional online         Download models/assets or    Must be opt-in,
  enhancement             use a remote                 disclosed, and never
                          image-to-3D/video service if required for basic
                          user enables it.             project access.
  ----------------------------------------------------------------------------

Do not market a feature as offline until it has passed an airplane-mode
test on the target device. A model that runs on a developer computer or
server is not a local phone feature.

## 2.1 A realistic production strategy

-   Best reliability: use a library of rigged 3D character templates,
    body types, hair, costumes, facial-expression shapes, props, and
    environment kits; adapt them to the user's approved references.

-   For a single existing portrait, create a stylised likeness or a 2.5D
    animated portrait as a fallback. One image cannot reveal the true
    back of the head, body proportions or unseen clothing.

-   For several photos from different angles, offer a guided "multi-view
    capture" process and a quality score. Reconstruct only when
    sufficient views exist.

-   Keep a 2.5D mode available: layered cut-outs, depth maps, parallax,
    camera movement and animated facial layers can look convincing on
    mobile while using far less compute.

-   Use full 3D when the asset has a suitable mesh, texture and rig. Do
    not force every photo into a 3D mesh if the result is distorted.

# 3. Mobile platform strategy

  -----------------------------------------------------------------------
  **Target**              **Recommended           **Notes**
                          approach**              
  ----------------------- ----------------------- -----------------------
  Android phone           Kotlin + Jetpack        Broader file access and
                          Compose for native UI,  device variation
                          or Flutter for shared   require testing. Check
                          UI; native media/photo  GPU delegate support
                          picker; ONNX Runtime    and memory limits per
                          Mobile or LiteRT/TFLite model.
                          for supported models;   
                          Filament or a suitable  
                          3D engine for viewport; 
                          FFmpeg-based or native  
                          media pipeline where    
                          licensing/platform      
                          constraints allow.      

  iPhone                  SwiftUI or Flutter;     iOS background
                          PhotosPicker/document   execution and file
                          picker; Core ML for     permissions are
                          converted supported     restrictive. Heavy
                          models;                 generation may pause
                          RealityKit/SceneKit or  when app is
                          a cross-platform engine backgrounded.
                          for 3D preview;         
                          AVFoundation for        
                          video/audio assembly.   

  One codebase            Flutter UI plus native  Choose one primary
                          platform channels for   3D/rendering engine
                          photo access, ML        early. Avoid mixing
                          inference and           several engines without
                          rendering;              a clear reason.
                          alternatively a game    
                          engine for the whole    
                          interactive scene.      
  -----------------------------------------------------------------------

If "local on phone" is mandatory, begin with Android or iPhone as a
target, not both at once. Prove performance on one representative
mid-range device before expanding. On-device model compatibility must be
verified for the exact model format, operator set, quantisation,
accelerator and OS version.

# 4. End-to-end user workflow

1.  Create Project → choose aspect ratio (16:9, 9:16 or 1:1), resolution
    target, language, frame rate and offline mode.

2.  Import Pictures → select existing photos; show permission
    explanation; copy or reference files safely within app sandbox.

3.  Build Reference Library → user names each character/place/object;
    tag age range, costume, hairstyle, colours, distinctive marks and
    confidence.

4.  Confirm Identity → present candidate matches and ask user to confirm
    ambiguous assignments.

5.  Add Story → paste script, type a synopsis, or import a text
    document. Preserve original text unchanged as a source version.

6.  Analyse Story → extract characters, locations, actions, dialogue,
    emotional beats, time of day and props.

7.  Expand into Scenes → add only motivated transitions, reaction shots,
    establishing shots, inserts and action coverage.

8.  Create Assets → select a rigged template, apply textures/likeness,
    set scale and approve front/profile/three-quarter previews.

9.  Build Storyboard → each shot card displays scene, framing, movement,
    action, dialogue, duration and reference links.

10. Animate → apply reusable motion clips, facial expression curves,
    camera moves and object actions; allow manual keyframes.

11. Voice and Lip-sync → choose recorded voice, device TTS where
    available, or imported audio; align speech and visemes.

12. Preview → render low-resolution proxy; flag continuity, clipping,
    collisions, unreadable dialogue and audio timing.

13. Final Render → render scene sequence, composite audio/subtitles,
    encode and save to device.

14. Export and Archive → save MP4 plus project file and asset manifest;
    offer a compact backup package.

# 5. Photo intake and reference library

## 5.1 Import procedure

15. Ask for the minimum required permission and use the operating
    system's photo picker wherever possible.

16. Keep the original image immutable. Create derived copies for crops,
    masks, thumbnails and textures.

17. Read image orientation metadata and colour profile; normalise
    orientation before feature extraction.

18. Generate a thumbnail and a small preview first. Defer
    high-resolution processing until requested.

19. Record source path or app-managed copy, checksum, dimensions,
    format, import date and user-defined labels.

20. If the image is too dark, blurred, occluded or low-resolution,
    explain the problem and ask for a better image rather than inventing
    details.

## 5.2 Recommended reference capture

-   For a human character: front, left and right three-quarter views,
    both profiles if possible, full-body front and side, plus
    costume/accessory close-ups.

-   Use even light, neutral expression for identity images, and a
    separate set for expression references.

-   For a location: wide view, reverse angle, key architecture,
    entrances/exits, landmarks, materials and lighting at the story's
    relevant time.

-   For a prop: front, side, rear, scale reference and close-ups of
    distinctive details.

-   Do not assume an image depicts the same person as another image
    solely because the clothing is similar. Ask the user to approve
    identity links.

## 5.3 Reference record schema

  -----------------------------------------------------------------------
  **Field**                           **Example / purpose**
  ----------------------------------- -----------------------------------
  reference_id                        Stable ID such as CHAR_001; never
                                      use a display name as the sole key.

  type                                character, location, prop, costume,
                                      vehicle, animal, texture.

  display_name                        Name shown in UI.

  source_images                       List of local image asset IDs and
                                      view labels.

  approved_traits                     User-approved eye shape, hair, face
                                      outline, skin tone, costume
                                      colours, marks and accessories.

  unknown_traits                      Unseen or ambiguous details that
                                      must not be presented as verified.

  3D_asset_id                         Mesh/rig/texture or 2.5D asset
                                      generated from this reference.

  confidence                          Separate confidence for identity,
                                      geometry, texture and view
                                      coverage.

  revision                            Version of the approved reference;
                                      shots record the version they used.
  -----------------------------------------------------------------------

# 6. Character identity and facial-feature preservation

Identity preservation must be an explicit pipeline, not a vague prompt.
Use approved reference images and a canonical character record that
every shot reads from.

## 6.1 Step-by-step character lock

21. Select one primary identity image and attach additional views. Mark
    which images show the same person/character.

22. Detect face landmarks only when technically supported; use them as
    guides, not as proof of identity.

23. Record visible, user-approved traits: face shape, brow line, eye
    spacing and shape, nose profile, mouth shape, hairline, hairstyle,
    facial hair, ears, complexion, freckles/scars and age presentation.

24. Separate fixed traits from variable traits. Fixed: identity, age
    presentation, hair, costume unless story says otherwise. Variable:
    expression, gaze, head angle, lighting and temporary dirt/wetness.

25. Generate a character turntable: front, three-quarter, side and rear.
    Show uncertainty for hidden features and ask the user to approve the
    design.

26. Create and save a master asset. Every shot references the same asset
    ID and approved version.

27. Use the same texture/material palette and facial rig. Do not
    independently regenerate a new face for each shot.

28. For close-ups, compare against the approved face sheet. If the face
    drifts, reject the shot and regenerate using the same master asset,
    not a new identity description.

29. Keep a continuity log for any deliberate changes such as disguise,
    ageing, injury or costume change.

## 6.2 Face quality checklist

-   Eye spacing and eye size remain stable across camera angles.

-   Nose bridge, nose tip, lip proportions, jaw and chin do not change
    randomly.

-   Hairline, parting, curl pattern, facial hair and accessories remain
    consistent.

-   Expression changes do not distort identity.

-   Skin/material response is consistent under different lighting.

-   No melted features, asymmetrical pupils, duplicated teeth or facial
    texture seams.

Privacy note: facial recognition and face embeddings can be sensitive.
Prefer local processing, avoid uploading face data by default, offer
deletion of derived face data, and do not identify unknown real people
by name. The app should organise user-provided references, not claim to
discover a person's identity.

# 7. Convert images into usable 3D assets

## 7.1 Route selection logic

  -----------------------------------------------------------------------
  **Available input**     **Preferred route**     **Fallback**
  ----------------------- ----------------------- -----------------------
  One portrait            Stylised likeness on a  Animated cut-out with
                          rigged template; facial depth/parallax.
                          texture/projection;     
                          2.5D portrait.          

  Several face angles     Multi-view fitting or a Template fitting plus
                          compatible local        user correction.
                          reconstruction model;   
                          then retopology/rig     
                          validation.             

  Full-body front and     Fit a body template,    2.5D character or
  side                    adjust proportions,     limited-angle rig.
                          costume and textures.   

  Existing 3D file        Import supported        Convert externally
                          glTF/GLB or other       before importing.
                          explicitly supported    
                          format; validate scale, 
                          materials and rig.      

  Location photos         Use environment kit,    2.5D layered
                          photo-textured planes,  environment and camera
                          depth-assisted          parallax.
                          projection or           
                          multi-view              
                          reconstruction.         

  Small object/prop       Template mesh plus      Billboard for distant
                          photo texture, or       background use.
                          multi-view              
                          reconstruction.         
  -----------------------------------------------------------------------

## 7.2 Asset creation steps

30. Classify the reference type and evaluate image coverage, sharpness,
    occlusion and view angles.

31. Choose a route and show its expected fidelity, time and device cost
    before processing.

32. Segment subject from background when supported; let the user correct
    the mask with brush/erase tools.

33. Estimate depth or geometry only when a compatible model is available
    locally. Treat inferred rear surfaces as guesses.

34. Fit the image to a template mesh or build a simple proxy. Preserve
    silhouette before fine details.

35. Create UVs and textures; use consistent texture resolution and avoid
    stretching facial features.

36. Choose or create a skeleton. Validate joint orientation, bone
    hierarchy, bind pose and scale.

37. Bind skin weights; test shoulders, elbows, wrists, hips, knees, neck
    and jaw through extreme poses.

38. Create facial controls or visemes if dialogue is needed.

39. Generate preview renders at low quality; let the user approve or
    correct proportions, texture and costume.

40. Store the asset, settings and provenance locally. Never overwrite
    the source image.

## 7.3 Asset validation gates

-   Geometry gate: no holes, inverted normals, severe
    self-intersections, missing parts or implausible scale.

-   Texture gate: face texture is not mirrored incorrectly; seams are
    not prominent; important marks remain visible.

-   Rig gate: character can stand, walk, sit, turn and raise arms
    without severe deformation.

-   Performance gate: polygon count, texture memory and material count
    fit the target device budget.

-   Identity gate: the user approves the result before it becomes the
    canonical character.

## 7.4 Reusable asset policy

Cache approved characters, locations and props. A scene should reference
asset IDs instead of duplicating meshes and textures. Use
level-of-detail variants, compressed textures, instancing and pooled
objects. Delete cache files only when safe; preserve source references
and project manifests.

# 8. Story analysis and scene expansion

## 8.1 Script parsing

Parse the script into structured data without rewriting its meaning.
Extract: scene headings, characters present, location, time, action,
dialogue, emotional intention, props, continuity dependencies and
required visual information. Keep a link from every generated shot back
to the source passage.

## 8.2 How to add scenes responsibly

-   Add an establishing shot when viewers need to understand a new place
    or geography.

-   Add reaction shots when emotion or a decision changes the scene.

-   Add inserts for important objects, clues, hands, doors, letters or
    visual evidence.

-   Add transitions when time, location or emotional state changes.

-   Break complex action into readable cause-and-effect shots: setup,
    action, consequence and reaction.

-   Use wide, medium and close framing to control geography and emotion;
    avoid random camera changes.

-   Do not add new plot facts, characters, motives or dialogue unless
    the user authorises expansion.

-   Estimate shot duration from action and spoken words. Leave breathing
    room after important reveals.

## 8.3 Shot card required fields

  -----------------------------------------------------------------------
  **Field**                           **Description**
  ----------------------------------- -----------------------------------
  shot_id / scene_id                  Stable identifiers for editing and
                                      regeneration.

  story_source                        Source paragraph, line or script
                                      segment.

  purpose                             What the audience must learn or
                                      feel.

  cast / location / props             Asset IDs, not free-text names
                                      alone.

  framing                             Establishing, wide, medium,
                                      close-up, insert,
                                      over-the-shoulder, etc.

  blocking                            Where each character starts, moves
                                      and ends.

  camera                              Position, lens/FOV, movement, focus
                                      target and duration.

  action / emotion                    Observable action plus intended
                                      emotional beat.

  dialogue / audio                    Exact text, speaker ID, language,
                                      voice asset and timing.

  continuity constraints              Costume, screen direction, prop
                                      hand, lighting, prior/next pose.

  validation status                   Draft, previewed, approved, needs
                                      repair.
  -----------------------------------------------------------------------

# 9. Animation, camera and cinematic direction

## 9.1 Build shots from reusable motion

41. Choose a motion clip that matches the action (walk, turn, sit,
    reach, point, run, stumble, embrace).

42. Retarget only if skeleton mappings are valid; inspect foot contacts
    and joint orientation.

43. Blend clips with controlled transitions; avoid abrupt changes in
    root position or facing direction.

44. Add secondary motion sparingly: hair, cloth, ears, tails, props and
    body follow-through.

45. Use facial expression curves and gaze targets to support the
    emotional beat.

46. Use IK for hands contacting objects and feet contacting the ground
    where supported.

47. Create anticipation, action, follow-through and settling. Not every
    movement should start and stop instantly.

48. Run collision and continuity checks after changes to camera,
    blocking or character scale.

## 9.2 Camera rules

-   Establish geography before fast cutting. Maintain screen direction
    unless the camera deliberately crosses the axis with a clear
    transition.

-   Use slow dolly, pan or orbit moves for emotional beats; avoid moving
    every camera at once.

-   Keep faces visible during key dialogue and do not let props obscure
    eyes or mouths.

-   Use depth of field selectively; never blur critical story
    information.

-   Use a consistent virtual lens language. Avoid extreme wide-angle
    distortion on close-up faces.

-   Check safe areas for vertical video, captions and platform UI.

## 9.3 Lighting and art direction

-   Create a visual bible: palette, material roughness, light direction,
    contrast, atmosphere and stylisation level.

-   Reuse lighting rigs per location/time of day. Character skin and
    costume should not change colour randomly.

-   Use baked lighting or simpler lights for mobile preview; reserve
    higher-quality settings for final render if supported.

-   Offer quality presets: Draft, Balanced and High, each with a clear
    performance warning.

## 9.4 Mobile performance budget

Set measurable budgets per target device rather than universal hard
limits. Track frame time, peak RAM, GPU memory, thermal state, polygon
count, texture memory, draw calls, audio memory and render time. Reduce
resolution, shadows, particles, texture size and level of detail before
compromising identity or story readability.

# 10. Hindi dialogue, voice and lip-sync

## 10.1 Audio pipeline

49. Preserve the script's exact Hindi dialogue and punctuation as a
    source of truth.

50. Assign each speaker a stable voice profile and pronunciation
    dictionary for names, places and unusual words.

51. Prefer user-recorded dialogue or an on-device TTS voice when
    available. Check licensing and offline availability for every voice.

52. Normalise audio sample rate and loudness for the project; keep
    dialogue, music and effects on separate tracks.

53. Create word or phoneme timing from speech alignment when a supported
    local model exists; otherwise allow manual timing.

54. Map phonemes to a compact set of visemes (closed lips, open, wide,
    rounded, teeth/lip contact, etc.).

55. Blend visemes smoothly and include natural pauses. Do not animate
    the mouth continuously during silence.

56. Preview dialogue at normal speed and inspect plosives, long vowels,
    pauses and emotional emphasis.

57. Mix music and effects below dialogue; include mute controls and
    subtitle timing.

## 10.2 Lip-sync acceptance criteria

-   Mouth opens and closes in time with syllables; it does not flap at a
    constant rate.

-   Jaw movement is plausible and does not stretch the face mesh.

-   Visemes blend without popping; teeth and tongue do not visibly
    penetrate lips.

-   Speaker identity and voice remain stable between scenes.

-   Subtitles match the approved script and do not expose internal
    timing tags.

# 11. Rendering and export

## 11.1 Render stages

58. Storyboard animatic: stills or proxy models with temporary dialogue
    and timing.

59. Low-resolution scene preview: basic materials, simple shadows,
    reduced effects.

60. Shot approval render: final camera, character animation, facial
    expression and sound timing.

61. Final sequence render: consistent resolution, frame rate, colour
    space and audio format.

62. Composition: assemble shots in order, add transitions, subtitles,
    music and sound effects.

63. Encode and verify: produce MP4 (H.264 is a practical compatibility
    baseline) with AAC audio where supported.

64. Playback verification: open the exported file in the phone's player;
    confirm duration, audio, aspect ratio and no black/frozen frames.

65. Project packaging: save project manifest, asset references, fonts,
    subtitle file and optional proxies.

## 11.2 Export options

  -----------------------------------------------------------------------
  **Preset**              **Use**                 **Trade-off**
  ----------------------- ----------------------- -----------------------
  Draft preview           Fast review and timing. Lower resolution,
                                                  reduced
                                                  shadows/effects.

  Standard mobile         Sharing and social      Balanced quality and
                          platforms.              render time.

  High quality            Best supported device   Longer render, heat and
                          quality.                battery use; may not be
                                                  supported on every
                                                  phone.

  Vertical short          9:16 delivery.          Requires separate
                                                  camera framing; do not
                                                  simply crop a 16:9
                                                  master.

  Project archive         Continue editing later. Includes project
                                                  metadata and may be
                                                  much larger than the
                                                  MP4.
  -----------------------------------------------------------------------

Always warn before long renders that the phone may heat up, consume
battery and need free storage. Save render progress and recover
gracefully after interruption.

# 12. Recommended technical architecture

Use a modular design so the app can replace a model or renderer without
rewriting the entire product.

  -----------------------------------------------------------------------
  **Module**              **Responsibilities**    **Offline requirement**
  ----------------------- ----------------------- -----------------------
  Mobile UI               Project browser,        Must work offline.
                          reference library,      
                          storyboard, timeline,   
                          asset approval,         
                          settings.               

  Project database        SQLite or equivalent    Must work offline.
                          local database;         
                          migrations, versioning, 
                          autosave.               

  Asset store             Original photos,        Must work offline.
                          derived images, meshes, 
                          textures, audio,        
                          thumbnails, cache.      

  Story director          Script parser,          Rule-based mode must
                          scene/shot planner,     work offline; local LLM
                          continuity rules and    is optional.
                          user-editable shot      
                          cards.                  

  Vision services         Image classification,   Only claim local
                          segmentation,           support for installed
                          landmarks, depth        compatible models.
                          estimation, embeddings. 

  3D asset service        Template fitting,       Must work for supported
                          import, texture setup,  local
                          rig validation and LOD. templates/imports.

  Animation service       Timeline, keyframes,    Must work offline.
                          clips, IK, camera and   
                          facial controls.        

  Audio service           Record/import, TTS      Offline only for
                          integration, alignment, supported local
                          visemes, mixing.        voices/models.

  Renderer/exporter       Preview render, final   Must work offline for
                          render, compositing,    supported scenes.
                          encode and              
                          verification.           

  Optional update service Model downloads, asset  Disabled unless user
                          packs and version       opts in.
                          checks.                 
  -----------------------------------------------------------------------

## 12.1 Data design rules

-   Use stable IDs and explicit versions for projects, characters,
    locations, shots and assets.

-   Keep source images separate from derived media and cached previews.

-   Use atomic saves, schema migrations and a recoverable project
    journal.

-   Store relative paths or app-managed asset IDs, not fragile absolute
    paths to temporary locations.

-   Keep a manifest of required assets and model versions so a project
    can detect missing components.

-   Avoid logging private image contents, face descriptors or story text
    to analytics.

## 12.2 Model and engine selection criteria

Before adopting any model or engine, verify: commercial-use licence,
redistribution terms, offline inference support, model size, memory use,
minimum OS, supported hardware acceleration, quantised-model accuracy,
input/output format, maintenance activity and whether it permits the
intended face/voice use. Do not choose a model solely because a demo
looks impressive on a desktop.

## 12.3 Suggested project layout

-   /app --- mobile screens, navigation, permissions and settings

-   /core/project --- project schema, database, migrations and autosave

-   /core/assets --- imports, metadata, thumbnails and asset manifest

-   /ai/story --- script parser, shot planner and continuity rules

-   /ai/vision --- model adapters and image-quality assessment

-   /assets/templates --- licensed characters, rigs, environments and
    props

-   /animation --- timeline, motion clips, camera, IK and facial
    controls

-   /audio --- recording, voice selection, timing, visemes and mixing

-   /render --- preview, final rendering, compositing and export

-   /tests --- fixtures, device tests, regression scenes and golden
    images

-   /docs --- model cards, licences, offline feature matrix and
    troubleshooting

# 13. Step-by-step build plan

## Phase 1 --- Prove the mobile foundation

66. Choose the first platform and minimum supported device class.

67. Build a project home screen, create/open project, and persistent
    local database.

68. Import five images from the photo picker and display thumbnails.

69. Implement app-managed storage, delete/undo, permissions and
    crash-safe saves.

70. Pass airplane-mode tests for project creation, image import and
    reopening.

## Phase 2 --- Reference library and character lock

71. Add character/place/prop records with tags and multiple image views.

72. Build a user approval screen for identity and visual traits.

73. Create character sheets and location sheets from templates or 2.5D
    layers.

74. Version approved assets and show which shots use each version.

75. Add a drift review screen comparing generated frames with the master
    sheet.

## Phase 3 --- Storyboard and scene planning

76. Import plain-text scripts and preserve the original.

77. Parse scene headings, speakers, locations, actions and dialogue.

78. Generate editable shot cards using deterministic rules first.

79. Add scene expansion rules for establishing, reaction, insert and
    transition shots.

80. Let the user approve scene count, duration and any new material
    before asset generation.

## Phase 4 --- 3D/2.5D animation prototype

81. Import one known-good rigged character and one environment.

82. Implement camera, timeline, basic movement clips and expression
    controls.

83. Support image-textured 2.5D scenes as a lower-cost fallback.

84. Add preview render and shot approval workflow.

85. Measure performance and simplify the scene automatically when
    budgets are exceeded.

## Phase 5 --- Voice, lip-sync and export

86. Add audio import/recording and speaker assignment.

87. Implement manual viseme timing first; integrate local alignment only
    after benchmarking.

88. Assemble shots and audio into an MP4 using a platform-compatible
    pipeline.

89. Verify playback, audio synchronisation, storage errors and
    interruption recovery.

## Phase 6 --- AI enhancements

90. Benchmark one vision model at a time on target devices.

91. Add segmentation, depth estimation or local text assistance only if
    the value justifies the cost.

92. Add optional model packs with version, size, licence, supported
    devices and deletion controls.

93. Create clear fallbacks when a model is absent, unsupported or out of
    memory.

94. Run regression tests after every model or renderer update.

## Phase 7 --- Beta and release

95. Test on low, mid and high tier devices; include older supported
    devices.

96. Run a 3--5 minute sample project repeatedly and check thermal
    throttling.

97. Test interruption during import, AI processing, preview and final
    export.

98. Publish a supported-format matrix, offline feature matrix and
    privacy explanation.

99. Release gradually and collect opt-in crash diagnostics that exclude
    user media.

# 14. Quality assurance and failure handling

  -----------------------------------------------------------------------
  **Failure**                         **Required response**
  ----------------------------------- -----------------------------------
  Ambiguous character match           Show candidates and ask the user;
                                      do not silently merge identities.

  Insufficient photo angles           Explain which views are missing;
                                      offer template or 2.5D mode.

  Model unavailable/offline           Use deterministic/basic workflow
                                      and show which feature is
                                      unavailable.

  Out of memory                       Stop safely, save state, reduce
                                      resolution/texture/scene complexity
                                      and retry.

  Overheating                         Pause or offer draft render, reduce
                                      quality and recommend cooling
                                      before resuming.

  Broken rig or deformation           Mark asset as failed, offer another
                                      rig/template and block final render
                                      if severe.

  Continuity mismatch                 Show frame and rule violated; allow
                                      targeted shot repair.

  Missing asset after reopen          Offer relink or replace; never
                                      silently substitute another
                                      character.

  Export interrupted                  Resume if supported or restart from
                                      cached shot renders; preserve
                                      project.

  Storage almost full                 Estimate required space before
                                      render and offer cache cleanup
                                      without deleting source media.
  -----------------------------------------------------------------------

## 14.1 Test matrix

-   Photo tests: portrait, landscape, rotated EXIF, transparent PNG,
    very large image, blurry image and corrupt file.

-   Identity tests: same person in different lighting, profile, costume
    change, expression change and partial occlusion.

-   Continuity tests: character exits left and enters next shot
    correctly; prop remains in the correct hand; costume version changes
    only when scripted.

-   Animation tests: walk-stop-turn, sit-stand, hand-object contact,
    two-character interaction and close-up speech.

-   Offline tests: first launch after setup, airplane mode, app restart,
    render and export.

-   Resource tests: low storage, low memory, thermal slowdown, incoming
    call/backgrounding and battery saver.

-   Accessibility tests: readable text, screen reader labels, touch
    target size, subtitles and colour contrast.

# 15. Privacy, storage and performance

-   Use explicit photo and microphone permission explanations.

-   Keep user photos, scripts, audio and derived face data on-device by
    default.

-   Provide project-level deletion and a separate "delete
    cached/generated data" control.

-   If an online service is enabled, display exactly what is sent, why,
    and whether it is retained.

-   Do not train models on user media without separate, informed opt-in
    consent.

-   Encrypt sensitive project data where appropriate and use OS-provided
    secure storage for secrets.

-   Use model checksums and trusted distribution sources; never execute
    arbitrary code embedded in a project file.

-   Show storage estimates before importing large libraries or rendering
    high-resolution video.

Offline does not automatically mean secure: backups, crash logs, shared
exports and imported project bundles can still expose personal content.
Treat these as separate data paths.

# 16. Acceptance checklist

-   [ ] User can create and reopen a project with no network connection.

-   [ ] User can import and tag existing photos without modifying
    originals.

-   [ ] Every generated character and location has a stable ID and
    approved reference sheet.

-   [ ] Ambiguous identities and unseen physical details are surfaced
    for user review.

-   [ ] The same master character asset is reused across all shots.

-   [ ] The user can inspect and edit the generated scene and shot list.

-   [ ] Scene expansion is explainable and does not silently add plot
    facts.

-   [ ] At least one rigged 3D route and one lightweight 2.5D fallback
    work on the target device.

-   [ ] Preview rendering is responsive and does not lose work when
    interrupted.

-   [ ] Audio, dialogue and lip-sync can be reviewed before final
    export.

-   [ ] The exported video opens and plays on the device.

-   [ ] The project can be archived, restored and checked for missing
    assets.

-   [ ] Licence, model size, device requirements and offline limitations
    are documented.

# Appendix A. Prompts for the AI director

## A1. Story-to-shot planner system prompt

You are a continuity-focused animation director. Preserve the supplied
story and dialogue. Break it into scenes and purposeful shots. For every
shot specify: shot ID, source passage, narrative purpose, cast asset
IDs, location asset ID, props, framing, camera movement, blocking,
action, emotion, dialogue, duration, sound, continuity constraints and
validation checks. Add shots only when they improve geography,
causality, emotion, suspense or clarity. Do not invent plot facts. Flag
missing information as a question. Use the same approved character and
location asset IDs throughout.

## A2. Character reference-lock prompt

Use the user-approved reference images and character record as the
source of truth. Preserve face outline, eye spacing, brows, nose, mouth,
hairline, hairstyle, complexion, distinctive marks, costume and
accessories. Vary only expression, gaze, pose, camera angle and lighting
unless the script explicitly changes a trait. Do not invent unseen
features as facts. If the reference set is insufficient for the
requested angle, mark low confidence and offer a template or 2.5D
fallback. Do not create a new identity for each shot.

## A3. Shot generation prompt

Generate this shot using the approved project assets only. Follow the
shot card's action, blocking, camera, emotional beat, duration, lighting
bible and continuity constraints. Preserve screen direction and prop
ownership. Keep the face unobstructed during important dialogue. Use
plausible body mechanics, contact and foot placement. Do not change
character design, costume, location layout or dialogue. Return a preview
and a list of any constraints that could not be met.

## A4. Continuity reviewer prompt

Compare the rendered shot with the approved character sheet, previous
and next shots, location sheet and continuity log. Check identity,
costume, accessories, prop hand, scale, screen direction, lighting, time
of day, character position, foot contact, facial deformation, camera
geography and dialogue timing. Return each issue with severity,
timestamp/frame, evidence and a targeted repair instruction. Do not
silently rewrite the story.

# Appendix B. Example project structure

A project archive may use a folder layout such as the following. The app
should generate and maintain it; users should not have to create folders
manually.

-   project.json --- project settings and version

-   database.sqlite --- scenes, shots, assets, revisions and timeline
    metadata

-   references/originals/ --- imported source pictures, unchanged

-   references/derived/ --- crops, masks, thumbnails and texture maps

-   characters/CHAR_001/ --- mesh, rig, materials, face sheet and
    approval metadata

-   locations/LOC_001/ --- environment assets, textures and lighting
    preset

-   props/PROP_001/ --- prop asset and texture data

-   animation/ --- motion clips, keyframes and camera tracks

-   audio/ --- voice recordings, generated voice files, music and
    effects

-   renders/proxy/ --- low-resolution previews

-   renders/final/ --- approved shot renders and final MP4

-   models/manifest.json --- installed model IDs, versions, licences and
    checksums

# Appendix C. Practical scope boundaries

The following should be stated honestly in the product UI:

-   A single photograph cannot reliably reveal the hidden sides of a
    person or object. Reconstruction requires inference and user
    approval.

-   A fully automatic feature-film-quality 3D movie generator running
    entirely on every phone is not a realistic baseline. Target a staged
    workflow and validate on actual hardware.

-   The strongest quality improvement usually comes from asset reuse, a
    consistent rig, approved reference sheets, shot planning, controlled
    camera work and human review---not simply from making prompts
    longer.

-   Do not describe the app as "Pixar-level" or promise parity with a
    major animation studio. Define measurable visual targets and build a
    distinctive, consistent art direction.

-   Treat generative AI as an assistant to a controllable animation
    pipeline. The app must retain editing, deterministic assets, review
    gates and reliable export.

# Final implementation recommendation

Build a dependable offline-first mobile editor first: photo library →
approved reference sheets → reusable 3D/2.5D assets → editable
storyboard → timeline animation → audio/lip-sync → preview → MP4 export.
Then add on-device vision and local language models selectively after
device benchmarks. This sequence produces a usable product sooner and
avoids making the entire app dependent on a large model that may not fit
or run well on a phone.
