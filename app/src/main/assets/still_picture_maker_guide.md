# Training Manual
## Pixar-Level 3D Still Picture MakerFor Mobile (Text + Image Library Based)
Version 1.0 | October 09, 2026Mobile-First AI Image Generation System
## Table of Contents
- 1. Executive Summary & Product Vision
- 2. What is 'Pixar-Level 3D Still' - Quality Benchmark
- 3. System Architecture - Mobile Focus
- 4. Technology Stack Options
- 5. Core Workflows: Text-to-Image & Library Image-to-Image
- 6. Library Management for Uploaded Pictures
- 7. Prompt Engineering Guide for Pixar 3D Style
- 8. Model Selection & Training Pipeline
- 9. Data Preparation & Dataset Curation
- 10. Fine-Tuning: LoRA / DreamBooth for Pixar Style
- 11. Mobile App Implementation (Android / iOS)
- 12. Cloud vs On-Device Inference Strategy
- 13. Performance Optimization for Mobile
- 14. UI/UX Flow & Screens
- 15. QA, Evaluation & Approval Checklist
- 16. 30-Day Implementation Roadmap
- 17. Code Snippets & API Integration
- 18. Future Scope: From Still to Animated
- 19. Appendix - Resources & Tools
## 1. Executive Summary & Product Vision
This training document covers building a Mobile App that generates Pixar-level 3D still images. The app will take two inputs: (A) User text descriptions (prompts), and (B) Already uploaded pictures from a library (for style reference, character consistency, or image-to-image).
Goal: One-tap generation of high-quality, cinematic, Pixar-style 3D renders that look like still frames from a Pixar movie - suitable for children's books, storyboards, marketing, avatars, and games.
Target Platform: Mobile-first (Android & iOS), optimized for low RAM and offline fallback.
## 2. What is 'Pixar-Level 3D Still' - Quality Benchmark
A Pixar-level still must meet these 5 criteria. Train your team to evaluate every output against this:
- Subsurface Scattering (SSS): Skin, wax, leaves should have soft light bleeding - not plastic
- Global Illumination: Soft shadows, bounced light, not harsh
- Character Design: Big expressive eyes (PBR), stylized proportions (head 1:3 to 1:4 body), appealing shapes
- Materials: Fabric fuzz, hair strands, volumetric depth - not flat texture
- Cinematic Framing: Depth of field, 3-point lighting, filmic color grade
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Level | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Description | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Example Prompt Add-on |
|---|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Level 1 - Basic 3D | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Simple cartoon shading | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>cartoon 3d render |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Level 2 - Good 3D | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Better lighting, PBR materials | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Pixar style, 3D render, soft lighting |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Level 3 - PIXAR LEVEL (Target) | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Cinematic, SSS, ray traced, ultra detail | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Pixar movie still, ultra detailed 3D character, subsurface scattering, global illumination, cinematic lighting, octane render, 8k |

## 3. System Architecture - Mobile Focus
We recommend Hybrid Architecture: Heavy AI runs on Cloud GPU, Mobile app handles Library, Prompt Builder, and Preview. This gives Pixar quality without burning phone battery.
- Mobile Layer (Flutter / React Native): Image Library, Prompt Input, History, User Login
- API Gateway (Node.js / FastAPI): Handles auth, queue, prompt enhancement
- AI Engine (Cloud - RunPod / Replicate / AWS): Stable Diffusion XL + Pixar LoRA + IP-Adapter + ControlNet
- Storage Layer: S3 / Firebase for uploaded library + generated images
- Optional On-Device: Tiny quantized SD model (LCM) for offline draft preview
## 4. Technology Stack Options
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Component | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Recommended Tool (Best Quality) | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Lightweight Alternative |
|---|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Base Model | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>SDXL 1.0 + RealVisXL / JuggernautXL | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>SD 1.5 + DreamShaper |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Pixar Style | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Custom LoRA trained on 300-500 Pixar frames | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Style preset + prompt magic |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Image Reference | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>IP-Adapter Plus (for library images) | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Image2Image 0.6 strength |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Face/Character Consistency | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>InstantID / PhotoMaker | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Same seed + face swap |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Mobile Framework | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Flutter + Firebase | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>React Native + Supabase |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Inference API | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>ComfyUI API / Automatic1111 API / Replicate | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>HuggingFace Inference Endpoints |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Upscaler | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Real-ESRGAN 4x + SUPIR | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Latent upscaler |

## 5. Core Workflows
The app will support 3 generation modes:
## Mode A: Text-to-3D Still (Primary)
Input: User writes description -> Prompt Enhancer adds Pixar keywords -> Generates 4 options -> User picks one.
- Example: User types 'a brave little boy with a backpack in a forest' -> Enhanced to: 'Pixar movie still of a brave little boy with big expressive eyes, wearing red backpack, in magical forest, cinematic lighting, subsurface scattering, ultra detailed fur on backpack, volumetric light rays, octane render, 8k --ar 3:4 --style raw'
## Mode B: Library Image + Text (Style/Character Reference)
- User selects 1-3 images from 'Already Uploaded Pictures Library'
- System uses IP-Adapter: Extracts style, color palette, character face
- User adds text: 'same boy but now riding a bicycle'
- Output maintains character identity from library + new action
## Mode C: Library Image-to-Image (Restyle)
- User uploads a sketch/photo -> Converts to Pixar 3D still with controllable denoise 0.4-0.75
## 6. Library Management for Uploaded Pictures
This is critical for your app. The 'Already Uploaded Pictures' library is your moat for character consistency.
- Tagging System: Auto-tag each uploaded image with BLIP2 captioning + manual tags (Character Name, Style, Mood)
- Embedding: Generate CLIP embedding for each library image for fast similarity search
- Categories: Create folders: Characters, Backgrounds, Props, Styles, References
- Character Lock: When user marks an image as 'Main Character', save its InstantID / Face embedding for future use
- Cleaning Rule: Remove images <512x512, blurry, watermarked. Keep only clean front-facing or 3/4 view for best training
- Mobile Storage: Use Firebase Storage + local SQLite cache of embeddings for offline search
## 7. Prompt Engineering Guide for Pixar 3D Style
Create a Prompt Builder in the app. Don't let users type raw prompts. Use structured builder.
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Block | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>What to Select | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Examples |
|---|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>1. Subject | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Who is it | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>little girl, robot dog, old wizard |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>2. Action/Emotion | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>What doing/feeling | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>laughing, holding balloon, scared |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>3. Style Tag (LOCKED) | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Always add for Pixar level | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Pixar style, Pixar movie still, 3D render, adorable, expressive |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>4. Lighting | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Cinematic | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>cinematic lighting, soft volumetric light, golden hour |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>5. Quality | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Render engine | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>octane render, Unreal Engine 5, subsurface scattering, 8k, ultra detailed |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>6. Camera | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Shot type | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>close-up portrait, depth of field, bokeh |

NEGATIVE PROMPT (Always use): blurry, low quality, distorted face, deformed, ugly, bad anatomy, 2d, flat, plastic skin, cartoonish, watermark, text
## 8. Model Selection & Training Pipeline
For true Pixar level, you must fine-tune a LoRA (Low Rank Adaptation) on your own curated Pixar dataset. Base SDXL alone is not enough.
Pipeline: Collect 400 high-quality Pixar/Disney 3D movie stills (from trailers, artbooks - for internal training) + 100 of your own library images that are already Pixar-like -> Caption them -> Train LoRA -> Test -> Deploy.
## 9. Data Preparation & Dataset Curation
- Resolution: All training images 1024x1024 minimum. Crop to center, keep face in upper third
- Captioning: Use WD14 Tagger + BLIP. Each image caption MUST include: 'Pixar style, 3d render, [character description], [lighting], [material detail]'
- Class Images: For DreamBooth, keep 20 images of '3d character' as regularization
- Library Images: If using uploaded pictures, ask user permission, then clean and add to dataset with tag 'in style of [user library]'
- Dataset Structure: /dataset/0001.jpg + 0001.txt (caption)
## 10. Fine-Tuning: LoRA / DreamBooth for Pixar Style
Training Configuration (Proven for SDXL Pixar LoRA):
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Parameter | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Recommended Value |
|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Base Model | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>SDXL 1.0 |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Resolution | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>1024 x 1024 |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Batch Size | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>2-4 (with gradient accumulation 4) |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Learning Rate | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>1e-4 for UNet, 5e-5 for Text Encoder |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Steps | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>3000-5000 steps (for 400 images) |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>LoRA Rank | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>32 (good balance quality/size ~150MB) |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Optimizer | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>AdamW8bit |
| <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>Caption Dropout | <w:tcPr><w:tcW w:type="dxa" w:w="4320"/></w:tcPr><w:p><w:r><w:t>10% for better prompt following |

Training Command (Kohya SS / OneTrainer):
- accelerate launch --num_cpu_threads_per_process=2 train_network.py --pretrained_model_name_or_path=sdxl-base-1.0 --dataset_config=dataset.toml --output_dir=./lora-pixar --output_name=pixar-style-v1 --save_model_as=safetensors --prior_loss_weight=1.0 --max_train_steps=4000 --learning_rate=0.0001 --lr_scheduler=cosine --network_dim=32 --mixed_precision=fp16 --cache_latents --xformers
## 11. Mobile App Implementation
Recommended Flutter Architecture:
- Screens: 1. Home (Library Grid) 2. Create (Prompt Builder + Library Picker) 3. Generate (Progress + 4-up preview) 4. Detail (Upscale, Save, Re-generate)
- Image Library Widget: GridView with filter chips: Characters, Styles, Backgrounds. Long-press to set as Reference
- Generation: Send POST /generate {enhanced_prompt, library_image_ids[], strength:0.7, negative_prompt, seed}
- Backend: FastAPI endpoint calls ComfyUI workflow via WebSocket API
- ComfyUI Workflow: Load SDXL -> Load Pixar LoRA (weight 0.8) -> IP-Adapter (library images) -> KSampler -> Upscale -> Save
- Display generated images with before/after slider if library image was used
## 12. Cloud vs On-Device Inference Strategy
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Factor | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Cloud Inference (RECOMMENDED) | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>On-Device |
|---|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Quality | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Pixar Level - Full SDXL + LoRA | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Medium - SD 1.5 quantized |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Speed | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>12-25 sec on A100/L4 | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>45-90 sec on flagship phones, heats phone |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Cost | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>$0.01-0.03 per image | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Free after download, but model 2-4GB |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Best For | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Production app launch | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Offline draft mode / premium offline |

Hybrid Launch Plan: Launch with Cloud API first. Add on-device 'Draft Mode' using LCM-LoRA + Tiny VAE for 3-sec previews at 512x512, then upscale via cloud.
## 13. Performance Optimization for Mobile
- Image Compression: Compress library uploads to WebP 1024px before upload to save bandwidth
- Caching: Cache generated images in local Hive DB + show instantly
- Progressive Loading: Show low-res 512px in 5 sec, then replace with 1024px + 2048px upscaled
- Queue System: Use Firebase Queue - don't block UI, send push notification when done
- Battery: Limit 4 parallel generations, add 'Battery Saver' toggle that reduces steps from 30 to 20
## 14. UI/UX Flow & Screens
Wireframe Flow: User opens app -> Sees Library (already uploaded) -> Taps '+' -> Chooses 'Create from Text' OR 'Use Library Image' -> If library: multi-select 1-3 images with strength slider -> Opens Prompt Builder (subject/action/lighting dropdowns) -> Taps Generate -> Sees 4 images loading skeleton -> Taps one to view full screen -> Actions: Upscale 4x, Variation, Edit Prompt, Save to Library, Share.
- Important UX: Show 'Character Lock' toggle - when ON, face from library stays 95% same
- Show 'Style Strength' slider: 0.3 (loose inspiration) to 0.9 (exact style copy)
- Add 'Pixar Magic Button' - one tap that adds all quality tags automatically
## 15. QA, Evaluation & Approval Checklist
- Does skin have SSS glow? Not plastic?
- Are eyes expressive, with reflection/highlight?
- Is lighting soft with ambient occlusion, not flat?
- Is background blurred (DoF) making character pop?
- Does character match library reference if used? (Face similarity > 0.75)
- Any deformed hands/fingers? If yes, auto inpaint hands with hand-fix model
- Resolution final at least 2048px after upscaler?
## 16. 30-Day Implementation Roadmap
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Week | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Tasks | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Deliverable |
|---|---|---|
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Week 1 | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Setup ComfyUI, collect 400 Pixar stills, caption dataset, setup Flutter + Firebase | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Working SDXL pipeline + App skeleton |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Week 2 | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Train Pixar LoRA v1, test with 50 prompts, build Library management + tagging | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>LoRA file + Library screen working |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Week 3 | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Build FastAPI, IP-Adapter integration, Prompt Builder UI, Generate + Preview screens | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>End-to-end generation from mobile |
| <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Week 4 | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Upscaler, QA filter, Play Store build, TestFlight, beta with 10 users, fix bugs | <w:tcPr><w:tcW w:type="dxa" w:w="2880"/></w:tcPr><w:p><w:r><w:t>Beta APK + Training Doc for team |

## 17. Code Snippets & API Integration
Backend FastAPI generate endpoint:
from fastapi import FastAPIimport requestsapp = FastAPI()COMFY_URL = "http://your-gpu:8188/prompt"@app.post("/generate")def generate(prompt: str, library_urls: list, style_strength: float = 0.7):    # 1. Enhance prompt    enhanced = f"{prompt}, Pixar movie still, 3d render, subsurface scattering, cinematic lighting, octane render, ultra detailed, 8k"    # 2. Build ComfyUI workflow JSON with IPAdapter    workflow = { ... } # Load from comfy_workflow_pixar.json    workflow["prompt_text"] = enhanced    workflow["ip_adapter_images"] = library_urls    workflow["ip_strength"] = style_strength        r = requests.post(COMFY_URL, json=workflow)    return r.json()
Flutter Library Picker Snippet:
MultiSelectGrid(  images: libraryImages, // from Firebase  onSelect: (selected) {    ref.read(selectedRefsProvider.notifier).state = selected;  },  maxSelect: 3,)
## 18. Future Scope: From Still to Animated
- Once still quality is locked, add animation: Use AnimateDiff + your Pixar LoRA for 2-sec loops
- Add Talking Avatar: Use SadTalker or LivePortrait with your 3D stills
- Add 3D Model Export: Use TripoSR or LRM to convert still to 3D mesh (.glb) for AR on mobile
- Roadmap: Still v1 -> 4x variation v1.1 -> Short 3-sec animation v2 -> Full 3D model v3
## 19. Appendix - Resources & Tools
- Models: SDXL 1.0 (base), RealVisXL V4, IP-Adapter SDXL, InstantID
- Training Tools: Kohya SS GUI, OneTrainer, ComfyUI
- Dataset Tools: WD14 Tagger, BLIP2 Captioning, CLIP Interrogator
- Mobile: Flutter 3.22, Firebase Storage + Firestore, Hive for cache
- GPU Cloud: RunPod.io ($0.4/hr A100), Replicate, Vast.ai
- Upscalers: Real-ESRGAN, SUPIR, Magnific
- Communities: r/StableDiffusion, Civitai.com (search Pixar LoRA for reference)
END OF TRAINING MANUALReady for Team Training & Development
