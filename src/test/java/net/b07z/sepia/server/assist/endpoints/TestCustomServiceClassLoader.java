package net.b07z.sepia.server.assist.endpoints;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import net.b07z.sepia.server.assist.data.Parameter;
import net.b07z.sepia.server.assist.server.ConfigServices;
import net.b07z.sepia.server.assist.services.ServiceInfo;
import net.b07z.sepia.server.assist.services.ServiceInterface;
import net.b07z.sepia.server.core.tools.SandboxClassLoader;

/**
 * Test loading custom services via {@link SandboxClassLoader} and {@link ConfigServices}.
 * @author Florian
 *
 */
public class TestCustomServiceClassLoader {

	@Test
	public void test() throws ClassNotFoundException, IOException, InstantiationException, IllegalAccessException {
		String userId = "test";
		String myServiceClass = "BuildTest";
		String className = ConfigServices.getCustomServicesPackage() + "." + userId + "." + myServiceClass;
		
		String dir = ConfigServices.getCustomServicesBaseFolder() + userId + "/";
    	File uploadDir = new File(dir);
    	File classFile = new File(dir + myServiceClass + ".class");
    	//System.out.println("Class name: " + className);
    	assertTrue(uploadDir.isDirectory());
    	assertTrue(classFile.canRead());
    	//System.out.println("File URL: " + classFile.toURI().toURL());
    	    	    	
    	/*
    	URLClassLoader classLoader = new URLClassLoader(new URL[]{uploadDir.toURI().toURL()});
    	System.out.println("ClassLoader path: " + classLoader.getURLs()[0]);
    	classLoader.loadClass(className);
    	ServiceInterface service = (ServiceInterface) classLoader.loadClass(className).newInstance();
    	*/
    	
    	ConfigServices.setupSandbox();
    	SandboxClassLoader sbcl = ConfigServices.addCustomServiceClassLoader(className);
    	ServiceInterface service = (ServiceInterface) sbcl.loadClass(className).newInstance();
    	
    	ServiceInfo si = service.getInfo("en");
    	assertTrue(si.serviceType.equals(ServiceInfo.Type.program.toString()));
    	
    	Parameter customParameter = si.getAllParameters().get(0);
    	assertTrue(customParameter.getName().contains(myServiceClass));
	}

}
