package tests;

import java.util.Properties;

import org.testng.annotations.BeforeTest;

import jenkins.Base;

public class BaseTest extends Base{
	public Properties prop;
	
	public static String crumb;
	
	@BeforeTest
	public void intialize() {
		prop=configProp();
		crumb = intialize(prop); 
	}

}
