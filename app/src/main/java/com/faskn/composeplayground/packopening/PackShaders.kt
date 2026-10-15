package com.faskn.composeplayground.packopening

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

// ---- Scene Smoke Shader ----
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun DrawScope.drawSceneSmokeShader(
    shader: RuntimeShader,
    rarity: CardRarity,
    alpha: Float,
    timeSec: Float,
) {
    if (alpha <= 0.005f) return

    val w = size.width
    val h = size.height

    shader.setFloatUniform("resolution", w, h)
    shader.setFloatUniform("color", rarity.color.red, rarity.color.green, rarity.color.blue)
    shader.setFloatUniform("time", timeSec)
    shader.setFloatUniform("alpha", alpha * (0.6f + 0.4f * rarity.glow))

    drawRect(
        brush = ShaderBrush(shader),
        topLeft = Offset.Zero,
        size = Size(w, h),
        blendMode = BlendMode.Plus,
    )
}

internal const val SCENE_SMOKE_AGSL = """
uniform float2 resolution;
uniform float3 color;
uniform float time;
uniform float alpha;

float rand(float2 co) {
    return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
}

float value_noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);

    float a = rand(i);
    float b = rand(i + float2(1.0, 0.0));
    float c = rand(i + float2(0.0, 1.0));
    float d = rand(i + float2(1.0, 1.0));

    float2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    float2 shift = float2(100.0);
    float2x2 rot = float2x2(0.80, 0.60, -0.60, 0.80);
    for (int i = 0; i < 5; ++i) {
        v += a * value_noise(p);
        p = rot * p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

float complex_smoke(float2 uv, float timeSec) {
    float2 q = float2(
        fbm(uv + timeSec * 0.12),
        fbm(uv + float2(1.0, 1.0) + timeSec * 0.15)
    );

    float2 r = float2(
        fbm(uv + 2.5 * q + float2(1.7, 9.2) + 0.16 * timeSec),
        fbm(uv + 2.5 * q + float2(8.3, 2.8) + 0.12 * timeSec)
    );

    return fbm(uv + 3.0 * r + timeSec * 0.08);
}

half4 main(float2 coord) {
    if (alpha <= 0.001) return half4(0.0);

    float2 uv = coord / resolution;
    float aspect = resolution.y / resolution.x;
    float2 center = float2(0.5, 0.5);

    float2 distVec = (uv - center) * float2(1.0, aspect * 0.85);
    float distSq = dot(distVec, distVec);
    float radialMask = exp(-distSq * 5.0);

    if (radialMask <= 0.001) return half4(0.0);

    float2 smokeUv = (uv - center) * float2(3.2, 3.2 * aspect);
    float smoke = complex_smoke(smokeUv, time);

    float density = pow(clamp(smoke, 0.0, 1.0), 1.6) * radialMask * alpha;
    density = clamp(density * 1.35, 0.0, 1.0);

    float3 smokeColor = mix(color * 0.7, color * 1.6, density);

    return half4(half3(smokeColor * density), half(density));
}
"""

// ---- Zip Glow Shader ----
private const val TUBE_RADIUS_DP = 12f
private const val GLOW_RADIUS_DP = 32f

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal fun DrawScope.drawZipGlowShader(
    shader: RuntimeShader,
    rarity: CardRarity,
    head: Offset,
    strength: Float,
    timeSec: Float = 0f,
) {
    val s = strength.coerceIn(0f, 1f)
    if (s <= 0.01f) return

    val glowR = GLOW_RADIUS_DP.dp.toPx()

    shader.setFloatUniform("head", head.x, head.y)
    shader.setFloatUniform("packWidth", size.width)
    shader.setFloatUniform(
        "color",
        rarity.color.red,
        rarity.color.green,
        rarity.color.blue,
    )
    shader.setFloatUniform("strength", s * (0.5f + 0.5f * rarity.glow))
    shader.setFloatUniform("tubeR", TUBE_RADIUS_DP.dp.toPx())
    shader.setFloatUniform("glowR", glowR)
    shader.setFloatUniform("time", timeSec)

    drawRect(
        brush = ShaderBrush(shader),
        topLeft = Offset(0f, head.y - glowR),
        size = Size(size.width, glowR * 2f),
        blendMode = BlendMode.Plus,
    )
}

internal const val ZIP_GLOW_AGSL = """
uniform float2 head;
uniform float packWidth;
uniform float3 color;
uniform float strength;
uniform float tubeR;
uniform float glowR;
uniform float time;

half4 main(float2 coord) {
    if (coord.x < 0.0 || coord.x > packWidth) {
        return half4(0.0);
    }

    float endX = clamp(head.x, 0.0, packWidth);
    float2 a = float2(0.0, head.y);
    float2 b = float2(endX, head.y);
    float2 pa = coord - a;
    float2 ba = b - a;

    float t = clamp(dot(pa, ba) / max(dot(ba, ba), 1.0), 0.0, 1.0);
    float2 nearest = a + ba * t;
    float2 delta = coord - nearest;
    float d2 = dot(delta, delta);

    float bloomRadius = mix(glowR * 0.29, glowR * 0.36, t);
    float midRadius = mix(glowR * 0.14, glowR * 0.19, t);
    float coreRadius = max(tubeR * 0.38, 1.0);

    float outerBloom = exp(-d2 / max(bloomRadius * bloomRadius, 1.0));
    float innerBloom = exp(-d2 / max(midRadius * midRadius, 1.0));
    float core = exp(-d2 / max(coreRadius * coreRadius, 1.0));

    float2 fromHead = coord - head;
    float tipDist2 = dot(fromHead, fromHead);
    float knobRadius = glowR * 0.45;
    float knobCoreRadius = glowR * 0.36;
    float headKnobGlow = exp(-tipDist2 / max(knobRadius * knobRadius, 1.0));
    float headKnobCore = exp(-tipDist2 / max(knobCoreRadius * knobCoreRadius, 1.0));

    float lineAlphaRamp = mix(0.5, 1.0, t);

    float energy = (outerBloom * 0.20 + innerBloom * 0.40 + core * 0.85) * lineAlphaRamp
                 + headKnobGlow * 0.60
                 + headKnobCore * 1.0;
    float alpha = clamp(energy * strength, 0.0, 1.0);

    float whiten = clamp((core * 0.85) * lineAlphaRamp + headKnobCore * 0.95, 0.0, 1.0);
    float3 glowColor = mix(color, float3(1.0), whiten);

    return half4(half3(glowColor * alpha), half(alpha));
}
"""

// ---- Puffy Pack AGSL Shader ----
internal const val PUFFY_AGSL = """
uniform shader image;
uniform float2 size;
uniform float3 press;
uniform float strip;

const float2 LIGHT_POS = float2(0.23, 0.22);
const float LIGHT_HEIGHT = 0.82;

const float SLOPE_STRENGTH = 1.05;
const float DIFFUSE_GAIN = 0.95;
const float SHADOW_LIMIT = -0.12;
const float SHININESS = 64.0;
const float SPEC_STRENGTH = 0.68;
const float BROAD_SPEC_STRENGTH = 0.045;
const float REFRACTION_STRENGTH = 1.2;
const float RIM_STRENGTH = 0.04;

const float SEAM_OFFSET = 0.014;
const float PUFF_START = 0.025;
const float PUFF_END = 0.085;
const float PUFF_HEIGHT = 0.005;

const float SEAM_WRINKLE_AMP = 0.0022;
const float SEAM_WRINKLE_FREQ = 36.0;
const float SEAM_WRINKLE_REACH = 0.16;
const float BODY_WRINKLE_AMP = 0.0010;
const float BODY_WRINKLE_FREQ = 12.0;

const float PRESS_DEPTH = 0.01;
const float PRESS_RADIUS = 0.11;
const float PRESS_WRINKLE_AMP = 0.0008;
const float PRESS_WRINKLE_COUNT = 6.0;
const float PRESS_WRINKLE_RADIUS = 0.16;

float height(float2 uv) {
    float aspect = size.y / size.x;

    float distToSide = max(min(uv.x, 1.0 - uv.x), 1e-4);
    float distBelowSeam = (uv.y - (strip - SEAM_OFFSET)) * aspect;
    float distToBottom = (1.0 - uv.y) * aspect;
    float distVertical = max(min(distBelowSeam, distToBottom), 1e-4);

    float edgePower = pow(1.0 / distToSide, 4.0)
                    + pow(1.0 / distVertical, 4.0);
    float edgeDist = 1.0 / pow(edgePower, 0.25);

    float puff = smoothstep(PUFF_START, PUFF_END, edgeDist);
    float h = PUFF_HEIGHT * (1.0 - (1.0 - puff) * (1.0 - puff));

    float seamMask = (1.0 - smoothstep(0.0, SEAM_WRINKLE_REACH, distVertical))
                   * smoothstep(0.0, 0.02, distBelowSeam);
    float seamPhase = uv.x * SEAM_WRINKLE_FREQ
                    + sin(uv.x * 8.3 + sin(uv.x * 17.0) * 0.8) * 1.2
                    + sin(uv.y * 9.0) * 0.7;
    float seamBreakup = 0.58 + 0.42 * (0.5 + 0.5 * sin(uv.x * 7.1 + sin(uv.x * 13.0)));
    h += SEAM_WRINKLE_AMP * sin(seamPhase) * seamMask * seamBreakup;

    float bodyMask = smoothstep(0.025, 0.18, edgeDist);
    float bodyWaveA = sin((uv.x * 0.82 + uv.y * 0.31) * BODY_WRINKLE_FREQ
                        + sin(uv.x * 3.8 + uv.y * 1.7) * 0.75);
    float bodyWaveB = sin((uv.x * 0.29 - uv.y * 0.61) * (BODY_WRINKLE_FREQ * 0.73)
                        + sin(uv.y * 6.1 + uv.x * 2.0) * 0.65);
    float bodyWave = bodyWaveA * 0.62 + bodyWaveB * 0.38;
    h += BODY_WRINKLE_AMP * bodyWave * bodyMask;

    float pressMask = smoothstep(strip - 0.005, strip + 0.015, uv.y);
    float activePress = press.z * pressMask;

    float2 fromPress = uv - press.xy;
    fromPress.y *= aspect;
    float pressDistSq = dot(fromPress, fromPress);
    float pressDistance = sqrt(pressDistSq);
    float dent = exp(-pressDistSq / (PRESS_RADIUS * PRESS_RADIUS));
    float creaseFalloff = exp(-pressDistSq / (PRESS_WRINKLE_RADIUS * PRESS_WRINKLE_RADIUS));
    float creaseRing = smoothstep(0.0, PRESS_RADIUS * 0.65, pressDistance);

    h -= activePress * PRESS_DEPTH * dent;
    h += activePress * PRESS_WRINKLE_AMP
       * sin(atan(fromPress.y, fromPress.x) * PRESS_WRINKLE_COUNT)
       * creaseFalloff * creaseRing;

    return h;
}

half4 main(float2 coord) {
    float2 uv = coord / size;
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return half4(0.0);
    }

    float2 px = 1.0 / size;
    float aspect = size.y / size.x;

    float dx = height(uv + float2(px.x, 0.0))
             - height(uv - float2(px.x, 0.0));
    float dy = height(uv + float2(0.0, px.y))
             - height(uv - float2(0.0, px.y));

    float slopeX = dx / (2.0 * px.x);
    float slopeY = dy / (2.0 * px.y * aspect);
    float3 normal = normalize(float3(-float2(slopeX, slopeY) * SLOPE_STRENGTH, 1.0));

    float3 lightDir = normalize(float3(
        (LIGHT_POS - uv) * float2(1.0, aspect),
        LIGHT_HEIGHT
    ));
    float3 viewDir = float3(0.0, 0.0, 1.0);
    float3 halfVec = normalize(lightDir + viewDir);

    float diffuse = dot(normal, lightDir) - lightDir.z;
    float shade = diffuse > 0.0
        ? diffuse * DIFFUSE_GAIN
        : max(diffuse, SHADOW_LIMIT);

    float ndh = max(dot(normal, halfVec), 0.0);
    float broadSpecular = pow(ndh, 12.0) * BROAD_SPEC_STRENGTH;
    float sharpSpecular = pow(ndh, SHININESS) * SPEC_STRENGTH;
    float fresnel = pow(1.0 - clamp(dot(normal, viewDir), 0.0, 1.0), 3.0)
                  * RIM_STRENGTH;
    float specular = broadSpecular + sharpSpecular + fresnel;

    float2 refractCoord = clamp(
        coord + normal.xy * REFRACTION_STRENGTH,
        float2(0.0),
        size - float2(1.0)
    );
    half4 baseColor = image.eval(refractCoord);

    half3 rgb = baseColor.rgb * half(1.0 + shade) + half3(specular) * baseColor.a;
    return half4(rgb, baseColor.a);
}
"""

// ---- Holographic Foil AGSL Shader ----
internal const val SHIFT_FOIL_AGSL = """
uniform float2 resolution;
uniform float2 pointer;
uniform float time;
uniform float strength;
uniform float tier;
uniform float3 tint;

half4 main(float2 p) {
    float2 uv = p / max(resolution, float2(1.0));
    float2 d = uv - pointer;
    float spot = exp(-dot(d * float2(1.0, 1.35), d * float2(1.0, 1.35)) * 11.0);

    float axis = uv.x * 0.82 + uv.y * 0.53 - time - pointer.x * 0.18;
    float wave = axis * 6.2831853;
    float beam = pow(max(0.0, cos(wave)), 38.0);
    float softBeam = pow(max(0.0, cos(wave)), 8.0);

    float2 edgeDistance = min(uv, 1.0 - uv);
    float outerEdge = 1.0 - smoothstep(0.002, 0.035, min(edgeDistance.x, edgeDistance.y));

    float artX = smoothstep(0.035, 0.055, uv.x) * (1.0 - smoothstep(0.945, 0.965, uv.x));
    float artY = smoothstep(0.025, 0.045, uv.y) * (1.0 - smoothstep(0.73, 0.78, uv.y));
    float artArea = artX * artY;
    float artBorder = max(0.0, smoothstep(0.005, 0.025, uv.x) - smoothstep(0.045, 0.065, uv.x));
    artBorder += max(0.0, smoothstep(0.935, 0.955, uv.x) - smoothstep(0.975, 0.995, uv.x));
    artBorder *= artY;

    float footer = smoothstep(0.75, 0.83, uv.y);
    float frameMask = max(outerEdge, artBorder * 0.55);
    float faceMask = (tier >= 2.0 ? 0.12 : 0.0) * artArea;
    float footerMask = footer * (tier >= 3.0 ? 0.46 : (tier >= 2.0 ? 0.22 : 0.05));
    float mask = clamp(frameMask + faceMask + footerMask, 0.0, 1.0);

    float spectral = clamp((tier - 1.0) / 3.0, 0.0, 1.0);
    float3 rainbow = float3(
        0.52 + 0.48 * cos(wave + 0.0),
        0.52 + 0.48 * cos(wave + 2.094),
        0.52 + 0.48 * cos(wave + 4.188)
    );
    float3 foilColor = mix(tint, rainbow, spectral * 0.85);
    float energy = (beam * 0.65 + softBeam * 0.24 + spot * 0.13) * strength * mask;
    energy = clamp(energy, 0.0, 0.8);
    return half4(half3(foilColor * energy), half(energy));
}
"""
