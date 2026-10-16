**AI 3D ANIMATED IMAGE & VIDEO SCENE MAKER**

Reference-Based Training and Production Guide

*Character identity • 3D construction • Cinematic rendering Scene continuity • Video workflows • Quality assurance*

Practical system specification for an AI director or image-generation pipeline

# Document purpose

This guide specifies how an AI system should create polished, cinematic, stylised 3D cartoon images and consistent sequences of images for video production, using reference pictures and written descriptions. The target is the broad quality of premium animated feature films: appealing character design, expressive faces, clear visual storytelling, carefully controlled lighting, rich environments, and believable 3D form. The aim is not to copy any one studio's distinctive characters or exact designs.

  ------------------------------------------------------------------------------------------------------------------
  **GOVERNING PRINCIPLE\
  **The character must remain the same; the camera, expression, action and environment may change when instructed.
  ------------------------------------------------------------------------------------------------------------------

  ------------------------------------------------------------------------------------------------------------------

The system must manage five connected tasks:

1.  Understand reference pictures and written descriptions.

2.  Build persistent character, style, and environment specifications.

3.  Generate images with consistent 3D design.

4.  Maintain continuity across shots and video frames.

5.  Inspect results, detect errors, and correct them.

# 1. System architecture

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Layer**                **Responsibilities**                                                                                          **Expected output**
  ------------------------ ------------------------------------------------------------------------------------------------------------- -------------------------------------
  Input layer              Accept reference pictures, descriptions, scripts, shot instructions, aspect ratio and quality requirements.   Validated project inputs

  Reference analyser       Extract visual attributes, compare images, detect uncertainty and identify style characteristics.             Structured reference analysis

  Canonical asset memory   Store character IDs, approved references, descriptions, environment state and version history.                Reusable project asset records

  Still-image engine       Control subject, pose, expression, composition, materials, lighting and camera.                               Candidate still images

  Scene-sequence engine    Plan shots, keyframes, motion and continuity across scenes.                                                   Ordered shots or frames

  Quality control          Evaluate identity, anatomy, style, instruction compliance and continuity.                                     Pass/fail decision and repair notes
  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **ENGINEERING RULE\
  **Do not rely on a prompt alone as character memory. Store character specifications and reference assets separately, and retrieve them whenever the character appears. For demanding video work, use a consistent 3D model or suitable identity-conditioning system wherever possible.
  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 2. Reference-image understanding

Before generation, convert each supplied image into structured visual information. Do not stop at a generic caption.

  ---------------------------------------------------------------------------------------------------------------
  **Category**                        **What to extract**
  ----------------------------------- ---------------------------------------------------------------------------
  Identity                            Face silhouette, eye spacing, nose, mouth, ears and distinctive marks

  Proportions                         Head-to-body ratio, torso length, limb length, hand and foot size

  Design language                     Rounded, angular, soft, exaggerated, realistic or toy-like forms

  Colour                              Skin, hair, eyes, clothing, accessories and secondary colour accents

  Surface                             Skin shading, hair strands or clumps, fabric, roughness and gloss

  Expression                          Eye openness, eyebrow shape, mouth curvature and cheek movement

  Clothing                            Silhouette, seams, folds, layering, patterns and accessories

  Environment                         Architecture, vegetation, furniture, props, colours and spatial layout

  Cinematography                      Camera height, framing, perspective, lens impression and depth of field

  Lighting                            Key light, fill light, rim light, shadow direction and colour temperature
  ---------------------------------------------------------------------------------------------------------------

## 2.1 Reference interpretation rules

-   Separate directly visible facts from inferred details.

-   Use multiple views to establish 3D structure; a front view does not reveal the back of a character.

-   When references conflict, prioritise the designated master image and explicit written instructions.

-   Treat lighting, colour balance and perspective differences as possible image-capture differences rather than automatically changing the design.

-   Preserve intentional asymmetry, unusual proportions and distinctive features.

-   Do not invent important identifying details without support from a reference or description.

## 2.2 Confidence labels

  --------------------------------------------------------------------------------------------------------------------------------------------
  **Confidence**          **Definition**                                        **System behaviour**
  ----------------------- ----------------------------------------------------- --------------------------------------------------------------
  High                    Clearly visible in several references.                Treat as a stable attribute.

  Medium                  Visible in one usable reference or partly obscured.   Use cautiously; verify when possible.

  Low                     Hidden, ambiguous or inferred.                        Keep flexible; do not turn a guess into a permanent feature.
  --------------------------------------------------------------------------------------------------------------------------------------------

# 3. Character identity specification

Every recurring character must have a persistent Character Bible and a unique ID, such as CHAR_001.

  ---------------------------------------------------------------------------------------------------------------------------------------------
  **Record field**                    **What to store**
  ----------------------------------- ---------------------------------------------------------------------------------------------------------
  Identity                            Permanent character ID, name or label, canonical reference set and approved version

  Face                                Head shape, eye design, eye spacing, nose, mouth, ears, skin tone and distinctive marks

  Hair                                Hairline, shape, colour, volume, texture and stable styling

  Body                                Height impression, body build, head-to-body ratio, limb proportions and silhouette

  Wardrobe                            Default outfit, materials, colours, patterns, shoes and accessories

  Style                               Shape language, exaggeration level, detail density and rendering conventions

  Variable traits                     Expression, gaze, pose, camera, lighting and authorised outfit changes

  Assets                              Master images, front/side/three-quarter views, expression sheet, full-body view, optional model and rig

  Change history                      Approved corrections, version number and notes about changes authorised by the user
  ---------------------------------------------------------------------------------------------------------------------------------------------

## 3.1 Identity-preservation rules

6.  Assign each character a unique ID that never changes.

7.  Keep original reference images linked to that ID.

8.  Reuse the same canonical model or identity-conditioned references across scenes.

9.  Change only attributes authorised by the current instruction.

10. Compare every new output with the canonical references.

11. Record approved corrections so later generations do not repeat previous errors.

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **IMPORTANT LIMITATION\
  **A text description alone often cannot preserve exact facial identity across many images. For higher consistency, use multiple reference images, image-conditioning features, character adapters, or a shared 3D model.
  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 4. Building a convincing 3D animated character

## Stage A --- Silhouette and proportions

-   Establish the head, torso, pelvis, arms, legs, hands and feet.

-   Match the reference's overall head-to-body ratio.

-   Preserve distinctive silhouette features such as a large head, narrow shoulders or unusual ears.

-   Check recognisability using a simple dark silhouette.

-   Prevent limb lengths and body proportions from changing between shots.

## Stage B --- Facial construction

-   Maintain eye spacing, eye size, eyelid shape, nose position and mouth placement.

-   Preserve relationships between facial features, not just individual shapes.

-   Build expressions through coherent changes in eyebrows, eyelids, cheeks and mouth.

-   Avoid accidental changes in apparent age or identity.

-   Keep facial features correctly attached to the head during rotation.

## Stage C --- Geometry and deformation

For a real 3D production pipeline, use a consistent mesh or compatible character model, suitable topology for joints and facial movement, a stable rig, and facial controls or blend shapes. For an image-only pipeline, reference conditioning can approximate consistency, but generated images do not automatically create a reusable 3D model.

## Stage D --- Materials

  --------------------------------------------------------------------------------------------------------------
  **Material**                        **Target behaviour**
  ----------------------------------- --------------------------------------------------------------------------
  Skin                                Soft shading, restrained subsurface scattering and controlled highlights

  Hair                                Stylised strands or clumps with coherent highlights and volume

  Eyes                                Consistent iris and pupil design with controlled reflections

  Clothing                            Fabric folds, seams and appropriate surface roughness

  Hard surfaces                       Believable material response for metal, wood, glass, ceramic or plastic
  --------------------------------------------------------------------------------------------------------------

The objective is not maximum detail. It is detail that supports the selected animation style.

# 5. Visual style specification

Aim for cinematic, appealing, stylised 3D animation---not a photograph with a cartoon filter. Use broad qualities associated with high-quality animated feature films while developing original characters and designs.

  ------------------------------------------------------------------------------------------------------------------------
  **Style dimension**                 **Rules to define**
  ----------------------------------- ------------------------------------------------------------------------------------
  Character design                    Proportions, expression readability, exaggeration and silhouette clarity

  Environment                         Foreground/middle-ground/background separation, colour grouping and detail density

  Colour                              Project palette, saturation range, contrast and character-background separation

  Materials                           Surface response for skin, hair, clothing and props

  Lighting                            Key/fill/rim conventions, shadow softness, highlights and atmosphere

  Camera                              Typical shot sizes, perspective, depth of field and framing preferences

  Rendering                           Shading quality, output resolution, anti-aliasing and acceptable detail level
  ------------------------------------------------------------------------------------------------------------------------

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **STYLE LOCK\
  **Store the project style separately from each character's identity record. This allows scene lighting or atmosphere to change without accidentally redesigning the character.
  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 6. Prompt interpretation and scene construction

Convert natural-language instructions into a structured scene brief before generation.

  -----------------------------------------------------------------------------------------------------------------------
  **Scene field**                     **Example: "A little boy runs toward his dog in a garden at sunset"**
  ----------------------------------- -----------------------------------------------------------------------------------
  Subjects                            Boy and dog, linked to their existing identity records

  Action                              Boy running toward the dog, with a clear direction and plausible posture

  Environment                         Garden, ground plane, vegetation, background objects and spatial relationships

  Lighting and mood                   Warm low-angle sunlight, long soft shadows and joyful atmosphere

  Camera                              Wide enough to show both subjects, their movement and the garden

  Constraints                         Preserve character identity; include both subjects; no unrequested outfit changes
  -----------------------------------------------------------------------------------------------------------------------

## Prompt interpretation rules

-   Distinguish mandatory instructions from optional artistic enhancements.

-   Do not let decorative choices override a specified outfit, action, object or camera angle.

-   Identify which assets must be retrieved before generation.

-   Keep the prompt, character record, scene record and rendering controls separate where the system supports structured inputs.

-   If an essential instruction is contradictory, ask for clarification or apply a documented priority rule rather than silently guessing.

# 7. Camera, composition and cinematic language

  -------------------------------------------------------------------------------------
  **Shot type**                       **Primary purpose**
  ----------------------------------- -------------------------------------------------
  Extreme wide shot                   Establish geography and atmosphere

  Wide shot                           Show full-body action and spatial relationships

  Medium shot                         Communicate gestures and interaction

  Close-up                            Emphasise emotion or dialogue

  Extreme close-up                    Highlight a significant facial or object detail

  Over-the-shoulder                   Show a character's perspective in conversation

  Low angle                           Create visual importance or a sense of scale

  High angle                          Show spatial relationships or vulnerability

  Tracking-style composition          Anticipate movement through the frame
  -------------------------------------------------------------------------------------

## Composition rules

-   Establish the main subject before adding secondary details.

-   Keep faces and important actions clearly visible.

-   Use foreground and background elements to establish depth.

-   Maintain believable camera perspective and subject scale.

-   Leave room in the frame for movement when an image belongs to a video sequence.

-   Use depth of field to support the story, not simply to blur everything behind the subject.

-   Preserve screen direction across consecutive shots depicting continuous action.

# 8. Lighting and rendering pipeline

  --------------------------------------------------------------------------------------------------------
  **Lighting component**              **Function**
  ----------------------------------- --------------------------------------------------------------------
  Key light                           Primary light source; establishes form, direction and mood

  Fill light                          Controls shadow darkness while preserving readable facial features

  Rim/backlight                       Separates the character from the background and defines silhouette

  Ambient shading                     Helps objects feel integrated into the environment

  Contact shadows                     Grounds characters and props so they do not appear to float
  --------------------------------------------------------------------------------------------------------

## Rendering quality rules

-   Preserve readable detail in highlights and shadows.

-   Keep light direction consistent across characters and objects in the same shot.

-   Avoid excessive bloom, oversaturation and artificial sharpening.

-   Ensure reflective or transparent objects behave plausibly for the chosen style.

-   Prevent textures and materials from changing arbitrarily between images.

-   Match the reference's level of polish rather than automatically maximising surface complexity.

# 9. Consistency across video scenes

A collection of individually attractive images is not sufficient. The images must look as if they belong to the same film.

## 9.1 Continuity record for each scene and shot

-   Scene ID and shot ID.

-   Characters present and their identity IDs.

-   Character positions, orientation and pose.

-   Clothing, accessories and carried objects.

-   Environment layout and object positions.

-   Lighting direction, time of day and weather.

-   Camera position, lens impression and framing.

-   Action start state and intended end state.

-   Important details inherited from the preceding shot.

## 9.2 Three kinds of continuity

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Continuity type**     **Requirement**                                                                         **Example failure**
  ----------------------- --------------------------------------------------------------------------------------- --------------------------------------------------------------------------
  Character               Same face, proportions, hair, clothing and accessories unless changes are authorised.   A character's eye shape changes between shots.

  Spatial                 Characters and objects retain coherent positions and relationships.                     A dog jumps from left to right without a movement or camera explanation.

  Temporal                Actions develop logically from one state to the next.                                   A seated character suddenly stands in the next frame.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

## 9.3 Recommended video workflow

12. Break the script into scenes, then divide each scene into shots.

13. Define the starting state and intended ending state of each shot.

14. Generate a master reference or keyframe for every important shot.

15. Approve identity, composition, pose and environment before generating variations.

16. Use a video model, image-to-video system or 3D animation pipeline to produce motion.

17. Review motion, identity, anatomy, object continuity and transitions.

18. Repair the affected shot or frames rather than unnecessarily changing the whole sequence.

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **TECHNICAL DISTINCTION\
  **Still-image generation does not automatically create temporally consistent video. Video requires an additional motion-generation stage or a 3D rigged animation pipeline. A persistent 3D model provides more control over identity and camera movement than independently generated images.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 10. Master system prompt for the AI director

Use the following as the core behavioural specification for an existing AI model or generation pipeline. It guides behaviour; it does not itself retrain model weights.

## MISSION

-   Create polished, cinematic, stylised 3D animated images and consistent visual sequences from reference pictures, descriptions and scripts.

-   Maintain character identity, scene continuity, visual quality and adherence to the user's instructions.

## A. INPUT ANALYSIS

-   Inspect every supplied reference image.

-   Extract visible character features, proportions, colours, materials, clothing, accessories, environment and composition.

-   Interpret the written description and identify mandatory visual requirements.

-   Separate observed facts from uncertain inferences.

-   Resolve conflicts using the designated master reference and explicit user instructions.

-   Never invent important identifying features without justification.

## B. CANONICAL CHARACTER MEMORY

-   Assign a permanent unique ID to each character.

-   Store the canonical description and approved reference images.

-   Record facial structure, eye design, hairstyle, body proportions, distinctive marks and clothing.

-   Separate permanent identity features from variable pose, expression, camera, lighting and scene attributes.

-   Retrieve the correct character record for every generation.

-   Compare new outputs against canonical references and preserve approved corrections.

## C. 3D CHARACTER CONSTRUCTION

-   Establish coherent volume, silhouette and proportions.

-   Preserve the reference's head-to-body ratio and distinctive shape language.

-   Keep facial features correctly positioned as the head rotates.

-   Ensure plausible joints, limbs, hands, feet and body deformation.

-   Use consistent geometry, rigs and facial controls when a reusable 3D model is available.

-   Avoid malformed anatomy and unexplained design changes.

-   Preserve intentional stylisation rather than forcing realistic anatomy onto cartoon characters.

## D. ART DIRECTION

-   Use appealing, expressive, cinematic 3D animation aesthetics.

-   Maintain the project's established palette, shape language, material style and detail level.

-   Use controlled exaggeration and clear emotional readability.

-   Keep character design distinct from environment design.

-   Do not change the visual style without authorisation.

-   Develop original designs using broad stylistic qualities rather than copying distinctive protected characters or exact studio designs.

## E. MATERIALS AND RENDERING

-   Assign suitable material behaviour to skin, hair, eyes, cloth and environmental objects.

-   Use coherent lighting, shadows, reflections and ambient shading.

-   Preserve surface detail appropriate to the selected style.

-   Avoid excessive gloss, oversharpening, artificial smoothing and visual clutter.

-   Keep the subject readable against the background.

-   Ensure coherent perspective, scale and illumination.

## F. SCENE AND CAMERA CONTROL

-   Translate each request into a structured scene specification.

-   Identify subjects, actions, environment, mood, camera and lighting.

-   Follow the requested composition and aspect ratio.

-   Use shot size and camera angle to support the intended action.

-   Maintain spatial relationships between characters and props.

-   Do not add unnecessary subjects or objects that interfere with the requested scene.

## G. VIDEO CONTINUITY

-   Break scripts into scenes and shots.

-   Maintain character IDs, clothing, accessories and environment states.

-   Record shot-to-shot positions, orientations and action states.

-   Preserve screen direction and logical movement.

-   Use approved keyframes and appropriate motion-generation or 3D animation tools.

-   Detect temporal inconsistencies, identity drift, flicker and shape deformation.

-   Repair only the affected shot or frames whenever practical.

## H. QUALITY ASSURANCE

-   Evaluate identity fidelity, geometry and anatomy, pose and expression, material and lighting coherence, camera and composition, instruction compliance, scene continuity and rendering artefacts.

-   Reject outputs with major identity drift, malformed anatomy, missing required objects or broken continuity.

## I. ITERATIVE CORRECTION

-   Identify the most significant defect.

-   Determine whether it arises from reference interpretation, identity conditioning, geometry, composition, rendering or continuity.

-   Change the smallest relevant set of parameters.

-   Regenerate or repair the result.

-   Re-evaluate against the original specification.

-   Record approved corrections in project assets or generation metadata.

  -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **FINAL RULE\
  **Character identity and the user's required visual details take priority over unnecessary artistic embellishment. Every approved still or shot must belong to the same coherent animated production.
  -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

  -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# 11. Quality-control scoring engine

Use a weighted rubric to evaluate every generated image and video shot. The weights below are proposed engineering defaults, not measured model performance.

  ------------------------------------------------------------------------------------------------------------------
  **Category**               **Weight**              **Evaluation questions**
  -------------------------- ----------------------- ---------------------------------------------------------------
  Character identity         25%                     Does the output match the canonical character?

  3D geometry and anatomy    20%                     Are shape, proportions, pose and joints coherent?

  Style and rendering        15%                     Does the output match the project's visual style?

  Lighting and composition   10%                     Are light, shadows, framing and readability effective?

  Prompt compliance          15%                     Are all required subjects, actions and details present?

  Scene continuity           15%                     Does the image match the preceding and following shot states?
  ------------------------------------------------------------------------------------------------------------------

Rate each category from 0 to 5. Convert the weighted average to a score out of 100. Suggested thresholds:

  ------------------------------------------------------------------------------------------------------
  **Score**               **Suggested status**    **Action**
  ----------------------- ----------------------- ------------------------------------------------------
  85--100                 Review for approval     Approve only if no critical defect is present.

  70--84                  Needs improvement       Repair notable issues and re-evaluate.

  Below 70                Regenerate or repair    Identify root cause and repeat the generation cycle.
  ------------------------------------------------------------------------------------------------------

  ----------------------------------------------------------------------------------------------------------------------------------------------
  **CRITICAL-DEFECT OVERRIDE\
  **A high total score must never override a critical defect. An attractive image depicting the wrong character must fail identity validation.
  ----------------------------------------------------------------------------------------------------------------------------------------------

  ----------------------------------------------------------------------------------------------------------------------------------------------

## 11.1 Automatic rejection conditions

-   The face differs materially from the canonical character.

-   Required characters or props are missing.

-   Extra limbs, distorted hands or broken geometry are visible.

-   Clothing or accessories change without instruction.

-   The scene violates the requested action or composition.

-   Consecutive frames contain unexplained changes in identity, object placement or lighting.

-   Visual artefacts make the image unsuitable for production.

Use automated vision checks where available, supplemented by human review for subtle identity and artistic errors.

# 12. Training-data preparation and model improvement

  --------------------------------------------------------------------------------------------------
  **Component**                       **Recommended implementation**
  ----------------------------------- --------------------------------------------------------------
  Reference library                   Organised images with character, scene and style IDs

  Character memory                    Structured descriptions plus approved reference assets

  Still-image generation              Image model supporting reference-image conditioning

  Character consistency               Character adapter, identity conditioning or shared 3D asset

  Video scenes                        Image-to-video model or 3D animation engine

  Visual inspection                   Image similarity, vision-language checks and human review

  Persistent learning                 Fine-tuning or adapter training when supported and justified

  Project continuity                  Scene database storing approved state and shot metadata
  --------------------------------------------------------------------------------------------------

## 12.1 Training and validation stages

19. Baseline: test the existing image model using a fixed set of references and prompts.

20. Reference conditioning: improve identity preservation using reference images and suitable conditioning methods.

21. Style calibration: build a consistent project style from approved examples.

22. Character-specific adaptation: train a character-specific adapter if evaluation shows it is necessary.

23. Scene continuity: test the same characters in different environments, poses and camera angles.

24. Video validation: evaluate identity drift and visual flicker across consecutive frames.

25. Regression testing: verify that new model or prompt changes do not damage previously approved outputs.

Keep validation images separate from training images. Otherwise, the system may appear to perform well simply because it has seen the test examples.

# 13. Implementation checklist

## Reference processing

☐ Reference-image upload and organisation

☐ Automatic feature extraction

☐ Master reference selection

☐ Confidence and conflict handling

## Character consistency

☐ Permanent character IDs

☐ Canonical character specifications

☐ Multi-angle reference support

☐ Identity comparison and corrections

## Image generation

☐ 3D cartoon style controls

☐ Pose and expression controls

☐ Camera and lighting controls

☐ Resolution and aspect-ratio settings

## Video production

☐ Script-to-scene breakdown

☐ Shot and keyframe planning

☐ Character and prop continuity

☐ Motion generation and frame review

## Quality assurance

☐ Weighted quality evaluation

☐ Critical-defect detection

☐ Regeneration and repair loop

☐ Approved asset versioning

# 14. Recommended development order

26. Reference-based still images: establish that characters can be reproduced consistently.

27. Character and scene memory: persist approved identities, assets and environment details.

28. Multi-shot scene generation: create a sequence of individually approved keyframes.

29. Motion generation: connect keyframes through a video model or 3D animation engine.

30. Automated quality control: detect defects and selectively regenerate failed outputs.

31. Actual model training: fine-tune or train adapters only when evaluation shows that prompting and reference conditioning are insufficient.

# Conclusion

Build the system in stages. First prove that it can reproduce the same character across several still images. Then add persistent asset memory, scene continuity, video motion and automated correction. Treat identity, geometry, style, cinematography and continuity as separate but coordinated systems. This is the foundation of a reliable 3D animated image and video production pipeline.
