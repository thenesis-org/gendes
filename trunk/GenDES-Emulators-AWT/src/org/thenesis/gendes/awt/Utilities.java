package org.thenesis.gendes.awt;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

public class Utilities {

	public static String getFileName(String pathName) {
        int index=pathName.lastIndexOf(File.separator);
        if (index!=-1) pathName=pathName.substring(index+1);
		return pathName;
	}
	
    public static String removeFileExtension(String filename) {
        int index=filename.lastIndexOf('.');
        if (index!=-1) filename=filename.substring(0, index);
        return filename;
    }
    
    public static String replaceFileExtension(String filename, String extension) {
        int index=filename.lastIndexOf('.');
        if (index!=-1) filename=filename.substring(0, index);
        return filename+extension;
    }
    
    public static String findFreeFilename(String name, int startIndex, String suffix) {
        String fullname=null;
        
        try {
            File f;
            String pathname;
            
            // Check if the path is valid.
            f=new File(name);
            pathname=f.getParent();
            f=new File(pathname);
            if (!f.exists()) {
                if (!f.mkdirs()) return null; // Cannot create the path.
            }
            
            // Find a non-existing filename.
            pathname=removeFileExtension(name);
            while (true) {
                fullname=pathname+"-"+startIndex+suffix;
                f=new File(fullname);
                if (!f.exists()) break;
                startIndex++;
            }
        } catch (Exception e) { return null; }
        
        return fullname;
    }

    public static boolean saveImage(BufferedImage img, String filename) {
        try {
            File file=new File(filename);
            ImageIO.write(img, "png", file);
        } catch (Exception e) {
            return true;
        }
        return false;        
    }
}
