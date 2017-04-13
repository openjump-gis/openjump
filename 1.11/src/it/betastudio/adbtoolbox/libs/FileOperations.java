package it.betastudio.adbtoolbox.libs;

import java.io.File;

public class FileOperations {

    /*
     * 2015_03_18 Giuseppem Aruta <giuseppe_aruta[at]yahoo.it This class derives
     * from AdBToolbox 1.7
     */
    public static File getFile() {
        return lastVisitedFolder;
    }

    public static void setFile(File lastVisitedFolder1) {

        File thePath = new File(lastVisitedFolder1.getAbsolutePath());

        lastVisitedFolder = thePath;

    };

    public static File lastVisitedFolder;

}
