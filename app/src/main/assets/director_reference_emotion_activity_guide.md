# Comprehensive Training Guide for AI-Based Animated Director
Emotion, Activity & Camera Angle IntelligenceWith Special Focus on Dark Image Front/Back Disambiguation
Version: 2.0 | Created: October 09, 2026 | For: Production Training Team
---
## Table of Contents
- 1. Executive Summary & Goals
- 2. Understanding the Core Failures
- 3. System Architecture Overview
- 4. Ontology & Label Taxonomy
- 5. Dataset Strategy & Collection
- 6. Annotation Protocol & Quality Control
- 7. Preprocessing Pipeline - The Critical Fix
- 8. Model Architecture - Multi-Task Design
- 9. Training Curriculum - Phase by Phase
- 10. Loss Functions & Optimization
- 11. Special Module: Solving Dark Image Back-as-Front Bug
- 12. Data Augmentation for Robustness
- 13. Evaluation Framework & Metrics
- 14. Integration with Animated Director
- 15. Deployment, Optimization & MLOps
- 16. Ethics, Privacy & Bias
- 17. Appendix A: Tools, Libraries, Hardware
- 18. Appendix B: Code Templates & Checklists
- 19. Appendix C: Sample Data & Configs
## 1. Executive Summary & Goals
This guide trains an AI Animated Director that takes any uploaded or saved picture (from gallery, camera roll, drive) and outputs three correct signals for animation: What is the person feeling (emotion), what is the person doing (activity), and from where are we viewing them (angle/orientation).Primary Goal: 95%+ accuracy on front vs back classification even in low-light (< 10 lux) images, and elimination of false emotion prediction on back-view images.Secondary Goals: Robust emotion detection under occlusion, activity recognition from single image, and a gated inference system where low-confidence predictions do not cause bad animation.
## 2. Understanding the Core Failures
## Why back side is taken as front in dark pictures?
- Face detector is brightness-dependent: In dark images, it fails to find face, but head silhouette still exists. Model guesses.
- Lack of texture: Dark removes eye/nose/mouth cues. The round head shape from back looks identical to front face in silhouette.
- Training bias: 85% of public datasets are bright, front-facing images. Model has never seen dark back images.
- EXIF rotation ignored: Saved pictures from phone may have rotation tag, model sees upside-down back as front.
- No gating logic: Current pipeline predicts emotion even when no face is present, leading to hallucinated smiles on back of head.
## Failure Taxonomy to Track
Failure Type
Example
Impact on Animation
Front-Back Flip in Dark
Back of head in dark room -> predicted as sad front face
Character talks with back to camera
False Emotion on Back
No face visible but predicts happy 0.9
Back of head smiles
Activity Confusion
Bending seen as sitting due to angle
Wrong body rig applied
Angle Jitter
Slight dark -> yaw jumps 0 to 180
Camera flickers
## 3. System Architecture Overview
Architecture is multi-task with a Validation Gate, not a single end-to-end model.Flow: Input Image -> Preprocessing (EXIF fix, Low-Light Enhancement, Detection) -> Shared Backbone (Vision Transformer) -> Validation Gate (Is face/body confidence enough?) -> If FAIL: Return angle only + request brighter image. If PASS: Parallel Heads (Emotion, Activity, Angle) -> Rule Engine Fusion -> Animation Director JSON.Why this works: Angle head uses body cues which survive in dark. Emotion head uses face crop which fails in dark. Gate prevents emotion head from running on back images.
## 4. Ontology & Label Taxonomy
## 4.1 Emotion Taxonomy
Do not use only 7 labels. Use hierarchical:Level 1 (Primary): Happy, Sad, Angry, Fear, Surprise, Disgust, Neutral, No_FaceLevel 2 (Intensity): 0.0 to 1.0 continuousLevel 3 (Compound): Happily_Surprised, Angrily_Disgusted, Sadly_Fearful etc. (21 combos)Level 4 (Visibility): fully_visible, partially_occluded, heavily_shadowed, not_visibleCritical Rule: If view is back_view, emotion MUST be No_Face. Enforce in training with penalty loss.
## 4.2 Activity Taxonomy
Use verb + direction + intensity for animation mapping:Atomic Actions: stand, sit, walk, run, bend, turn, wave, dance, talk, hold_objectBody Parts: head_turn_left, torso_twist_right, arm_raisePose States: Use 17-point COCO keypoints + 68 face landmarks + hand landmarksExample labels: walk_forward_slow, sit_on_chair_leaning, back_facing_stationary
## 4.3 Angle & Orientation Taxonomy
Yaw (horizontal): -180 to +180 degrees, 0 = front, 90 = left side, 180 = back, -90 = right sidePitch: -90 up to +90 downView Class: front_view (yaw -45 to 45), side_view (45 to 135), back_view (135 to 225), top_view etc.Additional attributes: camera_distance (close, medium, far), tilt, Dutch angle
## 5. Dataset Strategy & Collection
You need minimum 100k images total, split into 4 buckets. Do not scrape randomly.
Bucket
Size
Sources
Key Requirement
Emotion
40k-60k
AffectNet, RAF-DB, FER+, Your gallery
30% low-light, include No_Face
Activity
30k
Kinetics-400 frames, COCO, NTU RGB+D, Custom
Full body, diverse angles
Orientation
25k
Custom collection critical
MUST include 8k dark back-view pairs
Dark Augmentation
20k synthetic
Generated from clean
Same image bright + dark versions
## Custom Dark Back-View Collection Protocol
This is how you fix the bug:1. Record 20 people in a dark room (<15 lux) with phone flash off. Take 10 photos each: 5 front, 5 back, same position, same distance.2. Vary conditions: with/without cap, long hair/short hair, hoodie, backpack.3. Capture at 3 distances: 1m, 2.5m, 4m.4. For each image, annotate: face confidence (manual), shoulder width, ear visible yes/no, hair outline, depth cue.5. This 2k set is your gold test set. Never train on it, only evaluate.
## 6. Annotation Protocol & Quality Control
Use CVAT or Label Studio. Each image needs 2 annotators + 1 reviewer for orientation bucket.Annotation Steps per Image:Step 1: Is face visible? Yes/No. If No, skip emotion.Step 2: Mark face bounding box, body bounding box.Step 3: Label view class + yaw angle using protractor tool.Step 4: If front/side, label emotion + intensity + occlusion level.Step 5: Label activity + keypoints.Step 6: Tag light_level: bright (>200 lux), normal, low (30-100), very_low (<30), silhouette.Quality: Inter-annotator agreement for view must be >0.95 Kappa. For dark back images, 100% review.
## 7. Preprocessing Pipeline - The Critical Fix
- Step 7.1 EXIF Fix: Use Pillow ImageOps.exif_transpose. Many saved images have Orientation=6 (rotated 90 deg). If not fixed, model sees sideways back as front.
- Step 7.2 Face & Body Detection First: Run YOLOv8-face (confidence threshold 0.2 low for dark) and RTMPose for body. Get bounding boxes BEFORE enhancement to compare.
- Step 7.3 Low-Light Enhancement - Dual Path: Path A: Original image kept. Path B: Enhanced using Zero-DCE network (pretrained, lightweight). Do NOT overwrite original. Model will see both. For face region, apply CLAHE on L-channel in LAB color space with clipLimit 2.0. This reveals eyes without washing out.
- Step 7.4 Region-Specific Normalization: For face crop: histogram equalization + face alignment via 5-point landmarks. For body: keep aspect ratio, pad to square. For dark images, apply gamma correction gamma=1.8 for body.
- Step 7.5 Confidence Gating: If face_confidence_enhanced < 0.35 and body shows back orientation cues (see Section 11), route directly to Angle Head, set emotion=No_Face, confidence low. This prevents hallucination.
## 8. Model Architecture - Multi-Task Design
## Shared Backbone Options
Recommended: DINOv2 ViT-S/14 or ConvNeXt-V2-Tiny. Both robust to lighting. Pretrained on 142M images, good for low-light.Input size: 384x384 for backbone, 224x224 for face crop.Feature extraction: Use last 4 layers, concatenate.Head Architectures:Emotion Head: Face crop -> EfficientNet-B3 -> Attention pooling (focus on eyes/mouth) -> FC 256 -> 8 classes + intensity regression. Use dropout 0.5 due to small face data.Activity Head: Body keypoint heatmaps (17 channels) + RGB body crop -> SlowFast-style or PoseConv3D. For single image, use HRNet-W32 for pose + MLP classifier. Output activity + keypoints refinement.Angle Head: MOST IMPORTANT. Input = full image + body keypoint heatmap + face heatmap + edge map (Sobel) + depth map (MiDaS small). Architecture: ResNet18 + CBAM attention. Two outputs: view classification (4 classes) + yaw regression. Add auxiliary loss for ear visibility, shoulder orientation.Fusion Module: Angle logits gate emotion. Formula: if P(back_view) > 0.7, then emotion_loss_weight = 0. So model not penalized for not predicting emotion on back.
## 9. Training Curriculum - Phase by Phase
- Phase 0 - Setup (1 day): Prepare dataloaders with light_level tag. Create weighted sampler so each batch has 40% bright, 30% normal, 30% dark. Implement validation gate logic.
- Phase 1 - Angle Foundation (3-5 epochs): Train ONLY Angle Head + backbone last 2 layers on Orientation Bucket. Learning rate 1e-4. Augmentation: only geometric (rotate +-15 deg, flip). Goal: >93% front/back accuracy on bright set.
- Phase 2 - Dark Robustness Injection (5 epochs): Freeze backbone, train Angle Head on Dark + Bright pairs. Use consistency loss: L_cons = MSE(pred_bright, pred_dark). Add hard negatives: dark back images misclassified as front get 3x loss. LR 5e-5.
- Phase 3 - Emotion & Activity Parallel (8 epochs): Freeze angle head. Train emotion head only on images where view != back_view and face_conf >0.5. Use focal loss gamma=2 for class imbalance. Train activity head on all images. LR 1e-4 for heads, 1e-5 for backbone.
- Phase 4 - Joint Multi-Task Fine-Tune (10 epochs): Unfreeze all. Combined loss = 0.4*L_angle + 0.3*L_emotion + 0.2*L_activity + 0.1*L_consistency. Add rule penalty: If view=back and emotion != No_Face, add L_penalty=2.0. Use AdamW, weight decay 0.01. Cosine scheduler.
- Phase 5 - Hard Case Fine-Tune (3 epochs): Collect all failures from validation (especially dark back as front). Create mini dataset 2k images. Train with LR 1e-6, high augmentation.
## 10. Loss Functions & Optimization
L_angle = CrossEntropy(view_class) + 0.5 * SmoothL1(yaw_pred, yaw_true) + 0.2 * BCE(ear_visibility)L_emotion = FocalLoss(emotion_class, gamma=2) + 0.3 * L1(intensity) . Masked: if view==back, L_emotion=0L_activity = CrossEntropy(activity) + 0.5 * MSE(keypoints)L_consistency = MSE(pred(bright_version), pred(dark_version)) for same identityL_penalty = 2.0 if (P_back>0.7 and P_emotion_not_NoFace>0.5) else 0Total = weighted sum. Use gradient clipping 1.0. Early stopping on Dark Back Accuracy, not overall accuracy.
## 11. Special Module: Solving Dark Image Back-as-Front Bug
## Why Silhouette Confusion Happens
In RGB, dark image loses chroma. Model relies on shape. Head from back is oval, from front also oval. Without eyes, model uses context: if image is dark, it defaults to front because 90% of training data is front. We must force it to use body cues.
## 6-Layer Fix Implemented
- Layer 1 - Asymmetry Cues: Train model to detect ear (front has 1-2 ears visible at sides, back has 0 or occluded), hairline (front shows forehead, back shows nape), nose tip. Add segmentation head for hair vs face. In dark, hair outline is still detectable via edge.
- Layer 2 - Shoulder & Torso Orientation: From pose keypoints: Front view: shoulder line slightly curved, chest keypoint visible, clavicle. Back view: flat shoulder blade line, spine line visible, no chest. Compute feature: vector from left_shoulder to right_shoulder vs nose position. If nose is behind shoulder line, it's back.
- Layer 3 - Depth & Edge: Run MiDaS small (10ms). Front face has depth variation nose closer than ears. Back head depth is smooth sphere. Edge density: front face in dark still has some specular highlight on nose, back has uniform. Concatenate depth map as 4th channel.
- Layer 4 - Temporal Consistency (if video or burst photos): Yaw cannot jump 0 to 180 in one frame. Apply Kalman filter on yaw predictions.
- Layer 5 - Confidence-Based Gating: Final inference rule: IF face_conf < 0.35 AND ear_visibility <0.2 AND shoulder_back_prob >0.6 THEN force view=back_view, emotion=No_Face, regardless of emotion head output. This is non-learned safety net.
- Layer 6 - User Feedback Loop: In Animated Director UI, if confidence <0.65, show prompt: 'Image is dark, appears to be back view - confirm?' Let user correct, log correction for retraining.
## Inference Pseudo-Code
def infer(image):    img_fixed = exif_transpose(image)    face_box, face_conf = yolo_face(img_fixed)    pose = rtmpose(img_fixed)    img_enhanced = zero_dce(img_fixed)    face_conf_enh = yolo_face(img_enhanced)[1]        angle_view, yaw = angle_head(img_fixed, pose, depth_map)        # Safety Gate for Dark Back    if face_conf_enh < 0.35 and angle_view == 'back_view':        return {'view':'back_view','yaw':yaw,'emotion':'No_Face','activity':activity_head(img_fixed,pose),'confidence':0.85}        if face_conf_enh >= 0.35:        emotion = emotion_head(face_crop(img_enhanced))    else:        emotion = 'No_Face'    return {...}
## 12. Data Augmentation for Robustness
Photometric (for dark robustness): RandomBrightnessContrast (brightness -70% to +20%), RandomGamma, ISO Noise, Motion Blur, JPEG Compression (for saved pictures), Cutout on face region (simulate occlusion).Geometric: RandomRotate +-20 deg (angle label must be adjusted), HorizontalFlip (swap left/right yaw), Perspective 0.2, Scale jitter 0.8-1.2.Synthetic Dark: Use LIME or EnlightenGAN to generate paired bright/dark. Keep same label.Mixup: Mix front and back images with alpha 0.2 to force model to not be overconfident.
## 13. Evaluation Framework & Metrics
Create 4 test sets, never train on them:1. Bright Front Test (2k)2. Dark Front Test (2k)3. Bright Back Test (1k)4. Dark Back Gold Test (2k) - your critical metric.Metrics to track per test set:- View Accuracy: Front vs Back binary. Target: >96% on Dark Back.- Yaw MAE: mean absolute error in degrees. Target <12 deg.- Emotion F1 on visible faces only. Do NOT compute emotion accuracy on back images.- Activity Top-1 and Top-3.- False Emotion Rate on Back: % of back images where model predicts an emotion other than No_Face. Target <2%.- Robustness Drop: Acc_bright - Acc_dark. Target <4%.Log confusion matrix for Dark Back set. Every image misclassified as front must be visualized and added to hard negative set.
## 14. Integration with Animated Director
Animated Director should receive structured JSON, not raw labels.API Contract:{  "input_image_id": "...",  "view": "back_view",  "yaw": 182,  "pitch": -5,  "emotion": "No_Face",  "emotion_intensity": 0.0,  "activity": "standing_stationary",  "pose_keypoints": [...],  "light_level": "very_low",  "confidence": 0.88,  "animation_commands": {    "face_rig": "disable - back view",    "body_rig": "idle_standing_back",    "camera": "position behind character, yaw 180"  },  "requires_user_confirmation": false}Prompt Engineering for Director LLM: If view is back, prompt must include 'Do not animate face, character facing away'. If light_level is very_low and confidence <0.7, prompt: 'Scene is dark, use silhouette style'.Mapping: Emotion Happy 0.8 + Front -> blendshape smile 0.8. Activity walk_forward + side_view -> walk cycle side. Always validate that face animation is disabled for back_view.
## 15. Deployment, Optimization & MLOps
Edge vs Cloud:- On device (mobile app): Use quantized YOLOv8n-face + MobileNetV3 angle classifier for quick gate (15ms). If gate says probable dark back, skip cloud call and return directly.- Cloud: Full ViT backbone for high accuracy.Optimization: Export to ONNX, quantize to INT8 for angle head, FP16 for emotion. Use TensorRT.Latency budget: Preprocessing 30ms, Backbone 60ms, Heads 20ms total <150ms.MLOps Loop:1. Log every inference with confidence, view, face_conf, light_level.2. Weekly, sample 200 low-confidence dark images, manually re-label.3. Retrain Phase 5 on new failures.4. Version datasets with DVC, models with MLflow.5. A/B test new model on Dark Back Gold set before production.
## 16. Ethics, Privacy & Bias
Privacy: Uploaded/saved pictures are sensitive. Do not store raw images after inference unless user opts in. Store embeddings only. Blur background by default.Bias: Ensure orientation dataset includes diverse skin tones, age, gender, clothing (hijab, turban, long hair). Dark image performance often drops for darker skin tones - test separately and balance.Consent: For custom collection, get consent, especially for dark room photos.Transparency: If emotion is No_Face due to back view, tell user why, don't just show blank.
## 17. Appendix A: Tools, Libraries, Hardware
Libraries: PyTorch 2.2+, HuggingFace Transformers, Ultralytics, MediaPipe, OpenCV, Albumentations, PyTorch Lightning, Zero-DCE (https://github.com/Li-Chongyi/Zero-DCE), MiDaS, ONNX Runtime.Hardware for Training: 1x A100 40GB or 2x RTX 4090. Batch size 32. Training time ~2 days for full curriculum.Hardware for Inference: Cloud: T4 GPU. Edge: Snapdragon 8 Gen 2 NPU for quantized model.Labeling Tools: CVAT, Label Studio with custom template for yaw.Tracking: Weights & Biases - log separate charts for Bright vs Dark accuracy.
## 18. Appendix B: Code Templates & Checklists
## Preprocessing Checklist
- [ ] EXIF transpose applied?
- [ ] Both original and enhanced version saved for training pair?
- [ ] Face confidence logged from both original and enhanced?
- [ ] Pose keypoints extracted and validated (confidence >0.3)?
- [ ] Depth map generated?
- [ ] Light level tagged automatically via mean luminance?
## Training Config YAML Template
model:  backbone: dinov2_vits14  image_size: 384  dropout: 0.5data:  batch_size: 32  dark_ratio: 0.3  orientation_weight: 0.4  emotion_weight: 0.3training:  phases: [angle, dark, emotion_activity, joint, hard]  lr: 1e-4  optimizer: AdamW  focal_gamma: 2.0  penalty_back_emotion: 2.0
## 19. Appendix C: Sample Data & Configs
Sample Annotation JSON:{  "image_path": "gallery/dark_back_001.jpg",  "face_bbox": null,  "face_confidence": 0.08,  "face_confidence_enhanced": 0.22,  "view": "back_view",  "yaw": 178,  "light_level": "very_low",  "emotion": "No_Face",  "activity": "standing_stationary",  "keypoints": [[...]],  "attributes": {"ear_visible": false, "hair_visible": true, "backpack": true}}Success Criteria for Production Release:- Dark Back Gold accuracy >=96%- False Emotion on Back <=2%- Overall latency <200ms on cloud- No regression on Bright Front accuracy >94%
End of Guide - Iterate with real failures from your gallery