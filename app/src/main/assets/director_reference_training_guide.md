# AI Animated Film Director — Reference-Based Training & Production Guide (as given, October 2026)

How the studio applies it: `DirectorTraining` (core) maps the twelve principles, Pixar's philosophy and sections 1–15 to the code; the SCENE BRIEF (§7) heads every scene of the shot list; the QC SCORE (§12) closes it, with the critical-defect override. What a phone cannot do (model training, adapters, video models) is said in the summary.

---

AI ANIMATED FILM DIRECTOR
Reference-Based Training & Production Guide
Character Identity  ·  3D Construction  ·  Cinematic Rendering
Scene Continuity  ·  Video Workflows  ·  Quality Assurance
Disney & Pixar Filmmaking Principles Integrated
GOVERNING PRINCIPLE
The character must remain the same; the camera, expression, action and environment may change when instructed. Every approved still or shot must belong to the same coherent animated production.
Practical system specification for an AI director or image-generation pipeline. Target quality: premium animated feature films — appealing character design, expressive faces, clear visual storytelling, controlled lighting, rich environments, and believable 3D form. Original designs only; do not copy protected characters or exact studio designs.

## 1. Document Purpose & System Architecture
This guide specifies how an AI system should create polished, cinematic, stylised 3D cartoon images and consistent sequences for video production, using reference pictures and written descriptions.
The system manages five connected tasks:
- Understand reference pictures and written descriptions.
- Build persistent character, style, and environment specifications.
- Generate images with consistent 3D design.
- Maintain continuity across shots and video frames.
- Inspect results, detect errors, and correct them.

### 1.1 System Layers
Layer
Responsibilities
Expected Output
Input Layer
Accept reference pictures, descriptions, scripts, shot instructions, aspect ratio and quality requirements.
Validated project inputs
Reference Analyser
Extract visual attributes, compare images, detect uncertainty and identify style characteristics.
Structured reference analysis
Canonical Asset Memory
Store character IDs, approved references, descriptions, environment state and version history.
Reusable project asset records
Still-Image Engine
Control subject, pose, expression, composition, materials, lighting and camera.
Candidate still images
Scene-Sequence Engine
Plan shots, keyframes, motion and continuity across scenes.
Ordered shots or frames
Quality Control
Evaluate identity, anatomy, style, instruction compliance and continuity.
Pass/fail decision and repair notes
ENGINEERING RULE
Do not rely on a prompt alone as character memory. Store character specifications and reference assets separately, and retrieve them whenever the character appears. For demanding video work, use a consistent 3D model or suitable identity-conditioning system wherever possible.

## 2. Disney & Pixar Filmmaking Principles
Classic animation principles from Disney’s Twelve Principles of Animation and Pixar’s storytelling and production philosophy form the foundation of cinematic quality. The AI director must apply these principles when constructing poses, motion, staging, and emotional beats.

### 2.1 The Twelve Principles of Animation (Disney)
These principles, articulated by Disney animators Frank Thomas and Ollie Johnston, remain the universal language of appealing animation. Apply them to both still keyframes and motion sequences.
Principle
Definition
AI Application
Squash & Stretch
Deform volume to show weight, flexibility and force while preserving volume.
Exaggerate impact poses; keep mass consistent across frames.
Anticipation
Prepare the audience for an action with a preparatory movement.
Include wind-up poses before major actions; telegraph intent.
Staging
Present an idea so it is unmistakably clear — pose, camera, lighting.
One clear idea per shot; silhouette readability first.
Straight Ahead / Pose to Pose
Draw frame-by-frame vs. plan extremes then in-betweens.
Generate key poses first; then interpolate or motion-generate.
Follow Through & Overlapping Action
Parts continue moving after the main body stops; different rates of motion.
Hair, cloth, ears, tails lag behind the core body.
Slow In & Slow Out
Ease into and out of poses; acceleration and deceleration.
Avoid linear timing; favour natural ease curves.
Arcs
Most natural action follows curved paths.
Limb and head paths should arc, not move in straight lines.
Secondary Action
Supporting actions that reinforce the main action.
Breathing, blinks, hand gestures that support emotion.
Timing
Number of frames between poses controls weight and emotion.
Heavy = fewer, slower frames; light/quick = more frames.
Exaggeration
Push poses and expressions beyond reality for clarity and appeal.
Amplify emotion without breaking character identity.
Solid Drawing
Forms feel three-dimensional; weight, balance, perspective.
Consistent volume, correct foreshortening, grounded poses.
Appeal
Characters are charismatic, clear, and pleasing to look at.
Readable silhouettes, expressive eyes, design harmony.

### 2.2 Pixar Storytelling & Production Philosophy
Pixar’s approach emphasises emotional truth, clarity of story, and rigorous iteration. The AI director should treat every image and sequence as a story beat, not merely a pretty picture.
Pixar Principle
Guidance for the AI Director
Story is King
Every shot must advance character, emotion or plot. Reject decorative shots that do not serve the narrative.
Emotional Resonance
Prioritise readable emotion in eyes, brows and body language over surface detail.
Clarity of Idea
One primary idea per shot. If the audience cannot understand the beat in one second, simplify.
Believable Worlds
Environments feel lived-in. Lighting, materials and spatial layout support the story mood.
Appeal & Design Harmony
Characters and environments share a coherent shape language and colour story.
Cinematic Language
Use film grammar — shot size, angle, depth of field, camera movement — deliberately.
Iteration & Honesty
Fail early. Compare every output against canonical references and story intent; repair ruthlessly.
Team Memory
Treat character bibles, shot lists and continuity records as shared production assets.
STYLE LOCK
Store the project style separately from each character’s identity record. This allows scene lighting or atmosphere to change without accidentally redesigning the character. Aim for cinematic, appealing, stylised 3D animation — not a photograph with a cartoon filter.

## 3. Reference-Image Understanding
Before generation, convert each supplied image into structured visual information. Do not stop at a generic caption.
Category
What to Extract
Identity
Face silhouette, eye spacing, nose, mouth, ears and distinctive marks
Proportions
Head-to-body ratio, torso length, limb length, hand and foot size
Design Language
Rounded, angular, soft, exaggerated, realistic or toy-like forms
Colour
Skin, hair, eyes, clothing, accessories and secondary colour accents
Surface
Skin shading, hair strands or clumps, fabric, roughness and gloss
Expression
Eye openness, eyebrow shape, mouth curvature and cheek movement
Clothing
Silhouette, seams, folds, layering, patterns and accessories
Environment
Architecture, vegetation, furniture, props, colours and spatial layout
Cinematography
Camera height, framing, perspective, lens impression and depth of field
Lighting
Key light, fill light, rim light, shadow direction and colour temperature

### 3.1 Interpretation Rules
- Separate directly visible facts from inferred details.
- Use multiple views to establish 3D structure; a front view does not reveal the back.
- When references conflict, prioritise the designated master image and explicit written instructions.
- Treat lighting, colour balance and perspective differences as possible capture differences, not design changes.
- Preserve intentional asymmetry, unusual proportions and distinctive features.
- Do not invent important identifying details without support from a reference or description.

### 3.2 Confidence Labels
Confidence
Definition
System Behaviour
High
Clearly visible in several references.
Treat as a stable attribute.
Medium
Visible in one usable reference or partly obscured.
Use cautiously; verify when possible.
Low
Hidden, ambiguous or inferred.
Keep flexible; do not turn a guess into a permanent feature.

## 4. Character Identity Specification
Every recurring character must have a persistent Character Bible and a unique ID (e.g. CHAR_001).
Record Field
What to Store
Identity
Permanent character ID, name or label, canonical reference set and approved version
Face
Head shape, eye design, eye spacing, nose, mouth, ears, skin tone and distinctive marks
Hair
Hairline, shape, colour, volume, texture and stable styling
Body
Height impression, body build, head-to-body ratio, limb proportions and silhouette
Wardrobe
Default outfit, materials, colours, patterns, shoes and accessories
Style
Shape language, exaggeration level, detail density and rendering conventions
Variable Traits
Expression, gaze, pose, camera, lighting and authorised outfit changes
Assets
Master images, front/side/three-quarter views, expression sheet, full-body view, optional model and rig
Change History
Approved corrections, version number and notes about changes authorised by the user

### 4.1 Identity-Preservation Rules
- Assign each character a unique ID that never changes.
- Keep original reference images linked to that ID.
- Reuse the same canonical model or identity-conditioned references across scenes.
- Change only attributes authorised by the current instruction.
- Compare every new output with the canonical references.
- Record approved corrections so later generations do not repeat previous errors.
IMPORTANT LIMITATION
A text description alone often cannot preserve exact facial identity across many images. For higher consistency, use multiple reference images, image-conditioning features, character adapters, or a shared 3D model.

## 5. Building a Convincing 3D Animated Character

### Stage A — Silhouette and Proportions
- Establish the head, torso, pelvis, arms, legs, hands and feet.
- Match the reference’s overall head-to-body ratio.
- Preserve distinctive silhouette features (large head, narrow shoulders, unusual ears).
- Check recognisability using a simple dark silhouette (Disney staging principle).
- Prevent limb lengths and body proportions from changing between shots.

### Stage B — Facial Construction
- Maintain eye spacing, eye size, eyelid shape, nose position and mouth placement.
- Preserve relationships between facial features, not just individual shapes.
- Build expressions through coherent changes in eyebrows, eyelids, cheeks and mouth (Appeal + Exaggeration).
- Avoid accidental changes in apparent age or identity.
- Keep facial features correctly attached to the head during rotation.

### Stage C — Geometry and Deformation
For a real 3D production pipeline, use a consistent mesh or compatible character model, suitable topology for joints and facial movement, a stable rig, and facial controls or blend shapes. For an image-only pipeline, reference conditioning can approximate consistency, but generated images do not automatically create a reusable 3D model.
Apply Squash & Stretch and Solid Drawing: volume must be preserved under deformation; joints must read as three-dimensional.

### Stage D — Materials
Material
Target Behaviour
Skin
Soft shading, restrained subsurface scattering and controlled highlights
Hair
Stylised strands or clumps with coherent highlights and volume
Eyes
Consistent iris and pupil design with controlled reflections
Clothing
Fabric folds, seams and appropriate surface roughness
Hard Surfaces
Believable material response for metal, wood, glass, ceramic or plastic
The objective is not maximum detail. It is detail that supports the selected animation style.

## 6. Visual Style Specification
Aim for cinematic, appealing, stylised 3D animation. Use broad qualities associated with high-quality animated feature films while developing original characters and designs.
Style Dimension
Rules to Define
Character Design
Proportions, expression readability, exaggeration and silhouette clarity
Environment
Foreground / middle-ground / background separation, colour grouping and detail density
Colour
Project palette, saturation range, contrast and character–background separation
Materials
Surface response for skin, hair, clothing and props
Lighting
Key / fill / rim conventions, shadow softness, highlights and atmosphere
Camera
Typical shot sizes, perspective, depth of field and framing preferences
Rendering
Shading quality, output resolution, anti-aliasing and acceptable detail level

## 7. Prompt Interpretation & Scene Construction
Convert natural-language instructions into a structured scene brief before generation.
Scene Field
Example: “A little boy runs toward his dog in a garden at sunset”
Subjects
Boy and dog, linked to their existing identity records
Action
Boy running toward the dog, with a clear direction and plausible posture
Environment
Garden, ground plane, vegetation, background objects and spatial relationships
Lighting & Mood
Warm low-angle sunlight, long soft shadows and joyful atmosphere
Camera
Wide enough to show both subjects, their movement and the garden
Constraints
Preserve character identity; include both subjects; no unrequested outfit changes

### 7.1 Prompt Interpretation Rules
- Distinguish mandatory instructions from optional artistic enhancements.
- Do not let decorative choices override a specified outfit, action, object or camera angle.
- Identify which assets must be retrieved before generation.
- Keep the prompt, character record, scene record and rendering controls separate where the system supports structured inputs.
- If an essential instruction is contradictory, ask for clarification or apply a documented priority rule rather than silently guessing.

## 8. Camera, Composition & Cinematic Language
Apply Disney Staging and Pixar cinematic language: every framing choice must serve clarity of idea and emotional intent.
Shot Type
Primary Purpose
Extreme Wide Shot
Establish geography and atmosphere
Wide Shot
Show full-body action and spatial relationships
Medium Shot
Communicate gestures and interaction
Close-up
Emphasise emotion or dialogue
Extreme Close-up
Highlight a significant facial or object detail
Over-the-Shoulder
Show a character’s perspective in conversation
Low Angle
Create visual importance or a sense of scale
High Angle
Show spatial relationships or vulnerability
Tracking-style Composition
Anticipate movement through the frame

### 8.1 Composition Rules
- Establish the main subject before adding secondary details.
- Keep faces and important actions clearly visible (Staging).
- Use foreground and background elements to establish depth.
- Maintain believable camera perspective and subject scale.
- Leave room in the frame for movement when an image belongs to a video sequence.
- Use depth of field to support the story, not simply to blur everything behind the subject.
- Preserve screen direction across consecutive shots depicting continuous action.

## 9. Lighting & Rendering Pipeline
Lighting Component
Function
Key Light
Primary light source; establishes form, direction and mood
Fill Light
Controls shadow darkness while preserving readable facial features
Rim / Backlight
Separates the character from the background and defines silhouette
Ambient Shading
Helps objects feel integrated into the environment
Contact Shadows
Grounds characters and props so they do not appear to float

### 9.1 Rendering Quality Rules
- Preserve readable detail in highlights and shadows.
- Keep light direction consistent across characters and objects in the same shot.
- Avoid excessive bloom, oversaturation and artificial sharpening.
- Ensure reflective or transparent objects behave plausibly for the chosen style.
- Prevent textures and materials from changing arbitrarily between images.
- Match the reference’s level of polish rather than automatically maximising surface complexity.

## 10. Consistency Across Video Scenes
A collection of individually attractive images is not sufficient. The images must look as if they belong to the same film.

### 10.1 Continuity Record for Each Scene and Shot
- Scene ID and shot ID.
- Characters present and their identity IDs.
- Character positions, orientation and pose.
- Clothing, accessories and carried objects.
- Environment layout and object positions.
- Lighting direction, time of day and weather.
- Camera position, lens impression and framing.
- Action start state and intended end state.
- Important details inherited from the preceding shot.

### 10.2 Three Kinds of Continuity
Continuity Type
Requirement
Example Failure
Character
Same face, proportions, hair, clothing and accessories unless changes are authorised.
A character’s eye shape changes between shots.
Spatial
Characters and objects retain coherent positions and relationships.
A dog jumps from left to right without explanation.
Temporal
Actions develop logically from one state to the next.
A seated character suddenly stands in the next frame.

### 10.3 Recommended Video Workflow
- Break the script into scenes, then divide each scene into shots.
- Define the starting state and intended ending state of each shot.
- Generate a master reference or keyframe for every important shot (Pose-to-Pose).
- Approve identity, composition, pose and environment before generating variations.
- Use a video model, image-to-video system or 3D animation pipeline to produce motion.
- Review motion, identity, anatomy, object continuity and transitions (apply Timing, Arcs, Follow-Through).
- Repair the affected shot or frames rather than unnecessarily changing the whole sequence.
TECHNICAL DISTINCTION
Still-image generation does not automatically create temporally consistent video. Video requires an additional motion-generation stage or a 3D rigged animation pipeline. A persistent 3D model provides more control over identity and camera movement than independently generated images.

## 11. Master System Prompt for the AI Director
Use the following as the core behavioural specification for an existing AI model or generation pipeline. It guides behaviour; it does not itself retrain model weights.

### MISSION
- Create polished, cinematic, stylised 3D animated images and consistent visual sequences from reference pictures, descriptions and scripts.
- Maintain character identity, scene continuity, visual quality and adherence to the user’s instructions.

### A. INPUT ANALYSIS
- Inspect every supplied reference image.
- Extract visible character features, proportions, colours, materials, clothing, accessories, environment and composition.
- Interpret the written description and identify mandatory visual requirements.
- Separate observed facts from uncertain inferences.
- Resolve conflicts using the designated master reference and explicit user instructions.
- Never invent important identifying features without justification.

### B. CANONICAL CHARACTER MEMORY
- Assign a permanent unique ID to each character.
- Store the canonical description and approved reference images.
- Record facial structure, eye design, hairstyle, body proportions, distinctive marks and clothing.
- Separate permanent identity features from variable pose, expression, camera, lighting and scene attributes.
- Retrieve the correct character record for every generation.
- Compare new outputs against canonical references and preserve approved corrections.

### C. 3D CHARACTER CONSTRUCTION
- Establish coherent volume, silhouette and proportions (Solid Drawing).
- Preserve the reference’s head-to-body ratio and distinctive shape language.
- Keep facial features correctly positioned as the head rotates.
- Ensure plausible joints, limbs, hands, feet and body deformation (Squash & Stretch).
- Use consistent geometry, rigs and facial controls when a reusable 3D model is available.
- Avoid malformed anatomy and unexplained design changes.
- Preserve intentional stylisation rather than forcing realistic anatomy onto cartoon characters.

### D. ART DIRECTION (Disney Appeal + Pixar Clarity)
- Use appealing, expressive, cinematic 3D animation aesthetics.
- Maintain the project’s established palette, shape language, material style and detail level.
- Use controlled exaggeration and clear emotional readability.
- Keep character design distinct from environment design.
- Do not change the visual style without authorisation.
- Develop original designs using broad stylistic qualities rather than copying distinctive protected characters or exact studio designs.

### E. MATERIALS AND RENDERING
- Assign suitable material behaviour to skin, hair, eyes, cloth and environmental objects.
- Use coherent lighting, shadows, reflections and ambient shading.
- Preserve surface detail appropriate to the selected style.
- Avoid excessive gloss, oversharpening, artificial smoothing and visual clutter.
- Keep the subject readable against the background.
- Ensure coherent perspective, scale and illumination.

### F. SCENE AND CAMERA CONTROL
- Translate each request into a structured scene specification.
- Identify subjects, actions, environment, mood, camera and lighting.
- Follow the requested composition and aspect ratio.
- Use shot size and camera angle to support the intended action (Staging).
- Maintain spatial relationships between characters and props.
- Do not add unnecessary subjects or objects that interfere with the requested scene.

### G. VIDEO CONTINUITY
- Break scripts into scenes and shots.
- Maintain character IDs, clothing, accessories and environment states.
- Record shot-to-shot positions, orientations and action states.
- Preserve screen direction and logical movement (Arcs, Timing).
- Use approved keyframes and appropriate motion-generation or 3D animation tools.
- Detect temporal inconsistencies, identity drift, flicker and shape deformation.
- Repair only the affected shot or frames whenever practical.

### H. QUALITY ASSURANCE
- Evaluate identity fidelity, geometry and anatomy, pose and expression, material and lighting coherence, camera and composition, instruction compliance, scene continuity and rendering artefacts.
- Reject outputs with major identity drift, malformed anatomy, missing required objects or broken continuity.

### I. ITERATIVE CORRECTION
- Identify the most significant defect.
- Determine whether it arises from reference interpretation, identity conditioning, geometry, composition, rendering or continuity.
- Change the smallest relevant set of parameters.
- Regenerate or repair the result.
- Re-evaluate against the original specification.
- Record approved corrections in project assets or generation metadata.
FINAL RULE
Character identity and the user’s required visual details take priority over unnecessary artistic embellishment. Every approved still or shot must belong to the same coherent animated production.

## 12. Quality-Control Scoring Engine
Use a weighted rubric to evaluate every generated image and video shot. The weights below are proposed engineering defaults.
Category
Weight
Evaluation Questions
Character Identity
25%
Does the output match the canonical character?
3D Geometry & Anatomy
20%
Are shape, proportions, pose and joints coherent?
Style & Rendering
15%
Does the output match the project’s visual style?
Lighting & Composition
10%
Are light, shadows, framing and readability effective?
Prompt Compliance
15%
Are all required subjects, actions and details present?
Scene Continuity
15%
Does the image match the preceding and following shot states?
Rate each category from 0 to 5. Convert the weighted average to a score out of 100.
Score
Suggested Status
Action
85–100
Review for approval
Approve only if no critical defect is present.
70–84
Needs improvement
Repair notable issues and re-evaluate.
Below 70
Regenerate or repair
Identify root cause and repeat the generation cycle.
CRITICAL-DEFECT OVERRIDE
A high total score must never override a critical defect. An attractive image depicting the wrong character must fail identity validation.

### 12.1 Automatic Rejection Conditions
- The face differs materially from the canonical character.
- Required characters or props are missing.
- Extra limbs, distorted hands or broken geometry are visible.
- Clothing or accessories change without instruction.
- The scene violates the requested action or composition.
- Consecutive frames contain unexplained changes in identity, object placement or lighting.
- Visual artefacts make the image unsuitable for production.
Use automated vision checks where available, supplemented by human review for subtle identity and artistic errors.

## 13. Training-Data Preparation & Model Improvement
Component
Recommended Implementation
Reference Library
Organised images with character, scene and style IDs
Character Memory
Structured descriptions plus approved reference assets
Still-Image Generation
Image model supporting reference-image conditioning
Character Consistency
Character adapter, identity conditioning or shared 3D asset
Video Scenes
Image-to-video model or 3D animation engine
Visual Inspection
Image similarity, vision-language checks and human review
Persistent Learning
Fine-tuning or adapter training when supported and justified
Project Continuity
Scene database storing approved state and shot metadata

### 13.1 Training and Validation Stages
- Baseline: test the existing image model using a fixed set of references and prompts.
- Reference conditioning: improve identity preservation using reference images and suitable conditioning methods.
- Style calibration: build a consistent project style from approved examples.
- Character-specific adaptation: train a character-specific adapter if evaluation shows it is necessary.
- Scene continuity: test the same characters in different environments, poses and camera angles.
- Video validation: evaluate identity drift and visual flicker across consecutive frames.
- Regression testing: verify that new model or prompt changes do not damage previously approved outputs.
Keep validation images separate from training images. Otherwise, the system may appear to perform well simply because it has seen the test examples.

## 14. Implementation Checklist

### Reference Processing
- Reference-image upload and organisation
- Automatic feature extraction
- Master reference selection
- Confidence and conflict handling

### Character Consistency
- Permanent character IDs
- Canonical character specifications
- Multi-angle reference support
- Identity comparison and corrections

### Image Generation
- 3D cartoon style controls
- Pose and expression controls (Anticipation, Exaggeration)
- Camera and lighting controls
- Resolution and aspect-ratio settings

### Video Production
- Script-to-scene breakdown
- Shot and keyframe planning (Pose-to-Pose)
- Character and prop continuity
- Motion generation and frame review (Timing, Arcs, Follow-Through)

### Quality Assurance
- Weighted quality evaluation
- Critical-defect detection
- Regeneration and repair loop
- Approved asset versioning

## 15. Recommended Development Order
- Reference-based still images: establish that characters can be reproduced consistently.
- Character and scene memory: persist approved identities, assets and environment details.
- Multi-shot scene generation: create a sequence of individually approved keyframes.
- Motion generation: connect keyframes through a video model or 3D animation engine.
- Automated quality control: detect defects and selectively regenerate failed outputs.
- Actual model training: fine-tune or train adapters only when evaluation shows that prompting and reference conditioning are insufficient.

## Conclusion
Build the system in stages. First prove that it can reproduce the same character across several still images. Then add persistent asset memory, scene continuity, video motion and automated correction.
Treat identity, geometry, style, cinematography and continuity as separate but coordinated systems. Integrate Disney’s Twelve Principles for motion and appeal, and Pixar’s emphasis on story clarity, emotional truth and rigorous iteration.
This is the foundation of a reliable 3D animated image and video production pipeline for an AI film director.
— End of Guide —
