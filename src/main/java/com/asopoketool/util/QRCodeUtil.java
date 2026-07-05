package com.asopoketool.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class QRCodeUtil {

    public static byte[] generateQRCode(String content, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }

    public static String generatePayload(Long entryId, Long tournamentId, String hmacSecret) {
        String data = entryId + "|" + tournamentId;
        String signature = calculateHmacSha256(data, hmacSecret);
        return data + "|" + signature;
    }

    public static Long verifyPayload(String payload, String hmacSecret) throws Exception {
        String[] parts = payload.split("\\|");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid QR payload format");
        }
        
        String entryIdStr = parts[0];
        String tournamentIdStr = parts[1];
        String signature = parts[2];
        
        String data = entryIdStr + "|" + tournamentIdStr;
        String expectedSignature = calculateHmacSha256(data, hmacSecret);
        
        if (!expectedSignature.equals(signature)) {
            throw new SecurityException("QR signature mismatch! Possible tampering detected.");
        }
        
        return Long.parseLong(entryIdStr);
    }

    private static String calculateHmacSha256(String data, String key) {
        try {
            byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, "HmacSHA256");
            sha256Hmac.init(keySpec);
            byte[] macData = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(macData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to calculate HMAC-SHA256", e);
        }
    }
}
