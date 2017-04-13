package jumptest.junit;

import com.vividsolutions.jump.util.FileUtil;
import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by UMichael on 28/03/2016.
 */
public class FileUtilTestCase extends TestCase {

    public FileUtilTestCase(String Name_) {
        super(Name_);
    }

    public static void main(String[] args) {
        String[] testCaseName = {FileUtilTestCase.class.getName()};
        junit.textui.TestRunner.main(testCaseName);
    }

    public void testZip() throws IOException {
        File tmp1 = File.createTempFile("testZip",".txt");
        File tmp2 = File.createTempFile("testZip",".txt");
        File tmp3 = File.createTempFile("testZip",".txt");
        File zip1 = File.createTempFile("testZip",".zip");
        try (FileWriter fw1 = new FileWriter(tmp1);
             FileWriter fw2 = new FileWriter(tmp2);
             FileWriter fw3 = new FileWriter(tmp3);
             BufferedWriter bw1 = new BufferedWriter(fw1);
             BufferedWriter bw2 = new BufferedWriter(fw2);
             BufferedWriter bw3 = new BufferedWriter(fw3)) {
            for (int i = 0 ; i < 1000 ; i++) {
                bw1.write("azertyuiop\n");
                bw2.write("qsdfghjklm\n");
                bw3.write("wxcvbn\n");
            }
        }
        FileUtil.zip(Arrays.asList(tmp1, tmp2, tmp3), zip1);
        assertTrue(isClosed(tmp1));
        assertTrue(isClosed(tmp2));
        assertTrue(isClosed(tmp3));
        assertTrue(isClosed(zip1));
        assertTrue(tmp1.delete());
        assertTrue(tmp2.delete());
        assertTrue(tmp3.delete());
        assertTrue(zip1.delete());
    }

    // small tip to check the file is lock-free
    // (this is just a tip. It seems very difficult to check a file is
    // lockfree in a way that works in all the cases for any platform)
    private boolean isClosed(File file) {
        return file.renameTo(file);
    }
}
