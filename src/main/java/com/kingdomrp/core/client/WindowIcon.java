package com.kingdomrp.core.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Установка иконки окна игры из ресурсов мода.
 * <p>
 * Ваниль ставит иконку окна один раз в конструкторе {@code Minecraft} из КОРНЯ
 * game-jar ({@code icons/icon_*.png}, см. {@code Window.setIcon}/{@code IconSet}) —
 * туда мод дотянуться не может. Поэтому переустанавливаем иконку сами через GLFW
 * после загрузки клиента (на главном потоке, см. {@code ClientModEvents}).
 * <p>
 * Кладём PNG в {@code src/main/resources/icons/}: GLFW выбирает ближайший размер
 * под нужды ОС (панель задач/заголовок/Alt-Tab), поэтому даём набор размеров;
 * отсутствующие файлы просто пропускаются.
 */
public final class WindowIcon {

    private static final String[] SIZES = {"16x16", "32x32", "48x48", "128x128", "256x256"};

    private WindowIcon() {}

    public static void apply() {
        long handle = Minecraft.getInstance().getWindow().getWindow();

        List<NativeImage> images = new ArrayList<>();
        List<ByteBuffer> buffers = new ArrayList<>();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (String size : SIZES) {
                String path = "/icons/icon_" + size + ".png";
                try (InputStream in = WindowIcon.class.getResourceAsStream(path)) {
                    if (in != null) images.add(NativeImage.read(in));
                } catch (Exception e) {
                    // повреждённый/нечитаемый файл — пропускаем, не валим запуск
                }
            }
            if (images.isEmpty()) return;

            GLFWImage.Buffer glfwImages = GLFWImage.malloc(images.size(), stack);
            for (int i = 0; i < images.size(); i++) {
                NativeImage img = images.get(i);
                ByteBuffer buf = MemoryUtil.memAlloc(img.getWidth() * img.getHeight() * 4);
                buffers.add(buf);
                buf.asIntBuffer().put(img.getPixelsRGBA());
                glfwImages.position(i);
                glfwImages.width(img.getWidth());
                glfwImages.height(img.getHeight());
                glfwImages.pixels(buf);
            }
            glfwImages.position(0);
            GLFW.glfwSetWindowIcon(handle, glfwImages);
        } catch (Exception e) {
            // иконка некритична — не мешаем запуску игры
        } finally {
            buffers.forEach(MemoryUtil::memFree);
            images.forEach(NativeImage::close);
        }
    }
}
