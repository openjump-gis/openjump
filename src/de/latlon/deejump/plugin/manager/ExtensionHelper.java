package de.latlon.deejump.plugin.manager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.vividsolutions.jts.util.Assert;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.task.TaskMonitor;
import com.vividsolutions.jump.util.StringUtil;
import com.vividsolutions.jump.workbench.Logger;
import com.vividsolutions.jump.workbench.WorkbenchContext;
import com.vividsolutions.jump.workbench.plugin.Configuration;
import com.vividsolutions.jump.workbench.plugin.PlugInContext;
import com.vividsolutions.jump.workbench.plugin.PlugInManager;

public class ExtensionHelper {



    
    private ExtensionHelper() {
        // no, no, never instantiate
    }

    public static final void install( ExtensionManagerDialog extensionManager, WorkbenchContext workbenchContext,
            ExtensionWrapper ext, TaskMonitor monitor) throws Exception {
        
        PlugInManager manager = workbenchContext.getWorkbench().getPlugInManager();
        
        monitor.report( I18N.get("deejump.pluging.manager.ExtensionHelper.Downloading-resources"));
        
        File[] files = 
            downloadAndSaveResources( 
                    extensionManager.getExtensionSite(), 
                    ext, 
                    manager.getPlugInDirectory());
        
        URLClassLoader classLoader = new URLClassLoader( toURLs( files ) );

        monitor.report( I18N.get("deejump.pluging.manager.ExtensionHelper.Loading-classes"));
        
        
        // list Extension and/or Configuration classes inside the zips/jars
        List extensionClasses = new ArrayList( 10 );
        for (int i = 0; i < files.length; i++) {
            extensionClasses.addAll( classes( new ZipFile( files[i] ), 
                    classLoader ) );
        }
        
        List configs = new ArrayList();
        for (Iterator iter = extensionClasses.iterator(); iter.hasNext();) {
            Class c = (Class)iter.next();
            Configuration configuration = (Configuration) c.newInstance();
            configs.add( configuration );
        }
        
        monitor.report( I18N.get("deejump.pluging.manager.ExtensionHelper.Loading-extensions"));
        
        
        //finally, load configs/extensions
        // this method will call extension.configure(new PlugInContext(context, null, null,
        //null, null)); )
        loadConfigurations( configs, workbenchContext );
        
    }

    public static final void remove( List fileList,
            ExtensionWrapper ext, TaskMonitor monitor)
    
     throws Exception {
        List resourceList = ext.getResourceList();
        
        for (Iterator iter = fileList.iterator(); iter.hasNext();) {
            File file = (File) iter.next();
            if (file.isFile()) {
                
                if ( resourceList.contains( file.getName() ) ) {
//                    System.out.println("will delete: " + file);
                    
                    monitor.report( I18N.get("deejump.pluging.manager.ExtensionHelper.Deleting-file") + " " + file );
                    
                    boolean deleted = false;
                    
                        try {
                            deleted = file.delete();
                        } catch (SecurityException e) {
                            // TODO decide if re-throw here or handle or log??
                            e.printStackTrace();
                        }
                    
                        //TODO throw exc if false.
                        
                    // hmm no exception thrown?
                    // file permissions?
                    // bye bye
                     System.out.println(I18N.get("deejump.pluging.manager.ExtensionHelper.deleted") + " '" + file+ "': " +  deleted);
                   //???? above hasn't worked, file locked? del on exited didn'T work...
                     //put file on a list and finish it off?
                     // inlcude in a plug-in, listener --> workbench.isclosed -> del file!
//                     file.deleteOnExit();
                     
                     
                     
                }
            }

        }
    }

    private static File[] downloadAndSaveResources(String extensionSite, ExtensionWrapper ext, File plugInDirectory) {
        // TODO pull down resources from ext and save to pluginDir
        List fileList = new ArrayList( ext.getResourceList().size() );
        
        for (Iterator iter = ext.getResourceList().iterator(); iter.hasNext();) {
            String resourceName = (String) iter.next();
            
            try {
                URL resURL = new URL(               //+ ext.getCategory() + "/"
                        "jar:" + extensionSite + resourceName + "!/"
                        );

                JarURLConnection juc = (JarURLConnection)resURL.openConnection();
                JarFile jf = juc.getJarFile();
                
                Enumeration enumer = jf.entries();
                String resourceFile = plugInDirectory + File.separator + resourceName;
                fileList.add( new File(resourceFile) );
                
                FileOutputStream fos = new FileOutputStream( resourceFile );
System.out.println(":vv " + resourceFile);
                JarOutputStream jos = new JarOutputStream( fos );
                
                while (enumer.hasMoreElements()) {
                    JarEntry je = (JarEntry) enumer.nextElement();
                    jos.putNextEntry( new JarEntry( je ));
                    
                    InputStream in = jf.getInputStream( je );   
                    int c;
                    //TODO FIXME use a buffered reader!!!
                    while( (c=in.read()) >= 0 ) {
                        jos.write(c);   
                    }
                    in.close();
                }
                jos.closeEntry();
                jos.close();
                fos.close();
                
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
        }
        return (File[])fileList.toArray( new File[ fileList.size() ]);
    }

    private static void saveRemoteResources( String extensionSite, ExtensionWrapper ext ) {
        List extList = ext.getResourceList();
        for (Iterator iter = extList.iterator(); iter.hasNext();) {
                String resource = (String) iter.next();
                
                try {
                    URL resURL = new URL( extensionSite  + "/" + ext.getCategory() + ext.getName());
                    byte[] bitsAndBytes = null;                    
                    DataInputStream dis = new DataInputStream( resURL.openStream() );
                    dis.readFully( bitsAndBytes );
                    FileOutputStream fos = new FileOutputStream(
                            "c:\temp\ttt.jar"
                            //manager.getPlugInDirectory()+ ext.getName() 
                            );
                    fos.write( bitsAndBytes );
                    fos.close();
                    dis.close();
                    
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        
    }
    
    /* shamelessly copied from PlugInManager */
    private static URL[] toURLs(File[] files) {
        URL[] urls = new URL[files.length];
        for (int i = 0; i < files.length; i++) {
            try {
                System.out.println( "path: " + files[i].getPath()  );
                urls[i] = new URL("jar:file:" + files[i].getPath() + "!/");
            } catch (MalformedURLException e) {
                Assert.shouldNeverReachHere(e.toString());
            }
        }
        return urls;
    }
    
    /* shamelessly copied from PlugInManager */
    private static List classes(ZipFile zipFile, ClassLoader classLoader) {
        ArrayList classes = new ArrayList();
        for (Enumeration e = zipFile.entries(); e.hasMoreElements();) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            //Filter by filename; otherwise we'll be loading all the classes,
            // which takes
            //significantly longer [Jon Aquino]
            if (!(entry.getName().endsWith("Extension.class") || entry
                    .getName().endsWith("Configuration.class"))) {
                //Include "Configuration" for backwards compatibility. [Jon
                // Aquino]
                continue;
            }
            Class c = toClass(entry, classLoader);
            if (c != null) {
                classes.add(c);
            }
        }
        return classes;
    }
    
    /* shamelessly copied from PlugInManager */
    private static Class toClass(ZipEntry entry, ClassLoader classLoader) {
        if (entry.isDirectory()) {
            return null;
        }
        if (!entry.getName().endsWith(".class")) {
            return null;
        }
        if (entry.getName().indexOf("$") != -1) {
            //I assume it's not necessary to load inner classes explicitly.
            // [Jon Aquino]
            return null;
        }
        String className = entry.getName();
        className = className.substring(0, className.length()
                - ".class".length());
        className = StringUtil.replaceAll(className, "/", ".");
        Class candidate;
        try {
            candidate = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Assert.shouldNeverReachHere("Class not found: " + className
                    + ". Refine class name algorithm.");
            return null;
        } catch (Throwable t) {
            Logger.error(t);
            //e.g. java.lang.VerifyError: class
            // org.apache.xml.serialize.XML11Serializer
            //overrides final method [Jon Aquino]

            return null;
        }
        return candidate;
    }
    
    private static void loadConfigurations(List configurations, WorkbenchContext context) throws Exception {
        for (Iterator i = configurations.iterator(); i.hasNext();) {
            Configuration configuration = (Configuration) i.next();
            configuration.configure(new PlugInContext(context, null, null,
                    null, null));
        }
    }
}
