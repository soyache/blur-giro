// Adapted from DuoFold-Android (MIT). See NOTICE and THIRD_PARTY_NOTICES.txt
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif

varying vec3 vWorldPos;

uniform sampler2D uTexture;
uniform vec2 uTexelSize;
uniform float uAperture;
uniform float uMaxBlurPixels;
uniform float uCameraDistance;
uniform vec2 uPhotoHalfSize;
uniform vec2 uPhotoOffset;

vec4 samplePhoto(vec2 uv, float lod) {
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        return vec4(0.0, 0.0, 0.0, 1.0);
    }
    vec2 edgeDist = min(uv, 1.0 - uv) / uTexelSize;
    float edgeAlpha = clamp(min(edgeDist.x, edgeDist.y), 0.0, 1.0);
    vec4 col = texture2D(uTexture, uv, lod);
    return vec4(col.rgb * edgeAlpha, 1.0);
}

void main() {
    float denom = max(0.02 * uCameraDistance, uCameraDistance - vWorldPos.z);
    float t = uCameraDistance / denom;
    vec2 tablePos = vWorldPos.xy * t;

    vec2 relPos = tablePos - uPhotoOffset;
    vec2 photoUV;
    photoUV.x = (relPos.x + uPhotoHalfSize.x) / (2.0 * uPhotoHalfSize.x);
    photoUV.y = 1.0 - (relPos.y + uPhotoHalfSize.y) / (2.0 * uPhotoHalfSize.y);

    float vGap = abs(vWorldPos.z);
    float normGap = clamp(vGap * 0.95, 0.0, 1.0);
    float easeIn = normGap * normGap;
    float blurCoC = clamp(easeIn * uAperture * 1.5, 0.0, 1.0);

    if (blurCoC <= 0.0001) {
        gl_FragColor = samplePhoto(photoUV, 0.0);
        return;
    }

    float radius = blurCoC * uMaxBlurPixels;
    vec2 stepUV = uTexelSize * radius;
    float lodBias = clamp(blurCoC * 3.0, 0.0, 3.0);

    float ign = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float angle = ign * 6.283185307;

    const float C_STEP = -0.73736888;
    const float S_STEP =  0.67549029;

    vec2 curDir = vec2(cos(angle), sin(angle));

    vec4 sum = vec4(0.0);
    float totalWeight = 0.0;

    for (int i = 0; i < 16; i++) {
        float r = sqrt((float(i) + 0.5) * 0.0625);
        vec2 sampleUV = photoUV + curDir * (r * stepUV);

        sum += samplePhoto(sampleUV, lodBias);
        totalWeight += 1.0;

        curDir = vec2(
            curDir.x * C_STEP - curDir.y * S_STEP,
            curDir.y * C_STEP + curDir.x * S_STEP
        );
    }

    gl_FragColor = sum / totalWeight;
}
