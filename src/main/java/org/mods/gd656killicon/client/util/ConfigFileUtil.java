package org.mods.gd656killicon.client.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 配置文件文本读取工具。
 * 配置写入统一 UTF-8; 读取时 UTF-8 严格解码优先, 失败回退 GBK,
 * 兼容历史上 FileWriter(平台默认编码 = Windows GBK)写入的旧配置文件,
 * 避免多客户端/JVM 平台编码不一致导致的中文乱码。
 */
public final class ConfigFileUtil {
    private ConfigFileUtil() {
    }

    /** 读取文本: UTF-8 优先, 非 UTF-8(历史 GBK 文件)回退 GBK。 */
    public static String readText(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            // 历史 FileWriter 平台编码(Windows = GBK)写入的旧文件
            return new String(bytes, Charset.forName("GBK"));
        }
    }
}
