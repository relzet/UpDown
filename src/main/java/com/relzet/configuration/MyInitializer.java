package com.relzet.configuration;

import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;

public class MyInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		return new Class[] { MyConfiguration.class };
	}
 
	@Override
	protected Class<?>[] getServletConfigClasses() {
		return null;
	}
 
	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

    @Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
    	registration.setMultipartConfig(getMultipartConfigElement());
	}

    private MultipartConfigElement getMultipartConfigElement(){
		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(LOCATION, MAX_FILE_SIZE, MAX_REQUEST_SIZE, FILE_SIZE_THRESHOLD);
		return multipartConfigElement;
	}
    
    /*Set these variables for your project needs*/ 
    
	private static final String LOCATION = "D:/temp/";

	private static final long MAX_FILE_SIZE = 1024 * 1024 * 512;//512MB
	
	private static final long MAX_REQUEST_SIZE = 1024 * 1024 * 512;//512MB

    private static final int FILE_SIZE_THRESHOLD = 0;
}
