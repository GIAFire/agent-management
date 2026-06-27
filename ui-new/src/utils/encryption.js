/**
 * AES-256-GCM 前端加密工具
 * 使用 Web Crypto API
 */
class ApiKeyCrypto {

    static masterKeyB64key = "SzdnWThuUDJzWDltUTR2RjN3UjZ0RTFhTDVvWjhjVTA="

    /**
     * AES-256-GCM 加密（与Java后端完全兼容）
     */
    static async encrypt(plainText, masterKeyB64) {
        if (!plainText) throw new Error('明文不能为空');

        // 1. 解码主密钥（Base64 → ArrayBuffer）
        const keyBuffer = this.base64ToBuffer(masterKeyB64);
        const cryptoKey = await crypto.subtle.importKey(
            'raw',
            keyBuffer,
            { name: 'AES-GCM' },
            false,
            ['encrypt']
        );

        // 2. 生成12字节随机IV（GCM标准）
        const iv = crypto.getRandomValues(new Uint8Array(12));

        // 3. 加密
        const encoder = new TextEncoder();
        const dataBuffer = encoder.encode(plainText);

        const cipherBuffer = await crypto.subtle.encrypt(
            {
                name: 'AES-GCM',
                iv: iv,
                tagLength: 128  // 128 bits = 16 bytes
            },
            cryptoKey,
            dataBuffer
        );

        // 4. 组合：IV(12字节) + 密文(含16字节认证标签)
        const combined = new Uint8Array(iv.length + cipherBuffer.byteLength);
        combined.set(iv, 0);
        combined.set(new Uint8Array(cipherBuffer), iv.length);

        // 5. Base64编码
        return this.bufferToBase64(combined);
    }

    static base64ToBuffer(base64) {
        const binaryString = atob(base64);
        const bytes = new Uint8Array(binaryString.length);
        for (let i = 0; i < binaryString.length; i++) {
            bytes[i] = binaryString.charCodeAt(i);
        }
        return bytes.buffer;
    }

    static bufferToBase64(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = '';
        for (let i = 0; i < bytes.length; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        return btoa(binary);
    }
}

// 导出
export default ApiKeyCrypto;
