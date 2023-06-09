package utilities.sceneComponents;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import utilities.readers.TextureReader;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT;
import static org.lwjgl.opengl.GL43.*;
public class Texture extends TextureReader {
    private final int USING_UNIT;

    public Texture(int usingUnit, String filepath) {
        super(filepath);
        this.USING_UNIT = usingUnit;
        config(GL_LINEAR_MIPMAP_LINEAR);
        glActiveTexture(GL_TEXTURE0 + usingUnit);
    }

    public void config(int mipMapSampleMode) {
        // if needed, this can be overridden
       genMipMap(mipMapSampleMode); // GL_LINEAR_MIPMAP_LINEAR

        GLCapabilities capabilities = GL.getCapabilities();
        // if graphics card has anisotropic
        if (capabilities.GL_EXT_texture_filter_anisotropic) {
            enableAnisotropic();
        } else {
            System.err.println("Your graphics card does not support anisotropic filtering. So it is disabled.");
        }

    }

    private void genMipMap(int sampleMode) {
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, sampleMode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, sampleMode);
        glGenerateMipmap(GL_TEXTURE_2D);
        shout("generated mipmap.");
    }
    private void enableAnisotropic() {
        FloatBuffer anisoset = BufferUtils.createFloatBuffer(1);
        glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, anisoset);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoset.get(0));
        shout("enabled anisotropic.");
    }
    private void shout(String message) {
        System.out.println("Texture on unit " + USING_UNIT + "(id: " + getTexID() + ") " + message);
    }

    public void bind() {
        glActiveTexture(GL_TEXTURE0 + USING_UNIT);
        glBindTexture(GL_TEXTURE_2D, getTexID());
    }
}
