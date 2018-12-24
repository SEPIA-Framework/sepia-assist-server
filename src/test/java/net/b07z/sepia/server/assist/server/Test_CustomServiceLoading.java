package net.b07z.sepia.server.assist.server;

import java.io.File;
import java.net.MalformedURLException;
import net.b07z.sepia.server.assist.services.ServiceInterface;

public class Test_CustomServiceLoading {

	public static void main(String[] args) throws ExceptionInInitializerError, InstantiationException, IllegalAccessException, ClassNotFoundException, MalformedURLException {
		String userId = "uid1007";
		String myServiceClass = "HelloWorld";
		String className = ConfigServices.getCustomPackage() + "." + userId + "." + myServiceClass;
		
		String dir = Config.sdkClassesFolder + "services/" + userId + "/";
    	File uploadDir = new File(dir);
    	File classFile = new File(dir + myServiceClass + ".class");
    	System.out.println("Class name: " + className);
    	System.out.println("Access to dir '" + uploadDir.getAbsolutePath() + "'? " + uploadDir.isDirectory());
    	System.out.println("Access to file '" + classFile.getAbsolutePath() + "'? " + classFile.canRead());
    	System.out.println("File URL: " + classFile.toURI().toURL());
    	    	    	
    	/*
    	URLClassLoader classLoader = new URLClassLoader(new URL[]{uploadDir.toURI().toURL()});
    	System.out.println("ClassLoader path: " + classLoader.getURLs()[0]);
    	classLoader.loadClass(className);
    	ServiceInterface service = (ServiceInterface) classLoader.loadClass(className).newInstance();
    	*/
    	
    	ConfigServices.setupSandbox();
    	ServiceInterface service = (ServiceInterface) ConfigServices.addCustomClassLoader(className).loadClass(className).newInstance();
    	
    	System.out.println(service.getInfo("en").serviceType);
	}

}
